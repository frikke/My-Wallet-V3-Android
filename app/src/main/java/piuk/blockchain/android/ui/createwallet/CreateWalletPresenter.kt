package piuk.blockchain.android.ui.createwallet

import androidx.annotation.StringRes
import com.blockchain.api.services.Geolocation
import com.blockchain.core.CountryIso
import com.blockchain.core.EligibilityDataManager
import com.blockchain.core.user.NabuUserDataManager
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.notifications.analytics.AnalyticsEvents
import com.blockchain.notifications.analytics.ProviderSpecificAnalytics
import info.blockchain.wallet.util.PasswordUtil
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.kotlin.zipWith
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.Locale
import kotlin.math.roundToInt
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.base.BasePresenter
import piuk.blockchain.android.ui.base.View
import piuk.blockchain.android.ui.createwallet.CreateWalletActivity.Companion.CODE_US
import piuk.blockchain.android.util.AppUtil
import piuk.blockchain.android.util.FormatChecker
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.utils.PersistentPrefs
import timber.log.Timber

interface CreateWalletView : View {
    fun showError(@StringRes message: Int)
    fun warnWeakPassword(email: String, password: String)
    fun startPinEntryActivity()
    fun showProgressDialog(message: Int)
    fun dismissProgressDialog()
    fun getDefaultAccountName(): String
    fun setEligibleCountries(countries: List<CountryIso>)
    fun setGeolocationInCountrySpinner(geolocation: Geolocation)
}

class CreateWalletPresenter(
    private val payloadDataManager: PayloadDataManager,
    private val prefs: PersistentPrefs,
    private val appUtil: AppUtil,
    private val specificAnalytics: ProviderSpecificAnalytics,
    private val analytics: Analytics,
    private val environmentConfig: EnvironmentConfig,
    private val formatChecker: FormatChecker,
    private val nabuUserDataManager: NabuUserDataManager,
    private val eligibilityDataManager: EligibilityDataManager
) : BasePresenter<CreateWalletView>() {

    override fun onViewReady() {
        eligibilityDataManager.getCustodialEligibleCountries()
            .subscribeBy(
                onSuccess = { eligibleCountries ->
                    view.setEligibleCountries(eligibleCountries)
                },
                onError = {
                    view.setEligibleCountries(Locale.getISOCountries().toList())
                }
            )
    }

    fun getUserGeolocation() {
        nabuUserDataManager.getUserGeolocation()
            .zipWith(eligibilityDataManager.getCustodialEligibleCountries())
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread()).subscribeBy(
                onSuccess = { (geolocation, eligibleCountries) ->
                    if (eligibleCountries.contains(geolocation.countryCode)) {
                        view.setGeolocationInCountrySpinner(geolocation)
                    }
                },
                onError = {
                    Timber.e(it.localizedMessage)
                }
            )
    }

    fun validateCredentials(email: String, password1: String, password2: String): Boolean =
        when {
            !formatChecker.isValidEmailAddress(email) -> {
                view.showError(R.string.invalid_email); false
            }
            password1.length < MIN_PWD_LENGTH -> {
                view.showError(R.string.invalid_password_too_short); false
            }
            password1.length > MAX_PWD_LENGTH -> {
                view.showError(R.string.invalid_password); false
            }
            password1 != password2 -> {
                view.showError(R.string.password_mismatch_error); false
            }
            !PasswordUtil.getStrength(password1).roundToInt().isStrongEnough() -> {
                view.warnWeakPassword(email, password1); false
            }
            else -> true
        }

    fun validateGeoLocation(countryCode: String? = null, stateCode: String? = null): Boolean =
        when {
            countryCode.isNullOrBlank() -> {
                view.showError(R.string.country_not_selected)
                false
            }
            countryCode == CODE_US && stateCode.isNullOrBlank() -> {
                view.showError(R.string.state_not_selected)
                false
            }
            else -> true
        }

    fun createOrRestoreWallet(
        email: String,
        password: String,
        recoveryPhrase: String,
        countryCode: String,
        stateIsoCode: String? = null
    ) = when {
        recoveryPhrase.isNotEmpty() -> recoverWallet(email, password, recoveryPhrase)
        else -> createWallet(email, password, countryCode, stateIsoCode)
    }

    private fun createWallet(
        emailEntered: String,
        password: String,
        countryCode: String,
        stateIsoCode: String? = null
    ) {
        analytics.logEventOnce(AnalyticsEvents.WalletSignupCreated)

        compositeDisposable += payloadDataManager.createHdWallet(
            password,
            view.getDefaultAccountName(),
            emailEntered
        )
            .doOnSubscribe { view.showProgressDialog(R.string.creating_wallet) }
            .doOnTerminate { view.dismissProgressDialog() }
            .subscribeBy(
                onSuccess = { wallet ->
                    prefs.isNewlyCreated = true
                    analytics.logEvent(WalletCreationAnalytics.WalletSignUp(countryCode, stateIsoCode))
                    prefs.apply {
                        walletGuid = wallet.guid
                        sharedKey = wallet.sharedKey
                        countrySelectedOnSignUp = countryCode
                        stateIsoCode?.let { stateSelectedOnSignUp = it }
                        email = emailEntered
                    }
                    analytics.logEvent(AnalyticsEvents.WalletCreation)
                    view.startPinEntryActivity()
                    specificAnalytics.logSingUp(true)
                },
                onError = {
                    Timber.e(it)
                    view.showError(R.string.hd_error)
                    appUtil.clearCredentialsAndRestart()
                    specificAnalytics.logSingUp(false)
                }
            )
    }

    private fun recoverWallet(
        emailEntered: String,
        password: String,
        recoveryPhrase: String
    ) {
        compositeDisposable += payloadDataManager.restoreHdWallet(
            recoveryPhrase,
            view.getDefaultAccountName(),
            emailEntered,
            password
        )
            .doOnSubscribe {
                view.showProgressDialog(R.string.restoring_wallet)
            }.doOnTerminate {
                view.dismissProgressDialog()
            }.subscribeBy(
                onSuccess = { wallet ->
                    prefs.apply {
                        isNewlyCreated = true
                        isRestored = true
                        walletGuid = wallet.guid
                        sharedKey = wallet.sharedKey
                        email = emailEntered
                        isOnBoardingComplete = true
                    }
                    view.startPinEntryActivity()
                },
                onError = {
                    Timber.e(it)
                    view.showError(R.string.restore_failed)
                }
            )
    }

    fun logEventEmailClicked() = analytics.logEventOnce(AnalyticsEvents.WalletSignupClickEmail)

    fun logEventPasswordOneClicked() =
        analytics.logEventOnce(AnalyticsEvents.WalletSignupClickPasswordFirst)

    fun logEventPasswordTwoClicked() =
        analytics.logEventOnce(AnalyticsEvents.WalletSignupClickPasswordSecond)

    private fun Int.isStrongEnough(): Boolean {
        val limit = if (environmentConfig.isRunningInDebugMode()) 1 else 50
        return this >= limit
    }

    companion object {
        private const val MIN_PWD_LENGTH = 4
        private const val MAX_PWD_LENGTH = 255
    }
}
