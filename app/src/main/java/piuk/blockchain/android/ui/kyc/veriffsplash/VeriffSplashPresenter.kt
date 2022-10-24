package piuk.blockchain.android.ui.kyc.veriffsplash

import com.blockchain.analytics.Analytics
import com.blockchain.analytics.AnalyticsEvent
import com.blockchain.analytics.events.KYCAnalyticsEvents
import com.blockchain.api.NabuApiException
import com.blockchain.api.NabuErrorStatusCodes
import com.blockchain.core.kyc.data.datasources.KycTiersStore
import com.blockchain.nabu.api.getuser.domain.UserService
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.NabuDataManager
import com.blockchain.nabu.models.responses.nabu.KycState
import com.blockchain.preferences.SessionPrefs
import com.blockchain.veriff.VeriffApplicantAndToken
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.Singles
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.base.BasePresenter
import piuk.blockchain.android.ui.kyc.navhost.models.UiState
import timber.log.Timber

class VeriffSplashPresenter(
    private val userService: UserService,
    private val custodialWalletManager: CustodialWalletManager,
    private val nabuDataManager: NabuDataManager,
    private val kycTiersStore: KycTiersStore,
    private val prefs: SessionPrefs,
    private val analytics: Analytics
) : BasePresenter<VeriffSplashView>() {

    private var applicantToken: VeriffApplicantAndToken? = null

    override fun onViewReady() {
        updateUiState(UiState.LOADING)
        fetchRequiredDocumentList()
        fetchVeriffStartApplicantToken()

        compositeDisposable +=
            view.nextClick
                .subscribe {
                    @Suppress("ConstantConditionIf")
                    // In some DEBUG builds - but ONLY in DEBUG builds - it can be useful to skip the veriff kyc steps:
                    if (BuildConfig.DEBUG && BuildConfig.SKIP_VERIFF_KYC) {
                        view.continueToCompletion(KycState.Verified, false)
                    } else {
                        view.continueToVeriff(applicantToken!!)
                    }
                }

        compositeDisposable +=
            view.swapClick
                .subscribe { view.continueToSwap() }
    }

    private fun fetchRequiredDocumentList() {
        compositeDisposable += nabuDataManager.getSupportedDocuments(view.countryCode)
            .doOnError(Timber::e)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy { documents ->
                view.supportedDocuments(documents)
            }
    }

    private fun fetchVeriffStartApplicantToken() {
        compositeDisposable +=
            nabuDataManager.startVeriffSession()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = {
                        applicantToken = it
                        updateUiState(UiState.CONTENT)
                    },
                    onError = { e ->
                        Timber.e(e)
                        if (e is NabuApiException) {
                            handleSessionStartError(e)
                        } else {
                            view.showError(R.string.kyc_veriff_splash_verification_error)
                        }
                    }
                )
    }

    private fun handleSessionStartError(e: NabuApiException) {
        when (e.getErrorStatusCode()) {
            // If we get a pre-IDV check failed, then this device is now blacklisted and so won't be able to
            // get to tier 2 verification. Remember this in prefs, so that the UI can avoid showing 'upgrade'
            // announcements etc
            NabuErrorStatusCodes.PreIDVCheckFailed,
            // Or did we try to register with a duplicate email?
            NabuErrorStatusCodes.Conflict -> {
                prefs.devicePreIDVCheckFailed = true
                updateUiState(UiState.FAILURE)
            }
            // For anything else, just show the 'failed' toast as before:
            NabuErrorStatusCodes.TokenExpired,
            NabuErrorStatusCodes.Unknown -> view.showError(R.string.kyc_veriff_splash_verification_error)
            NabuErrorStatusCodes.InternalServerError,
            NabuErrorStatusCodes.BadRequest,
            NabuErrorStatusCodes.Forbidden,
            NabuErrorStatusCodes.NotFound -> {
            }
        }
    }

    internal fun submitVerification() {
        compositeDisposable +=
            nabuDataManager.submitVeriffVerification()
                .andThen(
                    Singles.zip(
                        userService.getUser().map { it.kycState },
                        custodialWalletManager.fetchSimplifiedDueDiligenceUserState().map { it.isVerified }
                    )
                )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { view.showProgressDialog(false) }
                .doOnTerminate { view.dismissProgressDialog() }
                .doOnError(Timber::e)
                .subscribeBy(
                    onSuccess = { (kycState, isSddVerified) ->
                        analytics.logEvent(KYCAnalyticsEvents.VeriffInfoSubmitted)
                        kycTiersStore.markAsStale()
                        view.continueToCompletion(kycState, isSddVerified)
                    },
                    onError = {
                        view.showError(R.string.kyc_veriff_splash_verification_error)
                    }
                )
    }

    internal fun onProgressCancelled() {
        // Clear outbound requests
        compositeDisposable.clear()
        // Resubscribe
        onViewReady()
    }

    private fun updateUiState(@UiState.UiStateDef state: Int) {
        view?.setUiState(state)

        val params = when (state) {
            UiState.CONTENT -> mapOf("result" to "START_KYC")
            UiState.FAILURE -> mapOf("result" to "UNAVAILABLE")
            else -> null
        }

        params?.let {
            analytics.logEvent(object : AnalyticsEvent {
                override val event = "kyc_splash_request_gold_preIDV"
                override val params = it
            }
            )
        }
    }
}
