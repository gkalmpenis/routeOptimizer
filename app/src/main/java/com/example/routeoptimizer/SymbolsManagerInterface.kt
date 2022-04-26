package com.example.routeoptimizer

import com.mapbox.api.geocoding.v5.models.CarmenFeature
import com.mapbox.api.optimization.v1.models.OptimizationWaypoint
import com.mapbox.mapboxsdk.plugins.annotation.Symbol

interface SymbolsManagerInterface {
    companion object {
        const val RED_MARKER_ORIGINAL_ICON_SIZE = 1.0f
        const val BLUE_MARKER_ORIGINAL_ICON_SIZE = 0.74f
        const val BLUE_MARKER_ORIGINAL_TEXT_SIZE = 16.0f
        const val BLUE_MARKER_EXPANDED_ICON_SIZE = 0.9f
        const val BLUE_MARKER_EXPANDED_TEXT_SIZE = 20.0f
    }

    fun createSymbolInMap(selectedCarmenFeature: CarmenFeature, iconImageString: String): Symbol?
    fun switchSymbolIconInMap(symbol: Symbol)
    fun updateNumberInSymbolIcons(waypoints: List<OptimizationWaypoint>)
    fun deleteSymbolFromMap(symbol: Symbol)
    fun changeIconAndTextSize(symbol: Symbol, iconSize: Float, textSize: Float)
    fun getLatestSearchedSymbol(): Symbol?
    // Mporeis na peis "val latestSearchedSymbolOne: Symbol?"
    // kai sthn mainActivity na kaneis
    // override val latestSearchedSymbolOne: Symbol?
    //        get() = { return latestSearchedLocationSymbol }

}