package ch.hikemate.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController

/** Object containing route constants. */
object Route {
  const val SAVED_HIKES = "SavedHikes"
  const val MAP = "Map"
  const val AUTH = "Auth"
  const val PROFILE = "Profile"
  const val HIKE_DETAILS = "HikeDetails"
  const val TUTORIAL = "Guide"
}

/** Object containing screen constants. */
object Screen {
  const val AUTH = "Auth Screen"
  const val SIGN_IN_WITH_EMAIL = "Sign-in-with-email Screen"
  const val CREATE_ACCOUNT = "Create-account Screen"
  const val DELETE_ACCOUNT = "Delete-account Screen"
  const val SAVED_HIKES = "SavedHikes Screen"
  const val MAP = "Map Screen"
  const val PROFILE = "Profile Screen"
  const val EDIT_PROFILE = "Edit-profile Screen"
  const val HIKE_DETAILS = "HikeDetails Screen"
  const val TUTORIAL = "Guide Screen"
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
  val SAVED_HIKES = TopLevelDestination(Route.SAVED_HIKES, Icons.Outlined.Bookmark, "Saved Hikes")
  val MAP = TopLevelDestination(Route.MAP, Icons.Filled.LocationOn, "Map")
  val PROFILE = TopLevelDestination(Route.PROFILE, Icons.Filled.Person, "Profile")
  val AUTH = TopLevelDestination(Route.AUTH, Icons.Outlined.Person, "Auth")
  val TUTORIAL =
      TopLevelDestination(Route.TUTORIAL, Icons.AutoMirrored.Outlined.HelpOutline, "Tutorial")
}

/** List of top-level destinations. */
val LIST_TOP_LEVEL_DESTINATIONS =
    listOf(
        TopLevelDestinations.SAVED_HIKES,
        TopLevelDestinations.MAP,
        TopLevelDestinations.PROFILE,
        TopLevelDestinations.TUTORIAL)

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
