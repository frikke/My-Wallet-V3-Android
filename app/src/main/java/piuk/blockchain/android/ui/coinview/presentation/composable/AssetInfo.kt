package piuk.blockchain.android.ui.coinview.presentation.composable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.button.SmallMinimalButton
import com.blockchain.componentlib.expandables.ExpandableItem
import com.blockchain.componentlib.system.ShimmerLoadingTableRow
import com.blockchain.componentlib.theme.AppTheme
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.coinview.presentation.CoinviewAssetInfoState
import piuk.blockchain.android.ui.coinview.presentation.ValueAvailability

@Composable
fun AssetInfo(
    data: CoinviewAssetInfoState,
    onWebsiteClick: () -> Unit
) {
    when (data) {
        CoinviewAssetInfoState.Loading -> {
            AssetInfoLoading()
        }

        CoinviewAssetInfoState.Error -> {
            Empty()
        }

        is CoinviewAssetInfoState.Data -> {
            AssetInfoData(
                data = data,
                onWebsiteClick = onWebsiteClick
            )
        }
    }
}

@Composable
fun AssetInfoLoading() {
    Column(modifier = Modifier.fillMaxWidth()) {
        ShimmerLoadingTableRow(showIconLoader = true)
    }
}

@Composable
fun AssetInfoData(
    data: CoinviewAssetInfoState.Data,
    onWebsiteClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(AppTheme.dimensions.paddingLarge)
    ) {

        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = AppTheme.dimensions.xPaddingSmall),
            text = stringResource(R.string.coinview_about_asset, data.assetName),
            style = AppTheme.typography.body2,
            color = AppTheme.colors.title,
        )

        Spacer(modifier = Modifier.size(AppTheme.dimensions.paddingSmall))

        when (data.description) {
            is ValueAvailability.Available -> {
                ExpandableItem(
                    text = data.description.value,
                    numLinesVisible = 6,
                    textButtonToExpand = stringResource(R.string.coinview_expandable_button),
                    textButtonToCollapse = stringResource(R.string.coinview_collapsable_button)
                )
            }

            ValueAvailability.NotAvailable -> {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = AppTheme.dimensions.xPaddingSmall),
                    text = stringResource(R.string.coinview_no_asset_description),
                    style = AppTheme.typography.paragraph1,
                    color = AppTheme.colors.body,
                )
            }
        }

        when (data.website) {
            is ValueAvailability.Available -> {
                Spacer(modifier = Modifier.size(AppTheme.dimensions.paddingSmall))

                SmallMinimalButton(
                    text = stringResource(R.string.coinview_asset_info_cta),
                    onClick = { /*TODO*/ }
                )
            }

            ValueAvailability.NotAvailable -> {
                Empty()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewAssetInfo_Loading() {
    AssetInfo(CoinviewAssetInfoState.Loading, {})
}

@Preview(showBackground = true)
@Composable
fun PreviewAssetInfo_Data() {
    AssetInfo(
        CoinviewAssetInfoState.Data(
            assetName = "ETH",
            description = ValueAvailability.Available(
                "Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to make a type specimen book. It has survived not only five centuries, but also the leap into electronic typesetting, remaining essentially unchanged. It was popularised in the 1960s with the release of Letraset sheets containing Lorem Ipsum passages, and more recently with desktop publishing software like Aldus PageMaker including versions of Lorem Ipsum."
            ),
            website = ValueAvailability.NotAvailable
        ),
        {}
    )
}
