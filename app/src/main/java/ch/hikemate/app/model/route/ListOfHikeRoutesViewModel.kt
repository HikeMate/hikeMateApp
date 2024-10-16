package ch.hikemate.app.model.route

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.osmdroid.util.BoundingBox

/** ViewModel for the list of hike routes */
open class ListOfHikeRoutesViewModel(private val hikeRoutesRepository: HikeRoutesRepository) : ViewModel() {
  // List of all routes in the database
  private val hikeRoutes_ =
      MutableStateFlow<List<HikeRoute>>(emptyList())
  val hikeRoutes: StateFlow<List<HikeRoute>> = hikeRoutes_.asStateFlow()

  // Selected route, i.e the route for the detail view
  private val selectedHikeRoute_ = MutableStateFlow<HikeRoute?>(null)
  open val selectedHikeRoute: StateFlow<HikeRoute?> = selectedHikeRoute_.asStateFlow()

  private val area_ = MutableStateFlow<BoundingBox?>(null)

  // Creates a factory
  companion object {
    val Factory: ViewModelProvider.Factory =
        object : ViewModelProvider.Factory {
          @Suppress("UNCHECKED_CAST")
          override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ListOfHikeRoutesViewModel(HikeRoutesRepositoryOverpass(OkHttpClient())) as T
          }
        }
  }

  private suspend fun getRoutesAsync() {
    withContext(Dispatchers.IO) {
      val area = area_.value ?: return@withContext
      hikeRoutesRepository.getRoutes(
        bounds = area.toBounds(),
        onSuccess = { routes -> hikeRoutes_.value = routes},
        onFailure = { exception ->
          // TODO : Add feedback for the user when an API error occurs and test it
        }
      )
    }
  }

  /** Gets all the routes from the database and updates the routes_ variable */
  fun getRoutes() {
    viewModelScope.launch(Dispatchers.IO) { getRoutesAsync() }
  }

  /**
   * Sets the current displayed area on the map and updates the list of routes displayed in the
   * list.
   *
   * @param area The area to be displayed
   */
  fun setArea(area: BoundingBox) {
    area_.value = area
    getRoutes()
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
