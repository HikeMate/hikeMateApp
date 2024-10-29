package ch.hikemate.app

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import ch.hikemate.app.ui.auth.SignInScreen
import ch.hikemate.app.ui.map.MapScreen
import ch.hikemate.app.ui.navigation.LIST_TOP_LEVEL_DESTINATIONS
import ch.hikemate.app.ui.navigation.NavigationActions
import ch.hikemate.app.ui.navigation.Route
import ch.hikemate.app.ui.navigation.Screen
import ch.hikemate.app.ui.navigation.SideBarNavigation
import ch.hikemate.app.ui.navigation.TopLevelDestinations
import ch.hikemate.app.ui.saved.SavedHikesScreen
import ch.hikemate.app.ui.theme.HikeMateTheme

class MainActivity : ComponentActivity() {
  @SuppressLint("SourceLockedOrientationActivity")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    setContent { HikeMateTheme { Surface(modifier = Modifier.fillMaxSize()) { HikeMateApp() } } }
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
        startDestination = Screen.SAVED_HIKES,
        route = Route.SAVED_HIKES,
    ) {
      composable(Screen.SAVED_HIKES) { SavedHikesScreen(navigationActions = navigationActions) }
    }

    navigation(
        startDestination = Screen.MAP,
        route = Route.MAP,
    ) {
      composable(Screen.MAP) { MapScreen(navigationActions = navigationActions) }
    }
    navigation(
        startDestination = Screen.PROFILE,
        route = Route.PROFILE,
    ) {
      composable(Screen.PROFILE) {
        // TODO: Implement Profile Screen
        // The Screen will need to be incorporated into the SideBarNavigation composable
        SideBarNavigation(
            onTabSelect = { route -> navigationActions.navigateTo(route) },
            tabList = LIST_TOP_LEVEL_DESTINATIONS,
            selectedItem = Route.PROFILE,
        ) {
          Text(text = "Profile to be implemented", modifier = Modifier.testTag(Screen.PROFILE))
        }
      }
    }
  }
}
