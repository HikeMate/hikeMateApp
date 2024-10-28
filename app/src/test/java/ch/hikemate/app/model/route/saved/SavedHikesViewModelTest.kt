package ch.hikemate.app.model.route.saved

import ch.hikemate.app.R
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

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
  fun canBeCreatedAsFactory() {
    val factory = SavedHikesViewModel.Factory
    val viewModel = factory.create(SavedHikesViewModel::class.java)
    assertNotNull(viewModel)
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
}
