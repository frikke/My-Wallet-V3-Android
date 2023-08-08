package piuk.blockchain.android.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.control.CancelableOutlinedSearch
import com.blockchain.componentlib.divider.HorizontalDivider
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import piuk.blockchain.android.R

@Composable
fun SearchPickerItemScreen(
    suggestedPick: PickerItem?,
    items: List<PickerItem>,
    onItemClicked: (PickerItem) -> Unit
) {
    var searchInput by remember { mutableStateOf("") }

    @Suppress("RememberReturnType")
    val filteredItems: List<PickerItem> = remember(searchInput) {
        items.filter { item ->
            item.label.contains(searchInput, ignoreCase = true)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(AppColors.backgroundSecondary)
    ) {
        CancelableOutlinedSearch(
            onValueChange = { searchInput = it },
            placeholder = stringResource(com.blockchain.stringResources.R.string.search)
        )

        Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))

        LazyColumn() {
            if (suggestedPick != null && searchInput.isEmpty()) {
                item {
                    SimpleText(
                        modifier = Modifier.padding(start = AppTheme.dimensions.mediumSpacing),
                        text = stringResource(com.blockchain.stringResources.R.string.country_selection_suggested),
                        style = ComposeTypographies.Caption1,
                        color = ComposeColors.Title,
                        gravity = ComposeGravities.Start
                    )

                    PickerItemContent(suggestedPick, onItemClicked)

                    HorizontalDivider(
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = AppTheme.dimensions.tinySpacing)
                    )
                }
            }

            items(filteredItems) {
                PickerItemContent(it, onItemClicked)
            }
        }
    }
}

@Composable
private fun PickerItemContent(item: PickerItem, onClick: (PickerItem) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(item) }
            .padding(horizontal = AppTheme.dimensions.mediumSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item.icon?.let { icon ->
            Text(
                text = icon,
                fontSize = 30.sp
            )
        }
        Text(
            modifier = Modifier.padding(AppTheme.dimensions.smallSpacing),
            text = item.label,
            fontSize = 14.sp,
            color = AppColors.title,
            fontWeight = FontWeight.Medium
        )
    }
}

@Preview
@Composable
private fun PreviewScreen() {
    val suggestedPick = object : PickerItem {
        override val label: String = "Portugal"
        override val code: String = "PT"
        override val icon: String? = "🇵🇹"
    }
    val items = (0..20).map {
        object : PickerItem {
            override val label: String = "United Kingdom"
            override val code: String = "UK $it"
            override val icon: String? = "🇬🇧"
        }
    }
    SearchPickerItemScreen(
        suggestedPick = suggestedPick,
        items = items,
        onItemClicked = {}
    )
}

@Preview
@Composable
private fun PreviewPickerItemContent() {
    val item = object : PickerItem {
        override val label: String = "United Kingdom"
        override val code: String = "UK"
        override val icon: String? = "🇬🇧"
    }
    PickerItemContent(item, {})
}
