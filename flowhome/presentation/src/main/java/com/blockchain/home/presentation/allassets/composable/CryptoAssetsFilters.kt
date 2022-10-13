package com.blockchain.home.presentation.allassets.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.sheets.SheetNub
import com.blockchain.componentlib.tablerow.FlexibleToggleTableRow
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.home.model.AssetFilter
import com.blockchain.home.model.AssetFilterStatus
import com.blockchain.home.presentation.R
import com.blockchain.utils.replaceInList

@Composable
fun CryptoAssetsFilters(
    filters: List<AssetFilterStatus>,
    onConfirmClick: (List<AssetFilterStatus>) -> Unit
) {
    val editableFilters = remember { filters.toMutableStateList() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                AppTheme.colors.background, RoundedCornerShape(
                    topStart = AppTheme.dimensions.borderRadiiMedium,
                    topEnd = AppTheme.dimensions.borderRadiiMedium
                )
            )
            .padding(
                start = AppTheme.dimensions.smallSpacing,
                top = AppTheme.dimensions.noSpacing,
                end = AppTheme.dimensions.smallSpacing,
                bottom = AppTheme.dimensions.smallSpacing,
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
            text = stringResource(R.string.assets_filter_title),
            style = AppTheme.typography.body2,
            color = AppTheme.colors.title,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.size(AppTheme.dimensions.standardSpacing))

        Card(
            backgroundColor = AppTheme.colors.light,
            shape = RoundedCornerShape(AppTheme.dimensions.borderRadiiMedium),
            elevation = 0.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                editableFilters.forEach { assetFilter ->
                    FlexibleToggleTableRow(
                        paddingValues = PaddingValues(horizontal = AppTheme.dimensions.smallSpacing),
                        isChecked = assetFilter.isEnabled,
                        primaryText = stringResource(R.string.assets_filter_small_balances),
                        onCheckedChange = { isChecked ->
                            editableFilters.replaceInList(
                                replacement = assetFilter.copy(isEnabled = isChecked),
                                where = { it.filter == assetFilter.filter }
                            )
                        },
                        backgroundColor = Color.Transparent
                    )
                }
            }
        }

        Spacer(modifier = Modifier.size(AppTheme.dimensions.largeSpacing))

        PrimaryButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.assets_filter_confirmation),
            onClick = { onConfirmClick(editableFilters) }
        )
    }
}

@Preview(backgroundColor = 0xF281DF, showBackground = true)
@Composable
fun CryptoAssetsFiltersScreen(
) {
    CryptoAssetsFilters(
        filters = listOf(
            AssetFilterStatus(filter = AssetFilter.ShowSmallBalances, isEnabled = true)
        ),
        onConfirmClick = {}
    )
}
