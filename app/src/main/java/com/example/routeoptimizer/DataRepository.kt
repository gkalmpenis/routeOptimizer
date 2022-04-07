package com.example.routeoptimizer

import com.mapbox.api.geocoding.v5.models.CarmenFeature
import com.mapbox.geojson.Point

object DataRepository {
    // LinkedHashMap that will contain locations that user adds as stop
    val stopsHashMap = LinkedHashMap<Point, CarmenFeature>()

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
}