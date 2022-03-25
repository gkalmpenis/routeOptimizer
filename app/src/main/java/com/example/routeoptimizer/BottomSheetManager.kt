package com.example.routeoptimizer

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.google.android.material.bottomsheet.BottomSheetBehavior
import android.widget.TextView
import com.mapbox.api.geocoding.v5.models.CarmenFeature
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.example.routeoptimizer.SymbolsManagerInterface
import com.example.routeoptimizer.RouteOptimizationInterface
import com.mapbox.api.optimization.v1.MapboxOptimization
//import com.example.routeoptimizer.BottomSheetManager.StopsButtonState
import com.example.routeoptimizer.MainActivity
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import com.example.routeoptimizer.R
import timber.log.Timber
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.optimization.v1.models.OptimizationResponse
import com.mapbox.api.optimization.v1.models.OptimizationWaypoint
import com.example.routeoptimizer.BottomSheetManager
import com.example.routeoptimizer.databinding.BottomSheetPersistentBinding
import com.example.routeoptimizer.viewmodels.MainActivityViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.mapbox.geojson.Point
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.LinkedHashMap

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

//class BottomSheetManager  // A private Constructor prevents any other class from instantiating.
//private constructor() {
//object BottomSheetManager {
//class BottomSheetManager: BottomSheetDialogFragment() {
class BottomSheetManager: ConstraintLayout {
    constructor(context: Context) : super (context)
    constructor(context: Context, attrSet: AttributeSet) : super(context, attrSet)
    constructor(context: Context, attrSet: AttributeSet, defStyleAttr: Int): super (context, attrSet, defStyleAttr)

    init {
        Timber.d("--Kaloume thn init tou bottomSheetManager--")
        binding = BottomSheetPersistentBinding.inflate(LayoutInflater.from(context), this, true)
    }
//
//    // Variable "this" in this class refers to the View of bottomSheet per se!
//
////    private lateinit var activity: Activity
//
    private lateinit var binding: BottomSheetPersistentBinding
//
////    private val binding = BottomSheetPersistentBinding.inflate(context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater)
////        .also { addView(it.root) }
//
//    //    private lateinit var bottomSheetView: View // Should be replaced with "this" . DELETE when done!
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
//    private val bottomSheetBehavior: BottomSheetBehavior<View> = BottomSheetBehavior.from(this)
//
//    private lateinit var mainActivityViewModel: MainActivityViewModel
//    private var currentCarmenFeature: CarmenFeature? = null
//    private var currentCarmenFeatureGeometry: Point? = null
//    private var optimizedRoute: DirectionsRoute? = null
//
//    enum class StopsButtonState {
//        ADD_NEW_STOP, REMOVE_A_STOP
//    }
//
//    private var ADD_AS_STOP: String = resources.getString(R.string.add_stop_txt)
//    private var REMOVE_FROM_STOPS: String = resources.getString(R.string.remove_stop_txt)
//    private var symbolsManagerInterface : SymbolsManagerInterface? = null // To perform CRUD operations on map symbols
//    private var routeOptimizationInterface: RouteOptimizationInterface? = null
//    private var optimizedClient: MapboxOptimization? = null
//
////    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
////        Timber.d("--Mphkame sthn onCreateView--")
////        binding = BottomSheetPersistentBinding.inflate(inflater, container, false)
////        return binding.root
////    }
////
////    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
////        super.onViewCreated(view, savedInstanceState)
////        Timber.d("--Mphkame sthn onViewCreated--")
////
////        ADD_AS_STOP = resources.getString(R.string.add_stop_txt)
////        REMOVE_FROM_STOPS = resources.getString(R.string.remove_stop_txt)
////
////        decideOptimizeButtonVisibility(mainActivityViewModel.stopsHashMap.value)
////        changeBottomSheetState(BottomSheetBehavior.STATE_HIDDEN) // Do not reveal bottom sheet on creation of the application.
////
////        // Initialize listeners
////        setOnClickListener()
////        addStopsButtonOnClickListener()
////    }
//
    fun initValues(mainActivityViewModel: MainActivityViewModel) {
        Timber.d("--Mphkame sthn initValues--")
//        this.activity = activity
//        this.mainActivityViewModel = mainActivityViewModel
//        this.bottomSheetView = bottomSheetView // Set bottom sheet reference
        bottomSheetBehavior = BottomSheetBehavior.from(this)
//        changeBottomSheetState(BottomSheetBehavior.STATE_HIDDEN) // Do not reveal bottom sheet on creation of the application.

//        decideOptimizeButtonVisibility(mainActivityViewModel.stopsHashMap.value)
//        changeBottomSheetState(BottomSheetBehavior.STATE_HIDDEN) // Do not reveal bottom sheet on creation of the application.

        // Initialize listeners
//        setOnClickListener()
//        addStopsButtonOnClickListener()

        Timber.d("--Twra tha bgoume apo thn initValues--")

        // Afou edwses times twra prepei na kaneis ta upoloipa initialization steps sthn onViewCreated, kai tha deikseis to bottomsheet me thn show() mesa sto mainactivity
    }
//
//    /* Methods protected by singleton-ness */
////    fun setBottomSheetReference(bottomSheet: View?) { this.bottomSheet = bottomSheet }
////    fun initializeBottomSheetBehavior() { bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet) }
//    fun changeBottomSheetState(newState: Int) { bottomSheetBehavior.state = newState }
////    protected fun setPlaceNameReference(placeNameTextView: TextView?) { this.placeNameTextView = placeNameTextView }
////    protected fun setStopsButtonReference(stopsButton: Button?) { this.stopsButton = stopsButton }
////    protected fun setCurrentStopsCounterReference(currentStopsCounterTextView: TextView?) { this.currentStopsCounterTextView = currentStopsCounterTextView }
////    protected fun setOptimizeButtonReference(optimizeButton: Button?) { this.optimizeButton = optimizeButton }
//    fun setSymbolsManagerInterface(symbolsManagerInterface: SymbolsManagerInterface?) { this.symbolsManagerInterface = symbolsManagerInterface }
//    fun setRouteOptimizationInterface(routeOptimizationInterface: RouteOptimizationInterface?) { this.routeOptimizationInterface = routeOptimizationInterface }
//
//    /**
//     * Adds an onClick listener that will show/hide the bottomSheet when user clicks anywhere on it
//     */
//    private fun setOnClickListener() {
//        this.setOnClickListener {
//            if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
//                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED)
//            } else {
//                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED)
//            }
//        }
//    }
//
//    fun changePlaceNameText(newText: String?) {
//        Timber.d("--Mphkame sthn changePlaceNameText, newText: $newText--")
////        binding.tvPlaceName.text = newText
//    }
//
//    private fun changeStateOfStopsButton(state: StopsButtonState) {
//        // So far this method only changes the text of the button
//        if (state == StopsButtonState.ADD_NEW_STOP) {
////            binding.btnStops.text = ADD_AS_STOP
//        }
//        if (state == StopsButtonState.REMOVE_A_STOP) {
////            binding.btnStops.text = REMOVE_FROM_STOPS
//        }
//    }
//
//    fun setCurrentCarmenFeature(currentCarmenFeature: CarmenFeature, currentCarmenFeatureGeometry: Point) {
//        this.currentCarmenFeature = currentCarmenFeature
//        this.currentCarmenFeatureGeometry = currentCarmenFeatureGeometry
//    }
//
//    /**
//     * Checks if the *stopsHashMap* contains the *currentCarmenFeature* (which was either searched or clicked)
//     * and updates the state of the *stopsButton* accordingly
//     */
//    fun refreshStateOfStopsButton(stopsHashMap: LinkedHashMap<Point, CarmenFeature>?) {
//        Timber.d("--kalesame thn refreshStateOfStopsButton()--")
//        stopsHashMap?.let {
//            if (it.containsKey(currentCarmenFeatureGeometry)) {
//                changeStateOfStopsButton(StopsButtonState.REMOVE_A_STOP)
//            } else {
//                changeStateOfStopsButton(StopsButtonState.ADD_NEW_STOP)
//            }
//        }
//    }
//
////    private fun addStopsButtonOnClickListener() {
////        Timber.d("--mphkame sto addStopsButtonOnClickListener--")
////        binding.btnStops.setOnClickListener {
////            Timber.d("--mphkame sto binding.btnStops.setOnClickListener--")
////            when (binding.btnStops.text) {
////                ADD_AS_STOP -> {
////                    // Add the manager's currently shown CarmenFeature in MainActivity's HashMap
//////                    MainActivity.stopsHashMap[currentCarmenFeatureGeometry!!] = currentCarmenFeature!!
////                    Timber.d("--Vazoume stop ston hashmap--")
////                    mainActivityViewModel.addStopToMap(currentCarmenFeatureGeometry!!, currentCarmenFeature!!)
////
////                    // Update the current stops counter
//////                    binding.tvCurrentStopsCounter.text = MainActivity.stopsHashMap.size.toString()
////                    Timber.d("--kanoume update ton counter--")
////                    binding.tvCurrentStopsCounter.text = mainActivityViewModel.stopsHashMap.value?.size.toString() //
////
////                    // kanw comment ta parakatw epithdes na dw ti tha ginei an kanw to stopshashmap null
////
//////                    decideOptimizeButtonVisibility(MainActivity.stopsHashMap)
////
////                    // Update the marker in that location
//////                    symbolsManagerInterface!!.updateSymbolIconInMap(symbolsManagerInterface!!.latestSearchedSymbol)
//////                    symbolsManagerInterface!!.changeIconSize(
//////                        symbolsManagerInterface!!.latestSearchedSymbol,
//////                        SymbolsManagerInterface.BLUE_MARKER_EXPANDED_SIZE
//////                    )
////
////                    // Change the stopsButton's text
////                    changeStateOfStopsButton(StopsButtonState.REMOVE_A_STOP)
////                }
////                REMOVE_FROM_STOPS -> {
////                    // Remove the selectedCarmenFeature from the HashMap
//////                    MainActivity.stopsHashMap.remove(currentCarmenFeatureGeometry)
////
////                    // Update the current stops counter
//////                    binding.tvCurrentStopsCounter.text = MainActivity.stopsHashMap.size.toString()
//////                    decideOptimizeButtonVisibility(MainActivity.stopsHashMap)
////
////                    // Update the marker in that location
//////                    symbolsManagerInterface!!.updateSymbolIconInMap(symbolsManagerInterface!!.latestSearchedSymbol)
////
////                    // Change the stopsButton's text
//////                    changeStateOfStopsButton(StopsButtonState.ADD_NEW_STOP)
////                }
////                else -> { /* There should not be another case */ }
////            }
////        }
////    }
//
//    /**
//     * Will decide if "Optimize" button will be shown.
//     * If there are >=2 stops it will, else it will not.
//     */
//    private fun decideOptimizeButtonVisibility(hashMap: LinkedHashMap<Point, CarmenFeature>?) {
//        Timber.d("decideOptimizeButtonVisibility() called")
//        if (hashMap == null || hashMap.size < 2) {
//            Timber.d("decideOptimizeButtonVisibility() --> hashMap is null or hashMap.size < 2")
//
////            binding.btnOptimize.visibility = View.GONE
//        } else {
//            Timber.d("decideOptimizeButtonVisibility() --> hashMap.size >= 2")
//
////            binding.btnOptimize.visibility = View.VISIBLE
//        }
//    }
//
////    protected fun addOptimizeButtonOnClickListener() {
////        optimizeButton!!.setOnClickListener {
////            if (MainActivity.stopsHashMap.size > 12) {
//                    // replace "bottomSheetView" with "this"
////                Toast.makeText(
////                    bottomSheetView!!.context,
////                    R.string.only_twelve_stops_allowed,
////                    Toast.LENGTH_LONG
////                ).show()
////            } else {
////                val coordinates = routeOptimizationInterface!!.convertStopsToPoints()
////                //                Point firstPoint = coordinates.get(0);                      // The list of coordinates has at least two items because "optimizeButton" appears after two items have been inserted in stopsHashMap,
////                //                Point lastPoint = coordinates.get(coordinates.size() - 1);  // so we can safely obtain a firstPoint and lastPoint from them.
////                getOptimizedRoute(coordinates)
////            }
////        }
////    }
//
////    protected fun getOptimizedRoute(coordinates: List<Point?>?) {
////        Timber.d("----- BEFORE BUILD ------")
////
//    // // replace "bottomSheetView" with "this"
//
////        // Build the optimized route
////        optimizedClient = MapboxOptimization.builder()
////            .source(DirectionsCriteria.SOURCE_FIRST)
////            .destination(DirectionsCriteria.DESTINATION_LAST)
////            .coordinates(coordinates!!)
////            .roundTrip(false)
////            .overview(DirectionsCriteria.OVERVIEW_FULL)
////            .profile(DirectionsCriteria.PROFILE_DRIVING) //.steps(true)
////            .accessToken(bottomSheetView!!.resources.getString(R.string.mapbox_access_token))
////            .build()
////        Timber.d("----- AFTER BUILD ------")
////        optimizedClient.enqueueCall(object : Callback<OptimizationResponse?> {
////            override fun onResponse(
////                call: Call<OptimizationResponse?>,
////                response: Response<OptimizationResponse?>
////            ) {
////                Timber.d("----- INSIDE onResponse ------")
////                if (!response.isSuccessful) {
////                    Timber.d("----- 1. ------")
////                    Timber.d(bottomSheetView!!.resources.getString(R.string.no_success))
////                    Toast.makeText(
////                        bottomSheetView!!.context,
////                        bottomSheetView!!.resources.getString(R.string.no_success),
////                        Toast.LENGTH_LONG
////                    ).show()
////                } else {
////                    if (response.body() != null) {
////                        Timber.d("----- 2. ------")
////                        val routes = response.body()!!
////                            .trips()
////                        if (routes != null) {
////                            Timber.d("----- 3. ------")
////                            if (routes.isEmpty()) {
////                                Timber.d("----- 4. ------")
////                                Timber.d(
////                                    "%s size = %s",
////                                    bottomSheetView!!.resources.getString(R.string.successful_but_no_routes),
////                                    routes.size
////                                )
////                                Toast.makeText(
////                                    bottomSheetView!!.context,
////                                    bottomSheetView!!.resources.getString(R.string.successful_but_no_routes),
////                                    Toast.LENGTH_SHORT
////                                ).show()
////                            } else {
////                                Timber.d("----- 5. ------")
////                                // Get most optimized route from API response
////                                optimizedRoute = routes[0]
////                                Timber.d("\tNow will show the order of the waypoints")
////                                for (w in response.body()!!.waypoints()!!) {
////                                    Timber.d("\t\t---------------------")
////                                    Timber.d("\t\twaypoint index: " + w.waypointIndex())
////                                    Timber.d("\t\twaypoint name: " + w.name())
////                                    Timber.d("\t\twaypoint location: " + w.location())
////                                    Timber.d("\t\ttrips index: " + w.tripsIndex())
////                                }
////                                Timber.d("----- BEFORE DRAW ------")
////                                routeOptimizationInterface!!.drawOptimizedRoute(optimizedRoute)
////                                Timber.d("----- BEFORE UPDATE OF SYMBOL ICON NUMBERS ------")
////                                symbolsManagerInterface!!.updateNumberInSymbolIcons(
////                                    response.body()!!.waypoints()
////                                )
////                            }
////                        } else {
////                            Timber.d("----- 6. ------")
////                            Timber.d("List of routes in the response is null")
////                            Toast.makeText(
////                                bottomSheetView!!.context, String.format(
////                                    bottomSheetView!!.resources.getString(R.string.null_in_response),
////                                    "The Optimization API response's body"
////                                ), Toast.LENGTH_SHORT
////                            ).show()
////                        }
////                    }
////                }
////            }
////
////            override fun onFailure(call: Call<OptimizationResponse?>, t: Throwable) {
////                Timber.d("Error: %s", t.message)
////            }
////        })
////    }
//
////    companion object {
////        @Volatile private var mInstance: BottomSheetManager? = null
////
////        fun getInstance(): BottomSheetManager =
////            mInstance ?: synchronized(this) {
////                val newInstance = mInstance ?: BottomSheetManager().also { mInstance = it }
////                newInstance
////            }
////    }
}