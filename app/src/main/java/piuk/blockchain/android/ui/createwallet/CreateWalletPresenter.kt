package piuk.blockchain.android.ui.createwallet

import androidx.annotation.StringRes
import com.blockchain.analytics.Analytics
import com.blockchain.analytics.ProviderSpecificAnalytics
import com.blockchain.analytics.events.AnalyticsEvents
import com.blockchain.domain.common.model.CountryIso
import com.blockchain.domain.eligibility.EligibilityService
import com.blockchain.enviroment.EnvironmentConfig
import info.blockchain.wallet.payload.data.Wallet
import info.blockchain.wallet.util.PasswordUtil
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import java.util.Locale
import kotlin.math.roundToInt
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.base.BasePresenter
import piuk.blockchain.android.ui.base.View
import piuk.blockchain.android.ui.createwallet.CreateWalletActivity.Companion.CODE_US
import piuk.blockchain.android.util.AppUtil
import piuk.blockchain.android.util.FormatChecker
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.utils.PersistentPrefs
import timber.log.Timber

interface CreateWalletView : View {
    fun showError(@StringRes message: Int)
    fun warnWeakPassword(email: String, password: String)
    fun startPinEntryActivity(referral: String?)
    fun showProgressDialog(message: Int)
    fun dismissProgressDialog()
    fun getDefaultAccountName(): String
    fun setEligibleCountries(countries: List<CountryIso>)
    fun showReferralInvalidMessage()
    fun hideReferralInvalidMessage()
}

class CreateWalletPresenter(
    private val payloadDataManager: PayloadDataManager,
    private val prefs: PersistentPrefs,
    private val appUtil: AppUtil,
    private val specificAnalytics: ProviderSpecificAnalytics,
    private val analytics: Analytics,
    private val environmentConfig: EnvironmentConfig,
    private val formatChecker: FormatChecker,
    private val eligibilityService: EligibilityService,
    private val referralInteractor: ReferralInteractor
) : BasePresenter<CreateWalletView>() {

    override fun onViewReady() {
        eligibilityService.getCustodialEligibleCountries()
            .subscribeBy(
                onSuccess = { eligibleCountries ->
                    view.setEligibleCountries(eligibleCountries)
                },
                onError = {
                    view.setEligibleCountries(Locale.getISOCountries().toList())
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

    fun validateReferralFormat(referralCode: String?): Boolean {
        return when {
            referralCode.isNullOrEmpty() || referralCode.length == 8 -> {
                view.hideReferralInvalidMessage()
                true
            }
            else -> {
                view.showReferralInvalidMessage()
                false
            }
        }
    }

    fun createOrRestoreWallet(
        email: String,
        password: String,
        recoveryPhrase: String,
        countryCode: String,
        stateIsoCode: String? = null,
        referralCode: String
    ) = when {
        recoveryPhrase.isNotEmpty() -> recoverWallet(email, password, recoveryPhrase)
        else -> createWallet(email, password, countryCode, stateIsoCode, referralCode)
    }

    private fun createWallet(
        emailEntered: String,
        password: String,
        countryCode: String,
        stateIsoCode: String? = null,
        referralCode: String
    ) {
        analytics.logEventOnce(AnalyticsEvents.WalletSignupCreated)

        compositeDisposable += referralInteractor.validateReferralIfNeeded(referralCode)
            .flatMap { codeState ->
                if (codeState != ReferralCodeState.INVALID) {
                    payloadDataManager.createHdWallet(
                        password,
                        view.getDefaultAccountName(),
                        emailEntered
                    )
                        .map { wallet ->
                            CreateWalletResult.WalletCreated(wallet) as CreateWalletResult
                        }
                        .onErrorReturn { CreateWalletResult.CreateWalletError(it) }
                } else {
                    Single.just(CreateWalletResult.ReferralInvalid)
                }
            }
            .onErrorReturn { CreateWalletResult.ValidateReferralError(it) }
            .doOnSubscribe { view.showProgressDialog(R.string.creating_wallet) }
            .doOnTerminate { view.dismissProgressDialog() }
            .subscribeBy(
                onSuccess = { result ->
                    when (result) {
                        is CreateWalletResult.WalletCreated -> handleWalletCreateSuccess(
                            wallet = result.wallet,
                            countryCode = countryCode,
                            stateIsoCode = stateIsoCode,
                            emailEntered = emailEntered,
                            referralCode = referralCode
                        )
                        is CreateWalletResult.ReferralInvalid -> view.showReferralInvalidMessage()
                        is CreateWalletResult.ValidateReferralError -> handleValidationError(result.t)
                        is CreateWalletResult.CreateWalletError -> handleWalletCreateError(result.t)
                    }
                },
                onError = { handleWalletCreateError(it) }
            )
    }

    private fun handleWalletCreateSuccess(
        wallet: Wallet,
        countryCode: String,
        stateIsoCode: String?,
        emailEntered: String,
        referralCode: String?
    ) {
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
        view.startPinEntryActivity(referralCode)
        specificAnalytics.logSingUp(true)
    }

    private fun handleWalletCreateError(throwable: Throwable) {
        Timber.e(throwable)
        view.showError(R.string.hd_error)
        appUtil.clearCredentialsAndRestart()
        specificAnalytics.logSingUp(false)
    }

    private fun handleValidationError(throwable: Throwable) {
        Timber.e(throwable)
        view.showReferralInvalidMessage()
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
                    }
                    view.startPinEntryActivity(null)
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

private sealed class CreateWalletResult {
    object ReferralInvalid : CreateWalletResult()
    data class WalletCreated(val wallet: Wallet) : CreateWalletResult()
    data class ValidateReferralError(val t: Throwable) : CreateWalletResult()
    data class CreateWalletError(val t: Throwable) : CreateWalletResult()
}
