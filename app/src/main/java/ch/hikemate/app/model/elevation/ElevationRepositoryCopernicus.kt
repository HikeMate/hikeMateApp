package ch.hikemate.app.model.elevation

import android.util.Log
import ch.hikemate.app.model.route.LatLong
import java.io.IOException
import java.util.Date
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json.Default.decodeFromString
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

/**
 * Serializable data class for the response from the Elevation API
 *
 * @param elevations The list of elevations. An elevation returned as null means that the API
 *   couldn't find the elevation for the corresponding coordinate
 */
@Serializable data class ElevationResponse(val elevations: List<Double?>)

/**
 * A data class that represents a cache entry for the elevation data
 *
 * @param timestamp The timestamp of the cache entry (i.e when the data was added to the cache)
 * @param elevation The elevation data
 */
data class ElevationCacheEntry(val timestamp: Date, val elevation: Double)

/**
 * A request for the ElevationRepository
 *
 * @param coordinates The list of coordinates to get the elevation of
 * @param onSuccess The callback to be called when the request is successful
 * @param onFailure The callback to be called when the request fails
 */
data class ElevationRequest(
    val coordinates: List<LatLong>,
    val onSuccess: (List<Double>) -> Unit,
    val onFailure: (Exception) -> Unit
)

/**
 * A part of a request for the ElevationRepository
 *
 * @param coordinates The chunk of coordinates to get the elevation of
 * @param associatedRequests The [ElevationRequest]s that are associated with this chunk
 */
data class ElevationRequestChunk(
    val coordinates: List<LatLong>,
    val associatedRequests: Set<ElevationRequest>
)

/**
 * A repository for the ElevationRepository. This class is responsible for making the network
 * request
 */
class ElevationRepositoryCopernicus(
    private val client: OkHttpClient,
    private val repoDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val maxCacheSize: Int = DEFAULT_CACHE_MAX_SIZE
) : ElevationRepository {

  companion object {
    // The base URL for the Elevation API
    private const val BASE_URL = "https://www.elevation-api.eu/v1/elevation?pts="

    // We need to wait some time before updating the cache to allow for multiple requests to be
    // batched together
    private const val WAITING_DELAY_BEFORE_SENDING_REQUEST_FOR_BATCHING = 500L

    // Limit the cache size to avoid memory issues
    // This has been tested with a cache size of 150000, on a Google Pixel 8 Pro, without any issues
    private const val DEFAULT_CACHE_MAX_SIZE = 100000

    // The maximum number of failed requests before giving up
    private const val MAX_FAILED_REQUESTS = 5

    // Delay to add per failed request
    private const val FAILED_REQUEST_DELAY = 1000L

    // The max number of coordinates that can be sent in a single request
    const val MAX_COORDINATES_PER_REQUEST = 500

    // The log tag
    private val LOG_TAG = this::class.simpleName
  }

  // Cache for the elevation data, indexed by hike ID
  private val cache = hashMapOf<LatLong, ElevationCacheEntry>()

  // List of requests that are waiting for the cache to be updated
  private val requests = mutableListOf<ElevationRequest>()

  // A mutex to ensure that the cache and requests are accessed atomically
  private val mutex: Mutex = Mutex()

  // The job performing the cache update
  private var cacheUpdateJob: Job? = null

  /**
   * Get the size of the cache
   *
   * @return The size of the cache
   */
  fun getCacheSize(): Int {
    return cache.size
  }

  override fun getElevation(
      coordinates: List<LatLong>,
      onSuccess: (List<Double>) -> Unit,
      onFailure: (Exception) -> Unit
  ) {
    if (coordinates.isEmpty()) {
      onSuccess(emptyList())
      return
    }

    CoroutineScope(repoDispatcher).launch {
      mutex.withLock {
        requests.add(ElevationRequest(coordinates, onSuccess, onFailure))

        if (cacheUpdateJob == null || cacheUpdateJob?.isCompleted == true) {
          launchUpdateOfCache()
        }
      }
    }
  }

  /**
   * Launch the update of the cache
   *
   * The update will be delayed by some time to allow for multiple requests to be batched together
   */
  private fun launchUpdateOfCache() {

    if (requests.isEmpty()) return

    // We need to start a new job to update the cache
    cacheUpdateJob =
        CoroutineScope(repoDispatcher).launch {
          // Delay the update by some amount of time to allow for multiple requests
          // to be batched together
          // This is purely empirical and can be adjusted
          // If we already have enough data for a request, we don't need to wait
          var numberOfCoordinates: Long
          var chunks: List<ElevationRequestChunk>
          mutex.withLock {
            clearRequestsUsingCache()
            val parsingResult = parseRequestsIntoChunks()
            numberOfCoordinates = parsingResult.first
            chunks = parsingResult.second
            if (numberOfCoordinates < MAX_COORDINATES_PER_REQUEST) {
              mutex.unlock()
              delay(WAITING_DELAY_BEFORE_SENDING_REQUEST_FOR_BATCHING)
              mutex.lock()
              // If there were new requests in the meantime, we need to update the chunks
              clearRequestsUsingCache()
              val newParsingResult = parseRequestsIntoChunks()
              numberOfCoordinates = newParsingResult.first
              chunks = newParsingResult.second
            }
            for (chunk in chunks) {
              // Create the JSON body for the request
              sendChunkRequest(chunk)
            }
          }
        }
  }

  /**
   * Clear the requests using the cache If all the coordinates are in the cache, we can return the
   * data directly
   *
   * Warning: This function is not locking the mutex!
   */
  private fun clearRequestsUsingCache() {
    val clearedRequests = mutableListOf<ElevationRequest>()

    requests.forEach { req ->
      if (req.coordinates.minus(cache.keys).isEmpty()) {
        Log.d(LOG_TAG, "All coordinates are in the cache")
        // All coordinates are in the cache
        CoroutineScope(repoDispatcher).launch {
          req.onSuccess(
              req.coordinates.map {
                val value = cache[it]
                if (value == null) {
                  Log.e(LOG_TAG, "Coordinate is missing in cache")
                  return@map 0.0
                } else {
                  value.elevation
                }
              })
        }
        clearedRequests.add(req)
      }
    }

    requests.removeAll(clearedRequests)
  }

  /**
   * Parse the requests into a list of [ElevationRequestChunk].
   *
   * @return The number of coordinates parsed and the list of [ElevationRequestChunk]
   *
   * Warning: This function is not locking the mutex!
   */
  private fun parseRequestsIntoChunks(): Pair<Long, List<ElevationRequestChunk>> {
    val coordinates: List<LatLong> =
        this.requests.flatMap { it.coordinates }.distinct().minus(cache.keys)
    val requests: List<ElevationRequest> = this.requests.toList()

    val chunks = coordinates.chunked(MAX_COORDINATES_PER_REQUEST)

    val chunksWithRequests = mutableListOf<ElevationRequestChunk>()

    for (chunk in chunks) {
      val associatedRequests =
          requests.filter { it.coordinates.any { latLong -> chunk.contains(latLong) } }.toSet()
      chunksWithRequests.add(
          ElevationRequestChunk(
              chunk,
              associatedRequests))
    }

    return Pair(coordinates.size.toLong(), chunksWithRequests)
  }

  /**
   * Process an [ElevationRequestChunk] and send the request to the Elevation API
   *
   * @param chunkRequest The [ElevationRequestChunk] to process
   * @param failedRequests The number of failed requests for this chunk
   */
  private fun sendChunkRequest(chunkRequest: ElevationRequestChunk, failedRequests: Int = 0) {
    val jsonString = buildJsonArray {
      chunkRequest.coordinates.forEach { coordinate ->
        add(
            buildJsonArray {
              add(coordinate.lat)
              add(coordinate.lon)
            })
      }
    }

    val request = Request.Builder().url(BASE_URL + jsonString.toString()).get().build()

    // Callback for the network request, it also updates the cache
    val onSuccessWithCache: (List<Double?>) -> Unit = parseCacheOnDataRetrieval(chunkRequest)

    val onFailure: (Exception) -> Unit = { e ->
      CoroutineScope(repoDispatcher).launch {
        chunkRequest.associatedRequests.forEach { request ->
          requests.remove(request)
          request.onFailure(e)
        }
      }
    }

    client
        .newCall(request)
        .enqueue(
            ElevationServiceCallback(
                onSuccessWithCache, onFailure, failedRequests, chunkRequest))
  }

  /**
   * Parse the cache on data retrieval
   *
   * @param chunkRequest The chunk request
   * @return A callback that will parse the cache on data retrieval
   */
  private fun parseCacheOnDataRetrieval(
      chunkRequest: ElevationRequestChunk
  ): (List<Double?>) -> Unit = { listOfElevation ->
    CoroutineScope(repoDispatcher).launch {

      // Update the cache with the new data
      mutex.withLock {
        chunkRequest.coordinates.forEachIndexed { index, it ->
          cache[it] = ElevationCacheEntry(Date(), listOfElevation[index] ?: 0.0)
        }

        // Call the onSuccess callback for each request
        chunkRequest.associatedRequests.forEach { request ->
          val elevations = request.coordinates.map { cache[it]?.elevation }
          val notNullElevations = elevations.filterNotNull()
          if (notNullElevations.size != elevations.size) {
            Log.i(LOG_TAG, "Some elevations are missing in cache, waiting for more data")
            return@forEach
          }
          // We ensure onSuccess is called only once
          val removed = requests.remove(request)
          if (removed) {
            CoroutineScope(repoDispatcher).launch { request.onSuccess(notNullElevations) }
          }
        }
      }

      // If the cache is too big, we need to clean it up
      // This is done after returning onSuccess to not have issues with graph being not
      // shown because of the cleanup
      Log.d(LOG_TAG, "Cache size: ${getCacheSize()}")
      if (getCacheSize() > maxCacheSize) {
        launchCacheCleanup()
      }
    }
  }

  /**
   * The cache is limited to [maxCacheSize] entries. If the cache exceeds this size, we remove the
   * oldest entries to free up space.
   */
  private fun launchCacheCleanup() {
    Log.d(LOG_TAG, "Cache cleanup launched")
    CoroutineScope(repoDispatcher).launch {
      mutex.withLock {
        if (cache.size <= maxCacheSize) return@launch

        val sortedCache = cache.toList().sortedBy { it.second.timestamp }

        val deletionTimestamp = sortedCache[sortedCache.size - maxCacheSize].second.timestamp

        // The current cache must keep entries related to currently being processed requests
        val currentRequestCoordinates = requests.flatMap { it.coordinates }

        val kept =
            sortedCache.filter {
              it.second.timestamp > deletionTimestamp ||
                  currentRequestCoordinates.contains(it.first)
            }

        cache.clear()
        cache.putAll(kept)
      }
    }
  }

  /**
   * A callback for the network request to the Elevation API. This callback will parse the response
   *
   * @param onSuccess The callback to be called when the request is successful
   * @param onFailure The callback to be called when the request fails
   * @param failedRequests The number of failed requests
   */
  private inner class ElevationServiceCallback(
      private val onSuccess: (List<Double?>) -> Unit,
      private val onFailure: (Exception) -> Unit,
      private val failedRequests: Int = 0,
      private val processedRequestChunk: ElevationRequestChunk
  ) : okhttp3.Callback {

    override fun onFailure(call: okhttp3.Call, e: IOException) {
      onFailure(e)
    }

    override fun onResponse(call: okhttp3.Call, response: Response) {
      if (response.isSuccessful) {
        val body = response.body?.string()

        if (body == null) {
          response.close()
          onFailure(Exception("Failed to get elevation. Body is null"))
          return
        }

        // This function decodes the JSON response into a ElevationResponse which is a List of
        // ElevationResult
        val elevationResponse = decodeFromString<ElevationResponse>(body)

        // Call the onSuccess callback with the list of elevations
        onSuccess(elevationResponse.elevations)
      } else {
        handleError(response)
      }

      response.close()
    }

    private fun handleError(response: Response) {
      when (response.code) {
        500,
        504,
        429 -> {
          Log.e(
              LOG_TAG,
              "Failed to get elevation. Status code: ${response.code}, retrying, failedRequests: $failedRequests")
          if (failedRequests > MAX_FAILED_REQUESTS) {
            onFailure(Exception("Failed to get elevation. Status code: ${response.code}"))
            return
          } else {
            CoroutineScope(repoDispatcher).launch {
              delay(failedRequests * FAILED_REQUEST_DELAY)
              sendChunkRequest(processedRequestChunk, failedRequests + 1)
            }
          }
        }
        else -> {
          onFailure(Exception("Failed to get elevation. Status code: ${response.code}"))
        }
      }
    }
  }
}
