package ch.hikemate.app.ui.guide

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import ch.hikemate.app.R
import ch.hikemate.app.model.guide.GuideTopic
import ch.hikemate.app.ui.navigation.NavigationActions
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class GuideScreenTest {
  @get:Rule val composeTestRule = createComposeRule()

  private val mockNavigationActions = mock<NavigationActions>()

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

  @Test
  fun guideScreen_displayHeader() {
    setupGuideScreen()
    composeTestRule.onNodeWithTag(GuideScreen.GUIDE_HEADER).assertExists()
  }

  @Test
  fun guideScreen_displaysAppTopics() {
    setupGuideScreen()
    composeTestRule
        .onNodeWithTag("${GuideScreen.TOPIC_CARD}_${testAppTopics[0].titleResId}")
        .assertExists()
  }

  @Test
  fun guideScreen_displaysHikingTopics() {
    setupGuideScreen()
    composeTestRule
        .onNodeWithTag("${GuideScreen.TOPIC_CARD}_${testHikingTopics[0].titleResId}")
        .assertExists()
  }

  @Test
  fun topicCard_expandsWhenClicked() {
    setupGuideScreen()
    val topicCard = "${GuideScreen.TOPIC_CARD}_${testAppTopics[0].titleResId}"
    val topicContent = "${GuideScreen.TOPIC_CONTENT}_${testAppTopics[0].titleResId}"

    composeTestRule
        .onNodeWithTag(topicCard)
        .onChildren()[0] // Access the clickable Column inside Card
        .performClick()

    composeTestRule.waitForIdle()
    // The parameter useUnmergedTree is necessary because of the animation.
    composeTestRule.onNodeWithTag(topicContent, useUnmergedTree = true).assertExists()
  }

  @Test
  fun navigationButton_triggersNavigation() {
    setupGuideScreen()
    val topicCard = "${GuideScreen.TOPIC_CARD}_${testAppTopics[0].titleResId}"
    val navigationButton = "${GuideScreen.NAVIGATION_BUTTON}_${testAppTopics[0].actionRoute}"

    composeTestRule.onNodeWithTag(topicCard).performClick()
    composeTestRule.onNodeWithTag(navigationButton).performClick()

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
