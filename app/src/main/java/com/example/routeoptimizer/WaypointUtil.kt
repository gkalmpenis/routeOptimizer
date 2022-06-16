package com.example.routeoptimizer

import com.mapbox.api.geocoding.v5.models.CarmenFeature
import com.mapbox.api.optimization.v1.models.OptimizationWaypoint
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.geometry.LatLng

object WaypointUtil {
    val alreadyCheckedWaypoints = mutableListOf<Int>() // List that indicates which Waypoints should not be checked because they already were

    fun getWaypointIndexByLatLng(waypoints: List<OptimizationWaypoint>, latLng: LatLng): Int? {
        // Since waypoints always have slightly different coordinates than user selected locations,
        // we will obtain them in approximation within a radius eg. 00.0000 - 00.0006 (+-3).
        // Do not place a large radius otherwise the correctness of the order will be affected.

        val precisionRadius = .0006

        waypoints.asSequence()
            .filter { !alreadyCheckedWaypoints.contains(it.waypointIndex()) } // Do not search all the waypoints for optimization purpose
            .forEach {
                // it.location().coordinates()[0] -> longitude
                // it.location().coordinates()[1] -> latitude
                if (it.location()!!.coordinates()[0] in latLng.longitude.minus(precisionRadius/2)..latLng.longitude.plus(precisionRadius/2)
                    && it.location()!!.coordinates()[1] in latLng.latitude.minus(precisionRadius/2)..latLng.latitude.plus(precisionRadius/2)
                ) {
                    alreadyCheckedWaypoints.add(it.waypointIndex())
                    return it.waypointIndex()
                }
            }

        // If there is no match in the specified radius return null.
        // It can happen in locations slightly away from the road, like mountains, but can also happen in city buildings.
        return null
    }
}