package ch.hikemate.app.model.route

import android.util.JsonReader
import android.util.Log
import ch.hikemate.app.R
import java.io.Reader
import okhttp3.OkHttpClient
import okhttp3.Request

/** The URL of the Overpass API interpreter. */
private const val OVERPASS_API_URL: String = "https://overpass-api.de/api/interpreter"

/** The type of format to request from the Overpass API, written in OverpassQL. */
private const val JSON_OVERPASS_FORMAT_TAG = "[out:json]"

/** The maximum distance between two points to consider them as the same point. */
private const val MAX_DISTANCE_BETWEEN_OPENED_PATH = 10 // meters

/**
 * Overpass implementation of the hiking route provider repository.
 *
 * This implementation uses the Overpass API to fetch the hiking routes from OSM.
 *
 * @see <a href="https://dev.overpass-api.de/overpass-doc/">Overpass API documentation</a>
 */
class HikeRoutesRepositoryOverpass(val client: OkHttpClient) : HikeRoutesRepository {
  private val cachedHikeRoutes = mutableMapOf<Bounds, List<HikeRoute>>()

  /** @return The size of the cache. */
  fun getCacheSize(): Int {
    return cachedHikeRoutes.size
  }

  override fun getRoutes(
      bounds: Bounds,
      onSuccess: (List<HikeRoute>) -> Unit,
      onFailure: (Exception) -> Unit
  ) {

    // Check if the cache contains the bounds
    // If so just return the content of the cache
    cachedHikeRoutes.forEach {
      if (it.key.containsBounds(bounds)) {
        val filteredRoutes = it.value.filter { route -> bounds.intersectsBounds(route.bounds) }
        onSuccess(filteredRoutes)
        return
      }
    }

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

    client
        .newCall(requestBuilder.build())
        .enqueue(OverpassResponseHandler(bounds, onSuccess, onFailure))
  }

  override fun getRouteById(
      routeId: String,
      onSuccess: (HikeRoute) -> Unit,
      onFailure: (Exception) -> Unit
  ) {
    // Query the Overpass API for this specific route
    val overpassRequestData =
        """
        ${JSON_OVERPASS_FORMAT_TAG};
        relation($routeId);
        out geom;
      """
            .trimIndent()

    val requestBuilder = Request.Builder().url("$OVERPASS_API_URL?data=$overpassRequestData").get()

    setRequestHeaders(requestBuilder)

    client.newCall(requestBuilder.build()).enqueue(OverpassSingleRouteHandler(onSuccess, onFailure))
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
   * @param requestedBounds The bounds that were requested. If null, the cache is not updated.
   * @param onSuccess The callback to be called when the routes are successfully fetched.
   * @param onFailure The callback to be called when the routes could not be fetched.
   */
  private open inner class OverpassResponseHandler(
      val requestedBounds: Bounds?,
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

      if (requestedBounds != null) {
        cachedHikeRoutes[requestedBounds] = routes
      }

      onSuccess(routes)
    }

    /**
     * Flattens a hike route into a list of points.
     *
     * This function is used to convert a hike route into a list of points that can be used to draw
     * a polyline on the map. It also finds the starting and ending points of each sub way to create
     * a smooth path.
     *
     * @param hikeWays The hike ways to be flattened.
     * @return The list of points that represent the hike route. An empty list is returned either if
     *   the hike is considered invalid or if there are no points in the hike.
     */
    private fun flattenHikeWays(hikeWays: List<HikeWay>): HikeWay {
      val points = mutableListOf<LatLong>()
      var status: Boolean
      for (i in hikeWays.indices) {
        // Since the first way doesn't have a previous way to connect to, we need to handle it
        // differently
        if (points.isEmpty()) {
          status = handleFirstWay(hikeWays, points)
        } else {
          status = handleSubsequentWays(hikeWays, points, i)
        }

        // If the status is false, i.e. something wrong happened, we discard the hike
        if (!status) {
          return emptyList()
        }
      }
      return points
    }

    /**
     * Handles the first way of the hike during the flattening process.
     *
     * @param hikeWays The list of hike ways.
     * @param points The list of points to add the way to.
     * @return True if the way has been added to the points, false otherwise.
     */
    private fun handleFirstWay(hikeWays: List<HikeWay>, points: MutableList<LatLong>): Boolean {
      return if (hikeWays.size > 1 &&
          (hikeWays.first().first() == hikeWays[1].first() ||
              hikeWays.first().first() == hikeWays[1].last())) {
        points.addAll(hikeWays.first().reversed())
      } else {
        points.addAll(hikeWays.first())
      }
    }

    /**
     * Handles the subsequent ways of the hike during the flattening process.
     *
     * @param hikeWays The list of hike ways.
     * @param points The list of points to add the way to.
     * @param i The index of the way to handle.
     * @return True if the way has been added to the points, false otherwise.
     */
    private fun handleSubsequentWays(
        hikeWays: List<HikeWay>,
        points: MutableList<LatLong>,
        i: Int
    ): Boolean {
      return when {
        hikeWays[i].first() == points.last() ->
            points.addAll(hikeWays[i].subList(1, hikeWays[i].size))
        hikeWays[i].last() == points.last() ->
            points.addAll(hikeWays[i].reversed().subList(1, hikeWays[i].size))
        else -> handleUnconnectedWays(hikeWays, points, i)
      }
    }

    /**
     * Handles the unconnected ways of the hike during the flattening process.
     *
     * @param hikeWays The list of hike ways.
     * @param points The list of points to add the way to.
     * @param i The index of the way to handle.
     * @return True if the way has been added to the points, false otherwise.
     */
    private fun handleUnconnectedWays(
        hikeWays: List<HikeWay>,
        points: MutableList<LatLong>,
        i: Int
    ): Boolean {
      val lastPoint = points.last()
      val firstWayPoint = hikeWays[i].first()
      val lastWayPoint = hikeWays[i].last()
      val distanceFirst = firstWayPoint.distanceTo(lastPoint)
      val distanceLast = lastWayPoint.distanceTo(lastPoint)

      return when {
        distanceFirst < distanceLast && distanceFirst < MAX_DISTANCE_BETWEEN_OPENED_PATH ->
            points.addAll(hikeWays[i])
        distanceFirst >= distanceLast && distanceLast < MAX_DISTANCE_BETWEEN_OPENED_PATH ->
            points.addAll(hikeWays[i].reversed())
        else -> false // Discard the hikes if unconnected
      }
    }

    /**
     * Parses the routes from the Overpass API response.
     *
     * @param responseReader The reader for the response body.
     * @return The list of parsed routes.
     */
    private fun parseRoutes(responseReader: Reader): List<HikeRoute> {
      val routes = mutableListOf<HikeRoute?>()
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
      return routes.filterNotNull()
    }

    /**
     * Parses a single element from the Overpass API response. The reader is supposed to already be
     * in the element object.
     *
     * @param elementReader The reader for the element object.
     * @return The parsed route or null if the route has been discarded.
     */
    private fun parseElement(elementReader: JsonReader): HikeRoute? {
      var id = ""
      val boundsBuilder = Bounds.Builder()
      val ways = mutableListOf<HikeWay>()
      var elementName: String? = null
      var description: String? = null
      while (elementReader.hasNext()) {
        val name = elementReader.nextName()
        when (name) {
          "id" -> id = elementReader.nextLong().toString()
          "bounds" -> {
            elementReader.beginObject() // We're in the bounds object of the element
            while (elementReader.hasNext()) {
              when (elementReader.nextName()) {
                "minlat" -> boundsBuilder.setMinLat(elementReader.nextDouble())
                "minlon" -> boundsBuilder.setMinLon(elementReader.nextDouble())
                "maxlat" -> boundsBuilder.setMaxLat(elementReader.nextDouble())
                "maxlon" -> boundsBuilder.setMaxLon(elementReader.nextDouble())
                else -> elementReader.skipValue()
              }
            }
            elementReader.endObject()
          }
          "members" -> {
            elementReader.beginArray() // We're in the members array of the element
            while (elementReader.hasNext()) {
              elementReader.beginObject() // We're in a member object
              ways.addAll(parseMember(elementReader))
              elementReader.endObject()
            }
            elementReader.endArray()
          }
          "tags" -> {
            elementReader.beginObject() // We're in the tags object of the element
            parseTags(elementReader).let { pair ->
              elementName = pair.first
              description = pair.second
            }
            elementReader.endObject()
          }
          else -> elementReader.skipValue()
        }
      }
      val flattenWays = flattenHikeWays(ways)
      // If there is no way in the route regardless of the reason, we discard the route
      if (flattenWays.isEmpty()) {
        return null
      }
      return HikeRoute(id, boundsBuilder.build(), flattenWays, elementName, description)
    }

    /**
     * Parses the tags from the Overpass API response. The reader is supposed to already be in the
     * tags object.
     *
     * @param tagsReader The reader for the tags object.
     * @return The name and description of the route, in a Pair, the name first, the description
     *   after.
     */
    private fun parseTags(tagsReader: JsonReader): Pair<String?, String?> {
      // The name of the route can be in multiple tags, we'll try to get the most relevant one
      // The usual tag is the name tag, but if it's not present, we'll try the name:fr tag, then
      // alternative tags...
      // Since OSM is a public database, we can't be sure of the tags used, so we'll try to get the
      // most relevant one
      var name: String? = null
      var description: String? = null
      var nameEn: String? = null
      var otherName: String? = null
      var from: String? = null
      var to: String? = null
      while (tagsReader.hasNext()) {
        when (tagsReader.nextName()) {
          // int_name is used for international names
          "int_name" -> name = tagsReader.nextString()
          // int_name has priority over name
          "name" -> if (name == null) name = tagsReader.nextString() else tagsReader.skipValue()
          "name:en" -> nameEn = tagsReader.nextString()
          "osmc:name",
          "operator",
          "symbol" ->
              if (otherName == null) otherName = tagsReader.nextString() else tagsReader.skipValue()
          "from" -> from = tagsReader.nextString()
          "to" -> to = tagsReader.nextString()
          "description" -> description = tagsReader.nextString()
          else -> tagsReader.skipValue()
        }
      }

      val finalName =
          when {
            name != null -> name
            nameEn != null -> nameEn
            otherName != null -> otherName
            from != null && to != null -> "$from - $to"
            from != null -> "${R.string.hike_name_from_prefix} $from"
            to != null -> "${R.string.hike_name_to_prefix} $to"
            else -> null
          }

      // If the description is not set, we'll set it to the from - to, if available
      // We also assert that the name is not the same as the description
      if (description == null && from != null && to != null && finalName != "$from - $to") {
        description = "$from - $to"
      }

      if (finalName == null) {
        Log.w(this.javaClass::class.simpleName, "No name found for route")
      }
      return Pair(finalName, description)
    }

    /**
     * Parses a member from the Overpass API response. The reader is supposed to already be in the
     * member object.
     *
     * @param memberReader The reader for the member object.
     * @return The list of parsed points for this member.
     */
    private fun parseMember(memberReader: JsonReader): List<HikeWay> {
      val ways = mutableListOf<HikeWay>()
      while (memberReader.hasNext()) {
        val name = memberReader.nextName()
        if (name == "type") {
          val type = memberReader.nextString()
          if (type == "way") {
            ways.add(parseWay(memberReader))
          }
        } else {
          memberReader.skipValue()
        }
      }

      return ways
    }

    /**
     * Parses a way from the Overpass API response. The reader is supposed to already be in the way
     * object.
     *
     * @param wayReader The reader for the way object.
     * @return The list of parsed points for this way.
     */
    private fun parseWay(wayReader: JsonReader): HikeWay {
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

  /**
   * The response handler for the Overpass API request.
   *
   * @param onHike To be called if a single route is found.
   * @param noSingleHike To be called if no or multiple routes are found.
   */
  private inner class OverpassSingleRouteHandler(
      val onHike: (HikeRoute) -> Unit,
      val noSingleHike: (Exception) -> Unit,
  ) :
      OverpassResponseHandler(
          requestedBounds = null,
          onSuccess = { routes ->
            when {
              routes.isEmpty() -> noSingleHike(Exception("No route found"))
              routes.size != 1 -> noSingleHike(Exception("Multiple routes found"))
              else -> onHike(routes.first())
            }
          },
          onFailure = { e -> noSingleHike(e) })
}
