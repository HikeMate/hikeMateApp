package ch.hikemate.app.model.route

import ch.hikemate.app.model.elevation.ElevationService
import ch.hikemate.app.model.route.saved.SavedHike
import ch.hikemate.app.model.route.saved.SavedHikesRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HikesViewModelTest {
  private lateinit var savedHikesRepo: SavedHikesRepository
  private lateinit var osmHikesRepo: HikeRoutesRepository
  private lateinit var elevationRepo: ElevationService

  private lateinit var hikesViewModel: HikesViewModel

  private val dispatcher = UnconfinedTestDispatcher()

  @Before
  fun setUp() {
    // Testing coroutines is easier if everything is set to run on a single thread
    Dispatchers.setMain(dispatcher)

    savedHikesRepo = mockk<SavedHikesRepository>()
    osmHikesRepo = mockk<HikeRoutesRepository>()
    elevationRepo = mockk<ElevationService>()

    hikesViewModel = HikesViewModel(savedHikesRepo, osmHikesRepo, elevationRepo, dispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  // ==========================================================================
  // UTILS FOR TESTING
  // ==========================================================================

  private val singleSavedHike1: List<SavedHike> = listOf(SavedHike(id = "saved1", name = "Saved Hike 1", date = null))

  private fun loadSavedHikes(savedHikes: List<SavedHike>, onSuccess: () -> Unit = {}, onFailure: () -> Unit = {})  {
    coEvery { savedHikesRepo.loadSavedHikes() } returns savedHikes
    hikesViewModel.loadSavedHikes(onSuccess, onFailure)
  }

  // ==========================================================================
  // HikesViewModel.selectHike
  // ==========================================================================

  @Test
  fun selectHikeFailsIfHikeNotFound() = runTest(dispatcher) {
    // No hike is selected initially
    assertNull(hikesViewModel.selectedHike.value)
    val hikeId = "nonexistent"

    // Select a non-existent hike
    var onFailureCalled = false
    hikesViewModel.selectHike(
      hikeId = hikeId,
      onSuccess = { fail("onSuccess should not have been called") },
      onFailure = { onFailureCalled = true }
    )

    // The selected hike should still be null
    assertNull(hikesViewModel.selectedHike.value)
    // The appropriate callback should be called
    assertTrue(onFailureCalled)
  }

  @Test
  fun selectHikeSucceedsIfHikeFound() = runTest(dispatcher) {
    // There needs to be at least one hike in the loaded list to select it
    loadSavedHikes(singleSavedHike1)
    // No hike is selected initially
    assertNull(hikesViewModel.selectedHike.value)
    val hikeId = singleSavedHike1[0].id

    // Select the hike
    var onSuccessCalled = false
    hikesViewModel.selectHike(
      hikeId = hikeId,
      onSuccess = { onSuccessCalled = true },
      onFailure = { fail("onFailure should not have been called") }
    )

    // The selected hike should now be the one that was selected
    assertNotNull(hikesViewModel.selectedHike.value)
    assertEquals(hikeId, hikesViewModel.selectedHike.value?.id)
    // The appropriate callback should be called
    assertTrue(onSuccessCalled)
  }

  // ==========================================================================
  // HikesViewModel.selectHike
  // ==========================================================================

  @Test
  fun unselectHikeSucceedsWhenHikeIsSelected() = runTest(dispatcher) {
    // There needs to be at least one hike in the loaded list to select it
    loadSavedHikes(singleSavedHike1)
    // Select a hike
    val hikeId = singleSavedHike1[0].id
    hikesViewModel.selectHike(hikeId)
    // Check that the hike is selected, otherwise the test does not make sense
    assertNotNull(hikesViewModel.selectedHike.value)

    // Unselect the hike
    hikesViewModel.unselectHike()

    // Check that the hike was indeed unselected
    assertNull(hikesViewModel.selectedHike.value)
  }

  @Test
  fun unselectHikeSucceedsWhenNoHikeIsSelected() = runTest(dispatcher) {
    // No hike should be selected at the start of the test
    assertNull(hikesViewModel.selectedHike.value)

    // Unselect the hike
    hikesViewModel.unselectHike()

    // Check that nothing has changed and that no hike is selected
    assertNull(hikesViewModel.selectedHike.value)
  }
}