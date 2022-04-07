package com.example.routeoptimizer

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.geocoding.v5.models.CarmenFeature
import com.mapbox.geojson.Point

interface RouteOptimizationInterface {
    fun drawOptimizedRoute(route: DirectionsRoute)
}