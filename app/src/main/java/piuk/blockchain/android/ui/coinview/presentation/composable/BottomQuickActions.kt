package piuk.blockchain.android.ui.coinview.presentation.composable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.SecondaryButton
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.value
import piuk.blockchain.android.ui.coinview.presentation.CoinviewBottomQuickActionsState
import piuk.blockchain.android.ui.coinview.presentation.CoinviewQuickActionState

@Composable
fun BottomQuickActions(
    data: CoinviewBottomQuickActionsState,
    onQuickActionClick: (CoinviewQuickActionState) -> Unit
) {
    when (data) {
        CoinviewBottomQuickActionsState.NotSupported -> {
            Empty()
        }

        CoinviewBottomQuickActionsState.Loading -> {
            BottomQuickActionLoading()
        }

        is CoinviewBottomQuickActionsState.Data -> {
            BottomQuickActionData(
                data = data,
                onQuickActionClick = onQuickActionClick
            )
        }
    }
}

@Composable
fun BottomQuickActionLoading() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Divider(color = Color(0XFFF1F2F7))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTheme.dimensions.mediumSpacing)
        ) {
            SecondaryButton(
                modifier = Modifier.weight(1F),
                text = "",
                state = ButtonState.Loading,
                onClick = {}
            )

            Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))

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
    data: CoinviewBottomQuickActionsState.Data,
    onQuickActionClick: (CoinviewQuickActionState) -> Unit
) {
    val atLeastOneButton =
        data.start !is CoinviewQuickActionState.None || data.end !is CoinviewQuickActionState.None

    if (atLeastOneButton) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Divider(color = Color(0XFFF1F2F7))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppTheme.dimensions.mediumSpacing)
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
                            size = AppTheme.dimensions.standardSpacing
                        ),
                        state = if (data.start.enabled) ButtonState.Enabled else ButtonState.Disabled,
                        onClick = { onQuickActionClick(data.start) }
                    )
                }

                if (showSpacer) {
                    Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))
                }

                if (data.end !is CoinviewQuickActionState.None) {
                    SecondaryButton(
                        modifier = Modifier.weight(1F),
                        text = data.end.name.value(),
                        icon = ImageResource.Local(
                            data.end.logo.value,
                            colorFilter = ColorFilter.tint(AppTheme.colors.background),
                            size = AppTheme.dimensions.standardSpacing
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
fun PreviewBottomQuickActions_Loading() {
    BottomQuickActions(
        CoinviewBottomQuickActionsState.Loading, {}
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewBottomQuickActions_Data_Enabled() {
    BottomQuickActions(
        CoinviewBottomQuickActionsState.Data(
            start = CoinviewQuickActionState.Send(true),
            end = CoinviewQuickActionState.Receive(true)
        ),
        {}
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewBottomQuickActions_Data_Disabled() {
    BottomQuickActions(
        CoinviewBottomQuickActionsState.Data(
            start = CoinviewQuickActionState.Buy(false),
            end = CoinviewQuickActionState.Sell(false)
        ),
        {}
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewQuickActionsBottom_Data_1None() {
    BottomQuickActions(
        CoinviewBottomQuickActionsState.Data(
            start = CoinviewQuickActionState.None,
            end = CoinviewQuickActionState.Sell(false)
        ),
        {}
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewQuickActionsBottom_Data_None() {
    BottomQuickActions(
        CoinviewBottomQuickActionsState.Data(
            start = CoinviewQuickActionState.None,
            end = CoinviewQuickActionState.None
        ),
        {}
    )
}
