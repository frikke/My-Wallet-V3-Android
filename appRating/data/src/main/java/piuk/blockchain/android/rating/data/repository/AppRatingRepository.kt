package piuk.blockchain.android.rating.data.repository

import com.blockchain.core.payments.PaymentsDataManager
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.nabu.Feature
import com.blockchain.nabu.Tier
import com.blockchain.nabu.UserIdentity
import com.blockchain.outcome.fold
import com.blockchain.preferences.AppRatingPrefs
import com.blockchain.preferences.CurrencyPrefs
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.rx3.await
import piuk.blockchain.android.rating.data.api.AppRatingApi
import piuk.blockchain.android.rating.data.model.AppRatingApiKeys
import piuk.blockchain.android.rating.data.remoteconfig.AppRatingApiKeysRemoteConfig
import piuk.blockchain.android.rating.data.remoteconfig.AppRatingRemoteConfig
import piuk.blockchain.android.rating.domain.model.AppRating
import piuk.blockchain.android.rating.domain.service.AppRatingService

internal class AppRatingRepository(
    private val appRatingRemoteConfig: AppRatingRemoteConfig,
    private val appRatingApiKeysRemoteConfig: AppRatingApiKeysRemoteConfig,
    private val defaultThreshold: Int,
    private val appRatingApi: AppRatingApi,
    private val appRatingPrefs: AppRatingPrefs,

    private val appRatingFF: FeatureFlag,

    // prerequisites verification
    private val userIdentity: UserIdentity,
    private val currencyPrefs: CurrencyPrefs,
    private val paymentsDataManager: PaymentsDataManager,
    private val environmentConfig: EnvironmentConfig
) : AppRatingService {

    override suspend fun getThreshold(): Int {
        return appRatingRemoteConfig.getThreshold().fold(
            onSuccess = { it },
            onFailure = { defaultThreshold }
        )
    }

    override suspend fun postRatingData(appRating: AppRating): Boolean {
        // get api keys from remote config
        val apiKeys: AppRatingApiKeys? = appRatingApiKeysRemoteConfig.getApiKeys().fold(
            onSuccess = { it },
            onFailure = { null }
        )

        // if for some reason we can't get api keys, or json is damaged, we return false
        // for the vm to retrigger again in 1 month
        return apiKeys?.let {
            appRatingApi.postRatingData(
                apiKeys = apiKeys,
                appRating = appRating
            ).fold(
                // if there is any error in the api, we return false
                // for the vm to retrigger again in 1 month
                onSuccess = { true },
                onFailure = { false }
            )
        } ?: false
    }

    override suspend fun shouldShowRating(): Boolean {
        return when {
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
                val minDuration = if (environmentConfig.isRunningInDebugMode())
                    TimeUnit.MILLISECONDS.convert(30, TimeUnit.SECONDS) // 30 seconds for debug
                else
                    TimeUnit.MILLISECONDS.convert(31, TimeUnit.DAYS) // 31 days for release

                val currentTimeMillis = Calendar.getInstance().timeInMillis
                val difference = currentTimeMillis - appRatingPrefs.promptDateMillis
                return difference > minDuration
            }
        }
    }

    private suspend fun isFFEnabled(): Boolean = appRatingFF.enabled.await()

    private suspend fun isKycGold(): Boolean = userIdentity.isVerifiedFor(Feature.TierLevel(Tier.GOLD)).await()

    private suspend fun hasWithdrawalLocks(): Boolean {
        paymentsDataManager.getWithdrawalLocks(currencyPrefs.selectedFiatCurrency).await().let { fundsLocks ->
            return fundsLocks.onHoldTotalAmount.isPositive
        }
    }

    override fun markRatingCompleted() {
        appRatingPrefs.promptDateMillis = Calendar.getInstance().timeInMillis
        appRatingPrefs.completed = true
    }

    override fun saveRatingDateForLater() {
        appRatingPrefs.promptDateMillis = Calendar.getInstance().timeInMillis
    }
}
