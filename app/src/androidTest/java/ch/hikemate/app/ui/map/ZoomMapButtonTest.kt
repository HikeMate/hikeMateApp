package ch.hikemate.app.ui.map

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import ch.hikemate.app.model.authentication.AuthRepository
import ch.hikemate.app.model.authentication.AuthViewModel
import ch.hikemate.app.model.elevation.ElevationRepository
import ch.hikemate.app.model.profile.HikingLevel
import ch.hikemate.app.model.profile.Profile
import ch.hikemate.app.model.profile.ProfileRepository
import ch.hikemate.app.model.profile.ProfileViewModel
import ch.hikemate.app.model.route.HikeRoutesRepository
import ch.hikemate.app.model.route.HikesViewModel
import ch.hikemate.app.model.route.saved.SavedHikesRepository
import ch.hikemate.app.ui.navigation.NavigationActions
import com.google.firebase.Timestamp
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.eq

class ZoomMapButtonTest {

  private lateinit var savedHikesRepository: SavedHikesRepository
  private lateinit var hikesRepository: HikeRoutesRepository
  private lateinit var elevationRepository: ElevationRepository
  private lateinit var hikesViewModel: HikesViewModel
  private lateinit var navigationActions: NavigationActions
  private lateinit var profileRepository: ProfileRepository
  private lateinit var profileViewModel: ProfileViewModel
  private lateinit var authRepository: AuthRepository
  private lateinit var authViewModel: AuthViewModel

  @get:Rule val composeTestRule = createComposeRule()

  private val profile =
      Profile(
          id = "1",
          name = "John Doe",
          email = "john-doe@gmail.com",
          hikingLevel = HikingLevel.AMATEUR,
          joinedDate = Timestamp.now())

  @OptIn(ExperimentalCoroutinesApi::class)
  @Before
  fun setUp() {
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
  fun buttonIsDisplayed() {
    composeTestRule.setContent {
      MapScreen(
          hikesViewModel = hikesViewModel,
          navigationActions = navigationActions,
          authViewModel = authViewModel,
          profileViewModel = profileViewModel)
    }
    composeTestRule.onNodeWithTag(ZoomMapButton.ZOOM_MAP_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ZoomMapButton.ZOOM_IN_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ZoomMapButton.ZOOM_OUT_BUTTON).assertIsDisplayed()
  }

  @Test
  fun zoomButtonsAreClickable() {

    composeTestRule.setContent {
      MapScreen(
          hikesViewModel = hikesViewModel,
          navigationActions = navigationActions,
          authViewModel = authViewModel,
          profileViewModel = profileViewModel)
    }
    composeTestRule.onNodeWithTag(ZoomMapButton.ZOOM_OUT_BUTTON).assertHasClickAction()
    composeTestRule.onNodeWithTag(ZoomMapButton.ZOOM_IN_BUTTON).assertHasClickAction()
  }

  @Test
  fun zoomInButtonWorks() {
    val onZoomIn: () -> Unit = mock()
    val onZoomOut: () -> Unit = mock()
    composeTestRule.setContent { ZoomMapButton(onZoomOut = onZoomOut, onZoomIn = onZoomIn) }
    composeTestRule.onNodeWithTag(ZoomMapButton.ZOOM_IN_BUTTON).performClick()
    verify(onZoomIn).invoke()
  }

  @Test
  fun zoomOutButtonWorks() {
    val onZoomIn: () -> Unit = mock()
    val onZoomOut: () -> Unit = mock()
    composeTestRule.setContent { ZoomMapButton(onZoomOut = onZoomOut, onZoomIn = onZoomIn) }
    composeTestRule.onNodeWithTag(ZoomMapButton.ZOOM_OUT_BUTTON).performClick()
    verify(onZoomOut).invoke()
  }

  @Test
  fun bothButtonsWork() {
    val onZoomIn: () -> Unit = mock()
    val onZoomOut: () -> Unit = mock()
    composeTestRule.setContent { ZoomMapButton(onZoomOut = onZoomOut, onZoomIn = onZoomIn) }
    composeTestRule.onNodeWithTag(ZoomMapButton.ZOOM_OUT_BUTTON).performClick()
    verify(onZoomOut).invoke()
    composeTestRule.onNodeWithTag(ZoomMapButton.ZOOM_IN_BUTTON).performClick()
    verify(onZoomIn).invoke()
  }
}
