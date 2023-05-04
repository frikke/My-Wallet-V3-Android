package piuk.blockchain.android.ui.coinview.presentation.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.analytics.Analytics
import com.blockchain.analytics.events.LaunchOrigin
import com.blockchain.componentlib.alert.AlertType
import com.blockchain.componentlib.alert.CardAlert
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.Sync
import com.blockchain.componentlib.system.ShimmerLoadingTableRow
import com.blockchain.componentlib.tablerow.ButtonTableRow
import com.blockchain.componentlib.tablerow.DefaultTableRow
import com.blockchain.componentlib.tablerow.TableRowHeader
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.TextValue
import com.blockchain.componentlib.utils.previewAnalytics
import com.blockchain.componentlib.utils.value
import com.blockchain.data.DataResource
import com.blockchain.home.presentation.recurringbuy.RecurringBuysAnalyticsEvents
import org.koin.androidx.compose.get
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.coinview.presentation.CoinviewRecurringBuysState
import piuk.blockchain.android.ui.coinview.presentation.CoinviewRecurringBuysState.Data.CoinviewRecurringBuyState
import piuk.blockchain.android.ui.recurringbuy.RecurringBuyAnalytics

@Composable
fun RecurringBuys(
    analytics: Analytics = get(),
    data: DataResource<CoinviewRecurringBuysState?>,
    assetTicker: String,
    onRecurringBuyUpsellClick: () -> Unit,
    onRecurringBuyItemClick: (String) -> Unit
) {
    if (data is DataResource.Data && data.data == null) return

    Column(
        modifier = Modifier.padding(AppTheme.dimensions.smallSpacing)
    ) {
        TableRowHeader(
            title = stringResource(R.string.recurring_buy_toolbar),
        )
        Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))

        when (data) {
            DataResource.Loading -> {
                RecurringBuysLoading()
            }

            is DataResource.Error -> {
                RecurringBuysError()
            }

            is DataResource.Data -> {
                data.data?.let { rbState ->
                    when (rbState) {
                        CoinviewRecurringBuysState.Upsell -> {
                            RecurringBuysUpsell(
                                analytics = analytics,
                                onRecurringBuyUpsellClick = onRecurringBuyUpsellClick
                            )
                        }

                        is CoinviewRecurringBuysState.Data -> {
                            RecurringBuysData(
                                analytics = analytics,
                                data = rbState,
                                assetTicker = assetTicker,
                                onRecurringBuyItemClick = onRecurringBuyItemClick
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecurringBuysLoading() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = Color.White, shape = RoundedCornerShape(AppTheme.dimensions.borderRadiiMedium))
    ) {
        ShimmerLoadingTableRow()
    }
}

@Composable
fun RecurringBuysError() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = Color.White, shape = RoundedCornerShape(AppTheme.dimensions.borderRadiiMedium))
    ) {
        CardAlert(
            title = stringResource(R.string.coinview_recuring_buy_load_error_title),
            subtitle = stringResource(R.string.coinview_recuring_buy_load_error_subtitle),
            alertType = AlertType.Warning,
            backgroundColor = AppTheme.colors.background,
            isBordered = false,
            isDismissable = false,
        )
    }
}

@Composable
fun RecurringBuysUpsell(
    analytics: Analytics = get(),
    onRecurringBuyUpsellClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(AppTheme.dimensions.borderRadiiMedium)
    ) {

        val title = stringResource(R.string.recurring_buy_automate_title)

        ButtonTableRow(
            title = title,
            subtitle = stringResource(R.string.recurring_buy_automate_description),
            imageResource = Icons.Filled.Sync.withTint(AppTheme.colors.primary),
            actionText = stringResource(R.string.common_go),
            onClick = {
                analytics.logEvent(RecurringBuyAnalytics.RecurringBuyLearnMoreClicked(LaunchOrigin.CURRENCY_PAGE))
                analytics.logEvent(RecurringBuyAnalytics.RecurringBuyLearnMoreXSellClicked(dcaTitle = title))
                analytics.logEvent(RecurringBuysAnalyticsEvents.CoinviewCtaClicked)
                onRecurringBuyUpsellClick()
            }
        )
    }
}

@Composable
fun RecurringBuysData(
    analytics: Analytics = get(),
    data: CoinviewRecurringBuysState.Data,
    assetTicker: String,
    onRecurringBuyItemClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = Color.White, shape = RoundedCornerShape(AppTheme.dimensions.borderRadiiMedium))
    ) {
        data.recurringBuys.forEachIndexed { index, recurringBuy ->
            DefaultTableRow(
                primaryText = recurringBuy.description.value(),
                secondaryText = recurringBuy.status.value(),
                startImageResource = ImageResource.Local(
                    id = R.drawable.ic_tx_rb,
                    colorFilter = ColorFilter.tint(
                        Color(android.graphics.Color.parseColor(recurringBuy.assetColor))
                    ),
                    shape = CircleShape
                ),
                backgroundColor = Color.Transparent,
                onClick = {
                    analytics.logEvent(
                        RecurringBuyAnalytics.RecurringBuyDetailsClicked(
                            LaunchOrigin.CURRENCY_PAGE,
                            assetTicker
                        )
                    )

                    onRecurringBuyItemClick(recurringBuy.id)
                }
            )

            if (data.recurringBuys.lastIndex != index) {
                Divider(color = Color(0XFFF1F2F7))
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0XFFF0F2F7)
@Composable
fun PreviewRecurringBuys_Loading() {
    RecurringBuys(
        previewAnalytics,
        DataResource.Loading, assetTicker = "ETH", {}, {}
    )
}

@Preview(showBackground = true, backgroundColor = 0XFFF0F2F7)
@Composable
fun PreviewRecurringBuys_Error() {
    RecurringBuys(
        previewAnalytics,
        DataResource.Error(Exception()), assetTicker = "ETH", {}, {}
    )
}

@Preview(showBackground = true, backgroundColor = 0XFFF0F2F7)
@Composable
fun PreviewRecurringBuys_Upsell() {
    RecurringBuys(
        previewAnalytics,
        DataResource.Data(CoinviewRecurringBuysState.Upsell), assetTicker = "ETH", {}, {}
    )
}

@Preview(showBackground = true, backgroundColor = 0XFFF0F2F7)
@Composable
fun PreviewRecurringBuys_Data() {
    RecurringBuys(
        previewAnalytics,
        DataResource.Data(
            CoinviewRecurringBuysState.Data(
                listOf(
                    CoinviewRecurringBuyState(
                        id = "1",
                        description = TextValue.StringValue("RecurringBuyState description 1"),
                        status = TextValue.StringValue("RecurringBuyState status 1"),
                        assetColor = "#2949F8"
                    ),
                    CoinviewRecurringBuyState(
                        id = "2",
                        description = TextValue.StringValue("RecurringBuyState description 2"),
                        status = TextValue.StringValue("RecurringBuyState status 2"),
                        assetColor = "#2949F8"
                    ),
                    CoinviewRecurringBuyState(
                        id = "3",
                        description = TextValue.StringValue("RecurringBuyState description 3"),
                        status = TextValue.StringValue("RecurringBuyState status 3"),
                        assetColor = "#2949F8"
                    )
                )
            )
        ),
        assetTicker = "ETH",
        {}, {}
    )
}
