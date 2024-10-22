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

@Serializable data class ElevationResponse(val results: List<ElevationResult>)

@Serializable
data class ElevationResult(val latitude: Double, val longitude: Double, val elevation: Double)

/**
 * A repository for the ElevationService. This class is responsible for making the network request
 */
class ElevationServiceRepository(private val client: OkHttpClient) : ElevationService {

  private val BASE_URL = "https://api.open-elevation.com/api/v1/lookup"

  override fun getElevation(
      coordinates: List<LatLong>,
      onSuccess: (List<Double>) -> Unit,
      onFailure: (Exception) -> Unit
  ) {
    if (coordinates.isEmpty()) {
      onFailure(Exception("No coordinates provided"))
      return
    }
    // Create the JSON body for the request
    val jsonBody = buildJsonObject {
      // The major object
      put(
          "locations",
          buildJsonArray {
            coordinates.forEach { coordinate ->
              add(
                  // Object with the location
                  buildJsonObject {
                    put("latitude", coordinate.lat)
                    put("longitude", coordinate.lon)
                  })
            }
          })
    }

    val body =
        RequestBody.create("application/json; charset=utf-8".toMediaType(), jsonBody.toString())

    val requestBuilder =
        okhttp3.Request.Builder()
            .url(BASE_URL)
            .post(body)
            .addHeader("Accept", "application/json")
            .addHeader("Content-Type", "application/json")
            .build()

    client.newCall(requestBuilder).enqueue(ElevationServiceCallback(onSuccess, onFailure))
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

        if (body == null) {
          onFailure(Exception("Failed to get elevation"))
          return
        }

        val elevationResponse = decodeFromString<ElevationResponse>(body)

        onSuccess(elevationResponse.results.map { it.elevation })
      } else {

        onFailure(Exception("Failed to get elevation"))
      }
    }
  }
}
