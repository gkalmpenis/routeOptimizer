package com.example.routeoptimizer

import androidx.appcompat.app.AppCompatActivity
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.android.core.permissions.PermissionsListener
import com.example.routeoptimizer.SymbolsManagerInterface
import com.example.routeoptimizer.RouteOptimizationInterface
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.mapboxsdk.location.LocationComponent
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager
import com.example.routeoptimizer.BottomSheetManager
import android.os.Bundle
import timber.log.Timber
import timber.log.Timber.DebugTree
import com.mapbox.mapboxsdk.Mapbox
import com.example.routeoptimizer.R
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.mapbox.mapboxsdk.maps.Style.OnStyleLoaded
import android.graphics.BitmapFactory
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.location.modes.CameraMode
import android.content.Intent
import com.mapbox.mapboxsdk.plugins.places.autocomplete.PlaceAutocomplete
import com.mapbox.mapboxsdk.plugins.places.autocomplete.model.PlaceOptions
import com.example.routeoptimizer.MainActivity
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.plugins.annotation.OnSymbolClickListener
import com.mapbox.api.geocoding.v5.models.CarmenFeature
import android.app.Activity
import android.graphics.Color
import android.view.View
import com.mapbox.geojson.FeatureCollection
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions
import android.widget.Toast
import com.mapbox.mapboxsdk.maps.MapboxMap.OnMapLongClickListener
import com.mapbox.api.geocoding.v5.MapboxGeocoding
import com.mapbox.api.geocoding.v5.GeocodingCriteria
import com.mapbox.api.geocoding.v5.models.GeocodingResponse
import com.mapbox.core.exceptions.ServicesException
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.LineString
import com.mapbox.api.optimization.v1.models.OptimizationWaypoint
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.Feature
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.Symbol
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.ArrayList
import java.util.LinkedHashMap

class MainActivity : AppCompatActivity(), OnMapReadyCallback, PermissionsListener, SymbolsManagerInterface, RouteOptimizationInterface {

    // variables for adding location layer
    private var mapView: MapView? = null
    private var mapboxMap: MapboxMap? = null

    // variables for adding location layer
    private var permissionsManager: PermissionsManager? = null
    private var locationComponent: LocationComponent? = null

    private val geojsonSourceLayerId = "geojsonSourceLayerId"
    private val symbolIconId = "symbolIconId" // Maybe won't be used, should delete?
    private val RED_MARKER = "RED_MARKER" // Corresponds to locations that are searched but not added in stopsHashMap
    private val BLUE_MARKER = "BLUE_MARKER" // Corresponds to locations added in stopsHashMap
    private var latestSearchedLocationSymbol // Will contain symbolOptions for the latest user searched location's symbol (either searched or clicked)
            : Symbol? = null
    private var symbolManager // SymbolManager to add/remove symbols on the map
            : SymbolManager? = null

    // Variable to manipulate the bottom sheet
    private var bottomSheetManager: BottomSheetManager? = null

    companion object {
        // variables for adding search functionality
        private const val REQUEST_CODE_AUTOCOMPLETE = 1
        private const val TEAL_COLOR = "#23D2BE" // For optimized route's line
        private const val POLYLINE_WIDTH = 5f // For optimized route's line
        private var counter //DELETE THIS!
                = 0

        // HashMap that will contain locations that user adds as stop
        @JvmField
        var stopsHashMap = LinkedHashMap<Point, CarmenFeature>()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Timber for logging
        if (BuildConfig.DEBUG) {
            Timber.plant(DebugTree())
        }
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token))
        setContentView(R.layout.activity_main)

        mapView = findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        // Initialize unique BottomSheetManager instance
        bottomSheetManager = BottomSheetManager.instance
        bottomSheetManager.setSymbolsManagerInterface(this)
        bottomSheetManager.setBottomSheetReference(findViewById(R.id.bottomSheet))
        bottomSheetManager.initializeBottomSheetBehavior()
        bottomSheetManager.setPlaceNameReference(findViewById(R.id.placeNameTextView))
        bottomSheetManager.setStopsButtonReference(findViewById(R.id.stopsButton))
        bottomSheetManager.setCurrentStopsCounterReference(findViewById(R.id.currentStopsCounterTextView))
        bottomSheetManager.setOptimizeButtonReference(findViewById(R.id.optimizeButton))
        bottomSheetManager.setRouteOptimizationInterface(this)

        bottomSheetManager.changeBottomSheetState(BottomSheetBehavior.STATE_HIDDEN) // Do not reveal bottom sheet on creation of the application.
        bottomSheetManager.decideOptimizeButtonVisibility() // Should not be visible until stopsHashMap contains at least 2 stops.
        bottomSheetManager.setOnClickListener()
        bottomSheetManager.addStopsButtonOnClickListener()
        bottomSheetManager.addOptimizeButtonOnClickListener()
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        mapboxMap.setStyle(Style.MAPBOX_STREETS) { style ->
            enableLocationComponent(style)
            initSearchFab() // Initiates location search floating action button

            // Add the symbol layer icon to map and specify a name for each of the markers.
            style.addImage(RED_MARKER, BitmapFactory.decodeResource(this@MainActivity.resources, R.drawable.mapbox_marker_icon_default))
            style.addImage(BLUE_MARKER, BitmapFactory.decodeResource(this@MainActivity.resources, R.drawable.map_default_map_marker))

            // Create an empty GeoJSON source using the empty feature collection
            setUpSource(style)

            // Initialize symbol manager to add and remove icons on the map
            initializeSymbolManager(style)

            // Set up a layer to display the Symbols on the map
//                initSymbolLayer(style);

            // Set up a layer to display the optimized route's line
            initOptimizedRouteLineLayer(style)

            addAnnotationClickListener()
            addMapLongClickListener()
        }
    }

    private fun enableLocationComponent(loadedMapStyle: Style) {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            // Activate the MapboxMap LocationComponent to show user location
            // Adding in LocationComponentOptions is also an optional parameter
            locationComponent = mapboxMap!!.locationComponent
            locationComponent!!.activateLocationComponent(this, loadedMapStyle)
            locationComponent!!.isLocationComponentEnabled = true
            locationComponent!!.renderMode = RenderMode.COMPASS
            // Set the component's camera mode
            locationComponent!!.cameraMode = CameraMode.TRACKING
        } else {
            permissionsManager = PermissionsManager(this)
            permissionsManager!!.requestLocationPermissions(this)
        }
    }

    private fun initSearchFab() {
        findViewById<View>(R.id.fab_location_search).setOnClickListener {
            val intent = PlaceAutocomplete.IntentBuilder()
                    .accessToken((if (Mapbox.getAccessToken() != null) Mapbox.getAccessToken() else getString(R.string.mapbox_access_token))!!)
                    .placeOptions(PlaceOptions.builder()
                            .backgroundColor(Color.parseColor("#EEEEEE"))
                            .limit(10)
                            .build(PlaceOptions.MODE_CARDS))
                    .build(this@MainActivity)
            startActivityForResult(intent, REQUEST_CODE_AUTOCOMPLETE)
        }
    }

    private fun setUpSource(loadedMapStyle: Style) {
        loadedMapStyle.addSource(GeoJsonSource(geojsonSourceLayerId))
    }

    private fun initializeSymbolManager(style: Style) {
        symbolManager = SymbolManager(mapView!!, mapboxMap!!, style)

        // Set non-data-driven properties
        symbolManager!!.iconAllowOverlap = true
        symbolManager!!.textAllowOverlap = true
    }

    private fun initSymbolLayer(loadedMapStyle: Style) {
        loadedMapStyle.addLayer(SymbolLayer("SYMBOL_LAYER_ID",
                geojsonSourceLayerId).withProperties(
//                iconImage(symbolIconId),
                PropertyFactory.iconImage(symbolIconId),
                PropertyFactory.iconAllowOverlap(true),
                PropertyFactory.iconOffset(arrayOf(0f, -8f))
        ))
    }

    private fun initOptimizedRouteLineLayer(loadedMapStyle: Style) {
        loadedMapStyle.addSource(GeoJsonSource("optimized-route-source-id"))
        loadedMapStyle.addLayerBelow(LineLayer("optimized-route-layer-id", "optimized-route-source-id")
                .withProperties(
                        PropertyFactory.lineColor(Color.parseColor(TEAL_COLOR)),
                        PropertyFactory.lineWidth(POLYLINE_WIDTH)
                ), symbolManager!!.layerId)
    }

    /**
     * Adds a listener on **every** symbol (also called *annotation*) that will be created.
     */
    private fun addAnnotationClickListener() {
        symbolManager!!.addClickListener(OnSymbolClickListener { symbol -> // Currently there is functionality only for clicking blue markers, clicking on
            // a red one will have no effect.
            if (symbol.iconImage == BLUE_MARKER) {
                // If the previously displayed symbol was red, delete it
                deleteSymbolFromMapIfRed(latestSearchedLocationSymbol)
                resetIconSizeInBlueMarkers()

                // Expand the symbol size of the currently displayed blue marker
                changeIconSize(symbol, SymbolsManagerInterface.BLUE_MARKER_EXPANDED_SIZE)

                // The new symbol now becomes the latest one we searched
                latestSearchedLocationSymbol = symbol

                // Get CarmenFeature from geometry. The symbol was blue so the location exists in stopsHashMap
                val carmenFeatureOfSelectedSymbol = stopsHashMap[symbol.geometry]
                bottomSheetManager!!.setCurrentCarmenFeature(carmenFeatureOfSelectedSymbol!!, symbol.geometry)

                // Update place name in bottom sheet
                bottomSheetManager!!.changePlaceNameText(carmenFeatureOfSelectedSymbol.placeName())

                // Update the text of stopsButton
                bottomSheetManager!!.refreshStateOfStopsButton()

                // Reveal bottom sheet. Will work even if it is already in "STATE_EXPANDED"
                bottomSheetManager!!.changeBottomSheetState(BottomSheetBehavior.STATE_EXPANDED)
            }
            true
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_AUTOCOMPLETE) {
            // Retrieve selected location's CarmenFeature
            val selectedCarmenFeature = PlaceAutocomplete.getPlace(data)
            //            Point selectedCarmenFeatureGeometry = (Point) selectedCarmenFeature.geometry();

            // Create a new FeatureCollection and add a new Feature to it using selectedCarmenFeature above.
            // Then retrieve and update the source designated for showing a selected location's symbol layer icon.
            if (mapboxMap != null) {
                val style = mapboxMap!!.style
                if (style != null) {
                    val source = style.getSourceAs<GeoJsonSource>(geojsonSourceLayerId)
                    source?.setGeoJson(FeatureCollection.fromFeatures(arrayOf(Feature.fromJson(selectedCarmenFeature.toJson()))))

                    // Move map camera to the selected location
                    mapboxMap!!.animateCamera(CameraUpdateFactory.newCameraPosition(
                            CameraPosition.Builder()
                                    .target(LatLng((selectedCarmenFeature.geometry() as Point?)!!.latitude(),
                                            (selectedCarmenFeature.geometry() as Point?)!!.longitude()))
                                    .zoom(14.0)
                                    .build()), 4000)
                    performActionsOnSearchResult(selectedCarmenFeature)
                }
            }
        }
    }

    override fun deleteSymbolFromMap(symbol: Symbol) {
        symbolManager!!.delete(symbol)
    }

    protected fun deleteSymbolFromMapIfRed(symbol: Symbol?) {
        if (symbol != null && symbol.iconImage == RED_MARKER) {
            deleteSymbolFromMap(symbol)
        }
    }

    override fun createSymbolInMap(selectedCarmenFeature: CarmenFeature, iconImageString: String): Symbol {
        // This class uses "symbolManager" and requires it to be initialized.

        // Specify symbol size for the markers, to make them have approx. the same size
        val iconSize = specifyIconSize(iconImageString)
        counter++
        // Create a symbol at the specified location.
        val symbolOptions = SymbolOptions()
                .withLatLng(LatLng((selectedCarmenFeature.geometry() as Point?)!!.latitude(),
                        (selectedCarmenFeature.geometry() as Point?)!!.longitude()))
                .withIconImage(iconImageString) //                .withTextField(String.valueOf(counter))
                //                .withTextAnchor(Property.TEXT_ANCHOR_BOTTOM)
                .withTextColor("white")
                .withTextHaloColor("black")
                .withTextHaloWidth(1.0f)
                .withTextHaloBlur(0.25f)
                .withTextSize(20.0f)
                .withTextOffset(arrayOf(0f, -.05f))
                .withIconSize(iconSize)

        // Use the manager to draw the symbol
        return symbolManager!!.create(symbolOptions)
    }

    protected fun specifyIconSize(iconImageString: String): Float {
        if (iconImageString == RED_MARKER) {
            return SymbolsManagerInterface.RED_MARKER_ORIGINAL_SIZE
        }
        return if (iconImageString == BLUE_MARKER) {
            SymbolsManagerInterface.BLUE_MARKER_ORIGINAL_SIZE
        } else 1.0f
    }

    override fun changeIconSize(symbol: Symbol, size: Float) {
        symbol.iconSize = size
        symbolManager!!.update(symbol)
    }

    private fun resetIconSizeInBlueMarkers() {
        Timber.v("resetIconSizeInBlueMarkers() is called")
        for (i in 0 until symbolManager!!.annotations.size()) {
            Timber.v("Entered loop through all annotations, iteration: %s", i)
            val currentSymbol = symbolManager!!.annotations.valueAt(i)
            if (currentSymbol.iconImage == BLUE_MARKER) {
                changeIconSize(currentSymbol, SymbolsManagerInterface.BLUE_MARKER_ORIGINAL_SIZE)
                Timber.v("Just changed a blue marker size")
            }
        }
    }

    /**
     * Replaces the symbol's icon. Specifically, **RED_MARKER** to **BLUE_MARKER** and vice versa.
     * For the update to take place the **SymbolManager** is used.
     *
     * @param symbol The symbol to be updated.
     */
    override fun updateSymbolIconInMap(symbol: Symbol) {
        if (symbol != null) {
            val currentIconImageString = symbol.iconImage
            if (currentIconImageString == BLUE_MARKER) {
                symbol.iconImage = RED_MARKER
                symbol.iconSize = specifyIconSize(RED_MARKER)
                symbolManager!!.update(symbol)
            }
            if (currentIconImageString == RED_MARKER) {
                symbol.iconImage = BLUE_MARKER
                symbol.iconSize = specifyIconSize(BLUE_MARKER)
                symbolManager!!.update(symbol)
            }
        }
    }

    override fun getLatestSearchedSymbol(): Symbol {
        return latestSearchedLocationSymbol!!
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        permissionsManager!!.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onExplanationNeeded(permissionsToExplain: List<String>) {
        Toast.makeText(this, R.string.user_location_permission_explanation, Toast.LENGTH_LONG).show()
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            enableLocationComponent(mapboxMap!!.style!!)
        } else {
            Toast.makeText(this, R.string.user_location_permission_not_granted, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun addMapLongClickListener() {
        mapboxMap!!.addOnMapLongClickListener { point ->
            reverseGeocode(Point.fromLngLat(point.longitude, point.latitude))
            true
        }
    }

    /**
     * This method is used to reverse geocode where the user has dropped the marker.
     *
     * @param point The location to use for the search
     */
    private fun reverseGeocode(point: Point) {
        try {
            val client = MapboxGeocoding.builder()
                    .accessToken(getString(R.string.mapbox_access_token))
                    .query(Point.fromLngLat(point.longitude(), point.latitude()))
                    .geocodingTypes(GeocodingCriteria.TYPE_ADDRESS)
                    .build()
            client.enqueueCall(object : Callback<GeocodingResponse?> {
                override fun onResponse(call: Call<GeocodingResponse?>, response: Response<GeocodingResponse?>) {
                    if (response.body() != null) {
                        val results = response.body()!!.features()
                        if (results.size > 0) {
                            // If the geocoder returns a result, we take the first in the list.
                            val feature = results[0]
                            Timber.d("Successfully got a geocoding result, place name: %s", feature.placeName())
                            performActionsOnSearchResult(feature)
                        } else {
                            Timber.i("No results found for the clicked location")

                            // Print a toast message
                            mapboxMap!!.getStyle { Toast.makeText(this@MainActivity, "Could not find results for this location, please try again", Toast.LENGTH_SHORT).show() }
                        }
                    }
                }

                override fun onFailure(call: Call<GeocodingResponse?>, t: Throwable) {
                    Timber.e("Geocoding Failure: %s", t.message)

                    // Print a toast message
                    mapboxMap!!.getStyle { Toast.makeText(this@MainActivity, "Error requesting geocoding information, check your internet connection", Toast.LENGTH_SHORT).show() }
                }
            })
        } catch (servicesException: ServicesException) {
            Timber.e("Error geocoding: %s", servicesException.toString())
            servicesException.printStackTrace()
        }
    }

    private fun performActionsOnSearchResult(feature: CarmenFeature) {
        // Make sure we have no red marker leftover (which means the location was searched) from the previous search query
        deleteSymbolFromMapIfRed(latestSearchedLocationSymbol)

        // Reset all blue markers to their original size, so they do not look like they are selected
        resetIconSizeInBlueMarkers()

        // Create a symbol for that location and set it on the class' appropriate variable
        latestSearchedLocationSymbol = createSymbolInMap(feature, RED_MARKER)

        // Update place name in bottom sheet
        bottomSheetManager!!.changePlaceNameText(feature.placeName())

        // Notify the bottom sheet about its new currentCarmenFeature
        bottomSheetManager!!.setCurrentCarmenFeature(feature, (feature.geometry() as Point?)!!)

        // Refresh the state of bottom sheet's stopsButton
        bottomSheetManager!!.refreshStateOfStopsButton()

        // Reveal bottom sheet
        bottomSheetManager!!.changeBottomSheetState(BottomSheetBehavior.STATE_EXPANDED)
    }

    /**
     * This method converts each *CarmenFeature* in **stopsHashMap** to a *Point*.
     *
     * @return A list of *Point* objects that correspond to each element in **stopsHashMap**
     */
    override fun convertStopsToPoints(): List<Point> {
        val coordinates: MutableList<Point> = ArrayList()
        for (point in stopsHashMap.keys) {
            coordinates.add(point)
        }
        return coordinates
    }

    /**
     * This method will draw the optimized route (as a line) on the map.
     *
     * @param route The DirectionsRoute object which has the optimized route.
     */
    override fun drawOptimizedRoute(route: DirectionsRoute) {
        Timber.d("----- INSIDE drawOptimizedRoute ------")
        mapboxMap!!.getStyle { style ->
            Timber.d("----- INSIDE onStyleLoaded ------")
            val optimizedLineSource = style.getSourceAs<GeoJsonSource>("optimized-route-source-id")
            Timber.d("----- before if ------")
            if (optimizedLineSource != null) {
                Timber.d("----- inside if ------")
                optimizedLineSource.setGeoJson(FeatureCollection.fromFeature(Feature.fromGeometry(LineString.fromPolyline(route.geometry()!!, Constants.PRECISION_6))))
            }
        }
    }

    /**
     * This method will place a number on each location's Symbol depending on the order in which the route should be followed.
     *
     * @param waypoints A list of the optimized route's waypoints
     */
    override fun updateNumberInSymbolIcons(waypoints: List<OptimizationWaypoint>) {
//        mapboxMap.getStyle().getSourceAs().set
//        LongSparseArray<Symbol> allSymbols = symbolManager.getAnnotations().get().getLatLng()Lng()
//        for (Symbol symbol : allSymbols) {
//
//        }
    }

    override fun onStart() {
        super.onStart()
        mapView!!.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView!!.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView!!.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView!!.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView!!.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView!!.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView!!.onDestroy()
    }
}