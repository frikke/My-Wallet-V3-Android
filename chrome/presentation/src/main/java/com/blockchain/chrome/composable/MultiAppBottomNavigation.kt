package com.blockchain.chrome.composable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.blockchain.chrome.ChromeBottomNavigationItem
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.clickableWithIndication
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@Composable
fun MultiAppBottomNavigation(
    modifier: Modifier = Modifier,
    navigationItems: ImmutableList<ChromeBottomNavigationItem>,
    navControllerProvider: () -> NavController,
    onSelected: (ChromeBottomNavigationItem) -> Unit
) {
    Card(
        modifier = modifier,
        elevation = 15.dp,
        contentColor = Color.White,
        shape = RoundedCornerShape(100.dp)
    ) {
        Row(modifier = Modifier.padding(horizontal = AppTheme.dimensions.mediumSpacing)) {
            val navBackStackEntry by navControllerProvider().currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            navigationItems.forEach { item ->
                Surface(
                    shape = AppTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier
                            .clickableWithIndication {
                                onSelected(item)
                            }
                            .padding(
                                horizontal = AppTheme.dimensions.verySmallSpacing,
                                vertical = AppTheme.dimensions.tinySpacing
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            imageResource = ImageResource.Local(
                                if (item.route == currentRoute) {
                                    item.iconSelected
                                } else {
                                    item.iconDefault
                                }
                            )
                        )

                        Spacer(modifier = Modifier.size(AppTheme.dimensions.composeSmallestSpacing))

                        Text(
                            text = stringResource(item.name),
                            style = AppTheme.typography.micro2,
                            color = AppTheme.colors.title,
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun PreviewMultiAppBottomNavigation() {
    val navController = rememberNavController()
    MultiAppBottomNavigation(
        navigationItems = listOf(
            ChromeBottomNavigationItem.Home,
            ChromeBottomNavigationItem.Prices,
            ChromeBottomNavigationItem.Nft
        ).toImmutableList(),
        navControllerProvider = { navController },
        onSelected = {}
    )
}
