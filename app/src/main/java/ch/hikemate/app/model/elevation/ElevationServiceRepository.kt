package ch.hikemate.app.model.elevation

import ch.hikemate.app.model.route.LatLong
import java.io.IOException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json.Default.decodeFromString
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody

private const val BASE_URL = "https://api.open-elevation.com/api/v1/lookup"
private const val LOCATION_KEY = "locations"
private const val LATITUDE_KEY = "latitude"
private const val LONGITUDE_KEY = "longitude"
private const val ACCEPT_HEADER = "Accept"
private const val APPLICATION_JSON = "application/json"
private const val CONTENT_TYPE_HEADER = "Content-Type"
private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

/** Serializable data class for the response from the Elevation API */
@Serializable
data class ElevationResponse(val results: List<ElevationResult>)

/** Serializable data class for the response from the Elevation API */
@Serializable
data class ElevationResult(val latitude: Double, val longitude: Double, val elevation: Double)

/**
 * A repository for the ElevationService. This class is responsible for making the network request
 */
class ElevationServiceRepository(private val client: OkHttpClient) : ElevationService {

    // Cache for the elevation data, indexed by hike ID
    private val cache = mutableMapOf<Long, List<Double>>()

    /**
     * Get the size of the cache
     *
     * @return The size of the cache
     */
    fun getCacheSize(): Int {
        return cache.size
    }

    override fun getElevation(
        coordinates: List<LatLong>,
        hikeID: Long,
        onSuccess: (List<Double>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        if (coordinates.isEmpty()) {
            onFailure(Exception("No coordinates provided"))
            return
        }
        // If the cache already has the elevation data for this hike, call onSuccess
        for (mutableEntry in cache) {
            if (mutableEntry.key == hikeID) {
                onSuccess(mutableEntry.value)
                return
            }
        }
        // Create the JSON body for the request
        val jsonBody = buildJsonObject {
            // The major object
            put(
                LOCATION_KEY,
                buildJsonArray {
                    coordinates.forEach { coordinate ->
                        add(
                            // Object with the location
                            buildJsonObject {
                                put(LATITUDE_KEY, coordinate.lat)
                                put(LONGITUDE_KEY, coordinate.lon)
                            })
                    }
                })
        }

        val body =
            RequestBody.create(JSON_MEDIA_TYPE, jsonBody.toString())

        val requestBuilder =
            okhttp3.Request.Builder()
                .url(BASE_URL)
                .post(body)
                .addHeader(ACCEPT_HEADER, APPLICATION_JSON)
                .addHeader(CONTENT_TYPE_HEADER, APPLICATION_JSON)
                .build()

        // Callback for the network request, it also updates the cache
        val onSuccessWithCache: (List<Double>) -> Unit = {
            cache[hikeID] = it
            onSuccess(it)
        }
        client.newCall(requestBuilder)
            .enqueue(ElevationServiceCallback(onSuccessWithCache, onFailure))
    }

    /**
     * A callback for the network request to the Elevation API. This callback will parse the response
     */
    private inner class ElevationServiceCallback(
        private val onSuccess: (List<Double>) -> Unit,
        private val onFailure: (Exception) -> Unit
    ) : okhttp3.Callback {

        override fun onFailure(call: okhttp3.Call, e: IOException) {
            onFailure(e)
        }

        override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
            if (response.isSuccessful) {
                val body = response.body?.string()
                print(body)

                if (body == null) {
                    onFailure(Exception("Failed to get elevation"))
                    return
                }

                // This function decodes the JSON response into a ElevationResponse which is a List of
                // ElevationResult
                val elevationResponse = decodeFromString<ElevationResponse>(body)

                // Call the onSuccess callback with the list of elevations
                onSuccess(elevationResponse.results.map { it.elevation })
            } else {

                onFailure(Exception("Failed to get elevation"))
            }
        }
    }
}
