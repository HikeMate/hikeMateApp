package ch.hikemate.app

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import ch.hikemate.app.model.authentication.AuthViewModel
import ch.hikemate.app.model.authentication.FirebaseAuthRepository
import ch.hikemate.app.model.profile.ProfileRepositoryFirestore
import ch.hikemate.app.model.profile.ProfileViewModel
import ch.hikemate.app.model.route.HikesViewModel
import ch.hikemate.app.ui.auth.CreateAccountScreen
import ch.hikemate.app.ui.auth.SignInScreen
import ch.hikemate.app.ui.auth.SignInWithEmailScreen
import ch.hikemate.app.ui.guide.GuideScreen
import ch.hikemate.app.ui.map.HikeDetailScreen
import ch.hikemate.app.ui.map.MapScreen
import ch.hikemate.app.ui.map.RunHikeScreen
import ch.hikemate.app.ui.navigation.NavigationActions
import ch.hikemate.app.ui.navigation.Route
import ch.hikemate.app.ui.navigation.Screen
import ch.hikemate.app.ui.navigation.TopLevelDestinations
import ch.hikemate.app.ui.profile.DeleteAccountScreen
import ch.hikemate.app.ui.profile.EditProfileScreen
import ch.hikemate.app.ui.profile.ProfileScreen
import ch.hikemate.app.ui.saved.SavedHikesScreen
import ch.hikemate.app.ui.theme.HikeMateTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import org.osmdroid.config.Configuration

class MainActivity : ComponentActivity() {
  @SuppressLint("SourceLockedOrientationActivity")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    FirebaseApp.initializeApp(this) // Initialize Firebase
    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

    setContent {
      HikeMateTheme {
        val systemBarStyle by remember {
          val defaultSystemBarColor = android.graphics.Color.TRANSPARENT
          mutableStateOf(
              SystemBarStyle.auto(
                  lightScrim = defaultSystemBarColor, darkScrim = defaultSystemBarColor))
        }
        LaunchedEffect(systemBarStyle) {
          enableEdgeToEdge(statusBarStyle = systemBarStyle, navigationBarStyle = systemBarStyle)
        }
        Surface(modifier = Modifier.fillMaxSize()) { HikeMateApp() }
      }
    }
  }
}

/**
 * The main composable function for the HikeMate application. It
 * 1. Sets up the several view models that are needed for the whole app
 * 2. Ensures the user's profile and saved hikes are reloaded every time a new user logs in
 * 3. Configures what needs to be configured before using OSMDroid
 * 4. Sets up the navigation host and defines the navigation graph
 */
@Composable
fun HikeMateApp() {
  val profileViewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory)
  val authViewModel =
      AuthViewModel(
          FirebaseAuthRepository(), ProfileRepositoryFirestore(FirebaseFirestore.getInstance()))

  val hikesViewModel: HikesViewModel = viewModel(factory = HikesViewModel.Factory)

  // When a user logs-in again with a different account, get the new profile and the new user's
  // saved hikes list
  val user by authViewModel.currentUser.collectAsState()
  LaunchedEffect(user) {
    if (user != null) {
      profileViewModel.getProfileById(user!!.uid)
      hikesViewModel.refreshSavedHikesCache()
    }
  }

  // The configuration only needs to be done once, not on every recomposition
  val context = LocalContext.current
  LaunchedEffect(Unit) {
    Configuration.getInstance().apply {
      // Set user-agent to avoid rejected requests
      userAgentValue = context.packageName

      // Allow for faster loading of tiles. Default OSMDroid value is 2
      tileDownloadThreads = 4

      // Maximum number of tiles that can be downloaded at once. Default is 40
      tileDownloadMaxQueueSize = 40

      // Maximum number of bytes that can be used by the tile file system cache. Default is 600MB
      tileFileSystemCacheMaxBytes = 600L * 1024L * 1024L
    }
  }

  // Prepare the navigation actions to go from one page to the other
  val navController = rememberNavController()
  val navigationActions = NavigationActions(navController)

  // When the user is logged in, start on the map, otherwise start on the sign-in screen
  val isUserLoggedIn = authViewModel.isUserLoggedIn()
  val startDestination =
      if (isUserLoggedIn) TopLevelDestinations.MAP.route else TopLevelDestinations.AUTH.route

  // Set the app's screens hierarchy into the navigation actions
  NavHost(navController = navController, startDestination = startDestination) {
    navigation(
        startDestination = Screen.AUTH,
        route = Route.AUTH,
    ) {
      composable(Screen.AUTH) { SignInScreen(navigationActions, authViewModel) }
      composable(Screen.SIGN_IN_WITH_EMAIL) {
        SignInWithEmailScreen(navigationActions, authViewModel)
      }
      composable(Screen.CREATE_ACCOUNT) { CreateAccountScreen(navigationActions, authViewModel) }
      composable(Screen.DELETE_ACCOUNT) { DeleteAccountScreen(navigationActions, authViewModel) }
    }

    navigation(
        startDestination = Screen.SAVED_HIKES,
        route = Route.SAVED_HIKES,
    ) {
      composable(Screen.SAVED_HIKES) {
        SavedHikesScreen(navigationActions = navigationActions, hikesViewModel = hikesViewModel)
      }
    }

    navigation(
        startDestination = Screen.MAP,
        route = Route.MAP,
    ) {
      composable(Screen.MAP) {
        MapScreen(
            navigationActions = navigationActions,
            hikesViewModel = hikesViewModel,
            authViewModel = authViewModel)
      }
    }
    navigation(
        startDestination = Screen.HIKE_DETAILS,
        route = Route.HIKE_DETAILS,
    ) {
      composable(Screen.HIKE_DETAILS) {
        HikeDetailScreen(
            profileViewModel = profileViewModel,
            authViewModel = authViewModel,
            navigationActions = navigationActions,
            hikesViewModel = hikesViewModel,
        )
      }

      composable(Screen.RUN_HIKE) {
        RunHikeScreen(hikesViewModel = hikesViewModel, navigationActions = navigationActions)
      }
    }

    navigation(
        startDestination = Screen.PROFILE,
        route = Route.PROFILE,
    ) {
      composable(Screen.PROFILE) {
        ProfileScreen(
            navigationActions = navigationActions,
            profileViewModel = profileViewModel,
            authViewModel = authViewModel)
      }
      composable(Screen.EDIT_PROFILE) {
        EditProfileScreen(
            navigationActions = navigationActions, profileViewModel = profileViewModel)
      }
    }
    navigation(startDestination = Screen.TUTORIAL, route = Route.TUTORIAL) {
      composable(Screen.TUTORIAL) { GuideScreen(navigationActions) }
    }
  }
}
