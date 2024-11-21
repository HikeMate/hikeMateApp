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

  private suspend fun getFacilitiesAsync(
      bounds: Bounds,
      onSuccess: (List<Facility>) -> Unit,
      onFailure: (Exception) -> Unit
  ) {
    withContext(dispatcher) {

      // Check cache. If the bounds are already contained in the cache, fetch facilities from cache
      // instead of the API
      for (cacheItem in _cache) {
        if (cacheItem.key.containsBounds(
            bounds)) { // If the bounds are contained by the cached bounds
          val filteredFacilities =
              cacheItem.value.filter { facility ->
                bounds.containsCoordinate(
                    facility.coordinates.latitude, facility.coordinates.longitude)
              }
          onSuccess(filteredFacilities)
          return@withContext
        }
      }

      facilitiesRepository.getFacilities(
          bounds = bounds,
          onSuccess = { facilities ->
            _cache[bounds] = facilities
            onSuccess(facilities)
          },
          onFailure = onFailure)
    }
  }

  fun getFacilities(
      bounds: Bounds,
      onSuccess: (List<Facility>) -> Unit,
      onFailure: (Exception) -> Unit
  ) {
    viewModelScope.launch { getFacilitiesAsync(bounds, onSuccess, onFailure) }
  }
}
