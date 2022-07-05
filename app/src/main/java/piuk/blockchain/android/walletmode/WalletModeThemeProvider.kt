package piuk.blockchain.android.walletmode

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.blockchain.componentlib.theme.AppDimensions
import com.blockchain.componentlib.theme.AppShapes
import com.blockchain.componentlib.theme.AppThemeProvider
import com.blockchain.componentlib.theme.AppTypography
import com.blockchain.componentlib.theme.DefaultAppTheme
import com.blockchain.componentlib.theme.Purple0000
import com.blockchain.componentlib.theme.SemanticColors
import com.blockchain.componentlib.theme.Theme
import com.blockchain.componentlib.theme.defDarkColors
import com.blockchain.componentlib.theme.defLightColors
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

object DefiWalletTheme : Theme() {
    override val lightColors: SemanticColors
        @Composable
        @ReadOnlyComposable
        get() = defLightColors.copy(primary = Purple0000)

    override val darkColors: SemanticColors
        @Composable
        @ReadOnlyComposable
        get() = defDarkColors

    override val typography: AppTypography
        @Composable
        @ReadOnlyComposable
        get() = AppTypography()

    override val dimensions: AppDimensions
        @Composable
        @ReadOnlyComposable
        get() = AppDimensions()

    override val shapes: AppShapes
        @Composable
        @ReadOnlyComposable
        get() = AppShapes()
}
