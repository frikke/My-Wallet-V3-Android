package com.blockchain.componentlib.control

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.SnackbarDefaults.backgroundColor
import androidx.compose.material.TabRow
import androidx.compose.material.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.R
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Green600
import com.blockchain.componentlib.theme.Grey400
import com.blockchain.componentlib.utils.clickableNoEffect

// This composable assumes the first item given is the Live item
@Composable
fun TabLayoutLiveBoxed(
    items: List<String>,
    onItemSelected: (index: Int) -> Unit,
    modifier: Modifier = Modifier,
    selectedItemIndex: Int = 0,
    showLiveIndicator: Boolean = true
) {
    TabRow(
        selectedTabIndex = selectedItemIndex,
        backgroundColor = Color.Transparent,
        contentColor = AppTheme.colors.primary,
        divider = {},
        indicator = {},
        modifier = modifier
    ) {
        items.forEachIndexed { index, itemName ->
            val isSelected = selectedItemIndex == index

            Box(modifier = Modifier
                .clickableNoEffect { onItemSelected(index) }
                .padding(AppTheme.dimensions.xPaddingSmall)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(AppTheme.dimensions.borderRadiiLarge),
                    backgroundColor = AppTheme.colors.background,
                    elevation = if (isSelected) AppTheme.dimensions.xPaddingSmall else AppTheme.dimensions.paddingZero
                ) {
                    if (index == 0 && showLiveIndicator) {
                        LiveTabBoxedLayoutItem(
                            itemName = itemName,
                            isSelected = isSelected,
                        )
                    } else {
                        TabBoxedLayoutItem(
                            itemName = itemName,
                            isSelected = isSelected,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveTabBoxedLayoutItem(
    itemName: String,
    isSelected: Boolean,
) {
    Box(contentAlignment = Alignment.Center) {
        Row {
            Box(
                modifier = Modifier
                    .size(AppTheme.dimensions.xPaddingSmall)
                    .background(
                        color = if (isSelected) Green600 else Grey400,
                        shape = CircleShape
                    )
                    .align(Alignment.CenterVertically)
            )
            Spacer(Modifier.width(dimensionResource(R.dimen.minuscule_margin)))
            TabBoxedLayoutItem(
                itemName = itemName,
                isSelected = isSelected
            )
        }
    }
}

@Composable
private fun TabBoxedLayoutItem(
    itemName: String,
    isSelected: Boolean,
) {
    Text(
        text = itemName,
        color = if (isSelected) AppTheme.colors.primary else Grey400,
        style = AppTheme.typography.body2,
        modifier = Modifier.padding(vertical = AppTheme.dimensions.xPaddingSmall),
        textAlign = TextAlign.Center
    )
}

@Preview(showBackground = true)
@Composable
private fun TabLayoutLivePreview() {
    var selectedItem by remember { mutableStateOf(1) }
    AppTheme {
        AppSurface {
            TabLayoutLiveBoxed(
                items = listOf("Live", "1D", "1W", "1M", "1Y", "All"),
                onItemSelected = { index -> selectedItem = index },
                modifier = Modifier.fillMaxWidth(),
                selectedItemIndex = selectedItem
            )
        }
    }
}
