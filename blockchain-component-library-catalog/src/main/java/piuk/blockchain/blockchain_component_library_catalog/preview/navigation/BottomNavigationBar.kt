package piuk.blockchain.blockchain_component_library_catalog.preview.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.navigation.BottomNavigationBar
import com.blockchain.componentlib.navigation.BottomNavigationState
import com.blockchain.componentlib.navigation.NavigationItem
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

@Preview(name = "BottomNavigationBar", group = "Navigation")
@Composable
fun BottomNavigationBarPreview() {

    var selectedNavigationItem by remember { mutableStateOf(null as? NavigationItem?) }
    var bottomNavigationState by remember { mutableStateOf(BottomNavigationState.Add) }

    AppTheme {
        AppSurface {
            BottomNavigationBar(
                selectedNavigationItem = selectedNavigationItem,
                onNavigationItemClick = {
                    selectedNavigationItem = it
                },
                onMiddleButtonClick = {
                    bottomNavigationState = when (bottomNavigationState) {
                        BottomNavigationState.Add -> BottomNavigationState.Cancel
                        BottomNavigationState.Cancel -> BottomNavigationState.Add
                    }
                },
                bottomNavigationState = bottomNavigationState
            )
        }
    }
}