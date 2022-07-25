package piuk.blockchain.android.ui.launcher.loader

import com.blockchain.analytics.Analytics
import com.blockchain.analytics.AnalyticsEvent
import com.blockchain.analytics.events.AnalyticsNames
import com.blockchain.core.user.NabuUserDataManager
import com.blockchain.domain.fiatcurrencies.FiatCurrenciesService
import com.blockchain.domain.referral.ReferralService
import com.blockchain.notifications.NotificationTokenManager
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.WalletStatusPrefs
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.FiatCurrency.Companion.Dollars
import info.blockchain.wallet.api.data.Settings
import info.blockchain.wallet.payload.data.Wallet
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.subjects.BehaviorSubject
import java.io.Serializable
import kotlinx.coroutines.rx3.rxCompletable
import piuk.blockchain.android.ui.launcher.DeepLinkPersistence
import piuk.blockchain.android.ui.launcher.Prerequisites
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.settings.SettingsDataManager
import piuk.blockchain.androidcore.utils.extensions.rxCompletableOutcome
import piuk.blockchain.androidcore.utils.extensions.then

class LoaderInteractor(
    private val payloadDataManager: PayloadDataManager,
    private val prerequisites: Prerequisites,
    private val deepLinkPersistence: DeepLinkPersistence,
    private val settingsDataManager: SettingsDataManager,
    private val notificationTokenManager: NotificationTokenManager,
    private val currencyPrefs: CurrencyPrefs,
    private val nabuUserDataManager: NabuUserDataManager,
    private val walletPrefs: WalletStatusPrefs,
    private val analytics: Analytics,
    private val assetCatalogue: AssetCatalogue,
    private val ioScheduler: Scheduler,
    private val referralService: ReferralService,
    private val fiatCurrenciesService: FiatCurrenciesService,
) {

    private val wallet: Wallet
        get() = payloadDataManager.wallet!!

    private val metadata
        get() = Completable.defer { prerequisites.initMetadataAndRelatedPrerequisites() }

    private val settings: Single<Settings>
        get() = Single.defer {
            prerequisites.initSettings(
                wallet.guid,
                wallet.sharedKey
            )
        }

    private val warmCaches: Completable
        get() = prerequisites.warmCaches().subscribeOn(ioScheduler)

    private val emitter =
        BehaviorSubject.createDefault<LoaderIntents>(LoaderIntents.UpdateProgressStep(ProgressStep.START))

    val loaderIntents: Observable<LoaderIntents>
        get() = emitter

    private val notificationTokenUpdate: Completable
        get() = notificationTokenManager.resendNotificationToken().onErrorComplete()

    fun initSettings(isAfterWalletCreation: Boolean, referralCode: String?): Disposable {
        return settings
            .flatMap {
                metadata.toSingle { it }
            }.flatMapCompletable {
                syncFiatCurrencies(it)
            }.then {
                saveInitialCountry()
            }.then {
                updateUserFiatIfNotSet()
            }.then {
                notificationTokenUpdate
            }.then {
                warmCaches.doOnSubscribe {
                    emitter.onNext(
                        LoaderIntents.UpdateProgressStep(ProgressStep.LOADING_PRICES)
                    )
                }
            }.then {
                rxCompletable {
                    referralService.associateReferralCodeIfPresent(referralCode)
                }
            }.doOnSubscribe {
                emitter.onNext(LoaderIntents.UpdateProgressStep(ProgressStep.SYNCING_ACCOUNT))
            }.subscribeBy(
                onComplete = {
                    onInitSettingsSuccess(isAfterWalletCreation && shouldCheckForEmailVerification())
                },
                onError = { throwable ->
                    emitter.onNext(LoaderIntents.UpdateLoadingStep(LoadingStep.Error(throwable)))
                }
            )
    }

    private fun syncFiatCurrencies(settings: Settings): Completable {
        val syncDisplayCurrency = when {
            walletPrefs.isNewlyCreated -> settingsDataManager.setDefaultUserFiat().ignoreElement()
            settings.currency != currencyPrefs.selectedFiatCurrency.networkTicker -> Completable.fromAction {
                currencyPrefs.selectedFiatCurrency =
                    assetCatalogue.fiatFromNetworkTicker(settings.currency) ?: Dollars
            }
            else -> Completable.complete()
        }

        // warm selectedTradingCurrency cache
        val syncTradingCurrency = rxCompletableOutcome { fiatCurrenciesService.getTradingCurrencies() }

        return syncDisplayCurrency.mergeWith(syncTradingCurrency)
    }

    private fun onInitSettingsSuccess(shouldLaunchEmailVerification: Boolean) {
        emitter.onNext(LoaderIntents.UpdateProgressStep(ProgressStep.FINISH))
        if (shouldLaunchEmailVerification) {
            emitter.onNext(LoaderIntents.UpdateLoadingStep(LoadingStep.EmailVerification))
        } else {
            emitter.onNext(
                LoaderIntents.LaunchDashboard(
                    data = deepLinkPersistence.popDataFromSharedPrefs(),
                    shouldLaunchUiTour = false
                )
            )
        }
        emitter.onComplete()
        walletPrefs.isAppUnlocked = true
        analytics.logEvent(LoginAnalyticsEvent)
    }

    private fun updateUserFiatIfNotSet(): Completable {
        return if (currencyPrefs.noCurrencySet)
            settingsDataManager.setDefaultUserFiat().ignoreElement()
        else {
            Completable.complete()
        }
    }

    private fun shouldCheckForEmailVerification() = walletPrefs.isNewlyCreated && !walletPrefs.isRestored

    private fun saveInitialCountry(): Completable {
        val countrySelected = walletPrefs.countrySelectedOnSignUp
        return if (countrySelected.isNotEmpty()) {
            val stateSelected = walletPrefs.stateSelectedOnSignUp
            nabuUserDataManager.saveUserInitialLocation(
                countrySelected,
                stateSelected.takeIf { it.isNotEmpty() }
            ).doOnComplete {
                walletPrefs.clearGeolocationPreferences()
            }.onErrorComplete()
        } else Completable.complete()
    }

    object LoginAnalyticsEvent : AnalyticsEvent {
        override val event: String
            get() = AnalyticsNames.SIGNED_IN.eventName
        override val params: Map<String, Serializable>
            get() = mapOf()
    }
}
