package piuk.blockchain.android.ui.launcher.loader

import com.blockchain.core.user.NabuUserDataManager
import com.blockchain.notifications.NotificationTokenManager
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.notifications.analytics.AnalyticsEvent
import com.blockchain.notifications.analytics.AnalyticsNames
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.WalletStatus
import info.blockchain.wallet.api.data.Settings
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.kotlin.zipWith
import piuk.blockchain.android.ui.launcher.DeepLinkPersistence
import piuk.blockchain.android.ui.launcher.Prerequisites
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.settings.SettingsDataManager
import piuk.blockchain.androidcore.utils.PersistentPrefs
import java.io.Serializable

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
    fun initSettings(isAfterWalletCreation: Boolean): Observable<LoaderIntents> {
        return Observable.create { emitter ->
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
                    if (prefs.isNewlyCreated)
                        setCurrencyUnits(it)
                }
            }

            val metadata = Completable.defer { prerequisites.initMetadataAndRelatedPrerequisites() }

            emitter.onNext(LoaderIntents.UpdateProgressStep(ProgressStep.START))
            settings.zipWith(
                metadata.toSingleDefault(true)
            ).map {
                if (!shouldCheckForEmailVerification())
                    false
                else {
                    isAfterWalletCreation
                }
            }.flatMap { _isAfterWalletCreation ->
                saveInitialCountry(_isAfterWalletCreation)
            }.flatMap { emailVerifShouldLaunched ->
                if (noCurrencySet())
                    settingsDataManager.setDefaultUserFiat()
                        .map { emailVerifShouldLaunched }
                else {
                    Single.just(emailVerifShouldLaunched)
                }
            }.doOnSuccess {
                walletPrefs.isAppUnlocked = true
                analytics.logEvent(LoginAnalyticsEvent)
            }.flatMap { emailVerifShouldLaunched ->
                notificationTokenManager.resendNotificationToken()
                    .onErrorComplete()
                    .toSingle { emailVerifShouldLaunched }
            }.flatMap { emailVerifShouldLaunched ->
                emitter.onNext(LoaderIntents.UpdateProgressStep(ProgressStep.LOADING_PRICES))
                prerequisites.warmCaches()
                    .toSingle { emailVerifShouldLaunched }
                    .subscribeOn(ioScheduler)
            }.doOnSubscribe {
                emitter.onNext(LoaderIntents.UpdateProgressStep(ProgressStep.SYNCING_ACCOUNT))
            }.subscribeBy(
                onSuccess = { emailVerifShouldLaunched ->
                    if (emailVerifShouldLaunched) {
                        emitter.onNext(LoaderIntents.UpdateLoaderStep(LoaderStep.EmailVerification))
                        emitter.onNext(LoaderIntents.UpdateProgressStep(ProgressStep.FINISH))
                    } else {
                        emitter.onNext(
                            LoaderIntents.UpdateLoaderStep(
                                LoaderStep.Main(deepLinkPersistence.popDataFromSharedPrefs(), false)
                            )
                        )
                    }
                    emitter.onComplete()
                }, onError = { throwable ->
                    emitter.onError(throwable)
                }
            )
        }
    }

    private fun setCurrencyUnits(settings: Settings) {
        prefs.selectedFiatCurrency = settings.currency
    }

    private fun shouldCheckForEmailVerification() = prefs.isNewlyCreated && !prefs.isRestored

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

    private fun noCurrencySet() =
        currencyPrefs.selectedFiatCurrency.isEmpty()

    object LoginAnalyticsEvent : AnalyticsEvent {
        override val event: String
            get() = AnalyticsNames.SIGNED_IN.eventName
        override val params: Map<String, Serializable>
            get() = mapOf()
    }
}