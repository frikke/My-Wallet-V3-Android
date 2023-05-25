package com.blockchain.home.presentation.allassets.composable

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.sheets.SheetNub
import com.blockchain.componentlib.tablerow.FlexibleToggleTableRow
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.extensions.replace
import com.blockchain.home.domain.AssetFilter

@Composable
fun CryptoAssetsFilters(
    filters: List<AssetFilter>,
    onConfirmClick: (List<AssetFilter>) -> Unit
) {
    var editableFilters by remember(filters) { mutableStateOf(filters) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppTheme.colors.backgroundSecondary)
            .padding(
                start = AppTheme.dimensions.smallSpacing,
                top = AppTheme.dimensions.noSpacing,
                end = AppTheme.dimensions.smallSpacing,
                bottom = AppTheme.dimensions.smallSpacing
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            SheetNub(modifier = Modifier.padding(AppTheme.dimensions.tinySpacing))
        }

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(com.blockchain.stringResources.R.string.assets_filter_title),
            style = AppTheme.typography.body2,
            color = AppTheme.colors.title,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.size(AppTheme.dimensions.standardSpacing))

        Surface(
            color = AppTheme.colors.light,
            shape = AppTheme.shapes.large
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                editableFilters.forEach { assetFilter ->
                    when (assetFilter) {
                        is AssetFilter.ShowSmallBalances -> SmallBalancesFilter(assetFilter) { isChecked ->
                            editableFilters = editableFilters.replace(
                                assetFilter,
                                AssetFilter.ShowSmallBalances(isChecked)
                            )
                        }

                        is AssetFilter.SearchFilter -> {
                            // Do nothing
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.size(AppTheme.dimensions.largeSpacing))

        PrimaryButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(com.blockchain.stringResources.R.string.common_confirm),
            onClick = { onConfirmClick(editableFilters) }
        )
    }
}

@Composable
fun SmallBalancesFilter(assetFilter: AssetFilter.ShowSmallBalances, onCheckedChanged: (Boolean) -> Unit) {
    FlexibleToggleTableRow(
        paddingValues = PaddingValues(horizontal = AppTheme.dimensions.smallSpacing),
        isChecked = assetFilter.enabled,
        primaryText = stringResource(com.blockchain.stringResources.R.string.assets_filter_small_balances),
        onCheckedChange = { isChecked ->
            onCheckedChanged(isChecked)
        },
        backgroundColor = Color.Transparent
    )
}

@Preview
@Composable
private fun PreviewCryptoAssetsFiltersScreen() {
    CryptoAssetsFilters(
        filters = listOf(
            AssetFilter.ShowSmallBalances(true)
        ),
        onConfirmClick = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewCryptoAssetsFiltersScreenDark() {
    PreviewCryptoAssetsFiltersScreen()
}