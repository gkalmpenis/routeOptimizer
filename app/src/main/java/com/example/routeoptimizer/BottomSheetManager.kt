package com.example.routeoptimizer

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.routeoptimizer.databinding.BottomSheetPersistentBinding
import com.example.routeoptimizer.viewmodels.MainActivityViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.geocoding.v5.models.CarmenFeature
import com.mapbox.api.optimization.v1.MapboxOptimization
import com.mapbox.api.optimization.v1.models.OptimizationResponse
import com.mapbox.geojson.Point
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber
import kotlin.time.DurationUnit
import kotlin.time.toDuration


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

class BottomSheetManager: ConstraintLayout {
    constructor(context: Context) : super (context)
    constructor(context: Context, attrSet: AttributeSet) : super(context, attrSet)
    constructor(context: Context, attrSet: AttributeSet, defStyleAttr: Int): super (context, attrSet, defStyleAttr)

    init {
        binding = BottomSheetPersistentBinding.inflate(LayoutInflater.from(context), this, true)
    }
//
//    // Variable "this" in this class refers to the View of bottomSheet per se!
//
    private lateinit var activity: Activity // Is it needed? If yes, it can be set inside initValues()
//
    private lateinit var binding: BottomSheetPersistentBinding
    //    private lateinit var bottomSheetView: View // Should be replaced with "this" . DELETE when done!
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

    private lateinit var mainActivityViewModel: MainActivityViewModel
    private var currentCarmenFeature: CarmenFeature? = null
    private var currentCarmenFeatureGeometry: Point? = null
    private lateinit var optimizedRoute: DirectionsRoute

    enum class StopsButtonState {
        ADD_NEW_STOP, REMOVE_A_STOP
    }

    private var ADD_AS_STOP: String = resources.getString(R.string.add_stop_txt)
    private var REMOVE_FROM_STOPS: String = resources.getString(R.string.remove_stop_txt)
    private lateinit var symbolsManagerInterface : SymbolsManagerInterface // To perform CRUD operations on map symbols
    private lateinit var routeOptimizationInterface: RouteOptimizationInterface
    private lateinit var optimizedClient: MapboxOptimization

    fun initValues(activity: Activity, mainActivityViewModel: MainActivityViewModel) {
        this.activity = activity // needed?
        this.mainActivityViewModel = mainActivityViewModel
//        this.bottomSheetView = bottomSheetView // Set bottom sheet reference // delete because bottomSheetView is "this" !
        bottomSheetBehavior = BottomSheetBehavior.from(this)
        redesignWhenExpanded()

        symbolsManagerInterface = activity as SymbolsManagerInterface
        routeOptimizationInterface = activity as RouteOptimizationInterface

        changeBottomSheetState(BottomSheetBehavior.STATE_HIDDEN) // Do not reveal bottom sheet on creation of the application.

//        decideOptimizeButtonVisibility(DataRepository.stopsHashMap)

        // Initialize listeners
        setOnClickListener()
        addStopsButtonOnClickListener()
        addOptimizeButtonOnClickListener()
        addClearButtonOnClickListener()
    }

    fun changeBottomSheetState(newState: Int) { bottomSheetBehavior.state = newState }

    /**
     * Adds an onClick listener that will show/hide the bottomSheet when user clicks anywhere on it
     */
    private fun setOnClickListener() {
        this.setOnClickListener {
            Timber.d("setOnClickListener() for bottomsheet called")
            if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED)
            } else {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED)
            }
        }
    }

    fun setPlaceNameText(newText: String?) {
        binding.tvPlaceName.text = newText
    }

    private fun setStateOfStopsButton(state: StopsButtonState) {
        // So far this method only changes the text of the button

        when (state) {
            StopsButtonState.ADD_NEW_STOP -> binding.btnStops.text = ADD_AS_STOP
            StopsButtonState.REMOVE_A_STOP -> binding.btnStops.text = REMOVE_FROM_STOPS
        }
    }

//    /**
//     * Sets the text of stops counter according to the size of stopsHashMap
//     */
//    fun setStopsCounterText(hashMap: LinkedHashMap<Point, CarmenFeature>) {
//        binding.tvCurrentStopsCounter.text = hashMap.size.toString()
//    }

    fun setStopsCounterText(text: String) {
        binding.tvCurrentStopsCounter.text = text
    }

    fun setCurrentCarmenFeature(currentCarmenFeature: CarmenFeature, currentCarmenFeatureGeometry: Point) {
        this.currentCarmenFeature = currentCarmenFeature
        this.currentCarmenFeatureGeometry = currentCarmenFeatureGeometry
    }

    /**
     * Checks if the *stopsHashMap* contains the *currentCarmenFeature* (which was either searched or clicked)
     * and updates the state of the *stopsButton* accordingly
     */
    fun refreshStateOfStopsButton(stopsHashMap: LinkedHashMap<Point, CarmenFeature>?) {
        stopsHashMap?.let {
            if (it.containsKey(currentCarmenFeatureGeometry)) {
                setStateOfStopsButton(StopsButtonState.REMOVE_A_STOP)
            } else {
                setStateOfStopsButton(StopsButtonState.ADD_NEW_STOP)
            }
        }
    }

    private fun addStopsButtonOnClickListener() {
        binding.btnStops.setOnClickListener {
            when (binding.btnStops.text) {
                ADD_AS_STOP -> {
                    Timber.d("addStopsButtonOnClickListener() called, case add as stop")

                    // Add the manager's currently shown CarmenFeature in stopsHashMap
//                    DataRepository.stopsHashMap[currentCarmenFeatureGeometry!!] = currentCarmenFeature!!
                    mainActivityViewModel.addToStopsHashMap(currentCarmenFeatureGeometry!!, currentCarmenFeature!!)

                    // Update the current stops counter
//                    Timber.d("--kanoume update ton counter--")
//                    binding.tvCurrentStopsCounter.text = DataRepository.stopsHashMap.size.toString()

//                    decideOptimizeButtonVisibility(DataRepository.stopsHashMap)

                    // Update the marker in that location. It should have been red, make it blue
                    symbolsManagerInterface.switchSymbolIconInMap(symbolsManagerInterface.getLatestSearchedSymbol()!!)
                    symbolsManagerInterface.changeIconAndTextSize(
                        symbolsManagerInterface.getLatestSearchedSymbol()!!,
                        SymbolsManagerInterface.BLUE_MARKER_EXPANDED_ICON_SIZE,
                        SymbolsManagerInterface.BLUE_MARKER_EXPANDED_TEXT_SIZE
                    )

                    // Change the stopsButton's text
                    setStateOfStopsButton(StopsButtonState.REMOVE_A_STOP)
                }
                REMOVE_FROM_STOPS -> {
                    Timber.d("addStopsButtonOnClickListener() called, case remove from stops")

                    // Remove the selectedCarmenFeature from the HashMap
//                    DataRepository.stopsHashMap.remove(currentCarmenFeatureGeometry!!)
                    mainActivityViewModel.removeFromStopsHashMap(currentCarmenFeatureGeometry!!)

//                    // Update the current stops counter
//                    binding.tvCurrentStopsCounter.text = DataRepository.stopsHashMap.size.toString()

//                    decideOptimizeButtonVisibility(DataRepository.stopsHashMap)

                    // Update the marker in that location. It should have been blue, make it red
                    symbolsManagerInterface.switchSymbolIconInMap(symbolsManagerInterface.getLatestSearchedSymbol()!!)

                    // Remove text of the previous blue marker (if existed)
                    symbolsManagerInterface.getLatestSearchedSymbol()!!.textField = ""

                    // Change the stopsButton's text
                    setStateOfStopsButton(StopsButtonState.ADD_NEW_STOP)
                }
                else -> { /* There should not be another case */ }
            }
        }
    }

    /**
     * Will decide if "Optimize" button will be shown.
     * If stopsHashMap contains >=2 stops it will, else it will not.
     */
    fun decideOptimizeButtonVisibility(hashMapSize: Int) {
        Timber.d("decideOptimizeButtonVisibility() called")
        if (hashMapSize < 2) {
            binding.btnOptimize.visibility = View.GONE
        } else {
            binding.btnOptimize.visibility = View.VISIBLE
        }
    }


    /**
     * This method solves a UI bug that made the bottom sheet appear incorrectly when expanded
     */
    private fun redesignWhenExpanded() {
        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_EXPANDED -> {
                        bottomSheet.post {
                            //workaround for the when the bottomsheet expands with incorrect UI
                            bottomSheet.requestLayout()
                            //bottomSheet.invalidate() // Seems to have no effect
                        }
                    }
                    else -> {/* No action */}
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        })
    }

    private fun addOptimizeButtonOnClickListener() {
        binding.btnOptimize.setOnClickListener {
//            if (DataRepository.stopsHashMap.size > 12) {
            if (mainActivityViewModel.getStopsHashMapSize() > 12) {
                Toast.makeText(context, R.string.only_twelve_stops_allowed, Toast.LENGTH_LONG).show()
            } else {
//                val coordinates = DataRepository.convertStopsToPoints(DataRepository.stopsHashMap)
                val coordinates = mainActivityViewModel.convertStopsToPoints(mainActivityViewModel.stopsHashMap)
                //                Point firstPoint = coordinates.get(0);                      // The list of coordinates has at least two items because "optimizeButton" appears after two items have been inserted in stopsHashMap,
                //                Point lastPoint = coordinates.get(coordinates.size() - 1);  // so we can safely obtain a firstPoint and lastPoint from them.
                getOptimizedRoute(coordinates)
            }
        }
    }

    private fun getOptimizedRoute(coordinates: List<Point?>) {

        // Build the optimized route
        optimizedClient = MapboxOptimization.builder()
            .source(DirectionsCriteria.SOURCE_FIRST) // First means the first element found in coordinates, which would be the first element inserted in stopsHashMap
            .destination(DirectionsCriteria.DESTINATION_LAST) // Last means the last element found in coordinates
            .coordinates(coordinates)
            .roundTrip(false)
            .overview(DirectionsCriteria.OVERVIEW_FULL)
            .profile(DirectionsCriteria.PROFILE_DRIVING)
//            .steps(true) // Turn-by-turn instructions
            .accessToken(this.resources.getString(R.string.mapbox_access_token))
            .build()

        optimizedClient.enqueueCall(object : Callback<OptimizationResponse?> {
            override fun onResponse(call: Call<OptimizationResponse?>, response: Response<OptimizationResponse?>) {
                if (!response.isSuccessful) {
                    Toast.makeText(context, resources.getString(R.string.no_success), Toast.LENGTH_LONG).show()
                } else {
                    if (response.body() != null) {
                        val routes = response.body()!!.trips()
                        if (routes != null) {
                            if (routes.isEmpty()) {
                                Timber.d("%s size = %s", resources.getString(R.string.successful_but_no_routes), routes.size)
                                Toast.makeText(context, resources.getString(R.string.successful_but_no_routes), Toast.LENGTH_SHORT).show()
                            } else {
                                // Get most optimized route from API response
                                optimizedRoute = routes[0]
                                val distance = routes[0].distance() // distance in meters
                                val duration = routes[0].duration() // duration in seconds

                                // Below snippet is for debug
//                                Timber.d("\tOrder of the waypoints:")
//                                for (w in response.body()!!.waypoints()!!) {
//                                    Timber.d("\t\t---------------------")
//                                    Timber.d("\t\twaypoint index: ${w.waypointIndex()}")
//                                    Timber.d("\t\twaypoint name: ${w.name()}")
//                                    Timber.d("\t\twaypoint location: ${w.location()}")
//                                    Timber.d("\t\ttrips index: ${w.tripsIndex()}")
//                                }
                                Timber.d("Drawing the optimized route on map...")
                                routeOptimizationInterface.drawOptimizedRoute(optimizedRoute)
                                symbolsManagerInterface.updateNumberInSymbolIcons(response.body()!!.waypoints()!!)
                                setTripDistance(distance)
                                setTripDuration(duration)
                                setVisibilityOfDistanceAndDuration(View.VISIBLE)
                            }
                        } else {
                            Timber.d("List of routes in the response is null")
                            Toast.makeText(context, String.format(resources.getString(R.string.null_in_response), "The Optimization API response's body"), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            override fun onFailure(call: Call<OptimizationResponse?>, t: Throwable) {
                Timber.d("Error: %s", t.message)
            }
        })
    }

    private fun setTripDistance(distance: Double?) {
        distance?.let {
            binding.tvTripDistanceValue.text = String.format("%.2f km", it*0.001)
        }
    }

    private fun setTripDuration(duration: Double?) {
        duration?.let {
            val convertedDuration = it.toDuration(DurationUnit.SECONDS)
            val timeString = when (it) {
                in 0f..3600f -> convertedDuration.toComponents { minutes, seconds, nanoseconds ->
                    String.format("%02d:%02d", minutes, seconds)
                }
                else -> convertedDuration.toComponents { hours, minutes, seconds, nanoseconds ->
                    String.format("%02d:%02d:%02d", hours, minutes, seconds)
                }
            }

            binding.tvTripDurationValue.text = timeString
        }
    }

    fun setVisibilityOfDistanceAndDuration(visibility: Int) {
        binding.tvTripDistanceText.visibility = visibility
        binding.tvTripDistanceValue.visibility = visibility
        binding.tvTripDurationText.visibility = visibility
        binding.tvTripDurationValue.visibility = visibility
    }

    private fun addClearButtonOnClickListener() {
        binding.btnClear.setOnClickListener {
            val dialog = MaterialAlertDialogBuilder(context, R.style.MaterialAlertDialog_rounded)
                .setMessage(R.string.clear_every_stop_txt)
                .setPositiveButton(R.string.yes) { _, _ -> symbolsManagerInterface.clearMapData() }
                .setNegativeButton(R.string.no) { dialog, _ -> dialog.dismiss() }
                .setCancelable(true)

            dialog.show()
        }
    }
}