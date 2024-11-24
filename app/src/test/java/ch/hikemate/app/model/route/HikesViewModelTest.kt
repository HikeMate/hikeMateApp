package ch.hikemate.app.model.route

import ch.hikemate.app.model.elevation.ElevationService
import ch.hikemate.app.model.route.saved.SavedHike
import ch.hikemate.app.model.route.saved.SavedHikesRepository
import ch.hikemate.app.utils.from
import com.google.firebase.Timestamp
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.osmdroid.util.BoundingBox

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

  private val firstJanuary2024 = Timestamp.from(2024, 1, 1)

  private val singleSavedHike1: List<SavedHike> = listOf(SavedHike(id = "saved1", name = "Saved Hike 1", date = null))

  private val singleSavedHike2: List<SavedHike> = listOf(SavedHike(id = "saved2", name = "Saved Hike 2", date = firstJanuary2024))

  private fun loadSavedHikes(savedHikes: List<SavedHike>, onSuccess: () -> Unit = {}, onFailure: () -> Unit = {})  {
    coEvery { savedHikesRepo.loadSavedHikes() } returns savedHikes
    hikesViewModel.loadSavedHikes(onSuccess, onFailure)
  }

  private val singleOsmHike1: List<HikeRoute> = listOf(HikeRoute(id = "osm1", name = "OSM Hike 1", bounds = Bounds(0.0, 0.0, 0.0, 0.0), ways = emptyList()))

  private val singleOsmHike2: List<HikeRoute> = listOf(HikeRoute(id = "osm2", name = "OSM Hike 2", bounds = Bounds(0.0, 0.0, 0.0, 0.0), ways = emptyList()))

  private val doubleOsmHikes1: List<HikeRoute> = listOf(
    singleOsmHike1[0],
    singleOsmHike2[0]
  )

  private fun loadOsmHikes(osmHikes: List<HikeRoute>, onSuccess: () -> Unit = {}, onFailure: () -> Unit = {}) {
    every { osmHikesRepo.getRoutes(any(), any(), any()) } answers {
      val successCallback = secondArg<(List<HikeRoute>) -> Unit>()
      successCallback(osmHikes)
    }
    hikesViewModel.loadHikesInBounds(BoundingBox(0.0, 0.0, 0.0, 0.0), onSuccess, onFailure)
  }

  private val elevationProfile1: List<Double> = listOf(0.0, 1.0, 2.0, 3.0, 2.0, 1.0, 0.0)

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

  // ==========================================================================
  // HikesViewModel.refreshSavedHikesCache
  // ==========================================================================

  @Test
  fun refreshSavedHikesCacheFailsIfRepoFails() = runTest(dispatcher) {
    // Whenever asked for saved hikes, the repository will throw an exception
    coEvery { savedHikesRepo.loadSavedHikes() } throws Exception("Failed to load saved hikes")

    // Try refreshing the saved hikes cache through the view model
    var onFailureCalled = false
    hikesViewModel.refreshSavedHikesCache(
      onSuccess = { fail("onSuccess should not have been called") },
      onFailure = { onFailureCalled = true }
    )

    // The saved hikes repository should be called exactly once
    coVerify(exactly = 1) { savedHikesRepo.loadSavedHikes() }
    // The failure callback should be called
    assertTrue(onFailureCalled)
  }

  /**
   * This is a specific test case. If the saved hikes are loaded (for example, we are on the saved
   * hikes screen), and [HikesViewModel.refreshSavedHikesCache] is called for some reason, the
   * function will update [HikesViewModel.hikeFlows] with the new saved hikes.
   */
  @Test
  fun refreshSavedHikesCacheSucceedsWhileSavedHikesAreLoaded() = runTest(dispatcher) {
    // Load a first version of the saved hikes (to see later that it is replaced by the new one)
    loadSavedHikes(singleSavedHike1)
    // Check that the loading happened correctly
    assertEquals(singleSavedHike1.size, hikesViewModel.hikeFlows.value.size)

    // For the second loading, provide a different list of saved hikes
    coEvery { savedHikesRepo.loadSavedHikes() } returns singleSavedHike2

    // Refresh the saved hikes cache through the view model
    var onSuccessCalled = false
    hikesViewModel.refreshSavedHikesCache(
      onSuccess = { onSuccessCalled = true },
      onFailure = { fail("onFailure should not have been called") }
    )

    // Check that the view model uses the repository
    coVerify(exactly = 2) { savedHikesRepo.loadSavedHikes() }
    // Check that the view model correctly calls the success callback
    assertTrue(onSuccessCalled)
    // Check that the view model now contains the new saved hikes list
    assertEquals(singleSavedHike2.size, hikesViewModel.hikeFlows.value.size)
    assertEquals(singleSavedHike2[0].id, hikesViewModel.hikeFlows.value[0].value.id)
  }

  /**
   * This is a specific test case. If the saved hikes are in the cache, but not the primary loaded
   * hikes (for example the user is on the map screen and loaded a bunch of hikes from OSM), then
   * the [HikesViewModel.refreshSavedHikesCache] function should update the saved hikes cache with
   * the new saved hikes, update the loaded hikes with their new saved status, but not replace the
   * loaded hikes with the saved hikes.
   */
  @Test
  fun refreshSavedHikesCacheSucceedsWhileSavedHikesAreNotLoaded() = runTest(dispatcher) {
    // Load a first version of the OSM hikes (to see later that it is not replaced by the saved hikes)
    loadOsmHikes(singleOsmHike1)
    // Check that the loading happened correctly
    assertEquals(singleOsmHike1.size, hikesViewModel.hikeFlows.value.size)
    // Check that for now, the loaded OSM hike is not marked as saved
    assertFalse(hikesViewModel.hikeFlows.value[0].value.isSaved)

    // Include the loaded OSM hike in the saved hikes list, and add another unrelated one
    // This is to make sure that the view model correctly updates the loaded hikes
    val osmHike = singleOsmHike1[0]
    coEvery { savedHikesRepo.loadSavedHikes() } returns listOf(
      singleSavedHike1[0],
      SavedHike(id = osmHike.id, name = osmHike.name ?: "", date = firstJanuary2024)
    )

    // Refresh the saved hikes cache through the view model
    var onSuccessCalled = false
    hikesViewModel.refreshSavedHikesCache(
      onSuccess = { onSuccessCalled = true },
      onFailure = { fail("onFailure should not have been called") }
    )

    // Check that the view model uses the repository
    coVerify(exactly = 1) { savedHikesRepo.loadSavedHikes() }
    // Check that the view model correctly calls the success callback
    assertTrue(onSuccessCalled)
    // Check that the loaded hikes are not replaced with the saved hikes
    assertEquals(singleOsmHike1.size, hikesViewModel.hikeFlows.value.size)
    // Check that the loaded hike is now marked as saved
    assertTrue(hikesViewModel.hikeFlows.value[0].value.isSaved)
  }

  /**
   * This is an even more specific case than the two previous tests. Suppose one loads saved hikes
   * (for example because we are on the saved hikes screen), then loads hikes from bounds (because
   * we switched to the map).
   *
   * In that situation, refreshing the saved hikes should not override the hikes loaded from the
   * bounds. This is the specific test that is being tested here.
   *
   * Kind of a regression test, because I forgot the line that made it work in my first
   * implementation.
   */
  @Test
  fun refreshSavedHikesCacheSucceedsWhenSavedHikesWereLoadedButNotAnymore() = runTest(dispatcher) {
    // Load a first version of the saved hikes (to mark the saved hikes as currently loaded)
    loadSavedHikes(singleSavedHike1)
    // Check that the loading happened correctly
    assertEquals(singleSavedHike1.size, hikesViewModel.hikeFlows.value.size)
    assertEquals(singleSavedHike1[0].id, hikesViewModel.hikeFlows.value[0].value.id)

    // Then load OSM hikes to make sure that the saved hikes are not loaded anymore
    loadOsmHikes(singleOsmHike1)
    // Check that the loading happened correctly
    assertEquals(singleOsmHike1.size, hikesViewModel.hikeFlows.value.size)
    assertEquals(singleOsmHike1[0].id, hikesViewModel.hikeFlows.value[0].value.id)
    // Check that the loaded hike is not marked as saved
    assertFalse(hikesViewModel.hikeFlows.value[0].value.isSaved)

    // Include the loaded OSM hike in the saved hikes list, and add another unrelated one
    // This is to make sure that the view model correctly updates the loaded hikes
    val osmHike = singleOsmHike1[0]
    coEvery { savedHikesRepo.loadSavedHikes() } returns listOf(
      singleSavedHike1[0],
      SavedHike(id = osmHike.id, name = osmHike.name ?: "", date = firstJanuary2024)
    )

    // Refresh the saved hikes cache through the view model
    var onSuccessCalled = false
    hikesViewModel.refreshSavedHikesCache(
      onSuccess = { onSuccessCalled = true },
      onFailure = { fail("onFailure should not have been called") }
    )

    // Check that the view model uses the repository
    coVerify(exactly = 2) { savedHikesRepo.loadSavedHikes() }
    // Check that the view model correctly calls the success callback
    assertTrue(onSuccessCalled)
    // Check that the loaded hikes are not replaced with the saved hikes
    assertEquals(singleOsmHike1.size, hikesViewModel.hikeFlows.value.size)
    assertEquals(singleOsmHike1[0].id, hikesViewModel.hikeFlows.value[0].value.id)
    // Check that the loaded hike is now marked as saved
    assertTrue(hikesViewModel.hikeFlows.value[0].value.isSaved)
  }

  @Test
  fun `refreshSavedHikesCache sets loading to true then false`() = runTest(dispatcher) {
    // Listen to the changes made to loading during the call
    val emissions = mutableListOf<Boolean>()
    val job = backgroundScope.launch {
      hikesViewModel.loading.collect { emissions.add(it) }
    }

    // Set the repository to throw an exception because we do not care
    coEvery { savedHikesRepo.loadSavedHikes() } throws Exception("Failed to load saved hikes")

    // Refresh the saved hikes cache
    hikesViewModel.refreshSavedHikesCache()

    // Because we are on an UnconfinedTestDispatcher(), the coroutine should be done by now, hence
    // we can stop listening to the values emitted by loading.
    job.cancel()

    // Check that loading was false at first, then true during the call, and false again at the end
    assertEquals(listOf(false, true, false), emissions)
  }

  @Test
  fun `refreshSavedHikesCache updates selected hike if its status changes`() = runTest(dispatcher) {
    // Load some hikes to be selected
    loadOsmHikes(singleOsmHike1)

    // Select the hike to be updated
    val hikeId = singleOsmHike1[0].id
    hikesViewModel.selectHike(hikeId)
    // Check that the hike is selected
    assertNotNull(hikesViewModel.selectedHike.value)
    assertEquals(hikeId, hikesViewModel.selectedHike.value?.id)

    // Include the selected hike in the saved hikes list
    coEvery { savedHikesRepo.loadSavedHikes() } returns listOf(
      SavedHike(id = hikeId, name = singleOsmHike1[0].name ?: "", date = firstJanuary2024)
    )

    // Refresh the saved hikes cache
    hikesViewModel.refreshSavedHikesCache()

    // Check that the selected hike is now marked as saved
    assertTrue(hikesViewModel.selectedHike.value?.isSaved ?: false)
    assertEquals(firstJanuary2024, hikesViewModel.selectedHike.value?.plannedDate)
  }

  @Test
  fun `refreshSavedHikesCache clears selected hike if it is unloaded`() = runTest(dispatcher) {
    // Load some hikes to be selected
    loadSavedHikes(singleSavedHike1)

    // Select the hike to be updated
    val hikeId = singleSavedHike1[0].id
    hikesViewModel.selectHike(hikeId)
    // Check that the hike is selected
    assertNotNull(hikesViewModel.selectedHike.value)
    assertEquals(hikeId, hikesViewModel.selectedHike.value?.id)

    // Remove the selected hike from the saved hikes list
    coEvery { savedHikesRepo.loadSavedHikes() } returns emptyList()

    // Refresh the saved hikes cache
    hikesViewModel.refreshSavedHikesCache()

    // Check that the selected hike is now unselected
    assertNull(hikesViewModel.selectedHike.value)
  }

  // ==========================================================================
  // HikesViewModel.loadSavedHikes
  // ==========================================================================

  @Test
  fun loadSavedHikesFailsIfRepoFails() = runTest(dispatcher) {
    // Whenever asked for saved hikes, the repository will throw an exception
    coEvery { savedHikesRepo.loadSavedHikes() } throws Exception("Failed to load saved hikes")

    // Try loading saved hikes through the view model
    var onFailureCalled = false
    hikesViewModel.loadSavedHikes(
      onSuccess = { fail("onSuccess should not have been called") },
      onFailure = { onFailureCalled = true }
    )

    // The saved hikes repository should be called exactly once
    coVerify(exactly = 1) { savedHikesRepo.loadSavedHikes() }
    // The failure callback should be called
    assertTrue(onFailureCalled)
  }

  @Test
  fun loadSavedHikesSucceedsIfRepoSucceeds() = runTest(dispatcher) {
    // Check nothing is in the hikes list before further operations
    assertEquals(0, hikesViewModel.hikeFlows.value.size)

    // Make sure the saved hikes repository provides a hike to be loaded
    coEvery { savedHikesRepo.loadSavedHikes() } returns singleSavedHike1

    // Load the saved hikes through the view model
    var onSuccessCalled = false
    hikesViewModel.loadSavedHikes(
      onSuccess = { onSuccessCalled = true },
      onFailure = { fail("onFailure should not have been called") }
    )

    // Check that the view model uses the repository
    coVerify(exactly = 1) { savedHikesRepo.loadSavedHikes() }
    // Check that the view model correctly calls the success callback
    assertTrue(onSuccessCalled)
    // Check that the view model now contains the loaded hikes list
    assertEquals(singleSavedHike1.size, hikesViewModel.hikeFlows.value.size)
  }

  @Test
  fun `loadSavedHikes sets loading to true then false`() = runTest(dispatcher) {
    // Listen to the changes made to loading during the call
    val emissions = mutableListOf<Boolean>()
    val job = backgroundScope.launch {
      hikesViewModel.loading.collect { emissions.add(it) }
    }

    // Set the repository to throw an exception because we do not care
    coEvery { savedHikesRepo.loadSavedHikes() } throws Exception("Failed to load saved hikes")

    // Load the saved hikes
    hikesViewModel.loadSavedHikes()

    // Because we are on an UnconfinedTestDispatcher(), the coroutine should be done by now, hence
    // we can stop listening to the values emitted by loading.
    job.cancel()

    // Check that loading was false at first, then true during the call, and false again at the end
    assertEquals(listOf(false, true, false), emissions)
  }

  @Test
  fun `loadSavedHikes updates selected hike if its status changes`() = runTest(dispatcher) {
    // Load some hikes to be selected
    loadOsmHikes(singleOsmHike1)

    // Select the hike to be updated
    val hikeId = singleOsmHike1[0].id
    hikesViewModel.selectHike(hikeId)
    // Check that the hike is selected
    assertNotNull(hikesViewModel.selectedHike.value)
    assertEquals(hikeId, hikesViewModel.selectedHike.value?.id)
    // Check that the hike is not marked as saved yet
    assertFalse(hikesViewModel.selectedHike.value?.isSaved ?: true)

    // Include the selected hike in the saved hikes list
    coEvery { savedHikesRepo.loadSavedHikes() } returns listOf(
      SavedHike(id = hikeId, name = singleOsmHike1[0].name ?: "", date = firstJanuary2024)
    )

    // Load the saved hikes
    hikesViewModel.loadSavedHikes()

    // Check that the selected hike is now marked as saved
    assertTrue(hikesViewModel.selectedHike.value?.isSaved ?: false)
    assertEquals(firstJanuary2024, hikesViewModel.selectedHike.value?.plannedDate)
  }

  @Test
  fun `loadSavedHikes clears selected hike if it is unloaded`() = runTest(dispatcher) {
    // Load some hikes to be selected
    loadSavedHikes(singleSavedHike1)

    // Select the hike to be updated
    val hikeId = singleSavedHike1[0].id
    hikesViewModel.selectHike(hikeId)
    // Check that the hike is selected
    assertNotNull(hikesViewModel.selectedHike.value)
    assertEquals(hikeId, hikesViewModel.selectedHike.value?.id)

    // Remove the selected hike from the saved hikes list
    coEvery { savedHikesRepo.loadSavedHikes() } returns emptyList()

    // Load the saved hikes
    hikesViewModel.loadSavedHikes()

    // Check that the selected hike is now unselected
    assertNull(hikesViewModel.selectedHike.value)
  }

  // ==========================================================================
  // HikesViewModel.saveHike
  // ==========================================================================

  @Test
  fun saveHikeFailsIfNoCorrespondingHikeIsFound() = runTest(dispatcher) {
    // Try to save a hike that is not loaded
    var onFailureCalled = false
    hikesViewModel.saveHike(
      hikeId = "nonexistent",
      onSuccess = { fail("onSuccess should not have been called") },
      onFailure = { onFailureCalled = true }
    )

    // The saved hikes repository should not be called
    coVerify(exactly = 0) { savedHikesRepo.addSavedHike(any()) }
    // The appropriate callback should be called
    assertTrue(onFailureCalled)
  }

  @Test
  fun saveHikeFailsIfRepoFails() = runTest(dispatcher) {
    // Load a hike to be saved
    loadOsmHikes(singleOsmHike1)
    // Check that the hike was loaded
    assertEquals(1, hikesViewModel.hikeFlows.value.size)

    // Whenever asked to save a hike, the repository will throw an exception
    coEvery { savedHikesRepo.addSavedHike(any()) } throws Exception("Failed to save hike")

    // Try to save the loaded hike
    var onFailureCalled = false
    hikesViewModel.saveHike(
      hikeId = singleOsmHike1[0].id,
      onSuccess = { fail("onSuccess should not have been called") },
      onFailure = { onFailureCalled = true }
    )

    // The saved hikes repository should be called exactly once
    coVerify(exactly = 1) { savedHikesRepo.addSavedHike(any()) }
    // The appropriate callback should be called
    assertTrue(onFailureCalled)
  }

  @Test
  fun saveHikeSucceedsIfRepoSucceeds() = runTest(dispatcher) {
    // Load a hike to be saved
    loadOsmHikes(singleOsmHike1)
    // Check that the hike was loaded
    assertEquals(1, hikesViewModel.hikeFlows.value.size)
    // Check that the hike is not saved initially
    assertFalse(hikesViewModel.hikeFlows.value[0].value.isSaved)

    // Make sure the saved hikes repository saves the hike
    coEvery { savedHikesRepo.addSavedHike(any()) } returns Unit

    // Try to save the loaded hike
    var onSuccessCalled = false
    hikesViewModel.saveHike(
      hikeId = singleOsmHike1[0].id,
      onSuccess = { onSuccessCalled = true },
      onFailure = { fail("onFailure should not have been called") }
    )

    // The saved hikes repository should be called exactly once
    coVerify(exactly = 1) { savedHikesRepo.addSavedHike(any()) }
    // The appropriate callback should be called
    assertTrue(onSuccessCalled)
    // The hike should now be marked as saved
    assertTrue(hikesViewModel.hikeFlows.value[0].value.isSaved)
  }

  @Test
  fun `saveHike updates selected hike if its status changes`() = runTest(dispatcher) {
    // Load some hikes to be selected
    loadOsmHikes(singleOsmHike1)

    // Select the hike to be updated
    val hikeId = singleOsmHike1[0].id
    hikesViewModel.selectHike(hikeId)
    // Check that the hike is selected
    assertNotNull(hikesViewModel.selectedHike.value)
    assertEquals(hikeId, hikesViewModel.selectedHike.value?.id)
    // Check that the hike is not marked as saved yet
    assertFalse(hikesViewModel.selectedHike.value?.isSaved ?: true)

    // Make sure the saved hikes repository saves the hike
    coEvery { savedHikesRepo.addSavedHike(any()) } returns Unit

    // Save the selected hike
    hikesViewModel.saveHike(hikeId)

    // Check that the selected hike is now marked as saved
    assertTrue(hikesViewModel.selectedHike.value?.isSaved ?: false)
  }

  // ==========================================================================
  // HikesViewModel.unsaveHike
  // ==========================================================================

  @Test
  fun `unsaveHike fails if no corresponding hike is found`() = runTest(dispatcher) {
    // Try to unsave a hike that is not loaded (no hikes were loaded whatsoever)
    var onFailureCalled = false
    hikesViewModel.unsaveHike(
      hikeId = "nonexistent",
      onSuccess = { fail("onSuccess should not have been called") },
      onFailure = { onFailureCalled = true }
    )

    // The saved hikes repository should not be called
    coVerify(exactly = 0) { savedHikesRepo.removeSavedHike(any()) }
    // The appropriate callback should be called
    assertTrue(onFailureCalled)
  }

  @Test
  fun `unsaveHike fails if the repository fails`() = runTest(dispatcher) {
    // Load a hike to be unsaved
    loadSavedHikes(singleSavedHike1)
    // Check that the hike was loaded
    assertEquals(1, hikesViewModel.hikeFlows.value.size)

    // Whenever asked to unsave a hike, the repository will throw an exception
    coEvery { savedHikesRepo.removeSavedHike(any()) } throws Exception("Failed to unsave hike")

    // Try to unsave the loaded hike
    var onFailureCalled = false
    hikesViewModel.unsaveHike(
      hikeId = singleSavedHike1[0].id,
      onSuccess = { fail("onSuccess should not have been called") },
      onFailure = { onFailureCalled = true }
    )

    // The saved hikes repository should be called exactly once
    coVerify(exactly = 1) { savedHikesRepo.removeSavedHike(any()) }
    // The appropriate callback should be called
    assertTrue(onFailureCalled)
  }

  @Test
  fun `unsaveHike updates hike in the loaded OSM hikes`() = runTest(dispatcher) {
    // Make sure the loaded hike is included in the saved hikes
    coEvery { savedHikesRepo.loadSavedHikes() } returns listOf(
      SavedHike(id = singleOsmHike1[0].id, name = singleOsmHike1[0].name ?: "", date = firstJanuary2024)
    )
    hikesViewModel.refreshSavedHikesCache()

    // Load a hike to be unsaved (load hikes from bounds so that the hike won't be removed from the
    // loaded hikes list after being unsaved).
    loadOsmHikes(singleOsmHike1)
    // Check that the hike was loaded
    assertEquals(singleOsmHike1.size, hikesViewModel.hikeFlows.value.size)
    // Check that the hike is saved initially
    assertTrue(hikesViewModel.hikeFlows.value[0].value.isSaved)

    // Make sure the saved hikes repository unsaves the hike
    coEvery { savedHikesRepo.removeSavedHike(any()) } returns Unit

    // Try to unsave the loaded hike
    var onSuccessCalled = false
    hikesViewModel.unsaveHike(
      hikeId = singleOsmHike1[0].id,
      onSuccess = { onSuccessCalled = true },
      onFailure = { fail("onFailure should not have been called") }
    )

    // The saved hikes repository should be called exactly once
    coVerify(exactly = 1) { savedHikesRepo.removeSavedHike(any()) }
    // The appropriate callback should be called
    assertTrue(onSuccessCalled)
    // The hike should now be marked as unsaved
    assertFalse(hikesViewModel.hikeFlows.value[0].value.isSaved)
  }

  @Test
  fun `unsaveHike removes hike from the loaded saved hikes`() = runTest(dispatcher) {
    // Load a hike to be unsaved
    loadSavedHikes(singleSavedHike1)
    // Check that the hike was loaded
    assertEquals(singleSavedHike1.size, hikesViewModel.hikeFlows.value.size)
    // Check that the hike is initially marked as saved
    assertTrue(hikesViewModel.hikeFlows.value[0].value.isSaved)

    // Make sure the saved hikes repository unsaves the hike
    coEvery { savedHikesRepo.removeSavedHike(any()) } returns Unit

    // Try to unsave the loaded hike
    var onSuccessCalled = false
    hikesViewModel.unsaveHike(
      hikeId = singleSavedHike1[0].id,
      onSuccess = { onSuccessCalled = true },
      onFailure = { fail("onFailure should not have been called") }
    )

    // The saved hikes repository should be called exactly once
    coVerify(exactly = 1) { savedHikesRepo.removeSavedHike(any()) }
    // The appropriate callback should be called
    assertTrue(onSuccessCalled)
    // The hike should now be removed from the loaded saved hikes
    assertEquals(0, hikesViewModel.hikeFlows.value.size)
  }

  @Test
  fun `unsaveHike updates selected hike if its status changes`() = runTest(dispatcher) {
    // Make sure the loaded hike is included in the saved hikes
    coEvery { savedHikesRepo.loadSavedHikes() } returns listOf(
      SavedHike(id = singleOsmHike1[0].id, name = singleOsmHike1[0].name ?: "", date = firstJanuary2024)
    )
    hikesViewModel.refreshSavedHikesCache()

    // Load a hike to be unsaved (load hikes from bounds so that the hike won't be removed from the
    // loaded hikes list after being unsaved).
    loadOsmHikes(singleOsmHike1)

    // Select the saved hike that we will later unsave
    val hikeId = singleOsmHike1[0].id
    hikesViewModel.selectHike(hikeId)
    // Check that the hike is selected
    assertNotNull(hikesViewModel.selectedHike.value)
    assertEquals(hikeId, hikesViewModel.selectedHike.value?.id)
    // Check that the hike is saved
    assertTrue(hikesViewModel.selectedHike.value?.isSaved ?: false)

    // Make sure the saved hikes repository unsaves the hike
    coEvery { savedHikesRepo.removeSavedHike(any()) } returns Unit

    // Unsave the selected hike
    hikesViewModel.unsaveHike(hikeId)

    // Check that the selected hike is now marked as unsaved
    assertFalse(hikesViewModel.selectedHike.value?.isSaved ?: true)
  }

  @Test
  fun `unsaveHike clears selected hike if it is unloaded`() = runTest(dispatcher) {
    // Load some hikes to be selected
    loadSavedHikes(singleSavedHike1)

    // Select the saved hike that we will later unsave
    val hikeId = singleSavedHike1[0].id
    hikesViewModel.selectHike(hikeId)
    // Check that the hike is selected
    assertNotNull(hikesViewModel.selectedHike.value)
    assertEquals(hikeId, hikesViewModel.selectedHike.value?.id)

    // Remove the selected hike from the saved hikes list
    coEvery { savedHikesRepo.removeSavedHike(any()) } returns Unit

    // Unsave the selected hike
    hikesViewModel.unsaveHike(hikeId)

    // Check that the selected hike is now unselected
    assertNull(hikesViewModel.selectedHike.value)
  }

  // ==========================================================================
  // HikesViewModel.setPlannedDate
  // ==========================================================================

  @Test
  fun `setPlannedDate fails if no corresponding hike is found`() = runTest(dispatcher) {
    // Try to set a planned date for a hike that is not loaded
    var onFailureCalled = false
    hikesViewModel.setPlannedDate(
      hikeId = "nonexistent",
      date = firstJanuary2024,
      onSuccess = { fail("onSuccess should not have been called") },
      onFailure = { onFailureCalled = true }
    )

    // The saved hikes repository should not be called
    coVerify(exactly = 0) { savedHikesRepo.addSavedHike(any()) }
    coVerify(exactly = 0) { savedHikesRepo.removeSavedHike(any()) }
    // The appropriate callback should be called
    assertTrue(onFailureCalled)
  }

  @Test
  fun `setPlannedDate fails if the repository fails`() = runTest(dispatcher) {
    // Load a hike to set a planned date for
    loadSavedHikes(singleSavedHike1)
    // Check that the hike was loaded
    assertEquals(singleSavedHike1.size, hikesViewModel.hikeFlows.value.size)

    // Whenever we want to set a planned date for an already saved hike, we need to unsave it first.
    // With the following line, we ensure the operation should fail.
    coEvery { savedHikesRepo.removeSavedHike(any()) } throws Exception("Failed to set planned date")
    coEvery { savedHikesRepo.addSavedHike(any()) } throws Exception("Failed to set planned date")

    // Try to set a planned date for the loaded hike
    var onFailureCalled = false
    hikesViewModel.setPlannedDate(
      hikeId = singleSavedHike1[0].id,
      date = firstJanuary2024,
      onSuccess = { fail("onSuccess should not have been called") },
      onFailure = { onFailureCalled = true }
    )

    // The appropriate callback should be called
    assertTrue(onFailureCalled)
  }

  @Test
  fun `setPlannedDate saves hike if not saved yet`() = runTest(dispatcher) {
    // Load a hike to set a planned date for
    loadOsmHikes(singleOsmHike1)
    // Check that the hike was loaded
    assertEquals(singleOsmHike1.size, hikesViewModel.hikeFlows.value.size)
    // Check that the hike is not saved initially
    assertFalse(hikesViewModel.hikeFlows.value[0].value.isSaved)

    // Make sure the saved hikes repository saves the hike
    coEvery { savedHikesRepo.addSavedHike(any()) } returns Unit

    // Try to set a planned date for the loaded hike
    var onSuccessCalled = false
    hikesViewModel.setPlannedDate(
      hikeId = singleOsmHike1[0].id,
      date = firstJanuary2024,
      onSuccess = { onSuccessCalled = true },
      onFailure = { fail("onFailure should not have been called") }
    )

    // The saved hikes repository should be called exactly once
    coVerify(exactly = 1) { savedHikesRepo.addSavedHike(any()) }
    // The appropriate callback should be called
    assertTrue(onSuccessCalled)
    // The hike should now be marked as saved
    assertTrue(hikesViewModel.hikeFlows.value[0].value.isSaved)
    // The hike should now have the planned date set
    assertEquals(firstJanuary2024, hikesViewModel.hikeFlows.value[0].value.plannedDate)
  }

  @Test
  fun `setPlannedDate with null date on unsaved has no effect`() = runTest(dispatcher) {
    // Load a hike to set a planned date for
    loadOsmHikes(singleOsmHike1)
    // Check that the hike was loaded
    assertEquals(singleOsmHike1.size, hikesViewModel.hikeFlows.value.size)
    // Check that the hike is not saved initially
    assertFalse(hikesViewModel.hikeFlows.value[0].value.isSaved)

    // Try to set a null planned date for the loaded hike
    hikesViewModel.setPlannedDate(
      hikeId = singleOsmHike1[0].id,
      date = null
    )

    // The saved hikes repository should not be called
    coVerify(exactly = 0) { savedHikesRepo.addSavedHike(any()) }
    coVerify(exactly = 0) { savedHikesRepo.removeSavedHike(any()) }
    // The hike should not be marked as saved
    assertFalse(hikesViewModel.hikeFlows.value[0].value.isSaved)
    // The hike should not have the planned date set
    assertNull(hikesViewModel.hikeFlows.value[0].value.plannedDate)
  }

  @Test
  fun `setPlannedDate updates hike in the loaded OSM hikes`() = runTest(dispatcher) {
    // Make sure the loaded hike is included in the saved hikes, to test with a hike that is already
    // saved
    coEvery { savedHikesRepo.loadSavedHikes() } returns listOf(
      SavedHike(id = singleOsmHike1[0].id, name = singleOsmHike1[0].name ?: "", date = firstJanuary2024)
    )
    hikesViewModel.refreshSavedHikesCache()

    // Load a hike to set a planned date for
    loadOsmHikes(singleOsmHike1)
    // Check that the hike was loaded
    assertEquals(singleOsmHike1.size, hikesViewModel.hikeFlows.value.size)
    // Check that the hike is saved initially
    assertTrue(hikesViewModel.hikeFlows.value[0].value.isSaved)

    // Make sure the saved hikes repository saves the hike
    coEvery { savedHikesRepo.removeSavedHike(any()) } returns Unit
    coEvery { savedHikesRepo.addSavedHike(any()) } returns Unit

    // Try to set a planned date for the loaded hike (test with setting a null date, which is valid)
    hikesViewModel.setPlannedDate(
      hikeId = singleOsmHike1[0].id,
      date = null
    )

    // The saved hikes repository should be called exactly once
    coVerify(exactly = 1) { savedHikesRepo.addSavedHike(any()) }
    // The hike should now have the planned date set (kinda, cause it's null)
    assertNull(hikesViewModel.hikeFlows.value[0].value.plannedDate)
  }

  @Test
  fun `setPlannedDate updates selected hike if its status changes`() = runTest(dispatcher) {
    // Load some hikes to be selected
    loadOsmHikes(singleOsmHike1)

    // Select the hike to be updated
    val hikeId = singleOsmHike1[0].id
    hikesViewModel.selectHike(hikeId)
    // Check that the hike is selected
    assertNotNull(hikesViewModel.selectedHike.value)
    assertEquals(hikeId, hikesViewModel.selectedHike.value?.id)
    // Check that the hike is not marked as saved yet
    assertFalse(hikesViewModel.selectedHike.value?.isSaved ?: true)

    // Make sure the saved hikes repository saves the hike
    coEvery { savedHikesRepo.addSavedHike(any()) } returns Unit

    // Set a planned date for the selected hike
    hikesViewModel.setPlannedDate(hikeId, firstJanuary2024)

    // Check that the selected hike is now marked as saved
    assertTrue(hikesViewModel.selectedHike.value?.isSaved ?: false)
    // Check that the selected hike now has the planned date set
    assertEquals(firstJanuary2024, hikesViewModel.selectedHike.value?.plannedDate)
  }

  // ==========================================================================
  // HikesViewModel.loadHikesInBounds
  // ==========================================================================

  @Test
  fun `loadHikesInBounds fails if the repository fails`() = runTest(dispatcher) {
    // Make sure the repository throws an exception when asked for hikes in bounds
    coEvery { osmHikesRepo.getRoutes(any(), any(), any()) } throws Exception("Failed to load hikes in bounds")

    // Try to load hikes in bounds through the view model
    var onFailureCalled = false
    hikesViewModel.loadHikesInBounds(
      bounds = BoundingBox(0.0, 0.0, 0.0, 0.0),
      onSuccess = { fail("onSuccess should not have been called") },
      onFailure = { onFailureCalled = true }
    )

    // The OSM hikes repository should be called exactly once
    coVerify(exactly = 1) { osmHikesRepo.getRoutes(any(), any(), any()) }
    // The appropriate callback should be called
    assertTrue(onFailureCalled)
  }

  @Test
  fun `loadHikesInBounds succeeds if the repository succeeds`() = runTest(dispatcher) {
    // Make sure the repo has some OSM hikes to return
    every { osmHikesRepo.getRoutes(any(), any(), any()) } answers {
      val onSuccess = secondArg<(List<HikeRoute>) -> Unit>()
      onSuccess(doubleOsmHikes1)
    }

    // Try to load hikes in bounds through the view model
    var onSuccessCalled = false
    hikesViewModel.loadHikesInBounds(
      bounds = BoundingBox(0.0, 0.0, 0.0, 0.0),
      onSuccess = { onSuccessCalled = true },
      onFailure = { fail("onFailure should not have been called") }
    )

    // The OSM hikes repository should be called exactly once
    coVerify(exactly = 1) { osmHikesRepo.getRoutes(any(), any(), any()) }
    // The appropriate callback should be called
    assertTrue(onSuccessCalled)
    // The view model should now contain the loaded hikes
    assertEquals(doubleOsmHikes1.size, hikesViewModel.hikeFlows.value.size)
    assertEquals(doubleOsmHikes1[0].id, hikesViewModel.hikeFlows.value[0].value.id)
    assertEquals(doubleOsmHikes1[1].id, hikesViewModel.hikeFlows.value[1].value.id)
  }

  @Test
  fun `loadHikesInBounds sets loading to true then false`() = runTest(dispatcher) {
    // Listen to the changes made to loading during the call
    val emissions = mutableListOf<Boolean>()
    val job = backgroundScope.launch {
      hikesViewModel.loading.collect { emissions.add(it) }
    }

    // Set the repository to throw an exception because we do not care
    coEvery { osmHikesRepo.getRoutes(any(), any(), any()) } throws Exception("Failed to load saved hikes")

    // Load hikes in bounds
    hikesViewModel.loadHikesInBounds(BoundingBox(0.0, 0.0, 0.0, 0.0))

    // Because we are on an UnconfinedTestDispatcher(), the coroutine should be done by now, hence
    // we can stop listening to the values emitted by loading.
    job.cancel()

    // Check that loading was false at first, then true during the call, and false again at the end
    assertEquals(listOf(false, true, false), emissions)
  }

  @Test
  fun `loadHikesInBounds updates the selected hike`() = runTest(dispatcher) {
    // Load some hikes to be selected
    val osmHike = singleOsmHike1[0]
    loadSavedHikes(listOf(SavedHike(id = osmHike.id, name = osmHike.name ?: "", date = firstJanuary2024)))

    // Select the hike to be updated
    hikesViewModel.selectHike(osmHike.id)
    // Check that the hike is selected
    assertNotNull(hikesViewModel.selectedHike.value)
    assertEquals(osmHike.id, hikesViewModel.selectedHike.value?.id)
    // Check that OSM details of the selected hike are not obtained yet
    assertFalse(hikesViewModel.selectedHike.value?.description is DeferredData.Obtained)
    assertFalse(hikesViewModel.selectedHike.value?.bounds is DeferredData.Obtained)
    assertFalse(hikesViewModel.selectedHike.value?.waypoints is DeferredData.Obtained)

    // Make sure the repo has some OSM hikes to return
    every { osmHikesRepo.getRoutes(any(), any(), any()) } answers {
      val onSuccess = secondArg<(List<HikeRoute>) -> Unit>()
      onSuccess(doubleOsmHikes1)
    }

    // Load hikes in bounds
    hikesViewModel.loadHikesInBounds(BoundingBox(0.0, 0.0, 0.0, 0.0))

    // Check that the selected hike is still selected
    assertNotNull(hikesViewModel.selectedHike.value)
    assertEquals(osmHike.id, hikesViewModel.selectedHike.value?.id)
    // Check that the selected hike's OSM details have been updated
    assertTrue(hikesViewModel.selectedHike.value?.description is DeferredData.Obtained)
    assertTrue(hikesViewModel.selectedHike.value?.bounds is DeferredData.Obtained)
    assertTrue(hikesViewModel.selectedHike.value?.waypoints is DeferredData.Obtained)
  }

  @Test
  fun `loadHikesInBounds clears the selected hike if it is unloaded`() = runTest(dispatcher) {
    // Load some hikes to be selected
    loadSavedHikes(singleSavedHike1)

    // Select the hike to be updated
    hikesViewModel.selectHike(singleSavedHike1[0].id)
    // Check that the hike is selected
    assertNotNull(hikesViewModel.selectedHike.value)
    assertEquals(singleSavedHike1[0].id, hikesViewModel.selectedHike.value?.id)

    // Make sure the repo has some OSM hikes to return
    every { osmHikesRepo.getRoutes(any(), any(), any()) } answers {
      val onSuccess = secondArg<(List<HikeRoute>) -> Unit>()
      onSuccess(doubleOsmHikes1)
    }

    // Load hikes in bounds
    hikesViewModel.loadHikesInBounds(BoundingBox(0.0, 0.0, 0.0, 0.0))

    // Check that the selected hike is now unselected
    assertNull(hikesViewModel.selectedHike.value)
  }

  // ==========================================================================
  // HikesViewModel.retrieveLoadedHikesOsmData
  // ==========================================================================

  @Test
  fun `retrieveLoadedHikesOsmData fails if the repository fails`() = runTest(dispatcher) {
    // Make sure there are loaded hikes with missing OSM details
    loadSavedHikes(singleSavedHike1)

    // Make sure the repository throws an exception when asked for hikes IDs
    coEvery { osmHikesRepo.getRoutesByIds(any(), any(), any()) } throws Exception("Failed to load hikes in bounds")

    // Try to retrieve the OSM data for the loaded hikes
    var onFailureCalled = false
    hikesViewModel.retrieveLoadedHikesOsmData(
      onSuccess = { fail("onSuccess should not have been called") },
      onFailure = { onFailureCalled = true }
    )

    // The OSM hikes repository should be called exactly once
    coVerify(exactly = 1) { osmHikesRepo.getRoutesByIds(any(), any(), any()) }
    // The appropriate callback should be called
    assertTrue(onFailureCalled)
  }

  @Test
  fun `retrieveLoadedHikesOsmData updates hikes with their OSM data`() = runTest(dispatcher) {
    // Make sure there are loaded hikes with missing OSM details
    val osmHike = singleOsmHike1[0]
    loadSavedHikes(listOf(SavedHike(id = osmHike.id, name = osmHike.name ?: "", date = null)))

    // Make sure the repository returns the OSM data for the loaded hikes
    coEvery { osmHikesRepo.getRoutesByIds(any(), any(), any()) } answers {
      val onSuccess = secondArg<(List<HikeRoute>) -> Unit>()
      onSuccess(singleOsmHike1)
    }

    // Retrieve the OSM data for the loaded hikes
    var onSuccessCalled = false
    hikesViewModel.retrieveLoadedHikesOsmData(
      onSuccess = { onSuccessCalled = true },
      onFailure = { fail("onFailure should not have been called") }
    )

    // The OSM hikes repository should be called exactly once
    verify(exactly = 1) { osmHikesRepo.getRoutesByIds(any(), any(), any()) }
    // The appropriate callback should be called
    assertTrue(onSuccessCalled)
    // The view model should now contain the loaded hikes with their OSM data
    assertEquals(singleOsmHike1.size, hikesViewModel.hikeFlows.value.size)
    assertEquals(singleOsmHike1[0].id, hikesViewModel.hikeFlows.value[0].value.id)
    assertEquals(singleOsmHike1[0].description, (hikesViewModel.hikeFlows.value[0].value.description as DeferredData.Obtained).data)
    assertEquals(singleOsmHike1[0].bounds, (hikesViewModel.hikeFlows.value[0].value.bounds as DeferredData.Obtained).data)
    assertEquals(singleOsmHike1[0].ways, (hikesViewModel.hikeFlows.value[0].value.waypoints as DeferredData.Obtained).data)
  }

  @Test
  fun `retrieveLoadedHikesOsmData does not request already available data`() = runTest(dispatcher) {
    // Load a first OSM hike with all its OSM data
    loadOsmHikes(singleOsmHike1)

    // Then load saved hikes, including the already loaded OSM hike plus another one
    loadSavedHikes(listOf(
      SavedHike(id = singleOsmHike1[0].id, name = singleOsmHike1[0].name ?: "", date = firstJanuary2024),
      SavedHike(id = singleOsmHike2[0].id, name = singleOsmHike2[0].name ?: "", date = null)
    ))

    // Make sure the repository returns the OSM data for the loaded hikes
    every { osmHikesRepo.getRoutesByIds(any(), any(), any()) } answers {
      val ids = firstArg<List<String>>()
      assertEquals(1, ids.size)
      assertEquals(singleOsmHike2[0].id, ids[0])
      val onSuccess = secondArg<(List<HikeRoute>) -> Unit>()
      onSuccess(singleOsmHike2)
    }

    // Retrieve the OSM data for the loaded hikes
    hikesViewModel.retrieveLoadedHikesOsmData()

    // The OSM hikes repository should be called exactly once
    verify(exactly = 1) { osmHikesRepo.getRoutesByIds(any(), any(), any()) }
    // The view model should now contain the loaded hikes with their OSM data
    assertEquals(singleOsmHike1.size + singleOsmHike2.size, hikesViewModel.hikeFlows.value.size)
    val hike1 = hikesViewModel.hikeFlows.value.find { it.value.id == singleOsmHike1[0].id }?.value
    assertNotNull(hike1)
    assertEquals(singleOsmHike1[0].description, (hike1?.description as DeferredData.Obtained).data)
    assertEquals(singleOsmHike1[0].bounds, (hike1.bounds as DeferredData.Obtained).data)
    assertEquals(singleOsmHike1[0].ways, (hike1.waypoints as DeferredData.Obtained).data)
    val hike2 = hikesViewModel.hikeFlows.value.find { it.value.id == singleOsmHike2[0].id }?.value
    assertNotNull(hike2)
    assertEquals(singleOsmHike2[0].description, (hike2?.description as DeferredData.Obtained).data)
    assertEquals(singleOsmHike2[0].bounds, (hike2.bounds as DeferredData.Obtained).data)
    assertEquals(singleOsmHike2[0].ways, (hike2.waypoints as DeferredData.Obtained).data)
  }

  @Test
  fun `retrieveLoadedHikesOsmData does nothing if all data is available`() = runTest(dispatcher) {
    // Make sure there are loaded hikes with their OSM details
    loadOsmHikes(doubleOsmHikes1)

    // Try to retrieve the OSM data for the loaded hikes
    hikesViewModel.retrieveLoadedHikesOsmData()

    // The OSM hikes repository should not be called
    coVerify(exactly = 0) { osmHikesRepo.getRoutesByIds(any(), any(), any()) }
  }

  @Test
  fun `retrieveLoadedHikesOsmData sets loading to true then false`() = runTest(dispatcher) {
    // Listen to the changes made to loading during the call
    val emissions = mutableListOf<Boolean>()
    val job = backgroundScope.launch {
      hikesViewModel.loading.collect { emissions.add(it) }
    }

    // Set the repository to throw an exception because we do not care
    coEvery { osmHikesRepo.getRoutesByIds(any(), any(), any()) } throws Exception("Failed to load saved hikes")

    // Retrieve the OSM data for the loaded hikes
    hikesViewModel.retrieveLoadedHikesOsmData()

    // Because we are on an UnconfinedTestDispatcher(), the coroutine should be done by now, hence
    // we can stop listening to the values emitted by loading.
    job.cancel()

    // Check that loading was false at first, then true during the call, and false again at the end
    assertEquals(listOf(false, true, false), emissions)
  }

  @Test
  fun `retrieveLoadedHikesOsmData updates the selected hike`() = runTest(dispatcher) {
    // Load some hikes to be selected
    val osmHike = singleOsmHike1[0]
    loadSavedHikes(listOf(SavedHike(id = osmHike.id, name = osmHike.name ?: "", date = null)))

    // Select the hike to be updated
    hikesViewModel.selectHike(osmHike.id)
    // Check that the hike is selected
    assertNotNull(hikesViewModel.selectedHike.value)
    assertEquals(osmHike.id, hikesViewModel.selectedHike.value?.id)
    // Check that OSM details of the selected hike are not obtained yet
    assertFalse(hikesViewModel.selectedHike.value?.description is DeferredData.Obtained)
    assertFalse(hikesViewModel.selectedHike.value?.bounds is DeferredData.Obtained)
    assertFalse(hikesViewModel.selectedHike.value?.waypoints is DeferredData.Obtained)

    // Make sure the repository returns the OSM data for the loaded hikes
    coEvery { osmHikesRepo.getRoutesByIds(any(), any(), any()) } answers {
      val onSuccess = secondArg<(List<HikeRoute>) -> Unit>()
      onSuccess(singleOsmHike1)
    }

    // Retrieve the OSM data for the loaded hikes
    hikesViewModel.retrieveLoadedHikesOsmData()

    // Check that the selected hike is still selected
    assertNotNull(hikesViewModel.selectedHike.value)
    assertEquals(osmHike.id, hikesViewModel.selectedHike.value?.id)
    // Check that the selected hike's OSM details have been updated
    assertTrue(hikesViewModel.selectedHike.value?.description is DeferredData.Obtained)
    assertTrue(hikesViewModel.selectedHike.value?.bounds is DeferredData.Obtained)
    assertTrue(hikesViewModel.selectedHike.value?.waypoints is DeferredData.Obtained)
  }

  // ==========================================================================
  // HikesViewModel.retrieveElevationDataFor
  // ==========================================================================

  @Test
  fun `retrieveElevationDataFor fails if hike is not found`() = runTest(dispatcher) {
    // Try to retrieve elevation data for a hike that is not loaded
    var onFailureCalled = false
    hikesViewModel.retrieveElevationDataFor(
      hikeId = "nonexistent",
      onSuccess = { fail("onSuccess should not have been called") },
      onFailure = { onFailureCalled = true }
    )

    // The elevation repository should not be called at all
    verify(exactly = 0) { elevationRepo.getElevation(any(), any(), any(), any()) }
    // The appropriate callback should be called
    assertTrue(onFailureCalled)
  }

  @Test
  fun `retrieveElevationDataFor fails if hike has no waypoints`() = runTest(dispatcher) {
    // Make sure there is one loaded hike but with no waypoints
    loadSavedHikes(singleSavedHike1)

    // Try to retrieve elevation data for the loaded hike
    var onFailureCalled = false
    hikesViewModel.retrieveElevationDataFor(
      hikeId = singleSavedHike1[0].id,
      onSuccess = { fail("onSuccess should not have been called") },
      onFailure = { onFailureCalled = true }
    )

    // The elevation repository should not be called at all
    verify(exactly = 0) { elevationRepo.getElevation(any(), any(), any(), any()) }
    // The appropriate callback should be called
    assertTrue(onFailureCalled)
  }

  @Test
  fun `retrieveElevationDataFor fails if the repository fails`() = runTest(dispatcher) {
    // Make sure there is one loaded hike with waypoints
    loadOsmHikes(singleOsmHike1)

    // Make sure the repository throws an exception when asked for elevation data
    every { elevationRepo.getElevation(any(), any(), any(), any()) } throws Exception("Failed to load elevation data")

    // Try to retrieve elevation data for the loaded hike
    var onFailureCalled = false
    hikesViewModel.retrieveElevationDataFor(
      hikeId = singleOsmHike1[0].id,
      onSuccess = { fail("onSuccess should not have been called") },
      onFailure = { onFailureCalled = true }
    )

    // The elevation repository should be called exactly once
    coVerify(exactly = 1) { elevationRepo.getElevation(any(), any(), any(), any()) }
    // The appropriate callback should be called
    assertTrue(onFailureCalled)
  }

  @Test
  fun `retrieveElevationDataFor updates the hike with its elevation data`() = runTest(dispatcher) {
    // Make sure there is one loaded hike with waypoints
    loadOsmHikes(singleOsmHike1)
    // Check that the hike was loaded
    assertEquals(1, hikesViewModel.hikeFlows.value.size)
    // Check that the hike does not have elevation data yet
    assertFalse(hikesViewModel.hikeFlows.value[0].value.elevation is DeferredData.Obtained)

    // Make sure the repository returns the elevation data for the loaded hike
    coEvery { elevationRepo.getElevation(any(), any(), any(), any()) } answers {
      val onSuccess = thirdArg<(List<Double>) -> Unit>()
      onSuccess(elevationProfile1)
    }

    // Retrieve the elevation data for the loaded hike
    var onSuccessCalled = false
    hikesViewModel.retrieveElevationDataFor(
      hikeId = singleOsmHike1[0].id,
      onSuccess = { onSuccessCalled = true },
      onFailure = { fail("onFailure should not have been called") }
    )

    // The elevation repository should be called exactly once
    verify(exactly = 1) { elevationRepo.getElevation(any(), any(), any(), any()) }
    // The appropriate callback should be called
    assertTrue(onSuccessCalled)
    // The view model should now contain the loaded hike with its elevation data
    assertEquals(1, hikesViewModel.hikeFlows.value.size)
    assertEquals(singleOsmHike1[0].id, hikesViewModel.hikeFlows.value[0].value.id)
    assertEquals(elevationProfile1, (hikesViewModel.hikeFlows.value[0].value.elevation as DeferredData.Obtained).data)
  }

  @Test
  fun `retrieveElevationDataFor skips if the data is already available`() = runTest(dispatcher) {
    // Make sure there is one loaded hike with waypoints
    loadOsmHikes(singleOsmHike1)

    // Make sure the repository returns the elevation data for the loaded hike
    coEvery { elevationRepo.getElevation(any(), any(), any(), any()) } answers {
      val onSuccess = thirdArg<(List<Double>) -> Unit>()
      onSuccess(elevationProfile1)
    }

    // Retrieve the elevation data for the loaded hike
    hikesViewModel.retrieveElevationDataFor(hikeId = singleOsmHike1[0].id)

    // Check that the hike has elevation data
    assertTrue(hikesViewModel.hikeFlows.value[0].value.elevation is DeferredData.Obtained)

    // Request the data again to check that the repository is not called
    hikesViewModel.retrieveElevationDataFor(hikeId = singleOsmHike1[0].id)

    // The elevation repository should only be called once
    coVerify(exactly = 1) { elevationRepo.getElevation(any(), any(), any(), any()) }
  }

  @Test
  fun `retrieveElevationDataFor updates the selected hike if needed`() = runTest(dispatcher) {
    // Make sure there is one loaded hike with waypoints
    loadOsmHikes(singleOsmHike1)

    // Select the hike to be updated
    hikesViewModel.selectHike(singleOsmHike1[0].id)
    // Check that the hike is selected
    assertNotNull(hikesViewModel.selectedHike.value)
    assertEquals(singleOsmHike1[0].id, hikesViewModel.selectedHike.value?.id)
    // Check that the hike does not have elevation data yet
    assertFalse(hikesViewModel.selectedHike.value?.elevation is DeferredData.Obtained)

    // Make sure the repository returns the elevation data for the loaded hike
    coEvery { elevationRepo.getElevation(any(), any(), any(), any()) } answers {
      val onSuccess = thirdArg<(List<Double>) -> Unit>()
      onSuccess(elevationProfile1)
    }

    // Retrieve the elevation data for the loaded hike
    hikesViewModel.retrieveElevationDataFor(hikeId = singleOsmHike1[0].id)

    // Check that the selected hike is still selected
    assertNotNull(hikesViewModel.selectedHike.value)
    assertEquals(singleOsmHike1[0].id, hikesViewModel.selectedHike.value?.id)
    // Check that the selected hike's elevation data has been updated
    assertTrue(hikesViewModel.selectedHike.value?.elevation is DeferredData.Obtained)
    assertEquals(elevationProfile1, (hikesViewModel.selectedHike.value?.elevation as DeferredData.Obtained).data)
  }
}