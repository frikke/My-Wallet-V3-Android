package piuk.blockchain.android.ui.multiapp.composable

import androidx.annotation.StringRes
import androidx.compose.foundation.lazy.LazyListState
import com.blockchain.walletmode.WalletMode
import com.google.accompanist.swiperefresh.SwipeRefreshState
import piuk.blockchain.android.R

const val ANIMATION_DURATION = 400

data class ListStateInfo(
    val firstVisibleItemIndex: Int,
    val firstVisibleItemScrollOffset: Int,
    val isSwipeInProgress: Boolean
)

fun extractStatesInfo(
    listState: LazyListState,
    swipeRefreshState: SwipeRefreshState?
): ListStateInfo {
    return ListStateInfo(
        firstVisibleItemIndex = listState.firstVisibleItemIndex,
        firstVisibleItemScrollOffset = listState.firstVisibleItemScrollOffset,
        isSwipeInProgress = swipeRefreshState?.isSwipeInProgress ?: false
    )
}

@StringRes
fun WalletMode.titleSuperApp(): Int = when (this) {
    WalletMode.NON_CUSTODIAL_ONLY -> R.string.defi_wallet_name_superapp
    WalletMode.CUSTODIAL_ONLY -> R.string.brokerage_wallet_name_superapp
    else -> throw IllegalArgumentException("No title supported for mode")
}
