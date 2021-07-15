package com.example.routeoptimizer;

import androidx.annotation.NonNull;

import com.mapbox.api.geocoding.v5.models.CarmenFeature;
import com.mapbox.mapboxsdk.plugins.annotation.Symbol;

public interface SymbolsManagerInterface {
    float RED_MARKER_ORIGINAL_SIZE = 1.0f;
    float BLUE_MARKER_ORIGINAL_SIZE = 0.74f;
    float BLUE_MARKER_EXPANDED_SIZE = 0.9f;

    Symbol createSymbolInMap(@NonNull CarmenFeature selectedCarmenFeature, @NonNull String iconImageString);
    void updateSymbolIconInMap(Symbol symbol);
    void deleteSymbolFromMap(Symbol symbol);
    void changeIconSize (@NonNull Symbol symbol, float size);
    Symbol getLatestSearchedSymbol();
}
