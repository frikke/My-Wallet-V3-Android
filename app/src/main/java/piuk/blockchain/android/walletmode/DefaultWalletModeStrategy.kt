package piuk.blockchain.android.walletmode

import com.blockchain.domain.eligibility.model.EligibleProduct
import com.blockchain.domain.eligibility.model.ProductEligibility
import com.blockchain.domain.eligibility.model.TransactionsLimit
import com.blockchain.preferences.WalletModePrefs
import com.blockchain.walletmode.WalletMode

class DefaultWalletModeStrategy(private val walletModePrefs: WalletModePrefs) {
    private var custodialAccountsProduct = ProductEligibility(
        product = EligibleProduct.USE_CUSTODIAL_ACCOUNTS,
        canTransact = true,
        isDefault = true,
        maxTransactionsCap = TransactionsLimit.Unlimited,
        reasonNotEligible = null
    )

    fun updateProductEligibility(productEligibility: ProductEligibility) {
        require(productEligibility.product == EligibleProduct.USE_CUSTODIAL_ACCOUNTS)
        this.custodialAccountsProduct = productEligibility
    }

    fun defaultWalletMode(): WalletMode {
        return if (custodialAccountsProduct.canTransact) {
            try {
                WalletMode.valueOf(walletModePrefs.legacyWalletMode)
            } catch (e: Exception) {
                WalletMode.CUSTODIAL_ONLY
            }
        } else {
            WalletMode.NON_CUSTODIAL_ONLY.also {
                walletModePrefs.userDefaultedToPKW = true
            }
        }
    }
}
