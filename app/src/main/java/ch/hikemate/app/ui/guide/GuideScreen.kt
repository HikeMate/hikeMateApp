package ch.hikemate.app.ui.guide

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ch.hikemate.app.R
import ch.hikemate.app.model.guide.Guide.APP_GUIDE_TOPICS
import ch.hikemate.app.model.guide.Guide.HIKING_GUIDE_TOPICS
import ch.hikemate.app.model.guide.GuideTopic
import ch.hikemate.app.ui.navigation.BottomBarNavigation
import ch.hikemate.app.ui.navigation.LIST_TOP_LEVEL_DESTINATIONS
import ch.hikemate.app.ui.navigation.NavigationActions
import ch.hikemate.app.ui.navigation.Route

object GuideScreen {
  // Animation Constants
  const val ENTER_DURATION_MS = 200
  const val EXIT_DURATION_MS = 150

  // UI Dimensions
  const val HEADER_SPACING_DP = 16
  const val SECTION_SPACING_DP = 24
  const val CARD_SPACING_DP = 4
  const val CONTENT_PADDING_DP = 16
  const val ICON_SIZE_DP = 40
  const val CORNER_RADIUS_DP = 8
  const val HORIZONTAL_PADDING_DP = 16

  // Test Tags
  const val GUIDE_SCREEN = "guide_screen"
  const val GUIDE_HEADER = "guide_header"
  const val TOPIC_CARD = "topic_card"
  const val TOPIC_HEADER = "topic_header"
  const val TOPIC_CONTENT = "topic_content"
  const val NAVIGATION_BUTTON = "navigation_button"
}

/** AnimationConfig object which determines how the animation when opening a card is performed */
object AnimationConfig {
  val enterTransition =
      expandVertically(
          animationSpec =
              tween(durationMillis = GuideScreen.ENTER_DURATION_MS, easing = FastOutSlowInEasing)) +
          fadeIn(animationSpec = tween(GuideScreen.ENTER_DURATION_MS))

  val exitTransition =
      shrinkVertically(
          animationSpec =
              tween(durationMillis = GuideScreen.EXIT_DURATION_MS, easing = FastOutSlowInEasing)) +
          fadeOut(animationSpec = tween(GuideScreen.EXIT_DURATION_MS))
}

/**
 * The main Guide Screen
 *
 * @param navigationActions The navigation actions for the Guide Screen
 * @param modifier
 * @param appTopics The topics for the app guide
 * @param hikingTopics The topics for the hiking guide
 */
@Composable
fun GuideScreen(
    navigationActions: NavigationActions,
    modifier: Modifier = Modifier,
    appTopics: List<GuideTopic> = APP_GUIDE_TOPICS,
    hikingTopics: List<GuideTopic> = HIKING_GUIDE_TOPICS
) {

  BottomBarNavigation(
      onTabSelect = navigationActions::navigateTo,
      tabList = LIST_TOP_LEVEL_DESTINATIONS,
      selectedItem = Route.TUTORIAL) { padding ->
        GuideContent(
            modifier =
                modifier.padding(
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()),
            padding = padding,
            appTopics = appTopics,
            hikingTopics = hikingTopics,
            navigationActions = navigationActions)
      }
}

/**
 * The guide's content containing the LazyColumn with all the sections
 *
 * @param modifier
 * @param padding
 * @param appTopics
 * @param hikingTopics
 * @param navigationActions
 */
@Composable
private fun GuideContent(
    modifier: Modifier,
    padding: PaddingValues,
    appTopics: List<GuideTopic>,
    hikingTopics: List<GuideTopic>,
    navigationActions: NavigationActions
) {
  Box(modifier = Modifier.fillMaxSize().testTag(GuideScreen.GUIDE_SCREEN)) {
    LazyColumn(
        modifier =
            modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = GuideScreen.HORIZONTAL_PADDING_DP.dp)) {
          // The header which says HikeMate Guide with the logo
          item {
            Spacer(modifier = Modifier.height(GuideScreen.HEADER_SPACING_DP.dp))
            GuideHeader()
          }
          // The app's related topics
          items(appTopics) { topic ->
            ExpandableTopicCard(
                topic = topic,
                navigationActions = navigationActions,
            )
          }

          // Separator between the app's topics and the hiking's topics
          item { HikingGuideSection() }

          // Hiking in general related topics
          items(hikingTopics) { topic ->
            ExpandableTopicCard(
                topic = topic,
                navigationActions = navigationActions,
            )
          }

          item { Spacer(modifier = Modifier.height(GuideScreen.HEADER_SPACING_DP.dp)) }
        }
  }
}

/** Guide header which contains the logo and the title */
@Composable
private fun GuideHeader() {
  Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier =
          Modifier.padding(vertical = GuideScreen.CONTENT_PADDING_DP.dp)
              .testTag(GuideScreen.GUIDE_HEADER)) {
        // HikeMate's logo
        Image(
            painter = painterResource(id = R.drawable.app_icon),
            contentDescription = stringResource(R.string.guide_app_logo),
            modifier = Modifier.size(GuideScreen.ICON_SIZE_DP.dp))
        Spacer(modifier = Modifier.width(GuideScreen.HEADER_SPACING_DP.dp))
        Text(
            text = stringResource(R.string.guide_title),
            style = MaterialTheme.typography.headlineMedium)
      }
}

/** Hiking guide section title */
@Composable
private fun HikingGuideSection() {
  Spacer(modifier = Modifier.height(GuideScreen.SECTION_SPACING_DP.dp))
  Text(
      text = stringResource(R.string.guide_hiking_section_title),
      style = MaterialTheme.typography.headlineMedium)
  Spacer(modifier = Modifier.height(GuideScreen.HEADER_SPACING_DP.dp))
}

/**
 * The composable function for a single topic card
 *
 * @param topic the topic which will be described
 * @param navigationActions
 */
@Composable
private fun ExpandableTopicCard(
    topic: GuideTopic,
    navigationActions: NavigationActions,
) {
  var isExpanded by remember { mutableStateOf(false) }

  Card(
      modifier =
          Modifier.fillMaxWidth()
              .padding(vertical = GuideScreen.CARD_SPACING_DP.dp)
              .animateContentSize()
              .testTag("${GuideScreen.TOPIC_CARD}_${topic.titleResId}"),
      shape = RoundedCornerShape(GuideScreen.CORNER_RADIUS_DP.dp)) {
        TopicCardContent(
            topic = topic,
            isExpanded = isExpanded,
            onExpandToggle = { isExpanded = !isExpanded },
            navigationActions = navigationActions)
      }
}

/**
 * The content of a topic
 *
 * @param topic
 * @param isExpanded
 * @param onExpandToggle what to do when the user opens the Card
 * @param navigationActions
 */
@Composable
private fun TopicCardContent(
    topic: GuideTopic,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    navigationActions: NavigationActions
) {
  Column {
    // Move clickable to just the header
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .clickable(onClick = onExpandToggle)
                .testTag("${GuideScreen.TOPIC_HEADER}_${topic.titleResId}")
                .padding(GuideScreen.CONTENT_PADDING_DP.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
          Text(
              text = stringResource(topic.titleResId), style = MaterialTheme.typography.titleMedium)
          Icon(
              imageVector =
                  if (isExpanded) Icons.Default.KeyboardArrowUp
                  else Icons.Default.KeyboardArrowDown,
              contentDescription =
                  stringResource(
                      if (isExpanded) R.string.collapse_topic else R.string.expand_topic))
        }
    ExpandableContent(topic = topic, isExpanded = isExpanded, navigationActions = navigationActions)
  }
}

/**
 * The content of the card/topic once expanded
 *
 * @param topic
 * @param isExpanded
 * @param navigationActions
 */
@Composable
private fun ExpandableContent(
    topic: GuideTopic,
    isExpanded: Boolean,
    navigationActions: NavigationActions
) {
  AnimatedVisibility(
      modifier = Modifier.testTag("${GuideScreen.TOPIC_CONTENT}_${topic.titleResId}"),
      visible = isExpanded,
      enter = AnimationConfig.enterTransition,
      exit = AnimationConfig.exitTransition) {
        Column(modifier = Modifier.padding(GuideScreen.CONTENT_PADDING_DP.dp)) {
          Text(
              text = stringResource(topic.contentResId), style = MaterialTheme.typography.bodyLarge)

          Spacer(modifier = Modifier.height(GuideScreen.HEADER_SPACING_DP.dp))

          topic.actionRoute?.let { route ->
            NavigationButton(route = route, onClick = { navigationActions.navigateTo(route) })
          }
        }
      }
}

/** The button for the topic cards which redirect the user to another screen. */
@Composable
private fun NavigationButton(route: String, onClick: () -> Unit) {
  Button(
      onClick = onClick,
      modifier = Modifier.fillMaxWidth().testTag("${GuideScreen.NAVIGATION_BUTTON}_$route")) {
        Text(stringResource(R.string.guide_button_navigation, route))
      }
}
