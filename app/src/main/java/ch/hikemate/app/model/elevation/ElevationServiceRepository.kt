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

private const val BASE_URL = "https://www.elevation-api.eu/v1/elevation?pts="

// We need to wait some time before updating the cache to allow for multiple requests to be
// batched together
private const val CACHE_UPDATE_DELAY = 500L

// Limit the cache size to avoid memory issues
// This has been tested with a cache size of 100000, on a Google Pixel 8 Pro, without any issues
private const val CACHE_MAX_SIZE = 150000

// The maximum number of failed requests before giving up
private const val MAX_FAILED_REQUESTS = 5

// Delay to add per failed request
private const val FAILED_REQUEST_DELAY = 1000L

// The max number of coordinates that can be sent in a single request
private const val MAX_COORDINATES_PER_REQUEST = 500

/** Serializable data class for the response from the Elevation API */
@Serializable data class ElevationResponse(val elevations: List<Double?>)

/**
 * A data class that represents a cache entry for the elevation data
 *
 * @param timestamp The timestamp of the cache entry (i.e when the data was added to the cache)
 * @param elevation The elevation data
 */
data class ElevationCacheEntry(val timestamp: Date, val elevation: Double)

/**
 * A request for the ElevationService
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
 * A repository for the ElevationService. This class is responsible for making the network request
 */
class ElevationServiceRepository(
    private val client: OkHttpClient,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ElevationRepository {

  // The number of failed requests made to the Elevation API since the last successful request
  private var failedRequests = 0

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
  private fun getCacheSize(): Int {
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

    if (coordinates.minus(cache.keys).isEmpty()) {
      // All coordinates are in the cache
      onSuccess(
          coordinates.map {
            val value = cache[it]
            if (value == null) {
              Log.e("ElevationServiceRepository", "Coordinate is missing in cache")
              return@map 0.0
            } else {
              value.elevation
            }
          })
      return
    }

    CoroutineScope(dispatcher).launch {
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
   * The update will be delayed by 1 second to allow for multiple requests to be batched together
   *
   * @param onSuccess The callback to be called when the request is successful
   * @param onFailure The callback to be called when the request fails
   */
  private fun launchUpdateOfCache(
      onSuccess: (List<Double>) -> Unit = {},
      onFailure: (Exception) -> Unit = {}
  ) {

    if (requests.isEmpty()) return

    // We need to start a new job to update the cache
    cacheUpdateJob =
        CoroutineScope(dispatcher).launch {
          // Delay the update by some amount of time to allow for multiple requests
          // to be batched together
          // This is purely empirical and can be adjusted
          // If we already have enough data for a request, we don't need to wait
          var coordinates: List<LatLong> = emptyList()
          mutex.withLock { coordinates = requests.flatMap { it.coordinates } }
          if (coordinates.size < MAX_COORDINATES_PER_REQUEST) {
            delay(CACHE_UPDATE_DELAY)
          }
          mutex.withLock {
            val chunks = coordinates.chunked(MAX_COORDINATES_PER_REQUEST)

            for (chunk in chunks) {
              // Create the JSON body for the request
              val jsonString = buildJsonArray {
                chunk.forEach { coordinate ->
                  add(
                      buildJsonArray {
                        add(coordinate.lat)
                        add(coordinate.lon)
                      })
                }
              }

              val request =
                  okhttp3.Request.Builder().url(BASE_URL + jsonString.toString()).get().build()

              // Callback for the network request, it also updates the cache
              val onSuccessWithCache: (List<Double?>) -> Unit =
                  parseCacheOnDataRetrieval(chunk, onSuccess)

              val onFailureCallback: (Exception) -> Unit = { exception ->
                CoroutineScope(dispatcher).launch {
                  mutex.withLock { requests.forEach { request -> request.onFailure(exception) } }
                  onFailure(exception)
                }
              }

              client
                  .newCall(request)
                  .enqueue(ElevationServiceCallback(onSuccessWithCache, onFailureCallback))
            }
          }
        }
  }

  /**
   * Parse the cache on data retrieval
   *
   * @param chunk The chunk of coordinates that were requested
   * @param onSuccess The callback to be called when the request is successful
   * @return A callback that will parse the cache on data retrieval
   */
  private fun parseCacheOnDataRetrieval(
      chunk: List<LatLong>,
      onSuccess: (List<Double>) -> Unit
  ): (List<Double?>) -> Unit = { listOfElevation ->
    val nonNullListOfElevation = listOfElevation.map { it ?: 0.0 }

    CoroutineScope(dispatcher).launch {

      // Update the cache with the new data
      mutex.withLock {
        chunk.forEachIndexed { index, it ->
          cache[it] = ElevationCacheEntry(Date(), nonNullListOfElevation[index])
        }

        val processedRequests: MutableSet<ElevationRequest> = mutableSetOf()

        // Call the onSuccess callback for each request
        requests.forEach { request ->
          val elevations = request.coordinates.map { cache[it]?.elevation }
          val notNullElevations = elevations.filterNotNull()
          if (notNullElevations.size != elevations.size) {
            Log.e(
                "ElevationServiceRepository",
                "Some elevations are missing in cache, waiting for more data")
            return@forEach
          }
          request.onSuccess(notNullElevations)
          processedRequests += request
        }
        requests.removeAll(processedRequests)
      }

      // If the cache is too big, we need to clean it up
      // This is done after returning onSuccess to not have issues with graph being not
      // shown
      // because of the cleanup
      Log.d("ElevationServiceRepository", "Cache size: ${getCacheSize()}")
      if (getCacheSize() > CACHE_MAX_SIZE) {
        launchCacheCleanup()
        Log.d("ElevationServiceRepository", "Cache cleanup launched")
      }

      onSuccess(nonNullListOfElevation)
    }
  }

  /**
   * The cache is limited to [CACHE_MAX_SIZE] entries. If the cache exceeds this size, we remove the
   * oldest entries to free up space.
   */
  private fun launchCacheCleanup() {
    CoroutineScope(dispatcher).launch {
      mutex.withLock {
        val sortedCache = cache.toList().sortedBy { it.second.timestamp }
        cache.clear()
        cache.putAll(sortedCache.take(CACHE_MAX_SIZE))
      }
    }
  }

  /**
   * A callback for the network request to the Elevation API. This callback will parse the response
   */
  private inner class ElevationServiceCallback(
      private val onSuccess: (List<Double?>) -> Unit,
      private val onFailure: (Exception) -> Unit
  ) : okhttp3.Callback {

    override fun onFailure(call: okhttp3.Call, e: IOException) {
      onFailure(e)
    }

    override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
      if (response.isSuccessful) {
        val body = response.body?.string()

        if (body == null) {
          onFailure(Exception("Failed to get elevation. Body is null"))
          response.close()
          return
        }

        // This function decodes the JSON response into a ElevationResponse which is a List of
        // ElevationResult
        val elevationResponse = decodeFromString<ElevationResponse>(body)

        // Call the onSuccess callback with the list of elevations
        onSuccess(elevationResponse.elevations)
        failedRequests = 0
      } else {
        when (response.code) {
          500,
          504,
          429 -> {
            failedRequests++
            Log.e(
                "ElevationServiceRepository",
                "Failed to get elevation. Status code: ${response.code}, retrying, failedRequests: $failedRequests")
            if (failedRequests > MAX_FAILED_REQUESTS) {
              onFailure(Exception("Failed to get elevation. Status code: ${response.code}"))
              failedRequests = 0
              return
            } else {
              CoroutineScope(dispatcher).launch {
                delay(failedRequests * FAILED_REQUEST_DELAY)
                launchUpdateOfCache()
              }
            }
          }
          413 -> {
            Log.e(
                "ElevationServiceRepository",
                "Failed to get elevation. Status code: ${response.code}, too many coordinates")
            CoroutineScope(dispatcher).launch {
              var firstHalf: List<ElevationRequest>
              var secondHalf: List<ElevationRequest> = emptyList()
              Log.d("ElevationServiceRepository", "Split requests in half")
              mutex.withLock {
                val halves = requests.partition { requests.indexOf(it) <= requests.size / 2 }
                firstHalf = halves.first
                secondHalf = halves.second
                requests.clear()
                requests.addAll(firstHalf)
                Log.d("ElevationServiceRepository", "First half size: ${firstHalf.size}")
              }
              launchUpdateOfCache(
                  onSuccess = {
                    CoroutineScope(dispatcher).launch {
                      mutex.withLock {
                        Log.d("ElevationServiceRepository", "Second half size: ${secondHalf.size}")
                        requests.addAll(secondHalf)
                      }
                      launchUpdateOfCache()
                    }
                  })
            }
          }
          else -> {
            onFailure(Exception("Failed to get elevation. Status code: ${response.code}"))
          }
        }
      }

      response.close()
    }
  }
}
