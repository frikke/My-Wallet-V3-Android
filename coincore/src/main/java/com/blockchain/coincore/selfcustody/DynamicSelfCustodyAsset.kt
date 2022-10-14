package com.blockchain.coincore.selfcustody

import com.blockchain.coincore.CryptoAddress
import com.blockchain.coincore.IdentityAddressResolver
import com.blockchain.coincore.ReceiveAddress
import com.blockchain.coincore.SingleAccountList
import com.blockchain.coincore.impl.CryptoAssetBase
import com.blockchain.core.chains.dynamicselfcustody.domain.NonCustodialService
import com.blockchain.core.payload.PayloadDataManager
import com.blockchain.preferences.WalletStatusPrefs
import com.blockchain.utils.unsafeLazy
import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.rx3.rxSingle

internal class DynamicSelfCustodyAsset(
    override val currency: AssetInfo,
    private val payloadManager: PayloadDataManager,
    private val addressResolver: IdentityAddressResolver,
    private val addressValidation: String? = null,
    private val selfCustodyService: NonCustodialService,
    private val walletPreferences: WalletStatusPrefs
) : CryptoAssetBase() {

    override fun loadNonCustodialAccounts(labels: DefaultLabels): Single<SingleAccountList> =
        rxSingle {
            selfCustodyService.getCoinConfigurationFor(currency)?.let { coinConfiguration ->
                listOf(
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
            } ?: listOf()
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
}

internal class DynamicNonCustodialAddress(
    override val address: String,
    override val asset: AssetInfo,
    override val label: String = address,
    override val isDomain: Boolean = false
) : CryptoAddress
