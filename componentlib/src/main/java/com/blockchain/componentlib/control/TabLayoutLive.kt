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
import androidx.compose.material.TabRow
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
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

// This composable assumes the first item given is the Live item
@Composable
fun TabLayoutLive(
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

            if (index == 0 && showLiveIndicator) {
                LiveTabLayoutItem(
                    itemName = itemName,
                    isSelected = isSelected,
                    modifier = Modifier.clickable { onItemSelected(index) }
                )
            } else {
                TabLayoutItem(
                    itemName = itemName,
                    isSelected = isSelected,
                    modifier = Modifier
                        .clickable { onItemSelected(index) }
                        .padding(vertical = 14.dp)
                )
            }
        }
    }
}

@Composable
private fun LiveTabLayoutItem(
    itemName: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    Box(contentAlignment = Alignment.Center) {
        Row(modifier) {
            Box(
                modifier = Modifier
                    .size(dimensionResource(com.blockchain.componentlib.R.dimen.smallest_spacing))
                    .background(
                        color = if (isSelected) AppColors.success else AppColors.muted,
                        shape = CircleShape
                    )
                    .align(Alignment.CenterVertically)
            )
            Spacer(Modifier.width(dimensionResource(com.blockchain.componentlib.R.dimen.minuscule_spacing)))
            TabLayoutItem(
                itemName = itemName,
                isSelected = isSelected,
                modifier = Modifier.padding(vertical = 14.dp)
            )
        }
    }
}

@Composable
private fun TabLayoutItem(
    itemName: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    Text(
        text = itemName,
        color = if (isSelected) AppTheme.colors.title else AppColors.muted,
        style = AppTheme.typography.paragraph2,
        modifier = modifier,
        textAlign = TextAlign.Center
    )
}

@Preview
@Composable
private fun TabLayoutLivePreview() {
    var selectedItem by remember { mutableStateOf(1) }
    AppTheme {
        AppSurface {
            TabLayoutLive(
                items = listOf("Live", "1D", "1W", "1M", "1Y", "All"),
                onItemSelected = { index -> selectedItem = index },
                modifier = Modifier.fillMaxWidth(),
                selectedItemIndex = selectedItem
            )
        }
    }
}
