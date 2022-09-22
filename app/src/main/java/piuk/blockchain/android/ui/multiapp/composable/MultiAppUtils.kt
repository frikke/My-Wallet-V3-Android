package piuk.blockchain.android.ui.multiapp.composable

import androidx.annotation.StringRes
import androidx.compose.foundation.lazy.LazyListState
import com.blockchain.walletmode.WalletMode
import com.google.accompanist.swiperefresh.SwipeRefreshState
import java.util.concurrent.TimeUnit
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.multiapp.ChromeBottomNavigationItem

const val ANIMATION_DURATION = 400
val REVEAL_BALANCE_DELAY_MS = TimeUnit.SECONDS.toMillis(3)

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
    WalletMode.CUSTODIAL_ONLY -> R.string.brokerage_wallet_name_superapp
    WalletMode.NON_CUSTODIAL_ONLY -> R.string.defi_wallet_name_superapp
    else -> error("UNIVERSAL not supported")
}

fun WalletMode.bottomNavigationItems(): List<ChromeBottomNavigationItem> = when (this) {
    WalletMode.CUSTODIAL_ONLY -> listOf(
        ChromeBottomNavigationItem.Home,
        ChromeBottomNavigationItem.Trade,
        ChromeBottomNavigationItem.Card
    )
    WalletMode.NON_CUSTODIAL_ONLY -> listOf(
        ChromeBottomNavigationItem.Home,
        ChromeBottomNavigationItem.Trade,
        ChromeBottomNavigationItem.Nft
    )
    else -> error("UNIVERSAL not supported")
}
