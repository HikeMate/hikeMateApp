package ch.hikemate.app.ui.map

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import ch.hikemate.app.model.route.HikeRoutesRepository
import ch.hikemate.app.model.route.ListOfHikeRoutesViewModel
import ch.hikemate.app.ui.navigation.NavigationActions
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class ZoomMapButtonTest {

  private lateinit var hikesRepository: HikeRoutesRepository
  private lateinit var listOfHikeRoutesViewModel: ListOfHikeRoutesViewModel
  private lateinit var navigationActions: NavigationActions

  @get:Rule val composeTestRule = createComposeRule()

  @Before
  fun setUp() {
    navigationActions = mock(NavigationActions::class.java)
    hikesRepository = mock(HikeRoutesRepository::class.java)
    listOfHikeRoutesViewModel = ListOfHikeRoutesViewModel(hikesRepository)
  }

  @Test
  fun buttonIsDisplayed() {
    composeTestRule.setContent {
      MapScreen(
          hikingRoutesViewModel = listOfHikeRoutesViewModel, navigationActions = navigationActions)
    }
    composeTestRule.onNodeWithTag(ZoomMapButton.ZOOM_OUT_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ZoomMapButton.ZOOM_IN_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ZoomMapButton.ZOOM_OUT_BUTTON).assertIsDisplayed()
  }

  @Test
  fun zoomButtonsAreClickable() {

    composeTestRule.setContent {
      MapScreen(
          hikingRoutesViewModel = listOfHikeRoutesViewModel, navigationActions = navigationActions)
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
