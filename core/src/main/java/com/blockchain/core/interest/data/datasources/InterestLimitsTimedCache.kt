package com.blockchain.core.interest.data.datasources

import com.blockchain.core.common.caching.TimedCacheRequest
import com.blockchain.core.interest.domain.model.InterestLimits
import com.blockchain.nabu.Authenticator
import com.blockchain.nabu.service.NabuService
import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Single
import timber.log.Timber
import java.util.Calendar

class InterestLimitsTimedCache(
    private val authenticator: Authenticator,
    private val assetCatalogue: AssetCatalogue,
    private val nabuService: NabuService,
    private val currencyPrefs: CurrencyPrefs
) {

    private val cache = TimedCacheRequest(
        cacheLifetimeSeconds = CACHE_LIFETIME,
        refreshFn = ::refresh
    )

    private fun refresh(): Single<Map<AssetInfo, InterestLimits>> =
        authenticator.authenticate { token ->
            nabuService.getInterestLimits(token, currencyPrefs.selectedFiatCurrency.networkTicker)
                .map { interestLimits ->
                    interestLimits.limits.entries.mapNotNull { entry ->
                        assetCatalogue.assetInfoFromNetworkTicker(entry.key)?.let { asset ->

                            val calendar = Calendar.getInstance().apply {
                                set(Calendar.DAY_OF_MONTH, 1)
                                add(Calendar.MONTH, 1)
                            }

                            val minDepositFiatValue = Money.fromMinor(
                                currencyPrefs.selectedFiatCurrency,
                                entry.value.minDepositAmount.toBigInteger()
                            )
                            val maxWithdrawalFiatValue = Money.fromMinor(
                                currencyPrefs.selectedFiatCurrency,
                                entry.value.maxWithdrawalAmount.toBigInteger()
                            )

                            val interestLimit = InterestLimits(
                                interestLockUpDuration = entry.value.lockUpDuration,
                                nextInterestPayment = calendar.time,
                                minDepositFiatValue = minDepositFiatValue,
                                maxWithdrawalFiatValue = maxWithdrawalFiatValue
                            )

                            Pair(asset, interestLimit)
                        }
                    }.toMap()
                }
        }.doOnError { Timber.e("Limits call failed $it") }

    fun cached(): Single<Map<AssetInfo, InterestLimits>> = cache.getCachedSingle()

    companion object {
        private const val CACHE_LIFETIME = 240L
    }
}
