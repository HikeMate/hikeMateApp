package ch.hikemate.app.model.route.saved

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

  companion object {
    /** Tag used for logging. */
    private const val LOG_TAG = "SavedHikesViewModel"
  }

  private val _savedHikes = MutableStateFlow<List<SavedHike>>(emptyList())
  /**
   * The list of saved hikes for the user as a state flow. Observe this to get updates when the
   * saved hikes change.
   */
  val savedHike: StateFlow<List<SavedHike>> = _savedHikes.asStateFlow()

  /** Load saved hikes from the repository and update the [savedHike] state flow. */
  fun loadSavedHikes() {
    // Because loading saved hikes is also used by other methods, we extract it
    viewModelScope.launch { loadSavedHikesAsync() }
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
      } catch (e: Exception) {
        Log.e(LOG_TAG, "Error adding saved hike", e)
        return@launch
      }
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
      } catch (e: Exception) {
        Log.e(LOG_TAG, "Error removing saved hike", e)
        return@launch
      }
      loadSavedHikesAsync()
    }
  }

  private suspend fun loadSavedHikesAsync() {
    withContext(dispatcher) {
      try {
        _savedHikes.value = repository.loadSavedHikes()
      } catch (e: Exception) {
        Log.e(LOG_TAG, "Error loading saved hikes", e)
      }
    }
  }
}
