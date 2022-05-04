package com.example.routeoptimizer.viewmodels

import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mapbox.api.geocoding.v5.models.CarmenFeature
import com.mapbox.geojson.Point
import timber.log.Timber

class MainActivityViewModel: ViewModel() {

//    // Experiment to place stopsHashMap inside this viewModel
//    private val testHashMap = LinkedHashMap<Point, CarmenFeature>()
//    var testHashMapLive = MutableLiveData<LinkedHashMap<Point, CarmenFeature>>() // <-- This is what will be observed!
//    fun addToTestHasHMap(p: Point, c: CarmenFeature) {
//        testHashMap[p] = c
//        testHashMapLive.value = testHashMap
//    }
//    // And then in MainActivity we can do something like
//    mainActivityViewModel.addToTestHasHMap(point, carmenFeature)
//    // And to observe it (in onCreate)
//    mainActivityViewModel.testHashMapLive.observe(this) {
//        // actions!
//    }
}