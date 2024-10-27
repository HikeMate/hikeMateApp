package ch.hikemate.app.model.route.saved

import ch.hikemate.app.utils.from
import com.google.firebase.Timestamp
import java.time.LocalDate

class SavedHikesRepositoryDummy : SavedHikesRepository {
  private val savedHikes =
      mutableListOf(
          SavedHike("1", "Hike 1", null),
          SavedHike("2", "Hike 2", null),
          SavedHike("3", "Hike 3", null),
          SavedHike(
              "4",
              "Hike 4",
              Timestamp.from(LocalDate.now().year, LocalDate.now().monthValue + 1, 1)),
          SavedHike(
              "5",
              "Hike 5",
              Timestamp.from(LocalDate.now().year, LocalDate.now().monthValue + 1, 2)),
          SavedHike(
              "6",
              "Hike 6",
              Timestamp.from(LocalDate.now().year, LocalDate.now().monthValue + 1, 3)))

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
