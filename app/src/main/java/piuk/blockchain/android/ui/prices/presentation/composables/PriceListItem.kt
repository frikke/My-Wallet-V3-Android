package piuk.blockchain.android.ui.prices.presentation.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.foundation.layout.size
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.tablerow.TableRow
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.core.price.ExchangeRate
import com.blockchain.walletconnect.R
import piuk.blockchain.android.ui.dashboard.asPercentString
import piuk.blockchain.android.ui.dashboard.format
import piuk.blockchain.android.ui.prices.PricesItem

@Composable
fun PriceListItem(
    priceItem: PricesItem,
    onClick: () -> Unit,
) {

    val accessAssetName = stringResource(piuk.blockchain.android.R.string.accessibility_asset_name)
    val accessCurrentMarketPrice =
        stringResource(piuk.blockchain.android.R.string.accessibility_current_market_price)
    val access24hChange = stringResource(piuk.blockchain.android.R.string.accessibility_24h_change)
    val accessPriceNotAvailable =
        stringResource(piuk.blockchain.android.R.string.accessibility_price_not_available)

    TableRow(
        contentStart = {
            Image(
                imageResource = ImageResource.Remote(
                    priceItem.assetInfo.logo
                ),
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .size(dimensionResource(com.blockchain.componentlib.R.dimen.standard_margin))
            )
        },

        content = {
            dimensionResource(com.blockchain.componentlib.R.dimen.medium_margin)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = dimensionResource(com.blockchain.componentlib.R.dimen.medium_margin), end = 8.dp)
            ) {
                Text(
                    text = priceItem.assetInfo.name,
                    style = AppTheme.typography.body2,
                    color = AppTheme.colors.title,
                    modifier = Modifier.semantics {
                        contentDescription = "$accessAssetName " + priceItem.assetInfo.name
                    }
                )
                Row {
                    Text(
                        modifier = Modifier.semantics {
                            this.contentDescription = "$access24hChange " +
                                if (priceItem.priceWithDelta == null) {
                                    accessPriceNotAvailable
                                } else {
                                    val rate = priceItem.priceWithDelta.currentRate as? ExchangeRate
                                    rate?.price.format(priceItem.fiatCurrency)
                                }
                        },
                        text = if (priceItem.priceWithDelta == null) {
                            "--"
                        } else {
                            val rate = priceItem.priceWithDelta.currentRate as? ExchangeRate
                            rate?.price.format(priceItem.fiatCurrency)
                        },
                        style = AppTheme.typography.paragraph1,
                        color = AppTheme.colors.body
                    )
                    Text(
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .semantics {
                                this.contentDescription = "$accessCurrentMarketPrice " +
                                    if (priceItem.priceWithDelta == null || priceItem.priceWithDelta.delta24h.isNaN()) {
                                        accessPriceNotAvailable
                                    } else {
                                        priceItem.priceWithDelta.delta24h.asPercentString()
                                    }
                            },
                        text = if (priceItem.priceWithDelta == null || priceItem.priceWithDelta.delta24h.isNaN()) {
                            "--"
                        } else {
                            priceItem.priceWithDelta.delta24h.asPercentString()
                        },
                        style = AppTheme.typography.paragraph1,
                        color = if (priceItem.priceWithDelta == null ||
                            priceItem.priceWithDelta.delta24h.isNaN()
                        ) {
                            AppTheme.colors.success
                        } else {
                            AppTheme.colors.error
                        }
                    )
                }
            }
        },
        contentEnd = {
            Image(
                imageResource = ImageResource.Local(R.drawable.ic_chevron_end),
                modifier = Modifier.requiredSizeIn(
                    maxWidth = dimensionResource(com.blockchain.componentlib.R.dimen.standard_margin),
                    maxHeight = dimensionResource(com.blockchain.componentlib.R.dimen.standard_margin),
                ),
            )
        },
        onContentClicked = onClick,
    )
    Divider(color = AppTheme.colors.light, thickness = 1.dp)
}
