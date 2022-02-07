package com.example.routeoptimizer

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point

interface RouteOptimizationInterface {
    fun convertStopsToPoints(): List<Point?>?
    fun drawOptimizedRoute(route: DirectionsRoute?)
}