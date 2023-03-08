package piuk.blockchain.android.ui.launcher.loader

import com.blockchain.analytics.Analytics
import com.blockchain.analytics.AnalyticsEvent
import com.blockchain.analytics.events.AnalyticsNames
import com.blockchain.coincore.Coincore
import com.blockchain.core.experiments.cache.ExperimentsStore
import com.blockchain.core.kyc.domain.KycService
import com.blockchain.core.kyc.domain.model.KycTier
import com.blockchain.core.payload.PayloadDataManager
import com.blockchain.core.settings.SettingsDataManager
import com.blockchain.core.user.NabuUserDataManager
import com.blockchain.domain.fiatcurrencies.FiatCurrenciesService
import com.blockchain.domain.referral.ReferralService
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.nabu.UserIdentity
import com.blockchain.notifications.NotificationTokenManager
import com.blockchain.preferences.CowboysPrefs
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.WalletStatusPrefs
import com.blockchain.utils.rxCompletableOutcome
import com.blockchain.utils.then
import com.blockchain.utils.zipSingles
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
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
import piuk.blockchain.android.fraud.domain.service.FraudFlow
import piuk.blockchain.android.fraud.domain.service.FraudService
import piuk.blockchain.android.ui.launcher.DeepLinkPersistence
import piuk.blockchain.android.ui.launcher.Prerequisites

class LoaderInteractor(
    private val payloadDataManager: PayloadDataManager,
    private val prerequisites: Prerequisites,
    private val deepLinkPersistence: DeepLinkPersistence,
    private val settingsDataManager: SettingsDataManager,
    private val notificationTokenManager: NotificationTokenManager,
    private val currencyPrefs: CurrencyPrefs,
    private val nabuUserDataManager: NabuUserDataManager,
    private val walletPrefs: WalletStatusPrefs,
    private val walletModeServices: List<WalletModeService>,
    private val analytics: Analytics,
    private val assetCatalogue: AssetCatalogue,
    private val ioScheduler: Scheduler,
    private val referralService: ReferralService,
    private val fiatCurrenciesService: FiatCurrenciesService,
    private val cowboysPromoFeatureFlag: FeatureFlag,
    private val cowboysPrefs: CowboysPrefs,
    private val userIdentity: UserIdentity,
    private val coincore: Coincore,
    private val kycService: KycService,
    private val experimentsStore: ExperimentsStore,
    private val fraudService: FraudService
) {

    private val wallet: Wallet
        get() = payloadDataManager.wallet

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
                saveInitialCountry().then {
                    setUpWalletModeIfNeeded()
                }
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
            }.then {
                rxCompletable { invalidateExperiments() }
            }
            .then {
                checkForCowboysUser()
            }
            .then {
                WalletMode.values().map {
                    coincore.activeWalletsInModeRx(it)
                        .firstOrError()
                        .flatMap { it.balanceRx().firstOrError() }
                }.zipSingles()
                    .onErrorComplete()
                    .ignoreElement()
            }
            .doOnSubscribe {
                emitter.onNext(LoaderIntents.UpdateProgressStep(ProgressStep.SYNCING_ACCOUNT))
            }.subscribeBy(
                onComplete = {
                    invalidateExperiments()
                    onInitSettingsSuccess(isAfterWalletCreation && shouldCheckForEmailVerification())
                },
                onError = { throwable ->
                    emitter.onNext(LoaderIntents.UpdateLoadingStep(LoadingStep.Error(throwable)))
                }
            )
    }

    /**
     * Making sure default Wallet mode has been set
     */
    private fun setUpWalletModeIfNeeded(): Completable =
        Single.zip(
            walletModeServices.map {
                it.walletModeSingle
            }
        ) {}.ignoreElement().onErrorComplete()

    private fun invalidateExperiments() = experimentsStore.markAsStale()

    private fun checkForCowboysUser() = Single.zip(
        userIdentity.isCowboysUser(),
        kycService.getHighestApprovedTierLevelLegacy(),
        cowboysPromoFeatureFlag.enabled,
    ) { isCowboysUser, highestTier, isCowboysFlagEnabled ->
        if (isCowboysFlagEnabled && isCowboysUser) {
            // reset flag on login
            cowboysPrefs.hasCowboysReferralBeenDismissed = false

            if (highestTier == KycTier.BRONZE && !cowboysPrefs.hasSeenCowboysFlow) {
                cowboysPrefs.hasSeenCowboysFlow = true
                emitter.onNext(
                    LoaderIntents.UpdateCowboysPromo(isCowboysPromoUser = true)
                )
            }
        }
    }.ignoreElement()

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
        analytics.logEvent(LoginAnalyticsEvent(true))
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
            fraudService.endFlow(FraudFlow.SIGNUP)
            val stateSelected = walletPrefs.stateSelectedOnSignUp
            nabuUserDataManager.saveUserInitialLocation(
                countrySelected,
                stateSelected.takeIf { it.isNotEmpty() }
            ).doOnComplete {
                walletPrefs.clearGeolocationPreferences()
            }.onErrorComplete()
        } else Completable.complete()
    }

    class LoginAnalyticsEvent(private val isOnV1: Boolean) : AnalyticsEvent {
        override val event: String
            get() = AnalyticsNames.SIGNED_IN.eventName
        override val params: Map<String, Serializable>
            get() = mapOf(
                "is_superapp_v1" to isOnV1
            )
    }
}
