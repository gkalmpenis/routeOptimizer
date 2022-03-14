package com.example.routeoptimizer.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mapbox.api.geocoding.v5.models.CarmenFeature
import com.mapbox.geojson.Point

class MainActivityViewModel: ViewModel() {

    // HashMap that will contain locations that user adds as stop
//    @JvmField
    val stopsHashMap: MutableLiveData<LinkedHashMap<Point, CarmenFeature>> by lazy {
        MutableLiveData<LinkedHashMap<Point, CarmenFeature>>()
    }
}