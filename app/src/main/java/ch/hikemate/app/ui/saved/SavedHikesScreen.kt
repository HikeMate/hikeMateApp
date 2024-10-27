package ch.hikemate.app.ui.saved

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.hikemate.app.R
import ch.hikemate.app.model.route.saved.SavedHike
import ch.hikemate.app.model.route.saved.SavedHikesViewModel
import ch.hikemate.app.ui.components.HikeCard
import ch.hikemate.app.ui.navigation.LIST_TOP_LEVEL_DESTINATIONS
import ch.hikemate.app.ui.navigation.NavigationActions
import ch.hikemate.app.ui.navigation.Route
import ch.hikemate.app.ui.navigation.SideBarNavigation
import ch.hikemate.app.utils.humanReadablePlannedLabel

@Composable
fun SavedHikesScreen(
    savedHikesViewModel: SavedHikesViewModel = viewModel(factory = SavedHikesViewModel.Factory),
    navigationActions: NavigationActions
) {
  // The Screen will need to be incorporated into the SideBarNavigation composable
  SideBarNavigation(
      onTabSelect = { route -> navigationActions.navigateTo(route) },
      tabList = LIST_TOP_LEVEL_DESTINATIONS,
      selectedItem = Route.SAVED_HIKES,
  ) { paddingValues ->
    var currentSection by remember { mutableStateOf(SavedHikesScreen.Planned) }
    val savedHikes by savedHikesViewModel.savedHike.collectAsState()

    LaunchedEffect(Unit) { savedHikesViewModel.loadSavedHikes() }

    Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
      Column(modifier = Modifier.weight(1f)) {
        when (currentSection) {
          SavedHikesScreen.Planned -> PlannedHikes(savedHikes)
          SavedHikesScreen.Saved -> SavedHikes(savedHikes)
        }
      }

      // Navigation items between nearby hikes, planned hikes, and saved hikes
      SavedHikesBottomMenu(currentSection) { currentSection = it }
    }
  }
}

@Composable
private fun PlannedHikes(hikes: List<SavedHike>) {
  Text(
      LocalContext.current.getString(R.string.saved_hikes_screen_planned_section_title),
      style = MaterialTheme.typography.titleLarge,
      modifier = Modifier.padding(16.dp))

  val plannedHikes = hikes.filter { it.date != null }.sortedBy { it.date }

  LazyColumn {
    items(plannedHikes.size) { index ->
      val hike = plannedHikes[index]
      HikeCard(
          title = hike.id,
          altitudeDifference = (index + 1) * 329,
          onClick = {},
          messageIcon = painterResource(R.drawable.calendar_today),
          messageContent = hike.date!!.humanReadablePlannedLabel(LocalContext.current),
          messageColor = Color(0xFF3B82F6))
    }
  }
}

@Composable
private fun SavedHikes(hikes: List<SavedHike>) {
  Text(
      LocalContext.current.getString(R.string.saved_hikes_screen_saved_section_title),
      style = MaterialTheme.typography.titleLarge,
      modifier = Modifier.padding(16.dp))

  val savedHikes = hikes.filter { it.date == null }

  LazyColumn {
    items(savedHikes.size) { index ->
      val hike = savedHikes[index]
      HikeCard(
          title = hike.id,
          altitudeDifference = 1000,
          onClick = {},
      )
    }
  }
}

@Composable
private fun SavedHikesBottomMenu(
    selected: SavedHikesScreen,
    onSelectedChange: (SavedHikesScreen) -> Unit
) {
  NavigationBar {
    SavedHikesScreen.values().forEach { screen ->
      NavigationBarItem(
          icon = { Icon(painter = painterResource(screen.icon), contentDescription = null) },
          label = { Text(screen.label) },
          selected = selected == screen,
          onClick = { onSelectedChange(screen) })
    }
  }
}

/**
 * Enum class representing the different sections of the Saved Hikes screen.
 *
 * The order of the enum values determines the order of the sections in the bottom menu. The first
 * element of the enum will be the left-most section in the bottom menu.
 */
private enum class SavedHikesScreen(val label: String, @DrawableRes val icon: Int) {
  Planned("Planned", R.drawable.calendar_today),
  Saved("Saved", R.drawable.bookmark)
}
