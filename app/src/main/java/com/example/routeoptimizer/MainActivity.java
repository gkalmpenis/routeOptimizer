package com.example.routeoptimizer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.api.geocoding.v5.models.CarmenFeature;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.plugins.annotation.OnSymbolClickListener;
import com.mapbox.mapboxsdk.plugins.annotation.Symbol;
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager;
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions;
import com.mapbox.mapboxsdk.plugins.places.autocomplete.PlaceAutocomplete;
import com.mapbox.mapboxsdk.plugins.places.autocomplete.model.PlaceOptions;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import java.util.HashMap;
import java.util.List;

import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconOffset;

//public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, MapboxMap.OnMapClickListener, PermissionsListener {
public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, PermissionsListener, SymbolsManagerInterface {

    // variables for adding location layer
    private MapView mapView;
    private MapboxMap mapboxMap;

    // variables for adding location layer
    private PermissionsManager permissionsManager;
    private LocationComponent locationComponent;

    // variables for adding search functionality
    private static final int REQUEST_CODE_AUTOCOMPLETE = 1;
    private final String geojsonSourceLayerId = "geojsonSourceLayerId";
    private final String symbolIconId = "symbolIconId"; // Maybe won't be used, should delete?
    private final String RED_MARKER = "RED_MARKER"; // Corresponds to locations that are searched but not added in stopsHashMap
    private final String BLUE_MARKER = "BLUE_MARKER"; // Corresponds to locations added in stopsHashMap
    private Symbol latestSearchedLocationSymbol; // Will contain symbolOptions for the latest user searched location's symbol (either searched or clicked)
    private SymbolManager symbolManager; // SymbolManager to add/remove symbols on the map

    // Variable to manipulate the bottom sheet
    private BottomSheetManager bottomSheetManager;

    // HashMap that will contain locations that user adds as stop
    protected static HashMap<Point, CarmenFeature> stopsHashMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Mapbox.getInstance(this, getString(R.string.mapbox_access_token));

        setContentView(R.layout.activity_main);

        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        // Initialize unique BottomSheetManager instance
        bottomSheetManager = BottomSheetManager.getInstance();
        bottomSheetManager.setSymbolsManagerInterface(this);
        bottomSheetManager.setBottomSheetReference(findViewById(R.id.bottomSheet));
        bottomSheetManager.initializeBottomSheetBehavior();
        bottomSheetManager.setPlaceNameReference(findViewById(R.id.placeNameTextView));
        bottomSheetManager.setStopsButtonReference(findViewById(R.id.stopsButton));
        bottomSheetManager.setCurrentStopsCounterReference(findViewById(R.id.currentStopsCounterTextView));

        bottomSheetManager.changeBottomSheetState(BottomSheetBehavior.STATE_HIDDEN); // Do not reveal bottom sheet on creation of the application.
        bottomSheetManager.setOnClickListener();
        bottomSheetManager.addStopsButtonOnClickListener();
    }

    @Override
    public void onMapReady(@NonNull final MapboxMap mapboxMap) {
        this.mapboxMap = mapboxMap;
        mapboxMap.setStyle(Style.MAPBOX_STREETS, new Style.OnStyleLoaded() {
            @Override
            public void onStyleLoaded(@NonNull Style style) {
                enableLocationComponent(style);
                initSearchFab(); // Initiates location search floating action button

                // Add the symbol layer icon to map and specify a name for each of the markers.
                style.addImage(RED_MARKER, BitmapFactory.decodeResource(MainActivity.this.getResources(), R.drawable.mapbox_marker_icon_default));
                style.addImage(BLUE_MARKER, BitmapFactory.decodeResource(MainActivity.this.getResources(), R.drawable.map_default_map_marker));

                // Create an empty GeoJSON source using the empty feature collection
                setUpSource(style);

                // Initialize symbol manager to add and remove icons on the map
                initializeSymbolManager(style);

                // Set up a new symbol layer for displaying the searched location's feature coordinates - SEEMS NON FUNCTIONAL, DELETE?
                //setupLayer(style);

                addAnnotationClickListener();
            }
        });
    }

    @SuppressWarnings({"MissingPermission"})
    private void enableLocationComponent(@NonNull Style loadedMapStyle) {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            // Activate the MapboxMap LocationComponent to show user location
            // Adding in LocationComponentOptions is also an optional parameter
            locationComponent = mapboxMap.getLocationComponent();
            locationComponent.activateLocationComponent(this, loadedMapStyle);
            locationComponent.setLocationComponentEnabled(true);
            locationComponent.setRenderMode(RenderMode.COMPASS);
            // Set the component's camera mode
            locationComponent.setCameraMode(CameraMode.TRACKING);
        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }

    private void initSearchFab() {
        findViewById(R.id.fab_location_search).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new PlaceAutocomplete.IntentBuilder()
                        .accessToken(Mapbox.getAccessToken() != null ? Mapbox.getAccessToken() : getString(R.string.mapbox_access_token))
                        .placeOptions(PlaceOptions.builder()
                                .backgroundColor(Color.parseColor("#EEEEEE"))
                                .limit(10)
                                .build(PlaceOptions.MODE_CARDS))
                        .build(MainActivity.this);
                startActivityForResult(intent, REQUEST_CODE_AUTOCOMPLETE);
            }
        });
    }

    private void setUpSource(@NonNull Style loadedMapStyle) {
        loadedMapStyle.addSource(new
                GeoJsonSource(geojsonSourceLayerId));
    }

    private void initializeSymbolManager (@NonNull Style style) {
        symbolManager = new SymbolManager (mapView, mapboxMap, style);

        // Set non-data-driven properties
        symbolManager.setIconAllowOverlap(true);
        symbolManager.setTextAllowOverlap(true);
    }

    private void setupLayer(@NonNull Style loadedMapStyle) {
        loadedMapStyle.addLayer(new SymbolLayer("SYMBOL_LAYER_ID",
                geojsonSourceLayerId).withProperties(
                        iconImage(symbolIconId),
                        iconOffset(new Float[] {0f, -8f})
        ));
    }

    /**
     * Adds a listener on <b>every</b> symbol (also called <i>annotation</i>) that will be created.
     */
    private void addAnnotationClickListener() {
        symbolManager.addClickListener(new OnSymbolClickListener() {
            @Override
            public boolean onAnnotationClick(Symbol symbol) {
                deleteSymbolFromMapIfRed(latestSearchedLocationSymbol);

                latestSearchedLocationSymbol = symbol;

                CarmenFeature carmenFeatureOfSelectedSymbol = stopsHashMap.get(symbol.getGeometry()); // Get CarmenFeature from geometry

                bottomSheetManager.setCurrentCarmenFeature(carmenFeatureOfSelectedSymbol, symbol.getGeometry());

                // Update place name in bottom sheet
                bottomSheetManager.changePlaceNameText(carmenFeatureOfSelectedSymbol.placeName());

                // Update the text of stopsButton
                bottomSheetManager.refreshStateOfStopsButton();

                // Reveal bottom sheet. Will work even if it is already in "STATE_EXPANDED"
                bottomSheetManager.changeBottomSheetState(BottomSheetBehavior.STATE_EXPANDED);

                return true;
            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_AUTOCOMPLETE) {
            // Retrieve selected location's CarmenFeature
            CarmenFeature selectedCarmenFeature = PlaceAutocomplete.getPlace(data);
            Point selectedCarmenFeatureGeometry = (Point) selectedCarmenFeature.geometry();

            // Create a new FeatureCollection and add a new Feature to it using selectedCarmenFeature above.
            // Then retrieve and update the source designated for showing a selected location's symbol layer icon.

            if (mapboxMap != null) {
                Style style = mapboxMap.getStyle();
                if (style != null) {
                    GeoJsonSource source = style.getSourceAs(geojsonSourceLayerId);
                    if (source != null) {
                        source.setGeoJson(FeatureCollection.fromFeatures(
                                new Feature[]{Feature.fromJson(selectedCarmenFeature.toJson())}));
                    }

                    // Move map camera to the selected location
                    mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(
                            new CameraPosition.Builder()
                                    .target(new LatLng(((Point) selectedCarmenFeature.geometry()).latitude(),
                                            ((Point) selectedCarmenFeature.geometry()).longitude()))
                                    .zoom(14)
                                    .build()), 4000);

                    // Make sure we have no red marker leftover (which means the location was searched) from the previous search query
                    deleteSymbolFromMapIfRed(latestSearchedLocationSymbol);

                    // Create a symbol for that location and set it on the class' appropriate variable
                    latestSearchedLocationSymbol = createSymbolInMap(selectedCarmenFeature, RED_MARKER);

                    // Update place name in bottom sheet
                    bottomSheetManager.changePlaceNameText(selectedCarmenFeature.placeName());

                    // Notify the bottom sheet about its new currentCarmenFeature
                    bottomSheetManager.setCurrentCarmenFeature(selectedCarmenFeature, selectedCarmenFeatureGeometry);

                    // Refresh the state of bottom sheet's stopsButton
                    bottomSheetManager.refreshStateOfStopsButton();

                    // Reveal bottom sheet
                    bottomSheetManager.changeBottomSheetState(BottomSheetBehavior.STATE_EXPANDED);
                }
            }
        }
    }

    @Override
    public void deleteSymbolFromMap(Symbol symbol) {
        symbolManager.delete(symbol);
    }

    protected void deleteSymbolFromMapIfRed(Symbol symbol) {
        if ( (symbol != null) && (symbol.getIconImage().equals(RED_MARKER)) ) {
            deleteSymbolFromMap(symbol);
        }
    }

    @Override
    public Symbol createSymbolInMap(@NonNull CarmenFeature selectedCarmenFeature, @NonNull String iconImageString) {
        // This class uses "symbolManager" and requires it to be initialized.

        // Specify symbol size for the markers, to make them have approx. the same size
        float iconSize = specifyIconSize(iconImageString);

        // Create a symbol at the specified location.
        SymbolOptions symbolOptions = new SymbolOptions()
                .withLatLng(new LatLng(((Point) selectedCarmenFeature.geometry()).latitude(),
                        ((Point) selectedCarmenFeature.geometry()).longitude()))
                .withIconImage(iconImageString)
                .withIconSize(iconSize);

        // Use the manager to draw the symbol
        Symbol createdSymbol = symbolManager.create(symbolOptions);
        return createdSymbol;
    }

    protected float specifyIconSize (@NonNull String iconImageString) {
        if ( iconImageString.equals(RED_MARKER) ) {
            return 1.0f;
        }
        if ( iconImageString.equals(BLUE_MARKER) ) {
            return 0.74f;
        }
        return 1.0f;
    }

    /**
     * Replaces the symbol's icon. Specifically, <b>RED_MARKER</b> to <b>BLUE_MARKER</b> and vice versa.
     */
    @Override
    public void updateSymbolIconInMap(Symbol symbol) {
        if (symbol != null ) {
            String currentIconImageString = symbol.getIconImage();

            if ( currentIconImageString.equals(BLUE_MARKER) ) {
                symbol.setIconImage(RED_MARKER);
                symbol.setIconSize(specifyIconSize(RED_MARKER));
                symbolManager.update(symbol);
            }
            if ( currentIconImageString.equals(RED_MARKER) ) {
                symbol.setIconImage(BLUE_MARKER);
                symbol.setIconSize(specifyIconSize(BLUE_MARKER));
                symbolManager.update(symbol);
            }
        }
    }

    @Override
    public Symbol getLatestSearchedSymbol() {
        return this.latestSearchedLocationSymbol;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(this, R.string.user_location_permission_explanation, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            enableLocationComponent(mapboxMap.getStyle());
        } else {
            Toast.makeText(this, R.string.user_location_permission_not_granted, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }
}