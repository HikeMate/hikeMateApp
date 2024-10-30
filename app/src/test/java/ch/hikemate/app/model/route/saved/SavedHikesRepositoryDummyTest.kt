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
  fun loadSavedHikesReturnInitialDummyData() =
      runTest(timeout = 5.seconds) {
        // When
        val savedHikes = savedHikesRepositoryDummy.loadSavedHikes()

        // Then
        assertEquals(6, savedHikes.size)
        assertEquals("1", savedHikes[0].id)
        assertEquals("Hike 1", savedHikes[0].name)
        assertNull(savedHikes[0].date)
        assertEquals("2", savedHikes[1].id)
        assertEquals("Hike 2", savedHikes[1].name)
        assertNull(savedHikes[1].date)
        assertEquals("3", savedHikes[2].id)
        assertEquals("Hike 3", savedHikes[2].name)
        assertNull(savedHikes[2].date)
        assertEquals("4", savedHikes[3].id)
        assertEquals("Hike 4", savedHikes[3].name)
        assertNotNull(savedHikes[3].date)
        assertEquals("5", savedHikes[4].id)
        assertEquals("Hike 5", savedHikes[4].name)
        assertNotNull(savedHikes[4].date)
        assertEquals("6", savedHikes[5].id)
        assertEquals("Hike 6", savedHikes[5].name)
        assertNotNull(savedHikes[5].date)
      }

  @Test
  fun addSavedHikeSavesTheHike() =
      runTest(timeout = 5.seconds) {
        // Given
        val hike = SavedHike("7", "Hike 7", null)

        // Before
        assertEquals(6, savedHikesRepositoryDummy.loadSavedHikes().size)
        assert(!savedHikesRepositoryDummy.loadSavedHikes().contains(hike))

        // When
        savedHikesRepositoryDummy.addSavedHike(hike)

        // Then
        assertEquals(7, savedHikesRepositoryDummy.loadSavedHikes().size)
        assert(savedHikesRepositoryDummy.loadSavedHikes().contains(hike))
      }

  @Test
  fun removeSavedHikeUnSavesTheHike() =
      runTest(timeout = 5.seconds) {
        // Given
        val hike = SavedHike("3", "Hike 3", null)

        // Before
        assertEquals(6, savedHikesRepositoryDummy.loadSavedHikes().size)
        assert(savedHikesRepositoryDummy.loadSavedHikes().contains(hike))

        // When
        savedHikesRepositoryDummy.removeSavedHike(hike)

        // Then
        assertEquals(5, savedHikesRepositoryDummy.loadSavedHikes().size)
        assert(!savedHikesRepositoryDummy.loadSavedHikes().contains(hike))
      }
}
