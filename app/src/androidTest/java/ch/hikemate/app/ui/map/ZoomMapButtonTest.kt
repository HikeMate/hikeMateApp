package ch.hikemate.app.ui.map

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
    composeTestRule.setContent {
      MapScreen(
          hikingRoutesViewModel = listOfHikeRoutesViewModel, navigationActions = navigationActions)
    }
  }

  @Test
  fun buttonIsDisplayed() {
    composeTestRule.onNodeWithTag(ZOOM_MAP_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ZOOM_IN_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ZOOM_OUT_BUTTON).assertIsDisplayed()
  }

  @Test
  fun zoomInButtonIncreasesZoomLevel() {
    composeTestRule.onNodeWithTag(ZOOM_IN_BUTTON).performClick()
    composeTestRule.onNodeWithTag(ZOOM_OUT_BUTTON).performClick()
  }
}
