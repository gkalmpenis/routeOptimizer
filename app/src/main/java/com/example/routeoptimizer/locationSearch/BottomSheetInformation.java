package com.example.routeoptimizer.locationSearch;

public class BottomSheetInformation {
    /*
     The current idea is that this class will contain all the information that appear in the bottom sheet like:
     - CarmenFeature of the current selected location
     - placeNameTextView information
     - addAsStopButton information (--> rename to "stopsButton" ??)
     - currentStopsCounter

     The stuff in here should not be static, an single instance of this object should be created in the MainActivity
     and this class should help us get at any time information about the current state of all the info that appear
     inside the bottom sheet
    */

    private static BottomSheetInformation bottomSheetInformation = new BottomSheetInformation();

    // A private Constructor prevents any other class from instantiating.
    private BottomSheetInformation() {}

    public static BottomSheetInformation getInstance() {
        return bottomSheetInformation;
    }

    /* Other methods protected by singleton-ness */
    protected static void demoMethod() {
        System.out.println("demoMethod for singleton");
    }
}
