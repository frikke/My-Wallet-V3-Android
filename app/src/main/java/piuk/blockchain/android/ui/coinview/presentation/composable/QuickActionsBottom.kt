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
import piuk.blockchain.android.ui.coinview.presentation.CoinviewQuickActionState
import piuk.blockchain.android.ui.coinview.presentation.CoinviewQuickActionsBottomState

@Composable
fun QuickActionsBottom(
    data: CoinviewQuickActionsBottomState,
    onQuickActionClick: (CoinviewQuickActionState) -> Unit
) {
    when (data) {
        CoinviewQuickActionsBottomState.NotSupported -> {
            Empty()
        }

        CoinviewQuickActionsBottomState.Loading -> {
            QuickActionBottomLoading()
        }

        is CoinviewQuickActionsBottomState.Data -> {
            QuickActionBottomData(
                data = data,
                onQuickActionClick = onQuickActionClick
            )
        }
    }
}

@Composable
fun QuickActionBottomLoading() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Separator()

        Spacer(modifier = Modifier.size(AppTheme.dimensions.paddingSmall))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTheme.dimensions.paddingMedium)
        ) {
            SecondaryButton(
                modifier = Modifier.weight(1F),
                text = "",
                state = ButtonState.Loading,
                onClick = {}
            )

            Spacer(modifier = Modifier.size(AppTheme.dimensions.paddingSmall))

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
fun QuickActionBottomData(
    data: CoinviewQuickActionsBottomState.Data,
    onQuickActionClick: (CoinviewQuickActionState) -> Unit
) {
    val atLeastOneButton =
        data.start !is CoinviewQuickActionState.None || data.end !is CoinviewQuickActionState.None

    if (atLeastOneButton) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Separator()

            Spacer(modifier = Modifier.size(AppTheme.dimensions.paddingSmall))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppTheme.dimensions.paddingMedium)
            ) {
                val showSpacer =
                    data.start !is CoinviewQuickActionState.None && data.end !is CoinviewQuickActionState.None

                if (data.start !is CoinviewQuickActionState.None) {
                    SecondaryButton(
                        modifier = Modifier.weight(1F),
                        text = data.start.name.value(),
                        icon = ImageResource.Local(
                            data.start.logo.value,
                            colorFilter = ColorFilter.tint(AppTheme.colors.background),
                            size = AppTheme.dimensions.paddingLarge
                        ),
                        state = if (data.start.enabled) ButtonState.Enabled else ButtonState.Disabled,
                        onClick = { onQuickActionClick(data.start) }
                    )
                }

                if (showSpacer) {
                    Spacer(modifier = Modifier.size(AppTheme.dimensions.paddingSmall))
                }

                if (data.end !is CoinviewQuickActionState.None) {
                    SecondaryButton(
                        modifier = Modifier.weight(1F),
                        text = data.end.name.value(),
                        icon = ImageResource.Local(
                            data.end.logo.value,
                            colorFilter = ColorFilter.tint(AppTheme.colors.background),
                            size = AppTheme.dimensions.paddingLarge
                        ),
                        state = if (data.end.enabled) ButtonState.Enabled else ButtonState.Disabled,
                        onClick = { onQuickActionClick(data.end) }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewQuickActionsBottom_Loading() {
    QuickActionsBottom(
        CoinviewQuickActionsBottomState.Loading, {}
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewQuickActionsBottom_Data_Enabled() {
    QuickActionsBottom(
        CoinviewQuickActionsBottomState.Data(
            start = CoinviewQuickActionState.Send(true),
            end = CoinviewQuickActionState.Receive(true)
        ),
        {}
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewQuickActionsBottom_Data_Disabled() {
    QuickActionsBottom(
        CoinviewQuickActionsBottomState.Data(
            start = CoinviewQuickActionState.Buy(false),
            end = CoinviewQuickActionState.Sell(false)
        ),
        {}
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewQuickActionsBottom_Data_1None() {
    QuickActionsBottom(
        CoinviewQuickActionsBottomState.Data(
            start = CoinviewQuickActionState.None,
            end = CoinviewQuickActionState.Sell(false)
        ),
        {}
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewQuickActionsBottom_Data_None() {
    QuickActionsBottom(
        CoinviewQuickActionsBottomState.Data(
            start = CoinviewQuickActionState.None,
            end = CoinviewQuickActionState.None
        ),
        {}
    )
}
