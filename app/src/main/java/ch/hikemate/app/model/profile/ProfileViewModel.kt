package ch.hikemate.app.model.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
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

  // StateFlow to indicate if the profile is ready
  private val _isProfileReady = MutableStateFlow(false)
  val isProfileReady: StateFlow<Boolean> = _isProfileReady.asStateFlow()

  init {
    FirebaseAuth.getInstance().addAuthStateListener { auth ->
      auth.currentUser?.uid?.let { userId -> getProfileById(userId) }
    }
    repository.init { FirebaseAuth.getInstance().currentUser?.uid?.let { getProfileById(it) } }
  }

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
        onSuccess = { profile ->
          profile_.value = profile
          _isProfileReady.value = profile != null // Update readiness status
        },
        onFailure = {
          _isProfileReady.value = false // Set readiness to false on failure
        })
  }

  /**
   * Adds a profile.
   *
   * @param profile The profile to be added.
   */
  fun addProfile(profile: Profile) {
    repository.addProfile(
        profile = profile,
        onSuccess = {
          getProfileById(profile.id)
          _isProfileReady.value = true // Set readiness after creation
        },
        onFailure = { _isProfileReady.value = false })
  }

  /**
   * Updates a profile.
   *
   * @param profile The profile to be updated.
   */
  fun updateProfile(profile: Profile) {
    repository.updateProfile(
        profile = profile,
        onSuccess = {
          profile_.value = profile
          _isProfileReady.value = true // Update readiness on successful update
        },
        onFailure = { _isProfileReady.value = false })
  }

  /**
   * Deletes a profile by its ID.
   *
   * @param id The ID of the profile to delete.
   */
  fun deleteProfileById(id: String) {
    repository.deleteProfileById(
        id = id,
        onSuccess = {
          profile_.value = null
          _isProfileReady.value = false // Reset readiness after deletion
        },
        onFailure = {})
  }

  /** Reload the profile. */
  fun reloadProfile() {
    profile_.value?.let { getProfileById(it.id) }
  }
}
