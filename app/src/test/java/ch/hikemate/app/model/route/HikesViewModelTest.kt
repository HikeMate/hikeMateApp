package ch.hikemate.app.model.route

import ch.hikemate.app.model.elevation.ElevationService
import ch.hikemate.app.model.route.saved.SavedHike
import ch.hikemate.app.model.route.saved.SavedHikesRepository
import io.mockk.coEvery
import io.mockk.coVerify
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

  private val singleSavedHike: List<SavedHike> = listOf(SavedHike(id = "saved", name = "Saved Hike", date = null))

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
    loadSavedHikes(singleSavedHike)
    // No hike is selected initially
    assertNull(hikesViewModel.selectedHike.value)
    val hikeId = singleSavedHike[0].id

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
}