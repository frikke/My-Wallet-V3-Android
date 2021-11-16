package com.blockchain.componentlib.control

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Grey400
import com.blockchain.componentlib.theme.Grey600

@Composable
fun TabLayoutLarge(
    items: List<String>,
    onItemSelected: (index: Int) -> Unit,
    modifier: Modifier = Modifier,
    selectedItemIndex: Int = 0,
) {
    TabRow(
        selectedTabIndex = selectedItemIndex,
        backgroundColor = Color.Transparent,
        contentColor = AppTheme.colors.primary,
        modifier = modifier,
    ) {
        items.forEachIndexed { index, itemName ->
            TabLayoutItem(
                itemName = itemName,
                isSelected = selectedItemIndex == index,
                modifier = Modifier
                    .clickable { onItemSelected(index) }
                    .padding(vertical = 14.dp)
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
        color = if (isSelected) {
            AppTheme.colors.primary
        } else {
            if (isSystemInDarkTheme()) Grey400 else Grey600
        },
        style = AppTheme.typography.body2,
        modifier = modifier,
        textAlign = TextAlign.Center
    )
}

@Preview
@Composable
private fun TabLayoutLargePreview() {
    var selectedItem by remember { mutableStateOf(0) }
    AppTheme {
        AppSurface {
            TabLayoutLarge(
                items = listOf("First", "Second", "Third"),
                onItemSelected = { index -> selectedItem = index },
                modifier = Modifier.fillMaxWidth(),
                selectedItemIndex = selectedItem
            )
        }
    }
}
