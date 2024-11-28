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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import ch.hikemate.app.model.authentication.AuthViewModel
import ch.hikemate.app.model.authentication.FirebaseAuthRepository
import ch.hikemate.app.model.profile.ProfileRepositoryFirestore
import ch.hikemate.app.model.profile.ProfileViewModel
import ch.hikemate.app.model.route.ListOfHikeRoutesViewModel
import ch.hikemate.app.model.route.saved.SavedHikesViewModel
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
 * The main composable function for the HikeMate application. It sets up the navigation host and
 * defines the navigation graph.
 */
@Composable
fun HikeMateApp() {
  val navController = rememberNavController()
  val navigationActions = NavigationActions(navController)
  val profileViewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory)
  val authViewModel =
      AuthViewModel(
          FirebaseAuthRepository(), ProfileRepositoryFirestore(FirebaseFirestore.getInstance()))

  val isUserLoggedIn = authViewModel.isUserLoggedIn()

  val listOfHikeRoutesViewModel: ListOfHikeRoutesViewModel =
      viewModel(factory = ListOfHikeRoutesViewModel.Factory)
  val savedHikesViewModel: SavedHikesViewModel = viewModel(factory = SavedHikesViewModel.Factory)

  val user by authViewModel.currentUser.collectAsState()

  LaunchedEffect(user) {
    if (user != null) {
      profileViewModel.getProfileById(user!!.uid)
      savedHikesViewModel.loadSavedHikes()
    }
  }

  NavHost(
      navController = navController,
      startDestination =
          if (isUserLoggedIn) TopLevelDestinations.MAP.route else TopLevelDestinations.AUTH.route) {
        navigation(
            startDestination = Screen.AUTH,
            route = Route.AUTH,
        ) {
          composable(Screen.AUTH) { SignInScreen(navigationActions, authViewModel) }
          composable(Screen.SIGN_IN_WITH_EMAIL) {
            SignInWithEmailScreen(navigationActions, authViewModel)
          }
          composable(Screen.CREATE_ACCOUNT) {
            CreateAccountScreen(navigationActions, authViewModel)
          }
          composable(Screen.DELETE_ACCOUNT) {
            DeleteAccountScreen(navigationActions, authViewModel)
          }
        }

        navigation(
            startDestination = Screen.SAVED_HIKES,
            route = Route.SAVED_HIKES,
        ) {
          composable(Screen.SAVED_HIKES) {
            SavedHikesScreen(
                navigationActions = navigationActions, savedHikesViewModel = savedHikesViewModel)
          }
        }

        navigation(
            startDestination = Screen.MAP,
            route = Route.MAP,
        ) {
          composable(Screen.MAP) {
            MapScreen(
                navigationActions = navigationActions,
                hikingRoutesViewModel = listOfHikeRoutesViewModel,
                authViewModel = authViewModel)
          }
        }
        navigation(
            startDestination = Screen.HIKE_DETAILS,
            route = Route.HIKE_DETAILS,
        ) {
          composable(Screen.HIKE_DETAILS) {
            HikeDetailScreen(
                listOfHikeRoutesViewModel = listOfHikeRoutesViewModel,
                savedHikesViewModel = savedHikesViewModel,
                profileViewModel = profileViewModel,
                authViewModel = authViewModel,
                navigationActions = navigationActions,
            )
          }

          composable(Screen.RUN_HIKE) {
            RunHikeScreen(
                listOfHikeRoutesViewModel = listOfHikeRoutesViewModel,
                profileViewModel = profileViewModel,
                navigationActions = navigationActions)
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
