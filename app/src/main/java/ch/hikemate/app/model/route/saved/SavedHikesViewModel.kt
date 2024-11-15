package ch.hikemate.app.model.route.saved

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ch.hikemate.app.R
import ch.hikemate.app.model.route.HikeRoute
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SavedHikesViewModel(
    private val repository: SavedHikesRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

  data class HikeDetailState(
      val hike: HikeRoute,
      val bookmark: Int,
      val isSaved: Boolean,
      val plannedDate: Timestamp?
  )

  companion object {
    val Factory: ViewModelProvider.Factory =
        object : ViewModelProvider.Factory {
          @Suppress("UNCHECKED_CAST")
          override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SavedHikesViewModel(
                SavedHikesRepositoryFirestore(
                    FirebaseFirestore.getInstance(), FirebaseAuth.getInstance()))
                as T
          }
        }

    /** Tag used for logging. */
    private const val LOG_TAG = "SavedHikesViewModel"
  }

  private val _hikeDetailState = MutableStateFlow<HikeDetailState?>(null)
  val hikeDetailState: StateFlow<HikeDetailState?> = _hikeDetailState.asStateFlow()

  /**
   * Set the provided route as the "selected" hike for this view model.
   *
   * This function will update the [hikeDetailState] state flow with the details of this hike,
   * whether it is saved and/or planned.
   */
  fun updateHikeDetailState(route: HikeRoute) =
      viewModelScope.launch { updateHikeDetailStateAsync(route) }

  private suspend fun updateHikeDetailStateAsync(route: HikeRoute) =
      withContext(dispatcher) {
        val savedHike = isHikeSaved(route.id)
        _hikeDetailState.value =
            HikeDetailState(
                hike = route,
                isSaved = savedHike != null,
                bookmark =
                    if (savedHike != null) R.drawable.bookmark_filled_blue
                    else R.drawable.bookmark_no_fill,
                plannedDate = savedHike?.date)
      }

  /** Toggles the saved state of the currently selected hike. */
  fun toggleSaveState() = viewModelScope.launch { toggleSaveStateAsync() }

  private suspend fun toggleSaveStateAsync() =
      withContext(dispatcher) {
        // We want to toggle the saved state of the currently selected hike
        val current = _hikeDetailState.value ?: return@withContext

        // If current is already saved, we unsave it
        if (current.isSaved) {
          val savedHikeInfos = savedHike.value?.find { it.id == current.hike.id }
          if (savedHikeInfos != null) {
            // This will mark the current hike as not saved and update the saved hikes list
            removeSavedHikeAsync(savedHikeInfos)
          }
        } else {
          val savedHikeInfo =
              SavedHike(
                  id = current.hike.id,
                  name = current.hike.name ?: "Unnamed",
                  date = current.plannedDate)
          // This will mark the current hike as saved and update the saved hikes list
          addSavedHikeAsync(savedHikeInfo)
        }

        // Update the state after toggling
        _hikeDetailState.value =
            current.copy(
                isSaved = !current.isSaved,
                bookmark =
                    if (!current.isSaved) R.drawable.bookmark_filled_blue
                    else R.drawable.bookmark_no_fill)
      }

  fun updatePlannedDate(timestamp: Timestamp?) {
    viewModelScope.launch { updatePlannedDateAsync(timestamp) }
  }

  private suspend fun updatePlannedDateAsync(timestamp: Timestamp?) =
      withContext(dispatcher) {
        val current = _hikeDetailState.value ?: return@withContext

        val updated =
            SavedHike(id = current.hike.id, name = current.hike.name ?: "Unnamed", date = timestamp)

        // If the hike is already saved, we unsave it to save the new instance of it instead
        if (current.isSaved) {
          savedHike.value?.find { it.id == current.hike.id }?.let { removeSavedHikeAsync(it) }
        }

        // We save the hike with its new planned date
        addSavedHikeAsync(updated)

        // We set the new state with the new planned date
        _hikeDetailState.value = current.copy(plannedDate = timestamp)
      }

  private fun isHikeSaved(): Boolean {
    return _hikeDetailState.value?.isSaved ?: false
  }

  private val _savedHikes = MutableStateFlow<List<SavedHike>>(emptyList())
  /**
   * The list of saved hikes for the user as a state flow. Observe this to get updates when the
   * saved hikes change.
   */
  val savedHike: StateFlow<List<SavedHike>?> = _savedHikes.asStateFlow()

  private val _errorMessage = MutableStateFlow<Int?>(null)
  /**
   * If an error occurs while performing an operation related to saved hikes, the resource ID of an
   * appropriate error message will be set in this state flow.
   */
  val errorMessage: StateFlow<Int?> = _errorMessage.asStateFlow()

  private val _loadingSavedHikes = MutableStateFlow<Boolean>(false)
  /** Whether the saved hikes list is currently being loaded or reloaded. */
  val loadingSavedHikes: StateFlow<Boolean> = _loadingSavedHikes.asStateFlow()

  /**
   * Get the saved hike with the provided ID.
   *
   * @param id The ID of the hike to get.
   * @return The saved hike with the provided ID, or null if no such hike is found.
   */
  fun isHikeSaved(id: String): SavedHike? {
    return savedHike.value?.find { id == it.id }
  }

  /**
   * Add the provided hike as a saved hike for the user.
   *
   * This function will update the [savedHike] state flow.
   *
   * @param hike The hike to add as a saved hike.
   */
  fun addSavedHike(hike: SavedHike) = viewModelScope.launch { addSavedHikeAsync(hike) }

  private suspend fun addSavedHikeAsync(hike: SavedHike) =
      withContext(dispatcher) {
        try {
          repository.addSavedHike(hike)
          // Makes no sense to reset the error message here, an error might still occur when
          // loading hikes again
        } catch (e: Exception) {
          Log.e(LOG_TAG, "Error adding saved hike", e)
          _errorMessage.value = R.string.saved_hikes_screen_generic_error
          return@withContext
        }
        // As a side-effect, this call will reset the error message if no error occurs
        loadSavedHikesAsync()
      }

  /**
   * Remove the provided hike from the saved hikes for the user.
   *
   * This function will update the [savedHike] state flow.
   *
   * @param hike The hike to remove from the saved hikes.
   */
  fun removeSavedHike(hike: SavedHike) = viewModelScope.launch { removeSavedHikeAsync(hike) }

  private suspend fun removeSavedHikeAsync(hike: SavedHike) =
      withContext(dispatcher) {
        try {
          repository.removeSavedHike(hike)
          // Makes no sense to reset the error message here, an error might still occur when
          // loading hikes again
        } catch (e: Exception) {
          Log.e(LOG_TAG, "Error removing saved hike", e)
          _errorMessage.value = R.string.saved_hikes_screen_generic_error
          return@withContext
        }
        // As a side-effect, this call will reset the error message if no error occurs
        loadSavedHikesAsync()
      }

  /** Load saved hikes from the repository and update the [savedHike] state flow. */
  fun loadSavedHikes() = viewModelScope.launch { loadSavedHikesAsync() }

  private suspend fun loadSavedHikesAsync() {
    withContext(dispatcher) {
      try {
        _loadingSavedHikes.value = true
        _savedHikes.value = repository.loadSavedHikes()
        _errorMessage.value = null

        // If the current hike state is not null, update its saved state and planned date
        val current = _hikeDetailState.value
        if (current != null) {
          val savedHike = isHikeSaved(current.hike.id)
          _hikeDetailState.value =
              current.copy(
                  isSaved = savedHike != null,
                  bookmark =
                      if (savedHike != null) R.drawable.bookmark_filled_blue
                      else R.drawable.bookmark_no_fill,
                  plannedDate = savedHike?.date)
        }
      } catch (e: Exception) {
        Log.e(LOG_TAG, "Error loading saved hikes", e)
        _errorMessage.value = R.string.saved_hikes_screen_generic_error
      }
      _loadingSavedHikes.value = false
    }
  }
}
