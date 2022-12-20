package piuk.blockchain.android.ui.coinview.presentation.composable

import androidx.compose.runtime.Composable
import piuk.blockchain.android.ui.coinview.presentation.CoinviewCenterQuickActionsState
import piuk.blockchain.android.ui.coinview.presentation.CoinviewQuickActionState

@Composable
fun CenterQuickActions(
    data: CoinviewCenterQuickActionsState,
    onQuickActionClick: (CoinviewQuickActionState) -> Unit
) {
    when (data) {
        CoinviewCenterQuickActionsState.NotSupported -> {
            Empty()
        }

        CoinviewCenterQuickActionsState.Loading -> {
            Empty()
        }

        is CoinviewCenterQuickActionsState.Data -> {
            CenterQuickActionsData(
                data = data,
                onQuickActionClick = onQuickActionClick
            )
        }
    }
}

@Composable
fun CenterQuickActionsData(
    data: CoinviewCenterQuickActionsState.Data,
    onQuickActionClick: (CoinviewQuickActionState) -> Unit
) {
//    if (data.center !is CoinviewQuickActionState.None) {
//        Column(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(AppTheme.dimensions.standardSpacing)
//        ) {
//
//            TertiaryButton(
//                modifier = Modifier.fillMaxWidth(),
//                text = data.center.name.value(),
//                textColor = AppTheme.colors.title,
//                icon = ImageResource.Local(
//                    data.center.logo.value,
//                    colorFilter = ColorFilter.tint(AppTheme.colors.title),
//                    size = AppTheme.dimensions.standardSpacing
//                ),
//                state = if (data.center.enabled) ButtonState.Enabled else ButtonState.Disabled,
//                onClick = { onQuickActionClick(data.center) }
//            )
//        }
//    }
}
//
//@Preview(showBackground = true)
//@Composable
//fun PreviewCenterQuickActions_Data_Enabled() {
//    CenterQuickActions(
//        CoinviewCenterQuickActionsState.Data(
//            center = CoinviewQuickActionState.Swap(true)
//        ),
//        {}
//    )
//}
//
//@Preview(showBackground = true)
//@Composable
//fun PreviewCenterQuickActions_Data_Disabled() {
//    CenterQuickActions(
//        CoinviewCenterQuickActionsState.Data(
//            center = CoinviewQuickActionState.Swap(false)
//        ),
//        {}
//    )
//}
