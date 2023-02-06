package piuk.blockchain.android.walletmode

import com.blockchain.core.eligibility.cache.ProductsEligibilityStore
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.domain.eligibility.model.EligibleProduct
import com.blockchain.outcome.getOrNull
import com.blockchain.preferences.WalletModePrefs
import com.blockchain.store.firstOutcome
import com.blockchain.walletmode.WalletMode

class DefaultWalletModeStrategy(
    private val walletModePrefs: WalletModePrefs,
    private val productsEligibilityStore: ProductsEligibilityStore
) {

    suspend fun defaultWalletMode(): WalletMode {
        val productsEligibilityData =
            productsEligibilityStore.stream(FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale))
                .firstOutcome()
                .getOrNull()
                ?: return WalletMode.CUSTODIAL

        return productsEligibilityData.products[EligibleProduct.USE_CUSTODIAL_ACCOUNTS]?.let { eligibility ->
            if (eligibility.isDefault) {
                try {
                    WalletMode.valueOf(walletModePrefs.legacyWalletMode)
                } catch (e: Exception) {
                    WalletMode.CUSTODIAL
                }
            } else WalletMode.NON_CUSTODIAL.also {
                walletModePrefs.userDefaultedToPKW = true
            }
        } ?: WalletMode.CUSTODIAL
    }

    suspend fun custodialEnabled(): Boolean {
        val productsEligibilityData =
            productsEligibilityStore.stream(FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale))
                .firstOutcome()
                .getOrNull()
                ?: return true

        return productsEligibilityData.products[EligibleProduct.USE_CUSTODIAL_ACCOUNTS]?.canTransact ?: true
    }
}
