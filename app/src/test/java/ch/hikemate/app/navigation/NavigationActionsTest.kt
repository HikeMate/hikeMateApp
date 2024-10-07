package ch.hikemate.app.navigation

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.navOptions
import androidx.navigation.testing.TestNavHostController
import ch.hikemate.app.ui.navigation.NavigationActions
import ch.hikemate.app.ui.navigation.Route
import ch.hikemate.app.ui.navigation.Screen
import ch.hikemate.app.ui.navigation.TopLevelDestination
import ch.hikemate.app.ui.navigation.TopLevelDestinations
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq

class NavigationActionsTest {

  private lateinit var navigationDestination: NavDestination
  private lateinit var navHostController: NavHostController
  private lateinit var navigationActions: NavigationActions

  private lateinit var navGraph: NavGraph
  private lateinit var navController: TestNavHostController
  private lateinit var context: Context
  private var navOptionsCaptor = argumentCaptor<NavOptionsBuilder.() -> Unit>()

  @Before
  fun setUp() {
    context = mock(Context::class.java)
    navigationDestination = mock(NavDestination::class.java)
    navHostController = mock(NavHostController::class.java)
    navigationActions = NavigationActions(navHostController)
    navGraph = mock(NavGraph::class.java)
    `when`(navHostController.graph).thenReturn(navGraph)
    navController = spy(TestNavHostController(context))
  }

  @Test
  fun navigateToCallsController() {
    navigationActions.navigateTo(TopLevelDestinations.OVERVIEW)
    verify(navHostController)
        .navigate(eq(Route.OVERVIEW), org.mockito.kotlin.any<NavOptionsBuilder.() -> Unit>())
    navigationActions.navigateTo(Screen.MAP)
    verify(navHostController).navigate(Screen.MAP)
  }

  @Test
  fun goBackCallsController() {
    navigationActions.goBack()
    verify(navHostController).popBackStack()
  }

  @Test
  fun currentRouteWorksWithDestination() {
    `when`(navHostController.currentDestination).thenReturn(navigationDestination)
    `when`(navigationDestination.route).thenReturn(Route.OVERVIEW)

    assertThat(navigationActions.currentRoute(), `is`(Route.OVERVIEW))
  }

  @Test
  fun currentRoute_isEmptyWhenNoDestination() {
    `when`(navHostController.currentDestination).thenReturn(null)
    assertThat(navigationActions.currentRoute(), `is`(""))
  }

  @Test
  fun currentRoute_isEmptyWhenNoRoute() {
    `when`(navHostController.currentDestination).thenReturn(navigationDestination)
    `when`(navigationDestination.route).thenReturn(null)
    assertThat(navigationActions.currentRoute(), `is`(""))
  }

  @Test
  fun navigateToNotAuth_respectsProperties() {
    navigationActions.navigateTo(TopLevelDestinations.OVERVIEW)

    navHostController.navigate(currentRoute_isEmptyWhenNoRoute())
    verify(navHostController).navigate(eq(Route.OVERVIEW), navOptionsCaptor.capture())
    val navOptions = navOptions(navOptionsCaptor.firstValue)

    assertThat(navOptions.shouldRestoreState(), `is`(true))
    assertThat(navOptions.isPopUpToInclusive(), `is`(true))
    assertThat(navOptions.shouldPopUpToSaveState(), `is`(true))
    assertThat(navOptions.shouldLaunchSingleTop(), `is`(true))
  }

  @Test
  fun navigateToAuth_respectsProperties() {
    navigationActions.navigateTo(TopLevelDestination(Route.AUTH, Icons.Default.AccountBox, "Auth"))

    navHostController.navigate(currentRoute_isEmptyWhenNoRoute())
    verify(navHostController).navigate(eq(Route.AUTH), navOptionsCaptor.capture())
    val navOptions = navOptions(navOptionsCaptor.firstValue)

    assertThat(navOptions.shouldRestoreState(), `is`(false))
    assertThat(navOptions.isPopUpToInclusive(), `is`(true))
    assertThat(navOptions.shouldPopUpToSaveState(), `is`(true))
    assertThat(navOptions.shouldLaunchSingleTop(), `is`(true))
  }
}
