package com.example.routeoptimizer.viewmodels

import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mapbox.api.geocoding.v5.models.CarmenFeature
import com.mapbox.geojson.Point
import timber.log.Timber

class MainActivityViewModel: ViewModel() {

    // Experiment to place stopsHashMap inside this viewModel
    private val testHashMap = LinkedHashMap<Point, CarmenFeature>()
    var testHashMapLive = MutableLiveData<LinkedHashMap<Point, CarmenFeature>>() // <-- This is what will be observed!

    fun getHashMapSize(): Int {
        return testHashMapLive.value?.size ?: -1
    }

    fun addToTestHashMap(point: Point, carmenFeature: CarmenFeature) {
        testHashMap[point] = carmenFeature
        testHashMapLive.value = testHashMap
    }

    fun removeFromHashMap(key: Point) {
        testHashMap.remove(key)
        testHashMapLive.value = testHashMap
    }
//    // And then in MainActivity we can do something like
//    mainActivityViewModel.addToTestHasHMap(point, carmenFeature)
//    // And to observe it (in onCreate)
//    mainActivityViewModel.testHashMapLive.observe(this) {
//        // actions!
//    }
}