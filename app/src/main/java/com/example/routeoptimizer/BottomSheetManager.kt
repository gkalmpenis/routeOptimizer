package com.example.routeoptimizer

import android.view.View
import android.widget.Button
import com.google.android.material.bottomsheet.BottomSheetBehavior
import android.widget.TextView
import com.mapbox.api.geocoding.v5.models.CarmenFeature
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.example.routeoptimizer.SymbolsManagerInterface
import com.example.routeoptimizer.RouteOptimizationInterface
import com.mapbox.api.optimization.v1.MapboxOptimization
import com.example.routeoptimizer.BottomSheetManager.StopsButtonState
import com.example.routeoptimizer.MainActivity
import android.widget.Toast
import com.example.routeoptimizer.R
import timber.log.Timber
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.optimization.v1.models.OptimizationResponse
import com.mapbox.api.optimization.v1.models.OptimizationWaypoint
import com.example.routeoptimizer.BottomSheetManager
import com.mapbox.geojson.Point
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/*
     The current idea is that this class will contain all the information that appear in the bottom sheet like:
     - CarmenFeature of the current selected location
     - placeNameTextView information
     - stopsButton information
     - currentStopsCounter

     The stuff in here should not be static, a single instance of this object should be created in the MainActivity
     and this class should help us get at any time information about the current state of all the info that appear
     inside the bottom sheet
    */

class BottomSheetManager  // A private Constructor prevents any other class from instantiating.
private constructor() {
    private var bottomSheet: View? = null
    private var bottomSheetBehavior: BottomSheetBehavior<View?>? = null
    private var placeNameTextView: TextView? = null
    private var stopsButton: Button? = null
    private var currentStopsCounterTextView: TextView? = null
    private var optimizeButton: Button? = null
    private var currentCarmenFeature: CarmenFeature? = null
    private var currentCarmenFeatureGeometry: Point? = null
    private var optimizedRoute: DirectionsRoute? = null

    protected enum class StopsButtonState {
        ADD_NEW_STOP, REMOVE_A_STOP
    }

    companion object {

        private var bottomSheetManager: BottomSheetManager? = null

        // If there is no instance available, create a new one
        protected val instance: BottomSheetManager?
            protected get() {
                if (bottomSheetManager == null) { // If there is no instance available, create a new one
                    bottomSheetManager = BottomSheetManager()
                }
                return bottomSheetManager
            }
    }

    private val ADD_AS_STOP = "Add as stop" // Should be exactly the same as the text in R.string.add_stop_txt
    private val REMOVE_FROM_STOPS = "Remove from stops" // Should be exactly the same as the text in R.string.remove_stop_txt
    private var symbolsManagerInterface : SymbolsManagerInterface? = null // To perform CRUD operations on map symbols
    private var routeOptimizationInterface: RouteOptimizationInterface? = null
    private var optimizedClient: MapboxOptimization? = null

    /* Methods protected by singleton-ness */
    protected fun setBottomSheetReference(bottomSheet: View?) { this.bottomSheet = bottomSheet }
    protected fun initializeBottomSheetBehavior() { bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet) }
    protected fun changeBottomSheetState(newState: Int) { bottomSheetBehavior!!.state = newState }
    protected fun setPlaceNameReference(placeNameTextView: TextView?) { this.placeNameTextView = placeNameTextView }
    protected fun setStopsButtonReference(stopsButton: Button?) { this.stopsButton = stopsButton }
    protected fun setCurrentStopsCounterReference(currentStopsCounterTextView: TextView?) { this.currentStopsCounterTextView = currentStopsCounterTextView }
    protected fun setOptimizeButtonReference(optimizeButton: Button?) { this.optimizeButton = optimizeButton }
    protected fun setSymbolsManagerInterface(symbolsManagerInterface: SymbolsManagerInterface?) { this.symbolsManagerInterface = symbolsManagerInterface }
    protected fun setRouteOptimizationInterface(routeOptimizationInterface: RouteOptimizationInterface?) { this.routeOptimizationInterface = routeOptimizationInterface }

    /**
     * Adds an onClick listener that will show/hide the bottomSheet when user clicks anywhere on it
     */
    protected fun setOnClickListener() {
        bottomSheet!!.setOnClickListener {
            if (bottomSheetBehavior!!.state == BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetBehavior!!.setState(BottomSheetBehavior.STATE_COLLAPSED)
            } else {
                bottomSheetBehavior!!.setState(BottomSheetBehavior.STATE_EXPANDED)
            }
        }
    }

    protected fun changePlaceNameText(newText: String?) {
        placeNameTextView!!.text = newText
    }

    protected fun changeStateOfStopsButton(state: StopsButtonState) {
        // So far this method only changes the text of the button
        if (state == StopsButtonState.ADD_NEW_STOP) {
            stopsButton!!.text = ADD_AS_STOP
        }
        if (state == StopsButtonState.REMOVE_A_STOP) {
            stopsButton!!.text = REMOVE_FROM_STOPS
        }
    }

    protected fun setCurrentCarmenFeature(
        currentCarmenFeature: CarmenFeature,
        currentCarmenFeatureGeometry: Point
    ) {
        this.currentCarmenFeature = currentCarmenFeature
        this.currentCarmenFeatureGeometry = currentCarmenFeatureGeometry
    }

    /**
     * Checks if the *stopsHashMap* contains the *currentCarmenFeature* (which was either searched or clicked)
     * and updates the state of the *stopsButton* accordingly
     */
    protected fun refreshStateOfStopsButton() {
        if (MainActivity.stopsHashMap.containsKey(currentCarmenFeatureGeometry)) {
            changeStateOfStopsButton(StopsButtonState.REMOVE_A_STOP)
        } else {
            changeStateOfStopsButton(StopsButtonState.ADD_NEW_STOP)
        }
    }

    protected fun addStopsButtonOnClickListener() {
        stopsButton!!.setOnClickListener { v ->
            val button = v as Button
            val buttonText = button.text as String
            when (buttonText) {
                ADD_AS_STOP -> {
                    // Add the manager's currently shown CarmenFeature in MainActivity's HashMap
                    MainActivity.stopsHashMap[currentCarmenFeatureGeometry!!] =
                        currentCarmenFeature!!

                    // Update the current stops counter
                    currentStopsCounterTextView!!.text = MainActivity.stopsHashMap.size.toString()
                    decideOptimizeButtonVisibility()

                    // Update the marker in that location
                    symbolsManagerInterface!!.updateSymbolIconInMap(symbolsManagerInterface!!.latestSearchedSymbol)
                    symbolsManagerInterface!!.changeIconSize(
                        symbolsManagerInterface!!.latestSearchedSymbol,
                        SymbolsManagerInterface.BLUE_MARKER_EXPANDED_SIZE
                    )

                    // Change the stopsButton's text
                    changeStateOfStopsButton(StopsButtonState.REMOVE_A_STOP)
                }
                REMOVE_FROM_STOPS -> {
                    // Remove the selectedCarmenFeature from the HashMap
                    MainActivity.stopsHashMap.remove(currentCarmenFeatureGeometry)

                    // Update the current stops counter
                    currentStopsCounterTextView!!.text = MainActivity.stopsHashMap.size.toString()
                    decideOptimizeButtonVisibility()

                    // Update the marker in that location
                    symbolsManagerInterface!!.updateSymbolIconInMap(symbolsManagerInterface!!.latestSearchedSymbol)

                    // Change the stopsButton's text
                    changeStateOfStopsButton(StopsButtonState.ADD_NEW_STOP)
                }
                else -> {}
            }
        }
    }

    protected fun decideOptimizeButtonVisibility() {
        if (MainActivity.stopsHashMap.size < 2) {
            optimizeButton!!.visibility = View.GONE
        } else {
            optimizeButton!!.visibility = View.VISIBLE
        }
    }

    protected fun addOptimizeButtonOnClickListener() {
        optimizeButton!!.setOnClickListener {
            if (MainActivity.stopsHashMap.size > 12) {
                Toast.makeText(
                    bottomSheet!!.context,
                    R.string.only_twelve_stops_allowed,
                    Toast.LENGTH_LONG
                ).show()
            } else {
                val coordinates = routeOptimizationInterface!!.convertStopsToPoints()
                //                Point firstPoint = coordinates.get(0);                      // The list of coordinates has at least two items because "optimizeButton" appears after two items have been inserted in stopsHashMap,
                //                Point lastPoint = coordinates.get(coordinates.size() - 1);  // so we can safely obtain a firstPoint and lastPoint from them.
                getOptimizedRoute(coordinates)
            }
        }
    }

    protected fun getOptimizedRoute(coordinates: List<Point?>?) {
        Timber.d("----- BEFORE BUILD ------")

        // Build the optimized route
        optimizedClient = MapboxOptimization.builder()
            .source(DirectionsCriteria.SOURCE_FIRST)
            .destination(DirectionsCriteria.DESTINATION_LAST)
            .coordinates(coordinates!!)
            .roundTrip(false)
            .overview(DirectionsCriteria.OVERVIEW_FULL)
            .profile(DirectionsCriteria.PROFILE_DRIVING) //.steps(true)
            .accessToken(bottomSheet!!.resources.getString(R.string.mapbox_access_token))
            .build()
        Timber.d("----- AFTER BUILD ------")
        optimizedClient.enqueueCall(object : Callback<OptimizationResponse?> {
            override fun onResponse(
                call: Call<OptimizationResponse?>,
                response: Response<OptimizationResponse?>
            ) {
                Timber.d("----- INSIDE onResponse ------")
                if (!response.isSuccessful) {
                    Timber.d("----- 1. ------")
                    Timber.d(bottomSheet!!.resources.getString(R.string.no_success))
                    Toast.makeText(
                        bottomSheet!!.context,
                        bottomSheet!!.resources.getString(R.string.no_success),
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    if (response.body() != null) {
                        Timber.d("----- 2. ------")
                        val routes = response.body()!!
                            .trips()
                        if (routes != null) {
                            Timber.d("----- 3. ------")
                            if (routes.isEmpty()) {
                                Timber.d("----- 4. ------")
                                Timber.d(
                                    "%s size = %s",
                                    bottomSheet!!.resources.getString(R.string.successful_but_no_routes),
                                    routes.size
                                )
                                Toast.makeText(
                                    bottomSheet!!.context,
                                    bottomSheet!!.resources.getString(R.string.successful_but_no_routes),
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Timber.d("----- 5. ------")
                                // Get most optimized route from API response
                                optimizedRoute = routes[0]
                                Timber.d("\tNow will show the order of the waypoints")
                                for (w in response.body()!!.waypoints()!!) {
                                    Timber.d("\t\t---------------------")
                                    Timber.d("\t\twaypoint index: " + w.waypointIndex())
                                    Timber.d("\t\twaypoint name: " + w.name())
                                    Timber.d("\t\twaypoint location: " + w.location())
                                    Timber.d("\t\ttrips index: " + w.tripsIndex())
                                }
                                Timber.d("----- BEFORE DRAW ------")
                                routeOptimizationInterface!!.drawOptimizedRoute(optimizedRoute)
                                Timber.d("----- BEFORE UPDATE OF SYMBOL ICON NUMBERS ------")
                                symbolsManagerInterface!!.updateNumberInSymbolIcons(
                                    response.body()!!.waypoints()
                                )
                            }
                        } else {
                            Timber.d("----- 6. ------")
                            Timber.d("List of routes in the response is null")
                            Toast.makeText(
                                bottomSheet!!.context, String.format(
                                    bottomSheet!!.resources.getString(R.string.null_in_response),
                                    "The Optimization API response's body"
                                ), Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }

            override fun onFailure(call: Call<OptimizationResponse?>, t: Throwable) {
                Timber.d("Error: %s", t.message)
            }
        })
    }
}