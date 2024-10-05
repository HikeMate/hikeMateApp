package ch.hikemate.app.model.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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

  /** Gets all the routes from the database and updates the routes_ variable */
  fun getRoutes() {
    // TODO: should call the repository to get all the routes
    // repository.getRoutes(onSuccess = { routes_.value = it }, onFailure = {})

    hikeRoutes_.value = listOf("Route 1", "Route 2", "Route 3")
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
