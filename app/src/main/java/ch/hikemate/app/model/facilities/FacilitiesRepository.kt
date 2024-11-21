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

class FacilitiesRepository(private val client: OkHttpClient) {

  /**
   * Sets a standard header for API requests.
   *
   * @param request Request builder instance to which headers will be added
   */
  private fun setRequestHeaders(request: Request.Builder) {
    request.header("User-Agent", "Hikemate/1.0")
  }

  /**
   * Asynchronously fetches facilities within the bounds. Makes a request to the Overpass API to
   * retrieve amenities like toilets, parking areas, and waste baskets. These can be specified in
   * the FacilityType enum.
   *
   * @param bounds Geographical bounds within which to search for facilities
   * @param onSuccess Callback to handle the resulting list of facilities when the operation
   *   succeeds
   * @param onFailure Callback invoked when the operation fails
   */
  fun getFacilities(
      bounds: Bounds,
      onSuccess: (List<Facility>) -> Unit,
      onFailure: (Exception) -> Unit
  ) {

    // Generate pipe-separated list of amenities (e.g., "toilets|parking|waste_basket...")
    val listOfAmenities = FacilityType.listOfAmenitiesForOverpassRequest()

    val requestData =
        """
        [out:json];
        (
          node["amenity"~"$listOfAmenities"](${bounds.toStringForOverpassAPI()});        
        );
        out geom;
    """

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

  /**
   * Parses and filters JSON response from Overpass API into a list of Facility objects. Filters out
   * amenities that have an invalid latitude, longitude or facility type.
   *
   * @param jsonString JSON response string from Overpass API
   * @return List of parsed Facility objects
   */
  fun filterAmenities(jsonString: String): List<Facility> {
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
