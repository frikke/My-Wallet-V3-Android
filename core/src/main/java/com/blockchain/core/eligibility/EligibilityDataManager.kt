package com.blockchain.core.eligibility

import com.blockchain.api.eligibility.data.BuyEligibilityResponse
import com.blockchain.api.eligibility.data.CustodialWalletsEligibilityResponse
import com.blockchain.api.eligibility.data.ProductEligibilityResponse
import com.blockchain.api.eligibility.data.SwapEligibilityResponse
import com.blockchain.core.eligibility.models.EligibleProduct
import com.blockchain.core.eligibility.models.ProductEligibility
import com.blockchain.core.eligibility.models.TransactionsLimit
import com.blockchain.outcome.map
import com.blockchain.remoteconfig.FeatureFlag
import io.reactivex.rxjava3.core.Single
import java.util.Locale

// ISO 3166-1 alpha-2
typealias CountryIso = String

interface EligibilityDataManager {
    fun getCustodialEligibleCountries(): Single<List<CountryIso>>

    fun getProductEligibility(product: EligibleProduct): Single<ProductEligibility>
}

class EligibilityDataManagerImpl(
    private val productsEligibilityCache: ProductsEligibilityCache,
    private val entitySwitchSilverEligibilityFeatureFlag: FeatureFlag
) : EligibilityDataManager {
    override fun getCustodialEligibleCountries(): Single<List<CountryIso>> = Single.just(
        Locale.getISOCountries()
            .toList()
            .filterNot { SANCTIONED_COUNTRIES_ISO.contains(it) }
    )

    private fun getProductsEligibility(): Single<List<ProductEligibility>> =
        entitySwitchSilverEligibilityFeatureFlag.enabled
            .flatMap { enabled ->
                if (enabled) {
                    productsEligibilityCache.productsEligibility()
                        .map { response -> response.toDomain() }
                } else {
                    Single.just(emptyList())
                }
            }

    override fun getProductEligibility(product: EligibleProduct): Single<ProductEligibility> =
        getProductsEligibility().map { products ->
            products.find { it.product == product } ?: ProductEligibility.asEligible(product)
        }

    companion object {
        private val SANCTIONED_COUNTRIES_ISO: List<CountryIso> = listOf("CU", "IR", "KP", "SY")
    }
}

private fun ProductEligibilityResponse.toDomain(): List<ProductEligibility> =
    listOfNotNull(buy?.toProductEligibility(), swap?.toProductEligibility(), custodialWallets?.toProductEligibility())

private fun BuyEligibilityResponse.toProductEligibility(): ProductEligibility = ProductEligibility(
    product = EligibleProduct.BUY,
    canTransact = canPlaceOrder,
    maxTransactionsCap = if (maxOrdersCap != null && maxOrdersLeft != null) {
        TransactionsLimit.Limited(maxOrdersCap!!, maxOrdersLeft!!)
    } else {
        TransactionsLimit.Unlimited
    },
    canUpgradeTier = suggestedUpgrade != null
)

private fun SwapEligibilityResponse.toProductEligibility(): ProductEligibility = ProductEligibility(
    product = EligibleProduct.SWAP,
    canTransact = canPlaceOrder,
    maxTransactionsCap = if (maxOrdersCap != null && maxOrdersLeft != null) {
        TransactionsLimit.Limited(maxOrdersCap!!, maxOrdersLeft!!)
    } else {
        TransactionsLimit.Unlimited
    },
    canUpgradeTier = suggestedUpgrade != null
)

private fun CustodialWalletsEligibilityResponse.toProductEligibility(): ProductEligibility =
    ProductEligibility(
        product = EligibleProduct.CRYPTO_DEPOSIT,
        canTransact = canDepositCrypto,
        maxTransactionsCap = TransactionsLimit.Unlimited,
        canUpgradeTier = suggestedUpgrade != null
    )
