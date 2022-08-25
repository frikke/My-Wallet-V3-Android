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
fun QuickActionCenter(
    data: CoinviewQuickActionsCenterState
) {
    when (data) {
        CoinviewQuickActionsCenterState.Loading -> {
            Empty()
        }

        CoinviewQuickActionsCenterState.Error -> {
            Empty()
        }

        is CoinviewQuickActionsCenterState.Data -> {
            AssetAccountsCenterData(
                data = data
            )
        }
    }
}

@Composable
fun AssetAccountsCenterData(
    data: CoinviewQuickActionsCenterState.Data
) {
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
            onClick = { /*todo*/ }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewQuickActionCenter_Data_Enabled() {
    QuickActionCenter(
        CoinviewQuickActionsCenterState.Data(
            center = CoinviewQuickActionState.Swap(true)
        )
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewQuickActionCenter_Data_Disabled() {
    QuickActionCenter(
        CoinviewQuickActionsCenterState.Data(
            center = CoinviewQuickActionState.Swap(false)
        )
    )
}
