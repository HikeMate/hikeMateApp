package ch.hikemate.app.model.profile

import com.google.firebase.auth.FirebaseAuth

/** Interface for the profile repository. */
interface ProfileRepository {
  fun createProfile(
      firebaseAuth: FirebaseAuth,
      onSuccess: (Profile) -> Unit,
      onFailure: (Exception) -> Unit
  )

  /** Generates a new unique ID. */
  fun getNewUid(): String

  /**
   * Initializes the profile repository.
   *
   * @param onSuccess The callback to be called when the repository is successfully initialized.
   */
  fun init(onSuccess: () -> Unit)

  /**
   * Returns the profile with the given ID.
   *
   * @param id The ID of the profile to fetch.
   * @param onSuccess The callback to be called when the profile is successfully fetched.
   * @param onFailure The callback to be called when the profile could not be fetched.
   */
  fun getProfileById(id: String, onSuccess: (Profile) -> Unit, onFailure: (Exception) -> Unit)

  /**
   * Adds a profile.
   *
   * @param profile The profile to be added.
   * @param onSuccess The callback to be called when the profile is successfully added.
   * @param onFailure The callback to be called when the profile could not be added.
   */
  fun addProfile(profile: Profile, onSuccess: () -> Unit, onFailure: (Exception) -> Unit)

  /**
   * Updates a profile.
   *
   * @param profile The profile to be updated.
   * @param onSuccess The callback to be called when the profile is successfully updated.
   * @param onFailure The callback to be called when the profile could not be updated.
   */
  fun updateProfile(profile: Profile, onSuccess: () -> Unit, onFailure: (Exception) -> Unit)

  /**
   * Deletes a profile with the given ID.
   *
   * @param id The ID of the profile to delete.
   * @param onSuccess The callback to be called when the profile is successfully deleted.
   * @param onFailure The callback to be called when the profile could not be deleted.
   */
  fun deleteProfileById(id: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit)
}
