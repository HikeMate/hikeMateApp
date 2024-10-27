package ch.hikemate.app.model.profile

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.getField

/**
 * A Firestore implementation of the profile repository.
 *
 * @property db The Firestore database.
 */
@Suppress("UNCHECKED_CAST")
class ProfileRepositoryFirestore(private val db: FirebaseFirestore) : ProfileRepository {

  private val collectionPath = "profiles"

  override fun getNewUid(): String {
    return db.collection(collectionPath).document().id
  }

  override fun init(onSuccess: () -> Unit) {
    // Check if the user is logged in
    // If the user is logged in, call onSuccess
    Firebase.auth.addAuthStateListener {
      if (it.currentUser != null) {
        onSuccess()
      }
    }
  }

  override fun getProfileById(
      id: String,
      onSuccess: (Profile) -> Unit,
      onFailure: (Exception) -> Unit
  ) {
    // Check that the profile exists

    db.collection(collectionPath).document(id).get().addOnCompleteListener { task ->
      if (task.isSuccessful) {
        val profile = task.result?.let { documentToProfile(it) }
        if (profile != null) {
          onSuccess(profile)
        } else {
          onFailure(Exception("Profile not found"))
        }
      } else {
        task.exception?.let { e ->
          Log.e("ProfileRepositoryFirestore", "Error getting document", e)
          onFailure(e)
        }
      }
    }
  }

  override fun addProfile(profile: Profile, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
    // Call updateProfile as the implementation is the same
    updateProfile(profile, onSuccess, onFailure)
  }

  override fun updateProfile(
      profile: Profile,
      onSuccess: () -> Unit,
      onFailure: (Exception) -> Unit
  ) {

    val task = db.collection(collectionPath).document(profile.id).set(profile)

    performFirestoreOperation(task as Task<Unit>, onSuccess, onFailure)
  }

  override fun deleteProfileById(
      id: String,
      onSuccess: () -> Unit,
      onFailure: (Exception) -> Unit
  ) {
    val task = db.collection(collectionPath).document(id).delete()

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
      val name = document.getString("name") ?: return null
      val email = document.getString("email") ?: return null
      val fitnessLevelData = document.getField("fitnessLevel") as? Map<*, *> ?: return null
      val fitnessLevel =
          fitnessLevelData.let {
            FitnessLevel(
                level = it["level"] as? Int ?: 0, description = it["description"] as? String ?: "")
          }
      val joinedDate = document.getTimestamp("joinedDate") ?: return null
      Profile(uid, name, email, fitnessLevel, joinedDate)
    } catch (e: Exception) {
      Log.e("ProfileRepositoryFirestore", "Error converting document to Profile", e)
      null
    }
  }
}
