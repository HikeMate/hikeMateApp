package ch.hikemate.app.ui.map

import android.content.Context
import android.view.View
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.ComposeTestRule
import android.graphics.drawable.Drawable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import ch.hikemate.app.R
import ch.hikemate.app.model.authentication.AuthRepository
import ch.hikemate.app.model.authentication.AuthViewModel
import ch.hikemate.app.model.elevation.ElevationService
import ch.hikemate.app.model.facilities.FacilitiesRepository
import ch.hikemate.app.model.facilities.FacilitiesViewModel
import ch.hikemate.app.model.facilities.Facility
import ch.hikemate.app.model.facilities.FacilityType
import ch.hikemate.app.model.profile.HikingLevel
import ch.hikemate.app.model.profile.Profile
import ch.hikemate.app.model.profile.ProfileRepository
import ch.hikemate.app.model.profile.ProfileViewModel
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
import ch.hikemate.app.ui.components.BackButton.BACK_BUTTON_TEST_TAG
import ch.hikemate.app.ui.components.DetailRow
import ch.hikemate.app.ui.map.HikeDetailScreen.TEST_TAG_ADD_DATE_BUTTON
import ch.hikemate.app.ui.map.HikeDetailScreen.TEST_TAG_BOOKMARK_ICON
import ch.hikemate.app.ui.map.HikeDetailScreen.TEST_TAG_DATE_PICKER
import ch.hikemate.app.ui.map.HikeDetailScreen.TEST_TAG_DATE_PICKER_CANCEL_BUTTON
import ch.hikemate.app.ui.map.HikeDetailScreen.TEST_TAG_DATE_PICKER_CONFIRM_BUTTON
import ch.hikemate.app.ui.map.HikeDetailScreen.TEST_TAG_ELEVATION_GRAPH
import ch.hikemate.app.ui.map.HikeDetailScreen.TEST_TAG_HIKE_NAME
import ch.hikemate.app.ui.map.HikeDetailScreen.TEST_TAG_MAP
import ch.hikemate.app.ui.map.HikeDetailScreen.TEST_TAG_PLANNED_DATE_TEXT_BOX
import ch.hikemate.app.ui.map.HikeDetailScreen.TEST_TAG_RUN_HIKE_BUTTON
import ch.hikemate.app.ui.navigation.NavigationActions
import com.google.firebase.Timestamp
import io.mockk.every
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import okhttp3.Dispatcher
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.capture
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.osmdroid.events.ZoomEvent
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class HikeDetailScreenTest {
  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var mockNavigationActions: NavigationActions
  private lateinit var authRepository: AuthRepository
  private lateinit var authViewModel: AuthViewModel
  private lateinit var profileRepository: ProfileRepository
  private lateinit var profileViewModel: ProfileViewModel
  private lateinit var mockSavedHikesRepository: SavedHikesRepository
  private lateinit var hikesRepository: HikeRoutesRepository
  private lateinit var elevationService: ElevationService
  private lateinit var hikesViewModel: HikesViewModel
  private lateinit var facilitiesRepository: FacilitiesRepository
  private lateinit var facilitiesViewModel: FacilitiesViewModel
  private lateinit var mapView:MapView
  private lateinit var drawable:Drawable
  private lateinit var context:Context
  private lateinit var testDispatcher:TestDispatcher


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

  private fun setUpCompleteScreen() {
    composeTestRule.setContent {
      HikeDetailScreen(
          hikesViewModel = hikesViewModel,
          profileViewModel = profileViewModel,
          authViewModel = authViewModel,
          navigationActions = mockNavigationActions,
          facilitiesViewModel = facilitiesViewModel)
    }
  }

  private fun setUpBottomSheetScaffold(
      hike: DetailedHike = detailedHike,
      onRunThisHike: () -> Unit = {}
  ) {
    composeTestRule.setContent {
      HikesDetailsBottomScaffold(
          detailedHike = hike,
          hikesViewModel = hikesViewModel,
          userHikingLevel = HikingLevel.BEGINNER,
          onRunThisHike = onRunThisHike)
    }
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
    `when`(mockSavedHikesRepository.loadSavedHikes())
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
            mockSavedHikesRepository, hikesRepository, elevationService, UnconfinedTestDispatcher())

    // Load the hike from OSM, as if the user had searched it on the map
    hikesViewModel.loadHikesInBounds(detailedHike.bounds.toBoundingBox())

    // Retrieve the hike's elevation data from the repository
    hikesViewModel.retrieveElevationDataFor(hikeId)

    // Compute the hike's details
    hikesViewModel.computeDetailsFor(hikeId)

    // Mark the hike as selected, to make sure it is the one displayed on the details screen
    hikesViewModel.selectHike(hikeId)
  }

  private val profile =
      Profile(
          id = "1",
          name = "John Doe",
          email = "john-doe@gmail.com",
          hikingLevel = HikingLevel.INTERMEDIATE,
          joinedDate = Timestamp.now())

  @Before
  fun setUp() {
    mockNavigationActions = mock(NavigationActions::class.java)
    profileRepository = mock(ProfileRepository::class.java)
    profileViewModel = ProfileViewModel(profileRepository)
    authRepository = mock(AuthRepository::class.java)
    authViewModel = AuthViewModel(authRepository, profileRepository)
    hikesRepository = mock(HikeRoutesRepository::class.java)
    elevationService = mock(ElevationService::class.java)
    mockSavedHikesRepository = mock(SavedHikesRepository::class.java)
    facilitiesRepository = mock(FacilitiesRepository::class.java)
    facilitiesViewModel = FacilitiesViewModel(facilitiesRepository)
      mapView=mock(MapView::class.java)
      drawable=mock(Drawable::class.java)
      context=mock(Context::class.java)
      testDispatcher = UnconfinedTestDispatcher()
      Dispatchers.setMain(testDispatcher)


    `when`(profileRepository.getProfileById(eq(profile.id), any(), any())).thenAnswer {
      val onSuccess = it.getArgument<(Profile) -> Unit>(1)
      onSuccess(profile)
    }
    profileViewModel.getProfileById(profile.id)
  }

  @Test
  fun hikeDetailScreen_displaysMap() = runTest {
    setUpSelectedHike(detailedHike)
    setUpCompleteScreen()

    composeTestRule.onNodeWithTag(TEST_TAG_MAP).assertIsDisplayed()
  }

  @Test
  fun hikeDetails_displaysHikeNameAndBookmarkIcon() = runTest {
    setUpSelectedHike(detailedHike)
    setUpBottomSheetScaffold()

    composeTestRule.onNodeWithTag(TEST_TAG_HIKE_NAME).assertTextEquals(detailedHike.name!!)
    composeTestRule.onNodeWithTag(TEST_TAG_BOOKMARK_ICON).assertIsDisplayed()
  }

  @Test
  fun hikeDetails_showsElevationGraph() = runTest {
    setUpSelectedHike(detailedHike)
    setUpBottomSheetScaffold()

    composeTestRule.onNodeWithTag(TEST_TAG_ELEVATION_GRAPH).assertIsDisplayed()
  }

  @Test
  fun hikeDetails_showsPlannedDate_whenDateIsSet() = runTest {
    val plannedDate = Timestamp(1622563200, 0)
    val hike = detailedHike.copy(isSaved = true, plannedDate = plannedDate)
    setUpSelectedHike(hike)

    // Display only the bottom part without the map
    setUpBottomSheetScaffold(hike)

    composeTestRule.onNodeWithTag(TEST_TAG_PLANNED_DATE_TEXT_BOX).assertIsDisplayed()
  }

  @Test
  fun hikeDetails_showsCorrectDetailsRowsWhenNotSaved() = runTest {
    val hike = detailedHike.copy(isSaved = false, plannedDate = null)
    setUpSelectedHike(hike)

    // Display only the bottom part without the map
    setUpBottomSheetScaffold(hike)

    composeTestRule.onAllNodesWithTag(DetailRow.TEST_TAG_DETAIL_ROW_TAG).assertCountEquals(5)
    composeTestRule.onAllNodesWithTag(DetailRow.TEST_TAG_DETAIL_ROW_VALUE).assertCountEquals(5)
  }

  @Test
  fun hikeDetails_showsCorrectDetailsRowsWhenSavedAndNoDateIsSet() = runTest {
    val hike = detailedHike.copy(isSaved = true, plannedDate = null)
    setUpSelectedHike(hike)

    // Display only the bottom part without the map
    setUpBottomSheetScaffold(hike)

    composeTestRule.onAllNodesWithTag(DetailRow.TEST_TAG_DETAIL_ROW_TAG).assertCountEquals(6)
    composeTestRule.onAllNodesWithTag(DetailRow.TEST_TAG_DETAIL_ROW_VALUE).assertCountEquals(5)
    composeTestRule.onNodeWithTag(TEST_TAG_ADD_DATE_BUTTON).assertIsDisplayed()
  }

  @Test
  fun hikeDetails_showsCorrectDetailsRowsWhenSavedAndDateIsSet() = runTest {
    val plannedDate = Timestamp(1622563200, 0)
    val hike = detailedHike.copy(isSaved = true, plannedDate = plannedDate)
    setUpSelectedHike(hike)

    // Display only the bottom part without the map
    setUpBottomSheetScaffold(hike)

    composeTestRule.onAllNodesWithTag(DetailRow.TEST_TAG_DETAIL_ROW_TAG).assertCountEquals(6)
    composeTestRule.onAllNodesWithTag(DetailRow.TEST_TAG_DETAIL_ROW_VALUE).assertCountEquals(5)
    composeTestRule.onNodeWithTag(TEST_TAG_PLANNED_DATE_TEXT_BOX).assertIsDisplayed()
  }

  @Test
  fun hikeDetailScreen_navigatesBackOnBackButtonClick() = runTest {
    setUpSelectedHike(detailedHike)

    // Display the whole details screen for the hike, including the map
    setUpCompleteScreen()

    doNothing().`when`(mockNavigationActions).goBack()

    composeTestRule
        .onNodeWithTag(BACK_BUTTON_TEST_TAG)
        .assertIsDisplayed()
        .assertHasClickAction()
        .performClick()

    composeTestRule.waitForIdle()

    assertNull(hikesViewModel.selectedHike.value)
    verify(mockNavigationActions).goBack()
  }

  @Test
  fun hikeDetails_opensDatePicker_whenAddDateButtonClicked() = runTest {
    val hike = detailedHike.copy(isSaved = true, plannedDate = null)
    setUpSelectedHike(hike)

    // Display only the bottom part without the map
    setUpBottomSheetScaffold(hike)

    composeTestRule
        .onNodeWithTag(TEST_TAG_ADD_DATE_BUTTON)
        .assertIsDisplayed()
        .assertHasClickAction()
        .performClick()
    composeTestRule.onNodeWithTag(TEST_TAG_DATE_PICKER).assertIsDisplayed()
  }

  @Test
  fun hikeDetails_datePickerDismisses_whenClickedOnCancel() = runTest {
    val hike = detailedHike.copy(isSaved = true, plannedDate = null)
    setUpSelectedHike(hike)

    // Display only the bottom part without the map
    setUpBottomSheetScaffold(hike)

    composeTestRule
        .onNodeWithTag(TEST_TAG_ADD_DATE_BUTTON)
        .assertIsDisplayed()
        .assertHasClickAction()
        .performClick()
    composeTestRule.onNodeWithTag(TEST_TAG_DATE_PICKER).assertIsDisplayed()

    composeTestRule.onNodeWithTag(TEST_TAG_DATE_PICKER_CANCEL_BUTTON).performClick()

    composeTestRule.onNodeWithTag(TEST_TAG_DATE_PICKER).assertIsNotDisplayed()
  }

  @Test
  fun hikeDetails_datePickerDismisses_whenClickedOnConfirm() = runTest {
    val hike = detailedHike.copy(isSaved = true, plannedDate = null)
    setUpSelectedHike(hike)

    // Display only the bottom part without the map
    setUpBottomSheetScaffold(hike)

    composeTestRule.onNodeWithTag(TEST_TAG_ADD_DATE_BUTTON).assertHasClickAction().performClick()
    composeTestRule.onNodeWithTag(TEST_TAG_DATE_PICKER).assertIsDisplayed()

    composeTestRule.onNodeWithTag(TEST_TAG_DATE_PICKER_CONFIRM_BUTTON).performClick()

    composeTestRule.onNodeWithTag(TEST_TAG_DATE_PICKER).assertIsNotDisplayed()
  }

  @Test
  fun hikeDetails_showsCorrectDetailedHikesValues() = runTest {
    setUpSelectedHike(detailedHike)

    // Display only the bottom part without the map
    setUpBottomSheetScaffold(detailedHike)

    val distanceString = String.format(Locale.getDefault(), "%.2f", detailedHike.distance)
    val elevationGainString = detailedHike.elevationGain.roundToInt().toString()
    val hourString =
        String.format(Locale.getDefault(), "%02d", (detailedHike.estimatedTime / 60).toInt())
    val minuteString =
        String.format(Locale.getDefault(), "%02d", (detailedHike.estimatedTime % 60).roundToInt())

    composeTestRule
        .onAllNodesWithTag(DetailRow.TEST_TAG_DETAIL_ROW_VALUE)
        .assertAny(hasText("${distanceString}km"))
    composeTestRule
        .onAllNodesWithTag(DetailRow.TEST_TAG_DETAIL_ROW_VALUE)
        .assertAny(hasText("${elevationGainString}m"))
    composeTestRule
        .onAllNodesWithTag(DetailRow.TEST_TAG_DETAIL_ROW_VALUE)
        .assertAny(hasText("${hourString}h${minuteString}"))
    composeTestRule
        .onAllNodesWithTag(DetailRow.TEST_TAG_DETAIL_ROW_VALUE)
        .assertAny(
            hasText(
                ApplicationProvider.getApplicationContext<Context>()
                    .getString(detailedHike.difficulty.nameResourceId)))
  }

  @Test
  fun hikeDetails_showsCorrectDetailedHikesValues_whenTimeIsLessThan60Min() = runTest {
    val hike = detailedHike.copy(estimatedTime = 45.0) // test hike that takes just 45 min
    setUpSelectedHike(hike)

    // Display only the bottom part without the map
    setUpBottomSheetScaffold(hike)

    val minuteString =
        String.format(Locale.getDefault(), "%02d", (hike.estimatedTime % 60).roundToInt())

    composeTestRule
        .onAllNodesWithTag(DetailRow.TEST_TAG_DETAIL_ROW_VALUE)
        .assertAny(hasText("${minuteString}min"))
  }

  @Test
  fun hikeDetails_showsRunThisHikeButton_andTriggersOnRunThisHike() = runTest {
    val onRunThisHike = mock<() -> Unit>()
    setUpSelectedHike(detailedHike)

    setUpBottomSheetScaffold(onRunThisHike = onRunThisHike)

    composeTestRule
        .onNodeWithTag(TEST_TAG_RUN_HIKE_BUTTON)
        .assertIsDisplayed()
        .assertHasClickAction()
        .performClick()

    verify(onRunThisHike).invoke()
  }

  @Test
  fun hikeDetails_loadsFacilitiesOnInit() = runTest {
    val bounds = detailedHike.bounds
    val boundsWithMargin =
        Bounds(
            bounds.minLat - HikeDetailScreen.MARGIN_BOUNDS,
            bounds.minLon - HikeDetailScreen.MARGIN_BOUNDS,
            bounds.maxLat + HikeDetailScreen.MARGIN_BOUNDS,
            bounds.maxLon + HikeDetailScreen.MARGIN_BOUNDS)

    `when`(facilitiesRepository.getFacilities(eq(boundsWithMargin), any(), any())).thenAnswer {
      val onSuccess = it.getArgument<(List<Facility>) -> Unit>(1)
      onSuccess(listOf(mockFacility))
    }

    setUpSelectedHike(detailedHike)
    setUpCompleteScreen()

    verify(facilitiesRepository).getFacilities(eq(boundsWithMargin), any(), any())

  }

    @Test
    fun hikeDetails_drawsFacilitiesOnMapWhenZoomSufficient() = runTest {
        // Setup facilities
        val testFacility = Facility(
            type = FacilityType.BENCH,
            coordinates = LatLong(45.9, 7.6),
        )

        // Mock facilities repository response
        `when`(facilitiesRepository.getFacilities(any(), any(), any())).thenAnswer {
            val onSuccess = it.getArgument<(List<Facility>) -> Unit>(1)
            onSuccess(listOf(testFacility))
        }

        setUpSelectedHike(detailedHike)
        setUpCompleteScreen()

        var mapView: MapView? = context.

        composeTestRule.setContent {
            AndroidView(
                factory = { context ->
                    MapView(context).also { mapView = it }
                },
                modifier = Modifier.testTag(TEST_TAG_MAP)
            )
        }

        composeTestRule.waitForIdle()
        assertNotNull(mapView)

        // Now you can test the overlays
        assertTrue(
            mapView!!.overlays.any { overlay ->
                overlay is Marker &&
                        overlay.relatedObject == R.string.facility_marker &&
                        overlay.position.latitude == testFacility.coordinates.lat &&
                        overlay.position.longitude == testFacility.coordinates.lon
            }
        )
    }

    @Test
    fun hikeDetails_removesFacilitiesWhenZoomInsufficent() = runTest {
        val testFacility = Facility(
            type = FacilityType.BENCH,
            coordinates = LatLong(45.9, 7.6),
        )

        `when`(facilitiesRepository.getFacilities(any(), any(), any())).thenAnswer {
            val onSuccess = it.getArgument<(List<Facility>) -> Unit>(1)
            onSuccess(listOf(testFacility))
        }

        setUpSelectedHike(detailedHike)
        setUpCompleteScreen()

        val mapView = composeTestRule.onNodeWithTag(TEST_TAG_MAP)
            .fetchSemanticsNode().layoutInfo.getModifierInfo()
            .find { it.modifier is AndroidView }?.let {
                (it.modifier as AndroidView).view as MapView
            }

        assertNotNull(mapView)

        // Simulate zoom out below threshold
        mapView!!.controller.setZoom(HikeDetailScreen.MIN_ZOOM_FOR_FACILITIES - 1.0)

        // Verify facilities are removed
        assertFalse(
            mapView.overlays.any { overlay ->
                overlay is Marker && overlay.relatedObject == R.string.facility_marker
            }
        )
    }

    @Test
    fun hikeDetails_drawsOnlyFacilitiesWithinBounds() = runTest {
        // Create facilities inside and outside bounds
        val facilityInBounds = Facility(
            type = FacilityType.BENCH,
            coordinates = LatLong(45.95, 7.65), // Inside detailedHike bounds
        )

        val facilityOutOfBounds = Facility(
            type = FacilityType.BENCH,
            coordinates = LatLong(47.0, 8.0), // Outside detailedHike bounds
        )

        `when`(facilitiesRepository.getFacilities(any(), any(), any())).thenAnswer {
            val onSuccess = it.getArgument<(List<Facility>) -> Unit>(1)
            onSuccess(listOf(facilityInBounds, facilityOutOfBounds))
        }

        setUpSelectedHike(detailedHike)
        setUpCompleteScreen()

        val mapView = composeTestRule.onNodeWithTag(TEST_TAG_MAP)
            .fetchSemanticsNode().layoutInfo.getModifierInfo()
            .find { it.modifier is AndroidView }?.let {
                (it.modifier as AndroidView).view as MapView
            }

        assertNotNull(mapView)

        // Verify only in-bounds facility is shown
        val facilityMarkers = mapView!!.overlays.filterIsInstance<Marker>()
            .filter { it.relatedObject == R.string.facility_marker }

        assertEquals(1, facilityMarkers.size)
        assertEquals(facilityInBounds.coordinates.lat, facilityMarkers[0].position.latitude, 0.0001)
        assertEquals(facilityInBounds.coordinates.lon, facilityMarkers[0].position.longitude, 0.0001)
    }

    @Test
    fun hikeDetails_updatesDisplayedFacilitiesOnMapScroll() = runTest {
        val initialFacility = Facility(
            type = FacilityType.BENCH,
            coordinates = LatLong(45.95, 7.65),
        )

        var facilitiesList = listOf(initialFacility)

        `when`(facilitiesRepository.getFacilities(any(), any(), any())).thenAnswer {
            val onSuccess = it.getArgument<(List<Facility>) -> Unit>(1)
            onSuccess(facilitiesList)
        }

        setUpSelectedHike(detailedHike)
        setUpCompleteScreen()
        var mapView: MapView? = null

        composeTestRule.setContent {
            AndroidView(
                factory = { context ->
                    MapView(context).also { mapView = it }
                },
                modifier = Modifier.testTag(TEST_TAG_MAP)
            )
        }

        composeTestRule.waitForIdle()
        assertNotNull(mapView)

        // Now you can test the overlays
        assertTrue(
            mapView!!.overlays.any { overlay ->
                overlay is Marker &&
                        overlay.relatedObject == R.string.facility_marker &&
                        overlay.position.latitude == testFacility.coordinates.lat &&
                        overlay.position.longitude == testFacility.coordinates.lon
            }
        )
        // Update facilities list for next query
        val newFacility = Facility(
            type = FacilityType.BENCH,
            coordinates = LatLong(46.0, 7.7),
        )
        facilitiesList = listOf(newFacility)

        // Simulate scroll
        mapView.controller.setCenter(GeoPoint(46.0, 7.7))

        // Wait for debounce
        advanceTimeBy(HikeDetailScreen.DEBOUNCE_DURATION + 100)

        // Verify new facility is shown
        assertTrue(
            mapView.overlays.any { overlay ->
                overlay is Marker &&
                        overlay.relatedObject == R.string.facility_marker &&
                        overlay.position.latitude == newFacility.coordinates.lat
            }
        )
    }
  // Add to setUp():
  private val mockFacility =
      Facility(
          type = FacilityType.BENCH,
          coordinates = LatLong(45.9, 7.6),
      )
    @After
    fun tearDown(){
        Dispatchers.resetMain()
    }
}
