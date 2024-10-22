package ch.hikemate.app.model.route.saved

import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SavedHikesRepositoryFirebaseTest {
  private lateinit var savedHikesRepositoryFirebase: SavedHikesRepositoryFirebase

  @OptIn(ExperimentalCoroutinesApi::class)
  @Before
  fun setUp() {
    Dispatchers.setMain(UnconfinedTestDispatcher())

    savedHikesRepositoryFirebase = SavedHikesRepositoryFirebase()
  }

  @Test
  fun loadSavedHikesReturnsDummyData() =
      runTest(timeout = 5.seconds) {
        // When
        val savedHikes = savedHikesRepositoryFirebase.loadSavedHikes()

        // Then
        assertEquals(3, savedHikes.size)
        assertEquals("1", savedHikes[0].id)
        assertEquals("Hike 1", savedHikes[0].name)
        assertEquals("2", savedHikes[1].id)
        assertEquals("Hike 2", savedHikes[1].name)
        assertEquals("3", savedHikes[2].id)
        assertEquals("Hike 3", savedHikes[2].name)
      }

  @Test
  fun addSavedHikeDoesNothing() =
      runTest(timeout = 5.seconds) {
        // Add saved hike is a placeholder for now, it does nothing under the hood

        // When
        savedHikesRepositoryFirebase.addSavedHike(SavedHike("4", "Hike 4", null))

        // Then
        assert(true)
      }

  @Test
  fun removeSavedHikeDoesNothing() =
      runTest(timeout = 5.seconds) {
        // Remove saved hike is a placeholder for now, it does nothing under the hood

        // When
        savedHikesRepositoryFirebase.removeSavedHike(SavedHike("4", "Hike 4", null))

        // Then
        assert(true)
      }
}
