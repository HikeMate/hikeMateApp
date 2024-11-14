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

  fun updateHikeDetailState(route: HikeRoute) {
    viewModelScope.launch {
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
  }

  fun toggleSaveState() {
    _hikeDetailState.value?.let { currentState ->
      if (currentState.isSaved) {
        // Remove the saved hike
        savedHike.value.find { it.id == currentState.hike.id }?.let { removeSavedHike(it) }
      } else {
        // Add new saved hike
        addSavedHike(
            SavedHike(
                currentState.hike.id,
                currentState.hike.name ?: "Unnamed",
                currentState.plannedDate))
        updateHikeDetailState(currentState.hike)
      }
      // Update the state after toggling
      _hikeDetailState.value =
          currentState.copy(
              isSaved = !currentState.isSaved,
              bookmark =
                  if (!currentState.isSaved) R.drawable.bookmark_filled_blue
                  else R.drawable.bookmark_no_fill)
      loadSavedHikes()
    }
  }

  fun updatePlannedDate(timestamp: Timestamp?) {
    _hikeDetailState.value?.let { currentState ->
      val updatedHike =
          SavedHike(currentState.hike.id, currentState.hike.name ?: "Unnamed", timestamp)
      if (currentState.isSaved) {
        savedHike.value.find { it.id == currentState.hike.id }?.let { removeSavedHike(it) }
      }
      addSavedHike(updatedHike)
      _hikeDetailState.value = currentState.copy(plannedDate = timestamp)
      loadSavedHikes()
    }
  }

  private fun isHikeSaved(): Boolean {
    return _hikeDetailState.value?.isSaved ?: false
  }

  private val _savedHikes = MutableStateFlow<List<SavedHike>>(emptyList())
  /**
   * The list of saved hikes for the user as a state flow. Observe this to get updates when the
   * saved hikes change.
   */
  val savedHike: StateFlow<List<SavedHike>> = _savedHikes.asStateFlow()

  private val _errorMessage = MutableStateFlow<Int?>(null)
  /**
   * If an error occurs while performing an operation related to saved hikes, the resource ID of an
   * appropriate error message will be set in this state flow.
   */
  val errorMessage: StateFlow<Int?> = _errorMessage.asStateFlow()

  /** Load saved hikes from the repository and update the [savedHike] state flow. */
  fun loadSavedHikes() {
    // Because loading saved hikes is also used by other methods, we extract it
    viewModelScope.launch { loadSavedHikesAsync() }
  }

  /**
   * Get the saved hike with the provided ID.
   *
   * @param id The ID of the hike to get.
   * @return The saved hike with the provided ID, or null if no such hike is found.
   */
  fun isHikeSaved(id: String): SavedHike? {
    return savedHike.value.find { id == it.id }
  }

  /**
   * Add the provided hike as a saved hike for the user.
   *
   * This function will update the [savedHike] state flow.
   *
   * @param hike The hike to add as a saved hike.
   */
  fun addSavedHike(hike: SavedHike) {
    viewModelScope.launch(dispatcher) {
      try {
        repository.addSavedHike(hike)
        // Makes no sense to reset the error message here, an error might still occur when
        // loading hikes again
      } catch (e: Exception) {
        Log.e(LOG_TAG, "Error adding saved hike", e)
        _errorMessage.value = R.string.saved_hikes_screen_generic_error
        return@launch
      }
      // As a side-effect, this call will reset the error message if no error occurs
      loadSavedHikesAsync()
    }
  }

  /**
   * Remove the provided hike from the saved hikes for the user.
   *
   * This function will update the [savedHike] state flow.
   *
   * @param hike The hike to remove from the saved hikes.
   */
  fun removeSavedHike(hike: SavedHike) {
    viewModelScope.launch(dispatcher) {
      try {
        repository.removeSavedHike(hike)
        // Makes no sense to reset the error message here, an error might still occur when
        // loading hikes again
      } catch (e: Exception) {
        Log.e(LOG_TAG, "Error removing saved hike", e)
        _errorMessage.value = R.string.saved_hikes_screen_generic_error
        return@launch
      }
      // As a side-effect, this call will reset the error message if no error occurs
      loadSavedHikesAsync()
    }
  }

  private suspend fun loadSavedHikesAsync() {
    withContext(dispatcher) {
      try {
        _savedHikes.value = repository.loadSavedHikes()
        _errorMessage.value = null
      } catch (e: Exception) {
        Log.e(LOG_TAG, "Error loading saved hikes", e)
        _errorMessage.value = R.string.saved_hikes_screen_generic_error
      }
    }
  }
}
