package piuk.blockchain.android.ui.educational.walletmodes.screens

import androidx.compose.runtime.Composable

enum class EducationalWalletModePages(val index: Int, val Content: @Composable () -> Unit) {
    INTRO(index = 0, Content = { EducationalWalletModeIntroScreen() }),
    MENU(index = 1, Content = { EducationalWalletModeMenuScreen() }),
    DEFI(index = 2, Content = { EducationalWalletModeDefiScreen() }),
    TRADING(index = 3, Content = { EducationalWalletModeTradingScreen() })
}