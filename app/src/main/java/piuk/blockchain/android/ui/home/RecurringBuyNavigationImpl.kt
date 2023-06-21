package piuk.blockchain.android.ui.home

import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.chrome.navigation.RecurringBuyNavigation
import piuk.blockchain.android.ui.recurringbuy.onboarding.RecurringBuyOnboardingActivity

class RecurringBuyNavigationImpl(private val activity: BlockchainActivity?) : RecurringBuyNavigation {
    override fun openOnboarding() {
        activity?.startActivity(
            RecurringBuyOnboardingActivity.newIntent(
                context = activity,
                assetTicker = null
            )
        )
    }
}
