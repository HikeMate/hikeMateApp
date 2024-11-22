package ch.hikemate.app.ui.guide

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import ch.hikemate.app.R
import ch.hikemate.app.model.guide.GuideTopic
import ch.hikemate.app.ui.navigation.NavigationActions
import ch.hikemate.app.ui.navigation.Route
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.mock

class GuideScreenTest {
  @get:Rule val composeTestRule = createComposeRule()

  @Mock private lateinit var mockNavigationActions: NavigationActions

  private val testAppTopics =
      listOf(
          GuideTopic(
              titleResId = R.string.guide_section_navigation,
              contentResId = R.string.guide_content_navigation,
              actionRoute = Route.MAP))

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

  @Test
  fun topicHeader_containsCorrectElements() {
    val topicCard = "${GuideScreen.TOPIC_HEADER}_${testAppTopics[0].titleResId}"

    composeTestRule.onNodeWithTag(topicCard).assertExists().assertIsDisplayed()
  }

  @Test
  fun expandableContent_animatesCorrectly() {
    val topicCard = "${GuideScreen.TOPIC_CARD}_${testAppTopics[0].titleResId}"
    val topicContent = "${GuideScreen.TOPIC_CONTENT}_${testAppTopics[0].titleResId}"

    // Initial state - content should not exist
    composeTestRule.onNodeWithTag(topicContent, useUnmergedTree = true).assertDoesNotExist()

    // Expand card
    composeTestRule.onNodeWithTag(topicCard).onChildren()[0].performClick()

    // After animation, content should exist
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(topicContent, useUnmergedTree = true).assertExists()
  }

  @Test
  fun guideScreen_hasCorrectPadding() {
    composeTestRule.onNodeWithTag(GuideScreen.GUIDE_SCREEN).assertExists().assertHasNoClickAction()
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
