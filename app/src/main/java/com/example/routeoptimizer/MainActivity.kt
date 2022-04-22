package com.example.routeoptimizer

//import com.example.routeoptimizer.BottomSheetManager
import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.routeoptimizer.databinding.ActivityMainBinding
import com.example.routeoptimizer.viewmodels.MainActivityViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.geocoding.v5.models.CarmenFeature
import com.mapbox.api.optimization.v1.models.OptimizationWaypoint
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.LocationComponentOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.OnSymbolClickListener
import com.mapbox.mapboxsdk.plugins.annotation.Symbol
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions
import com.mapbox.mapboxsdk.plugins.places.autocomplete.PlaceAutocomplete
import com.mapbox.mapboxsdk.plugins.places.autocomplete.model.PlaceOptions
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import timber.log.Timber
import timber.log.Timber.DebugTree


class MainActivity : AppCompatActivity(), OnMapReadyCallback, PermissionsListener, SymbolsManagerInterface, RouteOptimizationInterface {

    // variables for adding location layer
    lateinit var mapboxMap: MapboxMap

    private var permissionsManager: PermissionsManager = PermissionsManager(this)

    // For Places plugin (Search) functionality
    private lateinit var resultLauncher: ActivityResultLauncher<Intent>

    private val mainActivityViewModel: MainActivityViewModel by viewModels()

    private val geojsonSourceLayerId = "geojsonSourceLayerId"
    private val symbolIconId = "symbolIconId" // Maybe won't be used, should delete?
    private val RED_MARKER = "RED_MARKER" // Corresponds to locations that are searched but not added in stopsHashMap
    private val BLUE_MARKER = "BLUE_MARKER" // Corresponds to locations added in stopsHashMap
    private var latestSearchedLocationSymbol: Symbol? = null// Will contain symbolOptions for the latest user searched location's symbol (either searched or clicked)
    private lateinit var symbolManager: SymbolManager // SymbolManager to add/remove symbols on the map

    // variables for adding search functionality
    private val REQUEST_CODE_AUTOCOMPLETE = 1
    private val TEAL_COLOR = "#23D2BE" // For optimized route's line
    private val POLYLINE_WIDTH = 5f // For optimized route's line
    private val OPTIMIZED_ROUTE_SOURCE_ID = "optimized-route-source-id"
    private val OPTIMIZED_ROUTE_LAYER_ID = "optimized-route-layer-id"
    private var counter = 0 //DELETE THIS!

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("--onCreate()--")
        // Initialize Timber for logging
        if (BuildConfig.DEBUG) { Timber.plant(DebugTree()) }

        Mapbox.getInstance(this, getString(R.string.mapbox_access_token))

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.getMapAsync(this)

        initPlacesPluginFunctionality()
        initSearchFabClickListener()

        // Initialize the bottom sheet manager
        binding.bottomSheetView.initValues(this, mainActivityViewModel)
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        mapboxMap.setStyle(Style.MAPBOX_STREETS) { style ->
            enableLocationComponent(style)

            // Add the symbol layer icon to map and specify a name for each of the markers.
            style.addImage(RED_MARKER, BitmapFactory.decodeResource(this@MainActivity.resources, R.drawable.mapbox_marker_icon_default))
            style.addImage(BLUE_MARKER, BitmapFactory.decodeResource(this@MainActivity.resources, R.drawable.map_default_map_marker))

            // Create an empty GeoJSON source using the empty feature collection
            setUpSource(style)

            // Initialize symbol manager to add and remove icons on the map
            initializeSymbolManager(style)

            // Set up a layer to display the Symbols on the map
            initSymbolLayer(style)

            // Set up a layer to display the optimized route's line
            initOptimizedRouteLineLayer(style)

            addAnnotationClickListener()
            addMapLongClickListener()
        }
    }

    private fun enableLocationComponent(loadedMapStyle: Style) {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            // Create and customize the LocationComponent's options
            val customLocationComponentOptions = LocationComponentOptions.builder(this)
                .trackingGesturesManagement(true)
                .accuracyColor(ContextCompat.getColor(this, R.color.mapbox_blue))
                .build()

            val locationComponentActivationOptions =
                LocationComponentActivationOptions.builder(this, loadedMapStyle)
                    .locationComponentOptions(customLocationComponentOptions)
                    .build()

            //Get an instance of the LocationComponent and then adjust its settings
            mapboxMap.locationComponent.apply {
                // Activate the LocationComponent with option
                activateLocationComponent(locationComponentActivationOptions)

                //Enable to make the LocationComponent visible
                isLocationComponentEnabled = true

                //Set the LocationComponent's camera mode
                cameraMode = CameraMode.TRACKING

                // Set the LocationComponent's render mode
                renderMode = RenderMode.COMPASS
            }
        } else {
            permissionsManager = PermissionsManager(this)
            permissionsManager.requestLocationPermissions(this)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onExplanationNeeded(permissionsToExplain: List<String>) {
        Toast.makeText(this, R.string.user_location_permission_explanation, Toast.LENGTH_LONG).show()
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            enableLocationComponent(mapboxMap.style!!)
        } else {
            Toast.makeText(this, R.string.user_location_permission_not_granted, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun initPlacesPluginFunctionality() {
        resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data

                // Retrieve selected location's CarmenFeature
                val selectedCarmenFeature = PlaceAutocomplete.getPlace(data)

                // Create a new FeatureCollection and add a new Feature to it using selectedCarmenFeature above.
                // Then retrieve and update the source designated for showing a selected location's symbol layer icon.
                val style = mapboxMap.style
                if (style != null) {
                    val source = style.getSourceAs<GeoJsonSource>(geojsonSourceLayerId)
                    source?.setGeoJson(FeatureCollection.fromFeatures(
                        arrayOf(Feature.fromJson(selectedCarmenFeature.toJson()))))

                    // Move map camera to the selected location
                    mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(
                        CameraPosition.Builder()
                            .target(LatLng((selectedCarmenFeature.geometry() as Point).latitude(),
                                           (selectedCarmenFeature.geometry() as Point).longitude()))
                            .zoom(14.0)
                            .build()
                    ))

                    performActionsOnSearchResult(selectedCarmenFeature)
                }
            }
        }
    }

    private fun initSearchFabClickListener() {
        binding.fabLocationSearch.setOnClickListener {
            val intent = PlaceAutocomplete.IntentBuilder()
                .accessToken(Mapbox.getAccessToken() ?: getString(R.string.mapbox_access_token))
                .placeOptions(PlaceOptions.builder()
                    .backgroundColor(Color.parseColor("#EEEEEE"))
                    .limit(10)
                    .build(PlaceOptions.MODE_CARDS))
                .build(this@MainActivity)
            resultLauncher.launch(intent)
        }
//        }
    }


    private fun setUpSource(loadedMapStyle: Style) {
        loadedMapStyle.addSource(GeoJsonSource(geojsonSourceLayerId))
}

    private fun initializeSymbolManager(style: Style) {
        symbolManager = SymbolManager(binding.mapView, mapboxMap, style)

        // Set non-data-driven properties
        symbolManager.iconAllowOverlap = true
        symbolManager.textAllowOverlap = true
    }
//
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
        loadedMapStyle.addSource(GeoJsonSource(OPTIMIZED_ROUTE_SOURCE_ID))
        loadedMapStyle.addLayerBelow(
            LineLayer(OPTIMIZED_ROUTE_LAYER_ID, OPTIMIZED_ROUTE_SOURCE_ID)
                .withProperties(
                        PropertyFactory.lineColor(Color.parseColor(TEAL_COLOR)),
                        PropertyFactory.lineWidth(POLYLINE_WIDTH)
                ), symbolManager.layerId)
    }

//    /**
//     * Adds a listener on **every** symbol (also called *annotation*) that will be created.
//     */
    private fun addAnnotationClickListener() {
        symbolManager.addClickListener(OnSymbolClickListener { symbol ->
            // Currently there is functionality only for clicking blue markers, clicking on a red symbol will have no effect.

            if (symbol.iconImage == BLUE_MARKER) {
                // If the previously displayed symbol was red, delete it from the map
                if (latestSearchedLocationSymbol?.iconImage == RED_MARKER)
                    deleteSymbolFromMap(latestSearchedLocationSymbol!!)

                resetIconSizeInBlueMarkers()

                // Expand the symbol size of the currently displayed blue marker
                changeIconSize(symbol, SymbolsManagerInterface.BLUE_MARKER_EXPANDED_SIZE)

                // The new symbol now becomes the latest one we searched
                latestSearchedLocationSymbol = symbol

                // Get CarmenFeature from geometry. The symbol was blue so the location exists in stopsHashMap
                val carmenFeatureOfSelectedSymbol = DataRepository.stopsHashMap[symbol.geometry]
                binding.bottomSheetView.setCurrentCarmenFeature(carmenFeatureOfSelectedSymbol!!, symbol.geometry)

                // Update place name in bottom sheet
                binding.bottomSheetView.changePlaceNameText(carmenFeatureOfSelectedSymbol.placeName())

                // Update the text of stopsButton
                binding.bottomSheetView.refreshStateOfStopsButton(DataRepository.stopsHashMap)

                // Reveal bottom sheet. Will work even if it is already in "STATE_EXPANDED"
                binding.bottomSheetView.changeBottomSheetState(BottomSheetBehavior.STATE_EXPANDED)
            }
            true
        })
    }
//
//
    override fun deleteSymbolFromMap(symbol: Symbol) {
        symbolManager.delete(symbol)
    }
//
//
    override fun createSymbolInMap(selectedCarmenFeature: CarmenFeature, iconImageString: String): Symbol {
        // This class uses "symbolManager" and requires it to be initialized.

        // Specify symbol size for the markers, to make them have approx. the same size
        val iconSize = specifyIconSize(iconImageString)
        counter++ // FOR DEBUG, DELETE!
        // Create a symbol at the specified location.
        val symbolOptions = SymbolOptions()
                .withLatLng(LatLng((selectedCarmenFeature.geometry() as Point?)!!.latitude(),
                        (selectedCarmenFeature.geometry() as Point?)!!.longitude()))
                .withIconImage(iconImageString)
                //                .withTextField(String.valueOf(counter))
                //                .withTextAnchor(Property.TEXT_ANCHOR_BOTTOM)
                //.withTextColor("white")
                //.withTextHaloColor("black")
                //.withTextHaloWidth(1.0f)
                //.withTextHaloBlur(0.25f)
                //.withTextSize(20.0f)
                //.withTextOffset(arrayOf(0f, -.05f))
                .withIconSize(iconSize)

        // Use the manager to draw the symbol
        return symbolManager.create(symbolOptions)
    }
//
    private fun specifyIconSize(iconImageString: String): Float {
        if (iconImageString == RED_MARKER) {
            return SymbolsManagerInterface.RED_MARKER_ORIGINAL_SIZE
        }
        return if (iconImageString == BLUE_MARKER) {
            SymbolsManagerInterface.BLUE_MARKER_ORIGINAL_SIZE
        } else 1.0f
    }
//
    override fun changeIconSize(symbol: Symbol, size: Float) {
        symbol.iconSize = size
        symbolManager.update(symbol)
    }
//
    private fun resetIconSizeInBlueMarkers() {
        for (i in 0 until symbolManager.annotations.size()) {
            val currentSymbol = symbolManager.annotations.valueAt(i)
            if (currentSymbol.iconImage == BLUE_MARKER) {
                changeIconSize(currentSymbol, SymbolsManagerInterface.BLUE_MARKER_ORIGINAL_SIZE)
            }
        }
    }

    /**
     * Replaces the symbol's icon. Specifically, **RED_MARKER** to **BLUE_MARKER** and vice versa.
     * For the update to take place the **SymbolManager** is used.
     *
     * @param symbol The symbol to be updated.
     */
    override fun switchSymbolIconInMap(symbol: Symbol) {
        val currentIconImageString = symbol.iconImage
        if (currentIconImageString == BLUE_MARKER) {
            symbol.iconImage = RED_MARKER
            symbol.iconSize = specifyIconSize(RED_MARKER)
            symbolManager.update(symbol)
        }
        if (currentIconImageString == RED_MARKER) {
            symbol.iconImage = BLUE_MARKER
            symbol.iconSize = specifyIconSize(BLUE_MARKER)
            symbolManager.update(symbol)
        }
    }


    override fun getLatestSearchedSymbol(): Symbol? {
        return latestSearchedLocationSymbol
    }

    fun addMapLongClickListener() {
        mapboxMap.addOnMapLongClickListener { point ->
//            reverseGeocode(Point.fromLngLat(point.longitude, point.latitude))
            ReverseGeocoderUtil.reverseGeocode(this, Point.fromLngLat(point.longitude, point.latitude))
            true
        }
    }

    fun performActionsOnSearchResult(feature: CarmenFeature) {
        // Make sure we have no red marker leftover (which means the location was searched) from the previous search query
        if (latestSearchedLocationSymbol?.iconImage == RED_MARKER) {
            deleteSymbolFromMap(latestSearchedLocationSymbol!!)
        }

        // Reset all blue markers to their original size, so they do not look like they are selected
        resetIconSizeInBlueMarkers()

        // Create a symbol for that location and set it as the latest searched location symbol
        latestSearchedLocationSymbol = createSymbolInMap(feature, RED_MARKER)

        // Update place name in bottom sheet
        binding.bottomSheetView.changePlaceNameText(feature.placeName())

        // Notify the bottom sheet about its new currentCarmenFeature
        binding.bottomSheetView.setCurrentCarmenFeature(feature, (feature.geometry() as Point))

        // Refresh the state of bottom sheet's stopsButton
        binding.bottomSheetView.refreshStateOfStopsButton(DataRepository.stopsHashMap)

        // Reveal bottom sheet
        binding.bottomSheetView.changeBottomSheetState(BottomSheetBehavior.STATE_EXPANDED)
    }


    /**
     * This method will draw the optimized route (as a line) on the map.
     *
     * @param route The DirectionsRoute object which has the optimized route.
     */
    override fun drawOptimizedRoute(route: DirectionsRoute) {
        Timber.d("----- INSIDE drawOptimizedRoute ------")
        mapboxMap.getStyle { style ->
            Timber.d("----- INSIDE onStyleLoaded ------")
            val optimizedLineSource = style.getSourceAs<GeoJsonSource>(OPTIMIZED_ROUTE_SOURCE_ID)
            Timber.d("----- before if ------")
            if (optimizedLineSource != null) {
                Timber.d("----- inside if ------")
                optimizedLineSource.setGeoJson(FeatureCollection.fromFeature(Feature.fromGeometry(
                    LineString.fromPolyline(route.geometry()!!, Constants.PRECISION_6))))
            }
        }
    }
//
//    /**
//     * This method will place a number on each location's Symbol depending on the order in which the route should be followed.
//     *
//     * @param waypoints A list of the optimized route's waypoints
//     */
//    override fun updateNumberInSymbolIcons(waypoints: List<OptimizationWaypoint>) {
////        mapboxMap.getStyle().getSourceAs().set
////        LongSparseArray<Symbol> allSymbols = symbolManager.getAnnotations().get().getLatLng()Lng()
////        for (Symbol symbol : allSymbols) {
////
////        }
//    }
    override fun updateNumberInSymbolIcons(waypoints: List<OptimizationWaypoint>) {
        // Not yet implemented!
    }

    override fun onStart() {
        super.onStart()
        binding.mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        binding.mapView.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.mapView.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.mapView.onDestroy()
    }
}