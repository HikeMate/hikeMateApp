package ch.hikemate.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Place
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController

/** Object containing route constants. */
object Route {
  const val PLANNED_HIKES = "PlannedHikes"
  const val MAP = "Map"
  const val AUTH = "Auth"
  const val PROFILE = "Profile"
}

/** Object containing screen constants. */
object Screen {
  const val AUTH = "Auth Screen"
  const val PLANNED_HIKES = "PlannedHikes Screen"
  const val MAP = "Map Screen"
  const val PROFILE = "Profile Screen"
  const val EDIT_PROFILE = "Edit-profile Screen"
}

/**
 * Data class representing a top-level destination.
 *
 * @property route The route of the destination.
 * @property icon The icon associated with the destination.
 * @property textId The text identifier for the destination.
 */
data class TopLevelDestination(val route: String, val icon: ImageVector, val textId: String)

/** Object containing top-level destinations. */
object TopLevelDestinations {
  val PLANNED_HIKES = TopLevelDestination(Route.PLANNED_HIKES, Icons.Outlined.Menu, "Planned Hikes")
  val MAP = TopLevelDestination(Route.MAP, Icons.Outlined.Place, "Map")
  val PROFILE = TopLevelDestination(Route.PROFILE, Icons.Outlined.Person, "Profile")
  val AUTH = TopLevelDestination(Route.AUTH, Icons.Outlined.Person, "Auth")
}

/** List of top-level destinations. */
val LIST_TOP_LEVEL_DESTINATIONS =
    listOf(
        TopLevelDestinations.PLANNED_HIKES, TopLevelDestinations.MAP, TopLevelDestinations.PROFILE)

/**
 * Class containing navigation actions.
 *
 * @property navController The navigation controller used to manage app navigation.
 */
open class NavigationActions(
    private val navController: NavHostController,
) {
  /**
   * Navigate to the specified [TopLevelDestination].
   *
   * @param destination The top-level destination to navigate to. Clears the back stack when
   *   navigating to a new destination. This is useful when navigating to a new screen from the
   *   bottom navigation bar as we don't want to keep the previous screen in the back stack.
   */
  open fun navigateTo(destination: TopLevelDestination) {
    navController.navigate(destination.route) {
      // Clear the back stack up to the start destination
      popUpTo(navController.graph.findStartDestination().id) {
        saveState = true
        inclusive = true
      }
      launchSingleTop = true
      // Restore state if not navigating to the Auth route
      if (destination.route != Route.AUTH) {
        restoreState = true
      }
    }
  }

  /**
   * Navigate to the specified screen.
   *
   * @param screen The screen to navigate to.
   */
  open fun navigateTo(screen: String) {
    navController.navigate(screen)
  }

  /** Navigate back to the previous screen. */
  open fun goBack() {
    navController.popBackStack()
  }

  /**
   * Get the current route of the navigation controller.
   *
   * @return The current route.
   */
  open fun currentRoute(): String {
    return navController.currentDestination?.route ?: ""
  }
}
