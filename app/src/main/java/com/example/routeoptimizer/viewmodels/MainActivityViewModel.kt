package com.example.routeoptimizer.viewmodels

import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mapbox.api.geocoding.v5.models.CarmenFeature
import com.mapbox.geojson.Point
import timber.log.Timber

class MainActivityViewModel: ViewModel() {

    // We have 2 structures because stopsHashMapLive as a MutableLiveData object must be assigned
    // to a value in order for the observables to perform actions, so this value will be stopsHashMap.
    val stopsHashMap = LinkedHashMap<Point, CarmenFeature>()
    var stopsHashMapLive = MutableLiveData<LinkedHashMap<Point, CarmenFeature>>() // This is what will be observed

    fun getStopsHashMapSize(): Int {
        return stopsHashMapLive.value?.size ?: -1
    }

    fun addToStopsHashMap(point: Point, carmenFeature: CarmenFeature) {
        stopsHashMap[point] = carmenFeature
        stopsHashMapLive.value = stopsHashMap
    }

    fun removeFromStopsHashMap(key: Point) {
        stopsHashMap.remove(key)
        stopsHashMapLive.value = stopsHashMap
    }

    fun clearStopsHashMap() {
        stopsHashMap.clear()
        stopsHashMapLive.value = stopsHashMap
    }

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