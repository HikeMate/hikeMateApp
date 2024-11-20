package ch.hikemate.app.ui.guide

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import ch.hikemate.app.R
import ch.hikemate.app.model.guide.GuideTopic
import ch.hikemate.app.ui.navigation.NavigationActions
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify

class GuideScreenTest {
  @get:Rule val composeTestRule = createComposeRule()

  @Mock private lateinit var mockNavigationActions: NavigationActions

  private val testAppTopics =
      listOf(
          GuideTopic(
              titleResId = R.string.guide_section_navigation,
              contentResId = R.string.guide_content_navigation,
              actionRoute = "test_route"))

  private val testHikingTopics =
      listOf(
          GuideTopic(
              titleResId = R.string.guide_section_weather,
              contentResId = R.string.guide_content_weather))

  @Before
  fun setup() {
    mockNavigationActions = mock(NavigationActions::class.java)
    setupGuideScreen()
  }

  @Test
  fun guideScreen_displayHeader() {
    composeTestRule.onNodeWithTag(GuideScreen.GUIDE_HEADER).assertExists()
  }

  @Test
  fun guideScreen_displaysAppTopics() {
    composeTestRule
        .onNodeWithTag("${GuideScreen.TOPIC_CARD}_${testAppTopics[0].titleResId}")
        .assertExists()
  }

  @Test
  fun guideScreen_displaysHikingTopics() {
    composeTestRule
        .onNodeWithTag("${GuideScreen.TOPIC_CARD}_${testHikingTopics[0].titleResId}")
        .assertExists()
  }

  @Test
  fun topicCard_expandsWhenClicked() {
    val topicCard = "${GuideScreen.TOPIC_CARD}_${testAppTopics[0].titleResId}"
    val topicContent = "${GuideScreen.TOPIC_CONTENT}_${testAppTopics[0].titleResId}"

    composeTestRule
        .onNodeWithTag(topicCard)
        .onChildren()[0] // Access the clickable Column inside Card
        .performClick()

    // The parameter useUnmergedTree is necessary because of the animation.
    composeTestRule.onNodeWithTag(topicContent, useUnmergedTree = true).assertExists()
  }

  // @Test
  fun navigationButton_triggersNavigation() {
    val topicCard = "${GuideScreen.TOPIC_CARD}_${testAppTopics[0].titleResId}"
    val navigationButton = "${GuideScreen.NAVIGATION_BUTTON}_${testAppTopics[0].actionRoute}"

    composeTestRule.waitForIdle()
    // Click to expand the card
    composeTestRule.onNodeWithTag(topicCard, useUnmergedTree = true).onChildren()[0].performClick()

    // Wait for animation to complete
    composeTestRule.waitForIdle()

    // Use useUnmergedTree when finding and interacting with the button

    composeTestRule
        .onNodeWithTag(navigationButton, useUnmergedTree = true)
        .assertIsEnabled()
        .performClick()

    verify(mockNavigationActions).navigateTo(testAppTopics[0].actionRoute!!)
  }

  private fun setupGuideScreen() {
    composeTestRule.setContent {
      GuideScreen(
          navigationActions = mockNavigationActions,
          appTopics = testAppTopics,
          hikingTopics = testHikingTopics)
    }
  }
}
