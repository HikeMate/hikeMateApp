package ch.hikemate.app.ui.saved

import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.hikemate.app.R
import ch.hikemate.app.model.route.saved.SavedHike
import ch.hikemate.app.model.route.saved.SavedHikesViewModel
import ch.hikemate.app.ui.components.HikeCard
import ch.hikemate.app.ui.components.HikeCardStyleProperties
import ch.hikemate.app.ui.navigation.BottomBarNavigation
import ch.hikemate.app.ui.navigation.LIST_TOP_LEVEL_DESTINATIONS
import ch.hikemate.app.ui.navigation.NavigationActions
import ch.hikemate.app.ui.navigation.Route
import ch.hikemate.app.utils.humanReadablePlannedLabel

const val TEST_TAG_SAVED_HIKES_TABS_MENU = "SavedHikesTabsMenu"
const val TEST_TAG_SAVED_HIKES_TABS_MENU_ITEM_PREFIX = "SavedHikesTabsMenuItem_"
const val TEST_TAG_SAVED_HIKES_SECTION_CONTAINER = "SavedHikesSectionContainer"
const val TEST_TAG_SAVED_HIKES_PLANNED_TITLE = "SavedHikesPlannedTitle"
const val TEST_TAG_SAVED_HIKES_SAVED_TITLE = "SavedHikesSavedTitle"
const val TEST_TAG_SAVED_HIKES_PLANNED_EMPTY_MESSAGE = "SavedHikesPlannedEmptyMessage"
const val TEST_TAG_SAVED_HIKES_SAVED_EMPTY_MESSAGE = "SavedHikesSavedEmptyMessage"
const val TEST_TAG_SAVED_HIKES_HIKE_CARD = "SavedHikesHikeCard"

@Composable
fun SavedHikesScreen(
    savedHikesViewModel: SavedHikesViewModel = viewModel(factory = SavedHikesViewModel.Factory),
    navigationActions: NavigationActions
) {
  BottomBarNavigation(
      onTabSelect = { route -> navigationActions.navigateTo(route) },
      tabList = LIST_TOP_LEVEL_DESTINATIONS,
      selectedItem = Route.SAVED_HIKES,
  ) { paddingValues ->
    var currentSection by remember { mutableStateOf(SavedHikesScreen.Planned) }
    val savedHikes by savedHikesViewModel.savedHike.collectAsState()

    val pagerState = rememberPagerState { SavedHikesScreen.values().size }

    LaunchedEffect(Unit) { savedHikesViewModel.loadSavedHikes() }

    LaunchedEffect(currentSection) { pagerState.animateScrollToPage(currentSection.ordinal) }

    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
      if (!pagerState.isScrollInProgress)
          currentSection = SavedHikesScreen.values()[pagerState.currentPage]
    }

    Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
      // Navigation items between nearby hikes, planned hikes, and saved hikes
      SavedHikesTabsMenu(
          selectedIndex = currentSection.ordinal, onSelectedChange = { currentSection = it })

      HorizontalPager(
          state = pagerState,
          modifier = Modifier.fillMaxWidth().weight(1f),
      ) { pageIndex ->
        Column(modifier = Modifier.testTag(TEST_TAG_SAVED_HIKES_SECTION_CONTAINER)) {
          SavedHikesScreen.values()[pageIndex].let {
            when (it) {
              SavedHikesScreen.Planned -> PlannedHikes(savedHikes)
              SavedHikesScreen.Saved -> SavedHikes(savedHikes)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun PlannedHikes(hikes: List<SavedHike>?) {
  val context = LocalContext.current
  Text(
      context.getString(R.string.saved_hikes_screen_planned_section_title),
      style = MaterialTheme.typography.titleLarge,
      modifier = Modifier.padding(16.dp).testTag(TEST_TAG_SAVED_HIKES_PLANNED_TITLE))

  val plannedHikes = hikes?.filter { it.date != null }?.sortedBy { it.date }

  if (plannedHikes.isNullOrEmpty()) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Text(
          text = context.getString(R.string.saved_hikes_screen_planned_section_empty_message),
          style = MaterialTheme.typography.bodyLarge,
          modifier = Modifier.padding(16.dp).testTag(TEST_TAG_SAVED_HIKES_PLANNED_EMPTY_MESSAGE))
    }
  } else {
    LazyColumn {
      items(plannedHikes.size, key = { plannedHikes[it].id }) { index ->
        val hike = plannedHikes[index]
        HikeCard(
            title = hike.name,
            // This generates a random list of elevation data for the hike
            // with a random number of points and altitude between 0 and 1000
            elevationData = (0..(0..1000).random()).map { it.toDouble() }.shuffled(),
            onClick = {
              Toast.makeText(
                      context,
                      "Hike details not implemented yet for this screen, since the listOfHikeRoutesVM does not support fetching just a single hike for the moment",
                      Toast.LENGTH_SHORT)
                  .show()
            },
            messageContent = hike.date!!.humanReadablePlannedLabel(LocalContext.current),
            modifier = Modifier.testTag(TEST_TAG_SAVED_HIKES_HIKE_CARD),
            styleProperties =
                HikeCardStyleProperties(
                    messageIcon = painterResource(R.drawable.calendar_today),
                    messageColor = Color(0xFF3B82F6)))
      }
    }
  }
}

@Composable
private fun SavedHikes(hikes: List<SavedHike>?) {
  val context = LocalContext.current
  Text(
      context.getString(R.string.saved_hikes_screen_saved_section_title),
      style = MaterialTheme.typography.titleLarge,
      modifier = Modifier.padding(16.dp).testTag(TEST_TAG_SAVED_HIKES_SAVED_TITLE))

  val savedHikes = hikes?.filter { it.date == null }

  if (savedHikes.isNullOrEmpty()) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Text(
          text = context.getString(R.string.saved_hikes_screen_saved_section_empty_message),
          style = MaterialTheme.typography.bodyLarge,
          modifier = Modifier.padding(16.dp).testTag(TEST_TAG_SAVED_HIKES_SAVED_EMPTY_MESSAGE))
    }
  } else {
    LazyColumn {
      items(savedHikes.size, key = { savedHikes[it].id }) { index ->
        val hike = savedHikes[index]
        HikeCard(
            title = hike.name,
            onClick = {
              Toast.makeText(
                      context,
                      "Hike details not implemented yet for this screen, since the listOfHikeRoutesVM does not support fetching just a single hike for the moment",
                      Toast.LENGTH_SHORT)
                  .show()
            },
            // This generates a random list of elevation data for the hike
            // with a random number of points and altitude between 0 and 1000
            elevationData = (0..(0..1000).random()).map { it.toDouble() }.shuffled(),
            modifier = Modifier.testTag(TEST_TAG_SAVED_HIKES_HIKE_CARD))
      }
    }
  }
}

@Composable
private fun SavedHikesTabsMenu(selectedIndex: Int, onSelectedChange: (SavedHikesScreen) -> Unit) {
  TabRow(
      selectedTabIndex = selectedIndex,
      modifier = Modifier.testTag(TEST_TAG_SAVED_HIKES_TABS_MENU)) {
        SavedHikesScreen.values().forEachIndexed { index, screen ->
          Tab(
              text = { Text(screen.label) },
              icon = { Icon(painter = painterResource(screen.icon), contentDescription = null) },
              selected = selectedIndex == index,
              onClick = { onSelectedChange(screen) },
              modifier = Modifier.testTag(TEST_TAG_SAVED_HIKES_TABS_MENU_ITEM_PREFIX + screen.name))
        }
      }
}

/**
 * Enum class representing the different sections of the Saved Hikes screen.
 *
 * The order of the enum values determines the order of the sections in the bottom menu. The first
 * element of the enum will be the left-most section in the bottom menu.
 */
enum class SavedHikesScreen(val label: String, @DrawableRes val icon: Int) {
  Saved("Saved", R.drawable.bookmark),
  Planned("Planned", R.drawable.calendar_today)
}
