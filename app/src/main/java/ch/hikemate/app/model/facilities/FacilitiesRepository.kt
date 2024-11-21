package ch.hikemate.app.model.facilities

import android.util.JsonReader
import android.util.Log
import ch.hikemate.app.model.route.Bounds
import java.io.IOException
import java.io.Reader
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.osmdroid.util.GeoPoint

enum class FacilityType(val type: String) {
  TOILETS("toilets"),
  PARKING("parking"),
  WASTE_BASKET("waste_basket"),
  SUPERMARKET("supermarket"),
  DRINKING_WATER("drinking_water"),
  RANGER_STATION("ranger_station"),
  BBQ("bbq"),
  BENCH("bench"),
  RESTAURANT("restaurant"),
  BIERGARTEN("biergarten");

  companion object {
    fun listOfAmenitiesForOverpassRequest(): String {
      var string = ""
      for (facility in FacilityType.values()) {
        string += facility.type + "|"
      }
      return string
    }

    fun fromString(string: String): FacilityType? {
      return when (string) {
        "toilets" -> TOILETS
        "parking" -> PARKING
        "waste_basket" -> WASTE_BASKET
        "supermarket" -> SUPERMARKET
        "drinking_water" -> DRINKING_WATER
        "ranger_station" -> RANGER_STATION
        "bbq" -> BBQ
        "bench" -> BENCH
        "restaurant" -> RESTAURANT
        "biergarten" -> BIERGARTEN
        else -> null
      }
    }
  }
}

data class Facility(val type: FacilityType, val coordinates: GeoPoint)

class FacilitiesRepository(private val client: OkHttpClient) {

  private fun setRequestHeaders(request: Request.Builder) {
    request.header("User-Agent", "Hikemate/1.0")
  }

  fun getFacilities(
      bounds: Bounds,
      onSuccess: (List<Facility>) -> Unit,
      onFailure: (Exception) -> Unit
  ) {

    // looks like: "toilets|parking|waste_basket...
    val listOfAmenities = FacilityType.listOfAmenitiesForOverpassRequest()

    val requestData =
        """
        [out:json];
        (
          node["amenity"~"$listOfAmenities"](${bounds.toStringForOverpassAPI()});        
        );
        out geom;
    """
            .trimIndent()

    val requestBuilder =
        Request.Builder().url("https://overpass-api.de/api/interpreter?data=$requestData").get()

    setRequestHeaders(requestBuilder)

    val response =
        client
            .newCall(requestBuilder.build())
            .enqueue(
                object : Callback {
                  override fun onFailure(call: Call, e: IOException) {
                    onFailure(e)
                  }

                  override fun onResponse(call: Call, response: Response) {
                    try {
                      if (response.isSuccessful) {
                        response.body?.let { body ->
                          val facilities = filterAmenities(body.charStream())
                          Log.d("FacilitiesRepository", "Got ${facilities.size} facilities")
                          onSuccess(facilities)
                        } ?: throw Exception("Empty response body") // TODO: Handle this better
                      } else {
                        throw Exception("Request failed with code: ${response.code}")
                      }
                    } catch (e: Exception) {
                      onFailure(e)
                    }
                  }
                })
  }

  // Function to filter all amenities from the Overpass API response
  private fun filterAmenities(responseJson: Reader): List<Facility> {
    val reader = JsonReader(responseJson)
    val facilities = mutableListOf<Facility>()

    reader.beginObject() // We're in the root object
    while (reader.hasNext()) {
      when (reader.nextName()) {
        "elements" -> { // Start reading the elements array
          reader.beginArray()
          while (reader.hasNext()) {
            reader.beginObject() // Begin a new element
            var lat: Double? = null
            var lon: Double? = null
            var amenity: FacilityType? = null

            // Read the object
            while (reader.hasNext()) {
              when (reader.nextName()) {
                "lat" -> lat = reader.nextDouble()
                "lon" -> lon = reader.nextDouble()
                "tags" -> {
                  reader.beginObject() // Read tags object
                  while (reader.hasNext()) {
                    val tagName = reader.nextName()
                    if (tagName == "amenity") {
                      amenity = FacilityType.fromString(reader.nextString())
                    } else {
                      reader.skipValue() // Skip other tag values
                    }
                  }
                  reader.endObject() // End tags object
                }
                else -> reader.skipValue() // Skip all other keys
              }
            }

            if (amenity != null && lat != null && lon != null) {
              facilities.add(Facility(amenity, GeoPoint(lat, lon)))
            } // else skip the facility and move on to the next one

            reader.endObject()
          }
          reader.endArray()
        }
        else -> reader.skipValue() // Skip unknown fields
      }
    }
    return facilities
  }
}
