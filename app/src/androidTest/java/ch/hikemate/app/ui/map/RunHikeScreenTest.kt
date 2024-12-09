package ch.hikemate.app.ui.map

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import ch.hikemate.app.model.elevation.ElevationRepository
import ch.hikemate.app.model.route.Bounds
import ch.hikemate.app.model.route.DetailedHike
import ch.hikemate.app.model.route.Hike
import ch.hikemate.app.model.route.HikeDifficulty
import ch.hikemate.app.model.route.HikeRoute
import ch.hikemate.app.model.route.HikeRoutesRepository
import ch.hikemate.app.model.route.HikesViewModel
import ch.hikemate.app.model.route.LatLong
import ch.hikemate.app.model.route.saved.SavedHike
import ch.hikemate.app.model.route.saved.SavedHikesRepository
import ch.hikemate.app.ui.components.CenteredLoadingAnimation
import ch.hikemate.app.ui.components.DetailRow
import ch.hikemate.app.ui.navigation.NavigationActions
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class RunHikeScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var mockNavigationActions: NavigationActions
  private lateinit var savedHikesRepository: SavedHikesRepository
  private lateinit var hikesRepository: HikeRoutesRepository
  private lateinit var elevationRepository: ElevationRepository
  private lateinit var hikesViewModel: HikesViewModel

  private val hikeId = "1"
  private val detailedHike =
      DetailedHike(
          id = hikeId,
          color = Hike(hikeId, true, null, null).getColor(),
          isSaved = true,
          plannedDate = null,
          name = "Sample Hike",
          description =
              "A scenic trail with breathtaking views of the Matterhorn and surrounding glaciers.",
          bounds = Bounds(minLat = 45.9, minLon = 7.6, maxLat = 46.0, maxLon = 7.7),
          waypoints = listOf(LatLong(45.9, 7.6), LatLong(45.95, 7.65), LatLong(46.0, 7.7)),
          elevation = listOf(0.0, 10.0, 20.0, 30.0),
          distance = 13.543077559212616,
          elevationGain = 68.0,
          estimatedTime = 169.3169307105514,
          difficulty = HikeDifficulty.DIFFICULT,
      )

  /** @param hike The hike to display on the screen. For test purposes, should always be saved. */
  private suspend fun setupCompleteScreenWithSelected(
      hike: DetailedHike,
      waypointsRetrievalSucceeds: Boolean = true,
      elevationRetrievalSucceeds: Boolean = true,
      alreadyLoadData: Boolean = true
  ) {

    // This setup function is designed for saved hikes only. If the provided hike is not saved, it
    // won't be loaded in the view model, hence the test will fail for sure. This message helps the
    // developer to find out this cause without many hours of debugging.
    if (!hike.isSaved) {
      fail(
          "setupCompleteScreenWithSelected is designed for saved hikes only, you provided a hike with isSaved=false.")
    }

    val asSavedHike = SavedHike(hike.id, hike.name ?: "", hike.plannedDate)

    val hikeAsOsm =
        HikeRoute(
            id = hike.id,
            bounds = hike.bounds,
            ways = hike.waypoints,
            name = hike.name,
            description = hike.description,
        )

    // Make sure the saved status of the hike is set correctly
    `when`(savedHikesRepository.loadSavedHikes())
        .thenReturn(if (hike.isSaved) listOf(asSavedHike) else emptyList())

    if (waypointsRetrievalSucceeds) {
      // Make sure that the hike is loaded from bounds when the view model gets it
      `when`(hikesRepository.getRoutesByIds(any(), any(), any())).thenAnswer {
        val onSuccess = it.getArgument<(List<HikeRoute>) -> Unit>(1)
        onSuccess(listOf(hikeAsOsm))
      }
    } else {
      // Make sure that the hike's bounds can't be loaded when the view model gets it
      `when`(hikesRepository.getRoutesByIds(any(), any(), any())).thenAnswer {
        val onFailure = it.getArgument<(Exception) -> Unit>(2)
        onFailure(Exception("Failed to load hike bounds"))
      }
    }

    if (waypointsRetrievalSucceeds && elevationRetrievalSucceeds) {
      // Make sure the appropriate elevation profile is obtained when requested
      `when`(elevationRepository.getElevation(any(), any(), any())).thenAnswer {
        val onSuccess = it.getArgument<(List<Double>) -> Unit>(1)
        onSuccess(hike.elevation)
      }
    } else {
      // Make sure the elevation profile can't be obtained when requested
      `when`(elevationRepository.getElevation(any(), any(), any())).thenAnswer {
        val onFailure = it.getArgument<(Exception) -> Unit>(2)
        onFailure(Exception("Failed to load elevation data"))
      }
    }

    // Reset the view model
    hikesViewModel =
        HikesViewModel(
            savedHikesRepository, hikesRepository, elevationRepository, UnconfinedTestDispatcher())

    // Load the hike from saved hikes
    hikesViewModel.loadSavedHikes()

    if (alreadyLoadData) {
      // Load the hike's waypoints
      hikesViewModel.retrieveLoadedHikesOsmData()

      // Retrieve the hike's elevation data from the repository
      hikesViewModel.retrieveElevationDataFor(hikeId)

      // Compute the hike's details
      hikesViewModel.computeDetailsFor(hikeId)
    }

    // Mark the hike as selected, to make sure it is the one displayed on the details screen
    hikesViewModel.selectHike(hikeId)

    // Set the test's content
    composeTestRule.setContent {
      RunHikeScreen(
          hikesViewModel = hikesViewModel,
          navigationActions = mockNavigationActions,
      )
    }
  }

  @Before
  fun setUp() {
    mockNavigationActions = mock(NavigationActions::class.java)
    savedHikesRepository = mock(SavedHikesRepository::class.java)
    hikesRepository = mock(HikeRoutesRepository::class.java)
    elevationRepository = mock(ElevationRepository::class.java)
  }

  @Test
  fun runHikeScreen_displaysLoadingAnimationForWaypoints() = runTest {
    setupCompleteScreenWithSelected(detailedHike, waypointsRetrievalSucceeds = false)

    composeTestRule
        .onNodeWithTag(CenteredLoadingAnimation.TEST_TAG_CENTERED_LOADING_ANIMATION)
        .assertIsDisplayed()
  }

  @Test
  fun runHikeScreen_displaysLoadingAnimationForElevation() = runTest {
    setupCompleteScreenWithSelected(detailedHike, elevationRetrievalSucceeds = false)

    composeTestRule
        .onNodeWithTag(CenteredLoadingAnimation.TEST_TAG_CENTERED_LOADING_ANIMATION)
        .assertIsDisplayed()
  }

  @OptIn(ExperimentalTestApi::class)
  @Test
  fun runHikeScreen_loadsMissingData() = runTest {
    setupCompleteScreenWithSelected(detailedHike, alreadyLoadData = false)

    composeTestRule.waitUntilExactlyOneExists(
        hasTestTag(RunHikeScreen.TEST_TAG_MAP), timeoutMillis = 10000)

    verify(hikesRepository).getRoutesByIds(any(), any(), any())
    verify(elevationRepository).getElevation(any(), any(), any())
  }

  @Test
  fun runHikeScreen_displaysMap() = runTest {
    setupCompleteScreenWithSelected(detailedHike)
    composeTestRule.onNodeWithTag(RunHikeScreen.TEST_TAG_MAP).assertIsDisplayed()
  }

  @Test
  fun runHikeScreen_displaysBackButton() = runTest {
    setupCompleteScreenWithSelected(detailedHike)
    composeTestRule.onNodeWithTag(RunHikeScreen.TEST_TAG_BACK_BUTTON).assertIsDisplayed()
  }

  @Test
  fun runHikeScreen_displaysZoomButtons() = runTest {
    setupCompleteScreenWithSelected(detailedHike)
    composeTestRule.onNodeWithTag(RunHikeScreen.TEST_TAG_ZOOM_BUTTONS).assertIsDisplayed()
  }

  @Test
  fun runHikeScreen_displaysBottomSheet() = runTest {
    setupCompleteScreenWithSelected(detailedHike)
    composeTestRule.onNodeWithTag(RunHikeScreen.TEST_TAG_BOTTOM_SHEET).assertExists()
  }

  /** Test all details which do not change during the hike run. */
  @Test
  fun runHikeScreen_displaysStaticDetails() = runTest {
    setupCompleteScreenWithSelected(detailedHike)

    composeTestRule.onNodeWithTag(RunHikeScreen.TEST_TAG_HIKE_NAME).assertIsDisplayed()

    composeTestRule.onAllNodesWithTag(DetailRow.TEST_TAG_DETAIL_ROW_TAG).assertCountEquals(4)
    composeTestRule.onAllNodesWithTag(DetailRow.TEST_TAG_DETAIL_ROW_VALUE).assertCountEquals(4)

    // TODO: THIS IS NOT LOCALE FRIENDLY
    composeTestRule
        .onNodeWithTag(RunHikeScreen.TEST_TAG_TOTAL_DISTANCE_TEXT)
        .assertIsDisplayed()
        .assert(hasText("13.54km"))
  }

  @Test
  fun runHikeScreen_displaysElevationGraph() = runTest {
    setupCompleteScreenWithSelected(detailedHike)

    composeTestRule.onNodeWithTag(RunHikeScreen.TEST_TAG_ELEVATION_GRAPH).assertIsDisplayed()
  }

  @Test
  fun runHikeScreen_displaysStopHikeButton() = runTest {
    setupCompleteScreenWithSelected(detailedHike)

    composeTestRule.onNodeWithTag(RunHikeScreen.TEST_TAG_STOP_HIKE_BUTTON).assertIsDisplayed()
  }

  @Test
  fun runHikeScreen_stopHikeButton_navigatesBack() = runTest {
    setupCompleteScreenWithSelected(detailedHike)
    doNothing().`when`(mockNavigationActions).goBack()

    composeTestRule
        .onNodeWithTag(RunHikeScreen.TEST_TAG_STOP_HIKE_BUTTON)
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitForIdle()

    verify(mockNavigationActions).goBack()
  }

  /**
   * TODO This test just tests the static hard-coded value for now, but should be updated as soon as
   * the screen is implemented to display the actual progress indicator.
   */
  @Test
  fun runHikeScreen_displaysProgressIndicator() = runTest {
    setupCompleteScreenWithSelected(detailedHike)

    composeTestRule
        .onNodeWithTag(RunHikeScreen.TEST_TAG_PROGRESS_TEXT)
        .assertIsDisplayed()
        .assert(hasText("23% complete"))
  }
}
