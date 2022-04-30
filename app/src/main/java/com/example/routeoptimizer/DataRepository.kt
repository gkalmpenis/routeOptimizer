package com.example.routeoptimizer

import com.mapbox.api.geocoding.v5.models.CarmenFeature
import com.mapbox.api.optimization.v1.models.OptimizationWaypoint
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.geometry.LatLng

object DataRepository {
    // LinkedHashMap that will contain locations that user adds as stop
    val stopsHashMap = LinkedHashMap<Point, CarmenFeature>()
    val alreadyCheckedWaypoints = mutableListOf<Int>() // List that indicates which Waypoints should not be checked because they already where

    /**
     * This method converts each *CarmenFeature* in **stopsHashMap** to a *Point*.
     *
     * @return A list of *Point* objects that correspond to each element in **stopsHashMap**
     */
    fun convertStopsToPoints(hashMap: LinkedHashMap<Point, CarmenFeature>): List<Point> {
        val coordinates: MutableList<Point> = ArrayList()
        for (point in hashMap.keys) {
            coordinates.add(point)
        }
        return coordinates
    }

    fun getWaypointIndexByLatLng(waypoints: List<OptimizationWaypoint>, latLng: LatLng): Int? {
        // Since waypoints always have slightly different coordinates than user selected locations,
        // we will obtain them in approximation within a radius eg. 00.0000 - 00.0006 (+-3)

        val precisionRadius = .0006

        waypoints.asSequence()
            .filter { !alreadyCheckedWaypoints.contains(it.waypointIndex()) } // Do not search all the waypoints for optimization purpose
            .forEach {
                if (it.location()!!.coordinates()[0] in latLng.longitude.minus(precisionRadius/2)..latLng.longitude.plus(precisionRadius/2)
                    && it.location()!!.coordinates()[1] in latLng.latitude.minus(precisionRadius/2)..latLng.latitude.plus(precisionRadius/2)
                ) {
                    alreadyCheckedWaypoints.add(it.waypointIndex())
                    return it.waypointIndex()
                }
            }

        // If there is no match in the specified radius return null.
        // It can happen in locations far away from the road, like mountains.
        return null
    }

    // Exe kata nou pws an bgeis apthn efarmogh kai ksanampeis (xwris na thn kleiseis)
    // tha meinoun ta data ston stopsHashMap opote prepei na paikseis m auto
    // (mporeis na ton adeiaseis sto ondestroy alla idanika ftiaksto me livedata sto viewmodel)
}