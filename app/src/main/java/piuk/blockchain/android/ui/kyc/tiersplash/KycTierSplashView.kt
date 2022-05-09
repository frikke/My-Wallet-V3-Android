package piuk.blockchain.android.ui.kyc.tiersplash

import androidx.annotation.StringRes
import piuk.blockchain.android.ui.base.View

interface KycTierSplashView : View {
    fun showError(@StringRes message: Int)
}
