package piuk.blockchain.android.ui.kyc.veriffsplash

import androidx.lifecycle.viewModelScope
import com.blockchain.analytics.Analytics
import com.blockchain.analytics.events.KYCAnalyticsEvents
import com.blockchain.api.NabuApiException
import com.blockchain.api.NabuErrorStatusCodes
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.core.kyc.data.datasources.KycTiersStore
import com.blockchain.core.kyc.domain.model.KycTier
import com.blockchain.domain.common.model.CountryIso
import com.blockchain.nabu.api.getuser.domain.UserService
import com.blockchain.nabu.datamanagers.NabuDataManager
import com.blockchain.nabu.models.responses.nabu.KycState
import com.blockchain.outcome.doOnFailure
import com.blockchain.outcome.doOnSuccess
import com.blockchain.outcome.flatMap
import com.blockchain.outcome.getOrDefault
import com.blockchain.preferences.SessionPrefs
import com.blockchain.utils.awaitOutcome
import com.blockchain.veriff.VeriffApplicantAndToken
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import piuk.blockchain.android.BuildConfig
import timber.log.Timber

sealed class Navigation : NavigationEvent {
    data class Veriff(val veriffApplicantAndToken: VeriffApplicantAndToken) : Navigation()
    data class TierCurrentState(val kycState: KycState) : Navigation()
}

@Parcelize
data class Args(
    val countryIso: CountryIso
) : ModelConfigArgs.ParcelableArgs

class VeriffSplashModel(
    private val userService: UserService,
    private val nabuDataManager: NabuDataManager,
    private val kycTiersStore: KycTiersStore,
    private val sessionPrefs: SessionPrefs,
    private val analytics: Analytics
) : MviViewModel<
    VeriffSplashIntent,
    VeriffSplashViewState,
    VeriffSplashModelState,
    Navigation,
    Args
    >(VeriffSplashModelState()) {

    private lateinit var veriffApplicantAndToken: VeriffApplicantAndToken

    override fun viewCreated(args: Args) {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, continueButtonState = ButtonState.Disabled) }
            val getSupportedDocumentsDeferred = async {
                nabuDataManager.getSupportedDocuments(args.countryIso).awaitOutcome()
            }
            val startVeriffSessionDeferred = async {
                nabuDataManager.startVeriffSession().awaitOutcome()
            }

            val supportedDocuments = getSupportedDocumentsDeferred.await().getOrDefault(emptyList())
            startVeriffSessionDeferred.await()
                .doOnFailure { error ->
                    Timber.e(error)
                    if (error is NabuApiException && (
                        // If we get a pre-IDV check failed, then this device is now blacklisted and so won't be able to
                        // get to tier 2 verification. Remember this in prefs, so that the UI can avoid showing 'upgrade'
                        // announcements etc
                        error.getErrorStatusCode() == NabuErrorStatusCodes.PreIDVCheckFailed ||
                            // Or did we try to register with a duplicate email?
                            error.getErrorStatusCode() == NabuErrorStatusCodes.Conflict
                        )
                    ) {
                        sessionPrefs.devicePreIDVCheckFailed = true
                        analytics.logEvent(KYCAnalyticsEvents.VeriffPreIDV("UNAVAILABLE"))
                        navigate(Navigation.TierCurrentState(KycState.Rejected))
                    } else {
                        updateState {
                            it.copy(
                                error = VeriffSplashError.Generic,
                                continueButtonState = ButtonState.Disabled
                            )
                        }
                    }
                }
                .doOnSuccess { veriffApplicantAndToken ->
                    this@VeriffSplashModel.veriffApplicantAndToken = veriffApplicantAndToken
                    analytics.logEvent(KYCAnalyticsEvents.VeriffPreIDV("START_KYC"))
                    updateState {
                        it.copy(
                            supportedDocuments = supportedDocuments.toSortedSet(),
                            continueButtonState = ButtonState.Enabled
                        )
                    }
                }
            updateState { it.copy(isLoading = false) }
        }
    }

    override fun reduce(state: VeriffSplashModelState): VeriffSplashViewState = VeriffSplashViewState(
        isLoading = state.isLoading,
        supportedDocuments = state.supportedDocuments,
        error = state.error,
        continueButtonState = state.continueButtonState
    )

    override suspend fun handleIntent(modelState: VeriffSplashModelState, intent: VeriffSplashIntent) {
        when (intent) {
            VeriffSplashIntent.ContinueClicked -> {
                if (BuildConfig.DEBUG && BuildConfig.SKIP_VERIFF_KYC) {
                    navigate(Navigation.TierCurrentState(KycState.Verified))
                } else {
                    navigate(Navigation.Veriff(veriffApplicantAndToken))
                }
            }
            VeriffSplashIntent.OnVeriffSuccess -> {
                updateState { it.copy(continueButtonState = ButtonState.Loading) }
                nabuDataManager.submitVeriffVerification().awaitOutcome()
                    .flatMap {
                        userService.getUser().map { it.kycState }.awaitOutcome()
                    }
                    .doOnSuccess { kycState ->
                        analytics.logEvent(KYCAnalyticsEvents.VeriffInfoSubmitted)
                        kycTiersStore.markAsStale()
                        navigate(Navigation.TierCurrentState(kycState))
                    }
                    .doOnFailure { error ->
                        updateState { it.copy(error = VeriffSplashError.Generic) }
                    }
                updateState { it.copy(continueButtonState = ButtonState.Enabled) }
            }
            is VeriffSplashIntent.OnVeriffFailure -> {
                analytics.logEvent(
                    VeriffAnalytics.VerifSubmissionFailed(
                        tierUserIsAboutToUpgrade = KycTier.GOLD,
                        failureReason = intent.error.orEmpty()
                    )
                )
                updateState {
                    it.copy(error = VeriffSplashError.Generic)
                }
            }
            VeriffSplashIntent.ErrorHandled -> updateState { it.copy(error = null) }
        }
    }
}
