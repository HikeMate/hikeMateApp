package ch.hikemate.app.model.route

import android.util.JsonReader
import java.io.Reader
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Overpass implementation of the hiking route provider repository.
 *
 * This implementation uses the Overpass API to fetch the hiking routes from OSM.
 *
 * @see <a href="https://dev.overpass-api.de/overpass-doc/">Overpass API documentation</a>
 */
class HikeRoutesRepositoryOverpass(val client: OkHttpClient) : HikeRoutesRepository {

  /** The URL of the Overpass API interpreter. */
  private val OVERPASS_API_URL: String = "https://overpass-api.de/api/interpreter"
  /** The type of format to request from the Overpass API, written in OverpassQL. */
  private val JSON_OVERPASS_FORMAT_TAG = "[out:json]"

  override fun getRoutes(
      bounds: Bounds,
      onSuccess: (List<HikeRoute>) -> Unit,
      onFailure: (Exception) -> Unit
  ) {
    val boundingBoxOverpass =
        "(${bounds.minLat},${bounds.minLon},${bounds.maxLat},${bounds.maxLon})"

    // See OverpassQL documentation for more information on the query format.
    val overpassRequestData =
        """
            ${JSON_OVERPASS_FORMAT_TAG};
            nwr[route="hiking"]${boundingBoxOverpass};
            out geom;
        """
            .trimIndent()

    val requestBuilder = Request.Builder().url("$OVERPASS_API_URL?data=$overpassRequestData").get()

    setRequestHeaders(requestBuilder)

    client.newCall(requestBuilder.build()).enqueue(OverpassResponseHandler(onSuccess, onFailure))
  }

  /**
   * Sets the headers for the request. Especially the user-agent.
   *
   * @param request The request builder to set the headers on.
   */
  private fun setRequestHeaders(request: Request.Builder) {
    request.header("User-Agent", "Hikemate/1.0")
  }

  /**
   * The response handler for the Overpass API request.
   *
   * @param onSuccess The callback to be called when the routes are successfully fetched.
   * @param onFailure The callback to be called when the routes could not be fetched.
   */
  inner class OverpassResponseHandler(
      val onSuccess: (List<HikeRoute>) -> Unit,
      val onFailure: (Exception) -> Unit
  ) : okhttp3.Callback {
    override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
      onFailure(e)
    }

    override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
      if (!response.isSuccessful) {
        onFailure(
            Exception("Failed to fetch routes from Overpass API. Response code: ${response.code}"))
        return
      }

      val responseBody = response.body?.charStream() ?: throw Exception("Response body is null")
      val routes = parseRoutes(responseBody)

      onSuccess(routes)
    }

    /**
     * Parses the routes from the Overpass API response.
     *
     * @param responseReader The reader for the response body.
     * @return The list of parsed routes.
     */
    private fun parseRoutes(responseReader: Reader): List<HikeRoute> {
      val routes = mutableListOf<HikeRoute>()
      val jsonReader = JsonReader(responseReader)
      jsonReader.beginObject() // We're in the root object
      while (jsonReader.hasNext()) {
        val name = jsonReader.nextName()
        if (name == "elements") {
          // Parse the elements array
          jsonReader.beginArray()
          while (jsonReader.hasNext()) {
            jsonReader.beginObject() // We're in an element object
            routes.add(parseElement(jsonReader))
            jsonReader.endObject()
          }
          jsonReader.endArray()
        } else {
          jsonReader.skipValue()
        }
      }
      jsonReader.endObject()
      return routes
    }

    /**
     * Parses a single element from the Overpass API response. The reader is supposed to already be
     * in the element object.
     *
     * @param elementReader The reader for the element object.
     * @return The parsed route.
     */
    private fun parseElement(elementReader: JsonReader): HikeRoute {
      var id = ""
      val boundsBuilder = Bounds.Builder()
      val points = mutableListOf<LatLong>()
      while (elementReader.hasNext()) {
        val name = elementReader.nextName()
        when (name) {
          "id" -> id = elementReader.nextInt().toString()
          "bounds" -> {
            elementReader.beginObject() // We're in the bounds object of the element
            while (elementReader.hasNext()) {
              when (elementReader.nextName()) {
                "minlat" -> boundsBuilder.setMinLat(elementReader.nextDouble())
                "minlon" -> boundsBuilder.setMinLon(elementReader.nextDouble())
                "maxlat" -> boundsBuilder.setMaxLat(elementReader.nextDouble())
                "maxlon" -> boundsBuilder.setMaxLat(elementReader.nextDouble())
                else -> elementReader.skipValue()
              }
            }
            elementReader.endObject()
          }
          "members" -> {
            elementReader.beginArray() // We're in the members array of the element
            while (elementReader.hasNext()) {
              elementReader.beginObject() // We're in a member object
              points.addAll(parseMember(elementReader))
              elementReader.endObject()
            }
            elementReader.endArray()
          }
          else -> elementReader.skipValue()
        }
      }
      return HikeRoute(id, boundsBuilder.build(), points)
    }

    /**
     * Parses a member from the Overpass API response. The reader is supposed to already be in the
     * member object.
     *
     * @param memberReader The reader for the member object.
     * @return The list of parsed points for this member.
     */
    private fun parseMember(memberReader: JsonReader): List<LatLong> {
      val points = mutableListOf<LatLong>()
      while (memberReader.hasNext()) {
        val name = memberReader.nextName()
        if (name == "type") {
          val type = memberReader.nextString()
          when (type) {
            // Lat and Long are in the object, no need to change the reader
            "node" -> points.add(parseLatLong(memberReader))
            "way" -> points.addAll(parseWay(memberReader))
            else -> memberReader.skipValue()
          }
        } else {
          memberReader.skipValue()
        }
      }

      return points
    }

    /**
     * Parses a way from the Overpass API response. The reader is supposed to already be in the way
     * object.
     *
     * @param wayReader The reader for the way object.
     * @return The list of parsed points for this way.
     */
    private fun parseWay(wayReader: JsonReader): List<LatLong> {
      val points = mutableListOf<LatLong>()
      while (wayReader.hasNext()) {
        if (wayReader.nextName() != "geometry") {
          wayReader.skipValue()
          continue
        }
        wayReader.beginArray()
        while (wayReader.hasNext()) {
          wayReader.beginObject()
          points.add(parseLatLong(wayReader))
          wayReader.endObject()
        }
        wayReader.endArray()
      }
      return points
    }

    /**
     * Parses a latitude and longitude from the Overpass API response. The reader is supposed to
     * already be in the object to parse.
     *
     * @param reader The reader for the latlong object.
     * @return The parsed latitude and longitude.
     */
    private fun parseLatLong(reader: JsonReader): LatLong {
      var lat = 0.0
      var lon = 0.0
      while (reader.hasNext()) {
        when (reader.nextName()) {
          "lat" -> lat = reader.nextDouble()
          "lon" -> lon = reader.nextDouble()
          else -> reader.skipValue()
        }
      }
      return LatLong(lat, lon)
    }
  }
}
