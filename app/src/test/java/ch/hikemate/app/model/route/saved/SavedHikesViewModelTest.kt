package ch.hikemate.app.model.route.saved

import ch.hikemate.app.R
import ch.hikemate.app.model.route.Bounds
import ch.hikemate.app.model.route.HikeRoute
import com.google.firebase.Timestamp
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class SavedHikesViewModelTest {
  private lateinit var savedHikesRepository: SavedHikesRepository
  private lateinit var savedHikesViewModel: SavedHikesViewModel

  @Before
  fun setUp() {
    // Testing coroutines is easier if everything is set to run on a single thread
    Dispatchers.setMain(UnconfinedTestDispatcher())

    savedHikesRepository = mock(SavedHikesRepository::class.java)
    savedHikesViewModel = SavedHikesViewModel(savedHikesRepository, UnconfinedTestDispatcher())
  }

  @Test
  fun loadSavedHikesSuccessUpdatesSavedHikes() =
      runTest(timeout = 5.seconds) {
        // Given
        val savedHikes = listOf(SavedHike("1", "Hike One", null), SavedHike("2", "Hike Two", null))
        `when`(savedHikesRepository.loadSavedHikes()).thenReturn(savedHikes)

        // When
        savedHikesViewModel.loadSavedHikes()

        // Then
        assertEquals(savedHikes, savedHikesViewModel.savedHike.value)
      }

  @Test
  fun loadSavedHikesFailureUpdatesErrorMessage() =
      runTest(timeout = 5.seconds) {
        // Given
        `when`(savedHikesRepository.loadSavedHikes()).thenThrow(RuntimeException("Load error"))

        // When
        savedHikesViewModel.loadSavedHikes()

        // Then
        assertEquals(
            R.string.saved_hikes_screen_generic_error, savedHikesViewModel.errorMessage.value)
      }

  @Test
  fun addSavedHikeSuccessCallsRepository() =
      runTest(timeout = 5.seconds) {
        // Given
        val savedHike = SavedHike("1", "Hike One", null)
        `when`(savedHikesRepository.loadSavedHikes()).thenReturn(emptyList())

        // When
        savedHikesViewModel.addSavedHike(savedHike)

        // Then
        verify(savedHikesRepository).addSavedHike(savedHike)
        verify(savedHikesRepository).loadSavedHikes()
      }

  @Test
  fun addSavedHikeSuccessUpdatesSavedHikes() =
      runTest(timeout = 5.seconds) {
        // Given
        val savedHike = SavedHike("1", "Hike One", null)
        `when`(savedHikesRepository.loadSavedHikes()).thenReturn(listOf(savedHike))

        // When
        savedHikesViewModel.addSavedHike(savedHike)

        // Then
        assertEquals(listOf(savedHike), savedHikesViewModel.savedHike.value)
      }

  @Test
  fun addSavedHikeFailureUpdatesErrorMessage() =
      runTest(timeout = 5.seconds) {
        // Given
        val savedHike = SavedHike("1", "Hike One", null)
        `when`(savedHikesRepository.addSavedHike(savedHike))
            .thenThrow(RuntimeException("Add error"))

        // When
        savedHikesViewModel.addSavedHike(savedHike)

        // Then
        assertEquals(
            R.string.saved_hikes_screen_generic_error, savedHikesViewModel.errorMessage.value)
      }

  @Test
  fun removeSavedHikeSuccessCallsRepository() =
      runTest(timeout = 5.seconds) {
        // Given
        val savedHike = SavedHike("1", "Hike One", null)
        `when`(savedHikesRepository.loadSavedHikes()).thenReturn(emptyList())

        // When
        savedHikesViewModel.removeSavedHike(savedHike)

        // Then
        verify(savedHikesRepository).removeSavedHike(savedHike)
        verify(savedHikesRepository).loadSavedHikes()
      }

  @Test
  fun removeSavedHikeSuccessUpdatesSavedHikes() =
      runTest(timeout = 5.seconds) {
        // Given
        val savedHike = SavedHike("1", "Hike One", null)
        `when`(savedHikesRepository.loadSavedHikes()).thenReturn(emptyList())

        // When
        savedHikesViewModel.removeSavedHike(savedHike)

        // Then
        assertEquals(emptyList<SavedHike>(), savedHikesViewModel.savedHike.value)
      }

  @Test
  fun removeSavedHikeFailureUpdatesErrorMessage() =
      runTest(timeout = 5.seconds) {
        // Given
        val savedHike = SavedHike("1", "Hike One", null)
        `when`(savedHikesRepository.removeSavedHike(savedHike))
            .thenThrow(RuntimeException("Remove error"))

        // When
        savedHikesViewModel.removeSavedHike(savedHike)

        // Then
        assertEquals(
            R.string.saved_hikes_screen_generic_error, savedHikesViewModel.errorMessage.value)
      }

  @Test
  fun updateHikeDetailStateSetsCorrectState() = runTest {
    // Given
    val hikeRoute = HikeRoute("1", Bounds(0.0, 0.0, 0.0, 0.0), emptyList(), "Test Hike")
    `when`(savedHikesRepository.isHikeSaved(hikeRoute.id)).thenReturn(null)

    // When
    savedHikesViewModel.updateHikeDetailState(hikeRoute)

    // Then
    val expectedState =
        SavedHikesViewModel.HikeDetailState(
            hike = hikeRoute,
            isSaved = false,
            bookmark = R.drawable.bookmark_no_fill,
            plannedDate = null)
    assertEquals(expectedState, savedHikesViewModel.hikeDetailState.first())
  }

  @Test
  fun toggleSaveStateAddsOrRemovesHike() = runTest {
    // Given
    val hikeRoute = HikeRoute("1", Bounds(0.0, 0.0, 0.0, 0.0), emptyList(), "Test Hike")

    savedHikesViewModel.updateHikeDetailState(hikeRoute)

    // When: toggle to save hike
    `when`(savedHikesRepository.loadSavedHikes()).thenReturn(emptyList())
    savedHikesViewModel.toggleSaveState()

    // Then: verify addSavedHike is called and state is updated
    val captor = argumentCaptor<SavedHike>()
    verify(savedHikesRepository).addSavedHike(captor.capture())
    assertEquals(true, savedHikesViewModel.hikeDetailState.first()?.isSaved)
  }

  @Test
  fun updatePlannedDateSetsCorrectDate() = runTest {
    // Given
    val hikeRoute = HikeRoute("1", Bounds(0.0, 0.0, 0.0, 0.0), emptyList(), "Test Hike")
    val newPlannedDate = Timestamp.now()

    // Mocking initial repository state
    whenever(savedHikesRepository.loadSavedHikes())
        .thenReturn(listOf(SavedHike("1", "Test Hike", null)))

    // Setting up initial state in view model
    savedHikesViewModel.updateHikeDetailState(hikeRoute)
    savedHikesViewModel.toggleSaveState()

    // When
    savedHikesViewModel.updatePlannedDate(newPlannedDate)

    // Verifying removal of the old hike entry
    verify(savedHikesRepository).removeSavedHike(SavedHike("1", "Test Hike", null))

    // Then: check that plannedDate is updated in the state
    assertEquals(newPlannedDate, savedHikesViewModel.hikeDetailState.first()?.plannedDate)

    // Verifying addition of the new hike entry with the updated planned date
    verify(savedHikesRepository)
        .addSavedHike(SavedHike(hikeRoute.id, hikeRoute.name ?: "", newPlannedDate))
  }
}
