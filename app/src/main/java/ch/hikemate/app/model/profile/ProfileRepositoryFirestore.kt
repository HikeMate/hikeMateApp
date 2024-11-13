package ch.hikemate.app.model.profile

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

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

  /**
   * Creates a profile for the current user. Calls updateProfile with the information of the
   * currentUser of the firebaseAuth instance.
   *
   * @param firebaseAuth The Firebase authentication instance.
   * @param onSuccess The callback to call if the profile is created successfully.
   * @param onFailure The callback to call if the profile creation fails.
   */
  override fun createProfile(
      fireUser: FirebaseUser?,
      onSuccess: (Profile) -> Unit,
      onFailure: (Exception) -> Unit
  ) {
    if (fireUser == null) {
      onFailure(Exception("User is null"))
      return
    }
    val profile =
        Profile(
            fireUser.uid,
            // This shouldn't null.
            fireUser.displayName ?: "Unnamed hiker",
            // This shouldn't null.
            fireUser.email ?: "unnamedhiker@example.com",
            HikingLevel.BEGINNER,
            Timestamp.now())
    profileExists(
        profile.id,
        onSuccess = { exists ->
          if (!exists)
              updateProfile(profile, onSuccess = { onSuccess(profile) }, onFailure = onFailure)
        },
        onFailure = { onFailure(Exception("Error getting document with id ${profile.id}")) })
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
    db.collection(collectionPath).document(id).get().addOnCompleteListener { task ->
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
      onSuccess: (Profile?) -> Unit,
      onFailure: (Exception) -> Unit
  ) {

    db.collection(collectionPath).document(id).get().addOnCompleteListener { task ->
      if (task.isSuccessful) {
        val profile = task.result?.let { documentToProfile(it) }
        onSuccess(profile)
      } else {
        task.exception?.let { e ->
          Log.e("ProfileRepositoryFirestore", "Error getting document", e)
          onFailure(e)
        }
      }
    }
  }

  override fun addProfile(profile: Profile, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
    profileExists(
        profile.id,
        onSuccess = { exists ->
          if (exists) {
            onFailure(Exception("Profile with id ${profile.id} already exists"))
          } else {
            updateProfile(profile, onSuccess, onFailure)
          }
        },
        onFailure = { onFailure(Exception("Error getting document with id ${profile.id}")) })
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
      val name = document.getString("name") ?: ""
      val email = document.getString("email") ?: ""
      val hikingLevelString = document.getString("hikingLevel") ?: HikingLevel.BEGINNER
      val hikingLevel =
          HikingLevel.values().find { it.name == hikingLevelString } ?: HikingLevel.BEGINNER
      val joinedDate = document.getTimestamp("joinedDate") ?: Timestamp.now()
      Profile(uid, name, email, hikingLevel, joinedDate)
    } catch (e: Exception) {
      Log.e("ProfileRepositoryFirestore", "Error converting document to Profile", e)
      null
    }
  }
}
