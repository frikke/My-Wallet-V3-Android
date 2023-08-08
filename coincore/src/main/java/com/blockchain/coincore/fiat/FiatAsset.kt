package com.blockchain.coincore.fiat

import com.blockchain.coincore.AccountGroup
import com.blockchain.coincore.Asset
import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.ReceiveAddress
import com.blockchain.coincore.SingleAccount
import com.blockchain.coincore.SingleAccountList
import com.blockchain.core.buy.domain.SimpleBuyService
import com.blockchain.core.custodial.domain.TradingService
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.core.price.HistoricalRateList
import com.blockchain.core.price.HistoricalTimeSpan
import com.blockchain.core.price.Prices24HrWithDelta
import com.blockchain.data.DataResource
import com.blockchain.data.asSingle
import com.blockchain.domain.paymentmethods.BankService
import com.blockchain.koin.scopedInject
import com.blockchain.nabu.Feature
import com.blockchain.nabu.api.getuser.domain.UserFeaturePermissionService
import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.Currency
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.FiatCurrency
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class FiatAsset(
    override val currency: Currency
) : Asset, KoinComponent {
    private val exchangeRates: ExchangeRatesDataManager by inject()
    private val userFeaturePermissionService: UserFeaturePermissionService by scopedInject()
    private val bankService: BankService by scopedInject()
    private val simpBuyService: SimpleBuyService by scopedInject()
    private val tradingService: TradingService by scopedInject()
    private val labels: DefaultLabels by inject()

    override fun defaultAccount(filter: AssetFilter): Single<SingleAccount> = accountGroup(filter).map {
        it.accounts[0]
    }.toSingle()

    override fun accountGroup(filter: AssetFilter): Maybe<AccountGroup> =
        when (filter) {
            AssetFilter.All,
            AssetFilter.Custodial,
            AssetFilter.Trading -> loadCustodialAccount()
            AssetFilter.NonCustodial,
            AssetFilter.Staking,
            AssetFilter.Interest,
            AssetFilter.ActiveRewards -> Maybe.empty() // Only support single accounts
        }

    private fun loadCustodialAccount(): Maybe<AccountGroup> {
        return userFeaturePermissionService.isEligibleFor(Feature.CustodialAccounts).asSingle().flatMapMaybe {
            if (it) {
                Maybe.just(
                    FiatAccountGroup(
                        label = labels.getDefaultTradingWalletLabel(),
                        accounts = listOf(custodialAccount)
                    )
                )
            } else Maybe.empty()
        }
    }

    val custodialAccount: FiatAccount by lazy {
        require(currency is FiatCurrency)
        FiatCustodialAccount(
            label = labels.getDefaultCustodialFiatWalletLabel(currency),
            currency = currency,
            tradingService = tradingService,
            exchangeRates = exchangeRates,
            bankService = bankService,
            simpleBuyService = simpBuyService
        )
    }

    override fun exchangeRate(): Single<ExchangeRate> =
        exchangeRates.exchangeRateToUserFiat(currency).firstOrError()

    override fun getPricesWith24hDeltaLegacy(): Single<Prices24HrWithDelta> =
        exchangeRates.getPricesWith24hDeltaLegacy(currency).firstOrError()

    override fun getPricesWith24hDelta(): Flow<DataResource<Prices24HrWithDelta>> {
        return exchangeRates.getPricesWith24hDelta(fromAsset = currency)
    }

    override fun historicRate(epochWhen: Long): Single<ExchangeRate> =
        exchangeRates.getHistoricRate(currency, epochWhen)

    override fun historicRateSeries(
        period: HistoricalTimeSpan
    ): Flow<DataResource<HistoricalRateList>> =
        currency.startDate?.let {
            exchangeRates.getHistoricPriceSeries(asset = currency, span = period)
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
