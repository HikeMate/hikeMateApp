package ch.hikemate.app.model.route

import android.util.JsonReader
import java.io.IOException
import java.io.Reader
import kotlinx.serialization.*
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody

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
    val jsonBody = buildJsonObject {
      put(
          "locations",
          buildJsonArray {
            coordinates.forEach { coordinate ->
              add(
                  buildJsonObject {
                    put("lat", coordinate.lat)
                    put("lon", coordinate.lon)
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

  private inner class ElevationServiceCallback(
      private val onSuccess: (List<Double>) -> Unit,
      private val onFailure: (Exception) -> Unit
  ) : okhttp3.Callback {

    override fun onFailure(call: okhttp3.Call, e: IOException) {
      onFailure(e)
    }

    override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
      if (response.isSuccessful) {
        val body = response.body?.charStream()
        if (body == null) {
          onFailure(Exception("Failed to get elevation"))
          return
        }
        val listElevations = parseElevations(body)
        onSuccess(listElevations)
      } else {
        onFailure(Exception("Failed to get elevation"))
      }
    }

    private fun parseElevations(responseReader: Reader): List<Double> {
      val elevations = mutableListOf<Double>()
      val jsonReader = JsonReader(responseReader)
      jsonReader.beginObject()
      while (jsonReader.hasNext()) {
        val name = jsonReader.nextName()
        if (name == "results") {
          // Parse the elements array
          jsonReader.beginArray()
          while (jsonReader.hasNext()) {
            jsonReader.beginObject() // We're in an element object
            elevations.add(parseElement(jsonReader))
            jsonReader.endObject()
          }
          jsonReader.endArray()
        } else {
          jsonReader.skipValue()
        }
      }
      jsonReader.endObject()
      return elevations
    }

    private fun parseElement(elementReader: JsonReader): Double {
      var elevation = 0.0
      while (elementReader.hasNext()) {
        val name = elementReader.nextName()
        when (name) {
          "elevation" -> elevation = elementReader.nextDouble()
          else -> elementReader.skipValue()
        }
      }
      return elevation
    }
  }
}
