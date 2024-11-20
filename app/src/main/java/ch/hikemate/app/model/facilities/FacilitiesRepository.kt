package ch.hikemate.app.model.facilities

import android.util.JsonReader
import android.util.Log
import ch.hikemate.app.model.route.Bounds
import okhttp3.OkHttpClient
import okhttp3.Request
import org.osmdroid.util.GeoPoint

data class Facility(val type: String, val coordinates: GeoPoint)

class FacilitiesRepository(private val client: OkHttpClient) {

  fun getFacilities(
      bounds: Bounds,
      onSuccess: (List<Facility>) -> Unit,
      onFailure: (Exception) -> Unit
  ) {

    val listOfAmenities =
        "toilets|parking|waste_basket|supermarket|drinking_water|ranger_station|bbq|bench|restaurant|biergarten"
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

    val response = client.newCall(requestBuilder.build()).execute()

    try {
      if (response.isSuccessful) {
        val responseBody = response.body?.string() ?: throw Exception("Empty response body")

        val facilities = filterAllAmenities(responseBody)

        Log.d("FacilitiesRepository", "Got facilities: $facilities")

        onSuccess(facilities)
      }
    } catch (e: Exception) {
      Log.d("FacilitiesRepository", "Failed to fetch facilities: ${e.message}")
      onFailure(e)
    }
  }

  // Function to filter all amenities from the Overpass API response
  fun filterAllAmenities(responseJson: String): List<Facility> {
    val reader = JsonReader(responseJson.reader())
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
            var amenity: String? = null

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
                      amenity = reader.nextString()
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
            }

            reader.endObject() // End the element object
          }
          reader.endArray() // End the elements array
        }
        else -> reader.skipValue() // Skip unknown fields
      }
    }
    return facilities
  }
}
