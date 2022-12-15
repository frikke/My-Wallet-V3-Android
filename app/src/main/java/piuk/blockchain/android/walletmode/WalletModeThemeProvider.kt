package piuk.blockchain.android.walletmode

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.blockchain.componentlib.theme.AppDimensions
import com.blockchain.componentlib.theme.AppShapes
import com.blockchain.componentlib.theme.AppThemeProvider
import com.blockchain.componentlib.theme.AppTypography
import com.blockchain.componentlib.theme.DefaultAppTheme
import com.blockchain.componentlib.theme.SemanticColors
import com.blockchain.componentlib.theme.Theme
import com.blockchain.componentlib.theme.defDarkColors
import com.blockchain.componentlib.theme.defLightColors
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class WalletModeThemeProvider : AppThemeProvider {
    override val appTheme: Flow<Theme>
        get() = flowOf(DefaultAppTheme)
}

object DefiWalletTheme : Theme() {
    override val lightColors: SemanticColors
        @Composable
        @ReadOnlyComposable
        get() = defLightColors

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
