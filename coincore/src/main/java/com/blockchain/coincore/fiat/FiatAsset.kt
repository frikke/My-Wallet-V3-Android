package com.blockchain.coincore.fiat

import com.blockchain.coincore.AccountGroup
import com.blockchain.coincore.Asset
import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.ReceiveAddress
import com.blockchain.coincore.SingleAccount
import com.blockchain.coincore.SingleAccountList
import com.blockchain.core.custodial.TradingBalanceDataManager
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.domain.paymentmethods.BankService
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.Currency
import info.blockchain.balance.FiatCurrency
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single

class FiatAsset(
    private val labels: DefaultLabels,
    private val exchangeRateDataManager: ExchangeRatesDataManager,
    private val tradingBalanceDataManager: TradingBalanceDataManager,
    private val custodialWalletManager: CustodialWalletManager,
    private val bankService: BankService,
    override val assetInfo: Currency
) : Asset {

    override fun accountGroup(filter: AssetFilter): Maybe<AccountGroup> =
        when (filter) {
            AssetFilter.All,
            AssetFilter.Custodial,
            AssetFilter.Trading -> Maybe.just(
                FiatAccountGroup(
                    label = "Fiat Account",
                    accounts = listOf(custodialAccount)
                )
            )
            AssetFilter.NonCustodial,
            AssetFilter.Interest -> Maybe.empty() // Only support single accounts
        }

    val custodialAccount: FiatAccount by lazy {
        require(assetInfo is FiatCurrency)
        FiatCustodialAccount(
            label = labels.getDefaultCustodialFiatWalletLabel(assetInfo),
            currency = assetInfo,
            tradingBalanceDataManager = tradingBalanceDataManager,
            exchangeRates = exchangeRateDataManager,
            custodialWalletManager = custodialWalletManager,
            bankService = bankService
        )
    }

    // we cannot transfer for fiat
    override fun transactionTargets(account: SingleAccount): Single<SingleAccountList> =
        Single.just(emptyList())

    override fun parseAddress(
        address: String,
        label: String?,
        isDomainAddress: Boolean
    ): Maybe<ReceiveAddress> = Maybe.empty()
}
