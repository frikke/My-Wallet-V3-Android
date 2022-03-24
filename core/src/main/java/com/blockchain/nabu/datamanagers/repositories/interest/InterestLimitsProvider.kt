package com.blockchain.nabu.datamanagers.repositories.interest

import com.blockchain.nabu.Authenticator
import com.blockchain.nabu.service.NabuService
import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Single
import java.util.Calendar
import java.util.Date
import timber.log.Timber

interface InterestLimitsProvider {
    fun getLimitsForAllAssets(): Single<InterestLimitsList>
}

class InterestLimitsProviderImpl(
    private val assetCatalogue: AssetCatalogue,
    private val nabuService: NabuService,
    private val authenticator: Authenticator,
    private val currencyPrefs: CurrencyPrefs
) : InterestLimitsProvider {
    override fun getLimitsForAllAssets(): Single<InterestLimitsList> =
        authenticator.authenticate {
            nabuService.getInterestLimits(it, currencyPrefs.selectedFiatCurrency.networkTicker)
                .map { responseBody ->
                    InterestLimitsList(
                        responseBody.limits.entries.mapNotNull { entry ->
                            assetCatalogue.assetInfoFromNetworkTicker(entry.key)?.let { crypto ->

                                val minDepositFiatValue = Money.fromMinor(
                                    currencyPrefs.selectedFiatCurrency,
                                    entry.value.minDepositAmount.toBigInteger()
                                )
                                val maxWithdrawalFiatValue = Money.fromMinor(
                                    currencyPrefs.selectedFiatCurrency,
                                    entry.value.maxWithdrawalAmount.toBigInteger()
                                )

                                val calendar = Calendar.getInstance()
                                calendar.set(Calendar.DAY_OF_MONTH, 1)
                                calendar.add(Calendar.MONTH, 1)

                                InterestLimits(
                                    interestLockUpDuration = entry.value.lockUpDuration,
                                    minDepositFiatValue = minDepositFiatValue,
                                    cryptoCurrency = crypto,
                                    currency = entry.value.currency,
                                    nextInterestPayment = calendar.time,
                                    maxWithdrawalFiatValue = maxWithdrawalFiatValue
                                )
                            }
                        }
                    )
                }
        }.doOnError { Timber.e("Limits call failed $it") }
}

data class InterestLimits(
    val interestLockUpDuration: Int,
    val minDepositFiatValue: Money,
    val cryptoCurrency: AssetInfo,
    val currency: String,
    val nextInterestPayment: Date,
    val maxWithdrawalFiatValue: Money
)

data class InterestLimitsList(
    val list: List<InterestLimits>
)
