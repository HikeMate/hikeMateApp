package ch.hikemate.app.model.route

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ch.hikemate.app.R
import ch.hikemate.app.model.elevation.ElevationRepository
import ch.hikemate.app.model.elevation.ElevationRepositoryCopernicus
import ch.hikemate.app.model.extensions.toBounds
import ch.hikemate.app.model.route.saved.SavedHike
import ch.hikemate.app.model.route.saved.SavedHikesRepository
import ch.hikemate.app.model.route.saved.SavedHikesRepositoryFirestore
import ch.hikemate.app.utils.MapUtils
import ch.hikemate.app.utils.RouteUtils
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint

/**
 * View model to work with hikes.
 *
 * This view model is responsible for a central part of the app's functionality, it handles hikes.
 *
 * @param savedHikesRepo The repository to work with saved hikes.
 * @param osmHikesRepo The repository to work with hikes from OpenStreetMap.
 * @param elevationRepository The service to retrieve elevation data.
 * @param dispatcher The dispatcher to be used to launch coroutines.
 */
class HikesViewModel(
    private val savedHikesRepo: SavedHikesRepository,
    private val osmHikesRepo: HikeRoutesRepository,
    private val elevationRepository: ElevationRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {
  companion object {
    val Factory: ViewModelProvider.Factory =
        object : ViewModelProvider.Factory {
          @Suppress("UNCHECKED_CAST")
          override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val client = OkHttpClient()
            return HikesViewModel(
                savedHikesRepo =
                    SavedHikesRepositoryFirestore(
                        FirebaseFirestore.getInstance(), FirebaseAuth.getInstance()),
                osmHikesRepo = HikeRoutesRepositoryOverpass(client),
                elevationRepository = ElevationRepositoryCopernicus(client))
                as T
          }
        }

    private const val LOG_TAG = "HikesViewModel"
  }

  private val _hikesMutex = Mutex()

  private val _savedHikesMap = mutableMapOf<String, SavedHike>()

  private var _hikeFlowsMap = mutableMapOf<String, MutableStateFlow<Hike>>()

  private val _osmDataRetrievalMutex = Mutex()

  private val _allOsmDataLoaded = MutableStateFlow(true)

  private val _loadingMutex = Mutex()

  private var _ongoingLoadingOperations: Int = 0

  private val _loading = MutableStateFlow(false)

  private val _loadingErrorMessageId = MutableStateFlow<Int?>(null)

  private val _hikeFlowsList = MutableStateFlow<List<StateFlow<Hike>>>(emptyList())

  /**
   * Indicates whether when unselecting it, the selected hike in [_selectedHike] should also be
   * removed from [_hikeFlowsMap] (and consequently [_hikeFlowsList]).
   *
   * This was introduced to solve a bug where
   * 1. Opening the details of a hike from the saved hikes screen
   * 2. Unsaving the hike
   *
   * Would result in the hike being removed from the saved hikes list, hence unselected, and hence
   * the user would be thrown out of the details screen.
   *
   * Instead of this behavior, we now keep the selected hike in the list as long as it is selected.
   * When we unselect it, we check this flag to see if it should be removed from the list.
   */
  private var _selectedHikeShouldBeRemoved = false

  private val _selectedHike = MutableStateFlow<Hike?>(null)

  private val _mapState = MutableStateFlow(MapUtils.MapViewState())

  /**
   * Enum used to designate where the hikes loaded in [hikeFlows] come from semantically.
   *
   * See the possible values for this enum along with [loadedHikesType] for more information.
   */
  enum class LoadedHikes {
    /** No hikes have been loaded yet. */
    None,

    /** The hikes loaded in [hikeFlows] are the saved hikes of the user. */
    FromSaved,

    /** The hikes loaded in [hikeFlows] were fetched from within a geographical rectangle. */
    FromBounds
  }

  private val _loadedHikesType = MutableStateFlow(LoadedHikes.None)

  /**
   * Indicates what type of hikes are currently loaded inside of [hikeFlows].
   * - [LoadedHikes.None] if no hikes have been loaded yet.
   * - [LoadedHikes.FromSaved] if [hikeFlows] contains a list of the user's saved hikes.
   * - [LoadedHikes.FromBounds] if [hikeFlows] contains hikes fetched from within a geographical
   *   rectangle.
   */
  val loadedHikesType: StateFlow<LoadedHikes> = _loadedHikesType.asStateFlow()

  /**
   * Indicates whether all the hikes in [hikeFlows] have their OSM data loaded.
   *
   * Note that if [hikeFlows] is empty, this value will be true, because there are no hikes that
   * need to be updated with their OSM data.
   *
   * This value is a state flow, so it can be observed for the UI to update directly when the value
   * changes.
   */
  val allOsmDataLoaded: StateFlow<Boolean> = _allOsmDataLoaded.asStateFlow()

  /**
   * Whether a new list of hikes is currently being retrieved.
   *
   * Set to true when the view model is performing an update of the whole list, or of the saved
   * hikes cache (even without updating [hikeFlows]. For example, this can happen when calling
   * [loadHikesInBounds], [refreshSavedHikesCache] or [loadSavedHikes].
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
   * The resource ID of the error message to display to the user when a loading operation fails.
   *
   * A loading operation can be either [refreshSavedHikesCache], [loadSavedHikes],
   * [loadHikesInBounds] or [retrieveLoadedHikesOsmData]. If any of those operations fail, this
   * value will be set to the corresponding error message ID.
   *
   * This value is null as long as no loading operation fails. It resets to null when a loading
   * operation succeeds.
   */
  val loadingErrorMessageId: StateFlow<Int?> = _loadingErrorMessageId.asStateFlow()

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
  fun selectHike(hikeId: String, onSuccess: () -> Unit = {}, onFailure: () -> Unit = {}) =
      viewModelScope.launch { selectHikeAsync(hikeId, onSuccess, onFailure) }

  /**
   * Sets [selectedHike] to null.
   *
   * Note: the currently selected hike will NOT be removed from [hikeFlows], only [selectedHike]
   * will be impacted.
   */
  fun unselectHike() = viewModelScope.launch { unselectHikeAsync() }

  /**
   * Downloads the current user's saved hikes from the database and caches them locally.
   *
   * This function updates the saved status of the loaded hikes in [hikeFlows]. If saved hikes are
   * currently loaded (see [loadedHikesType]), calling this function will update the whole loaded
   * hikes list to match it with the new saved hikes.
   *
   * @param onSuccess To be called when the saved hikes cache has been updated successfully.
   * @param onFailure Will be called if an error is encountered.
   */
  fun refreshSavedHikesCache(onSuccess: () -> Unit = {}, onFailure: () -> Unit = {}) =
      viewModelScope.launch {
        // Let the user know a heavy load operation is being performed
        setLoading(value = true, errorMessageId = null)

        val success = refreshSavedHikesCacheAsync(forceOverwriteHikesList = false)
        if (success) {
          setLoading(value = false, errorMessageId = null)
          onSuccess()
        } else {
          setLoading(value = false, errorMessageId = R.string.hikes_vm_error_refreshing_saved_hikes)
          onFailure()
        }
      }

  /**
   * Loads the current user's saved hikes and replaces [hikeFlows] with those.
   *
   * This function refreshes the local cache of saved hikes to ensure data is up-to-date.
   *
   * The loaded hikes will only contain minimal data. To load more data, see
   * [retrieveLoadedHikesOsmData] and [retrieveElevationDataFor].
   *
   * @param onSuccess To be called when the saved hikes have been loaded successfully.
   * @param onFailure Will be called if an error is encountered.
   */
  fun loadSavedHikes(onSuccess: () -> Unit = {}, onFailure: () -> Unit = {}) =
      viewModelScope.launch { loadSavedHikesAsync(onSuccess, onFailure) }

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
  fun saveHike(hikeId: String, onSuccess: () -> Unit = {}, onFailure: () -> Unit = {}) =
      viewModelScope.launch { saveHikeAsync(hikeId, onSuccess, onFailure) }

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
  fun unsaveHike(hikeId: String, onSuccess: () -> Unit = {}, onFailure: () -> Unit = {}) =
      viewModelScope.launch { unsaveHikeAsync(hikeId, onSuccess, onFailure) }

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
   * Setting a null date on a saved hike will removed the planned date but keep the hike saved.
   *
   * @param hikeId The ID of the hike to set the planned date of.
   * @param date The planned date for the hike. If null, the hike's planned date will be removed
   * @param onSuccess To be called if the hike's date is successfully updated.
   * @param onFailure To be called if a problem prevents the operation's success.
   */
  fun setPlannedDate(
      hikeId: String,
      date: Timestamp?,
      onSuccess: () -> Unit = {},
      onFailure: () -> Unit = {}
  ) = viewModelScope.launch { setPlannedDateAsync(hikeId, date, onSuccess, onFailure) }

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
  fun loadHikesInBounds(
      bounds: BoundingBox,
      onSuccess: () -> Unit = {},
      onFailure: () -> Unit = {}
  ) = viewModelScope.launch { loadHikesInBoundsAsync(bounds, onSuccess, onFailure) }

  /**
   * Retrieves the bounding box and way points of the currently loaded hikes.
   *
   * Filters the [hikeFlows] list to only include hikes that have not yet been loaded from OSM.
   * Retrieves those hikes' bounding boxes and way points from the OSM repository.
   *
   * The bounding box and way points are then stored in the respective hikes, the state flows are
   * updated directly, no need for a return value.
   *
   * This is a one-time operation. Calling this function does NOT start retrieving OSM data for
   * every new hike that gets added to [hikeFlows]. Instead, it takes a snapshot of [hikeFlows] and
   * retrieves the data for the hikes in that snapshot.
   *
   * Note that the snapshot might not exactly match the current state of [hikeFlows] when the
   * calling of this function is performed, depending on what other operations are performed by the
   * view model at the time of calling.
   *
   * @param onSuccess To be called once the data has been retrieved successfully. Do not use this
   *   callback to retrieve the data manually. Instead, observe [hikeFlows] to be notified
   *   automatically.
   * @param onFailure To be called if a problem is encountered, preventing the success of the
   *   operation.
   */
  fun retrieveLoadedHikesOsmData(onSuccess: () -> Unit = {}, onFailure: () -> Unit = {}) =
      viewModelScope.launch { retrieveLoadedHikesOsmDataAsync(onSuccess, onFailure) }

  /**
   * Indicates whether [retrieveElevationDataFor] can be called for the provided hike.
   *
   * See the documentation of [retrieveElevationDataFor] for details about what this function
   * checks. Everything is documented, no additional checks are performed. This is a merely a helper
   * function.
   *
   * @param hike The hike to check.
   * @return True if the [retrieveElevationDataFor] can be called for this hike, false otherwise.
   */
  fun canElevationDataBeRetrievedFor(hike: Hike): Boolean = hike.waypoints is DeferredData.Obtained

  /**
   * Retrieves the elevation data for a hike.
   *
   * Requires the hike's way points list to have been loaded before. If the way points are not
   * available, the elevation data cannot be retrieved, and [onFailure] will be called.
   *
   * If this function is called several times for the same hike, it will only send a request the
   * first time and mark the data as requested (see [DeferredData] and [Hike] for more information).
   *
   * @param hikeId The ID of the hike to retrieve the elevation data for.
   * @param onSuccess To be called once the elevation data have been successfully requested and/or
   *   retrieved. To avoid multiple requests for the same data, [onSuccess] will be called if
   *   another request is already in progress, even though the data are not yet available. Do not
   *   use this callback to retrieve the elevation data, rather observe the hike in [hikeFlows] to
   *   be notified automatically when it is available.
   * @param onFailure To be called if a problem is encountered and prevents the elevation from being
   *   retrieved.
   */
  fun retrieveElevationDataFor(
      hikeId: String,
      onSuccess: () -> Unit = {},
      onFailure: () -> Unit = {}
  ) = viewModelScope.launch { retrieveElevationDataForAsync(hikeId, onSuccess, onFailure) }

  /**
   * Indicates whether all details data have been computed for the provided hike.
   *
   * Hike details are the [Hike.distance], [Hike.estimatedTime], [Hike.elevationGain] and
   * [Hike.difficulty].
   *
   * This is merely a helper function and does not perform any operations on the hike.
   *
   * @param hike The hike to check.
   * @return True if all details attribute were already computed for this hike, false otherwise.
   */
  fun areDetailsComputedFor(hike: Hike): Boolean =
      hike.distance is DeferredData.Obtained &&
          hike.estimatedTime is DeferredData.Obtained &&
          hike.difficulty is DeferredData.Obtained &&
          hike.elevationGain is DeferredData.Obtained

  /**
   * Computes all details attributes of a hike.
   *
   * See [areDetailsComputedFor] to learn what the details of a hike are.
   *
   * Requires the hike's way points and elevation to have been loaded before. If the way points or
   * the elevation are not available, [onFailure] will be called.
   *
   * If this function is called several times for the same hike, it will only send a request the
   * first time and mark the data as requested (see [DeferredData] and [Hike] for more information).
   *
   * @param hikeId The ID of the hike to retrieve the details attributes for.
   * @param onSuccess To be called once the details have been successfully requested and/or
   *   retrieved. To avoid multiple requests for the same data, [onSuccess] will be called if
   *   another request is already in progress, even though the data are not yet available. Do not
   *   use this callback to retrieve the details values, rather observe the hike in [hikeFlows] to
   *   be notified automatically when it is available.
   * @see [areDetailsComputedFor]
   */
  fun computeDetailsFor(hikeId: String, onSuccess: () -> Unit = {}, onFailure: () -> Unit = {}) =
      viewModelScope.launch { computeDetailsForAsync(hikeId, onSuccess, onFailure) }

  /**
   * Saves the current state of the map.
   *
   * Updates the center and zoom level of the map's. This function enables saving the map's state,
   * so that the map's center and zoom level can be preserved when navigating between screens.
   *
   * @param center The center of the map. Can be fetched by calling .mapCenter on a MapView
   * @param zoom The zoom level of the map. Can be fetched by calling .zoomLevelDouble on a MapView
   */
  fun setMapState(center: GeoPoint, zoom: Double) {
    _mapState.value = MapUtils.MapViewState(center, zoom)
  }

  /**
   * Retrieves the current state of the map.
   *
   * Fetches the center and zoom level of the map's. This function enables saving the map's state,
   * so that the map's center and zoom level can be preserved when navigating between screens.
   *
   * @return The current [MapUtils.MapViewState] containing the map's center point and zoom level.
   */
  fun getMapState(): MapUtils.MapViewState {
    return _mapState.value
  }

  /**
   * Internal helper function.
   *
   * The exposed [loading] state indicates whether a loading operation is ongoing. Simply setting it
   * to true/false is not enough, because several loading operations might be ongoing.
   *
   * To avoid one operation setting [_loading] to false when another operation is still ongoing,
   * this function uses [_ongoingLoadingOperations] to count how many operations are left. It also
   * uses [_loadingMutex] to make its checks thread-safe.
   *
   * If [value] is true, [_ongoingLoadingOperations] is incremented. Reversely if [value] is false.
   *
   * If [_ongoingLoadingOperations] is 0 at the end of the operation, [_loading] is set to false. If
   * it is positive, [_loading] is set to true.
   *
   * If [_ongoingLoadingOperations] becomes negative, it is clamped at 0. [setLoading] should always
   * be used in pair, one call to set loading to true, and one to set it to false.
   *
   * Additionally, updates [_loadingErrorMessageId] with the [errorMessageId] parameter.
   *
   * @param value The new value of the loading state, whether loading (true) or not (false).
   * @param errorMessageId The ID of the string resource to display to the user if the loading
   *   operation has failed. Pass `null` if the loading operation that got completed was successful.
   *   This parameter is only considered if [value] is false. This value will be set even if other
   *   loading operations are still ongoing.
   */
  private suspend fun setLoading(value: Boolean, errorMessageId: Int?) =
      _loadingMutex.withLock {
        if (value) {
          // setLoading(true) is called to indicate the start of a new loading operation
          _ongoingLoadingOperations += 1
        } else if (_ongoingLoadingOperations > 0) {
          // setLoading(false) indicates the end of a loading operation. Ensure the counter does not
          // go below 0.
          _ongoingLoadingOperations -= 1
        }

        // Update the error message ID
        if (!value) {
          _loadingErrorMessageId.value = errorMessageId
        }

        // Adapt the _loading state flow accordingly
        if (_ongoingLoadingOperations < 1 && _loading.value) {
          _loading.value = false
        } else if (_ongoingLoadingOperations > 0 && !_loading.value) {
          _loading.value = true
        }
      }

  /**
   * Internal helper function.
   *
   * The exposed [hikeFlows] list directly mirrors the private mutable state flow [_hikeFlowsList].
   * However, for the view model to work with the hikes in an easier way, it uses [_hikeFlowsMap]
   * internally, which maps hike IDs to their corresponding state flows.
   *
   * [_hikeFlowsMap] is the one that actually gets updated during operations. Once it has been
   * updated, we need to update [_hikeFlowsList] to reflect the changes. This is what this helper
   * function does.
   *
   * Because [_hikeFlowsList] is updated, this function also updates [_allOsmDataLoaded] to reflect
   * whether all hikes in [_hikeFlowsList] have their OSM data loaded.
   */
  private fun updateHikeFlowsListAndOsmDataStatus() {
    _hikeFlowsList.value = _hikeFlowsMap.values.toList()
    updateOsmDataAvailabilityStatus()
  }

  /**
   * Internal helper function.
   *
   * The exposed [allOsmDataLoaded] indicates whether all loaded hikes in [hikeFlows] have their OSM
   * data available. This needs to be updated when either the entire list is updated, or a single
   * hike is updated.
   *
   * This helper function updates the value of [_allOsmDataLoaded] accordingly to [_hikeFlowsList].
   */
  private fun updateOsmDataAvailabilityStatus() {
    _allOsmDataLoaded.value = _hikeFlowsList.value.all { it.value.hasOsmData() }
  }

  /**
   * Internal helper function.
   *
   * Sets [_loadedHikesType] with the new provided value if not already equal to that value.
   *
   * Requires [_hikesMutex] to have been acquired by the caller.
   */
  private fun setLoadedHikesType(value: LoadedHikes) {
    if (_loadedHikesType.value != value) {
      _loadedHikesType.value = value
    }
  }

  /**
   * Helper function to update the selected hike once (or before) [hikeFlows] has been updated.
   *
   * In particular, this function will check that the selected hike stays in [_hikeFlowsMap] as long
   * as it is selected (only [unselectHike] should remove the selected hike from the map if
   * necessary. Uses the [_selectedHikeShouldBeRemoved] flag.
   *
   * This function does not acquire the [_hikesMutex]. It is the responsibility of the caller to
   * call this function inside of a [Mutex.withLock] block.
   *
   * This function does not switch context either, it is the responsibility of the caller to call
   * this function inside of a [withContext] block.
   */
  private fun updateSelectedHike() {
    // If there is no selected hike, don't bother.
    val selectedHike = _selectedHike.value ?: return

    // Retrieve the corresponding flow from the map.
    val selectedHikeFlow = _hikeFlowsMap[selectedHike.id]

    if (selectedHikeFlow == null) {
      // Set the flag for removing the hike from the list when it is unselected
      _selectedHikeShouldBeRemoved = true

      // The selected hike is not in the map, add it back and update its saved status
      val saved = _savedHikesMap[selectedHike.id]
      val newValue = selectedHike.copy(isSaved = saved != null, plannedDate = saved?.date)
      _hikeFlowsMap[selectedHike.id] = MutableStateFlow(newValue)
      if (selectedHike != newValue) {
        _selectedHike.value = newValue
      }
    } else {
      // The selected hike is still in the map, update it.
      val flowValue = selectedHikeFlow.value
      if (flowValue != selectedHike) {
        _selectedHike.value = flowValue
      }
    }
  }

  /**
   * See the documentation of [selectHike].
   *
   * This function is called by [selectHike] to perform its work asynchronously.
   */
  private suspend fun selectHikeAsync(
      hikeId: String,
      onSuccess: () -> Unit,
      onFailure: () -> Unit
  ) =
      withContext(dispatcher) {
        var successful = false
        _hikesMutex.withLock {
          // Retrieve the hike to select from the loaded hikes
          val hike = _hikeFlowsMap[hikeId] ?: return@withLock

          // Mark the hike as selected
          _selectedHike.value = hike.value
          successful = true
        }

        // Perform the callback without the lock, to avoid deadlocks and improve performance
        if (successful) {
          onSuccess()
        } else {
          onFailure()
        }
      }

  /**
   * See the documentation of [unselectHike].
   *
   * This function is called by [unselectHike] to perform its work asynchronously.
   */
  private suspend fun unselectHikeAsync() =
      _hikesMutex.withLock {
        val selectedHike = _selectedHike.value ?: return@withLock

        // See if the selected hike should be removed from the hike flows map
        if (_selectedHikeShouldBeRemoved) {
          _selectedHikeShouldBeRemoved = false
          _hikeFlowsMap.remove(selectedHike.id)
          updateHikeFlowsListAndOsmDataStatus()
        }

        // We checked before the selected hike was currently not null, hence no need to check before
        // emitting a value of null
        _selectedHike.value = null
      }

  /**
   * Loads the current user's saved hikes and updates the local cache [_savedHikesMap].
   *
   * If needed, updates the loaded hikes in [_hikeFlowsMap] to reflect the new saved hikes, and the
   * [_selectedHike] as well.
   *
   * This function performs a loading operation but DOES NOT set [_loading]. If calling it from
   * another function, please set [_loading] to true before calling the function and to false once
   * the call has ended.
   *
   * @param forceOverwriteHikesList If true, [_savedHikesMap] will be overwritten to contain all and
   *   only saved hikes. If false, [_savedHikesMap] will be updated according to [_loadedHikesType].
   * @return True if the operation is successful, false otherwise.
   */
  private suspend fun refreshSavedHikesCacheAsync(forceOverwriteHikesList: Boolean): Boolean =
      withContext(dispatcher) {
        val savedHikes: List<SavedHike>
        try {
          // Load the saved hikes from the repository
          savedHikes = savedHikesRepo.loadSavedHikes()
        } catch (e: Exception) {
          Log.e(LOG_TAG, "Error encountered while loading saved hikes", e)
          return@withContext false
        }

        // Wait for the lock to avoid concurrent modifications
        _hikesMutex.withLock {
          updateSavedHikesCache(savedHikes, forceOverwriteHikesList)
          if (forceOverwriteHikesList) {
            setLoadedHikesType(LoadedHikes.FromSaved)
          }
        }

        return@withContext true
      }

  /**
   * Helper function for [refreshSavedHikesCacheAsync].
   *
   * Takes a list of saved hikes and updates the local cache of saved hikes with that list.
   *
   * Also updates [hikeFlows] to reflect the changes, and updates the selected hike if necessary.
   *
   * It is the caller's responsibility to ensure the saved hikes list is up-to-date and to acquire
   * [_hikesMutex] before calling this function.
   *
   * @param newList The list of saved hikes to update the cache with.
   */
  private fun updateSavedHikesCache(newList: List<SavedHike>, forceOverwriteHikesList: Boolean) {
    // Clear the current saved hikes register
    _savedHikesMap.clear()

    // Build the new saved hikes register
    newList.forEach { savedHike -> _savedHikesMap[savedHike.id] = savedHike }

    // Now the saved hikes cache was updated, but we still need to update the hike flows list

    when {
      // Saved hikes are loaded or should be loaded. Remove the unsaved and add the new saved ones.
      forceOverwriteHikesList || _loadedHikesType.value == LoadedHikes.FromSaved -> {
        // First, remove the hikes that were unsaved
        _hikeFlowsMap = _hikeFlowsMap.filterKeys { _savedHikesMap.containsKey(it) }.toMutableMap()

        // Update the hikes already in the list (their planned date). We assume all those hikes are
        // saved.
        updateExistingHikesSavedStatus()

        // Add the saved hikes that are not in the list yet
        _savedHikesMap.minus(_hikeFlowsMap.keys).forEach { (hikeId, savedHike) ->
          _hikeFlowsMap[hikeId] =
              MutableStateFlow(
                  Hike(
                      id = savedHike.id,
                      isSaved = true,
                      plannedDate = savedHike.date,
                      name = savedHike.name))
        }
      }

      // No hikes were loaded yet, do nothing
      _loadedHikesType.value == LoadedHikes.None -> return

      // Hikes were loaded from bounds, do not remove nor add any hikes, just update existing ones
      _loadedHikesType.value == LoadedHikes.FromBounds -> {
        updateExistingHikesSavedStatus()
      }
    }

    // Update the selected hike's saved status, add it back to the hike flows list if it was removed
    updateSelectedHike()

    // Update the exposed list of hikes based on the map of hikes
    updateHikeFlowsListAndOsmDataStatus()
  }

  /**
   * Helper function for [updateSavedHikesCache].
   *
   * For each existing hike in [_hikeFlowsMap], determines whether it needs an update based on the
   * new [_savedHikesMap], and updates its [StateFlow] if needed.
   */
  private fun updateExistingHikesSavedStatus() {
    _hikeFlowsMap.forEach { (hikeId, hikeFlow) ->
      val savedHike = _savedHikesMap[hikeId]
      val hike = hikeFlow.value
      val (changeNeeded, updated) = hikeNeedsSavedStatusUpdate(hike, savedHike)
      if (changeNeeded) {
        hikeFlow.value = updated
      }
    }
  }

  /**
   * See the documentation of [loadSavedHikes].
   *
   * This function is called by [loadSavedHikes] to perform its work asynchronously.
   */
  private suspend fun loadSavedHikesAsync(onSuccess: () -> Unit, onFailure: () -> Unit) =
      withContext(dispatcher) {
        // Let the user know a heavy load operation is being performed
        setLoading(value = true, errorMessageId = null)

        // Update the local cache of saved hikes and add them to _hikeFlows
        val success = refreshSavedHikesCacheAsync(forceOverwriteHikesList = true)

        if (success) {
          setLoading(value = false, errorMessageId = null)
          onSuccess()
        } else {
          setLoading(value = false, errorMessageId = R.string.hikes_vm_error_loading_saved_hikes)
          onFailure()
        }
      }

  /**
   * Determines whether [hike] needs an update to its saved status based on [savedHike].
   *
   * Does not actually update the hike, that is left to the caller to do.
   *
   * @param hike The hike that might need an update to its saved status
   * @param savedHike The currently known saved status of the provided hike. Null means the hike is
   *   not saved.
   * @return (false, [hike]) if the hike does not need an update, (true, updated) if the hike does
   *   need an update, with updated being the hike with the new saved status.
   */
  private fun hikeNeedsSavedStatusUpdate(hike: Hike, savedHike: SavedHike?): Pair<Boolean, Hike> {
    // If the hike is marked as saved but was unsaved, clear its saved state
    if (savedHike == null) {
      return if (hike.isSaved) {
        Pair(true, hike.copy(isSaved = false, plannedDate = null))
      } else {
        Pair(false, hike)
      }
    }

    // If the hike is saved, check if one of the attributes changed (assume the name won't change)
    return if (!hike.isSaved || hike.plannedDate != savedHike.date) {
      Pair(true, hike.copy(isSaved = true, plannedDate = savedHike.date))
    } else {
      Pair(false, hike)
    }
  }

  /**
   * See the documentation of [saveHike].
   *
   * This function is called by [saveHike] to perform its work asynchronously.
   */
  private suspend fun saveHikeAsync(hikeId: String, onSuccess: () -> Unit, onFailure: () -> Unit) =
      withContext(dispatcher) {
        val successful: Boolean
        _hikesMutex.withLock { successful = saveHikeAsyncInternal(hikeId) }

        // Perform the callback without the lock, to avoid deadlocks and improve performance
        if (successful) {
          onSuccess()
        } else {
          onFailure()
        }
      }

  /**
   * Saves the hike corresponding to the provided [hikeId].
   *
   * CAUTION: This method DOES NOT acquire the [_hikesMutex]. It is the responsibility of the caller
   * to call this function inside of a [Mutex.withLock] block.
   *
   * @param hikeId The ID of the hike that should be saved.
   * @return True if the operation was successful, false otherwise.
   */
  private suspend fun saveHikeAsyncInternal(hikeId: String): Boolean {
    // Retrieve the hike to save from the loaded hikes
    val hikeFlow = _hikeFlowsMap[hikeId] ?: return false
    val hike = hikeFlow.value

    // Avoid updating the hike if it is already saved
    if (hike.isSaved) {
      return true
    }

    // Save the hike using the repository
    val savedHike = hike.toSavedHike()
    try {
      savedHikesRepo.addSavedHike(savedHike)
    } catch (e: Exception) {
      Log.e(LOG_TAG, "Error encountered while saving hike", e)
      return false
    }

    // Update the saved hikes map
    _savedHikesMap[hikeId] = savedHike

    // Update the hike's state flow
    hikeFlow.value = hikeFlow.value.copy(isSaved = true)

    // Update the selected hike if necessary
    updateSelectedHike()

    return true
  }

  /**
   * See the documentation of [unsaveHike].
   *
   * This function is called by [unsaveHike] to perform its work asynchronously.
   */
  private suspend fun unsaveHikeAsync(
      hikeId: String,
      onSuccess: () -> Unit,
      onFailure: () -> Unit
  ) =
      withContext(dispatcher) {
        var successful = false
        _hikesMutex.withLock {
          // Retrieve the hike to unsave from the loaded hikes
          val hikeFlow = _hikeFlowsMap[hikeId] ?: return@withLock
          val hike = hikeFlow.value

          // Avoid updating the hike if it is not saved
          if (!hike.isSaved) {
            successful = true
            return@withLock
          }

          // Unsave the hike using the repository
          try {
            savedHikesRepo.removeSavedHike(hike.toSavedHike())
          } catch (e: Exception) {
            Log.e(LOG_TAG, "Error encountered while unsaving hike", e)
            successful = false
            return@withLock
          }

          // Update the saved hikes map
          _savedHikesMap.remove(hikeId)

          if (_loadedHikesType.value == LoadedHikes.FromSaved) {
            // Only saved hikes may stay in the list, delete the unsaved hike from the list
            _hikeFlowsMap.remove(hikeId)
            // Update the selected hike if necessary, meaning add it back to the map if needed
            // This must be done before updating the list from the map
            updateSelectedHike()
            // Update the exposed list of loaded hikes from the internal map
            updateHikeFlowsListAndOsmDataStatus()
          } else {
            // The hike can stay even if it is not saved, so update it
            hikeFlow.value = hikeFlow.value.copy(isSaved = false, plannedDate = null)
            // Update the selected hike if necessary
            updateSelectedHike()
          }

          successful = true
        }

        // Perform the callback without the lock, to avoid deadlocks and improve performance
        if (successful) {
          onSuccess()
        } else {
          onFailure()
        }
      }

  /**
   * See the documentation of [setPlannedDate].
   *
   * This function is called by [setPlannedDate] to perform its work asynchronously.
   */
  private suspend fun setPlannedDateAsync(
      hikeId: String,
      date: Timestamp?,
      onSuccess: () -> Unit,
      onFailure: () -> Unit
  ) =
      withContext(dispatcher) {
        val successful: Boolean
        _hikesMutex.withLock { successful = updateHikePlannedDateAsync(hikeId, date) }

        if (successful) {
          onSuccess()
        } else {
          onFailure()
        }
      }

  /**
   * Helper function to update the planned date of a hike.
   *
   * This function requires [_hikesMutex]'s lock to be acquired before being called. It is the
   * caller's responsibility to acquire the lock.
   *
   * @param hikeId The unique ID of the hike to set the planned date for.
   * @return True if the operation was successful, false otherwise.
   */
  private suspend fun updateHikePlannedDateAsync(hikeId: String, date: Timestamp?): Boolean {
    // Retrieve the hike to update from the loaded hikes
    val hikeFlow = _hikeFlowsMap[hikeId] ?: return false
    val hike = hikeFlow.value

    // If the hike is already saved with the provided date, don't do anything
    if (hike.isSaved && hike.plannedDate == date) {
      return true
    }

    // If the hike is not saved but the hike date is null, the operation is useless
    if (!hike.isSaved && date == null) {
      return true
    }

    // If the hike is already saved, unsave it temporarily
    val savedHike = _savedHikesMap[hike.id]
    if (savedHike != null) {
      try {
        savedHikesRepo.removeSavedHike(savedHike)
      } catch (e: Exception) {
        Log.e(LOG_TAG, "Error encountered while unsaving hike temporarily", e)
        return false
      }
    }

    // Set the hike's planned date to the right one
    val newSavedHike = SavedHike(id = hike.id, name = hike.name, date = date)
    try {
      savedHikesRepo.addSavedHike(newSavedHike)
    } catch (e: Exception) {
      Log.e(LOG_TAG, "Error encountered while re-saving hike", e)
      return false
    }

    // Update the hike in the saved hikes map
    _savedHikesMap[hike.id] = newSavedHike

    // Update the hike's state flow
    hikeFlow.value = hike.copy(isSaved = true, plannedDate = date)

    // Update the selected hike if necessary
    updateSelectedHike()

    return true
  }

  /**
   * See the documentation of [loadHikesInBounds].
   *
   * This function is called by [loadHikesInBounds] to perform its work asynchronously.
   */
  private suspend fun loadHikesInBoundsAsync(
      boundingBox: BoundingBox,
      onSuccess: () -> Unit,
      onFailure: () -> Unit
  ) =
      withContext(dispatcher) {
        // Let the user know a heavy load operation is being performed
        setLoading(value = true, errorMessageId = null)

        // Load the hikes from the repository
        val hikes: List<HikeRoute>
        try {
          hikes = loadHikesInBoundsRepoWrapper(boundingBox.toBounds())
        } catch (e: Exception) {
          Log.e(LOG_TAG, "Error encountered while loading hikes in bounds", e)
          setLoading(
              value = false, errorMessageId = R.string.hikes_vm_error_loading_hikes_in_bounds)
          onFailure()
          return@withContext
        }

        // Wait for the lock to avoid concurrent modifications
        _hikesMutex.withLock {
          // Keep already loaded hikes, discard the ones that do not appear in the given bounds
          val keysToKeep = hikes.map { it.id }
          _hikeFlowsMap =
              _hikeFlowsMap.filterKeys { key -> keysToKeep.contains(key) }.toMutableMap()

          // Add the new hikes to the map of hikes
          _hikeFlowsMap =
              hikes
                  .map { osmData ->
                    val existingFlow = _hikeFlowsMap[osmData.id]

                    // The hike is not in the map yet, add it
                    return@map if (existingFlow == null) {
                      val hike =
                          Hike(
                              id = osmData.id,
                              isSaved = _savedHikesMap.containsKey(osmData.id),
                              plannedDate = _savedHikesMap[osmData.id]?.date,
                              name = osmData.name,
                              description = DeferredData.Obtained(osmData.description),
                              bounds = DeferredData.Obtained(osmData.bounds),
                              waypoints = DeferredData.Obtained(osmData.ways))
                      Pair(hike.id, MutableStateFlow(hike))
                    } else {
                      // The hike is already in the map, update it
                      val existingHike = existingFlow.value
                      // Assume a user session won't last long enough for a hike to change its
                      // bounds and/or waypoints. If they are already loaded, don't update them.
                      // OSM might update bounds and waypoints, but we decided it is sufficient to
                      // have the new values loaded the next time the user opens the app.
                      if (existingHike.bounds !is DeferredData.Obtained ||
                          existingHike.waypoints !is DeferredData.Obtained) {
                        val newHike =
                            existingHike.copy(
                                description = DeferredData.Obtained(osmData.description),
                                bounds = DeferredData.Obtained(osmData.bounds),
                                waypoints = DeferredData.Obtained(osmData.ways))
                        // As the list will be updated as a whole, no need to update the state flow
                        // here
                        Pair(existingHike.id, MutableStateFlow(newHike))
                      } else {
                        Pair(existingHike.id, existingFlow)
                      }
                    }
                  }
                  .toMap(mutableMapOf())

          // Update the selected hike or add it back to the hike flows map if necessary
          updateSelectedHike()

          // Update the exposed list of hikes based on the map of hikes
          updateHikeFlowsListAndOsmDataStatus()

          // We are loading hikes from bounds, remember this to avoid overriding loaded hikes with
          // saved hikes when those get refreshed.
          setLoadedHikesType(LoadedHikes.FromBounds)
        }

        // Call the success callback once the mutex has been released to avoid locking for too long
        setLoading(value = false, errorMessageId = null)
        onSuccess()
      }

  /**
   * The [HikeRoutesRepository] interface has been developed without coroutines in mind, hence we
   * need a wrapper to "convert" the [HikeRoutesRepository.getRoutes] function that uses callback to
   * a suspend function.
   */
  private suspend fun loadHikesInBoundsRepoWrapper(bounds: Bounds): List<HikeRoute> =
      suspendCoroutine { continuation ->
        osmHikesRepo.getRoutes(
            bounds = bounds,
            onSuccess = { routes -> continuation.resume(routes) },
            onFailure = { exception -> continuation.resumeWithException(exception) })
      }

  /**
   * Helper function to determine whether a hike has all its OSM data loaded.
   *
   * OSM data includes [Hike.description], [Hike.bounds] and [Hike.waypoints].
   *
   * @param hike The hike to check.
   * @return True if the hike has all its OSM data loaded, false otherwise.
   */
  private fun hasOsmData(hike: Hike): Boolean =
      hike.description is DeferredData.Obtained &&
          hike.bounds is DeferredData.Obtained &&
          hike.waypoints is DeferredData.Obtained

  /**
   * See the documentation of [retrieveLoadedHikesOsmData].
   *
   * This function is called by [retrieveLoadedHikesOsmData] to perform its work asynchronously.
   */
  private suspend fun retrieveLoadedHikesOsmDataAsync(
      onSuccess: () -> Unit,
      onFailure: () -> Unit
  ) =
      withContext(dispatcher) {
        // Allow only one retrieval request to be performed at any given time
        val success: Boolean
        _osmDataRetrievalMutex.withLock {
          // Prepare a list of hikes for which we need to retrieve the data
          val idsToRetrieve: List<String>
          val requestNeeded: Boolean
          _hikesMutex.withLock {
            requestNeeded = !_allOsmDataLoaded.value
            idsToRetrieve =
                _hikeFlowsMap.values.filter { !hasOsmData(it.value) }.map { it.value.id }
          }

          // If all hikes already have their OSM data, do nothing more
          if (!requestNeeded || idsToRetrieve.isEmpty()) {
            success = true
            return@withLock
          }

          // If the request is needed, indicate a heavy loading operation is being performed
          setLoading(value = true, errorMessageId = null)

          // Retrieve the OSM data of the hikes
          val hikeRoutes: List<HikeRoute>
          try {
            hikeRoutes = loadHikesByIdsRepoWrapper(idsToRetrieve)
          } catch (e: Exception) {
            Log.e(LOG_TAG, "Error encountered while loading hikes OSM data", e)
            success = false
            return@withLock
          }

          // Update the retrieved data for the loaded hikes
          _hikesMutex.withLock {
            hikeRoutes.forEach { hikeRoute ->
              val hikeFlow = _hikeFlowsMap[hikeRoute.id] ?: return@forEach
              val hike = hikeFlow.value

              val newHike =
                  hike.copy(
                      name = hikeRoute.name,
                      description = DeferredData.Obtained(hikeRoute.description),
                      bounds = DeferredData.Obtained(hikeRoute.bounds),
                      waypoints = DeferredData.Obtained(hikeRoute.ways))
              hikeFlow.value = newHike
            }

            // Update whether all hikes have their OSM data loaded
            updateOsmDataAvailabilityStatus()

            // Update the selected hike if necessary
            updateSelectedHike()
          }

          // Release the mutex before calling onSuccess to avoid deadlocks or performance issues
          success = true
        }

        // Call the appropriate callback and indicate the heavy loading operation has terminated
        if (success) {
          setLoading(value = false, errorMessageId = null)
          onSuccess()
        } else {
          setLoading(value = false, errorMessageId = R.string.hikes_vm_error_loading_hikes_osm_data)
          onFailure()
        }
      }

  /**
   * The [HikeRoutesRepository] interface has been developed without coroutines in mind, hence we
   * need a wrapper to "convert" the [HikeRoutesRepository.getRouteById] function that uses callback
   * to a suspend function.
   */
  private suspend fun loadHikesByIdsRepoWrapper(ids: List<String>): List<HikeRoute> =
      suspendCoroutine { continuation ->
        osmHikesRepo.getRoutesByIds(
            routeIds = ids,
            onSuccess = { hikeRoutes -> continuation.resume(hikeRoutes) },
            onFailure = { exception -> continuation.resumeWithException(exception) })
      }

  /**
   * See the documentation of [retrieveElevationDataFor].
   *
   * This function is called by [retrieveElevationDataFor] to perform its work asynchronously.
   */
  private suspend fun retrieveElevationDataForAsync(
      hikeId: String,
      onSuccess: () -> Unit,
      onFailure: () -> Unit
  ) =
      withContext(dispatcher) {
        // Retrieve the hike from the list to perform a few checks
        var success = false
        var alreadyComputed = false
        var mutableWaypoints: List<LatLong>? = null
        _hikesMutex.withLock {
          val hikeFlow = _hikeFlowsMap[hikeId] ?: return@withLock
          val hike = hikeFlow.value

          // If the elevation of this hike is already computed or requested, do nothing
          alreadyComputed = hike.elevation.obtained() || hike.elevation is DeferredData.Requested
          if (alreadyComputed) {
            success = true
            return@withLock
          }

          // If the way points of the hike have not been loaded, fail
          if (hike.waypoints !is DeferredData.Obtained) {
            success = false
            return@withLock
          }
          mutableWaypoints = hike.waypoints.data

          // The elevation has not been requested yet, mark it as requested
          hikeFlow.value = hike.copy(elevation = DeferredData.Requested)
          success = true
        }

        if (alreadyComputed) {
          onSuccess()
          return@withContext
        } else if (!success) {
          onFailure()
          return@withContext
        }

        // Extract the waypoints from the nullable variable safely for the compiler to be happy
        val waypoints: List<LatLong> =
            mutableWaypoints
                ?: run {
                  onFailure()
                  return@withContext
                }

        // Launch a request for the elevation data of the hike
        val elevation: List<Double>
        try {
          elevation = getElevationRepoWrapper(waypoints)
        } catch (e: Exception) {
          _hikesMutex.withLock {
            val hikeFlow = _hikeFlowsMap[hikeId] ?: return@withLock
            hikeFlow.value = hikeFlow.value.copy(elevation = DeferredData.Error(e))
            updateSelectedHike()
          }
          Log.e(LOG_TAG, "Error encountered while retrieving elevation", e)
          onFailure()
          return@withContext
        }

        // Update the hike's elevation data
        success = false
        _hikesMutex.withLock {
          val hikeFlow = _hikeFlowsMap[hikeId] ?: return@withLock
          val hike = hikeFlow.value

          hikeFlow.value = hike.copy(elevation = DeferredData.Obtained(elevation))

          // Update the selected hike if necessary
          updateSelectedHike()

          success = true
        }

        if (success) {
          onSuccess()
        } else {
          onFailure()
        }
      }

  /**
   * The [ElevationRepository] interface has been developed without coroutines in mind, hence we
   * need a wrapper to "convert" the [ElevationRepository.getElevation] function that uses callback
   * to a suspend function.
   */
  private suspend fun getElevationRepoWrapper(
      coordinates: List<LatLong>,
  ): List<Double> = suspendCoroutine { continuation ->
    elevationRepository.getElevation(
        coordinates = coordinates,
        onSuccess = { elevation -> continuation.resume(elevation) },
        onFailure = { exception -> continuation.resumeWithException(exception) })
  }

  /**
   * See the documentation of [computeDetailsFor].
   *
   * This function is called by [computeDetailsFor] to perform its work asynchronously.
   */
  private suspend fun computeDetailsForAsync(
      hikeId: String,
      onSuccess: () -> Unit,
      onFailure: () -> Unit
  ) =
      withContext(dispatcher) {
        // Retrieve the current value of the hike and note what computations are required
        val (performRequest, waypoints, elevation) =
            detailsPreRequestOperations(hikeId, onSuccess, onFailure)

        if (!performRequest) {
          return@withContext
        }

        // Perform the actual computations
        val distance: Double
        val elevationGain: Double
        val estimatedTime: Double
        val difficulty: HikeDifficulty
        try {
          distance = RouteUtils.computeTotalDistance(waypoints)
          elevationGain = RouteUtils.calculateElevationGain(elevation)
          estimatedTime = RouteUtils.estimateTime(distance, elevationGain)
          difficulty = RouteUtils.determineDifficulty(distance, elevationGain)
        } catch (e: Exception) {
          Log.e(LOG_TAG, "Error while computing hike details", e)

          // Set the detail properties back to NotRequested
          _hikesMutex.withLock {
            val hikeFlow = _hikeFlowsMap[hikeId] ?: return@withLock
            hikeFlow.value =
                hikeFlow.value.copy(
                    distance = DeferredData.Error(e),
                    elevationGain = DeferredData.Error(e),
                    estimatedTime = DeferredData.Error(e),
                    difficulty = DeferredData.Error(e),
                )

            // Update the selected hike if necessary
            updateSelectedHike()
          }

          onFailure()
          return@withContext
        }

        // Update the hike in the list
        var success = false
        _hikesMutex.withLock {
          val hikeFlow = _hikeFlowsMap[hikeId] ?: return@withLock
          hikeFlow.value =
              hikeFlow.value.copy(
                  distance = DeferredData.Obtained(distance),
                  elevationGain = DeferredData.Obtained(elevationGain),
                  estimatedTime = DeferredData.Obtained(estimatedTime),
                  difficulty = DeferredData.Obtained(difficulty))

          // Update the selected hike if necessary
          updateSelectedHike()

          success = true
        }

        if (success) {
          onSuccess()
        } else {
          onFailure()
        }
      }

  /**
   * Helper function for [computeDetailsForAsync].
   *
   * Checks that the required data ([Hike.waypoints] and [Hike.elevation]) are available, and that a
   * request is not already ongoing. Returns the data if it is available, and indicates whether a
   * request should be performed.
   *
   * If no request should be performed (can be either because of an error or because the data are
   * already requested/obtained), this function ([detailsPreRequestOperations]) is responsible for
   * calling the right callback ([onSuccess] or [onFailure]).
   *
   * If a request should be performed, this function will mark the data as requested (see
   * [DeferredData.Requested]). The caller is responsible for calling the right callback
   * ([onSuccess] or [onFailure]) once the request is done.
   *
   * Note: this function does not set its context to [dispatcher], it is the responsibility of the
   * caller to do so.
   *
   * @param hikeId The ID of the hike to check the data for.
   * @param onSuccess To be called if the data was already obtained or requested. Won't be called if
   *   a request should be performed.
   * @param onFailure To be called if no hike corresponding to the provided ID was found.
   * @return A triple containing:
   * - A `Boolean` indicating whether the request should be performed. A value of true means the
   *   request should be performed, false means the data is already available or an error occurred.
   * - The `List` of way points of the hike, if available. If not available, the list will be empty
   *   and the boolean will be false.
   * - The `List` of elevation data of the hike, if available. If not available, the list will be
   *   empty and the boolean will be false.
   */
  private suspend fun detailsPreRequestOperations(
      hikeId: String,
      onSuccess: () -> Unit,
      onFailure: () -> Unit
  ): Triple<Boolean, List<LatLong>, List<Double>> {
    var success = false
    var alreadyComputed = false
    var alreadyRequested = false
    var nullableWayPoints: List<LatLong>? = null
    var nullableElevation: List<Double>? = null
    _hikesMutex.withLock {
      val hikeFlow = _hikeFlowsMap[hikeId] ?: return@withLock
      val hike = hikeFlow.value

      // Check that the details data hasn't been computed already
      alreadyComputed = areDetailsComputedFor(hike)
      if (alreadyComputed) {
        success = true
        return@withLock
      }

      // Only perform the computation if it hasn't been started for all components yet
      if (hike.distance is DeferredData.Requested &&
          hike.estimatedTime is DeferredData.Requested &&
          hike.difficulty is DeferredData.Requested &&
          hike.elevationGain is DeferredData.Requested) {
        success = true
        alreadyRequested = true
        return@withLock
      }

      // Check that the way points and the elevation have been retrieved for this hike
      if (hike.waypoints !is DeferredData.Obtained || hike.elevation !is DeferredData.Obtained) {
        return@withLock
      }
      nullableWayPoints = hike.waypoints.data
      nullableElevation = hike.elevation.data

      // Set the details of the hike to have been requested
      hikeFlow.value =
          hike.copy(
              distance = DeferredData.Requested,
              estimatedTime = DeferredData.Requested,
              difficulty = DeferredData.Requested,
              elevationGain = DeferredData.Requested,
          )

      // Update the selected hike if necessary
      updateSelectedHike()

      success = true
    }

    if (!success) {
      onFailure()
      return Triple(false, emptyList(), emptyList())
    } else if (alreadyComputed || alreadyRequested) {
      onSuccess()
      return Triple(false, emptyList(), emptyList())
    }

    // Safely extract the way points from the nullable variable to make the compiler happy
    val waypoints =
        nullableWayPoints
            ?: run {
              onFailure()
              return Triple(false, emptyList(), emptyList())
            }

    // Safely extract the elevation from the nullable variable to make the compiler happy
    val elevation =
        nullableElevation
            ?: run {
              onFailure()
              return Triple(false, emptyList(), emptyList())
            }

    return Triple(true, waypoints, elevation)
  }
}
