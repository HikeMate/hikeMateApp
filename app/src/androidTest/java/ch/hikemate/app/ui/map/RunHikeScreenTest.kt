package ch.hikemate.app.ui.map

import android.location.Location
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import ch.hikemate.app.model.elevation.ElevationRepository
import ch.hikemate.app.model.facilities.FacilitiesRepository
import ch.hikemate.app.model.facilities.FacilitiesViewModel
import ch.hikemate.app.model.facilities.Facility
import ch.hikemate.app.model.facilities.FacilityType
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
import ch.hikemate.app.ui.components.CenteredErrorAction
import ch.hikemate.app.ui.components.DetailRow
import ch.hikemate.app.ui.components.LocationPermissionAlertDialog
import ch.hikemate.app.ui.navigation.NavigationActions
import ch.hikemate.app.utils.LocationUtils
import ch.hikemate.app.utils.MapUtils
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import kotlin.time.Duration.Companion.seconds
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
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker

@OptIn(ExperimentalCoroutinesApi::class)
class RunHikeScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var mockNavigationActions: NavigationActions
  private lateinit var savedHikesRepository: SavedHikesRepository
  private lateinit var hikesRepository: HikeRoutesRepository
  private lateinit var elevationRepository: ElevationRepository
  private lateinit var hikesViewModel: HikesViewModel
  private lateinit var facilitiesViewModel: FacilitiesViewModel
  private lateinit var facilitiesRepository: FacilitiesRepository

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

  private val detailedHike2 =
      DetailedHike(
          id = hikeId,
          color = Hike(hikeId, false, null, null).getColor(),
          isSaved = true,
          plannedDate = null,
          name = "Sample Hike",
          description =
              "A scenic trail with breathtaking views of the Matterhorn and surrounding glaciers.",
          bounds = Bounds(minLat = 45.9, minLon = 7.6, maxLat = 45.91, maxLon = 7.61),
          waypoints = listOf(LatLong(45.9, 7.6), LatLong(45.908, 7.605), LatLong(45.91, 7.61)),
          elevation = listOf(0.0, 10.0, 20.0, 30.0),
          distance = 13.543077559212616,
          elevationGain = 68.0,
          estimatedTime = 169.3169307105514,
          difficulty = HikeDifficulty.DIFFICULT,
      )

  private val detailedHike3 =
      DetailedHike(
          id = hikeId,
          color = Hike(hikeId, false, null, null).getColor(),
          isSaved = true,
          plannedDate = null,
          name = "Sample Hike",
          description =
              "A scenic trail with breathtaking views of the Matterhorn and surrounding glaciers.",
          bounds = Bounds(minLat = 44.9, minLon = 6.6, maxLat = 46.91, maxLon = 8.61),
          waypoints = listOf(LatLong(45.9, 7.6), LatLong(45.908, 7.605), LatLong(45.91, 7.61)),
          elevation = listOf(0.0, 10.0, 20.0, 30.0),
          distance = 13.543077559212616,
          elevationGain = 68.0,
          estimatedTime = 169.3169307105514,
          difficulty = HikeDifficulty.DIFFICULT,
      )

  /** @param hike The hike to display on the screen. For test purposes, should always be saved. */
  @OptIn(ExperimentalTestApi::class)
  private suspend fun setupCompleteScreenWithSelected(
      hike: DetailedHike,
      waypointsRetrievalSucceeds: Boolean = true,
      elevationRetrievalSucceeds: Boolean = true,
      alreadyLoadData: Boolean = true,
      openTheBottomSheet: Boolean = true
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
          facilitiesViewModel = facilitiesViewModel)
    }

    if (openTheBottomSheet) {
      // Open the bottom sheet
      composeTestRule.waitUntilExactlyOneExists(
          hasTestTag(RunHikeScreen.TEST_TAG_BOTTOM_SHEET), timeoutMillis = 10000)

      composeTestRule.onNodeWithTag(RunHikeScreen.TEST_TAG_BOTTOM_SHEET).performTouchInput {
        down(1, position = Offset(centerX, centerY))
        moveTo(1, position = Offset(centerX, 100f))
        up(1)
      }
    }
  }

  @OptIn(ExperimentalPermissionsApi::class)
  @Before
  fun setUp() {
    mockNavigationActions = mock(NavigationActions::class.java)
    savedHikesRepository = mock(SavedHikesRepository::class.java)
    hikesRepository = mock(HikeRoutesRepository::class.java)
    elevationRepository = mock(ElevationRepository::class.java)

    facilitiesRepository = mock(FacilitiesRepository::class.java)
    facilitiesViewModel = FacilitiesViewModel(facilitiesRepository)

    // By default, the user has location permission
    mockkObject(LocationUtils)
    every { LocationUtils.hasLocationPermission(any()) } returns true
  }

  @Test
  fun runHikeScreen_displaysError_whenWaypointsRetrievalFails() =
      runTest(timeout = 5.seconds) {
        setupCompleteScreenWithSelected(
            detailedHike, waypointsRetrievalSucceeds = false, openTheBottomSheet = false)

        // So far, the waypoints retrieval should have happened once
        verify(hikesRepository, times(1)).getRoutesByIds(any(), any(), any())

        // An error message should be displayed to the user, along with a go back action
        composeTestRule.onNodeWithTag(CenteredErrorAction.TEST_TAG_CENTERED_ERROR_MESSAGE)
        composeTestRule
            .onNodeWithTag(CenteredErrorAction.TEST_TAG_CENTERED_ERROR_BUTTON)
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()

        // Clicking the button should trigger going back with the navigation
        verify(mockNavigationActions, times(1)).goBack()
      }

  @Test
  fun runHikeScreen_displaysError_whenElevationRetrievalFails() =
      runTest(timeout = 5.seconds) {
        setupCompleteScreenWithSelected(
            detailedHike, elevationRetrievalSucceeds = false, openTheBottomSheet = false)

        // So far, the elevation retrieval should have happened once
        verify(elevationRepository, times(1)).getElevation(any(), any(), any())

        // An error message should be displayed to the user, along with a retry action
        composeTestRule.onNodeWithTag(CenteredErrorAction.TEST_TAG_CENTERED_ERROR_MESSAGE)
        composeTestRule
            .onNodeWithTag(CenteredErrorAction.TEST_TAG_CENTERED_ERROR_BUTTON)
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()

        // Clicking the button should trigger a retry of the elevation retrieval
        verify(elevationRepository, times(2)).getElevation(any(), any(), any())
      }

  @OptIn(ExperimentalTestApi::class)
  @Test
  fun runHikeScreen_loadsMissingData() =
      runTest(timeout = 5.seconds) {
        setupCompleteScreenWithSelected(detailedHike, alreadyLoadData = false)

        composeTestRule.waitUntilExactlyOneExists(
            hasTestTag(RunHikeScreen.TEST_TAG_MAP), timeoutMillis = 10000)

        verify(hikesRepository).getRoutesByIds(any(), any(), any())
        verify(elevationRepository).getElevation(any(), any(), any())
      }

  @Test
  fun runHikeScreen_displaysMap() =
      runTest(timeout = 5.seconds) {
        setupCompleteScreenWithSelected(detailedHike)
        composeTestRule.onNodeWithTag(RunHikeScreen.TEST_TAG_MAP).assertIsDisplayed()
      }

  @Test
  fun runHikeScreen_displaysBackButton() =
      runTest(timeout = 5.seconds) {
        setupCompleteScreenWithSelected(detailedHike)
        composeTestRule.onNodeWithTag(RunHikeScreen.TEST_TAG_BACK_BUTTON).assertIsDisplayed()
      }

  @Test
  fun runHikeScreen_displaysZoomButtons() =
      runTest(timeout = 5.seconds) {
        setupCompleteScreenWithSelected(detailedHike)
        composeTestRule.onNodeWithTag(RunHikeScreen.TEST_TAG_ZOOM_BUTTONS).assertIsDisplayed()
      }

  @Test
  fun runHikeScreen_displaysBottomSheet() =
      runTest(timeout = 5.seconds) {
        setupCompleteScreenWithSelected(detailedHike)
        composeTestRule.onNodeWithTag(RunHikeScreen.TEST_TAG_BOTTOM_SHEET).assertExists()
      }

  /** Test all details which do not change during the hike run. */
  @Test
  fun runHikeScreen_displaysStaticDetails() =
      runTest(timeout = 5.seconds) {
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
  fun runHikeScreen_displaysElevationGraph() =
      runTest(timeout = 5.seconds) {
        setupCompleteScreenWithSelected(detailedHike)

        composeTestRule.onNodeWithTag(RunHikeScreen.TEST_TAG_ELEVATION_GRAPH).assertIsDisplayed()
      }

  @Test
  fun runHikeScreen_displaysStopHikeButton() =
      runTest(timeout = 5.seconds) {
        setupCompleteScreenWithSelected(detailedHike)

        composeTestRule.onNodeWithTag(RunHikeScreen.TEST_TAG_STOP_HIKE_BUTTON).assertIsDisplayed()
      }

  @Test
  fun runHikeScreen_stopHikeButton_navigatesBack() =
      runTest(timeout = 5.seconds) {
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
  fun runHikeScreen_displaysProgressIndicator() =
      runTest(timeout = 5.seconds) {
        setupCompleteScreenWithSelected(detailedHike)

        composeTestRule.onNodeWithTag(RunHikeScreen.TEST_TAG_PROGRESS_TEXT).assertIsDisplayed()
      }

  @Test
  fun runHikeScreen_fetchesFacilities() = runTest {
    val listFacility =
        listOf(
            Facility(
                type = FacilityType.TOILETS, // We'll test the toilets drawable
                coordinates = LatLong(45.9, 7.6)))
    `when`(facilitiesRepository.getFacilities(any(), any(), any())).then {
      val onSuccess = it.getArgument<(List<Facility>) -> Unit>(1)
      onSuccess(listFacility)
    }
    setupCompleteScreenWithSelected(detailedHike2)
    verify(facilitiesRepository).getFacilities(any(), any(), any())
  }

  private suspend fun setUpSelectedHike(hike: DetailedHike) {
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
    `when`(elevationRepository.getElevation(any(), any(), any())).thenAnswer {
      val onSuccess = it.getArgument<(List<Double>) -> Unit>(1)
      onSuccess(hike.elevation)
    }

    // Reset the view model
    hikesViewModel =
        HikesViewModel(
            savedHikesRepository, hikesRepository, elevationRepository, UnconfinedTestDispatcher())

    // Load the hike from OSM, as if the user had searched it on the map
    hikesViewModel.loadHikesInBounds(detailedHike.bounds.toBoundingBox())

    // Retrieve the hike's elevation data from the repository
    hikesViewModel.retrieveElevationDataFor(hikeId)

    // Compute the hike's details
    hikesViewModel.computeDetailsFor(hikeId)

    // Mark the hike as selected, to make sure it is the one displayed on the details screen
    hikesViewModel.selectHike(hikeId)
  }

  @Test
  fun runHikeScreen_clickingOnCenterButtonWithPermissionCentersMapOnLocation() =
      runTest(timeout = 5.seconds) {
        // Given
        mockkObject(MapUtils)
        every { MapUtils.centerMapOnLocation(any(), any(), any()) } returns Unit

        setupCompleteScreenWithSelected(detailedHike)

        // When
        composeTestRule.onNodeWithTag(RunHikeScreen.TEST_TAG_CENTER_MAP_BUTTON).performClick()

        // Then
        io.mockk.verify { MapUtils.centerMapOnLocation(any(), any(), any()) }
      }

  @OptIn(ExperimentalPermissionsApi::class)
  @Test
  fun runHikeScreen_permissionIsAskedOnScreenLoad() =
      runTest(timeout = 5.seconds) {
        // Given the user did not grant location permission to the app
        every { LocationUtils.hasLocationPermission(any()) } returns false

        setupCompleteScreenWithSelected(detailedHike)

        // Then an alert will be shown to ask for the permission
        composeTestRule
            .onNodeWithTag(LocationPermissionAlertDialog.TEST_TAG_LOCATION_PERMISSION_ALERT)
            .assertIsDisplayed()
      }

  @OptIn(ExperimentalPermissionsApi::class)
  @Test
  fun runHikeScreen_goesBackToPreviousScreenWhenRefusing() =
      runTest(timeout = 5.seconds) {
        // Given the user did not grant location permission to the app
        every { LocationUtils.hasLocationPermission(any()) } returns false

        setupCompleteScreenWithSelected(detailedHike)

        // Then an alert will be shown to ask for the permission
        composeTestRule
            .onNodeWithTag(LocationPermissionAlertDialog.TEST_TAG_LOCATION_PERMISSION_ALERT)
            .assertIsDisplayed()

        // When the user clicks on the "No thanks" button
        composeTestRule
            .onNodeWithTag(LocationPermissionAlertDialog.TEST_TAG_NO_THANKS_ALERT_BUTTON)
            .performClick()

        composeTestRule.waitForIdle()

        verify(mockNavigationActions).goBack()
      }

  @OptIn(ExperimentalPermissionsApi::class)
  @Test
  fun runHikeScreen_locationIsUpdated() =
      runTest(timeout = 5.seconds) {
        val userLocation =
            Location("").let {
              it.latitude = 45.9
              it.longitude = 7.6
              it
            }

        val marker = mockk<Marker>()
        every { marker.position } returns GeoPoint(userLocation.latitude, userLocation.longitude)

        // Given the user did not grant location permission to the app
        mockkObject(MapUtils)
        every {
          LocationUtils.onLocationPermissionsUpdated(any(), any(), any(), any(), any(), any())
        } answers
            {
              val locationCallback = it.invocation.args[3] as LocationCallback
              locationCallback.onLocationResult(LocationResult.create(listOf(userLocation)))
            }

        setupCompleteScreenWithSelected(detailedHike)

        io.mockk.verify {
          MapUtils.centerMapOnLocation(
              any(),
              any(),
              match {
                it.position.latitude == userLocation.latitude &&
                    it.position.longitude == userLocation.longitude
              })
        }
      }
}
