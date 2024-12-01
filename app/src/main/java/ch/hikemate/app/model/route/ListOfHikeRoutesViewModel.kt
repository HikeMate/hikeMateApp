package ch.hikemate.app.model.route

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ch.hikemate.app.model.elevation.ElevationService
import ch.hikemate.app.model.elevation.ElevationServiceRepository
import ch.hikemate.app.model.extensions.crossesDateLine
import ch.hikemate.app.model.extensions.splitByDateLine
import ch.hikemate.app.model.extensions.toBounds
import ch.hikemate.app.ui.map.MapScreen
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint

/** ViewModel for the list of hike routes */
open class ListOfHikeRoutesViewModel(
    private val hikeRoutesRepository: HikeRoutesRepository,
    private val elevationService: ElevationService,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {
  // List of all routes in the database
  private val hikeRoutes_ = MutableStateFlow<List<HikeRoute>>(emptyList())
  val hikeRoutes: StateFlow<List<HikeRoute>> = hikeRoutes_.asStateFlow()

  // Selected route, i.e the route for the detail view
  private val selectedHikeRoute_ = MutableStateFlow<HikeRoute?>(null)
  open val selectedHikeRoute: StateFlow<HikeRoute?> = selectedHikeRoute_.asStateFlow()

  // So that the map can be maintained when the user navigates between screens with a map
  data class MapViewState(
      val center: GeoPoint? = MapScreen.MAP_INITIAL_CENTER,
      val zoom: Double = MapScreen.MAP_INITIAL_ZOOM,
  )

  private val _mapState = MutableStateFlow(MapViewState())
  val mapState = _mapState.asStateFlow()

  private val area_ = MutableStateFlow<BoundingBox?>(null)

  // Creates a factory and stores the tag used for logging
  companion object {
    val Factory: ViewModelProvider.Factory =
        object : ViewModelProvider.Factory {
          @Suppress("UNCHECKED_CAST")
          override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ListOfHikeRoutesViewModel(
                HikeRoutesRepositoryOverpass(OkHttpClient()),
                ElevationServiceRepository(OkHttpClient()))
                as T
          }
        }

    private const val LOG_TAG = "ListOfHikeRoutesViewModel"
  }

  private suspend fun getRoutesAsync(onSuccess: () -> Unit = {}, onFailure: () -> Unit = {}) {
    withContext(dispatcher) {
      val area = area_.value ?: return@withContext

      // Check if the area is on the date line
      if (area.crossesDateLine()) {
        val (bounds1, bounds2) = area.splitByDateLine()
        hikeRoutesRepository.getRoutes(
            bounds = bounds1.toBounds(),
            onSuccess = { routes1 ->
              hikeRoutesRepository.getRoutes(
                  bounds = bounds2.toBounds(),
                  onSuccess = { routes2 ->
                    hikeRoutes_.value = routes1 + routes2
                    onSuccess()
                  },
                  onFailure = { _ -> onFailure() })
            },
            onFailure = { _ -> onFailure() })
        return@withContext
      }

      hikeRoutesRepository.getRoutes(
          bounds = area.toBounds(),
          onSuccess = { routes ->
            hikeRoutes_.value = routes
            onSuccess()
          },
          onFailure = { exception ->
            Log.e(LOG_TAG, "[getRoutesAsync] Failed to get routes", exception)
            onFailure()
          })
    }
  }

  /** Gets all the routes from the database and updates the routes_ variable */
  fun getRoutes(onSuccess: () -> Unit = {}, onFailure: () -> Unit = {}) {
    viewModelScope.launch { getRoutesAsync(onSuccess = onSuccess, onFailure = onFailure) }
  }

  /** Gets the routes with the given IDs */
  fun getRoutesByIds(
      routeIds: List<String>,
      onSuccess: (List<HikeRoute>) -> Unit = {},
      onFailure: () -> Unit = {}
  ) {
    viewModelScope.launch {
      getRoutesByIdsAsync(routeIds, onSuccess = onSuccess, onFailure = onFailure)
    }
  }

  private suspend fun getRoutesByIdsAsync(
      routeIds: List<String>,
      onSuccess: (List<HikeRoute>) -> Unit = {},
      onFailure: () -> Unit = {}
  ) {
    withContext(dispatcher) {
      hikeRoutesRepository.getRoutesByIds(
          routeIds = routeIds,
          onSuccess = { routes ->
            hikeRoutes_.value = routes
            onSuccess(routes)
          },
          onFailure = { exception ->
            Log.e(LOG_TAG, "[getRoutesFromIds] Failed to get routes", exception)
            onFailure()
          })
    }
  }

  private suspend fun getRoutesElevationAsync(
      route: HikeRoute,
      onSuccess: (List<Double>) -> Unit = {},
      onFailure: () -> Unit = {}
  ) {
    withContext(dispatcher) {
      elevationService.getElevation(
          coordinates = route.ways,
          hikeID = route.id,
          onSuccess = { elevationData -> onSuccess(elevationData) },
          onFailure = { exception ->
            Log.d(LOG_TAG, "[getRoutesElevationAsync] Failed to get elevation data: $exception")
            onFailure()
          })
    }
  }

  /**
   * Gets the elevation data asynchronously for a route and return it as a list of doubles on
   * success.
   */
  fun getRoutesElevation(
      route: HikeRoute,
      onSuccess: (List<Double>) -> Unit = {},
      onFailure: () -> Unit = {}
  ) {
    viewModelScope.launch {
      getRoutesElevationAsync(route, onSuccess = onSuccess, onFailure = onFailure)
    }
  }

  /**
   * Sets the current displayed area on the map and updates the list of routes displayed in the
   * list.
   *
   * @param area The area to be displayed
   */
  fun setArea(area: BoundingBox, onSuccess: () -> Unit = {}, onFailure: () -> Unit = {}) {
    area_.value = area
    getRoutes(onSuccess = onSuccess, onFailure = onFailure)
  }

  /**
   * Selects a route to be displayed in the detail view
   *
   * @param hikeRoute The route to be displayed
   */
  fun selectRoute(hikeRoute: HikeRoute) {
    selectedHikeRoute_.value = hikeRoute
    // Clears map state when new route is selected
  }

  /** Clears the selected route */
  fun clearSelectedRoute() {
    selectedHikeRoute_.value = null
    // Clears map state when route is cleared
  }

  private suspend fun selectRouteByIdAsync(hikeId: String) {
    withContext(dispatcher) {
      hikeRoutesRepository.getRouteById(
          routeId = hikeId,
          onSuccess = { route -> selectedHikeRoute_.value = route },
          onFailure = { exception ->
            Log.e(LOG_TAG, "[selectRouteByIdAsync] Failed to get route", exception)
          })
    }
  }

  /**
   * Selects a particular hike (for example to then display it in the details screen).
   *
   * Use this function if only the route ID is available. If a [HikeRoute] instance is available,
   * use [selectRoute] instead.
   *
   * @param hikeId The ID of the route to be selected
   */
  fun selectRouteById(hikeId: String) {
    viewModelScope.launch { selectRouteByIdAsync(hikeId) }
  }

  fun setMapState(center: GeoPoint, zoom: Double) {
    _mapState.value = MapViewState(center, zoom)
  }

  fun getMapState(): MapViewState {
    return _mapState.value
  }
}
