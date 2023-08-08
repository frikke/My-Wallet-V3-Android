package piuk.blockchain.android.ui.coinview.presentation.composable

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.MinimalSecondaryButton
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.toImageResource
import com.blockchain.componentlib.utils.value
import com.blockchain.data.DataResource
import com.blockchain.domain.swap.SwapOption
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewQuickAction
import piuk.blockchain.android.ui.coinview.presentation.logo
import piuk.blockchain.android.ui.coinview.presentation.name

@Composable
fun CenterQuickActions(
    assetTicker: String,
    data: DataResource<List<CoinviewQuickAction>>,
    onQuickActionClick: (CoinviewQuickAction) -> Unit
) {
    when (data) {
        DataResource.Loading -> Empty()
        is DataResource.Error -> Empty()
        is DataResource.Data -> {
            CenterQuickActionsData(
                assetTicker = assetTicker,
                data = data,
                onQuickActionClick = onQuickActionClick
            )
        }
    }
}

@Composable
fun CenterQuickActionsData(
    assetTicker: String,
    data: DataResource.Data<List<CoinviewQuickAction>>,
    onQuickActionClick: (CoinviewQuickAction) -> Unit
) {
    if (data.data.isEmpty()) {
        Empty()
    } else {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            data.data.forEachIndexed { index, action ->
                MinimalSecondaryButton(
                    modifier = Modifier
                        .then(if (index < 2) Modifier.weight(1F) else Modifier)
                        .then(if (index < 2) Modifier else Modifier.requiredWidthIn(min = 48.dp)),
                    text = action.name(assetTicker).value().takeIf { index < 2 } ?: "",
                    icon = action.logo().toImageResource(),
                    state = if (action.enabled) ButtonState.Enabled else ButtonState.Disabled,
                    onClick = { onQuickActionClick(action) }
                )

                if (index < data.data.lastIndex) {
                    Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0XFFF0F2F7)
@Composable
fun PreviewCenterQuickActions_Data_3() {
    CenterQuickActions(
        assetTicker = "ETH",
        data = DataResource.Data(
            listOf(
                CoinviewQuickAction.Send(),
                CoinviewQuickAction.Swap(false, SwapOption.BcdcSwap),
                CoinviewQuickAction.Receive()
            )
        ),
        onQuickActionClick = {}
    )
}

@Preview(showBackground = true, backgroundColor = 0XFFF0F2F7)
@Composable
fun PreviewCenterQuickActions_Data_1() {
    CenterQuickActions(
        assetTicker = "ETH",
        data = DataResource.Data(
            listOf(CoinviewQuickAction.Send())
        ),
        onQuickActionClick = {}
    )
}

@Preview(showBackground = true, backgroundColor = 0XFFF0F2F7)
@Composable
fun PreviewQuickActionsCenter_Data_0() {
    CenterQuickActions(
        assetTicker = "ETH",
        data = DataResource.Data(
            listOf()
        ),
        onQuickActionClick = {}
    )
}

@Preview(showBackground = true, backgroundColor = 0XFFF0F2F7)
@Composable
fun PreviewQuickActionsCenter_Data_Error() {
    CenterQuickActions(
        assetTicker = "ETH",
        data = DataResource.Error(Exception()),
        onQuickActionClick = {}
    )
}
