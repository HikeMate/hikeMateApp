package ch.hikemate.app

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import ch.hikemate.app.model.authentication.AuthViewModel
import ch.hikemate.app.model.authentication.FirebaseAuthRepository
import ch.hikemate.app.ui.auth.SignInScreen
import ch.hikemate.app.ui.map.MapScreen
import ch.hikemate.app.ui.navigation.LIST_TOP_LEVEL_DESTINATIONS
import ch.hikemate.app.ui.navigation.NavigationActions
import ch.hikemate.app.ui.navigation.Route
import ch.hikemate.app.ui.navigation.Screen
import ch.hikemate.app.ui.navigation.SideBarNavigation
import ch.hikemate.app.ui.navigation.TopLevelDestinations

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val firebaseAuthRepository = FirebaseAuthRepository()
    val authViewModel = AuthViewModel(firebaseAuthRepository)

    setContent {
      Column {
        val context = LocalContext.current
        var currUser = authViewModel.currentUser.collectAsState().value

        Text(currUser?.displayName ?: "No user signed in")
        val coroutineScope = rememberCoroutineScope()
        // Create the launcher for adding a Google account
        val addAccountLauncher =
            rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()) {
                    result: ActivityResult ->
                    authViewModel.signInWithGoogle(coroutineScope, context, null)
                    Toast.makeText(context, "success", Toast.LENGTH_SHORT).show()
              Log.d("MainActivity", "addAccountLauncher result: $result")
                }

        Button(
            onClick = {
              authViewModel.signInWithGoogle(coroutineScope, context, addAccountLauncher)
            },
        ) {
          Text("Sign in with Google")
        }
        Button(
            onClick = { authViewModel.signOut() },
        ) {
          Text("Sign out")
        }
      }
    }
    // setContent { HikeMateTheme { Surface(modifier = Modifier.fillMaxSize()) { HikeMateApp() } } }
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
        // The Screen will need to be incorporated into the SideBarNavigation composable
        SideBarNavigation(
            onTabSelect = { route -> navigationActions.navigateTo(route) },
            tabList = LIST_TOP_LEVEL_DESTINATIONS,
            selectedItem = Route.PLANNED_HIKES,
        ) {
          Text(
              text = "Planned Hikes to be implemented",
              modifier = Modifier.testTag(Screen.PLANNED_HIKES))
        }
      }
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
