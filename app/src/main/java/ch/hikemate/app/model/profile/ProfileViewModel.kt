package ch.hikemate.app.model.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ch.hikemate.app.R
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The view model for the profile.
 *
 * @param repository The profile repository.
 */
open class ProfileViewModel(private val repository: ProfileRepository) : ViewModel() {

  // The profile for the detail view
  private val profile_ = MutableStateFlow<Profile?>(null)
  val profile: StateFlow<Profile?> = profile_.asStateFlow()

  private val _errorMessageId = MutableStateFlow<Int?>(null)
  /**
   * If an error occurs while performing an operation related to saved hikes, the resource ID of an
   * appropriate error message will be set in this state flow.
   */
  val errorMessageId: StateFlow<Int?> = _errorMessageId.asStateFlow()

  // Factory for creating instances of ProfileViewModel
  companion object {
    val Factory: ViewModelProvider.Factory =
        object : ViewModelProvider.Factory {
          @Suppress("UNCHECKED_CAST")
          override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ProfileViewModel(ProfileRepositoryFirestore(Firebase.firestore)) as T
          }
        }
  }

  /**
   * Get a profile by its ID.
   *
   * @param id The ID of the profile to fetch.
   */
  fun getProfileById(id: String) {
    repository.getProfileById(
        id,
        onSuccess = { profile -> profile_.value = profile },
        onFailure = {
          Log.e("ProfileViewModel", "Error fetching profile", it)
          _errorMessageId.value = R.string.an_error_occurred_while_fetching_the_profile
        })
  }

  /**
   * Updates a profile.
   *
   * @param profile The profile to be updated.
   */
  fun updateProfile(profile: Profile) {
    repository.updateProfile(
        profile = profile,
        onSuccess = { profile_.value = profile },
        onFailure = {
          Log.e("ProfileViewModel", "Error updating profile", it)
          _errorMessageId.value = R.string.an_error_occurred_while_updating_the_profile
        })
  }

  /**
   * Deletes a profile by its ID.
   *
   * @param id The ID of the profile to delete.
   */
  fun deleteProfileById(id: String) {
    repository.deleteProfileById(
        id = id,
        onSuccess = { profile_.value = null },
        onFailure = {
          Log.e("ProfileViewModel", "Error deleting profile", it)
          _errorMessageId.value = R.string.an_error_occurred_while_deleting_the_profile
        })
  }
}
