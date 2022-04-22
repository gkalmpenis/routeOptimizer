package com.example.routeoptimizer

import android.app.Activity
import android.content.Context
import android.widget.Toast
import com.mapbox.api.geocoding.v5.GeocodingCriteria
import com.mapbox.api.geocoding.v5.MapboxGeocoding
import com.mapbox.api.geocoding.v5.models.GeocodingResponse
import com.mapbox.core.exceptions.ServicesException
import com.mapbox.geojson.Point
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber

object ReverseGeocoderUtil {

    /**
     * This method is used to reverse geocode where the user has dropped the marker.
     *
     * @param point The location to use for the search
     */
    fun reverseGeocode(activity: MainActivity, point: Point) {
        try {
            val client = MapboxGeocoding.builder()
                .accessToken(activity.getString(R.string.mapbox_access_token))
                .query(Point.fromLngLat(point.longitude(), point.latitude()))
                .geocodingTypes(GeocodingCriteria.TYPE_ADDRESS)
                .build()

            client.enqueueCall(object : Callback<GeocodingResponse?> {
                override fun onResponse(call: Call<GeocodingResponse?>, response: Response<GeocodingResponse?>) {
                    if (response.body() != null) {
                        val results = response.body()!!.features()
                        if (results.size > 0) {
                            // If the geocoder returns a result, we take the first in the list.
                            val feature = results[0]
                            Timber.d("Successfully got a geocoding result from long click on map, place name: %s", feature.placeName())

                            // Call the MainActivity's method
                            activity.performActionsOnSearchResult(feature)
                        } else {
                            Timber.i("No results found for the clicked location")

                            // Print a toast message
                            activity.mapboxMap.getStyle { Toast.makeText(activity, activity.getString(R.string.no_results_for_clicked_location), Toast.LENGTH_SHORT).show() }
                        }
                    }
                }

                override fun onFailure(call: Call<GeocodingResponse?>, t: Throwable) {
                    Timber.e("Geocoding Failure: %s", t.message)

                    // Print a toast message
                    activity.mapboxMap.getStyle { Toast.makeText(activity, activity.getString(R.string.err_requesting_geocoding_check_network), Toast.LENGTH_SHORT).show() }
                }
            })
        } catch (servicesException: ServicesException) {
            Timber.e("Error geocoding: %s", servicesException.toString())
            servicesException.printStackTrace()
        }
    }
}