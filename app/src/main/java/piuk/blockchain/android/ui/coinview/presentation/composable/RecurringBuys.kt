package piuk.blockchain.android.ui.coinview.presentation.composable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.alert.AlertType
import com.blockchain.componentlib.alert.CardAlert
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.card.ButtonType
import com.blockchain.componentlib.card.CardButton
import com.blockchain.componentlib.card.DefaultCard
import com.blockchain.componentlib.sectionheader.SmallSectionHeader
import com.blockchain.componentlib.system.ShimmerLoadingTableRow
import com.blockchain.componentlib.tablerow.DefaultTableRow
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.TextValue
import com.blockchain.componentlib.utils.value
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.coinview.presentation.CoinviewRecurringBuysState
import piuk.blockchain.android.ui.coinview.presentation.CoinviewRecurringBuysState.Data.CoinviewRecurringBuyState

@Composable
fun RecurringBuys(
    data: CoinviewRecurringBuysState,
    onRecurringBuyUpsellClick: () -> Unit,
    onRecurringBuyItemClick: (String) -> Unit
) {
    when (data) {
        CoinviewRecurringBuysState.NotSupported -> {
            Empty()
        }

        CoinviewRecurringBuysState.Loading -> {
            RecurringBuysLoading()
        }

        CoinviewRecurringBuysState.Error -> {
            RecurringBuysError()
        }

        CoinviewRecurringBuysState.Upsell -> {
            RecurringBuysUpsell(onRecurringBuyUpsellClick = onRecurringBuyUpsellClick)
        }

        is CoinviewRecurringBuysState.Data -> {
            RecurringBuysData(data, onRecurringBuyItemClick = onRecurringBuyItemClick)
        }
    }
}

@Composable
fun RecurringBuysLoading() {
    Column(modifier = Modifier.fillMaxWidth()) {
        ShimmerLoadingTableRow()
    }
}

@Composable
fun RecurringBuysError() {
    Column(modifier = Modifier.fillMaxWidth()) {
        // header
        SmallSectionHeader(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.dashboard_recurring_buy_title)
        )

        // error
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = AppTheme.dimensions.standardSpacing,
                    vertical = AppTheme.dimensions.smallSpacing
                )
        ) {
            CardAlert(
                title = stringResource(R.string.coinview_recuring_buy_load_error_title),
                subtitle = stringResource(R.string.coinview_recuring_buy_load_error_subtitle),
                alertType = AlertType.Warning,
                isBordered = true,
                isDismissable = false,
            )
        }
    }
}

@Composable
fun RecurringBuysUpsell(
    onRecurringBuyUpsellClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = AppTheme.dimensions.standardSpacing,
                vertical = AppTheme.dimensions.smallSpacing
            )
    ) {
        DefaultCard(
            title = stringResource(R.string.coinview_rb_card_title),
            subtitle = stringResource(R.string.coinview_rb_card_blurb),
            iconResource = ImageResource.LocalWithBackground(
                R.drawable.ic_tx_recurring_buy, R.color.blue_600, R.color.blue_200
            ),
            callToActionButton = CardButton(
                text = stringResource(R.string.common_learn_more),
                type = ButtonType.Minimal,
                onClick = onRecurringBuyUpsellClick
            ),
            isDismissable = false
        )
    }
}

@Composable
fun RecurringBuysData(
    data: CoinviewRecurringBuysState.Data,
    onRecurringBuyItemClick: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // header
        SmallSectionHeader(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.dashboard_recurring_buy_title)
        )

        // list
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
                onClick = { onRecurringBuyItemClick(recurringBuy.id) }
            )

            if (data.recurringBuys.lastIndex != index) {
                Separator()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewRecurringBuys_Loading() {
    RecurringBuys(CoinviewRecurringBuysState.Loading, {}, {})
}

@Preview(showBackground = true)
@Composable
fun PreviewRecurringBuys_Error() {
    RecurringBuys(CoinviewRecurringBuysState.Error, {}, {})
}

@Preview(showBackground = true)
@Composable
fun PreviewRecurringBuys_Upsell() {
    RecurringBuys(CoinviewRecurringBuysState.Upsell, {}, {})
}

@Preview(showBackground = true)
@Composable
fun PreviewRecurringBuys_Data() {
    RecurringBuys(
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
        ),
        {}, {}
    )
}
