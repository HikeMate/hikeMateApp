package ch.hikemate.app.model.route.saved

class SavedHikesRepositoryDummy : SavedHikesRepository {
  private val savedHikes =
      mutableListOf(
          SavedHike("1", "Hike 1", null),
          SavedHike("2", "Hike 2", null),
          SavedHike("3", "Hike 3", null))

  override suspend fun loadSavedHikes(): List<SavedHike> {
    return savedHikes
  }

  override suspend fun addSavedHike(hike: SavedHike) {
    if (!savedHikes.contains(hike)) {
      savedHikes.add(hike)
    }
  }

  override suspend fun removeSavedHike(hike: SavedHike) {
    savedHikes.remove(hike)
  }
}
