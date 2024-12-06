package ch.hikemate.app.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource

const val TEST_TAG_BOTTOM_BAR = "TEST_TAG_BOTTOM_BAR"
const val TEST_TAG_MENU_ITEM_PREFIX = "TEST_TAG_MENU_ITEM_"

/**
 * Composable function for the bottom bar navigation.
 *
 * @param onTabSelect Callback function when an icon is selected.
 * @param tabList List of top-level destinations.
 * @param selectedItem The currently selected item.
 */
@Composable
fun BottomBarNavigation(
    onTabSelect: (TopLevelDestination) -> Unit,
    tabList: List<TopLevelDestination>,
    selectedItem: String,
    // The reason the content of the screen has to be passed as a lambda is because
    // the drawer has to be integrated with the screen.
    content: @Composable (PaddingValues) -> Unit
) {
  Scaffold(
      bottomBar = {
        NavigationBar(
            modifier = Modifier.testTag(TEST_TAG_BOTTOM_BAR),
        ) {
          tabList.forEach { tab ->
            NavigationBarItem(
                icon = {
                  Icon(
                      tab.icon,
                      contentDescription = stringResource(tab.textId),
                  )
                },
                label = { Text(stringResource(tab.textId)) },
                selected = tab.route == selectedItem,
                onClick = { onTabSelect(tab) },
                modifier = Modifier.testTag(TEST_TAG_MENU_ITEM_PREFIX + tab.route),
            )
          }
        }
      }) { padding: PaddingValues ->
        content(padding)
      }
}
