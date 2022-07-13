package piuk.blockchain.android.walletmode

import com.blockchain.componentlib.theme.AppThemeProvider
import com.blockchain.componentlib.theme.DefaultAppTheme
import com.blockchain.componentlib.theme.DefiWalletTheme
import com.blockchain.componentlib.theme.Theme
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class WalletModeThemeProvider(private val walletModeService: WalletModeService) : AppThemeProvider {
    override val appTheme: Flow<Theme>
        get() = walletModeService.walletMode.distinctUntilChanged().map {
            when (it) {
                WalletMode.UNIVERSAL,
                WalletMode.CUSTODIAL_ONLY -> DefaultAppTheme
                WalletMode.NON_CUSTODIAL_ONLY -> DefiWalletTheme
            }
        }
}
