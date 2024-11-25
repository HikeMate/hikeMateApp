package ch.hikemate.app.model.facilities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.hikemate.app.model.route.Bounds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FacilitiesViewModel(
    private val facilitiesRepository: FacilitiesRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

  private val _cache = mutableMapOf<Bounds, List<Facility>>()

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
}
