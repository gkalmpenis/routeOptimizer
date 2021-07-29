package com.example.routeoptimizer;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.mapbox.api.geocoding.v5.models.CarmenFeature;
import com.mapbox.geojson.Point;

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

    protected enum StopsButtonState { ADD_NEW_STOP, REMOVE_A_STOP }
    private final String ADD_AS_STOP = "Add as stop"; // Should be exactly the same as the text in R.string.add_stop_txt
    private final String REMOVE_FROM_STOPS = "Remove from stops"; // Should be exactly the same as the text in R.string.remove_stop_txt

    private SymbolsManagerInterface symbolsManagerInterface; // To perform create/update/delete actions on map symbols

    /* Methods protected by singleton-ness */
    protected void setBottomSheetReference(View bottomSheet) { this.bottomSheet = bottomSheet; }
    protected void initializeBottomSheetBehavior() { this.bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet); }
    protected void changeBottomSheetState(int newState) { this.bottomSheetBehavior.setState(newState); }
    protected void setPlaceNameReference(TextView placeNameTextView) { this.placeNameTextView = placeNameTextView; }
    protected void setStopsButtonReference(Button stopsButton) { this.stopsButton = stopsButton; }
    protected void setCurrentStopsCounterReference(TextView currentStopsCounterTextView) { this.currentStopsCounterTextView = currentStopsCounterTextView; }
    protected void setOptimizeButtonReference(Button optimizeButton) { this.optimizeButton = optimizeButton; }
    protected void setSymbolsManagerInterface(SymbolsManagerInterface symbolsManagerInterface) { this.symbolsManagerInterface = symbolsManagerInterface; }

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
}
