package ch.hikemate.app.ui.map

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import ch.hikemate.app.model.elevation.ElevationService
import ch.hikemate.app.model.profile.HikingLevel
import ch.hikemate.app.model.profile.Profile
import ch.hikemate.app.model.profile.ProfileRepository
import ch.hikemate.app.model.profile.ProfileViewModel
import ch.hikemate.app.model.route.Bounds
import ch.hikemate.app.model.route.HikeRoute
import ch.hikemate.app.model.route.HikeRoutesRepository
import ch.hikemate.app.model.route.LatLong
import ch.hikemate.app.model.route.ListOfHikeRoutesViewModel
import ch.hikemate.app.ui.map.RunHikeScreen.TEST_TAG_RUN_HIKE_SCREEN_BACK_BUTTON
import ch.hikemate.app.ui.map.RunHikeScreen.TEST_TAG_RUN_HIKE_SCREEN_BOTTOM_SHEET
import ch.hikemate.app.ui.map.RunHikeScreen.TEST_TAG_RUN_HIKE_SCREEN_MAP
import ch.hikemate.app.ui.map.RunHikeScreen.TEST_TAG_RUN_HIKE_SCREEN_ZOOM_BUTTONS
import ch.hikemate.app.ui.navigation.NavigationActions
import com.google.firebase.Timestamp
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.eq

class RunHikeScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var mockNavigationActions: NavigationActions
  private lateinit var profileRepository: ProfileRepository
  private lateinit var profileViewModel: ProfileViewModel
  private lateinit var hikesRepository: HikeRoutesRepository
  private lateinit var elevationService: ElevationService
  private lateinit var listOfHikeRoutesViewModel: ListOfHikeRoutesViewModel
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
    profileRepository = mock(ProfileRepository::class.java)
    profileViewModel = ProfileViewModel(profileRepository)
    hikesRepository = mock(HikeRoutesRepository::class.java)
    elevationService = mock(ElevationService::class.java)

    listOfHikeRoutesViewModel =
        ListOfHikeRoutesViewModel(hikesRepository, elevationService, UnconfinedTestDispatcher())

    listOfHikeRoutesViewModel.selectRoute(route)

    `when`(profileRepository.getProfileById(eq(profile.id), any(), any())).thenAnswer {
      val onSuccess = it.getArgument<(Profile) -> Unit>(1)
      onSuccess(profile)
    }
    profileViewModel.getProfileById(profile.id)
  }

  @Test
  fun runHikeScreen_displaysMap() = runTest {
    composeTestRule.setContent {
      RunHikeScreen(
          listOfHikeRoutesViewModel = listOfHikeRoutesViewModel,
          profileViewModel = profileViewModel,
          navigationActions = mockNavigationActions,
      )
    }
    composeTestRule.onNodeWithTag(TEST_TAG_RUN_HIKE_SCREEN_MAP).assertIsDisplayed()
  }

  @Test
  fun runHikeScreen_displaysBackButton() = runTest {
    composeTestRule.setContent {
      RunHikeScreen(
          listOfHikeRoutesViewModel = listOfHikeRoutesViewModel,
          profileViewModel = profileViewModel,
          navigationActions = mockNavigationActions,
      )
    }
    composeTestRule.onNodeWithTag(TEST_TAG_RUN_HIKE_SCREEN_BACK_BUTTON).assertIsDisplayed()
  }

  @Test
  fun runHikeScreen_displaysZoomButtons() = runTest {
    composeTestRule.setContent {
      RunHikeScreen(
          listOfHikeRoutesViewModel = listOfHikeRoutesViewModel,
          profileViewModel = profileViewModel,
          navigationActions = mockNavigationActions,
      )
    }
    composeTestRule.onNodeWithTag(TEST_TAG_RUN_HIKE_SCREEN_ZOOM_BUTTONS).assertIsDisplayed()
  }

  @Test
  fun runHikeScreen_displaysBottomSheet() {
    composeTestRule.setContent {
      RunHikeScreen(
          listOfHikeRoutesViewModel = listOfHikeRoutesViewModel,
          profileViewModel = profileViewModel,
          navigationActions = mockNavigationActions,
      )
    }
    composeTestRule.onNodeWithTag(TEST_TAG_RUN_HIKE_SCREEN_BOTTOM_SHEET).assertExists()
  }
}
