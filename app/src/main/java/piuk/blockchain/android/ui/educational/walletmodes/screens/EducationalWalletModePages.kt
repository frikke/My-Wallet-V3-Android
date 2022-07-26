package piuk.blockchain.android.ui.educational.walletmodes.screens

import androidx.compose.runtime.Composable

enum class EducationalWalletModePages(val Content: @Composable () -> Unit) {
    INTRO(Content = { EducationalWalletModeIntroScreen() }),
    MENU(Content = { EducationalWalletModeMenuScreen() }),
    DEFI(Content = { EducationalWalletModeDefiScreen() }),
    TRADING(Content = { EducationalWalletModeTradingScreen() })
}
