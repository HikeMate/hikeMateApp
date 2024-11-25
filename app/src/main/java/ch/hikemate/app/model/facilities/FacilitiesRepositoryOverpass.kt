package ch.hikemate.app.model.facilities

import android.util.Log
import ch.hikemate.app.model.overpassAPI.OverpassRepository
import ch.hikemate.app.model.route.Bounds
import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.Callback
import okhttp3.OkHttpClient
import org.osmdroid.util.GeoPoint

class FacilitiesRepositoryOverpass(private val client: OkHttpClient) :
    FacilitiesRepository, OverpassRepository() {

  override fun getFacilities(
      bounds: Bounds,
      onSuccess: (List<Facility>) -> Unit,
      onFailure: (Exception) -> Unit
  ) {

    // Generate pipe-separated list of amenities (e.g., "toilets|parking|waste_basket...")
    val listOfAmenities = FacilityType.listOfAmenitiesForOverpassRequest()

    val requestData =
        """
        $JSON_OVERPASS_FORMAT_TAG
        (
          node["amenity"~"$listOfAmenities"](${bounds.toStringForOverpassAPI()});        
        );
        out geom;
    """

    buildAndSendRequest(requestData, client, ResponseHandler(onSuccess, onFailure))
  }

  /**
   * The response handler for the API request.
   *
   * @param onSuccess The callback to be called when the facilities are successfully fetched.
   * @param onFailure The callback to be called when the facilities could not be fetched.
   */
  private inner class ResponseHandler(
      val onSuccess: (List<Facility>) -> Unit,
      val onFailure: (Exception) -> Unit
  ) : Callback {

    override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
      onFailure(e)
    }

    override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
      try {
        if (!response.isSuccessful) {
          onFailure(
              Exception(
                  "Failed to fetch facilities from Overpass API. Response code: ${response.code}"))
          return
        }

        val responseJsonString = response.body?.string() ?: throw Exception("Response body is null")
        val facilities = parseAmenities(responseJsonString)
        Log.d("FacilitiesRepository", "Got ${facilities.size} facilities")
        onSuccess(facilities)
      } catch (e: Exception) {
        onFailure(e)
      }
    }

    /**
     * Parses and filters JSON response from Overpass API into a list of Facility objects. Filters
     * out amenities that have an invalid latitude, longitude or facility type.
     *
     * @param jsonString JSON response string from Overpass API
     * @return List of parsed Facility objects
     */
    fun parseAmenities(jsonString: String): List<Facility> {
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
}
