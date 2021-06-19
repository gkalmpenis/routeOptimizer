package com.example.routeoptimizer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
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
import com.mapbox.mapboxsdk.plugins.annotation.Symbol;
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager;
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions;
import com.mapbox.mapboxsdk.plugins.places.autocomplete.PlaceAutocomplete;
import com.mapbox.mapboxsdk.plugins.places.autocomplete.model.PlaceOptions;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconOffset;

//public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, MapboxMap.OnMapClickListener, PermissionsListener {
public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, PermissionsListener {

    // variables for adding location layer
    private MapView mapView;
    private MapboxMap mapboxMap;

    // variables for adding location layer
    private PermissionsManager permissionsManager;
    private LocationComponent locationComponent;

    // variables for adding search functionality
    private static final int REQUEST_CODE_AUTOCOMPLETE = 1;
    private final String geojsonSourceLayerId = "geojsonSourceLayerId";
    private final String symbolIconId = "sumbolIconId"; // Maybe won't be used, should delete?
    private final String RED_MARKER = "RED_MARKER";
    private final String BLUE_MARKER = "BLUE_MARKER";
    private Symbol latestSearchedLocationSymbol; // Will contain symbolOptions for the latest user searched location's symbol
    private Symbol latestAddedAsStopSymbol; // Will contain symbolOptions for the symbol that was used in the latest stop addition
    SymbolManager symbolManager; // SymbolManager to add symbol on the map


    // variables for manipulating bottomSheet
    private View bottomSheet;
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private TextView placeNameTextView;

    // variables for manipulating addAsStop button
    private Button addAsStopButton;
    private final String ADD_AS_STOP = "Add as stop"; // Should be exactly the same as the text in R.string.add_stop_txt
    private final String REMOVE_FROM_STOPS = "Remove from stops"; // Should be exactly the same as the text in R.string.remove_stop_txt
    private TextView currentStopsCounterTextView;
    private Set<CarmenFeature> setOfStops = new HashSet<>(); // We do not use LinkedHashSet or TreeSet because we don't care about the order

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Mapbox.getInstance(this, getString(R.string.mapbox_access_token));

        setContentView(R.layout.activity_main);

        mapView = (MapView) findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        // BottomSheet manipulation
        bottomSheet = findViewById(R.id.bottomSheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN); // Do not reveal bottom sheet on creation of the application.
        bottomSheet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Show or hide bottomSheet when user clicks anywhere on it
                if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                } else {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                }
            }
        });

        placeNameTextView = findViewById(R.id.placeNameTextView);
        addAsStopButton = findViewById(R.id.StopsButton);
        currentStopsCounterTextView = findViewById(R.id.currentStopsCounterTextView);
    }

    @Override
    public void onMapReady(@NonNull final MapboxMap mapboxMap) {
        this.mapboxMap = mapboxMap;
        mapboxMap.setStyle(Style.MAPBOX_STREETS, new Style.OnStyleLoaded() {
            @Override
            public void onStyleLoaded(@NonNull Style style) {
                enableLocationComponent(style);
                initSearchFab(); // Initiates location search

                // Add the symbol layer icon to map and specify a name for each of the markers.
                style.addImage(RED_MARKER, BitmapFactory.decodeResource(MainActivity.this.getResources(), R.drawable.mapbox_marker_icon_default));
                style.addImage(BLUE_MARKER, BitmapFactory.decodeResource(MainActivity.this.getResources(), R.drawable.map_default_map_marker));

                // Create an empty GeoJSON source using the empty feature collection
                setUpSource(style);

                // Initialize symbol manager to add and remove icons on the map
                initializeSymbolManager(style);

                // Set up a new symbol layer for displaying the searched location's feature coordinates - SEEMS NON FUNCTIONAL, DELETE?
                //setupLayer(style);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_AUTOCOMPLETE) {
            // Retrieve selected location's CarmenFeature
            CarmenFeature selectedCarmenFeature = PlaceAutocomplete.getPlace(data);

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

                    // Update place name in bottom sheet
                    placeNameTextView.setText(selectedCarmenFeature.placeName());
                    // Reveal bottom sheet
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);

                    removeSymbolFromMap(latestSearchedLocationSymbol);
                    latestSearchedLocationSymbol = addSymbolInMap(selectedCarmenFeature, RED_MARKER);

                    refreshStopsButtonState();
                    manipulateAddAsStopButton(style, selectedCarmenFeature);
                }
            }
        }
    }

    private void removeSymbolFromMap(Symbol symbol) {
        if (symbol == null) { /*Do nothing*/ }
        else { symbolManager.delete(symbol); }
    }

    private Symbol addSymbolInMap(@NonNull CarmenFeature selectedCarmenFeature, @NonNull String iconImageString) {
        // This class uses "symbolManager" and requires it to be initialized.

        // Specify symbol size for the markers
        float iconSize;
        switch (iconImageString) {
            case BLUE_MARKER:
                iconSize = 0.74f; break;
            default:
                iconSize = 1.0f;
        }

        // Create a symbol at the specified location.
        SymbolOptions symbolOptions = new SymbolOptions()
                //.withLatLng(new LatLng(6.687337, 0.381457)) //for educational purposes - DELETE AFTERWARDS
                .withLatLng(new LatLng(((Point) selectedCarmenFeature.geometry()).latitude(),
                        ((Point) selectedCarmenFeature.geometry()).longitude()))
                .withIconImage(iconImageString)
                .withIconSize(iconSize);

        // Use the manager to draw the symbol
        Symbol createdSymbol = symbolManager.create(symbolOptions);
        return createdSymbol;
    }

    private void refreshStopsButtonState() {
        // So far this only changes the text of the button, later it would be good have a different way of understanding the state of the button
        // eg. have a variable for the button state, and if it should be at the "initial" state then text should be "add as stop",
        // if it should be at the "stop selection" state then text should be "remove from stop".
        addAsStopButton.setText(ADD_AS_STOP);
    }

    private void manipulateAddAsStopButton(@NonNull Style style, @NonNull CarmenFeature selectedCarmenFeature) {
        addAsStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button button = (Button) v;
                String buttonText = (String) button.getText();

                switch (buttonText) {
                    case ADD_AS_STOP:
                        // Add the selectedCarmenFeature in the HashSet
                        boolean stopAddedInSet = setOfStops.add(selectedCarmenFeature); // If element already exists in Set it will not be added

                        if (stopAddedInSet) {
                            // Update the current stops counter
                            currentStopsCounterTextView.setText(String.valueOf(setOfStops.size()));

                            // Remove the red marker from that location
                            removeSymbolFromMap(latestSearchedLocationSymbol);

                            // Place a blue marker on that location
                            latestAddedAsStopSymbol = addSymbolInMap(selectedCarmenFeature, BLUE_MARKER);

                            // Change the button's text
                            button.setText(REMOVE_FROM_STOPS);
                        }
                        break;
                    case REMOVE_FROM_STOPS:
                        // Remove the selectedCarmenFeature from the HashSet
                        boolean stopRemovedFromSet = setOfStops.remove(selectedCarmenFeature);

                        if (stopRemovedFromSet) {
                            // Update the current stops counter
                            currentStopsCounterTextView.setText(String.valueOf(setOfStops.size()));

                            // Remove the marker from that location
                            removeSymbolFromMap(latestAddedAsStopSymbol); // !!!! This need to be changed !!!!!!! Ama thelei o xrhsths na clickarei se location kai
                                                                            // na to svhseis tha prepei na pairneis ta symbol options tou kai na ta dineis edw, den einai lush
                                                                            // na pairneis panta to "latestAddedAsStopSymbol"

                            // Change the button's text
                            button.setText(ADD_AS_STOP);
                        }
                        break;
                    default:
                        break;
                }


            }
        });
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