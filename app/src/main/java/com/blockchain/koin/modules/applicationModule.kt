package com.blockchain.koin.modules

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import com.blockchain.api.ConnectionApi
import com.blockchain.api.interceptors.SessionInfo
import com.blockchain.appinfo.AppInfo
import com.blockchain.auth.LogoutTimer
import com.blockchain.banking.BankPartnerCallbackProvider
import com.blockchain.biometrics.BiometricAuth
import com.blockchain.biometrics.BiometricDataRepository
import com.blockchain.biometrics.CryptographyManager
import com.blockchain.biometrics.CryptographyManagerImpl
import com.blockchain.chrome.navigation.TransactionFlowNavigation
import com.blockchain.commonarch.presentation.base.AppUtilAPI
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.componentlib.theme.AppThemeProvider
import com.blockchain.core.access.PinRepository
import com.blockchain.core.auth.metadata.WalletCredentialsMetadataUpdater
import com.blockchain.core.utils.SSLVerifyUtil
import com.blockchain.domain.onboarding.OnBoardingStepsService
import com.blockchain.enviroment.Environment
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.home.presentation.fiat.actions.FiatActionsNavigation
import com.blockchain.home.presentation.navigation.AssetActionsNavigation
import com.blockchain.keyboard.InputKeyboard
import com.blockchain.koin.applicationScope
import com.blockchain.koin.ars
import com.blockchain.koin.buyRefreshQuoteFeatureFlag
import com.blockchain.koin.cardPaymentAsyncFeatureFlag
import com.blockchain.koin.eur
import com.blockchain.koin.explorerRetrofit
import com.blockchain.koin.feynmanCheckoutFeatureFlag
import com.blockchain.koin.feynmanEnterAmountFeatureFlag
import com.blockchain.koin.gbp
import com.blockchain.koin.hideDustFeatureFlag
import com.blockchain.koin.improvedPaymentUxFeatureFlag
import com.blockchain.koin.intercomChatFeatureFlag
import com.blockchain.koin.kotlinJsonAssetTicker
import com.blockchain.koin.payloadScope
import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.koin.plaidFeatureFlag
import com.blockchain.koin.rbExperimentFeatureFlag
import com.blockchain.koin.rbFrequencyFeatureFlag
import com.blockchain.koin.sellOrder
import com.blockchain.koin.stakingAccountFeatureFlag
import com.blockchain.koin.usd
import com.blockchain.lifecycle.LifecycleInterestedComponent
import com.blockchain.lifecycle.LifecycleObservable
import com.blockchain.logging.DigitalTrust
import com.blockchain.nabu.datamanagers.custodialwalletimpl.PaymentAccountMapper
import com.blockchain.network.websocket.Options
import com.blockchain.network.websocket.autoRetry
import com.blockchain.network.websocket.debugLog
import com.blockchain.network.websocket.newBlockchainWebSocket
import com.blockchain.payments.checkoutcom.CheckoutCardProcessor
import com.blockchain.payments.checkoutcom.CheckoutFactory
import com.blockchain.payments.core.CardProcessor
import com.blockchain.payments.stripe.StripeCardProcessor
import com.blockchain.payments.stripe.StripeFactory
import com.blockchain.prices.navigation.PricesNavigation
import com.blockchain.ui.password.SecondPasswordHandler
import com.blockchain.wallet.BackupWallet
import com.blockchain.wallet.DefaultLabels
import exchange.ExchangeLinking
import info.blockchain.wallet.metadata.MetadataDerivation
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.binds
import org.koin.dsl.module
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.android.auth.AppLockTimer
import piuk.blockchain.android.cards.CardModel
import piuk.blockchain.android.cards.partners.CardActivator
import piuk.blockchain.android.cards.partners.CardProviderActivator
import piuk.blockchain.android.data.GetAccumulatedInPeriodToIsFirstTimeBuyerMapper
import piuk.blockchain.android.data.GetNextPaymentDateListToFrequencyDateMapper
import piuk.blockchain.android.data.GetRecurringBuysStore
import piuk.blockchain.android.data.Mapper
import piuk.blockchain.android.data.RecurringBuyResponseToRecurringBuyMapper
import piuk.blockchain.android.data.TradeDataRepository
import piuk.blockchain.android.data.biometrics.BiometricsController
import piuk.blockchain.android.data.biometrics.BiometricsControllerImpl
import piuk.blockchain.android.data.biometrics.BiometricsDataRepositoryImpl
import piuk.blockchain.android.data.biometrics.WalletBiometricData
import piuk.blockchain.android.data.biometrics.WalletBiometricDataFactory
import piuk.blockchain.android.deeplink.BlockchainDeepLinkParser
import piuk.blockchain.android.deeplink.DeepLinkProcessor
import piuk.blockchain.android.deeplink.EmailVerificationDeepLinkHelper
import piuk.blockchain.android.deeplink.OpenBankingDeepLinkParser
import piuk.blockchain.android.domain.repositories.AssetActivityRepository
import piuk.blockchain.android.domain.repositories.TradeDataService
import piuk.blockchain.android.domain.usecases.CancelOrderUseCase
import piuk.blockchain.android.domain.usecases.GetAvailableCryptoAssetsUseCase
import piuk.blockchain.android.domain.usecases.GetAvailablePaymentMethodsTypesUseCase
import piuk.blockchain.android.domain.usecases.GetDashboardOnboardingStepsUseCase
import piuk.blockchain.android.domain.usecases.GetEligibilityAndNextPaymentDateUseCase
import piuk.blockchain.android.domain.usecases.GetReceiveAccountsForAssetUseCase
import piuk.blockchain.android.domain.usecases.IsFirstTimeBuyerUseCase
import piuk.blockchain.android.everypay.service.EveryPayCardService
import piuk.blockchain.android.exchange.ExchangeLinkingImpl
import piuk.blockchain.android.identity.SiftDigitalTrust
import piuk.blockchain.android.kyc.KycDeepLinkHelper
import piuk.blockchain.android.scan.QRCodeEncoder
import piuk.blockchain.android.scan.QrScanResultProcessor
import piuk.blockchain.android.scan.data.QrCodeDataRepository
import piuk.blockchain.android.scan.domain.QrCodeDataService
import piuk.blockchain.android.simplebuy.ARSPaymentAccountMapper
import piuk.blockchain.android.simplebuy.BankPartnerCallbackProviderImpl
import piuk.blockchain.android.simplebuy.BuyFlowNavigator
import piuk.blockchain.android.simplebuy.CreateBuyOrderUseCase
import piuk.blockchain.android.simplebuy.EURPaymentAccountMapper
import piuk.blockchain.android.simplebuy.GBPPaymentAccountMapper
import piuk.blockchain.android.simplebuy.SimpleBuyInteractor
import piuk.blockchain.android.simplebuy.SimpleBuyModel
import piuk.blockchain.android.simplebuy.SimpleBuyPrefsSerializer
import piuk.blockchain.android.simplebuy.SimpleBuyPrefsSerializerImpl
import piuk.blockchain.android.simplebuy.SimpleBuyState
import piuk.blockchain.android.simplebuy.SimpleBuySyncFactory
import piuk.blockchain.android.simplebuy.USDPaymentAccountMapper
import piuk.blockchain.android.ui.addresses.AccountPresenter
import piuk.blockchain.android.ui.airdrops.AirdropCentrePresenter
import piuk.blockchain.android.ui.auth.FirebaseMobileNoticeRemoteConfig
import piuk.blockchain.android.ui.auth.MobileNoticeRemoteConfig
import piuk.blockchain.android.ui.backup.completed.BackupWalletCompletedPresenter
import piuk.blockchain.android.ui.backup.start.BackupWalletStartingInteractor
import piuk.blockchain.android.ui.backup.start.BackupWalletStartingModel
import piuk.blockchain.android.ui.backup.start.BackupWalletStartingState
import piuk.blockchain.android.ui.backup.verify.BackupVerifyPresenter
import piuk.blockchain.android.ui.backup.wordlist.BackupWalletWordListPresenter
import piuk.blockchain.android.ui.brokerage.BuySellFlowNavigator
import piuk.blockchain.android.ui.brokerage.sell.SellViewModel
import piuk.blockchain.android.ui.createwallet.CreateWalletViewModel
import piuk.blockchain.android.ui.customviews.SecondPasswordDialog
import piuk.blockchain.android.ui.customviews.inputview.InputAmountKeyboard
import piuk.blockchain.android.ui.dataremediation.QuestionnaireModel
import piuk.blockchain.android.ui.dataremediation.QuestionnaireStateMachine
import piuk.blockchain.android.ui.home.ActionsSheetViewModel
import piuk.blockchain.android.ui.home.AssetActionsNavigationImpl
import piuk.blockchain.android.ui.home.CredentialsWiper
import piuk.blockchain.android.ui.home.FiatActionsNavigationImpl
import piuk.blockchain.android.ui.home.TransactionFlowNavigationImpl
import piuk.blockchain.android.ui.kyc.email.entry.EmailVerificationModel
import piuk.blockchain.android.ui.kyc.settings.KycStatusHelper
import piuk.blockchain.android.ui.launcher.DeepLinkPersistence
import piuk.blockchain.android.ui.launcher.GlobalEventHandler
import piuk.blockchain.android.ui.launcher.LauncherViewModel
import piuk.blockchain.android.ui.launcher.Prerequisites
import piuk.blockchain.android.ui.linkbank.BankAuthModel
import piuk.blockchain.android.ui.linkbank.BankAuthState
import piuk.blockchain.android.ui.onboarding.OnboardingPresenter
import piuk.blockchain.android.ui.pairingcode.PairingModel
import piuk.blockchain.android.ui.pairingcode.PairingState
import piuk.blockchain.android.ui.recover.AccountRecoveryInteractor
import piuk.blockchain.android.ui.recover.AccountRecoveryModel
import piuk.blockchain.android.ui.recover.AccountRecoveryState
import piuk.blockchain.android.ui.resources.AssetResources
import piuk.blockchain.android.ui.resources.AssetResourcesImpl
import piuk.blockchain.android.ui.ssl.SSLVerifyPresenter
import piuk.blockchain.android.ui.transfer.receive.detail.ReceiveDetailIntentHelper
import piuk.blockchain.android.ui.upsell.KycUpgradePromptManager
import piuk.blockchain.android.util.AppUtil
import piuk.blockchain.android.util.BackupWalletUtil
import piuk.blockchain.android.util.FormatChecker
import piuk.blockchain.android.util.OSUtil
import piuk.blockchain.android.util.ResourceDefaultLabels
import piuk.blockchain.android.util.RootUtil
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.android.util.wiper.DataWiper
import piuk.blockchain.android.util.wiper.DataWiperImpl
import piuk.blockchain.android.walletmode.DefaultWalletModeStrategy
import piuk.blockchain.android.walletmode.WalletModeThemeProvider

val applicationModule = module {

    single(applicationScope) { CoroutineScope(SupervisorJob()) }

    factory { OSUtil(get()) }

    factory { StringUtils(get()) }

    single {
        AppUtil(
            context = get(),
            payloadScopeWiper = get(),
            sessionPrefs = get(),
            trust = get(),
            pinRepository = get(),
            remoteLogger = get(),
            walletStatusPrefs = get(),
            unifiedActivityService = payloadScope.get()
        )
    }.bind(AppUtilAPI::class)

    single {
        AppLockTimer(
            application = get()
        )
    }.bind(LogoutTimer::class)

    single {
        val ctx: Context = get()
        object : AppInfo {
            override val cacheDir: File =
                ctx.applicationContext.cacheDir
        }
    }.bind(AppInfo::class)

    factory { RootUtil() }

    factory { get<Context>().resources }

    single {
        WalletModeThemeProvider(
            walletModeService = get()
        )
    }.bind(AppThemeProvider::class)

    single { LifecycleInterestedComponent() }
        .bind(LifecycleObservable::class)

    single {
        SiftDigitalTrust(
            accountId = BuildConfig.SIFT_ACCOUNT_ID,
            beaconKey = BuildConfig.SIFT_BEACON_KEY
        )
    }.bind(DigitalTrust::class)

    single {
        InputAmountKeyboard()
    }.bind(InputKeyboard::class)

    scope(payloadScopeQualifier) {

        factory {
            SecondPasswordDialog(context = androidContext(), payloadManager = get())
        }.bind(SecondPasswordHandler::class)

        factory {
            KycStatusHelper(
                kycService = get()
            )
        }

        factory {
            BankPartnerCallbackProviderImpl()
        }.bind(BankPartnerCallbackProvider::class)

        scoped { (activity: BlockchainActivity) -> AssetActionsNavigationImpl(activity = activity) }.apply {
            bind(PricesNavigation::class)
            bind(AssetActionsNavigation::class)
        }

        scoped { (activity: BlockchainActivity) ->
            FiatActionsNavigationImpl(activity = activity)
        }.bind(FiatActionsNavigation::class)

        scoped { (activity: AppCompatActivity) ->
            TransactionFlowNavigationImpl(activity = activity)
        }.bind(TransactionFlowNavigation::class)

        scoped {
            CredentialsWiper(
                appUtil = get(),
                ethDataManager = get(),
                bchDataManager = get(),
                walletModeService = get(),
                metadataService = get(),
                walletOptionsState = get(),
                nabuDataManager = get(),
                notificationTokenManager = get(),
                storeWiper = get(),
                intercomEnabledFF = get(intercomChatFeatureFlag)
            )
        }

        factory(gbp) {
            GBPPaymentAccountMapper(resources = get())
        }.bind(PaymentAccountMapper::class)

        factory(eur) {
            EURPaymentAccountMapper(resources = get())
        }.bind(PaymentAccountMapper::class)

        factory(usd) {
            USDPaymentAccountMapper(resources = get())
        }.bind(PaymentAccountMapper::class)

        factory(ars) {
            ARSPaymentAccountMapper(resources = get())
        }.bind(PaymentAccountMapper::class)

        factory {
            OkHttpClient()
                .newBlockchainWebSocket(options = Options(url = BuildConfig.COINS_WEBSOCKET_URL))
                .autoRetry()
                .debugLog("COIN_SOCKET")
        }

        factory {
            PairingModel(
                initialState = PairingState(),
                mainScheduler = AndroidSchedulers.mainThread(),
                environmentConfig = get(),
                remoteLogger = get(),
                qrCodeDataService = get(),
                payloadDataManager = get(),
                authDataManager = get(),
                analytics = get()
            )
        }

        viewModel {
            CreateWalletViewModel(
                environmentConfig = get(),
                defaultLabels = get(),
                authPrefs = get(),
                walletStatusPrefs = get(),
                analytics = get(),
                specificAnalytics = get(),
                appUtil = get(),
                formatChecker = get(),
                eligibilityService = get(),
                referralService = get(),
                payloadDataManager = get(),
                nabuUserDataManager = get(),
            )
        }

        viewModel {
            ActionsSheetViewModel(userIdentity = get())
        }

        viewModel {
            QuestionnaireModel(
                dataRemediationService = get(),
                stateMachine = QuestionnaireStateMachine(),
                analytics = get()
            )
        }

        factory {
            BackupWalletStartingInteractor(
                authPrefs = get(),
                settingsDataManager = get()
            )
        }

        factory {
            BackupWalletStartingModel(
                initialState = BackupWalletStartingState(),
                mainScheduler = AndroidSchedulers.mainThread(),
                environmentConfig = get(),
                remoteLogger = get(),
                interactor = get()
            )
        }

        factory {
            BackupWalletWordListPresenter(
                backupWallet = get()
            )
        }

        factory<BackupWallet> {
            BackupWalletUtil(
                payloadDataManager = get()
            )
        }

        factory {
            BackupVerifyPresenter(
                payloadDataManager = get(),
                backupWallet = get(),
                walletStatusPrefs = get()
            )
        }

        factory {
            AccountRecoveryModel(
                initialState = AccountRecoveryState(),
                mainScheduler = AndroidSchedulers.mainThread(),
                environmentConfig = get(),
                remoteLogger = get(),
                interactor = get()
            )
        }

        factory {
            AccountRecoveryInteractor(
                payloadDataManager = get(),
                authPrefs = get(),
                metadataInteractor = get(),
                metadataDerivation = MetadataDerivation(),
                nabuDataManager = get()
            )
        }

        factory {
            KycDeepLinkHelper(
                linkHandler = get()
            )
        }

        factory {
            BlockchainDeepLinkParser()
        }

        factory { EmailVerificationDeepLinkHelper() }

        factory {
            DeepLinkProcessor(
                linkHandler = get(),
                kycDeepLinkHelper = get(),
                emailVerifiedLinkHelper = get(),
                openBankingDeepLinkParser = get(),
                blockchainDeepLinkParser = get()
            )
        }

        factory {
            OpenBankingDeepLinkParser()
        }

        scoped {
            QrScanResultProcessor(
                bitPayDataManager = get(),
                walletConnectUrlValidator = get(),
                analytics = get()
            )
        }

        factory {
            AccountPresenter(
                privateKeyFactory = get(),
                analytics = get(),
                coincore = get()
            )
        }

        factory {
            SimpleBuyInteractor(
                withdrawLocksRepository = get(),
                kycService = get(),
                tradeDataService = get(),
                custodialWalletManager = get(),
                limitsDataManager = get(),
                coincore = get(),
                userIdentity = get(),
                simpleBuyService = get(),
                bankLinkingPrefs = get(),
                analytics = get(),
                exchangeRatesDataManager = get(),
                bankPartnerCallbackProvider = get(),
                cardProcessors = getCardProcessors().associateBy { it.acquirer },
                cancelOrderUseCase = get(),
                getAvailablePaymentMethodsTypesUseCase = get(),
                bankService = get(),
                cardService = get(),
                paymentMethodService = get(),
                paymentsRepository = get(),
                brokerageDataManager = get(),
                simpleBuyPrefs = get(),
                onboardingPrefs = get(),
                eligibilityService = get(),
                cardPaymentAsyncFF = get(cardPaymentAsyncFeatureFlag),
                buyQuoteRefreshFF = get(buyRefreshQuoteFeatureFlag),
                plaidFF = get(plaidFeatureFlag),
                rbFrequencySuggestionFF = get(rbFrequencyFeatureFlag),
                rbExperimentFF = get(rbExperimentFeatureFlag),
                feynmanEnterAmountFF = get(feynmanEnterAmountFeatureFlag),
                feynmanCheckoutFF = get(feynmanCheckoutFeatureFlag),
                improvedPaymentUxFF = get(improvedPaymentUxFeatureFlag),
                remoteConfigRepository = get(),
                quickFillRoundingService = get()
            )
        }

        factory {
            SimpleBuyModel(
                interactor = get(),
                uiScheduler = AndroidSchedulers.mainThread(),
                initialState = SimpleBuyState(),
                fiatCurrenciesService = get(),
                buyOrdersStore = get(),
                serializer = get(),
                cardActivator = get(),
                _activityIndicator = lazy { get<AppUtil>().activityIndicator },
                environmentConfig = get(),
                remoteLogger = get(),
                createBuyOrderUseCase = get(),
                getEligibilityAndNextPaymentDateUseCase = get(),
                bankPartnerCallbackProvider = get(),
                userIdentity = get(),
                getSafeConnectTosLinkUseCase = payloadScope.get(),
                appRatingService = get(),
                cardPaymentAsyncFF = get(cardPaymentAsyncFeatureFlag),
            )
        }

        factory {
            IsFirstTimeBuyerUseCase(
                tradeDataService = get()
            )
        }

        factory {
            GetEligibilityAndNextPaymentDateUseCase(
                tradeDataService = get()
            )
        }

        factory {
            GetAvailableCryptoAssetsUseCase(
                coincore = get()
            )
        }

        factory {
            GetReceiveAccountsForAssetUseCase(
                coincore = get()
            )
        }

        factory {
            GetAvailablePaymentMethodsTypesUseCase(
                kycService = get(),
                userIdentity = get(),
                paymentMethodService = get(),
                cardService = get()
            )
        }

        factory {
            CancelOrderUseCase(
                bankLinkingPrefs = get(),
                custodialWalletManager = get()
            )
        }

        factory {
            GetDashboardOnboardingStepsUseCase(
                dashboardPrefs = get(),
                userIdentity = get(),
                kycService = get(),
                bankService = get(),
                cardService = get(),
                tradeDataService = get()
            )
        }.bind(OnBoardingStepsService::class)

        factory<TradeDataService> {
            TradeDataRepository(
                tradeService = get(),
                accumulatedInPeriodMapper = GetAccumulatedInPeriodToIsFirstTimeBuyerMapper(),
                nextPaymentRecurringBuyMapper = GetNextPaymentDateListToFrequencyDateMapper(),
                recurringBuyMapper = get(),
                getRecurringBuysStore = get(),
                assetCatalogue = get()
            )
        }

        scoped {
            GetRecurringBuysStore(
                tradeService = get()
            )
        }

        factory {
            RecurringBuyResponseToRecurringBuyMapper(
                assetCatalogue = get()
            )
        }.bind(Mapper::class)

        scoped {
            CreateBuyOrderUseCase(
                cancelOrderUseCase = get(),
                brokerageDataManager = get(),
                custodialWalletManager = get(),
                _activityIndicator = lazy { get<AppUtil>().activityIndicator },
                buyQuoteRefreshFF = get(buyRefreshQuoteFeatureFlag)
            )
        }

        factory {
            SimpleBuyPrefsSerializerImpl(
                prefs = get(),
                json = get(kotlinJsonAssetTicker),
            )
        }.bind(SimpleBuyPrefsSerializer::class)

        factory {
            BankAuthModel(
                interactor = get(),
                bankService = get(),
                uiScheduler = AndroidSchedulers.mainThread(),
                initialState = BankAuthState(),
                environmentConfig = get(),
                remoteLogger = get()
            )
        }

        factory {
            CardModel(
                interactor = get(),
                currencyPrefs = get(),
                uiScheduler = AndroidSchedulers.mainThread(),
                cardActivator = get(),
                json = get(),
                prefs = get(),
                environmentConfig = get(),
                remoteLogger = get()
            )
        }

        factory {
            BuyFlowNavigator(
                simpleBuySyncFactory = get(),
                userIdentity = get(),
                kycService = get(),
                fiatCurrenciesService = get(),
                custodialWalletManager = get()
            )
        }

        factory {
            BuySellFlowNavigator(
                custodialWalletManager = get(),
                userIdentity = get(),
                simpleBuySyncFactory = get()
            )
        }

        scoped {
            SimpleBuySyncFactory(
                custodialWallet = get(),
                bankService = get(),
                cardService = get(),
                serializer = get()
            )
        }

        factory {
            KycUpgradePromptManager(
                identity = get()
            )
        }

        scoped {
            ExchangeLinkingImpl(
                userService = get()
            )
        }.bind(ExchangeLinking::class)

        factory {
            BackupWalletCompletedPresenter(
                walletStatusPrefs = get(),
                authDataManager = get()
            )
        }

        factory {
            OnboardingPresenter(
                biometricsController = get(),
                pinRepository = get(),
                settingsDataManager = get()
            )
        }

        factory {
            Prerequisites(
                metadataService = get(),
                settingsDataManager = get(),
                coincore = get(),
                exchangeRates = get(),
                remoteLogger = get(),
                globalEventHandler = get(),
                walletConnectServiceAPI = get(),
                walletCredentialsUpdater = get(),
                payloadDataManager = get(),
                xlmDataManager = get(),
                ethDataManager = get(),
            )
        }

        scoped {
            GlobalEventHandler(
                application = get(),
                walletConnectServiceAPI = get(),
                deeplinkRedirector = get(),
                destinationArgs = get(),
                notificationManager = get(),
                analytics = get(),
                stakingFF = get(stakingAccountFeatureFlag)
            )
        }

        factory {
            WalletCredentialsMetadataUpdater(
                payloadDataManager = get(),
                metadataRepository = get()
            )
        }

        factory {
            AirdropCentrePresenter(
                nabuToken = get(),
                nabu = get(),
                assetCatalogue = get(),
                remoteLogger = get()
            )
        }

        factory<BiometricDataRepository> {
            BiometricsDataRepositoryImpl(authPrefs = get())
        }

        factory {
            WalletBiometricData(get<PinRepository>().pin)
        }.bind(WalletBiometricData::class)

        scoped {
            WalletBiometricDataFactory()
        }.bind(WalletBiometricDataFactory::class)

        factory {
            BiometricManager.from(get())
        }.bind(BiometricManager::class)

        factory {
            BiometricsControllerImpl(
                applicationContext = get(),
                biometricData = get(),
                biometricDataFactory = get(),
                biometricDataRepository = get(),
                biometricManager = get(),
                cryptographyManager = get(),
                remoteLogger = get()
            )
        }.binds(arrayOf(BiometricAuth::class, BiometricsController::class))

        factory {
            CryptographyManagerImpl()
        }.bind(CryptographyManager::class)

        viewModel {
            EmailVerificationModel(
                emailUpdater = get(),
                getUserStore = get(),
            )
        }

        scoped {
            AssetActivityRepository()
        }

        factory {
            EveryPayCardService(
                everyPayService = get()
            )
        }

        factory {
            CardProviderActivator(
                cardService = get(),
                submitEveryPayCardService = get()
            )
        }.bind(CardActivator::class)

        factory {
            DataWiperImpl(
                ethDataManager = get(),
                bchDataManager = get(),
                walletOptionsState = get(),
                nabuDataManager = get(),
                walletConnectServiceAPI = get(),
                assetActivityRepository = get(),
                walletPrefs = get(),
                payloadScopeWiper = get(),
                sessionInfo = SessionInfo,
                remoteLogger = get()
            )
        }.bind(DataWiper::class)

        viewModel {
            SellViewModel(
                sellService = get(),
                coincore = get(),
                accountsSorting = get(sellOrder),
                localSettingsPrefs = get(),
                hideDustFlag = get(hideDustFeatureFlag)
            )
        }
    }

    factory {
        FirebaseMobileNoticeRemoteConfig(
            remoteConfig = get(),
            json = get()
        )
    }.bind(MobileNoticeRemoteConfig::class)

    factory<QrCodeDataService> {
        QrCodeDataRepository
    }

    single {
        ConnectionApi(retrofit = get(explorerRetrofit))
    }

    single {
        SSLVerifyUtil(connectionApi = get())
    }

    factory {
        SSLVerifyPresenter(
            sslVerifyUtil = get()
        )
    }

    factory {
        ResourceDefaultLabels(
            resources = get(),
            assetResources = get()
        )
    }.bind(DefaultLabels::class)

    single {
        AssetResourcesImpl(
            resources = get()
        )
    }.bind(AssetResources::class)

    factory {
        ReceiveDetailIntentHelper(
            context = get(),
            specificAnalytics = get()
        )
    }

    factory {
        QRCodeEncoder()
    }

    factory { FormatChecker() }

    viewModel {
        LauncherViewModel(
            appUtil = get(),
            deepLinkPersistence = get(),
            envSettings = get(),
            authPrefs = get(),
            getAppMaintenanceConfigUseCase = get(),
            sessionPrefs = get(),
            securityPrefs = get(),
            referralPrefs = get(),
            encryptedPrefs = get()
        )
    }

    factory {
        DeepLinkPersistence(
            sessionPrefs = get()
        )
    }

    single {
        StripeFactory(
            context = get(),
            stripeAccountId = null,
            enableLogging = true
        )
    }

    factory {
        StripeCardProcessor(
            stripeFactory = get()
        )
    }.bind(CardProcessor::class)

    single {
        val env: EnvironmentConfig = get()
        CheckoutFactory(
            context = get(),
            isProd = env.environment == Environment.PRODUCTION
        )
    }

    factory {
        CheckoutCardProcessor(
            checkoutFactory = get()
        )
    }.bind(CardProcessor::class)

    single {
        DefaultWalletModeStrategy(
            walletModePrefs = get()
        )
    }
}

fun getCardProcessors(): List<CardProcessor> {
    return payloadScope.getAll()
}
