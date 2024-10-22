package ch.hikemate.app.model.route.saved

import com.google.firebase.Timestamp

class SavedHikesRepositoryFirebase : SavedHikesRepository {
  override suspend fun loadSavedHikes(): List<SavedHike> {
    return listOf(
        SavedHike("1", "Hike 1", Timestamp.now()),
        SavedHike("2", "Hike 2", Timestamp.now()),
        SavedHike("3", "Hike 3", Timestamp.now()))
  }

  override suspend fun addSavedHike(hike: SavedHike) {
    // A first implementation of the repository without any interaction with Firebase
    // is implemented to allow the UI to be developed in parallel with the repository.
    // There is nothing to do here except interacting with Firebase, hence this function
    // is empty for now.
  }

  override suspend fun removeSavedHike(hike: SavedHike) {
    // A first implementation of the repository without any interaction with Firebase
    // is implemented to allow the UI to be developed in parallel with the repository.
    // There is nothing to do here except interacting with Firebase, hence this function
    // is empty for now.
  }
}
