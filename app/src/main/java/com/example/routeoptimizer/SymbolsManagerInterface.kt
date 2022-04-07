package com.example.routeoptimizer

import com.mapbox.api.geocoding.v5.models.CarmenFeature
import com.mapbox.api.optimization.v1.models.OptimizationWaypoint
import com.mapbox.mapboxsdk.plugins.annotation.Symbol

interface SymbolsManagerInterface {
    companion object {
        const val RED_MARKER_ORIGINAL_SIZE = 1.0f
        const val BLUE_MARKER_ORIGINAL_SIZE = 0.74f
        const val BLUE_MARKER_EXPANDED_SIZE = 0.9f
    }

    fun createSymbolInMap(selectedCarmenFeature: CarmenFeature, iconImageString: String): Symbol?
    fun switchSymbolIconInMap(symbol: Symbol)
    fun updateNumberInSymbolIcons(waypoints: List<OptimizationWaypoint?>?)
    fun deleteSymbolFromMap(symbol: Symbol)
    fun changeIconSize(symbol: Symbol, size: Float)
    fun getLatestSearchedSymbol(): Symbol?
}