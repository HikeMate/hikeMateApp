package ch.hikemate.app.model.overpassAPI

import android.util.Log
import ch.hikemate.app.ui.map.MapScreen
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Helper Repository for making requests to the Overpass API. This Repository provides useful
 * constants and methods for making requests to the Overpass API.
 */
abstract class OverpassRepository {

  companion object {
    /** The URL of the Overpass API interpreter. */
    private const val OVERPASS_API_URL: String = "https://overpass-api.de/api/interpreter"

    /** The type of format to request from the Overpass API, written in OverpassQL. */
    const val JSON_OVERPASS_FORMAT_TAG = "[out:json];"

    /**
     * The maximum number of hikes that can be requested in a single Overpass API request
     *
     * We limit in the [MapScreen], but since some hikes are filtered out, we need to request more
     * hikes than we actually want to display.
     */
    private const val MAX_HIKES_PER_REQUEST = MapScreen.MAX_HIKES_DRAWN_ON_MAP * 3

    /** The output modifier for the Overpass API to return geometry information. */
    const val GEOM_OUTPUT_MODIFIER = "out geom $MAX_HIKES_PER_REQUEST;"
  }

  /**
   * Sets a standard header for API requests.
   *
   * @param request Request builder instance to which headers will be added
   */
  protected open fun setRequestHeaders(request: Request.Builder) {
    request.header("User-Agent", "Hikemate/1.0")
  }

  protected open fun buildAndSendRequest(
      requestData: String,
      client: OkHttpClient,
      callback: Callback
  ) {
    val requestBuilder = Request.Builder().url("$OVERPASS_API_URL?data=$requestData").get()

    setRequestHeaders(requestBuilder)

    Log.d("OverpassRepository", "Sending request to Overpass API: ${requestBuilder.build()}")

    client.newCall(requestBuilder.build()).enqueue(callback)
  }
}
