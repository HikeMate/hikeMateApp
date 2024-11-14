package ch.hikemate.app.model.profile

import android.content.Context
import com.google.firebase.auth.FirebaseUser

/** Interface for the profile repository. */
interface ProfileRepository {
  /**
   * Creates a profile for the current user. Calls updateProfile with the information of the
   * currentUser of the firebaseAuth instance. Can be called when Sigining in and already having an
   * account this will just ignore it.
   *
   * @param fireUser The firebase user to create the profile for.
   * @param context The context of the application.
   * @param onSuccess The callback to call if the profile is created successfully.
   * @param onFailure The callback to call if the profile creation fails.
   */
  fun createProfile(
      fireUser: FirebaseUser?,
      onSuccess: (Profile) -> Unit,
      onFailure: (Exception) -> Unit,
      context: Context
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
