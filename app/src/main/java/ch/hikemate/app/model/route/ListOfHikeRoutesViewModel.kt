package ch.hikemate.app.model.route

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ch.hikemate.app.model.elevation.ElevationService
import ch.hikemate.app.model.elevation.ElevationServiceRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.osmdroid.util.BoundingBox

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
      hikeRoutesRepository.getRoutes(
          bounds = area.toBounds(),
          onSuccess = { routes ->
            hikeRoutes_.value = routes
            onSuccess()
          },
          onFailure = { exception ->
            Log.d(LOG_TAG, "[getRoutesAsync] Failed to get routes: $exception")
            onFailure()
          })
    }
  }

  /** Gets all the routes from the database and updates the routes_ variable */
  fun getRoutes(onSuccess: () -> Unit = {}, onFailure: () -> Unit = {}) {
    viewModelScope.launch { getRoutesAsync(onSuccess = onSuccess, onFailure = onFailure) }
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
  }
}
