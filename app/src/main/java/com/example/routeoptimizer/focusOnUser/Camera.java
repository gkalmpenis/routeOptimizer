package com.example.routeoptimizer.focusOnUser;

import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.maps.MapboxMap;

public class Camera {

    public static void centerAtUserLocation(MapboxMap mapboxMap) {
        CameraPosition currentCameraPosition = mapboxMap.getCameraPosition();
        double currentZoom = currentCameraPosition.zoom;
        double currentTilt = currentCameraPosition.tilt;
        System.out.println("ZOOM ----> " + currentZoom);
        System.out.println(" TILT -----> " + currentTilt);
        //just tests..
    }
}
