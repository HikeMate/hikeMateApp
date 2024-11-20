package ch.hikemate.app.ui.guide

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import ch.hikemate.app.R
import ch.hikemate.app.model.guide.Guide
import ch.hikemate.app.ui.navigation.*


@Composable
fun GuideScreen(
    navigationActions: NavigationActions,
    modifier: Modifier = Modifier
) {
    var activeGuide by remember { mutableStateOf<List<Guide.GuideStep>?>(null) }
    var currentStepIndex by remember { mutableStateOf(0) }

    BottomBarNavigation(
        onTabSelect = { navigationActions.navigateTo(it) },
        tabList = LIST_TOP_LEVEL_DESTINATIONS,
        selectedItem = Route.TUTORIAL
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
            ) {
                // Title with App Logo
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    GuideHeader()
                }

                // App Guide Topics
                items(appGuideTopics) { topic ->
                    ExpandableTopicCard(
                        topic = topic,
                        navigationActions = navigationActions,
                        onStartGuide = { steps ->
                            activeGuide = steps
                            currentStepIndex = 0
                        }
                    )
                }

                // Hiking Guide Section
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = stringResource(R.string.guide_hiking_section_title),
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Hiking Topics
                items(hikingTopics) { topic ->
                    ExpandableTopicCard(
                        topic = topic,
                        navigationActions = navigationActions,
                        onStartGuide = { steps ->
                            activeGuide = steps
                            currentStepIndex = 0
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Interactive Guide Overlay
            activeGuide?.let { guide ->
                if (currentStepIndex < guide.size) {
                    GuideOverlay(
                        navigationActions = navigationActions,
                        step = guide[currentStepIndex],
                        onNext = {
                            if (currentStepIndex < guide.size - 1) {
                                currentStepIndex++
                            } else {
                                activeGuide = null
                                currentStepIndex = 0
                            }
                        },
                        onClose = {
                            activeGuide = null
                            currentStepIndex = 0
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun GuideOverlay(
    navigationActions: NavigationActions,
    step: Guide.GuideStep,
    onNext: () -> Unit,
    onClose: () -> Unit
) {
    Dialog(onDismissRequest = onClose) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = stringResource(step.title),
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(step.description),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onClose) {
                        Text("Close")
                    }
                    Button(onClick = {
                        step.action?.invoke(navigationActions)
                        onNext()
                    }) {
                        Text("Next")
                    }
                }
            }
        }
    }
}

@Composable
private fun GuideHeader() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 16.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.app_icon),
            contentDescription = null,
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = stringResource(R.string.guide_title),
            style = MaterialTheme.typography.headlineMedium
        )
    }
}

@Composable
private fun ExpandableTopicCard(
    topic: Guide.GuideTopic,
    navigationActions: NavigationActions,
    onStartGuide: (List<Guide.GuideStep>) -> Unit // Can be removed if not needed anymore
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .animateContentSize(),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.clickable { isExpanded = !isExpanded }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    topic.iconResId?.let { iconResId ->
                        Image(
                            painter = painterResource(id = iconResId),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    Text(
                        text = stringResource(topic.titleResId),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp
                    else Icons.Default.KeyboardArrowDown,
                    contentDescription = null
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(
                    animationSpec = tween(
                        durationMillis = 200,
                        easing = FastOutSlowInEasing
                    )
                ) + fadeIn(
                    animationSpec = tween(200)
                ),
                exit = shrinkVertically(
                    animationSpec = tween(
                        durationMillis = 150,
                        easing = FastOutSlowInEasing
                    )
                ) + fadeOut(
                    animationSpec = tween(150)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(topic.contentResId),
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    topic.actionRoute?.let { route ->
                        Button(
                            onClick = { navigationActions.navigateTo(route) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.guide_button_try_now))
                        }
                    }
                }
            }
        }
    }
}
private val appGuideTopics = listOf(
    Guide.GuideTopic(
        titleResId = R.string.guide_topic_find_trails,
        contentResId = R.string.guide_content_find_trails,
        actionRoute = Route.MAP,
    ),
    Guide.GuideTopic(
        titleResId = R.string.guide_topic_save_trails,
        contentResId = R.string.guide_content_save_trails,
        actionRoute = Route.SAVED_HIKES,
    ),
    Guide.GuideTopic(
        titleResId = R.string.guide_section_planning_features,
        contentResId = R.string.guide_content_choosing_trail,
        actionRoute = Route.SAVED_HIKES
    ),
    Guide.GuideTopic(
        titleResId = R.string.guide_section_difficulty_levels,
        contentResId = R.string.guide_content_difficulty_levels
    ),
    Guide.GuideTopic(
        titleResId = R.string.guide_topic_track_location,
        contentResId = R.string.guide_content_choosing_trail
    )
)
private val hikingTopics = listOf(
    Guide.GuideTopic(
        titleResId = R.string.guide_section_basic_gear,
        contentResId = R.string.guide_content_basic_gear
    ),
    Guide.GuideTopic(
        titleResId = R.string.guide_section_weather,
        contentResId = R.string.guide_content_weather
    ),
    Guide.GuideTopic(
        titleResId = R.string.guide_section_safety_basics,
        contentResId = R.string.guide_content_difficulty_levels
    ),
    Guide.GuideTopic(
        titleResId = R.string.guide_section_navigation,
        contentResId = R.string.guide_content_choosing_trail
    ),
    Guide.GuideTopic(
        titleResId = R.string.guide_section_hiking_form,
        contentResId = R.string.guide_content_basic_gear
    ),
    Guide.GuideTopic(
        titleResId = R.string.guide_section_elevation,
        contentResId = R.string.guide_content_weather
    )
)