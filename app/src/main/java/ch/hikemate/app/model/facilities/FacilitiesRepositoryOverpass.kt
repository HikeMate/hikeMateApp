package ch.hikemate.app.model.facilities

import android.util.JsonReader
import android.util.Log
import ch.hikemate.app.model.overpassAPI.OverpassRepository
import ch.hikemate.app.model.route.Bounds
import ch.hikemate.app.model.route.LatLong
import java.io.Reader
import okhttp3.Callback
import okhttp3.OkHttpClient

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

        val responseJsonReader =
            response.body?.charStream() ?: throw Exception("Response body is null")
        val facilities = parseAmenities(responseJsonReader)
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
     * @param responseReader The Reader object to parse the JSON response from
     * @return List of parsed Facility objects
     */
    fun parseAmenities(responseReader: Reader): List<Facility> {
      val facilities = mutableListOf<Facility?>()
      val jsonReader = JsonReader(responseReader)
      jsonReader.beginObject() // We're in the root object
      while (jsonReader.hasNext()) {
        val name = jsonReader.nextName()
        if (name == "elements") {
          // Parse the elements array
          jsonReader.beginArray()
          while (jsonReader.hasNext()) {
            jsonReader.beginObject() // We're in an element object

            facilities.add(parseAmenity(jsonReader))

            jsonReader.endObject()
          }
          jsonReader.endArray()
        } else {
          jsonReader.skipValue()
        }
      }
      jsonReader.endObject()
      return facilities.filterNotNull()
    }

    /**
     * Parses a single amenity element from the JSON response.
     *
     * @param jsonReader The JsonReader object to parse the amenity from
     * @return The Facility object parsed from the JSON response, or null if the amenity is invalid
     */
    fun parseAmenity(jsonReader: JsonReader): Facility? {
      var lat = 0.0
      var lon = 0.0
      var amenity: FacilityType? = null

      while (jsonReader.hasNext()) {
        val name = jsonReader.nextName()
        when (name) {
          "lat" -> lat = jsonReader.nextDouble()
          "lon" -> lon = jsonReader.nextDouble()
          "tags" -> {
            jsonReader.beginObject() // We're in the tags object of the element

            while (jsonReader.hasNext()) {
              val typeName = jsonReader.nextName()

              when (typeName) {
                "amenity" -> {
                  val amenityName = jsonReader.nextString()
                  amenity = FacilityType.fromString(amenityName)
                }
                else -> jsonReader.skipValue()
              }
            }

            jsonReader.endObject()
          }
          else -> jsonReader.skipValue()
        }
      }

      if (lat != 0.0 && lon != 0.0 && amenity != null) {
        return Facility(amenity, LatLong(lat, lon))
      }

      return null
    }
  }
}
