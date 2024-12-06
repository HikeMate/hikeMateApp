package ch.hikemate.app.ui.map

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import ch.hikemate.app.model.elevation.ElevationService
import ch.hikemate.app.model.profile.HikingLevel
import ch.hikemate.app.model.profile.Profile
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
import ch.hikemate.app.model.route.toBoundingBox
import ch.hikemate.app.ui.components.DetailRow
import ch.hikemate.app.ui.navigation.NavigationActions
import com.google.firebase.Timestamp
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
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
  private lateinit var elevationService: ElevationService
  private lateinit var hikesViewModel: HikesViewModel

  private val hikeId = "1"
  private val detailedHike =
      DetailedHike(
          id = hikeId,
          color = Hike(hikeId, false, null, null).getColor(),
          isSaved = false,
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

  private suspend fun setupCompleteScreenWithSelected(hike: DetailedHike) {
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

    // Make sure that the hike is loaded from bounds when the view model gets it
    `when`(hikesRepository.getRoutes(any(), any(), any())).thenAnswer {
      val onSuccess = it.getArgument<(List<HikeRoute>) -> Unit>(1)
      onSuccess(listOf(hikeAsOsm))
    }

    // Make sure the appropriate elevation profile is obtained when requested
    `when`(elevationService.getElevation(any(), any(), any(), any())).thenAnswer {
      val onSuccess = it.getArgument<(List<Double>) -> Unit>(2)
      onSuccess(hike.elevation)
    }

    // Reset the view model
    hikesViewModel =
        HikesViewModel(
            savedHikesRepository, hikesRepository, elevationService, UnconfinedTestDispatcher())

    // Load the hike from OSM, as if the user had searched it on the map
    hikesViewModel.loadHikesInBounds(detailedHike.bounds.toBoundingBox())

    // Retrieve the hike's elevation data from the repository
    hikesViewModel.retrieveElevationDataFor(hikeId)

    // Compute the hike's details
    hikesViewModel.computeDetailsFor(hikeId)

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

  private val route =
      HikeRoute(
          id = "1",
          bounds = Bounds(minLat = 45.9, minLon = 7.6, maxLat = 46.0, maxLon = 7.7),
          ways = listOf(LatLong(45.9, 7.6), LatLong(45.95, 7.65), LatLong(46.0, 7.7)),
          name = "Sample Hike",
          description =
              "A scenic trail with breathtaking views of the Matterhorn and surrounding glaciers.")

  private val profile =
      Profile(
          id = "1",
          name = "John Doe",
          email = "john-doe@gmail.com",
          hikingLevel = HikingLevel.INTERMEDIATE,
          joinedDate = Timestamp.now())

  @OptIn(ExperimentalCoroutinesApi::class)
  @Before
  fun setUp() {
    mockNavigationActions = mock(NavigationActions::class.java)
    savedHikesRepository = mock(SavedHikesRepository::class.java)
    hikesRepository = mock(HikeRoutesRepository::class.java)
    elevationService = mock(ElevationService::class.java)
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
