package piuk.blockchain.android.ui.coinview.presentation.composable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.SecondaryButton
import com.blockchain.componentlib.theme.AppTheme
import piuk.blockchain.android.ui.coinview.presentation.CoinviewBottomQuickActionsState
import piuk.blockchain.android.ui.coinview.presentation.CoinviewQuickActionState

@Composable
fun BottomQuickActions(
    data: CoinviewBottomQuickActionsState
) {
    when (data) {
        CoinviewBottomQuickActionsState.Loading -> {
            BottomQuickActionLoading()
        }

        is CoinviewBottomQuickActionsState.Data -> {
            BottomQuickActionData(
                data = data
            )
        }
    }
}

@Composable
fun BottomQuickActionLoading() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Separator()

        Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTheme.dimensions.smallSpacing)
        ) {
            SecondaryButton(
                modifier = Modifier.weight(1F),
                text = "",
                state = ButtonState.Loading,
                onClick = {}
            )

            Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))

            SecondaryButton(
                modifier = Modifier.weight(1F),
                text = "",
                state = ButtonState.Loading,
                onClick = {}
            )
        }
    }
}

@Composable
fun BottomQuickActionData(
    data: CoinviewBottomQuickActionsState.Data
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Separator()

        Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTheme.dimensions.smallSpacing)
        ) {
            SecondaryButton(
                modifier = Modifier.weight(1F),
                text = data.start.name.value(),
                icon = ImageResource.Local(
                    data.start.logo.value,
                    colorFilter = ColorFilter.tint(AppTheme.colors.background),
                    size = AppTheme.dimensions.standardSpacing
                ),
                state = if (data.start.enabled) ButtonState.Enabled else ButtonState.Disabled,
                onClick = { /*todo*/ }
            )

            Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))

            SecondaryButton(
                modifier = Modifier.weight(1F),
                text = data.end.name.value(),
                icon = ImageResource.Local(
                    data.end.logo.value,
                    colorFilter = ColorFilter.tint(AppTheme.colors.background),
                    size = AppTheme.dimensions.standardSpacing
                ),
                state = if (data.end.enabled) ButtonState.Enabled else ButtonState.Disabled,
                onClick = { /*todo*/ }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewBottomQuickActions_Loading() {
    BottomQuickActions(
        CoinviewBottomQuickActionsState.Loading
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewBottomQuickActions_Data_Enabled() {
    BottomQuickActions(
        CoinviewBottomQuickActionsState.Data(
            start = CoinviewQuickActionState.Send(true),
            end = CoinviewQuickActionState.Receive(true)
        )
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewBottomQuickActions_Data_Disabled() {
    BottomQuickActions(
        CoinviewBottomQuickActionsState.Data(
            start = CoinviewQuickActionState.Buy(false),
            end = CoinviewQuickActionState.Sell(false)
        )
    )
}
