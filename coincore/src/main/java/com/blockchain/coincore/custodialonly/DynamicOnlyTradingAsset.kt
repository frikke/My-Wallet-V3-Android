package com.blockchain.coincore.custodialonly

import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.CryptoAddress
import com.blockchain.coincore.IdentityAddressResolver
import com.blockchain.coincore.ReceiveAddress
import com.blockchain.coincore.SingleAccountList
import com.blockchain.coincore.impl.CryptoAssetBase
import com.blockchain.coincore.impl.CustodialTradingAccount
import com.blockchain.core.custodial.TradingBalanceDataManager
import com.blockchain.core.interest.InterestBalanceDataManager
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.logging.RemoteLogger
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.wallet.DefaultLabels
import exchange.ExchangeLinking
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.isCustodialOnly
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy

internal class DynamicOnlyTradingAsset(
    override val assetInfo: AssetInfo,
    payloadManager: PayloadDataManager,
    custodialManager: CustodialWalletManager,
    interestBalances: InterestBalanceDataManager,
    tradingBalances: TradingBalanceDataManager,
    exchangeRates: ExchangeRatesDataManager,
    currencyPrefs: CurrencyPrefs,
    labels: DefaultLabels,
    exchangeLinking: ExchangeLinking,
    remoteLogger: RemoteLogger,
    identity: UserIdentity,
    private val addressValidation: String? = null,
    private val availableActions: Set<AssetAction> = emptySet(),
    addressResolver: IdentityAddressResolver
) : CryptoAssetBase(
    payloadManager,
    exchangeRates,
    currencyPrefs,
    labels,
    custodialManager,
    interestBalances,
    tradingBalances,
    exchangeLinking,
    remoteLogger,
    identity,
    addressResolver
) {
    override val isCustodialOnly: Boolean = assetInfo.isCustodialOnly
    override val multiWallet: Boolean = false

    override fun loadNonCustodialAccounts(labels: DefaultLabels): Single<SingleAccountList> =
        Single.just(emptyList())

    override fun loadCustodialAccounts(): Single<SingleAccountList> =
        Single.just(
            listOf(
                CustodialTradingAccount(
                    currency = assetInfo,
                    label = labels.getDefaultCustodialWalletLabel(),
                    exchangeRates = exchangeRates,
                    custodialWalletManager = custodialManager,
                    tradingBalances = tradingBalances,
                    identity = identity,
                    baseActions = availableActions
                )
            )
        )

    private val addressRegex: Regex? by unsafeLazy {
        addressValidation?.toRegex()
    }

    override fun parseAddress(address: String, label: String?, isDomainAddress: Boolean): Maybe<ReceiveAddress> =
        addressRegex?.let {
            if (address.matches(it)) {
                Maybe.just(
                    DynamicCustodialAddress(
                        address = address,
                        asset = assetInfo as AssetInfo,
                        isDomain = isDomainAddress
                    )
                )
            } else {
                Maybe.empty()
            }
        } ?: Maybe.empty()
}

internal class DynamicCustodialAddress(
    override val address: String,
    override val asset: AssetInfo,
    override val label: String = address,
    override val isDomain: Boolean = false
) : CryptoAddress
