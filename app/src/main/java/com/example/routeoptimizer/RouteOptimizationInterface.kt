package com.example.routeoptimizer;

import androidx.annotation.NonNull;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.maps.Style;

import java.util.List;

public interface RouteOptimizationInterface {
    List<Point> convertStopsToPoints();
    void drawOptimizedRoute(DirectionsRoute route);
}
