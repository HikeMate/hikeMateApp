package ch.hikemate.app.model.route

import androidx.lifecycle.ViewModel
import ch.hikemate.app.model.elevation.ElevationService
import ch.hikemate.app.model.route.saved.SavedHikesRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.osmdroid.util.BoundingBox

/**
 * View model to work with hikes.
 *
 * This view model is responsible for a central part of the app's functionality, it handles hikes.
 *
 * @param savedHikesRepo The repository to work with saved hikes.
 * @param osmHikesRepo The repository to work with hikes from OpenStreetMap.
 * @param elevationRepo The service to retrieve elevation data.
 * @param dispatcher The dispatcher to be used to launch coroutines.
 */
class HikesViewModel(
    private val savedHikesRepo: SavedHikesRepository,
    private val osmHikesRepo: HikeRoutesRepository,
    private val elevationRepo: ElevationService,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {
  private val _hikeFlowsMap = mutableMapOf<String, MutableStateFlow<Hike>>()

  private val _loading = MutableStateFlow<Boolean>(false)

  private val _hikeFlowsList = MutableStateFlow<List<StateFlow<Hike>>>(emptyList())

  private fun updateHikeFlowsList() {
    _hikeFlowsList.value = _hikeFlowsMap.values.toList()
  }

  private val _selectedHike = MutableStateFlow<Hike?>(null)

  /**
   * Whether a new list of hikes is currently being retrieved.
   *
   * Set to true when the view model is performing an update of the whole list. For example, this
   * can happen when calling [loadHikesInBounds] or [loadSavedHikes].
   *
   * Individual updates to particular hikes are more frequent and will not set this value to true.
   *
   * The state can be used to display a loading animation to the user or signify in any way that a
   * loading is performed in the background.
   *
   * Note: this value is ALSO set to true when calling [retrieveLoadedHikesOsmData].
   */
  val loading: StateFlow<Boolean> = _loading.asStateFlow()

  /**
   * The list of hikes currently loaded in the view model.
   *
   * For example, this could be the list of hikes in a certain rectangle on the map (bounds), or the
   * list of saved hikes. Basically contains the list of hikes to be currently displayed in the UI.
   *
   * This list is a state flow, so it can be observed for the UI to update directly when the list's
   * value changes.
   *
   * Each hike is also a state flow. This allows for more granular updates of the UI. If one
   * particular hike is updated, there is no need to update the whole list.
   */
  val hikeFlows: StateFlow<List<StateFlow<Hike>>> = _hikeFlowsList.asStateFlow()

  /**
   * The currently selected hike. Null if no hike is selected.
   *
   * If a hike is selected, it is also necessarily in [hikeFlows]. The two instances receive the
   * same updates, no need to observe both [selectedHike] and the hike in [hikeFlows].
   */
  val selectedHike: StateFlow<Hike?> = _selectedHike.asStateFlow()

  /**
   * Sets the selected hike to the provided one.
   *
   * Note that a hike corresponding to the provided ID must already be loaded (in [hikeFlows]),
   * otherwise the operation will fail.
   *
   * @param hikeId The ID of the hike to select.
   * @param onSuccess Callback for when the operation succeeds. Called after the [selectedHike]
   *   observable has been updated.
   * @param onFailure Callback for when the operation fails, mostly the provided ID does not
   *   correspond to any loaded hike.
   */
  fun selectHike(hikeId: String, onSuccess: () -> Unit, onFailure: () -> Unit) {
    // TODO : Implement HikesViewModel.selectHike
  }

  /**
   * Sets [selectedHike] to null.
   *
   * Note: the currently selected hike will NOT be removed from [hikeFlows], only [selectedHike]
   * will be impacted.
   */
  fun unselectHike() {
    // TODO : Implement HikesViewModel.unselectHike
  }

  /**
   * Loads the current user's saved hikes and replaces [hikeFlows] with those.
   *
   * The loaded hikes will only contain minimal data. To load more data, see
   * [retrieveLoadedHikesOsmData] and [retrieveElevationDataFor].
   *
   * @param onSuccess To be called when the saved hikes have been loaded successfully.
   * @param onFailure Will be called if an error is encountered.
   */
  fun loadSavedHikes(onSuccess: () -> Unit, onFailure: () -> Unit) {
    // TODO : Implement HikesViewModel.loadSavedHikes
  }

  /**
   * Marks a hike as saved by the current user.
   *
   * Once saved, updates the corresponding hike in [hikeFlows]. Does not load/reload the user's
   * saved hikes list.
   *
   * @param hikeId The ID of the hike to mark as saved. Must correspond to one of the loaded hikes
   *   in [hikeFlows].
   * @param onSuccess To be called if the hike is successfully marked as saved.
   * @param onFailure To be called if a problem is encountered and the hike cannot be saved.
   */
  fun saveHike(hikeId: String, onSuccess: () -> Unit, onFailure: () -> Unit) {
    // TODO : Implement HikesViewModel.saveHike
  }

  /**
   * Unmarks a hike as saved by the current user.
   *
   * Once removed from the saved hikes, updates the corresponding flow in [hikeFlows]. Does not
   * load/reload the user's saved hikes list.
   *
   * @param hikeId The ID of the hike to mark as saved. Must correspond to one of the loaded hikes
   *   in [hikeFlows].
   * @param onSuccess To be called if the hike is successfully unsaved.
   * @param onFailure To be called if a problem is encountered and the hike cannot be unsaved.
   */
  fun unsaveHike(hikeId: String, onSuccess: () -> Unit, onFailure: () -> Unit) {
    // TODO : Implement HikesViewModel.unsaveHike
  }

  /**
   * Replaces the current hikes in [hikeFlows] with hikes loaded from OSM in the provided bounds.
   *
   * Calling this function only launches a request asynchronously, the function will return before
   * the hikes have actually been updated. Observe [hikeFlows] to obtain the changed list.
   *
   * @param bounds The minimum and maximum latitude/longitude of the rectangle to search in.
   * @param onSuccess To be called when hikes have been successfully loaded.
   * @param onFailure Will be called if an error is encountered.
   */
  fun loadHikesInBounds(bounds: BoundingBox, onSuccess: () -> Unit, onFailure: () -> Unit) {
    // TODO : Implement HikesViewModel.loadHikesInBounds
  }

  /**
   * Retrieves the bounding box and way points of the currently loaded hikes.
   *
   * Filters the [hikeFlows] list to only include hikes that have not yet been loaded from OSM.
   * Retrieves those hikes' bounding boxes and way points from the OSM repository.
   *
   * The bounding box and way points are then stored in the respective hikes, the state flows are
   * updated directly, no need for a return value.
   *
   * This is a one-time operation, the OSM data will only be retrieved for the hikes that are in the
   * list at the time of calling this function.
   *
   * @param onSuccess To be called once the data has been retrieved successfully. Do not use this
   *   callback to retrieve the data manually. Instead, observe [hikeFlows] to be notified
   *   automatically.
   * @param onFailure To be called if a problem is encountered, preventing the success of the
   *   operation.
   */
  fun retrieveLoadedHikesOsmData(onSuccess: () -> Unit, onFailure: () -> Unit) {
    // TODO : Implement HikesViewModel.retrieveLoadedHikesOsmData
  }

  /**
   * Retrieves the elevation data for a hike.
   *
   * Requires the hike's way points list to have been loaded before. If the way points are not
   * available, the elevation data cannot be retrieved, and [onFailure] will be called.
   *
   * If this function is called several times for the same hike, it will only send a request the
   * first time and mark the data as requested (see [DeferredData] and [Hike] for more information).
   *
   * @param hike The hike to retrieve the elevation data for.
   * @param onSuccess To be called once the elevation has been successfully retrieved for the hike.
   *   Do not use this to retrieve the elevation, rather observe the hike in [hikeFlows] to be
   *   notified automatically.
   * @param onFailure To be called if a problem is encountered and prevents the elevation from being
   *   retrieved.
   */
  fun retrieveElevationDataFor(hike: Hike, onSuccess: () -> Unit, onFailure: () -> Unit) {
    // TODO : Implement HikesViewModel.retrieveElevationDataFor
  }

  /**
   * Sets the planned date of a hike.
   *
   * If the hike is not saved yet and the provided date is not null (i.e. the user wants to save the
   * hike), the hike will be saved with the provided date.
   *
   * If the hike is already saved, the hike will be updated with the new date.
   *
   * Setting a null date on an unsaved hike will have no effect.
   *
   * @param hikeId The ID of the hike to set the planned date of.
   * @param date The planned date for the hike.
   * @param onSuccess To be called if the hike's date is successfully updated.
   * @param onFailure To be called if a problem prevents the operation's success.
   */
  fun setPlannedDate(
      hikeId: String,
      date: Timestamp?,
      onSuccess: () -> Unit,
      onFailure: () -> Unit
  ) {
    // TODO : Implement HikesViewModel.setPlannedDate
  }
}
