package com.example.routeoptimizer;

import androidx.annotation.NonNull;

import com.mapbox.api.geocoding.v5.models.CarmenFeature;
import com.mapbox.mapboxsdk.plugins.annotation.Symbol;

public interface SymbolsManagerInterface {
    Symbol createSymbolInMap(@NonNull CarmenFeature selectedCarmenFeature, @NonNull String iconImageString);
    void updateSymbolIconInMap(Symbol symbol);
    void deleteSymbolFromMap(Symbol symbol);
    Symbol getLatestSearchedSymbol();
}
