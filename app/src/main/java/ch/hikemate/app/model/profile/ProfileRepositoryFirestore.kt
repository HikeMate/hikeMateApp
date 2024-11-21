package ch.hikemate.app.model.profile

import android.content.Context
import android.util.Log
import ch.hikemate.app.R
import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

/**
 * A Firestore implementation of the profile repository.
 *
 * @property db The Firestore database.
 */
@Suppress("UNCHECKED_CAST")
class ProfileRepositoryFirestore(private val db: FirebaseFirestore) : ProfileRepository {

  override fun getNewUid(): String {
    return db.collection(PROFILES_COLLECTION).document().id
  }

  override fun createProfile(
      fireUser: FirebaseUser?,
      onSuccess: (Profile) -> Unit,
      onFailure: (Exception) -> Unit,
      context: Context
  ) {
    if (fireUser == null) {
      onFailure(Exception("User is null"))
      return
    }
    val displayName = fireUser.displayName ?: context.getString(R.string.default_display_name)
    val email = fireUser.email ?: context.getString(R.string.default_email)
    val profile = Profile(fireUser.uid, displayName, email, HikingLevel.BEGINNER, Timestamp.now())

    profileExists(
        fireUser.uid,
        onSuccess = { exists ->
          if (exists) {
            getProfileById(fireUser.uid, onSuccess, onFailure)
          } else {
            val task = db.collection(PROFILES_COLLECTION).document(profile.id).set(profile)

            performFirestoreOperation(task as Task<Unit>, { onSuccess(profile) }, onFailure)
          }
        },
        onFailure = onFailure)
  }
  /**
   * Checks if the profile with the given ID exists.
   *
   * @param id The ID of the profile to check.
   * @param onSuccess The callback to call if the profile exists.
   * @param onFailure The callback to call if the profile does not exist.
   */
  private fun profileExists(
      id: String,
      onSuccess: (Boolean) -> Unit,
      onFailure: (Exception) -> Unit
  ) {
    db.collection(PROFILES_COLLECTION).document(id).get().addOnCompleteListener { task ->
      if (task.isSuccessful) {
        task.result?.let { onSuccess(it.exists()) }
      } else {
        task.exception?.let { e ->
          Log.e("ProfileRepositoryFirestore", "Error getting document", e)
          onFailure(e)
        }
      }
    }
  }

  override fun getProfileById(
      id: String,
      onSuccess: (Profile) -> Unit,
      onFailure: (Exception) -> Unit
  ) {

    db.collection(PROFILES_COLLECTION).document(id).get().addOnCompleteListener { task ->
      if (task.isSuccessful) {
        val profile = task.result?.let { documentToProfile(it) }
        if (profile != null) onSuccess(profile)
        else onFailure(Exception("Error converting document to Profile"))
      } else {
        task.exception?.let { e ->
          Log.e("ProfileRepositoryFirestore", "Error getting document", e)
          onFailure(e)
        }
      }
    }
  }

  override fun updateProfile(
      profile: Profile,
      onSuccess: () -> Unit,
      onFailure: (Exception) -> Unit
  ) {

    val task = db.collection(PROFILES_COLLECTION).document(profile.id).set(profile)

    performFirestoreOperation(task as Task<Unit>, onSuccess, onFailure)
  }

  override fun deleteProfileById(
      id: String,
      onSuccess: () -> Unit,
      onFailure: (Exception) -> Unit
  ) {
    val task = db.collection(PROFILES_COLLECTION).document(id).delete()

    performFirestoreOperation(task as Task<Unit>, onSuccess, onFailure)
  }

  /**
   * Performs a Firestore operation and calls the appropriate callback based on the result. Function
   * taken from the solution of the bootcamp.
   *
   * @param task The Firestore task to perform.
   * @param onSuccess The callback to call if the operation is successful.
   * @param onFailure The callback to call if the operation fails.
   */
  private fun performFirestoreOperation(
      task: Task<Unit>,
      onSuccess: () -> Unit,
      onFailure: (Exception) -> Unit
  ) {
    task.addOnCompleteListener { result ->
      if (result.isSuccessful) {
        onSuccess()
      } else {
        result.exception?.let { e ->
          Log.e("ProfileRepositoryFirestore", "Error performing Firestore operation", e)
          onFailure(e)
        }
      }
    }
  }

  /**
   * Converts a Firestore document to a Profile object. Function inspired from the solution of the
   * bootcamp.
   *
   * @param document The Firestore document to convert.
   */
  fun documentToProfile(document: DocumentSnapshot): Profile? {

    return try {
      val uid = document.id
      val name = document.getString("name")
      val email = document.getString("email")
      val hikingLevelString = document.getString("hikingLevel")
      val hikingLevel = HikingLevel.values().find { it.name == hikingLevelString }
      val joinedDate = document.getTimestamp("joinedDate")
      if (name == null || email == null || hikingLevel == null || joinedDate == null) {
        Log.e("ProfileRepositoryFirestore", "Error converting document to Profile: missing fields")
        return null
      }
      Profile(uid, name, email, hikingLevel, joinedDate)
    } catch (e: Exception) {
      Log.e("ProfileRepositoryFirestore", "Error converting document to Profile", e)
      null
    }
  }

  companion object {
    const val PROFILES_COLLECTION = "profiles"
  }
}
