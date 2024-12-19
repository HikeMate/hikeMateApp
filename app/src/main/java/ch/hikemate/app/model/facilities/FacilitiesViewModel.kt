package ch.hikemate.app.model.facilities

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ch.hikemate.app.model.route.Bounds
import ch.hikemate.app.model.route.DetailedHike
import ch.hikemate.app.model.route.LatLong
import ch.hikemate.app.utils.LocationUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint

class FacilitiesViewModel(
    private val facilitiesRepository: FacilitiesRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

  companion object {
    val Factory: ViewModelProvider.Factory =
        object : ViewModelProvider.Factory {
          @Suppress("UNCHECKED_CAST")
          override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val facilitiesRepository = FacilitiesRepositoryOverpass(OkHttpClient())
            return FacilitiesViewModel(
                facilitiesRepository,
            )
                as T
          }
        }
    const val LOG_TAG = "FacilitiesViewModel"
    const val MARGIN_BOUNDS = 0.001
    const val MIN_ZOOM_FOR_FACILITIES = 13.0
    const val MAX_DISTANCE_FROM_CENTER_BOUNDS_TO_ROUTE = 3000
    val MAX_FACILITIES_PER_ZOOM =
        mapOf(
            (13.0..14.0) to 15,
            (14.0..15.0) to 15,
            (15.0..16.0) to 20,
            (16.0..17.0) to 30,
            (17.0..18.0) to 50)
  }

  private val _isFiltering = MutableStateFlow(false)
  private val _cache = mutableMapOf<Bounds, List<Facility>>()

  private val _facilities = MutableStateFlow<List<Facility>?>(null)
  val facilities = _facilities.asStateFlow()

  /**
   * Filters facilities for display based on the current map view state and zoom level. The function
   * performs several checks to determine which facilities should be displayed:
   * 1. Verifies if the zoom level is sufficient for showing facilities
   * 2. Checks if the center of the view is close enough to the hike route
   * 3. Limits the number of displayed facilities based on the current zoom level
   *
   * The function uses lazy evaluation through sequences to efficiently process facilities, stopping
   * as soon as the maximum number of facilities for the current zoom level is reached.
   *
   * @param bounds The current visible bounds of the map view
   * @param zoomLevel The current zoom level of the map
   * @param hikeRoute The detailed hike route being displayed
   * @param onSuccess Callback invoked with the filtered list of facilities when processing succeeds
   * @param onNoFacilitiesForState Callback invoked when no facilities should be displayed for the
   *   current state
   */
  fun filterFacilitiesForDisplay(
      bounds: BoundingBox,
      zoomLevel: Double,
      hikeRoute: DetailedHike,
      onSuccess: (List<Facility>) -> Unit,
      onNoFacilitiesForState: () -> Unit
  ) {
    viewModelScope.launch(dispatcher) {
      // Prevents the viewModel to do multiple filter operations at once.
      if (_isFiltering.value) return@launch
      try {
        if (facilities.value == null) {
          Log.e(LOG_TAG, "Facilities are null cannot filter them")
          return@launch
        }
        val facilities = facilities.value!!
        // 1. Early returns for invalid states
        if (!isValidZoomAndBounds(zoomLevel, bounds, hikeRoute)) {
          onNoFacilitiesForState()
          return@launch
        }

        // 2. Calculate max facilities once
        val maxFacilities = getMaxFacilitiesForZoom(zoomLevel)
        if (maxFacilities == 0) {
          onNoFacilitiesForState()
          return@launch
        }

        // 3. Use sequence for lazy evaluation and take() for early termination
        val filteredFacilities =
            facilities
                .asSequence()
                .filter { facility ->
                  bounds.contains(GeoPoint(facility.coordinates.lat, facility.coordinates.lon))
                }
                .take(maxFacilities)
                .toList()

        onSuccess(filteredFacilities)
      } catch (e: Exception) {
        Log.e(LOG_TAG, "Error while filtering facilities: $e")
      } finally {
        _isFiltering.value = false
      }
    }
  }

  /**
   * Validates whether facilities should be displayed for the current zoom level and map bounds.
   * This function performs two main checks:
   * 1. Verifies if the zoom level meets the minimum threshold for displaying facilities
   * 2. Ensures the center point of the view is within the maximum allowed distance from the route
   *
   * @param zoomLevel Current zoom level of the map
   * @param bounds Current visible bounds of the map
   * @param hikeRoute The detailed hike route being displayed
   * @return true if facilities should be displayed for the current state, false otherwise
   */
  private fun isValidZoomAndBounds(
      zoomLevel: Double,
      bounds: BoundingBox,
      hikeRoute: DetailedHike
  ): Boolean {
    // This might seem redundant but the reason this is used is so that if the zoom level
    // is not according to the minimum we just return without doing the distance computation
    if (zoomLevel < MIN_ZOOM_FOR_FACILITIES) {
      return false
    }

    val centerPoint = LatLong(bounds.centerLatitude, bounds.centerLongitude)
    val distanceFromRoute =
        LocationUtils.projectLocationOnHike(centerPoint, hikeRoute)?.distanceFromRoute
            ?: return false

    return distanceFromRoute <= MAX_DISTANCE_FROM_CENTER_BOUNDS_TO_ROUTE
  }

  /**
   * Determines the maximum number of facilities that should be displayed for the current zoom
   * level. The function uses a predefined mapping of zoom ranges to facility counts to prevent
   * overcrowding the map at lower zoom levels while allowing more detail at higher zooms.
   *
   * @param zoomLevel Current zoom level of the map
   * @return The maximum number of facilities that should be displayed, or 0 if no facilities should
   *   be shown
   */
  private fun getMaxFacilitiesForZoom(zoomLevel: Double): Int {
    return MAX_FACILITIES_PER_ZOOM.filterKeys { zoomLevel in it }.values.firstOrNull() ?: 0
  }

  /**
   * Asynchronously fetches facilities within specified bounds, utilizing cache when possible. If
   * the requested bounds are contained within previously cached bounds, returns facilities from
   * cache instead of making a new API request.
   *
   * @param bounds Geographical bounds within which to search for facilities
   * @param onSuccess Callback to handle the resulting list of facilities when the operation
   *   succeeds
   * @param onFailure Callback invoked when the operation fails
   */
  private suspend fun getFacilitiesAsync(
      bounds: Bounds,
      onSuccess: (List<Facility>) -> Unit,
      onFailure: (Exception) -> Unit
  ) {
    withContext(dispatcher) {
      // Check cache. If the bounds are already contained in the cache, fetch facilities from cache
      // instead of the API
      for (cacheItem in _cache) {
        if (cacheItem.key.containsBounds(bounds)) {
          // Filters out facilities that are not within the requested bounds
          val filteredFacilities =
              cacheItem.value.filter { facility ->
                bounds.containsCoordinate(facility.coordinates.lat, facility.coordinates.lon)
              }
          onSuccess(filteredFacilities)
          return@withContext
        }
      }

      // If not in cache, fetch from repository
      facilitiesRepository.getFacilities(
          bounds = bounds,
          onSuccess = { facilities ->
            _cache[bounds] = facilities
            onSuccess(facilities)
          },
          onFailure = onFailure)
    }
  }

  /**
   * Public interface for fetching facilities. Launches a coroutine to perform the async operation.
   *
   * @param bounds Geographical bounds within which to search for facilities
   * @param onSuccess Callback to handle the resulting list of facilities when the operation
   *   succeeds
   * @param onFailure Callback invoked with the exception when the operation fails
   */
  fun getFacilities(
      bounds: Bounds,
      onSuccess: (List<Facility>) -> Unit,
      onFailure: (Exception) -> Unit
  ) {
    viewModelScope.launch { getFacilitiesAsync(bounds, onSuccess, onFailure) }
  }

  /**
   * Get all of the facilities encompassed by a hike.
   *
   * @param hike
   * @param onSuccess
   * @param onFailure
   */
  fun fetchFacilitiesForHike(hike: DetailedHike) {
    viewModelScope.launch {
      Log.d(LOG_TAG, "got")
      // This is calculated so that elements that are near but not contained into the bounds of
      // a Hike are still displayed. This is actually a very common occurrence.
      val boundsWithMargin =
          Bounds(
              hike.bounds.minLat - MARGIN_BOUNDS,
              hike.bounds.minLon - MARGIN_BOUNDS,
              hike.bounds.maxLat + MARGIN_BOUNDS,
              hike.bounds.maxLon + MARGIN_BOUNDS)
      // Get all the facilities within the bounds.
      getFacilities(
          boundsWithMargin,
          { _facilities.value = it },
          { Log.e(LOG_TAG, "Error while getting facilities for hike: $it") })
    }
  }
}
