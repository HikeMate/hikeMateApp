package ch.hikemate.app.ui.map

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
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
import ch.hikemate.app.model.elevation.ElevationService
import ch.hikemate.app.model.profile.HikingLevel
import ch.hikemate.app.model.profile.Profile
import ch.hikemate.app.model.profile.ProfileRepository
import ch.hikemate.app.model.profile.ProfileViewModel
import ch.hikemate.app.model.route.Bounds
import ch.hikemate.app.model.route.HikeRoute
import ch.hikemate.app.model.route.HikeRoutesRepository
import ch.hikemate.app.model.route.HikesViewModel
import ch.hikemate.app.model.route.saved.SavedHikesRepository
import ch.hikemate.app.ui.components.HikeCard
import ch.hikemate.app.ui.navigation.NavigationActions
import ch.hikemate.app.ui.navigation.TEST_TAG_BOTTOM_BAR
import ch.hikemate.app.utils.LocationUtils
import ch.hikemate.app.utils.MapUtils
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.firebase.Timestamp
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import io.mockk.every
import io.mockk.mockkObject
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
  private lateinit var elevationService: ElevationService
  private lateinit var hikesViewModel: HikesViewModel
  private lateinit var navigationActions: NavigationActions
  private lateinit var authRepository: AuthRepository
  private lateinit var authViewModel: AuthViewModel
  private lateinit var profileRepository: ProfileRepository
  private lateinit var profileViewModel: ProfileViewModel

  @get:Rule val composeTestRule = createComposeRule()

  private val profile =
      Profile(
          id = "1",
          name = "John Doe",
          email = "john-doe@gmail.com",
          hikingLevel = HikingLevel.INTERMEDIATE,
          joinedDate = Timestamp.now())

  private fun setUpMap(
      mapMinZoomLevel: Double = MapScreen.MAP_MIN_ZOOM,
      mapInitialZoomLevel: Double = MapScreen.MAP_INITIAL_ZOOM
  ) {
    composeTestRule.setContent {
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
    elevationService = mock(ElevationService::class.java)
    profileRepository = mock(ProfileRepository::class.java)
    profileViewModel = ProfileViewModel(profileRepository)
    authRepository = mock(AuthRepository::class.java)
    authViewModel = AuthViewModel(authRepository, profileRepository)
    hikesViewModel =
        HikesViewModel(
            savedHikesRepo = savedHikesRepository,
            osmHikesRepo = hikesRepository,
            elevationService = elevationService,
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
  fun searchButtonDisappearsWhenClicked() {
    setUpMap()
    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_SEARCH_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_SEARCH_BUTTON).performClick()
    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_SEARCH_BUTTON).assertIsNotDisplayed()
  }

  @Test
  fun searchButtonReappearsWhenSearchSucceeded() {
    setUpMap()
    `when`(hikesRepository.getRoutes(any(), any(), any())).thenAnswer {
      val onSuccess = it.getArgument<(List<HikeRoute>) -> Unit>(1)
      onSuccess(listOf())
    }
    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_SEARCH_BUTTON).performClick()
    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_SEARCH_BUTTON).assertIsDisplayed()
  }

  @Test
  fun searchButtonReappearsWhenSearchFailed() {
    setUpMap()
    `when`(hikesRepository.getRoutes(any(), any(), any())).thenAnswer {
      val onFailure = it.getArgument<(Exception) -> Unit>(2)
      onFailure(Exception("Test exception"))
    }
    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_SEARCH_BUTTON).performClick()
    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_SEARCH_BUTTON).assertIsDisplayed()
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
    val hikeRoute = HikeRoute("Route 1", Bounds(0.0, 0.0, 0.0, 0.0), emptyList())
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
    val hike = HikeRoute("1", Bounds(-1.0, -1.0, 1.0, 1.0), listOf(), hikeName, null)
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
    val hike = HikeRoute("1", Bounds(-1.0, -1.0, 1.0, 1.0), listOf(), null, null)
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

    // Check that the repository is called with valid bounds
    verify(hikesRepository, times(1)).getRoutes(any(), any(), any())
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
    every { MapUtils.centerMapOnUserLocation(any(), any(), any()) } returns Unit

    // When
    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_CENTER_MAP_BUTTON).performClick()

    // Then
    io.mockk.verify { MapUtils.centerMapOnUserLocation(any(), any(), any()) }
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
    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_LOCATION_PERMISSION_ALERT).assertIsDisplayed()

    // When the user clicks on the "No thanks" button
    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_NO_THANKS_ALERT_BUTTON).performClick()

    // Then the alert disappears
    composeTestRule
        .onNodeWithTag(MapScreen.TEST_TAG_LOCATION_PERMISSION_ALERT)
        .assertIsNotDisplayed()
  }
}
