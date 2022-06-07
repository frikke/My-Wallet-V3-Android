package piuk.blockchain.android.rating.data.repository

import com.blockchain.domain.paymentmethods.BankService
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.nabu.Feature
import com.blockchain.nabu.Tier
import com.blockchain.nabu.UserIdentity
import com.blockchain.outcome.doOnFailure
import com.blockchain.outcome.doOnSuccess
import com.blockchain.outcome.fold
import com.blockchain.preferences.AppRatingPrefs
import com.blockchain.preferences.CurrencyPrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx3.await
import piuk.blockchain.android.rating.data.api.AppRatingApi
import piuk.blockchain.android.rating.data.model.AppRatingApiKeys
import piuk.blockchain.android.rating.data.remoteconfig.AppRatingApiKeysRemoteConfig
import piuk.blockchain.android.rating.data.remoteconfig.AppRatingRemoteConfig
import piuk.blockchain.android.rating.domain.model.AppRating
import piuk.blockchain.android.rating.domain.service.AppRatingService
import timber.log.Timber
import java.util.Calendar
import java.util.concurrent.TimeUnit

internal class AppRatingRepository(
    private val externalScope: CoroutineScope,

    private val appRatingRemoteConfig: AppRatingRemoteConfig,
    private val appRatingApiKeysRemoteConfig: AppRatingApiKeysRemoteConfig,
    private val defaultThreshold: Int,
    private val appRatingApi: AppRatingApi,
    private val appRatingPrefs: AppRatingPrefs,

    private val appRatingFF: FeatureFlag,

    // prerequisites verification
    private val userIdentity: UserIdentity,
    private val currencyPrefs: CurrencyPrefs,
    private val bankService: BankService
) : AppRatingService {

    override suspend fun getThreshold(): Int {
        return appRatingRemoteConfig.getThreshold().fold(
            onSuccess = { it },
            onFailure = { defaultThreshold }
        )
    }

    override fun postRatingData(appRating: AppRating, forceRetrigger: Boolean) {
        externalScope.launch(Dispatchers.IO) {
            // get api keys from remote config
            val apiKeys: AppRatingApiKeys? = appRatingApiKeysRemoteConfig.getApiKeys().fold(
                onSuccess = { it },
                onFailure = { null }
            )

            apiKeys?.let {
                appRatingApi.postRatingData(
                    apiKeys = apiKeys,
                    appRating = appRating
                )
                    .doOnSuccess {
                        markRatingCompleted()
                    }
                    .doOnFailure {
                        // if there is any error in the api, we save to retrigger again in 1 month
                        saveRatingDateForLater()
                    }

            } ?: kotlin.run {
                // if for some reason we can't get api keys, or json is damaged, we save to retrigger again in 1 month
                saveRatingDateForLater()
            }
        }
    }

    override suspend fun shouldShowRating(): Boolean {
        return try {
            when {
                // FF enabled
                isFFEnabled().not() -> false

                // have not rated before
                appRatingPrefs.completed -> false

                // must be GOLD
                isKycGold().not() -> false

                // must have no withdrawal locks
                hasWithdrawalLocks() -> false

                // last try was more than a month ago
                else -> {
                    val minDuration = TimeUnit.MILLISECONDS.convert(31, TimeUnit.DAYS)

                    val currentTimeMillis = Calendar.getInstance().timeInMillis
                    val difference = currentTimeMillis - appRatingPrefs.promptDateMillis
                    difference > minDuration
                }
            }
        } catch (e: Throwable) {
            Timber.e(e)
            false
        }
    }

    private suspend fun isFFEnabled(): Boolean = appRatingFF.enabled.await()

    private suspend fun isKycGold(): Boolean = userIdentity.isVerifiedFor(Feature.TierLevel(Tier.GOLD)).await()

    private suspend fun hasWithdrawalLocks(): Boolean {
        bankService.getWithdrawalLocks(currencyPrefs.selectedFiatCurrency).await().let { fundsLocks ->
            return fundsLocks.onHoldTotalAmount.isPositive
        }
    }

    /**
     * Saves the current date/time if needed in the future [AppRatingPrefs.promptDateMillis]
     * And set [AppRatingPrefs.completed] true
     */
    private fun markRatingCompleted() {
        appRatingPrefs.promptDateMillis = Calendar.getInstance().timeInMillis
        appRatingPrefs.completed = true
    }

    override fun saveRatingDateForLater() {
        appRatingPrefs.promptDateMillis = Calendar.getInstance().timeInMillis
    }
}
