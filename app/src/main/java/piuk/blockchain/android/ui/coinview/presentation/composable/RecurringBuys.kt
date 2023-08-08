package piuk.blockchain.android.ui.coinview.presentation.composable

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.analytics.Analytics
import com.blockchain.analytics.events.LaunchOrigin
import com.blockchain.componentlib.alert.AlertType
import com.blockchain.componentlib.alert.CardAlert
import com.blockchain.componentlib.basic.AppDivider
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.Sync
import com.blockchain.componentlib.system.ShimmerLoadingTableRow
import com.blockchain.componentlib.tablerow.ActionTableRow
import com.blockchain.componentlib.tablerow.ButtonTableRow
import com.blockchain.componentlib.tablerow.TableRowHeader
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.TextValue
import com.blockchain.componentlib.utils.previewAnalytics
import com.blockchain.componentlib.utils.value
import com.blockchain.data.DataResource
import com.blockchain.home.presentation.recurringbuy.RecurringBuysAnalyticsEvents
import org.koin.androidx.compose.get
import piuk.blockchain.android.ui.coinview.presentation.CoinviewRecurringBuysState
import piuk.blockchain.android.ui.coinview.presentation.CoinviewRecurringBuysState.Data.CoinviewRecurringBuyState
import piuk.blockchain.android.ui.recurringbuy.RecurringBuyAnalytics

@Composable
fun RecurringBuys(
    analytics: Analytics = get(),
    rBuysState: DataResource<CoinviewRecurringBuysState>,
    assetTicker: String,
    onRecurringBuyUpsellClick: () -> Unit,
    onRecurringBuyItemClick: (String) -> Unit
) {
    Column {
        when (rBuysState) {
            DataResource.Loading -> {
                RecurringBuysTitle()
                Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))
                RecurringBuysLoading()
            }

            is DataResource.Error -> {
                RecurringBuysTitle()
                Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))
                RecurringBuysError()
            }

            is DataResource.Data -> {
                rBuysState.data.let { rbState ->
                    when (rbState) {
                        CoinviewRecurringBuysState.Upsell -> {
                            RecurringBuysTitle()
                            Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))
                            RecurringBuysUpsell(
                                analytics = analytics,
                                onRecurringBuyUpsellClick = onRecurringBuyUpsellClick
                            )
                        }

                        is CoinviewRecurringBuysState.Data -> {
                            if (rbState.recurringBuys.isNotEmpty()) {
                                RecurringBuysTitle()
                                Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))
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
}

@Composable
private fun RecurringBuysTitle() {
    TableRowHeader(
        title = stringResource(com.blockchain.stringResources.R.string.recurring_buy_toolbar)
    )
}

@Composable
private fun RecurringBuysLoading() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = AppColors.backgroundSecondary, shape = RoundedCornerShape(AppTheme.dimensions.borderRadiiMedium)
            )
    ) {
        ShimmerLoadingTableRow()
    }
}

@Composable
fun RecurringBuysError() {
    CardAlert(
        title = stringResource(com.blockchain.stringResources.R.string.coinview_recuring_buy_load_error_title),
        subtitle = stringResource(
            com.blockchain.stringResources.R.string.coinview_recuring_buy_load_error_subtitle
        ),
        alertType = AlertType.Warning,
        isBordered = false,
        isDismissable = false
    )
}

@Composable
fun RecurringBuysUpsell(
    analytics: Analytics = get(),
    onRecurringBuyUpsellClick: () -> Unit
) {
    Surface(
        color = AppColors.backgroundSecondary,
        shape = AppTheme.shapes.large
    ) {
        val title = stringResource(com.blockchain.stringResources.R.string.recurring_buy_automate_title)

        ButtonTableRow(
            title = title,
            subtitle = stringResource(com.blockchain.stringResources.R.string.recurring_buy_automate_description),
            imageResource = Icons.Filled.Sync.withTint(AppColors.muted),
            actionText = stringResource(com.blockchain.stringResources.R.string.common_go),
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
    Surface(
        color = AppColors.backgroundSecondary,
        shape = AppTheme.shapes.large
    ) {
        Column {
            data.recurringBuys.forEachIndexed { index, recurringBuy ->
                ActionTableRow(
                    title = recurringBuy.description.value(),
                    subtitle = recurringBuy.status.value(),
                    icon = StackedIcon.SingleIcon(Icons.Filled.Sync.withTint(AppColors.muted)),
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
                    AppDivider()
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0XFFF0F2F7)
@Composable
fun PreviewRecurringBuys_Loading() {
    RecurringBuys(
        previewAnalytics,
        DataResource.Loading,
        assetTicker = "ETH",
        {},
        {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, backgroundColor = 0XFF07080D)
@Composable
private fun PreviewRecurringBuys_LoadingDark() {
    PreviewRecurringBuys_Loading()
}

@Preview(showBackground = true, backgroundColor = 0XFFF0F2F7)
@Composable
fun PreviewRecurringBuys_Error() {
    RecurringBuys(
        previewAnalytics,
        DataResource.Error(Exception()),
        assetTicker = "ETH",
        {},
        {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, backgroundColor = 0XFF07080D)
@Composable
private fun PreviewRecurringBuys_ErrorDark() {
    PreviewRecurringBuys_Error()
}

@Preview(showBackground = true, backgroundColor = 0XFFF0F2F7)
@Composable
fun PreviewRecurringBuys_Upsell() {
    RecurringBuys(
        previewAnalytics,
        DataResource.Data(CoinviewRecurringBuysState.Upsell),
        assetTicker = "ETH",
        {},
        {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, backgroundColor = 0XFF07080D)
@Composable
private fun PreviewRecurringBuys_UpsellDark() {
    PreviewRecurringBuys_Upsell()
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
        {},
        {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, backgroundColor = 0XFF07080D)
@Composable
private fun PreviewRecurringBuys_DataDark() {
    PreviewRecurringBuys_Data()
}
