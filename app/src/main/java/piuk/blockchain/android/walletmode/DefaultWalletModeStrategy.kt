package piuk.blockchain.android.walletmode

import com.blockchain.core.eligibility.cache.ProductsEligibilityStore
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.domain.eligibility.model.EligibleProduct
import com.blockchain.preferences.WalletModePrefs
import com.blockchain.store.asSingle
import com.blockchain.walletmode.WalletMode
import kotlinx.coroutines.rx3.await

class DefaultWalletModeStrategy(
    private val walletModePrefs: WalletModePrefs,
    private val productsEligibilityStore: ProductsEligibilityStore
) {

    suspend fun defaultWalletMode(): WalletMode {
        val productsEligibilityData =
            productsEligibilityStore.stream(FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale)).asSingle().await()
        return productsEligibilityData.products[EligibleProduct.USE_CUSTODIAL_ACCOUNTS]?.let { eligibility ->
            if (eligibility.canTransact) {
                try {
                    WalletMode.valueOf(walletModePrefs.legacyWalletMode)
                } catch (e: Exception) {
                    WalletMode.CUSTODIAL_ONLY
                }
            } else WalletMode.NON_CUSTODIAL_ONLY.also {
                walletModePrefs.userDefaultedToPKW = true
            }
        } ?: WalletMode.CUSTODIAL_ONLY
    }
}
