package com.example.routeoptimizer;

import android.widget.TextView;

public class BottomSheetEntity {
    /*
     The current idea is that this class will contain all the information that appear in the bottom sheet like:
     - CarmenFeature of the current selected location
     - placeNameTextView information
     - addAsStopButton information (--> rename to "stopsButton" ??)
     - currentStopsCounter

     The stuff in here should not be static, a single instance of this object should be created in the MainActivity
     and this class should help us get at any time information about the current state of all the info that appear
     inside the bottom sheet
    */

    private static BottomSheetEntity bottomSheetEntity;

    // A private Constructor prevents any other class from instantiating.
    private BottomSheetEntity() {}

    public static BottomSheetEntity getInstance() {
        if (bottomSheetEntity == null) { // If there is no instance available create a new one
            bottomSheetEntity = new BottomSheetEntity();
        }

        return bottomSheetEntity;
    }

    private TextView placeNameTextView;

    /* Other methods protected by singleton-ness */

    public void setPlaceNameReference(TextView placeNameTextView) { this.placeNameTextView = placeNameTextView; }

    public void updatePlaceNameText(String newText) {
        this.placeNameTextView.setText(newText);
    }
}
