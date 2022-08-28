package piuk.blockchain.android.ui.coinview.presentation.composable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.theme.AppTheme
import piuk.blockchain.android.ui.coinview.presentation.CoinviewQuickActionState
import piuk.blockchain.android.ui.coinview.presentation.CoinviewQuickActionsCenterState

@Composable
fun QuickActionsCenter(
    data: CoinviewQuickActionsCenterState,
    onQuickActionClick: (CoinviewQuickActionState) -> Unit
) {
    when (data) {
        CoinviewQuickActionsCenterState.NotSupported -> {
            Empty()
        }

        CoinviewQuickActionsCenterState.Loading -> {
            Empty()
        }

        is CoinviewQuickActionsCenterState.Data -> {
            QuickActionsCenterData(
                data = data,
                onQuickActionClick = onQuickActionClick
            )
        }
    }
}

@Composable
fun QuickActionsCenterData(
    data: CoinviewQuickActionsCenterState.Data,
    onQuickActionClick: (CoinviewQuickActionState) -> Unit
) {
    if (data.center !is CoinviewQuickActionState.None) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTheme.dimensions.paddingLarge)
        ) {

            PrimaryButton(
                modifier = Modifier.fillMaxWidth(),
                text = data.center.name.value(),
                icon = ImageResource.Local(
                    data.center.logo.value,
                    colorFilter = ColorFilter.tint(AppTheme.colors.background),
                    size = AppTheme.dimensions.paddingLarge
                ),
                state = if (data.center.enabled) ButtonState.Enabled else ButtonState.Disabled,
                onClick = { onQuickActionClick(data.center) }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewQuickActionsCenter_Data_Enabled() {
    QuickActionsCenter(
        CoinviewQuickActionsCenterState.Data(
            center = CoinviewQuickActionState.Swap(true)
        ),
        {}
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewQuickActionsCenter_Data_Disabled() {
    QuickActionsCenter(
        CoinviewQuickActionsCenterState.Data(
            center = CoinviewQuickActionState.Swap(false)
        ),
        {}
    )
}
