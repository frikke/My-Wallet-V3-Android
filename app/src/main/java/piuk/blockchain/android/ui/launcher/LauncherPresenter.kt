package piuk.blockchain.android.ui.launcher

import android.content.Intent
import com.blockchain.core.user.NabuUserDataManager
import com.blockchain.logging.CrashLogger
import com.blockchain.nabu.Feature
import com.blockchain.nabu.UserIdentity
import com.blockchain.notifications.NotificationTokenManager
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.notifications.analytics.AnalyticsEvent
import com.blockchain.notifications.analytics.AnalyticsEvents
import com.blockchain.notifications.analytics.AnalyticsNames
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.WalletStatus
import info.blockchain.wallet.api.Environment
import info.blockchain.wallet.api.data.Settings
import info.blockchain.wallet.exceptions.HDWalletException
import info.blockchain.wallet.exceptions.InvalidCredentialsException
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.kotlin.zipWith
import piuk.blockchain.android.R
import piuk.blockchain.android.sdd.SDDAnalytics
import piuk.blockchain.android.ui.base.BasePresenter
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.util.AppUtil
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.metadata.MetadataInitException
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.settings.SettingsDataManager
import piuk.blockchain.androidcore.utils.PersistentPrefs
import timber.log.Timber
import java.io.Serializable

class LauncherPresenter(
    private val appUtil: AppUtil,
    private val payloadDataManager: PayloadDataManager,
    private val prefs: PersistentPrefs,
    private val deepLinkPersistence: DeepLinkPersistence,
    private val settingsDataManager: SettingsDataManager,
    private val notificationTokenManager: NotificationTokenManager,
    private val envSettings: EnvironmentConfig,
    private val currencyPrefs: CurrencyPrefs,
    private val analytics: Analytics,
    private val prerequisites: Prerequisites,
    private val userIdentity: UserIdentity,
    private val crashLogger: CrashLogger,
    private val walletPrefs: WalletStatus,
    private val nabuUserDataManager: NabuUserDataManager
) : BasePresenter<LauncherView>() {

    override fun onViewReady() {
        analytics.logEventOnce(AnalyticsEvents.WalletSignupOpen)

        val viewIntentData = view.getViewIntentData()

        // Store incoming bitcoin URI if needed
        if (
            viewIntentData.action == Intent.ACTION_VIEW &&
            viewIntentData.scheme == "bitcoin" &&
            viewIntentData.data != null
        ) {
            prefs.setValue(
                PersistentPrefs.KEY_SCHEME_URL,
                viewIntentData.data
            )
        }
        if (viewIntentData.action == Intent.ACTION_VIEW && viewIntentData.data != null) {
            deepLinkPersistence.pushDeepLink(viewIntentData.data)
        }

        if (
            Intent.ACTION_VIEW == viewIntentData.action &&
            viewIntentData.dataString?.contains("blockchain") == true
        ) {
            prefs.setValue(PersistentPrefs.KEY_METADATA_URI, viewIntentData.dataString)
        }

        val isPinValidated = viewIntentData.isPinValidated

        if (viewIntentData.isAutomationTesting && Environment.STAGING == envSettings.environment) {
            prefs.setIsUnderTest()
        }

        val hasBackup = prefs.hasBackup()
        val pin = prefs.pinId
        val isLoggedOut = prefs.isLoggedOut
        val walletGuid = prefs.walletGuid

        when {
            // No GUID and no backup? Treat as new installation
            walletGuid.isEmpty() && !hasBackup -> view.onNoGuid()
            // No GUID but a backup. Show PIN entry page to populate other values
            walletGuid.isEmpty() && hasBackup -> view.onRequestPin()
            // User has logged out recently. Show password reentry page
            isLoggedOut -> view.onReEnterPassword()
            // No PIN ID? Treat as installed app without confirmed PIN
            pin.isEmpty() -> view.onRequestPin()
            // Installed app, check sanity
            !appUtil.isSane -> view.onCorruptPayload()
            // App has been PIN validated
            isPinValidated && !isLoggedOut -> initSettings()
            // Something odd has happened, re-request PIN
            else -> view.onRequestPin()
        }
    }

    fun clearCredentialsAndRestart() =
        appUtil.clearCredentialsAndRestart()

    /**
     * Init of the [SettingsDataManager] must complete here so that we can access the [Settings]
     * object from memory when the user is logged in.
     */
    private fun initSettings() {

        val settings = Single.defer {
            Single.just(payloadDataManager.wallet!!)
        }.flatMap { wallet ->
            prerequisites.initSettings(
                wallet.guid,
                wallet.sharedKey
            ).doOnSuccess {
                // If the account is new, we need to check if we should launch Simple buy flow
                // (in that case, currency will be selected by user manually)
                // or select the default from device Locale
                if (!isNewAccount())
                    setCurrencyUnits(it)
            }
        }

        val metadata = Completable.defer { prerequisites.initMetadataAndRelatedPrerequisites() }

        compositeDisposable +=
            settings.zipWith(
                metadata.toSingleDefault(true)
            ).map {
                if (!shouldCheckForEmailVerification())
                    false
                else {
                    walletJustCreated()
                }
            }.flatMap { isWalletJustCreated ->
                saveInitialCountry(isWalletJustCreated)
            }.flatMap { emailVerifShouldLaunched ->
                if (noCurrencySet())
                    settingsDataManager.setDefaultUserFiat()
                        .map { emailVerifShouldLaunched }
                else {
                    Single.just(emailVerifShouldLaunched)
                }
            }.doOnSuccess {
                walletPrefs.isLoggedOut = false
                analytics.logEvent(LoginAnalyticsEvent)
            }.flatMap { emailVerifShouldLaunched ->
                notificationTokenManager.resendNotificationToken()
                    .onErrorComplete()
                    .toSingle { emailVerifShouldLaunched }
            }.flatMap { emailVerifShouldLaunched ->
                prerequisites.warmCaches()
                    .toSingle { emailVerifShouldLaunched }
            }.observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe {
                    view.updateProgressVisibility(true)
                }.subscribeBy(
                    onSuccess = { emailVerifShouldLaunched ->
                        view.updateProgressVisibility(false)
                        if (emailVerifShouldLaunched) {
                            launchEmailVerification()
                        } else {
                            startMainActivity()
                        }
                    }, onError = { throwable ->
                        handleOnErrorLauncher(throwable)
                    }
                )
    }

    private fun handleOnErrorLauncher(throwable: Throwable) {
        logException(throwable)
        view.updateProgressVisibility(false)
        if (throwable is InvalidCredentialsException || throwable is HDWalletException) {
            if (payloadDataManager.isDoubleEncrypted) {
                // Wallet double encrypted and needs to be decrypted to set up ether wallet, contacts etc
                view?.showSecondPasswordDialog()
            } else {
                view.showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR)
                view.onRequestPin()
            }
        } else if (throwable is MetadataInitException) {
            view?.showMetadataNodeFailure()
        } else {
            view.showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR)
            view.onRequestPin()
        }
    }

    private fun saveInitialCountry(isWalletJustCreated: Boolean): Single<Boolean> {
        val countrySelected = walletPrefs.countrySelectedOnSignUp
        return if (countrySelected.isNotEmpty()) {
            val stateSelected = walletPrefs.stateSelectedOnSignUp
            nabuUserDataManager.saveUserInitialLocation(
                countrySelected,
                stateSelected.takeIf { it.isNotEmpty() }
            ).doOnComplete {
                prefs.clearGeolocationPreferences()
            }.onErrorComplete().toSingleDefault(isWalletJustCreated)
        } else {
            Single.just(isWalletJustCreated)
        }
    }

    private fun isNewAccount(): Boolean = prefs.isNewlyCreated

    private fun walletJustCreated() =
        view?.getViewIntentData()?.isAfterWalletCreation ?: false

    internal fun decryptAndSetupMetadata(secondPassword: String) {
        if (!payloadDataManager.validateSecondPassword(secondPassword)) {
            view?.showToast(R.string.invalid_password, ToastCustom.TYPE_ERROR)
            view?.showSecondPasswordDialog()
        } else {
            compositeDisposable += prerequisites.decryptAndSetupMetadata(secondPassword)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe {
                    view.updateProgressVisibility(true)
                }.subscribeBy(
                    onError = {
                        view.updateProgressVisibility(false)
                        Timber.e(it)
                    },
                    onComplete = {
                        view.updateProgressVisibility(false)
                        appUtil.restartAppWithVerifiedPin(
                            piuk.blockchain.android.ui.launcher.LauncherActivity::class.java
                        )
                    }
                )
        }
    }

    private fun noCurrencySet() =
        currencyPrefs.selectedFiatCurrency.isEmpty()

    private fun logException(throwable: Throwable) {
        crashLogger.logEvent("Startup exception: ${throwable.message}")
        crashLogger.logException(throwable)
    }

    private fun shouldCheckForEmailVerification() = prefs.isNewlyCreated && !prefs.isRestored

    private fun startMainActivity() {
        view.onStartMainActivity(deepLinkPersistence.popUriFromSharedPrefs())
    }

    private fun launchEmailVerification() {
        view.launchEmailVerification()
    }

    private fun setCurrencyUnits(settings: Settings) {
        prefs.selectedFiatCurrency = settings.currency
    }

    fun onEmailVerificationFinished() {
        compositeDisposable += userIdentity.isEligibleFor(Feature.SimplifiedDueDiligence)
            .onErrorReturn { false }
            .doOnSuccess {
                if (it)
                    analytics.logEventOnce(SDDAnalytics.SDD_ELIGIBLE)
            }
            .subscribeBy(
                onSuccess = {
                    view.onStartMainActivity(null, it)
                }, onError = {}
            )
    }
}

object LoginAnalyticsEvent : AnalyticsEvent {
    override val event: String
        get() = AnalyticsNames.SIGNED_IN.eventName
    override val params: Map<String, Serializable>
        get() = mapOf()
}