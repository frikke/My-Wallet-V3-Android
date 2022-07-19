package com.blockchain.coincore.fiat

import com.blockchain.coincore.AccountGroup
import com.blockchain.coincore.Asset
import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.ReceiveAddress
import com.blockchain.coincore.SingleAccount
import com.blockchain.coincore.SingleAccountList
import com.blockchain.core.custodial.TradingBalanceDataManager
import com.blockchain.core.price.ExchangeRate
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.core.price.HistoricalRate
import com.blockchain.core.price.HistoricalRateList
import com.blockchain.core.price.HistoricalTimeSpan
import com.blockchain.core.price.Prices24HrWithDelta
import com.blockchain.domain.paymentmethods.BankService
import com.blockchain.koin.scopedInject
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.Currency
import info.blockchain.balance.FiatCurrency
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class FiatAsset(
    override val currency: Currency
) : Asset, KoinComponent {
    private val exchangeRates: ExchangeRatesDataManager by inject()
    private val bankService: BankService by scopedInject()
    private val custodialWalletManager: CustodialWalletManager by scopedInject()
    private val tradingBalanceDataManager: TradingBalanceDataManager by scopedInject()
    private val labels: DefaultLabels by inject()

    override fun defaultAccount(): Single<SingleAccount> = accountGroup(AssetFilter.All).map {
        it.accounts[0]
    }.toSingle()

    override fun accountGroup(filter: AssetFilter): Maybe<AccountGroup> =
        when (filter) {
            AssetFilter.All,
            AssetFilter.Custodial,
            AssetFilter.Trading -> Maybe.just(
                FiatAccountGroup(
                    label = labels.getDefaultCustodialWalletLabel(),
                    accounts = listOf(custodialAccount)
                )
            )
            AssetFilter.NonCustodial,
            AssetFilter.Interest -> Maybe.empty() // Only support single accounts
        }

    val custodialAccount: FiatAccount by lazy {
        require(currency is FiatCurrency)
        FiatCustodialAccount(
            label = labels.getDefaultCustodialFiatWalletLabel(currency),
            currency = currency,
            tradingBalanceDataManager = tradingBalanceDataManager,
            exchangeRates = exchangeRates,
            custodialWalletManager = custodialWalletManager,
            bankService = bankService
        )
    }

    override fun exchangeRate(): Single<ExchangeRate> =
        exchangeRates.exchangeRateToUserFiat(currency).firstOrError()

    override fun getPricesWith24hDelta(): Single<Prices24HrWithDelta> =
        exchangeRates.getPricesWith24hDelta(currency).firstOrError()

    override fun historicRate(epochWhen: Long): Single<ExchangeRate> =
        exchangeRates.getHistoricRate(currency, epochWhen)

    override fun historicRateSeries(period: HistoricalTimeSpan): Single<HistoricalRateList> =
        currency.startDate?.let {
            exchangeRates.getHistoricPriceSeries(currency, period)
        } ?: Single.just(emptyList())

    override fun lastDayTrend(): Single<List<HistoricalRate>> {
        return currency.startDate?.let {
            exchangeRates.get24hPriceSeries(currency)
        } ?: Single.just(emptyList())
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
