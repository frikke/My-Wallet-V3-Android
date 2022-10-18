package piuk.blockchain.android.ui.multiapp.composable

import androidx.annotation.StringRes
import com.blockchain.walletmode.WalletMode
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.multiapp.ChromeBackgroundColors
import piuk.blockchain.android.ui.multiapp.ChromeBottomNavigationItem
import java.util.concurrent.TimeUnit

const val ANIMATION_DURATION = 400
val REVEAL_BALANCE_DELAY_MS = TimeUnit.SECONDS.toMillis(3)

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

fun WalletMode.backgroundColors(): ChromeBackgroundColors {
    return when (this) {
        WalletMode.CUSTODIAL_ONLY -> ChromeBackgroundColors.Trading
        WalletMode.NON_CUSTODIAL_ONLY -> ChromeBackgroundColors.DeFi
        WalletMode.UNIVERSAL -> error("WalletMode.UNIVERSAL unsupported")
    }
}