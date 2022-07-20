package com.blockchain.coincore.selfcustody

import com.blockchain.coincore.IdentityAddressResolver
import com.blockchain.coincore.ReceiveAddress
import com.blockchain.coincore.SingleAccountList
import com.blockchain.coincore.impl.CryptoAssetBase
import com.blockchain.core.chains.dynamicselfcustody.domain.NonCustodialService
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.preferences.WalletStatusPrefs
import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.AssetInfo
import info.blockchain.wallet.dynamicselfcustody.CoinConfiguration
import info.blockchain.wallet.payload.data.Derivation
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy

internal class StxAsset(
    override val currency: AssetInfo,
    private val payloadManager: PayloadDataManager,
    private val addressResolver: IdentityAddressResolver,
    private val addressValidation: String? = null,
    private val selfCustodyService: NonCustodialService,
    private val stxForAllFeatureFlag: FeatureFlag,
    private val walletPreferences: WalletStatusPrefs
) : CryptoAssetBase() {

    override fun loadNonCustodialAccounts(labels: DefaultLabels): Single<SingleAccountList> =
        stxForAllFeatureFlag.enabled
            .map { isEnabled ->
                when {
                    isEnabled -> listOf(
                        DynamicNonCustodialAccount(
                            payloadManager,
                            currency,
                            coinConfiguration,
                            addressResolver,
                            selfCustodyService,
                            exchangeRates,
                            labels.getDefaultNonCustodialWalletLabel(),
                            walletPreferences
                        )
                    )
                    else -> emptyList()
                }
            }

    private val addressRegex: Regex? by unsafeLazy {
        addressValidation?.toRegex()
    }

    override fun parseAddress(address: String, label: String?, isDomainAddress: Boolean): Maybe<ReceiveAddress> =
        addressRegex?.let {
            if (address.matches(it)) {
                Maybe.just(
                    DynamicNonCustodialAddress(
                        address = address,
                        asset = currency,
                        isDomain = isDomainAddress
                    )
                )
            } else {
                Maybe.empty()
            }
        } ?: Maybe.empty()

    companion object {
        private const val STX_COIN_TYPE = 5757
        private val coinConfiguration = CoinConfiguration(
            coinType = STX_COIN_TYPE,
            purpose = Derivation.LEGACY_PURPOSE
        )
    }
}
