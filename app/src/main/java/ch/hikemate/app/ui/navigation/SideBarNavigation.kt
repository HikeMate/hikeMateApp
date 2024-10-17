package ch.hikemate.app.ui.navigation

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ch.hikemate.app.ui.components.AppIcon
import kotlinx.coroutines.launch

const val TEST_TAG_SIDEBAR_BUTTON = "TEST_TAG_SIDEBAR_BUTTON"
const val TEST_TAG_DRAWER_CONTENT = "TEST_TAG_DRAWER_CONTENT"
const val TEST_TAG_DRAWER_CLOSE_BUTTON = "TEST_TAG_DRAWER_CLOSE_BUTTON"
const val TEST_TAG_DRAWER_ITEM_PREFIX = "TEST_TAG_DRAWER_ITEM_"

val IsSelectedKey = SemanticsPropertyKey<Boolean>("IsSelected")

/**
 * Composable function for the sidebar navigation.
 *
 * @param onTabSelect Callback function when an icon is selected.
 * @param tabList List of top-level destinations.
 * @param selectedItem The currently selected item.
 */
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun SideBarNavigation(
    onTabSelect: (TopLevelDestination) -> Unit,
    tabList: List<TopLevelDestination>,
    selectedItem: String,
    // The reason the content of the screen has to be passed as a lambda is because the drawer has
    // to be
    // integrated with the screen.
    content: @Composable () -> Unit,
) {
  val drawerState = rememberDrawerState(DrawerValue.Closed)
  val scope = rememberCoroutineScope()
  // This makes it so that when the drawer is open, the back gesture in android phones closes the
  // drawer instead of going to the previous screen.
  BackHandler(enabled = drawerState.isOpen) { scope.launch { drawerState.close() } }
  ModalNavigationDrawer(
      gesturesEnabled = false,
      drawerState = drawerState,
      drawerContent = {
        ModalDrawerSheet(modifier = Modifier.width(180.dp).testTag(TEST_TAG_DRAWER_CONTENT)) {
          Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { scope.launch { drawerState.close() } },
                    modifier = Modifier.testTag(TEST_TAG_DRAWER_CLOSE_BUTTON),
                ) {
                  Icon(Icons.Filled.Close, contentDescription = "Close")
                }
                AppIcon(size = 50.dp)
              }
          HorizontalDivider()
          tabList.forEach { tab ->
            val isSelected = tab.route == selectedItem
            NavigationDrawerItem(
                label = { Text(text = tab.textId) },
                selected = isSelected,
                onClick = {
                  onTabSelect(tab)
                  scope.launch { drawerState.close() }
                },
                icon = {
                  Icon(
                      tab.icon,
                      contentDescription = tab.textId,
                      modifier = Modifier.testTag(TEST_TAG_DRAWER_ITEM_PREFIX + tab.route + "Icon"),
                  )
                },
                modifier =
                    Modifier.testTag(TEST_TAG_DRAWER_ITEM_PREFIX + tab.route).semantics {
                      set(IsSelectedKey, isSelected)
                    },
            )
          }
        }
      },
      content = {
        Scaffold(
            topBar = {
              Button(
                  onClick = { scope.launch { drawerState.open() } },
                  modifier = Modifier.testTag(TEST_TAG_SIDEBAR_BUTTON),
                  content = {
                    Icon(
                        Icons.Filled.Menu,
                        contentDescription = "SideBar",
                    )
                  },
              )
            }) {
              content()
            }
      },
  )
}
