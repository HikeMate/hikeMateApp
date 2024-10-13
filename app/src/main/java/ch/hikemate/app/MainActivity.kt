package ch.hikemate.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.navigation.NavHost
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import ch.hikemate.app.ui.auth.SignInScreen
import ch.hikemate.app.ui.navigation.LIST_TOP_LEVEL_DESTINATIONS
import ch.hikemate.app.ui.navigation.NavigationActions
import ch.hikemate.app.ui.navigation.Route
import ch.hikemate.app.ui.navigation.Screen
import ch.hikemate.app.ui.navigation.SideBarNavigation
import ch.hikemate.app.ui.navigation.TopLevelDestinations
import ch.hikemate.app.ui.theme.SampleAppTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent { SampleAppTheme { Surface(modifier = Modifier.fillMaxSize()) { HikeMateApp() } } }
  }
}

/**
 * The main composable function for the HikeMate application. It sets up the navigation host and
 * defines the navigation graph.
 */
@Composable
fun HikeMateApp() {
  val navController = rememberNavController()
  val navigationActions = NavigationActions(navController)
  NavHost(navController = navController, startDestination = TopLevelDestinations.AUTH.route) {
    navigation(
        startDestination = Screen.AUTH,
        route = Route.AUTH,
    ) {
      composable(Screen.AUTH) { SignInScreen(navigationActions) }
    }

    navigation(
        startDestination = Screen.PLANNED_HIKES,
        route = Route.PLANNED_HIKES,
    ) {
      composable(Screen.PLANNED_HIKES) {
        // TODO: Implement Planned Hikes Screen
        Column(modifier = Modifier.fillMaxSize().testTag(Screen.PLANNED_HIKES)) {
          SideBarNavigation(
              onTabSelect = { route -> navigationActions.navigateTo(route) },
              tabList = LIST_TOP_LEVEL_DESTINATIONS,
              selectedItem = Route.PLANNED_HIKES,
          )
          Text(text = "Planned Hikes")
        }
      }
    }

    navigation(
        startDestination = Screen.MAP,
        route = Route.MAP,
    ) {
      composable(Screen.MAP) { // TODO: MAP SCREEN
        Column(modifier = Modifier.fillMaxSize().testTag(Screen.MAP)) {
          SideBarNavigation(
              onTabSelect = { route -> navigationActions.navigateTo(route) },
              tabList = LIST_TOP_LEVEL_DESTINATIONS,
              selectedItem = Route.MAP,
          )
          Text(text = "Map")
        }
      }
    }
  }
}
