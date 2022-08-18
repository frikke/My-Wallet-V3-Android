package com.blockchain.coincore.fiat

import com.blockchain.coincore.AccountGroup
import com.blockchain.coincore.Asset
import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.ReceiveAddress
import com.blockchain.coincore.SingleAccount
import com.blockchain.coincore.SingleAccountList
import com.blockchain.core.custodial.domain.TradingService
import com.blockchain.core.price.ExchangeRate
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.core.price.HistoricalRateList
import com.blockchain.core.price.HistoricalTimeSpan
import com.blockchain.core.price.Prices24HrWithDelta
import com.blockchain.data.DataResource
import com.blockchain.domain.paymentmethods.BankService
import com.blockchain.koin.scopedInject
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.Currency
import info.blockchain.balance.FiatCurrency
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class FiatAsset(
    override val currency: Currency
) : Asset, KoinComponent {
    private val exchangeRates: ExchangeRatesDataManager by inject()
    private val bankService: BankService by scopedInject()
    private val custodialWalletManager: CustodialWalletManager by scopedInject()
    private val tradingService: TradingService by scopedInject()
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
            tradingService = tradingService,
            exchangeRates = exchangeRates,
            custodialWalletManager = custodialWalletManager,
            bankService = bankService
        )
    }

    override fun exchangeRate(): Single<ExchangeRate> =
        exchangeRates.exchangeRateToUserFiat(currency).firstOrError()

    override fun getPricesWith24hDeltaLegacy(): Single<Prices24HrWithDelta> =
        exchangeRates.getPricesWith24hDeltaLegacy(currency).firstOrError()

    override fun getPricesWith24hDelta(): Flow<DataResource<Prices24HrWithDelta>> {
       return  exchangeRates.getPricesWith24hDelta(currency)
    }

    override fun historicRate(epochWhen: Long): Single<ExchangeRate> =
        exchangeRates.getHistoricRate(currency, epochWhen)

    override fun historicRateSeries(period: HistoricalTimeSpan): Flow<DataResource<HistoricalRateList>> =
        currency.startDate?.let {
            exchangeRates.getHistoricPriceSeries(currency, period)
        } ?: flowOf(DataResource.Data(emptyList()))

    override fun lastDayTrend(): Flow<DataResource<HistoricalRateList>> {
        return currency.startDate?.let {
            exchangeRates.get24hPriceSeries(currency)
        } ?: flowOf(DataResource.Data(emptyList()))
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
