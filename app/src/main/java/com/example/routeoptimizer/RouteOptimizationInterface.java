package com.example.routeoptimizer;

import com.mapbox.geojson.Point;

import java.util.List;

public interface RouteOptimizationInterface {
    List<Point> convertStopsToPoints();
}
