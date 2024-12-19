package ch.hikemate.app.ui.saved

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ch.hikemate.app.R
import ch.hikemate.app.model.route.DeferredData
import ch.hikemate.app.model.route.Hike
import ch.hikemate.app.model.route.HikesViewModel
import ch.hikemate.app.ui.components.CenteredErrorAction
import ch.hikemate.app.ui.components.CenteredLoadingAnimation
import ch.hikemate.app.ui.components.HikeCard
import ch.hikemate.app.ui.components.HikeCardStyleProperties
import ch.hikemate.app.ui.navigation.BottomBarNavigation
import ch.hikemate.app.ui.navigation.LIST_TOP_LEVEL_DESTINATIONS
import ch.hikemate.app.ui.navigation.NavigationActions
import ch.hikemate.app.ui.navigation.Route
import ch.hikemate.app.ui.navigation.Screen
import kotlinx.coroutines.flow.StateFlow

object SavedHikesScreen {
  // Components used by several (or all) sections
  const val TEST_TAG_SAVED_HIKES_TABS_MENU = "SavedHikesTabsMenu"
  const val TEST_TAG_SAVED_HIKES_TABS_MENU_ITEM_PREFIX = "SavedHikesTabsMenuItem_"
  const val TEST_TAG_SAVED_HIKES_SECTION_CONTAINER = "SavedHikesSectionContainer"
  const val TEST_TAG_SAVED_HIKES_HIKE_CARD = "SavedHikesHikeCard"

  // Components specific for the Planned section
  const val TEST_TAG_SAVED_HIKES_PLANNED_TITLE = "SavedHikesPlannedTitle"
  const val TEST_TAG_SAVED_HIKES_SAVED_TITLE = "SavedHikesSavedTitle"

  // Components specific for the Saved section
  const val TEST_TAG_SAVED_HIKES_PLANNED_EMPTY_MESSAGE = "SavedHikesPlannedEmptyMessage"
  const val TEST_TAG_SAVED_HIKES_SAVED_EMPTY_MESSAGE = "SavedHikesSavedEmptyMessage"
}

@Composable
fun SavedHikesScreen(hikesViewModel: HikesViewModel, navigationActions: NavigationActions) {
  BottomBarNavigation(
      onTabSelect = { route -> navigationActions.navigateTo(route) },
      tabList = LIST_TOP_LEVEL_DESTINATIONS,
      selectedItem = Route.SAVED_HIKES,
  ) { paddingValues ->
    var currentSection by remember { mutableStateOf(SavedHikesSection.Saved) }
    val loading by hikesViewModel.loading.collectAsState()
    val hikesType by hikesViewModel.loadedHikesType.collectAsState()
    val savedHikes by hikesViewModel.hikeFlows.collectAsState()
    val selectedHike by hikesViewModel.selectedHike.collectAsState()
    val osmDataAvailable by hikesViewModel.allOsmDataLoaded.collectAsState()
    val loadingErrorMessageId by hikesViewModel.loadingErrorMessageId.collectAsState()

    val pagerState = rememberPagerState { SavedHikesSection.values().size }

    LaunchedEffect(selectedHike) {
      if (selectedHike != null) {
        navigationActions.navigateTo(Screen.HIKE_DETAILS)
      }
    }

    LaunchedEffect(currentSection) { pagerState.animateScrollToPage(currentSection.ordinal) }

    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
      if (!pagerState.isScrollInProgress)
          currentSection = SavedHikesSection.values()[pagerState.currentPage]
    }

    Column(
        modifier =
            Modifier.fillMaxSize()
                .padding(paddingValues)
                .testTag(SavedHikesScreen.TEST_TAG_SAVED_HIKES_SECTION_CONTAINER)) {
          // Navigation items between nearby hikes, planned hikes, and saved hikes
          SavedHikesTabsMenu(
              selectedIndex = currentSection.ordinal, onSelectedChange = { currentSection = it })

          when {
            // The view model is either loading the saved hikes list or their OSM data, display a
            // loading screen
            loading ->
                CenteredLoadingAnimation(
                    stringResource(R.string.saved_hikes_screen_loading_message))

            // An error occurred while loading the saved hikes, or their OSM data, display an error
            // message and a retry button
            loadingErrorMessageId != null ->
                CenteredErrorAction(
                    errorMessageId = loadingErrorMessageId!!,
                    actionIcon = Icons.Default.Refresh,
                    actionContentDescriptionStringId =
                        R.string.saved_hikes_screen_refresh_button_action,
                    onAction = { hikesViewModel.loadSavedHikes() })

            // There is no loading operation ongoing, but the saved hikes are not the ones
            // currently displayed, reload the saved hikes
            hikesType != HikesViewModel.LoadedHikes.FromSaved -> {
              hikesViewModel.loadSavedHikes()
              CenteredLoadingAnimation(stringResource(R.string.saved_hikes_screen_loading_message))
            }

            // All data is available, start displaying the list of saved hikes
            osmDataAvailable -> {
              HorizontalPager(
                  state = pagerState,
                  modifier = Modifier.fillMaxWidth(),
              ) { pageIndex ->
                Column {
                  // Display either the planned or saved hikes section
                  when (SavedHikesSection.values()[pageIndex]) {
                    SavedHikesSection.Saved ->
                        SavedHikes(savedHikes = savedHikes, hikesViewModel = hikesViewModel)
                    SavedHikesSection.Planned ->
                        PlannedHikes(hikes = savedHikes, hikesViewModel = hikesViewModel)
                  }
                }
              }
            }

            // OSM data are missing, retrieve them
            else -> {
              hikesViewModel.retrieveLoadedHikesOsmData()
              CenteredLoadingAnimation(stringResource(R.string.saved_hikes_screen_loading_message))
            }
          }
        }
  }
}

@Composable
private fun PlannedHikes(hikes: List<StateFlow<Hike>>?, hikesViewModel: HikesViewModel) {
  val context = LocalContext.current
  // Use fillMaxSize() to ensure consistent layout when switching between tabs
  // This prevents unwanted UI resizing that can occur when sections have different
  // numbers of items, particularly when transitioning between saved and planned hikes
  Column(modifier = Modifier.fillMaxSize()) {
    Text(
        context.getString(R.string.saved_hikes_screen_planned_section_title),
        style = MaterialTheme.typography.headlineLarge,
        modifier =
            Modifier.padding(16.dp).testTag(SavedHikesScreen.TEST_TAG_SAVED_HIKES_PLANNED_TITLE))

    val plannedHikes =
        hikes?.filter { it.value.plannedDate != null }?.sortedBy { it.value.plannedDate }

    if (plannedHikes.isNullOrEmpty()) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = context.getString(R.string.saved_hikes_screen_planned_section_empty_message),
            style = MaterialTheme.typography.bodyLarge,
            modifier =
                Modifier.padding(16.dp)
                    .testTag(SavedHikesScreen.TEST_TAG_SAVED_HIKES_PLANNED_EMPTY_MESSAGE))
      }
    } else {
      LazyColumn {
        items(plannedHikes.size, key = { plannedHikes[it].value.id }) { index ->
          val hike by plannedHikes[index].collectAsState()
          SavedHikeCardFor(hike, hikesViewModel)
        }
      }
    }
  }
}

@Composable
private fun SavedHikes(savedHikes: List<StateFlow<Hike>>?, hikesViewModel: HikesViewModel) {
  val context = LocalContext.current
  // Use fillMaxSize() to ensure consistent layout when switching between tabs
  // This prevents unwanted UI resizing that can occur when sections have different
  // numbers of items, particularly when transitioning between saved and planned hikes
  Column(modifier = Modifier.fillMaxSize()) {
    Text(
        context.getString(R.string.saved_hikes_screen_saved_section_title),
        style = MaterialTheme.typography.headlineLarge,
        modifier =
            Modifier.padding(16.dp).testTag(SavedHikesScreen.TEST_TAG_SAVED_HIKES_SAVED_TITLE))

    if (savedHikes.isNullOrEmpty()) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = context.getString(R.string.saved_hikes_screen_saved_section_empty_message),
            style = MaterialTheme.typography.bodyLarge,
            modifier =
                Modifier.padding(16.dp)
                    .testTag(SavedHikesScreen.TEST_TAG_SAVED_HIKES_SAVED_EMPTY_MESSAGE))
      }
    } else {
      LazyColumn {
        items(savedHikes.size, key = { savedHikes[it].value.id }) { index ->
          val hike by savedHikes[index].collectAsState()
          SavedHikeCardFor(hike, hikesViewModel)
        }
      }
    }
  }
}

@Composable
private fun SavedHikeCardFor(hike: Hike, hikesViewModel: HikesViewModel) {
  // This variable contains the current state of the hike's elevation data. It can be:
  // - null: the elevation data is not available yet
  // - emptyList(): the elevation data is not available because of an error
  // - List<Double>: the elevation data is available
  val elevation: List<Double>?
  if (hike.elevation is DeferredData.Error) {
    // Display an empty elevation graph if the data is not available because of an error
    elevation = emptyList()
  } else if (!hike.elevation.obtained()) {
    // Ask for elevation if not already available
    hikesViewModel.retrieveElevationDataFor(hike.id)
    elevation = null
  } else {
    // If the elevation data is available, display it
    elevation = hike.elevation.getOrNull()
  }

  // Display the hike card
  HikeCard(
      title = hike.name ?: stringResource(R.string.map_screen_hike_title_default),
      elevationData = elevation,
      onClick = { hikesViewModel.selectHike(hike.id) },
      modifier = Modifier.testTag(SavedHikesScreen.TEST_TAG_SAVED_HIKES_HIKE_CARD),
      styleProperties = HikeCardStyleProperties(graphColor = Color(hike.getColor())))
}

@Composable
private fun SavedHikesTabsMenu(selectedIndex: Int, onSelectedChange: (SavedHikesSection) -> Unit) {
  TabRow(
      selectedTabIndex = selectedIndex,
      modifier = Modifier.testTag(SavedHikesScreen.TEST_TAG_SAVED_HIKES_TABS_MENU)) {
        SavedHikesSection.values().forEachIndexed { index, screen ->
          Tab(
              text = { Text(stringResource(screen.labelId)) },
              icon = { Icon(painter = painterResource(screen.icon), contentDescription = null) },
              selected = selectedIndex == index,
              onClick = { onSelectedChange(screen) },
              modifier =
                  Modifier.testTag(
                      SavedHikesScreen.TEST_TAG_SAVED_HIKES_TABS_MENU_ITEM_PREFIX + screen.name))
        }
      }
}

/**
 * Enum class representing the different sections of the Saved Hikes screen.
 *
 * The order of the enum values determines the order of the sections in the bottom menu. The first
 * element of the enum will be the left-most section in the bottom menu.
 */
enum class SavedHikesSection(val labelId: Int, @DrawableRes val icon: Int) {
  Saved(R.string.saved_hikes_saved_label, R.drawable.bookmark),
  Planned(R.string.saved_hikes_planned_label, R.drawable.calendar_today)
}
