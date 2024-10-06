package ch.hikemate.app.ui.navigation

import android.graphics.drawable.Icon
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ch.hikemate.app.ui.theme.SampleAppTheme
import kotlinx.coroutines.launch

const val TAG = "HikeMate"

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
                ModalDrawerSheet(modifier = Modifier.width(180.dp)) {
                    Row {
                        Button(onClick = { scope.launch { drawerState.close() } }) { Icons.Filled.Menu }
                        Text(TAG, modifier = Modifier.padding(16.dp))
                    }
                    HorizontalDivider()
                    tabList.forEach { tab ->
                        NavigationDrawerItem(
                            label = { Text(text = tab.textId) },
                            selected = tab.route == selectedItem,
                            onClick = {
                                onIconSelect(tab)
                                scope.launch { drawerState.close() }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.textId) }
                        )

                    }
                }
            },
            content = {
                IconButton(onClick = {
                    scope.launch {
                        drawerState.open()
                    }
                },
                    content = {
                        Icon(
                            Icons.Filled.Menu,
                            contentDescription = "SideBar",
                        )
                    }
                )
            }
        )
    }
}

@Preview
@Composable
fun SideBarNavigationPreview() {
    SideBarNavigation(onIconSelect = {}, tabList = topLevelDestinations, selectedItem = "")
}