package ch.hikemate.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Place
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController

object Route {
    const val OVERVIEW = "Overview"
    const val MAP = "Map"
    const val AUTH = "Auth"
    const val PROFILE = "Profile"
}

object Screen {
    const val AUTH = "Auth Screen"
    const val OVERVIEW = "Overview Screen"
    const val MAP = "Map Screen"
    const val PROFILE = "Profile Screen"
    const val EDIT_PROFILE = "Edit-profile Screen"
}

data class TopLevelDestination(val route: String, val icon: ImageVector, val textId: String)

object TopLevelDestinations {
    val OVERVIEW=TopLevelDestination(Route.OVERVIEW, Icons.Outlined.Menu, "Overview")
    val MAP=TopLevelDestination(Route.MAP, Icons.Outlined.Place, "Map")
    val PROFILE=TopLevelDestination(Route.PROFILE, Icons.Outlined.Person, "Profile")
}
val topLevelDestinations = listOf(
    TopLevelDestinations.OVERVIEW,
    TopLevelDestinations.MAP,
    TopLevelDestinations.PROFILE
)

open class NavigationActions(
    private val navController: NavHostController,
) {
    /**
     * Navigate to the specified [TopLevelDestination]
     *
     * @param destination The top level destination to navigate to Clear the back stack when
     *   navigating to a new destination This is useful when navigating to a new screen from the
     *   bottom navigation bar as we don't want to keep the previous screen in the back stack
     */
    open fun navigateTo(destination: TopLevelDestination) {

        navController.navigate(destination.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
                inclusive = true
            }

            launchSingleTop = true

            if (destination.route != Route.AUTH) {
                restoreState = true
            }
        }
    }

    /**
     * Navigate to the specified screen.
     *
     * @param screen The screen to navigate to
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
     * @return The current route
     */
    open fun currentRoute(): String {
        return navController.currentDestination?.route ?: ""
    }
}
