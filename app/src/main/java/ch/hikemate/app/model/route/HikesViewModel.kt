package ch.hikemate.app.model.route

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.hikemate.app.model.elevation.ElevationService
import ch.hikemate.app.model.route.saved.SavedHike
import ch.hikemate.app.model.route.saved.SavedHikesRepository
import ch.hikemate.app.utils.RouteUtils
import com.google.firebase.Timestamp
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
  companion object {
    private const val LOG_TAG = "HikesViewModel"
  }

  private val _hikesMutex = Mutex()

  private val _savedHikesMap = mutableMapOf<String, SavedHike>()

  private var _hikeFlowsMap = mutableMapOf<String, MutableStateFlow<Hike>>()

  private val _loading = MutableStateFlow(false)

  private val _hikeFlowsList = MutableStateFlow<List<StateFlow<Hike>>>(emptyList())

  private fun updateHikeFlowsList() {
    _hikeFlowsList.value = _hikeFlowsMap.values.toList()
  }

  private var _selectedHikeId: String? = null
  private val _selectedHike = MutableStateFlow<Hike?>(null)

  /** Internal type used to store where the hikes in [_hikeFlowsMap] come from. */
  private enum class LoadedHikes {
    /** No hikes have been loaded yet. */
    None,

    /** The hikes loaded in [_hikeFlowsMap] are the saved hikes. */
    FromSaved,

    /** The hikes loaded in [_hikeFlowsMap] were fetched from within a geographical rectangle. */
    FromBounds
  }

  private var _loadedHikesType: LoadedHikes = LoadedHikes.None

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
   * currently loaded (a call to [loadSavedHikes] has been made and no other loading call has been
   * performed since), calling this function will update the whole loaded hikes list to match it
   * with the new saved hikes.
   *
   * @param onSuccess To be called when the saved hikes cache has been updated successfully.
   * @param onFailure Will be called if an error is encountered.
   */
  fun refreshSavedHikesCache(onSuccess: () -> Unit = {}, onFailure: () -> Unit = {}) =
      viewModelScope.launch {
        _loading.value = true
        refreshSavedHikesCacheAsync(onSuccess, onFailure)
        _loading.value = false
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
      viewModelScope.launch {
        _loading.value = true
        loadSavedHikesAsync(onSuccess, onFailure)
        _loading.value = false
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
   * @param hikeId The ID of the hike to set the planned date of.
   * @param date The planned date for the hike.
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
  fun loadHikesInBounds(bounds: BoundingBox, onSuccess: () -> Unit = {}, onFailure: () -> Unit = {}) =
      viewModelScope.launch {
        // Let the user know a heavy load operation is being performed
        _loading.value = true

        loadHikesInBoundsAsync(bounds, onSuccess, onFailure)

        // The heavy loading operation is done now
        _loading.value = false
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
   * @param onSuccess To be called once the elevation has been successfully retrieved for the hike.
   *   Do not use this to retrieve the elevation, rather observe the hike in [hikeFlows] to be
   *   notified automatically.
   * @param onFailure To be called if a problem is encountered and prevents the elevation from being
   *   retrieved.
   */
  fun retrieveElevationDataFor(hikeId: String, onSuccess: () -> Unit = {}, onFailure: () -> Unit = {}) =
      viewModelScope.launch { retrieveElevationDataForAsync(hikeId, onSuccess, onFailure) }

  /**
   * Indicates whether all details data have been computed for the provided hike.
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
   * Requires the hike's way points and elevation to have been loaded before. If the way points or
   * the elevation are not available, [onFailure] will be called.
   *
   * If this function is called several times for the same hike, it will only send a request the
   * first time and mark the data as requested (see [DeferredData] and [Hike] for more information).
   *
   * @param hikeId The ID of the hike to retrieve the details attributes for.
   * @param onSuccess To be called once the details have been successfully retrieved. Do not use
   *   this to retrieve the details values, rather observe the hike in [hikeFlows] to be notified
   *   automatically.
   */
  fun computeDetailsFor(hikeId: String, onSuccess: () -> Unit = {}, onFailure: () -> Unit = {}) =
      viewModelScope.launch { retrieveDetailsForAsync(hikeId, onSuccess, onFailure) }

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
          _selectedHikeId = hikeId
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

  private suspend fun unselectHikeAsync() =
      _hikesMutex.withLock {
        // Only emit null as a value if the selected hike was not already null
        if (_selectedHikeId != null) {
          _selectedHikeId = null
          _selectedHike.value = null
        }
      }

  /**
   * This function performs a loading operation but DOES NOT set [_loading]. If calling it from
   * another function, please set [_loading] to true before calling the function and to false once
   * the call has ended.
   *
   * @return True if the operation is successful, false otherwise.
   */
  private suspend fun refreshSavedHikesCacheAsync(
      onSuccess: () -> Unit = {},
      onFailure: () -> Unit = {}
  ): Boolean =
      withContext(dispatcher) {
        val savedHikes: List<SavedHike>
        try {
          // Load the saved hikes from the repository
          savedHikes = savedHikesRepo.loadSavedHikes()
        } catch (e: Exception) {
          Log.e(LOG_TAG, "Error encountered while loading saved hikes", e)
          onFailure()
          return@withContext false
        }

        // Wait for the lock to avoid concurrent modifications
        _hikesMutex.withLock { updateSavedHikesCache(savedHikes) }

        onSuccess()
        return@withContext true
      }

  /**
   * Helper function for [refreshSavedHikesCacheAsync].
   *
   * Takes a list of saved hikes and updates the local cache of saved hikes with that list.
   *
   * It is the caller's responsibility to ensure the saved hikes list is up-to-date and to acquire
   * [_hikesMutex] before calling this function.
   *
   * @param newList The list of saved hikes to update the cache with.
   */
  private fun updateSavedHikesCache(newList: List<SavedHike>) {
    // Clear the current saved hikes register
    _savedHikesMap.clear()

    // Build the new saved hikes register
    newList.forEach { savedHike -> _savedHikesMap[savedHike.id] = savedHike }

    // Update the map of hikes to reflect the changes
    val toRemove: MutableSet<String> = mutableSetOf()
    _hikeFlowsMap.values.forEach { hikeFlow ->
      val hike = hikeFlow.value
      val savedHike = _savedHikesMap[hike.id]
      // If the list currently only contains saved hikes and this hike was unsaved, remove it
      if (_loadedHikesType == LoadedHikes.FromSaved && savedHike == null) {
        toRemove.add(hike.id)
      }
      // Otherwise, check if a change actually happened to avoid unnecessary updates
      else {
        val (changeNeeded, updated) = hikeNeedsSavedStatusUpdate(hike, savedHike)
        if (changeNeeded) {
          hikeFlow.value = updated
        }
      }
    }

    // Remove the hikes that need to be removed
    for (hikeId in toRemove) {
      _hikeFlowsMap.remove(hikeId)
    }

    // Add the saved hikes that were not in the list yet
    if (_loadedHikesType == LoadedHikes.FromSaved) {
      _savedHikesMap.forEach { (hikeId, savedHike) ->
        if (!_hikeFlowsMap.containsKey(hikeId)) {
          _hikeFlowsMap[hikeId] = MutableStateFlow(
            Hike(
              id = savedHike.id,
              isSaved = true,
              plannedDate = savedHike.date,
              name = savedHike.name
            )
          )
        }
      }
    }

    // Update the selected hike's saved status, unselect it if it's not loaded anymore
    updateSelectedHikeSavedStatus()

    // Update the exposed list of hikes based on the map of hikes
    updateHikeFlowsList()
  }

  /**
   * Helper function for [updateSavedHikesCache].
   *
   * Updates the saved status of the selected hike if it is currently loaded (sets it to saved if it
   * was updated from unsaved to saved, ...).
   *
   * Unselects the selected hike if it was unloaded (removed from [hikeFlows]).
   *
   * This function does not acquire the [_hikesMutex]. It is the responsibility of the caller to
   * call this function inside of a [Mutex.withLock] block.
   *
   * This function does not switch context either, it is the responsibility of the caller to call
   * this function inside of a [withContext] block.
   */
  private fun updateSelectedHikeSavedStatus() {
    val selectedHike = _selectedHike.value
    // Only bother to update the selected hike if there is one
    if (selectedHike != null) {
      if (_hikeFlowsMap.containsKey(selectedHike.id)) {
        // The selected hike is still loaded, update its saved status
        val savedHike = _savedHikesMap[selectedHike.id]
        val (changeNeeded, updated) = hikeNeedsSavedStatusUpdate(selectedHike, savedHike)
        if (changeNeeded) {
          _selectedHike.value = updated
        }
      } else {
        // The selected hike was unloaded, unselect it
        _selectedHikeId = null
        _selectedHike.value = null
      }
    }
  }

  private suspend fun loadSavedHikesAsync(onSuccess: () -> Unit, onFailure: () -> Unit) =
      withContext(dispatcher) {
        // Remember that the loaded hikes list will now only hold saved hikes
        _loadedHikesType = LoadedHikes.FromSaved

        // Update the local cache of saved hikes and add them to _hikeFlows
        val success = refreshSavedHikesCacheAsync()

        if (success) {
          onSuccess()
        } else {
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
    try {
      savedHikesRepo.addSavedHike(SavedHike(hike.id, hike.name ?: "", hike.plannedDate))
    } catch (e: Exception) {
      Log.e(LOG_TAG, "Error encountered while saving hike", e)
      return false
    }

    // Update the saved hikes map
    _savedHikesMap[hikeId] = SavedHike(hike.id, hike.name ?: "", hike.plannedDate)

    // Update the hike's state flow
    val newHikeState = hikeFlow.value.copy(isSaved = true)
    hikeFlow.value = newHikeState

    // Update the selected hike if necessary
    updateSelectedHikeSavedStatus()

    return true
  }

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
            savedHikesRepo.removeSavedHike(SavedHike(hike.id, hike.name ?: "", hike.plannedDate))
          } catch (e: Exception) {
            Log.e(LOG_TAG, "Error encountered while unsaving hike", e)
            successful = false
            return@withLock
          }

          // Update the saved hikes map
          _savedHikesMap.remove(hikeId)

          if (_loadedHikesType == LoadedHikes.FromSaved) {
            // Only saved hikes may stay in the list, delete the unsaved hike from the list
            _hikeFlowsMap.remove(hikeId)
            updateHikeFlowsList()
          } else {
            // The hike can stay even if it is not saved, so update it
            val newHikeState = hikeFlow.value.copy(isSaved = false, plannedDate = null)
            hikeFlow.value = newHikeState
          }

          // Update the selected hike if necessary
          updateSelectedHikeSavedStatus()

          successful = true
        }

        // Perform the callback without the lock, to avoid deadlocks and improve performance
        if (successful) {
          onSuccess()
        } else {
          onFailure()
        }
      }

  private suspend fun setPlannedDateAsync(
      hikeId: String,
      date: Timestamp?,
      onSuccess: () -> Unit,
      onFailure: () -> Unit
  ) =
      withContext(dispatcher) {
        var successful = false
        _hikesMutex.withLock {
          // Retrieve the hike to update from the loaded hikes
          val hikeFlow = _hikeFlowsMap[hikeId] ?: return@withLock
          successful = updateHikePlannedDateAsync(hikeFlow, date)
        }

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
   * @param hikeFlow The flow of the hike to update.
   * @return True if the operation was successful, false otherwise.
   */
  private suspend fun updateHikePlannedDateAsync(
      hikeFlow: MutableStateFlow<Hike>,
      date: Timestamp?
  ): Boolean {
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
    val newSavedHike = SavedHike(id = hike.id, name = hike.name ?: "", date = date)
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
    updateSelectedHikeSavedStatus()

    return true
  }

  private suspend fun loadHikesInBoundsAsync(
      boundingBox: BoundingBox,
      onSuccess: () -> Unit,
      onFailure: () -> Unit
  ) =
      withContext(dispatcher) {
        // We are loading hikes from bounds, remember this to avoid overriding loaded hikes with saved
        // hikes when those get reloaded.
        _loadedHikesType = LoadedHikes.FromBounds

        // Load the hikes from the repository
        val hikes: List<HikeRoute>
        try {
          hikes = loadHikesInBoundsRepoWrapper(boundingBox.toBounds())
        } catch (e: Exception) {
          Log.e(LOG_TAG, "Error encountered while loading hikes in bounds", e)
          _loading.value = false
          onFailure()
          return@withContext
        }

        // Wait for the lock to avoid concurrent modifications
        _hikesMutex.withLock {
          // Keep already loaded hikes, discard the ones that do not appear in the given bounds
          _hikeFlowsMap =
              _hikeFlowsMap
                  .filterKeys { key -> hikes.none { hike -> hike.id == key } }
                  .toMutableMap()

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
                      // Assume bounds and waypoints don't change for a hike. If they are already
                      // loaded, don't update them
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
                  .toMap()
                  .toMutableMap()

          // Update the exposed list of hikes based on the map of hikes
          updateHikeFlowsList()
        }

        // Call the success callback once the mutex has been released to avoid locking for too long
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

  private fun hasOsmData(hike: Hike): Boolean =
      hike.description is DeferredData.Obtained &&
          hike.bounds is DeferredData.Obtained &&
          hike.waypoints is DeferredData.Obtained

  private suspend fun retrieveLoadedHikesOsmDataAsync(
      onSuccess: () -> Unit,
      onFailure: () -> Unit
  ) =
      withContext(dispatcher) {
        // Notify the UI that a heavy loading operation is in progress
        _loading.value = true

        // Prepare a list of hikes for which we need to retrieve the data
        val idsToRetrieve: List<String>
        _hikesMutex.withLock {
          idsToRetrieve = _hikeFlowsMap.values.filter { hasOsmData(it.value) }.map { it.value.id }
        }

        // Retrieve the OSM data of the hikes
        val hikeRoutes: List<HikeRoute>
        try {
          hikeRoutes = loadHikesByIdsRepoWrapper(idsToRetrieve)
        } catch (e: Exception) {
          Log.e(LOG_TAG, "Error encountered while loading hikes OSM data", e)
          onFailure()
          return@withContext
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
        }

        // Release the mutex before calling onSuccess to avoid deadlocks or performance issues
        onSuccess()
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
          alreadyComputed = hike.elevation !is DeferredData.NotRequested
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
          val hikeMarkedAsRequested = hike.copy(elevation = DeferredData.Requested)
          hikeFlow.value = hikeMarkedAsRequested
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
          elevation = getElevationRepoWrapper(waypoints, hikeId)
        } catch (e: Exception) {
          Log.e(LOG_TAG, "Error encountered while retrieving elevation", e)
          onFailure()
          return@withContext
        }

        // Update the hike's elevation data
        success = false
        _hikesMutex.withLock {
          val hikeFlow = _hikeFlowsMap[hikeId] ?: return@withLock
          val hike = hikeFlow.value

          val updatedHike = hike.copy(elevation = DeferredData.Obtained(elevation))
          hikeFlow.value = updatedHike
          success = true
        }

        if (success) {
          onSuccess()
        } else {
          onFailure()
        }
      }

  /**
   * The [ElevationService] interface has been developed without coroutines in mind, hence we need a
   * wrapper to "convert" the [ElevationService.getElevation] function that uses callback to a
   * suspend function.
   */
  private suspend fun getElevationRepoWrapper(
      coordinates: List<LatLong>,
      hikeId: String
  ): List<Double> = suspendCoroutine { continuation ->
    elevationRepo.getElevation(
        coordinates = coordinates,
        hikeID = hikeId,
        onSuccess = { elevation -> continuation.resume(elevation) },
        onFailure = { exception -> continuation.resumeWithException(exception) })
  }

  private suspend fun retrieveDetailsForAsync(
      hikeId: String,
      onSuccess: () -> Unit,
      onFailure: () -> Unit
  ) =
      withContext(dispatcher) {
        // Retrieve the current value of the hike and note what computations are required
        val (performRequest, waypoints, elevation) =
            checkDetailsCanBeRetrievedForAsync(hikeId, onSuccess, onFailure)

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
            val updatedHike =
                hikeFlow.value.copy(
                    distance = DeferredData.NotRequested,
                    elevationGain = DeferredData.NotRequested,
                    estimatedTime = DeferredData.NotRequested,
                    difficulty = DeferredData.NotRequested,
                )
            hikeFlow.value = updatedHike
          }

          onFailure()
          return@withContext
        }

        // Update the hike in the list
        var success = false
        _hikesMutex.withLock {
          val hikeFlow = _hikeFlowsMap[hikeId] ?: return@withLock
          val updatedHike =
              hikeFlow.value.copy(
                  distance = DeferredData.Obtained(distance),
                  elevationGain = DeferredData.Obtained(elevationGain),
                  estimatedTime = DeferredData.Obtained(estimatedTime),
                  difficulty = DeferredData.Obtained(difficulty))
          hikeFlow.value = updatedHike
          success = true
        }

        if (success) {
          onSuccess()
        } else {
          onFailure()
        }
      }

  /**
   * Helper function for [retrieveDetailsForAsync].
   *
   * Checks that the required data ([Hike.waypoints] and [Hike.elevation]) are available, and that a
   * request is not already ongoing. Returns the data if it is available, and indicates whether a
   * request should be performed.
   *
   * If a request should be performed, this function will also mark the data as requested.
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
  private suspend fun checkDetailsCanBeRetrievedForAsync(
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
      val hikeWithRequestedAttributes =
          hike.copy(
              distance = DeferredData.Requested,
              estimatedTime = DeferredData.Requested,
              difficulty = DeferredData.Requested,
              elevationGain = DeferredData.Requested,
          )
      hikeFlow.value = hikeWithRequestedAttributes

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
