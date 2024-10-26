package ch.hikemate.app.ui.saved

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import ch.hikemate.app.ui.navigation.LIST_TOP_LEVEL_DESTINATIONS
import ch.hikemate.app.ui.navigation.NavigationActions
import ch.hikemate.app.ui.navigation.Route
import ch.hikemate.app.ui.navigation.Screen
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
    Text(text = "Planned Hikes to be implemented", modifier = Modifier.testTag(Screen.SAVED_HIKES))
  }
}
