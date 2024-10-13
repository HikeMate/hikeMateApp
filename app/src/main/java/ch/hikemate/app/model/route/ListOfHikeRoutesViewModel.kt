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
import org.osmdroid.util.BoundingBox

/**
 * ViewModel for the list of hike routes
 *
 * @param TODO: should take a repository as a parameter
 */
open class ListOfHikeRoutesViewModel() : ViewModel() {
  // List of all routes in the database
  private val hikeRoutes_ =
      MutableStateFlow<List<String>>(emptyList()) // TODO: should be a list of Route objects
  val hikeRoutes: StateFlow<List<String>> = hikeRoutes_.asStateFlow()

  // Selected route, i.e the route for the detail view
  private val selectedHikeRoute_ = MutableStateFlow<String?>(null) // TODO: should be a Route object
  open val selectedHikeRoute: StateFlow<String?> = selectedHikeRoute_.asStateFlow()

  private val area_ = MutableStateFlow<BoundingBox?>(null)

  // Creates a factory
  companion object {
    val Factory: ViewModelProvider.Factory =
        object : ViewModelProvider.Factory {
          @Suppress("UNCHECKED_CAST")
          override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ListOfHikeRoutesViewModel() as T
          }
        }
  }

  private suspend fun getRoutesAsync() {
    // TODO: Should call the API repository to get all the routes filtered by the area
    withContext(Dispatchers.IO) { Thread.sleep(100) }
    hikeRoutes_.value = listOf("Route 1", "Route 2", "Route 3")
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
  fun selectRoute(hikeRoute: String) { // TODO: should take a Route object
    selectedHikeRoute_.value = hikeRoute
  }
}
