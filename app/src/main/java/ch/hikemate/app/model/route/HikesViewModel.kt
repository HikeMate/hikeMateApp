package ch.hikemate.app.model.route

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.hikemate.app.model.elevation.ElevationService
import ch.hikemate.app.model.route.saved.SavedHike
import ch.hikemate.app.model.route.saved.SavedHikesRepository
import com.google.firebase.Timestamp
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
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

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

  private val _loading = MutableStateFlow<Boolean>(false)

  private val _hikeFlowsList = MutableStateFlow<List<StateFlow<Hike>>>(emptyList())

  private fun updateHikeFlowsList() {
    _hikeFlowsList.value = _hikeFlowsMap.values.toList()
  }

  private var _selectedHikeId: String? = null
  private val _selectedHike = MutableStateFlow<Hike?>(null)

  /**
   * Internal type used to store where the hikes in [_hikeFlowsMap] come from.
   */
  private enum class LoadedHikes {
    /**
     * No hikes have been loaded yet.
     */
    None,

    /**
     * The hikes loaded in [_hikeFlowsMap] are the saved hikes.
     */
    FromSaved,

    /**
     * The hikes loaded in [_hikeFlowsMap] were fetched from within a geographical rectangle.
     */
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
  fun selectHike(hikeId: String, onSuccess: () -> Unit, onFailure: () -> Unit) =
    viewModelScope.launch { selectHikeAsync(hikeId, onSuccess, onFailure) }

  /**
   * Sets [selectedHike] to null.
   *
   * Note: the currently selected hike will NOT be removed from [hikeFlows], only [selectedHike]
   * will be impacted.
   */
  fun unselectHike() =
    viewModelScope.launch { unselectHikeAsync() }

  /**
   * Downloads the current user's saved hikes from the database and caches them locally.
   *
   * This function DOES NOT update [hikeFlows], it simply caches saved hikes locally. To update
   * [hikeFlows] with the list of saved hikes, see [loadSavedHikes].
   *
   * @param onSuccess To be called when the saved hikes cache has been updated successfully.
   * @param onFailure Will be called if an error is encountered.
   */
  fun refreshSavedHikesCache(onSuccess: () -> Unit, onFailure: () -> Unit) =
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
  fun loadSavedHikes(onSuccess: () -> Unit, onFailure: () -> Unit) =
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
  fun saveHike(hikeId: String, onSuccess: () -> Unit, onFailure: () -> Unit) =
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
  fun unsaveHike(hikeId: String, onSuccess: () -> Unit, onFailure: () -> Unit) =
    viewModelScope.launch { unsaveHikeAsync(hikeId, onSuccess, onFailure) }

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
  fun loadHikesInBounds(bounds: BoundingBox, onSuccess: () -> Unit, onFailure: () -> Unit) =
    viewModelScope.launch { loadHikesInBoundsAsync(bounds, onSuccess, onFailure) }

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
  fun retrieveLoadedHikesOsmData(onSuccess: () -> Unit, onFailure: () -> Unit) =
    viewModelScope.launch { retrieveLoadedHikesOsmDataAsync(onSuccess, onFailure) }

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
  fun retrieveElevationDataFor(hike: Hike, onSuccess: () -> Unit, onFailure: () -> Unit) =
    viewModelScope.launch { retrieveElevationDataForAsync(hike, onSuccess, onFailure) }

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
  ) =
    viewModelScope.launch { setPlannedDateAsync(hikeId, date, onSuccess, onFailure) }

  private suspend fun selectHikeAsync(hikeId: String, onSuccess: () -> Unit, onFailure: () -> Unit) =
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
      }
      else {
        onFailure()
      }
    }

  private suspend fun unselectHikeAsync() =
    _hikesMutex.withLock {
      _selectedHikeId = null
      _selectedHike.value = null
    }

  /**
   * This function performs a loading operation but DOES NOT set [_loading]. If calling it from
   * another function, please set [_loading] to true before calling the function and to false once
   * the call has ended.
   *
   * @return True if the operation is successful, false otherwise.
   */
  private suspend fun refreshSavedHikesCacheAsync(onSuccess: () -> Unit = {}, onFailure: () -> Unit = {}): Boolean =
    withContext(dispatcher) {
      val savedHikes: List<SavedHike>
      try {
        // Load the saved hikes from the repository
        savedHikes = savedHikesRepo.loadSavedHikes()
      }
      catch (e: Exception) {
        Log.e(LOG_TAG, "Error encountered while loading saved hikes", e)
        _loading.value = false
        onFailure()
        return@withContext false
      }

      // Wait for the lock to avoid concurrent modifications
      _hikesMutex.withLock {
        // Clear the current saved hikes register
        _savedHikesMap.clear()

        // Build the new saved hikes register
        savedHikes.forEach { savedHike ->
          _savedHikesMap[savedHike.id] = savedHike
        }

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

        // Update the exposed list of hikes based on the map of hikes
        updateHikeFlowsList()
      }

      onSuccess()
      return@withContext true
    }

  private suspend fun loadSavedHikesAsync(onSuccess: () -> Unit, onFailure: () -> Unit) =
    withContext(dispatcher) {
      // Indicate the view model is currently loading, for the UI to tell the user in some way
      _loading.value = true

      // Remember that the loaded hikes list will now only hold saved hikes
      _loadedHikesType = LoadedHikes.FromSaved

      // Update the local cache of saved hikes and add them to _hikeFlows
      val success = refreshSavedHikesCacheAsync()

      // Indicate the view model has finished the heavy loading operation
      _loading.value = false

      if (success) {
        onSuccess()
      }
      else {
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
   * not saved.
   *
   * @return (false, [hike]) if the hike does not need an update, (true, updated) if the hike does
   * need an update, with updated being the hike with the new saved status.
   */
  private fun hikeNeedsSavedStatusUpdate(hike: Hike, savedHike: SavedHike?): Pair<Boolean, Hike> {
    // If the hike is marked as saved but was unsaved, clear its saved state
    if (savedHike == null) {
      return if (hike.isSaved) {
        Pair(true, hike.copy(isSaved = false, plannedDate = null))
      }
      else {
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
      _hikesMutex.withLock {
        successful = saveHikeAsyncInternal(hikeId)
      }

      // Perform the callback without the lock, to avoid deadlocks and improve performance
      if (successful) {
        onSuccess()
      }
      else {
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
   *
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
    }
    catch (e: Exception) {
      Log.e(LOG_TAG, "Error encountered while saving hike", e)
      return false
    }

    // Update the saved hikes map
    _savedHikesMap[hikeId] = SavedHike(hike.id, hike.name ?: "", hike.plannedDate)

    // Update the hike's state flow
    val newHikeState = hikeFlow.value.copy(isSaved = true)
    hikeFlow.value = newHikeState

    return true
  }

  private suspend fun unsaveHikeAsync(hikeId: String, onSuccess: () -> Unit, onFailure: () -> Unit) =
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
        }
        catch (e: Exception) {
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
        }
        else {
          // The hike can stay even if it is not saved, so update it
          val newHikeState = hikeFlow.value.copy(isSaved = false, plannedDate = null)
          hikeFlow.value = newHikeState
        }

        successful = true
      }

      // Perform the callback without the lock, to avoid deadlocks and improve performance
      if (successful) {
        onSuccess()
      }
      else {
        onFailure()
      }
    }

  private suspend fun setPlannedDateAsync(hikeId: String, date: Timestamp?, onSuccess: () -> Unit, onFailure: () -> Unit) {
    // TODO : Implement HikesViewModel.setPlannedDateAsync
  }

  private suspend fun loadHikesInBoundsAsync(boundingBox: BoundingBox, onSuccess: () -> Unit, onFailure: () -> Unit) =
    withContext(dispatcher) {
      // Let the user know a heavy load operation is being performed
      _loading.value = true

      // Load the hikes from the repository
      val hikes: List<HikeRoute>
      try {
        hikes = loadHikesInBoundsRepoWrapper(boundingBox.toBounds())
      }
      catch (e: Exception) {
        Log.e(LOG_TAG, "Error encountered while loading hikes in bounds", e)
        _loading.value = false
        onFailure()
        return@withContext
      }

      // Wait for the lock to avoid concurrent modifications
      _hikesMutex.withLock {
        // Keep already loaded hikes, discard the ones that do not appear in the given bounds
        _hikeFlowsMap = _hikeFlowsMap.filterKeys { key -> hikes.none { hike -> hike.id == key } }.toMutableMap()

        // Add the new hikes to the map of hikes
        _hikeFlowsMap = hikes.map { osmData ->
          val existingFlow = _hikeFlowsMap[osmData.id]

          // The hike is not in the map yet, add it
          return@map if (existingFlow == null) {
            val hike = Hike(
              id = osmData.id,
              isSaved = _savedHikesMap.containsKey(osmData.id),
              plannedDate = _savedHikesMap[osmData.id]?.date,
              name = osmData.name,
              description = osmData.description,
              bounds = DeferredData.Obtained(osmData.bounds),
              waypoints = DeferredData.Obtained(osmData.ways)
            )
            Pair(hike.id, MutableStateFlow(hike))

          } else {
            // The hike is already in the map, update it
            val existingHike = existingFlow.value
            // Assume bounds and waypoints don't change for a hike. If they are already loaded, don't update them
            if (existingHike.bounds !is DeferredData.Obtained || existingHike.waypoints !is DeferredData.Obtained) {
              val newHike = existingHike.copy(bounds = DeferredData.Obtained(osmData.bounds), waypoints = DeferredData.Obtained(osmData.ways))
              // As the list will be updated as a whole, no need to update the state flow here
              Pair(existingHike.id, MutableStateFlow(newHike))
            }
            else {
              Pair(existingHike.id, existingFlow)
            }
          }
        }
          .toMap()
          .toMutableMap()

        // Update the exposed list of hikes based on the map of hikes
        updateHikeFlowsList()
      }

      // The heavy loading operation is done now
      _loading.value = false

      // Call the success callback once the mutex has been released to avoid locking for too long
      onSuccess()
    }

  /**
   * The [HikeRoutesRepository] interface has been developed without coroutines in mind, hence we
   * need a wrapper to "convert" the [HikeRoutesRepository.getRoutes] function that uses callback
   * to a suspend function.
   */
  private suspend fun loadHikesInBoundsRepoWrapper(bounds: Bounds): List<HikeRoute> =
    suspendCoroutine { continuation ->
      osmHikesRepo.getRoutes(
        bounds = bounds,
        onSuccess = { routes -> continuation.resume(routes) },
        onFailure = { exception -> continuation.resumeWithException(exception) }
      )
    }

  private suspend fun retrieveLoadedHikesOsmDataAsync(onSuccess: () -> Unit, onFailure: () -> Unit) {
    // TODO : Implement HikesViewModel.retrieveLoadedHikesOsmDataAsync
  }

  private suspend fun retrieveElevationDataForAsync(hike: Hike, onSuccess: () -> Unit, onFailure: () -> Unit) {
    // TODO : Implement HikesViewModel.retrieveElevationDataForAsync
  }
}
