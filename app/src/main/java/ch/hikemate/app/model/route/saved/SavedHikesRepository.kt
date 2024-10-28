package ch.hikemate.app.model.route.saved

/**
 * Contract for the saved hikes repository. Allows to retrieve a list of hikes the user has saved,
 * add a hike to the list, or remove one.
 *
 * This simple contract was established instead of a paginated, more complex one, to simplify the
 * implementation of the repository and the view model. This choice is based on the assumption that
 * the number of saved hikes per user will be relatively small (probably less than 1000), which can
 * be handled in memory without the need for pagination.
 *
 * The contract also features suspending functions to take advantage of the Kotlin coroutines
 * framework. Instead of onSuccess and onFailure callbacks, the functions return the result directly
 * or throw an exception.
 */
interface SavedHikesRepository {
  /**
   * Loads and returns the list of saved hikes for the current user.
   *
   * @return The list of saved hikes.
   * @throws Exception If an error occurred while loading the hikes.
   */
  suspend fun loadSavedHikes(): List<SavedHike>

  /**
   * Adds a hike to the list of saved hikes for the current user.
   *
   * @param hike The hike to add to the saved hikes list.
   * @throws Exception If an error occurred while adding the hike.
   */
  suspend fun addSavedHike(hike: SavedHike)

  /**
   * Removes a hike from the list of saved hikes for the current user.
   *
   * @param hike The hike to remove from the saved hikes list.
   * @throws Exception If an error occurred while removing the hike.
   */
  suspend fun removeSavedHike(hike: SavedHike)
}
