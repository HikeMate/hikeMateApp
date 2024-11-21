package ch.hikemate.app.model.facilities

import android.util.Log
import ch.hikemate.app.model.route.Bounds
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.io.IOException
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
                    val responseJsonString =
                        response.body?.string() ?: throw Exception("Response body is null")
                    val facilities = filterAmenities(responseJsonString)
                    Log.d("FacilitiesRepository", "Got ${facilities.size} facilities")
                    onSuccess(facilities)
                  } else {
                    throw Exception("Request failed with code: ${response.code}")
                  }
                } catch (e: Exception) {
                  onFailure(e)
                }
              }
            })
  }

  fun filterAmenities(jsonString: String): MutableList<Facility> {
    val gson = Gson()
    val facilities = mutableListOf<Facility>()

    // Parse the root JSON object
    val rootObject = gson.fromJson(jsonString, JsonObject::class.java)
    val elements = rootObject.getAsJsonArray("elements") ?: return facilities

    // Process each element in the "elements" array
    elements.forEach { element ->
      val obj = element.asJsonObject
      val lat = obj["lat"]?.asDouble
      val lon = obj["lon"]?.asDouble

      val tags = obj.getAsJsonObject("tags")
      val amenity = tags?.get("amenity")?.asString?.let { FacilityType.fromString(it) }

      if (lat != null && lon != null && amenity != null) {
        facilities.add(Facility(amenity, GeoPoint(lat, lon)))
      }
    }

    return facilities
  }
}
