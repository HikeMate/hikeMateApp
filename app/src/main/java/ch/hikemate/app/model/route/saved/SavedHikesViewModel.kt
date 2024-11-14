package ch.hikemate.app.model.route.saved

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ch.hikemate.app.R
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

  private val _loadingSavedHikes = MutableStateFlow<Boolean>(false)
  /** Whether the saved hikes list is currently being loaded or reloaded. */
  val loadingSavedHikes: StateFlow<Boolean> = _loadingSavedHikes.asStateFlow()

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
        _loadingSavedHikes.value = true
        _savedHikes.value = repository.loadSavedHikes()
        _errorMessage.value = null
      } catch (e: Exception) {
        Log.e(LOG_TAG, "Error loading saved hikes", e)
        _errorMessage.value = R.string.saved_hikes_screen_generic_error
      }
      _loadingSavedHikes.value = false
    }
  }
}
