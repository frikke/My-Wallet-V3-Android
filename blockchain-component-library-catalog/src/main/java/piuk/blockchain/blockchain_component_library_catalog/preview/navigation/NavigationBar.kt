package piuk.blockchain.blockchain_component_library_catalog.preview.navigation

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.R
import com.blockchain.componentlib.navigation.NavigationBar
import com.blockchain.componentlib.navigation.NavigationBarButton
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

@Preview(name = "NavigationBar Title", group = "Navigation")
@Composable
fun NavigationBarPreview() {
    AppTheme {
        AppSurface {
            NavigationBar(title = "Activity", onBackButtonClick = null, navigationBarButtons = emptyList())
        }
    }
}

@Preview(name = "NavigationBar Title Back + Icons", group = "Navigation")
@Composable
fun NavigationBarPreview_TitleBack() {
    val context = LocalContext.current
    AppTheme {
        AppSurface {
            NavigationBar("Activity", {
                Toast.makeText(context, "Back Button Clicked", Toast.LENGTH_SHORT).show()
            }, listOf(
                NavigationBarButton.Icon(
                    drawable = R.drawable.ic_bottom_nav_buy
                ) {
                    Toast.makeText(context, "First Icon Clicked", Toast.LENGTH_SHORT).show()
                },
                NavigationBarButton.Icon(
                    drawable = R.drawable.ic_bottom_nav_activity
                ) {
                    Toast.makeText(context, "Second Icon Clicked", Toast.LENGTH_SHORT).show()
                }
            ))
        }
    }
}

@Preview(name = "NavigationBar Text Button", group = "Navigation")
@Composable
fun NavigationBarPreview3() {
    val context = LocalContext.current
    AppTheme {
        AppSurface {
            NavigationBar("Test", {
                Toast.makeText(context, "Back Button Clicked", Toast.LENGTH_SHORT).show()
            }, listOf(
                NavigationBarButton.Text(
                    text = "Cancel"
                ) {
                    Toast.makeText(context, "Text Clicked", Toast.LENGTH_SHORT).show()
                }
            ))
        }
    }
}