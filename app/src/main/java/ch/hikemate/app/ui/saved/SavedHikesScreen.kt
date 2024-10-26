package ch.hikemate.app.ui.saved

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import ch.hikemate.app.R
import ch.hikemate.app.ui.navigation.LIST_TOP_LEVEL_DESTINATIONS
import ch.hikemate.app.ui.navigation.NavigationActions
import ch.hikemate.app.ui.navigation.Route
import ch.hikemate.app.ui.navigation.SideBarNavigation

@Composable
fun SavedHikesScreen(navigationActions: NavigationActions) {
  // TODO: Implement Planned Hikes Screen
  // The Screen will need to be incorporated into the SideBarNavigation composable
  SideBarNavigation(
      onTabSelect = { route -> navigationActions.navigateTo(route) },
      tabList = LIST_TOP_LEVEL_DESTINATIONS,
      selectedItem = Route.SAVED_HIKES,
  ) {
    var currentSection by remember { mutableStateOf(SavedHikesScreen.Planned) }

    Column {
      Column(modifier = Modifier.weight(1f)) {
        when (currentSection) {
          SavedHikesScreen.Nearby -> NearbyHikes()
          SavedHikesScreen.Planned -> PlannedHikes()
          SavedHikesScreen.Saved -> SavedHikes()
        }
      }

      // Navigation items between nearby hikes, planned hikes, and saved hikes
      SavedHikesBottomMenu(currentSection) { currentSection = it }
    }
  }
}

@Composable
private fun NearbyHikes() {
  Text("Screen of nearby hikes")
}

@Composable
private fun PlannedHikes() {
  Text("Screen of planned hikes")
}

@Composable
private fun SavedHikes() {
  Text("Screen of saved hikes")
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
  Nearby("Nearby", R.drawable.location_on),
  Planned("Planned", R.drawable.calendar_today),
  Saved("Saved", R.drawable.bookmark)
}
