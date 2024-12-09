package ch.hikemate.app.model.facilities

import android.util.Log
import androidx.lifecycle.ViewModel
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
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint


class FacilitiesViewModel(
    private val facilitiesRepository: FacilitiesRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    companion object {
        const val MIN_ZOOM_FOR_FACILITIES = 13.0
        const val MAX_DISTANCE_FROM_ROUTE_TO_FACILITY = 3000
        val MAX_FACILITIES_PER_ZOOM = mapOf(
            (13.0..14.0) to 5,
            (14.0..15.0) to 10,
            (15.0..16.0) to 15,
            (16.0..17.0) to 20,
            (17.0..18.0) to 25,
            (18.0..19.0) to 30
        )
    }

    private val _cache = mutableMapOf<Bounds, List<Facility>>()
    private val _filteredFacilities = MutableStateFlow<List<Facility>>(emptyList())
    val filteredFacilities = _filteredFacilities.asStateFlow()


    fun filterFacilitiesForDisplay(
        facilities: List<Facility>,
        bounds: BoundingBox,
        zoomLevel: Double,
        hikeRoute: DetailedHike,
        onSuccess: (List<Facility>)->Unit,
        onFail: () -> Unit
    ) {
        viewModelScope.launch(dispatcher) {
            if (zoomLevel < MIN_ZOOM_FOR_FACILITIES) {
                _filteredFacilities.value = emptyList()
                onFail()
                return@launch
            }

            val centerPoint = LatLong(bounds.centerLatitude, bounds.centerLongitude)
            val distanceFromRoute = LocationUtils.projectLocationOnHike(centerPoint, hikeRoute)
                ?.distanceFromRoute ?: run {
                onFail()
                return@launch
            }
            if (distanceFromRoute > MAX_DISTANCE_FROM_ROUTE_TO_FACILITY) {
                _filteredFacilities.value = emptyList()
                onFail()
                return@launch
            }

            val maxFacilities: Int = MAX_FACILITIES_PER_ZOOM.filterKeys { zoomLevel in it }.values.firstOrNull() ?: 0
            // We make a list with facilities that are contained into the current bounds and
            // don't
            // surpass the max amount of facilities to not draw too many facilities.
            val filteredFacilities = mutableListOf<Facility>()
            for (facility in facilities) {
                if (filteredFacilities.size >= maxFacilities)
                    break
                if (bounds.contains(GeoPoint(facility.coordinates.lat, facility.coordinates.lon))) {
                    filteredFacilities.add(facility)
                }
            }

            if(filteredFacilities.isEmpty()){
                return@launch
            }
            onSuccess(filteredFacilities)
            _filteredFacilities.value = filteredFacilities
        }
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
                            bounds.containsCoordinate(
                                facility.coordinates.lat,
                                facility.coordinates.lon
                            )
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
                onFailure = onFailure
            )
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
        viewModelScope.launch {
            getFacilitiesAsync(bounds, onSuccess, onFailure)
        }
    }


}
