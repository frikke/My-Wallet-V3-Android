package piuk.blockchain.android.ui.coinview.presentation.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.SecondaryButton
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.toImageResource
import com.blockchain.componentlib.utils.value
import com.blockchain.data.DataResource
import com.blockchain.domain.swap.SwapOption
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewQuickAction
import piuk.blockchain.android.ui.coinview.presentation.logo
import piuk.blockchain.android.ui.coinview.presentation.name

@Composable
fun BottomQuickActions(
    assetTicker: String,
    data: DataResource<List<CoinviewQuickAction>>,
    onQuickActionClick: (CoinviewQuickAction) -> Unit
) {
    when (data) {
        DataResource.Loading,
        is DataResource.Error -> Empty()

        is DataResource.Data -> {
            BottomQuickActionData(
                assetTicker = assetTicker,
                data = data,
                onQuickActionClick = onQuickActionClick
            )
        }
    }
}

@Composable
fun BottomQuickActionData(
    assetTicker: String,
    data: DataResource.Data<List<CoinviewQuickAction>>,
    onQuickActionClick: (CoinviewQuickAction) -> Unit
) {
    if (data.data.isEmpty()) {
        Empty()
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = AppColors.backgroundSecondary,
                    shape = RoundedCornerShape(
                        topStart = AppTheme.dimensions.borderRadiiMedium,
                        topEnd = AppTheme.dimensions.borderRadiiMedium
                    )
                )
                .padding(AppTheme.dimensions.smallSpacing)
        ) {
            data.data.forEachIndexed { index, action ->
                SecondaryButton(
                    modifier = Modifier.weight(1F),
                    text = action.name(assetTicker).value(),
                    icon = action.logo().toImageResource().withTint(AppColors.backgroundSecondary),
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
fun PreviewBottomQuickActions_Data_2() {
    BottomQuickActions(
        assetTicker = "ETH",
        data = DataResource.Data(
            listOf(CoinviewQuickAction.Send(), CoinviewQuickAction.Receive(false))
        ),
        onQuickActionClick = {}
    )
}

@Preview(showBackground = true, backgroundColor = 0XFFF0F2F7)
@Composable
fun PreviewBottomQuickActions_Data_1() {
    BottomQuickActions(
        assetTicker = "ETH",
        data = DataResource.Data(
            listOf(CoinviewQuickAction.Get(swapOption = SwapOption.BcdcSwap))
        ),
        onQuickActionClick = {}
    )
}

@Preview(showBackground = true, backgroundColor = 0XFFF0F2F7)
@Composable
fun PreviewQuickActionsBottom_Data_0() {
    BottomQuickActions(
        assetTicker = "ETH",
        data = DataResource.Data(
            listOf()
        ),
        onQuickActionClick = {}
    )
}
