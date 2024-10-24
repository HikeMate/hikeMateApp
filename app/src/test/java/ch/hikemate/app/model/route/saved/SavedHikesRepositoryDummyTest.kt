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

class SavedHikesRepositoryDummyTest {
  private lateinit var savedHikesRepositoryDummy: SavedHikesRepositoryDummy

  @OptIn(ExperimentalCoroutinesApi::class)
  @Before
  fun setUp() {
    Dispatchers.setMain(UnconfinedTestDispatcher())

    savedHikesRepositoryDummy = SavedHikesRepositoryDummy()
  }

  @Test
  fun loadSavedHikesReturnsDummyData() =
      runTest(timeout = 5.seconds) {
        // When
        val savedHikes = savedHikesRepositoryDummy.loadSavedHikes()

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
        // Given
        val hike = SavedHike("4", "Hike 4", null)

        // Before
        assertEquals(3, savedHikesRepositoryDummy.loadSavedHikes().size)
        assert(!savedHikesRepositoryDummy.loadSavedHikes().contains(hike))

        // When
        savedHikesRepositoryDummy.addSavedHike(hike)

        // Then
        assertEquals(4, savedHikesRepositoryDummy.loadSavedHikes().size)
        assert(savedHikesRepositoryDummy.loadSavedHikes().contains(hike))
      }

  @Test
  fun removeSavedHikeDoesNothing() =
      runTest(timeout = 5.seconds) {
        // Given
        val hike = SavedHike("3", "Hike 3", null)

        // Before
        assertEquals(3, savedHikesRepositoryDummy.loadSavedHikes().size)
        assert(savedHikesRepositoryDummy.loadSavedHikes().contains(hike))

        // When
        savedHikesRepositoryDummy.removeSavedHike(hike)

        // Then
        assertEquals(2, savedHikesRepositoryDummy.loadSavedHikes().size)
        assert(!savedHikesRepositoryDummy.loadSavedHikes().contains(hike))
      }
}
