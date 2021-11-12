package piuk.blockchain.android.ui.launcher.loader

import com.blockchain.core.user.NabuUserDataManager
import com.blockchain.notifications.NotificationTokenManager
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.notifications.analytics.AnalyticsEvent
import com.blockchain.notifications.analytics.AnalyticsNames
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.WalletStatus
import info.blockchain.wallet.api.data.Settings
import info.blockchain.wallet.payload.data.Wallet
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.subjects.BehaviorSubject
import java.io.Serializable
import piuk.blockchain.android.ui.launcher.DeepLinkPersistence
import piuk.blockchain.android.ui.launcher.Prerequisites
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.settings.SettingsDataManager
import piuk.blockchain.androidcore.utils.PersistentPrefs
import piuk.blockchain.androidcore.utils.extensions.then

class LoaderInteractor(
    private val payloadDataManager: PayloadDataManager,
    private val prerequisites: Prerequisites,
    private val prefs: PersistentPrefs,
    private val deepLinkPersistence: DeepLinkPersistence,
    private val settingsDataManager: SettingsDataManager,
    private val notificationTokenManager: NotificationTokenManager,
    private val currencyPrefs: CurrencyPrefs,
    private val nabuUserDataManager: NabuUserDataManager,
    private val walletPrefs: WalletStatus,
    private val analytics: Analytics,
    private val ioScheduler: Scheduler
) {

    private val wallet: Wallet
        get() = payloadDataManager.wallet!!

    private val metadata
        get() = Completable.defer { prerequisites.initMetadataAndRelatedPrerequisites() }

    private val settings: Completable
        get() = Completable.defer {
            prerequisites.initSettings(
                wallet.guid,
                wallet.sharedKey
            ).doOnSuccess {
                // this is temporary. There will be some designs so we can set the user currency
                // from java locale but cannot be done until we have designs for buy and
                // currency selection. So for now, we set settings def that is USD.
                if (prefs.isNewlyCreated)
                    setCurrencyUnits(it)
            }.ignoreElement()
        }

    private val warmCaches: Completable
        get() = prerequisites.warmCaches().subscribeOn(ioScheduler)

    private val emitter =
        BehaviorSubject.createDefault<LoaderIntents>(LoaderIntents.UpdateProgressStep(ProgressStep.START))

    val loaderIntents: Observable<LoaderIntents>
        get() = emitter

    private val notificationTokenUpdate: Completable
        get() = notificationTokenManager.resendNotificationToken().onErrorComplete()

    fun initSettings(isAfterWalletCreation: Boolean): Disposable {
        return settings.then {
            metadata
        }.then {
            saveInitialCountry()
        }.then {
            updateUserFiatIfNotSet()
        }.then {
            notificationTokenUpdate
        }.then {
            warmCaches.doOnSubscribe { emitter.onNext(LoaderIntents.UpdateProgressStep(ProgressStep.LOADING_PRICES)) }
        }.doOnSubscribe {
            emitter.onNext(LoaderIntents.UpdateProgressStep(ProgressStep.SYNCING_ACCOUNT))
        }.subscribeBy(
            onComplete = {
                onInitSettingsSuccess(isAfterWalletCreation && shouldCheckForEmailVerification())
            }, onError = { throwable ->
            emitter.onNext(LoaderIntents.UpdateLoadingStep(LoadingStep.Error(throwable)))
        }
        )
    }

    private fun onInitSettingsSuccess(shouldLaunchEmailVerification: Boolean) {
        if (shouldLaunchEmailVerification) {
            emitter.onNext(LoaderIntents.UpdateLoadingStep(LoadingStep.EmailVerification))
            emitter.onNext(LoaderIntents.UpdateProgressStep(ProgressStep.FINISH))
        } else {
            emitter.onNext(
                LoaderIntents.UpdateLoadingStep(
                    LoadingStep.Main(deepLinkPersistence.popDataFromSharedPrefs(), false)
                )
            )
        }
        emitter.onComplete()
        walletPrefs.isAppUnlocked = true
        analytics.logEvent(LoginAnalyticsEvent)
    }

    private fun updateUserFiatIfNotSet(): Completable {
        return if (noCurrencySet())
            settingsDataManager.setDefaultUserFiat().ignoreElement()
        else {
            Completable.complete()
        }
    }

    private fun shouldCheckForEmailVerification() = prefs.isNewlyCreated && !prefs.isRestored

    private fun saveInitialCountry(): Completable {
        val countrySelected = walletPrefs.countrySelectedOnSignUp
        return if (countrySelected.isNotEmpty()) {
            val stateSelected = walletPrefs.stateSelectedOnSignUp
            nabuUserDataManager.saveUserInitialLocation(
                countrySelected,
                stateSelected.takeIf { it.isNotEmpty() }
            ).doOnComplete {
                prefs.clearGeolocationPreferences()
            }.onErrorComplete()
        } else Completable.complete()
    }

    private fun setCurrencyUnits(settings: Settings) {
        prefs.selectedFiatCurrency = settings.currency
    }

    private fun noCurrencySet() =
        currencyPrefs.selectedFiatCurrency.isEmpty()

    object LoginAnalyticsEvent : AnalyticsEvent {
        override val event: String
            get() = AnalyticsNames.SIGNED_IN.eventName
        override val params: Map<String, Serializable>
            get() = mapOf()
    }
}
