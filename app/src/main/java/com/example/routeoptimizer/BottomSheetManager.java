package com.example.routeoptimizer;

import android.content.res.Resources;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.geocoding.v5.models.CarmenFeature;
import com.mapbox.api.optimization.v1.MapboxOptimization;
import com.mapbox.api.optimization.v1.models.OptimizationResponse;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

public class BottomSheetManager {
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

    private static BottomSheetManager bottomSheetManager;

    // A private Constructor prevents any other class from instantiating.
    private BottomSheetManager() {
    }

    protected static BottomSheetManager getInstance() {
        if (bottomSheetManager == null) { // If there is no instance available, create a new one
            bottomSheetManager = new BottomSheetManager();
        }

        return bottomSheetManager;
    }

    private View bottomSheet;
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private TextView placeNameTextView;
    private Button stopsButton;
    private TextView currentStopsCounterTextView;
    private Button optimizeButton;
    private CarmenFeature currentCarmenFeature;
    private Point currentCarmenFeatureGeometry;
    private DirectionsRoute optimizedRoute;

    protected enum StopsButtonState { ADD_NEW_STOP, REMOVE_A_STOP }
    private final String ADD_AS_STOP = "Add as stop"; // Should be exactly the same as the text in R.string.add_stop_txt
    private final String REMOVE_FROM_STOPS = "Remove from stops"; // Should be exactly the same as the text in R.string.remove_stop_txt

    private SymbolsManagerInterface symbolsManagerInterface; // To perform create/update/delete actions on map symbols
    private RouteOptimizationInterface routeOptimizationInterface;
    private MapboxOptimization optimizedClient;

    /* Methods protected by singleton-ness */
    protected void setBottomSheetReference(View bottomSheet) { this.bottomSheet = bottomSheet; }
    protected void initializeBottomSheetBehavior() { this.bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet); }
    protected void changeBottomSheetState(int newState) { this.bottomSheetBehavior.setState(newState); }
    protected void setPlaceNameReference(TextView placeNameTextView) { this.placeNameTextView = placeNameTextView; }
    protected void setStopsButtonReference(Button stopsButton) { this.stopsButton = stopsButton; }
    protected void setCurrentStopsCounterReference(TextView currentStopsCounterTextView) { this.currentStopsCounterTextView = currentStopsCounterTextView; }
    protected void setOptimizeButtonReference(Button optimizeButton) { this.optimizeButton = optimizeButton; }
    protected void setSymbolsManagerInterface(SymbolsManagerInterface symbolsManagerInterface) { this.symbolsManagerInterface = symbolsManagerInterface; }
    protected void setRouteOptimizationInterface(RouteOptimizationInterface routeOptimizationInterface) { this.routeOptimizationInterface = routeOptimizationInterface; }

    /**
     * Adds an onClick listener that will show/hide the bottomSheet when user clicks anywhere on it
     */
    protected void setOnClickListener() {
        bottomSheet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                } else {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                }
            }
        });
    }

    protected void changePlaceNameText(String newText) {
        this.placeNameTextView.setText(newText);
    }

    protected void changeStateOfStopsButton(StopsButtonState state) {
        // So far this method only changes the text of the button
        if (state == StopsButtonState.ADD_NEW_STOP) { stopsButton.setText(ADD_AS_STOP); }
        if (state == StopsButtonState.REMOVE_A_STOP) { stopsButton.setText(REMOVE_FROM_STOPS); }
    }

    protected void setCurrentCarmenFeature(@NonNull CarmenFeature currentCarmenFeature, @NonNull Point currentCarmenFeatureGeometry) {
        this.currentCarmenFeature = currentCarmenFeature;
        this.currentCarmenFeatureGeometry = currentCarmenFeatureGeometry;
    }

    /**
     * Checks if the <i>stopsHashMap</i> contains the <i>currentCarmenFeature</i> (which was either searched or clicked)
     * and updates the state of the <i>stopsButton</i> accordingly
     */
    protected void refreshStateOfStopsButton() {
        if (MainActivity.stopsHashMap.containsKey(currentCarmenFeatureGeometry)) {
            changeStateOfStopsButton(StopsButtonState.REMOVE_A_STOP);
        } else {
            changeStateOfStopsButton(StopsButtonState.ADD_NEW_STOP);
        }
    }

    protected void addStopsButtonOnClickListener() {
        stopsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button button = (Button) v;
                String buttonText = (String) button.getText();

                switch (buttonText) {
                    case ADD_AS_STOP:
                        // Add the manager's currently shown CarmenFeature in MainActivity's HashMap
                        MainActivity.stopsHashMap.put(currentCarmenFeatureGeometry, currentCarmenFeature);

                        // Update the current stops counter
                        currentStopsCounterTextView.setText(String.valueOf(MainActivity.stopsHashMap.size()));

                        decideOptimizeButtonVisibility();

                        // Update the marker in that location
                        symbolsManagerInterface.updateSymbolIconInMap(symbolsManagerInterface.getLatestSearchedSymbol());
                        symbolsManagerInterface.changeIconSize(symbolsManagerInterface.getLatestSearchedSymbol(), SymbolsManagerInterface.BLUE_MARKER_EXPANDED_SIZE);

                        // Change the stopsButton's text
                        changeStateOfStopsButton(StopsButtonState.REMOVE_A_STOP);
                        break;
                    case REMOVE_FROM_STOPS:
                        // Remove the selectedCarmenFeature from the HashMap
                        MainActivity.stopsHashMap.remove(currentCarmenFeatureGeometry);

                        // Update the current stops counter
                        currentStopsCounterTextView.setText(String.valueOf(MainActivity.stopsHashMap.size()));

                        decideOptimizeButtonVisibility();

                        // Update the marker in that location
                        symbolsManagerInterface.updateSymbolIconInMap(symbolsManagerInterface.getLatestSearchedSymbol());

                        // Change the stopsButton's text
                        changeStateOfStopsButton(StopsButtonState.ADD_NEW_STOP);
                        break;
                    default:
                        break;
                }
            }
        });
    }

    protected void decideOptimizeButtonVisibility() {
        if ( MainActivity.stopsHashMap.size() < 2 ) {
            optimizeButton.setVisibility(View.GONE);
        } else {
            optimizeButton.setVisibility(View.VISIBLE);
        }
    }

    protected void addOptimizeButtonOnClickListener() {
        optimizeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ( MainActivity.stopsHashMap.size() > 12 ) {
                    // TODO: This doesn't work if it is the 2nd+ time we click "optimize", provide a fix.
                    // Maybe because onClick listener is implemented only one time?
                    Toast.makeText(bottomSheet.getContext(), R.string.only_twelve_stops_allowed, Toast.LENGTH_LONG).show();
                }
                List<Point> coordinates = routeOptimizationInterface.convertStopsToPoints();
//                Point firstPoint = coordinates.get(0);                      // The list of coordinates has at least two items because "optimizeButton" appears after two items have been inserted in stopsHashMap,
//                Point lastPoint = coordinates.get(coordinates.size() - 1);  // so we can safely obtain a firstPoint and lastPoint from them.

                Timber.d("----- BEFORE BUILD ------");

                // Build the optimized route
                optimizedClient = MapboxOptimization.builder()
                        .source(DirectionsCriteria.SOURCE_FIRST)
                        .destination(DirectionsCriteria.DESTINATION_LAST)
                        .coordinates(coordinates)
                        .overview(DirectionsCriteria.OVERVIEW_FULL)
                        .profile(DirectionsCriteria.PROFILE_DRIVING)
                        .accessToken(bottomSheet.getResources().getString(R.string.mapbox_access_token))
                        .build();

                Timber.d("----- AFTER BUILD ------");

                optimizedClient.enqueueCall(new Callback<OptimizationResponse>() {
                    @Override
                    public void onResponse(Call<OptimizationResponse> call, Response<OptimizationResponse> response) {
                        Timber.d("----- INSIDE onResponse ------");
                        if (!response.isSuccessful()) {
                            Timber.d("----- 1. ------");
                            Timber.d(bottomSheet.getResources().getString(R.string.no_success));
                            Toast.makeText(bottomSheet.getContext(), bottomSheet.getResources().getString(R.string.no_success), Toast.LENGTH_LONG).show();
                        } else {
                            if (response.body() != null) {
                                Timber.d("----- 2. ------");
                                List<DirectionsRoute> routes = response.body().trips();
                                if (routes != null) {
                                    Timber.d("----- 3. ------");
                                    if (routes.isEmpty()) {
                                        Timber.d("----- 4. ------");
                                        Timber.d("%s size = %s", bottomSheet.getResources().getString(R.string.successful_but_no_routes), routes.size());
                                        Toast.makeText(bottomSheet.getContext(), bottomSheet.getResources().getString(R.string.successful_but_no_routes), Toast.LENGTH_SHORT).show();
                                    } else {
                                        Timber.d("----- 5. ------");
                                        // Get most optimized route from API response
                                        optimizedRoute = routes.get(0);
                                        Timber.d("----- BEFORE DRAW ------");
                                        routeOptimizationInterface.drawOptimizedRoute(optimizedRoute);
                                    }
                                } else {
                                    Timber.d("----- 6. ------");
                                    Timber.d("List of routes in the response is null");
                                    Toast.makeText(bottomSheet.getContext(), String.format(bottomSheet.getResources().getString(R.string.null_in_response), "The Optimization API response's body"), Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<OptimizationResponse> call, Throwable t) {
                        Timber.d("Error: %s", t.getMessage());
                    }
                });
            }
        });
    }
}
