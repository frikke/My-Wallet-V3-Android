package piuk.blockchain.android.rating.data.repository

import com.blockchain.core.kyc.domain.model.KycTier
import com.blockchain.domain.paymentmethods.BankService
import com.blockchain.nabu.Feature
import com.blockchain.nabu.UserIdentity
import com.blockchain.outcome.doOnFailure
import com.blockchain.outcome.doOnSuccess
import com.blockchain.outcome.getOrDefault
import com.blockchain.outcome.getOrNull
import com.blockchain.preferences.AppRatingPrefs
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.store.asSingle
import com.blockchain.utils.awaitOutcome
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx3.await
import piuk.blockchain.android.rating.data.api.AppRatingApi
import piuk.blockchain.android.rating.data.model.AppRatingApiKeys
import piuk.blockchain.android.rating.data.remoteconfig.AppRatingApiKeysRemoteConfig
import piuk.blockchain.android.rating.data.remoteconfig.AppRatingRemoteConfig
import piuk.blockchain.android.rating.domain.model.AppRating
import piuk.blockchain.android.rating.domain.service.AppRatingService
import timber.log.Timber

internal class AppRatingRepository(
    private val coroutineScope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher,

    private val appRatingRemoteConfig: AppRatingRemoteConfig,
    private val appRatingApiKeysRemoteConfig: AppRatingApiKeysRemoteConfig,
    private val defaultThreshold: Int,
    private val appRatingApi: AppRatingApi,
    private val appRatingPrefs: AppRatingPrefs,
    // prerequisites verification
    private val userIdentity: UserIdentity,
    private val currencyPrefs: CurrencyPrefs,
    private val bankService: BankService
) : AppRatingService {

    override suspend fun getThreshold(): Int {
        return appRatingRemoteConfig.getThreshold().getOrDefault(defaultThreshold)
    }

    override fun postRatingData(appRating: AppRating) {
        coroutineScope.launch(dispatcher) {

            // get api keys from remote config
            val apiKeys: AppRatingApiKeys? = appRatingApiKeysRemoteConfig.getApiKeys().getOrDefault(null)

            apiKeys?.let {
                appRatingApi.postRatingData(apiKeys = apiKeys, appRating = appRating)
                    .doOnSuccess { markRatingCompleted() }
                    .doOnFailure { saveRatingDateForLater() }
            } ?: kotlin.run {
                // if for some reason we can't get api keys, or json is damaged, we save to retrigger again in 1 month
                saveRatingDateForLater()
            }
        }
    }

    override suspend fun shouldShowRating(): Boolean {
        return try {
            when {
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

    private suspend fun isKycGold(): Boolean =
        userIdentity.isVerifiedFor(Feature.TierLevel(KycTier.GOLD)).awaitOutcome().getOrDefault(false)

    private suspend fun hasWithdrawalLocks(): Boolean {
        bankService.getWithdrawalLocks(currencyPrefs.selectedFiatCurrency).asSingle().awaitOutcome().getOrNull()
            ?.let { fundsLocks ->
                return fundsLocks.onHoldTotalAmount.isPositive
            } ?: return false
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
