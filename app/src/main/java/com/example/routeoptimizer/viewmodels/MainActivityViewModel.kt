package com.example.routeoptimizer.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mapbox.api.geocoding.v5.models.CarmenFeature
import com.mapbox.geojson.Point
import timber.log.Timber

class MainActivityViewModel: ViewModel() {

    // HashMap that will contain locations that user adds as stop
//    @JvmField
//    val stopsHashMap: MutableLiveData<LinkedHashMap<Point, CarmenFeature>> by lazy {
//        MutableLiveData<LinkedHashMap<Point, CarmenFeature>>()
//    }

    // Etsi ginetai to observe? Epishs mallon paei sto activity kai oxi edw sto view model
//    mainActivityViewModel.stopsHashMap.observe(this, Observer { hashmap ->
//        hashmap.put((feature.geometry() as Point), feature)
//        Timber.d("--Megethos tou hashmap: ${hashmap.size}")
//    })

}