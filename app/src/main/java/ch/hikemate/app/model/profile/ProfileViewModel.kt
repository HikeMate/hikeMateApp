package ch.hikemate.app.model.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The view model for the profile.
 *
 * @param repository The profile repository.
 */
open class ProfileViewModel(private val repository: ProfileRepository) : ViewModel() {
  // The profile, i.e the profile for the detail view
  // This will probably always be the current user profile
  private val profile_ = MutableStateFlow<Profile?>(null)
  val profile: StateFlow<Profile?> = profile_.asStateFlow()

  init {
    repository.init {
      val firebaseInstance = FirebaseAuth.getInstance()
      firebaseInstance.currentUser?.uid?.let { checkAndCreateProfile(it, firebaseInstance) }
    }
  }

  private fun checkAndCreateProfile(userId: String, firebaseInstance: FirebaseAuth) {
    repository.getProfileById(
        userId,
        onSuccess = { if (it == null) createProfile(firebaseInstance) else profile_.value = it },
        onFailure = { throw it })
  }

  fun createProfile(firebaseInstance: FirebaseAuth) {
    repository.createProfile(
        firebaseInstance, onSuccess = { profile_.value = it }, onFailure = { throw it })
  }
  /**
   * Get a profile by its ID.
   *
   * @param id The ID of the profile to fetch.
   */
  fun getProfileById(id: String) {
    repository.getProfileById(
        id, onSuccess = { profile_.value = it }, onFailure = { throw Exception(it) })
  }

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
   * Adds a profile.
   *
   * @param profile The profile to be added.
   */
  fun addProfile(profile: Profile) {
    repository.addProfile(
        profile = profile, onSuccess = { getProfileById(profile.id) }, onFailure = {})
  }

  /**
   * Updates a profile.
   *
   * @param profile The profile to be updated.
   */
  fun updateProfile(profile: Profile) {
    repository.updateProfile(
        profile = profile, onSuccess = { profile_.value = profile }, onFailure = {})
  }

  /**
   * Deletes a profile by its ID.
   *
   * @param id The ID of the profile to delete.
   */
  fun deleteProfileById(id: String) {
    repository.deleteProfileById(id = id, onSuccess = { profile_.value = null }, onFailure = {})
  }
}
