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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
    selectedNavigationItem: ChromeBottomNavigationItem,
    onSelected: (ChromeBottomNavigationItem) -> Unit
) {
    Card(
        modifier = modifier,
        elevation = 15.dp,
        backgroundColor = AppTheme.colors.backgroundSecondary,
        shape = RoundedCornerShape(100.dp)
    ) {
        Row(modifier = Modifier.padding(horizontal = AppTheme.dimensions.mediumSpacing)) {
            navigationItems.forEach { item ->
                Surface(
                    shape = AppTheme.shapes.large,
                    color = AppTheme.colors.backgroundSecondary
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
                                if (item == selectedNavigationItem) {
                                    item.iconSelected
                                } else {
                                    item.iconDefault
                                }
                            ).withTint(AppTheme.colors.title)
                        )

                        Spacer(modifier = Modifier.size(AppTheme.dimensions.composeSmallestSpacing))

                        Text(
                            text = stringResource(item.name),
                            style = AppTheme.typography.micro2,
                            color = AppTheme.colors.title
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
    MultiAppBottomNavigation(
        navigationItems = listOf(
            ChromeBottomNavigationItem.Home,
            ChromeBottomNavigationItem.Prices,
            ChromeBottomNavigationItem.Nft
        ).toImmutableList(),
        selectedNavigationItem = ChromeBottomNavigationItem.Home,
        onSelected = {}
    )
}
