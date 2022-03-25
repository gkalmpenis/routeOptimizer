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
    val stopsHashMap: MutableLiveData<LinkedHashMap<Point, CarmenFeature>> by lazy {
        MutableLiveData<LinkedHashMap<Point, CarmenFeature>>()
    }

    fun addStopToMap(point: Point, feature: CarmenFeature) {
        Timber.d("--Mphkame sthn addStopToMap()--")
        stopsHashMap.value?.put(point, feature)
    }
}