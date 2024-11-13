package ch.hikemate.app.model.route.saved

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue.arrayRemove
import com.google.firebase.firestore.FieldValue.arrayUnion
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class SavedHikesRepositoryFirestore(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) : SavedHikesRepository {

  data class UserSavedHikes(val savedHikes: List<SavedHike>)

  override suspend fun loadSavedHikes(): List<SavedHike> {
    checkNotNull(auth.currentUser) { ERROR_MSG_USER_NOT_AUTHENTICATED }
    return db.collection(SAVED_HIKES_COLLECTION)
        .document(auth.currentUser!!.uid) // Only get the saved hikes of the current user
        .get()
        .await()
        .toObject(UserSavedHikes::class.java)
        ?.savedHikes ?: emptyList()
  }

  override suspend fun addSavedHike(hike: SavedHike) {
    checkNotNull(auth.currentUser) { ERROR_MSG_USER_NOT_AUTHENTICATED }

    val documentReference = db.collection(SAVED_HIKES_COLLECTION).document(auth.currentUser!!.uid)

    // Check that the document exists before updating it
    val documentExists = documentReference.get().await().exists()

    if (!documentExists) {
      documentReference.set(UserSavedHikes(listOf(hike)))
    } else {
      documentReference.update(UserSavedHikes::savedHikes.name, arrayUnion(hike)).await()
    }
  }

  override suspend fun removeSavedHike(hike: SavedHike) {
    checkNotNull(auth.currentUser) { ERROR_MSG_USER_NOT_AUTHENTICATED }
    db.collection(SAVED_HIKES_COLLECTION)
        .document(auth.currentUser!!.uid)
        .update(UserSavedHikes::savedHikes.name, arrayRemove(hike))
        .await()
  }

  override suspend fun getSavedHike(id: String): SavedHike? {
    checkNotNull(auth.currentUser) { ERROR_MSG_USER_NOT_AUTHENTICATED }
    return loadSavedHikes().find { it.id == id }
  }

  override suspend fun isHikeSaved(id: String): Boolean {
    checkNotNull(auth.currentUser) { ERROR_MSG_USER_NOT_AUTHENTICATED }
    return loadSavedHikes().any { it.id == id }
  }

  companion object {
    const val SAVED_HIKES_COLLECTION = "savedHikes"
    private const val ERROR_MSG_USER_NOT_AUTHENTICATED = "User is not authenticated"
  }
}
