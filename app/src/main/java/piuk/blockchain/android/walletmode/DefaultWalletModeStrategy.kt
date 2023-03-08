package piuk.blockchain.android.walletmode

import com.blockchain.domain.eligibility.EligibilityService
import com.blockchain.domain.eligibility.model.EligibleProduct
import com.blockchain.outcome.getOrNull
import com.blockchain.preferences.WalletModePrefs
import com.blockchain.store.firstOutcome
import com.blockchain.walletmode.WalletMode

class DefaultWalletModeStrategy(
    private val walletModePrefs: WalletModePrefs,
    private val eligibilityService: EligibilityService
) {

    suspend fun defaultWalletMode(): WalletMode {
        val productsEligibilityData =
            eligibilityService.getProductEligibility(product = EligibleProduct.USE_CUSTODIAL_ACCOUNTS).firstOutcome()
                .getOrNull()
                ?: return WalletMode.CUSTODIAL

        return if (productsEligibilityData.canTransact) {
            try {
                WalletMode.valueOf(walletModePrefs.legacyWalletMode)
            } catch (e: Exception) {
                WalletMode.CUSTODIAL
            }
        } else WalletMode.NON_CUSTODIAL.also {
            walletModePrefs.userDefaultedToPKW = true
        }
    }

    suspend fun custodialEnabled(): Boolean {
        val productsEligibilityData =
            eligibilityService.getProductEligibility(product = EligibleProduct.USE_CUSTODIAL_ACCOUNTS).firstOutcome()
                .getOrNull()
                ?: return true
        return productsEligibilityData.canTransact
    }
}
