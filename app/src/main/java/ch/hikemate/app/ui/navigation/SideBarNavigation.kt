package ch.hikemate.app.ui.navigation

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
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ch.hikemate.app.ui.theme.SampleAppTheme
import kotlinx.coroutines.launch

const val TAG = "HikeMate"
const val TEST_TAG_SIDEBAR_BUTTON = "TEST_TAG_SIDEBAR_BUTTON"
const val TEST_TAG_DRAWER_CONTENT = "TEST_TAG_DRAWER_CONTENT"
const val TEST_TAG_DRAWER_CLOSE_BUTTON = "TEST_TAG_DRAWER_CLOSE_BUTTON"
const val TEST_TAG_DRAWER_ITEM_PREFIX = "TEST_TAG_DRAWER_ITEM_"

val IsSelectedKey = SemanticsPropertyKey<Boolean>("IsSelected")

/**
 * Composable function for the sidebar navigation.
 *
 * @param onIconSelect Callback function when an icon is selected.
 * @param tabList List of top-level destinations.
 * @param selectedItem The currently selected item.
 */
@Composable
fun SideBarNavigation(
    onIconSelect: (TopLevelDestination) -> Unit,
    tabList: List<TopLevelDestination>,
    selectedItem: String
) {
  val drawerState = rememberDrawerState(DrawerValue.Closed)
  val scope = rememberCoroutineScope()
  SampleAppTheme {
    ModalNavigationDrawer(
        gesturesEnabled = false,
        drawerState = drawerState,
        drawerContent = {
          ModalDrawerSheet(modifier = Modifier.width(180.dp).testTag(TEST_TAG_DRAWER_CONTENT)) {
            Row {
              Button(
                  onClick = { scope.launch { drawerState.close() } },
                  modifier = Modifier.testTag(TEST_TAG_DRAWER_CLOSE_BUTTON)) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                  }
              Text(TAG, modifier = Modifier.padding(16.dp))
            }
            HorizontalDivider()
            tabList.forEach { tab ->
              val isSelected = tab.route == selectedItem
              NavigationDrawerItem(
                  label = { Text(text = tab.textId) },
                  selected = isSelected,
                  onClick = {
                    onIconSelect(tab)
                    scope.launch { drawerState.close() }
                  },
                  icon = {
                    Icon(
                        tab.icon,
                        contentDescription = tab.textId,
                        modifier =
                            Modifier.testTag(TEST_TAG_DRAWER_ITEM_PREFIX + tab.route + "Icon"))
                  },
                  modifier =
                      Modifier.testTag(TEST_TAG_DRAWER_ITEM_PREFIX + tab.route).semantics {
                        set(IsSelectedKey, isSelected)
                      })
            }
          }
        },
        content = {
          Button(
              onClick = { scope.launch { drawerState.open() } },
              modifier = Modifier.testTag(TEST_TAG_SIDEBAR_BUTTON),
              content = {
                Icon(
                    Icons.Filled.Menu,
                    contentDescription = "SideBar",
                )
              })
        })
  }
}
