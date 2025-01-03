package ch.hikemate.app.ui.map

import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import ch.hikemate.app.R
import ch.hikemate.app.model.authentication.AuthRepository
import ch.hikemate.app.model.authentication.AuthViewModel
import ch.hikemate.app.model.elevation.ElevationRepository
import ch.hikemate.app.model.profile.HikingLevel
import ch.hikemate.app.model.profile.Profile
import ch.hikemate.app.model.profile.ProfileRepository
import ch.hikemate.app.model.profile.ProfileViewModel
import ch.hikemate.app.model.route.Bounds
import ch.hikemate.app.model.route.HikeRoute
import ch.hikemate.app.model.route.HikeRoutesRepository
import ch.hikemate.app.model.route.HikesViewModel
import ch.hikemate.app.model.route.LatLong
import ch.hikemate.app.model.route.saved.SavedHikesRepository
import ch.hikemate.app.ui.components.CenteredErrorAction
import ch.hikemate.app.ui.components.HikeCard
import ch.hikemate.app.ui.components.LocationPermissionAlertDialog
import ch.hikemate.app.ui.navigation.NavigationActions
import ch.hikemate.app.ui.navigation.Route
import ch.hikemate.app.ui.navigation.TEST_TAG_BOTTOM_BAR
import ch.hikemate.app.utils.LocationUtils
import ch.hikemate.app.utils.MapUtils
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.firebase.Timestamp
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.spyk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.osmdroid.util.BoundingBox

class MapScreenTest : TestCase() {
  private lateinit var savedHikesRepository: SavedHikesRepository
  private lateinit var hikesRepository: HikeRoutesRepository
  private lateinit var elevationRepository: ElevationRepository
  private lateinit var hikesViewModel: HikesViewModel
  private lateinit var navigationActions: NavigationActions
  private lateinit var authRepository: AuthRepository
  private lateinit var authViewModel: AuthViewModel
  private lateinit var profileRepository: ProfileRepository
  private lateinit var context: Context
  private lateinit var profileViewModel: ProfileViewModel

  @get:Rule val composeTestRule = createComposeRule()

  private val profile =
      Profile(
          id = "1",
          name = "John Doe",
          email = "john-doe@gmail.com",
          hikingLevel = HikingLevel.AMATEUR,
          joinedDate = Timestamp.now())

  private fun setUpMap(
      mapMinZoomLevel: Double = MapScreen.MAP_MIN_ZOOM,
      mapInitialZoomLevel: Double = MapScreen.MAP_INITIAL_ZOOM,
      hikesViewModel: HikesViewModel = this.hikesViewModel,
  ) {
    composeTestRule.setContent {
      context = LocalContext.current
      MapScreen(
          hikesViewModel = hikesViewModel,
          navigationActions = navigationActions,
          authViewModel = authViewModel,
          profileViewModel = profileViewModel,
          mapInitialValues =
              MapInitialValues(
                  mapMinZoomLevel = mapMinZoomLevel, mapInitialZoomLevel = mapInitialZoomLevel))
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Before
  fun setUp() {
    // In those tests we tell the view model to use the UnconfinedTestDispatcher but we do
    // not set it as the default dispatcher, otherwise it blocks.
    // My hypothesis is that OSMDroid, the library used to display the map view, uses coroutines
    // as well and has some infinite loop somewhere. Using a coroutine, there's no problem, but if
    // it runs on the single existing thread, everything will be blocked.
    // Hence, do not set the main dispatcher, only the view model's one.

    navigationActions = mock(NavigationActions::class.java)
    savedHikesRepository = mock(SavedHikesRepository::class.java)
    hikesRepository = mock(HikeRoutesRepository::class.java)
    elevationRepository = mock(ElevationRepository::class.java)
    profileRepository = mock(ProfileRepository::class.java)
    profileViewModel = ProfileViewModel(profileRepository)
    authRepository = mock(AuthRepository::class.java)
    authViewModel = AuthViewModel(authRepository, profileRepository)
    hikesViewModel =
        HikesViewModel(
            savedHikesRepo = savedHikesRepository,
            osmHikesRepo = hikesRepository,
            elevationRepository = elevationRepository,
            UnconfinedTestDispatcher())

    `when`(profileRepository.getProfileById(eq(profile.id), any(), any())).thenAnswer {
      val onSuccess = it.getArgument<(Profile) -> Unit>(1)
      onSuccess(profile)
    }
    profileViewModel.getProfileById(profile.id)
  }

  @Test
  fun mapIsDisplayed() {
    setUpMap()
    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_MAP).assertIsDisplayed()
  }

  @Test
  fun hikesListIsDisplayed() {
    setUpMap()
    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_HIKES_LIST).assertIsDisplayed()
  }

  @Test
  fun menuIsDisplayed() {
    setUpMap()
    composeTestRule.onNodeWithTag(TEST_TAG_BOTTOM_BAR).assertIsDisplayed()
  }

  @Test
  fun searchButtonIsDisplayed() {
    setUpMap()
    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_SEARCH_BUTTON).assertIsDisplayed()
  }

  @Test
  fun searchButtonCallsRepositoryWhenClicked() {
    setUpMap()
    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_SEARCH_BUTTON).performClick()
    verify(hikesRepository, times(1)).getRoutes(any(), any(), any())
  }

  @Test
  fun searchButtonIsNotEnabledWhenClicked() {
    setUpMap()
    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_SEARCH_BUTTON).assertIsEnabled()
    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_SEARCH_BUTTON).performClick()
    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_SEARCH_BUTTON).assertIsNotEnabled()
  }

  @Test
  fun searchButtonIsEnabledWhenSearchSucceeded() {
    setUpMap()
    `when`(hikesRepository.getRoutes(any(), any(), any())).thenAnswer {
      val onSuccess = it.getArgument<(List<HikeRoute>) -> Unit>(1)
      onSuccess(listOf())
    }
    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_SEARCH_BUTTON).performClick()
    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_SEARCH_BUTTON).assertIsEnabled()
  }

  @Test
  fun searchButtonIsEnabledWhenSearchFailed() {
    setUpMap()
    `when`(hikesRepository.getRoutes(any(), any(), any())).thenAnswer {
      val onFailure = it.getArgument<(Exception) -> Unit>(2)
      onFailure(Exception("Test exception"))
    }
    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_SEARCH_BUTTON).performClick()
    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_SEARCH_BUTTON).assertIsEnabled()
  }

  @Test
  fun errorMessageDisplayedWhenSearchFails() {
    setUpMap()

    // Ensure a search will fail
    `when`(hikesRepository.getRoutes(any(), any(), any())).thenAnswer {
      val onFailure = it.getArgument<(Exception) -> Unit>(2)
      onFailure(Exception("Test exception"))
    }

    // Perform a search
    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_SEARCH_BUTTON).performClick()

    // Check that the error message is displayed
    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_LOADING_ERROR_MESSAGE).assertIsDisplayed()
  }

  @Test
  fun clickingSearchDisplaysSearchingMessageAndClearsList() {
    setUpMap()
    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_SEARCHING_MESSAGE).assertIsNotDisplayed()
    composeTestRule
        .onNodeWithTag(MapScreen.TEST_TAG_SEARCH_LOADING_ANIMATION)
        .assertIsNotDisplayed()

    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_SEARCH_BUTTON).performClick()

    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_SEARCHING_MESSAGE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_SEARCH_LOADING_ANIMATION).assertIsDisplayed()
  }

  @Test
  fun clickingOnHikeItemSelectsHikeRoute() {
    setUpMap()
    val hikeRoute = HikeRoute("Route 1", Bounds(0.0, 0.0, 0.0, 0.0), listOf(LatLong(1.0, 2.0)))
    `when`(hikesRepository.getRoutes(any(), any(), any())).thenAnswer {
      val onSuccess = it.getArgument<(List<HikeRoute>) -> Unit>(1)
      onSuccess(listOf(hikeRoute))
    }
    // Have an initial list of routes with a single route to display on the map
    hikesViewModel.loadHikesInBounds(BoundingBox(0.0, 0.0, 0.0, 0.0))

    // Click on the route item in the list
    composeTestRule
        .onNodeWithTag(MapScreen.TEST_TAG_HIKE_ITEM, useUnmergedTree = true)
        .performClick()

    // Check that the selected route is the one that was clicked
    assertNotNull(hikesViewModel.selectedHike.value)
    assertEquals(hikeRoute.id, hikesViewModel.selectedHike.value?.id)
  }

  @Test
  fun emptyHikesListDisplaysEmptyMessage() {
    setUpMap()
    `when`(hikesRepository.getRoutes(any(), any(), any())).thenAnswer {
      val onSuccess = it.getArgument<(List<HikeRoute>) -> Unit>(1)
      onSuccess(listOf())
    }
    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_SEARCH_BUTTON).performClick()
    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_EMPTY_HIKES_LIST_MESSAGE).assertIsDisplayed()
  }

  @Test
  fun hikesListDisplaysHikeNameIfAvailable() {
    // Given
    val hikeName = "My test hike whatever"
    val hike =
        HikeRoute("1", Bounds(-1.0, -1.0, 1.0, 1.0), listOf(LatLong(1.0, 2.0)), hikeName, null)
    `when`(hikesRepository.getRoutes(any(), any(), any())).thenAnswer {
      val onSuccess = it.getArgument<(List<HikeRoute>) -> Unit>(1)
      onSuccess(listOf(hike))
    }

    // When
    setUpMap()
    hikesViewModel.loadHikesInBounds(BoundingBox(0.0, 0.0, 0.0, 0.0))

    // Then
    composeTestRule
        .onAllNodesWithTag(HikeCard.TEST_TAG_HIKE_CARD_TITLE, useUnmergedTree = true)
        .assertCountEquals(1)
    composeTestRule
        .onNodeWithTag(HikeCard.TEST_TAG_HIKE_CARD_TITLE, useUnmergedTree = true)
        .assertTextEquals(hikeName)
  }

  @Test
  fun hikesListDisplaysDefaultValueIfNameNotAvailable() {
    // Given
    val hike = HikeRoute("1", Bounds(-1.0, -1.0, 1.0, 1.0), listOf(LatLong(1.0, 2.0)), null, null)
    `when`(hikesRepository.getRoutes(any(), any(), any())).thenAnswer {
      val onSuccess = it.getArgument<(List<HikeRoute>) -> Unit>(1)
      onSuccess(listOf(hike))
    }
    val context = InstrumentationRegistry.getInstrumentation().targetContext

    // When
    setUpMap()
    hikesViewModel.loadHikesInBounds(BoundingBox(0.0, 0.0, 0.0, 0.0))

    // Then
    composeTestRule
        .onAllNodesWithTag(HikeCard.TEST_TAG_HIKE_CARD_TITLE, useUnmergedTree = true)
        .assertCountEquals(1)
    composeTestRule
        .onNodeWithTag(HikeCard.TEST_TAG_HIKE_CARD_TITLE, useUnmergedTree = true)
        .assertTextEquals(context.getString(R.string.map_screen_hike_title_default))
  }

  /**
   * Regression test for the app crashing when searching for hikes while zoomed out too far.
   *
   * See https://github.com/HikeMate/hikeMateApp/issues/78 for more details.
   */
  @Test
  fun zoomedOutMapDoesNotCrashWhenSearchingForHikes() {
    // Initialize the map as zoomed out very far
    setUpMap(mapMinZoomLevel = 1.0, mapInitialZoomLevel = 1.0)

    // Click on the search button
    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_SEARCH_BUTTON).performClick()

    // Check that the search did not crash the app
    // (the search will not return any results because the map is zoomed out too far)
    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_MAP).assertIsDisplayed()
  }

  @Test
  fun zoomingOutMakesTheSearchButtonDisabled() {
    setUpMap()
    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_SEARCH_BUTTON).assertIsEnabled()
    composeTestRule.onNodeWithTag(ZoomMapButton.ZOOM_OUT_BUTTON).performClick()
    composeTestRule.onNodeWithTag(ZoomMapButton.ZOOM_OUT_BUTTON).performClick()
    composeTestRule.onNodeWithTag(ZoomMapButton.ZOOM_OUT_BUTTON).performClick()
    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_SEARCH_BUTTON).assertIsNotEnabled()
  }

  @Test
  fun searchingHikesTakesAMinimalTime() = runTest {
    setUpMap()
    `when`(hikesRepository.getRoutes(any(), any(), any())).thenAnswer {
      val onSuccess = it.getArgument<(List<HikeRoute>) -> Unit>(1)
      onSuccess(emptyList())
    }

    val startTime = System.currentTimeMillis()
    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_SEARCH_BUTTON).performClick()
    while (composeTestRule
        .onNodeWithTag(MapScreen.TEST_TAG_SEARCH_LOADING_ANIMATION)
        .isDisplayed()) {
      // Wait for the search to finish
    }
    val endTime = System.currentTimeMillis()

    // Check that the search took at least the minimal time
    assert(endTime - startTime >= MapScreen.MINIMAL_SEARCH_TIME_IN_MS)
    // Check that the search took at most twice the minimal time (only because we mocked it, so it
    // should return quickly)
    assert(endTime - startTime < 2 * MapScreen.MINIMAL_SEARCH_TIME_IN_MS)
  }

  @OptIn(ExperimentalPermissionsApi::class)
  @Test
  fun clickingOnCenterButtonWithPermissionCentersMapOnLocation() {
    // Given
    setUpMap()
    mockkObject(LocationUtils)
    every { LocationUtils.hasLocationPermission(any()) } returns true
    mockkObject(MapUtils)
    every { MapUtils.centerMapOnLocation(any(), any(), any()) } returns Unit

    // When
    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_CENTER_MAP_BUTTON).performClick()

    // Then
    io.mockk.verify { MapUtils.centerMapOnLocation(any(), any(), any()) }
  }

  @OptIn(ExperimentalPermissionsApi::class)
  @Test
  fun clickingOnCenterButtonWithoutPermissionsAsksForThem() {
    // Given the user did not grant location permission to the app
    setUpMap()
    mockkObject(LocationUtils)
    every { LocationUtils.hasLocationPermission(any()) } returns false

    // When the user clicks on the "Center map on me" button
    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_CENTER_MAP_BUTTON).performClick()

    // Then an alert will be shown to ask for the permission
    composeTestRule
        .onNodeWithTag(LocationPermissionAlertDialog.TEST_TAG_LOCATION_PERMISSION_ALERT)
        .assertIsDisplayed()

    // When the user clicks on the "No thanks" button
    composeTestRule
        .onNodeWithTag(LocationPermissionAlertDialog.TEST_TAG_NO_THANKS_ALERT_BUTTON)
        .performClick()

    // Then the alert disappears
    composeTestRule
        .onNodeWithTag(LocationPermissionAlertDialog.TEST_TAG_LOCATION_PERMISSION_ALERT)
        .assertIsNotDisplayed()
  }

  @Test
  fun testSignOutAndNavigateToAuthFromMapScreen() {
    `when`(profileRepository.getProfileById(any(), any(), any())).thenAnswer {
      val onError = it.getArgument<(Exception) -> Unit>(2)
      onError(Exception("No profile found"))
    }
    `when`(authRepository.signOut(any())).thenAnswer {
      val onSuccess = it.getArgument<() -> Unit>(0)
      onSuccess()
    }

    profileViewModel.getProfileById(profile.id)

    setUpMap()

    composeTestRule
        .onNodeWithTag(CenteredErrorAction.TEST_TAG_CENTERED_ERROR_MESSAGE)
        .assertIsDisplayed()
        .assertTextEquals(context.getString(R.string.an_error_occurred_while_fetching_the_profile))
    composeTestRule.onNodeWithTag(CenteredErrorAction.TEST_TAG_CENTERED_ERROR_BUTTON).performClick()

    verify(authRepository).signOut(any())
    verify(navigationActions).navigateTo(Route.AUTH)
    assertNull(authViewModel.currentUser.value)
  }

  @Test
  fun mapLoadsFromMapState() {
    // Uses Mockk since Mockito can not mock/spy final classes
    val spyHikesViewModel = spyk(hikesViewModel)
    setUpMap(hikesViewModel = spyHikesViewModel)

    composeTestRule.waitForIdle()

    io.mockk.verify { spyHikesViewModel.getMapState() }
  }
}
