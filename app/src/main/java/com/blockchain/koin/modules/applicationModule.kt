package com.blockchain.koin.modules

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavHostController
import com.blockchain.api.ConnectionApi
import com.blockchain.api.interceptors.SessionInfo
import com.blockchain.appinfo.AppInfo
import com.blockchain.auth.LogoutTimer
import com.blockchain.biometrics.BiometricAuth
import com.blockchain.biometrics.BiometricDataRepository
import com.blockchain.biometrics.CryptographyManager
import com.blockchain.biometrics.CryptographyManagerImpl
import com.blockchain.chrome.ChromePill
import com.blockchain.chrome.navigation.AssetActionsNavigation
import com.blockchain.chrome.navigation.DefiBackupNavigation
import com.blockchain.chrome.navigation.RecurringBuyNavigation
import com.blockchain.chrome.navigation.SettingsNavigation
import com.blockchain.chrome.navigation.SupportNavigation
import com.blockchain.chrome.navigation.TransactionFlowNavigation
import com.blockchain.chrome.navigation.WalletLinkAndOpenBankingNavigation
import com.blockchain.commonarch.presentation.base.AppUtilAPI
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.componentlib.theme.AppThemeProvider
import com.blockchain.core.access.PinRepository
import com.blockchain.core.auth.metadata.WalletCredentialsMetadataUpdater
import com.blockchain.core.recurringbuy.data.datasources.RecurringBuyFrequencyConfigStore
import com.blockchain.core.recurringbuy.data.datasources.RecurringBuyStore
import com.blockchain.core.trade.TradeDataRepository
import com.blockchain.core.utils.SSLVerifyUtil
import com.blockchain.deeplinking.processor.DeeplinkService
import com.blockchain.domain.buy.CancelOrderService
import com.blockchain.domain.paymentmethods.model.BankBuyNavigation
import com.blockchain.domain.paymentmethods.model.BankPartnerCallbackProvider
import com.blockchain.domain.trade.TradeDataService
import com.blockchain.earn.navigation.EarnNavigation
import com.blockchain.enviroment.Environment
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.fiatActions.fiatactions.FiatActionsNavigation
import com.blockchain.home.presentation.navigation.AuthNavigation
import com.blockchain.home.presentation.navigation.QrScanNavigation
import com.blockchain.internalnotifications.NotificationReceiver
import com.blockchain.internalnotifications.NotificationTransmitter
import com.blockchain.keyboard.InputKeyboard
import com.blockchain.koin.applicationScope
import com.blockchain.koin.buyRefreshQuoteFeatureFlag
import com.blockchain.koin.cardPaymentAsyncFeatureFlag
import com.blockchain.koin.explorerRetrofit
import com.blockchain.koin.feynmanCheckoutFeatureFlag
import com.blockchain.koin.feynmanEnterAmountFeatureFlag
import com.blockchain.koin.improvedPaymentUxFeatureFlag
import com.blockchain.koin.intercomChatFeatureFlag
import com.blockchain.koin.kotlinJsonAssetTicker
import com.blockchain.koin.payloadScope
import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.koin.plaidFeatureFlag
import com.blockchain.koin.rbExperimentFeatureFlag
import com.blockchain.koin.sellOrder
import com.blockchain.koin.vgsFeatureFlag
import com.blockchain.koin.walletConnectV1FeatureFlag
import com.blockchain.koin.walletConnectV2FeatureFlag
import com.blockchain.lifecycle.LifecycleInterestedComponent
import com.blockchain.lifecycle.LifecycleObservable
import com.blockchain.logging.DigitalTrust
import com.blockchain.network.websocket.Options
import com.blockchain.network.websocket.autoRetry
import com.blockchain.network.websocket.debugLog
import com.blockchain.network.websocket.newBlockchainWebSocket
import com.blockchain.payments.checkoutcom.CheckoutCardProcessor
import com.blockchain.payments.checkoutcom.CheckoutFactory
import com.blockchain.payments.core.CardProcessor
import com.blockchain.payments.stripe.StripeCardProcessor
import com.blockchain.payments.stripe.StripeFactory
import com.blockchain.transactions.upsell.buy.viewmodel.UpsellBuyViewModel
import com.blockchain.ui.password.SecondPasswordHandler
import com.blockchain.wallet.BackupWallet
import com.blockchain.wallet.DefaultLabels
import com.blockchain.walletconnect.ui.navigation.WalletConnectV2Navigation
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
import piuk.blockchain.android.cards.cvv.SecurityCodeViewModel
import piuk.blockchain.android.cards.partners.CardActivator
import piuk.blockchain.android.cards.partners.CardProviderActivator
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
import piuk.blockchain.android.domain.usecases.CancelOrderUseCase
import piuk.blockchain.android.domain.usecases.GetAvailableCryptoAssetsUseCase
import piuk.blockchain.android.domain.usecases.GetAvailablePaymentMethodsTypesUseCase
import piuk.blockchain.android.domain.usecases.GetReceiveAccountsForAssetUseCase
import piuk.blockchain.android.domain.usecases.IsFirstTimeBuyerUseCase
import piuk.blockchain.android.everypay.service.EveryPayCardService
import piuk.blockchain.android.exchange.ExchangeLinkingImpl
import piuk.blockchain.android.identity.SiftDigitalTrust
import piuk.blockchain.android.internalnotifications.NotificationsCenter
import piuk.blockchain.android.kyc.KycDeepLinkHelper
import piuk.blockchain.android.scan.QrScanResultProcessor
import piuk.blockchain.android.scan.data.QrCodeDataRepository
import piuk.blockchain.android.scan.domain.QrCodeDataService
import piuk.blockchain.android.simplebuy.BankBuyNavigationImpl
import piuk.blockchain.android.simplebuy.BankPartnerCallbackProviderImpl
import piuk.blockchain.android.simplebuy.BuyFlowNavigator
import piuk.blockchain.android.simplebuy.CreateBuyOrderUseCase
import piuk.blockchain.android.simplebuy.SimpleBuyInteractor
import piuk.blockchain.android.simplebuy.SimpleBuyModel
import piuk.blockchain.android.simplebuy.SimpleBuyPrefsSerializer
import piuk.blockchain.android.simplebuy.SimpleBuyPrefsSerializerImpl
import piuk.blockchain.android.simplebuy.SimpleBuyState
import piuk.blockchain.android.simplebuy.SimpleBuySyncFactory
import piuk.blockchain.android.ui.addresses.AccountPresenter
import piuk.blockchain.android.ui.airdrops.AirdropCentrePresenter
import piuk.blockchain.android.ui.auth.AuthNavigationImpl
import piuk.blockchain.android.ui.auth.FirebaseMobileNoticeRemoteConfig
import piuk.blockchain.android.ui.auth.MobileNoticeRemoteConfig
import piuk.blockchain.android.ui.brokerage.BuySellFlowNavigator
import piuk.blockchain.android.ui.brokerage.sell.SellRepository
import piuk.blockchain.android.ui.brokerage.sell.SellViewModel
import piuk.blockchain.android.ui.createwallet.CreateWalletViewModel
import piuk.blockchain.android.ui.customviews.SecondPasswordDialog
import piuk.blockchain.android.ui.customviews.inputview.InputAmountKeyboard
import piuk.blockchain.android.ui.dataremediation.QuestionnaireModel
import piuk.blockchain.android.ui.dataremediation.QuestionnaireStateMachine
import piuk.blockchain.android.ui.home.AssetActionsNavigationImpl
import piuk.blockchain.android.ui.home.CredentialsWiper
import piuk.blockchain.android.ui.home.DefiBackupNavigationImpl
import piuk.blockchain.android.ui.home.FiatActionsNavigationImpl
import piuk.blockchain.android.ui.home.HomeActivityLauncher
import piuk.blockchain.android.ui.home.QrScanNavigationImpl
import piuk.blockchain.android.ui.home.RecurringBuyNavigationImpl
import piuk.blockchain.android.ui.home.SettingsNavigationImpl
import piuk.blockchain.android.ui.home.SupportNavigationImpl
import piuk.blockchain.android.ui.home.TransactionFlowNavigationImpl
import piuk.blockchain.android.ui.home.WalletConnectV2NavigationImpl
import piuk.blockchain.android.ui.home.WalletLinkAndOpenBankingNavImpl
import piuk.blockchain.android.ui.interest.EarnNavigationImpl
import piuk.blockchain.android.ui.launcher.DeepLinkPersistence
import piuk.blockchain.android.ui.launcher.GlobalEventHandler
import piuk.blockchain.android.ui.launcher.LauncherViewModel
import piuk.blockchain.android.ui.launcher.Prerequisites
import piuk.blockchain.android.ui.linkbank.BankAuthModel
import piuk.blockchain.android.ui.linkbank.BankAuthState
import piuk.blockchain.android.ui.recover.AccountRecoveryInteractor
import piuk.blockchain.android.ui.recover.AccountRecoveryModel
import piuk.blockchain.android.ui.recover.AccountRecoveryState
import piuk.blockchain.android.ui.resources.AssetResources
import piuk.blockchain.android.ui.resources.AssetResourcesImpl
import piuk.blockchain.android.ui.ssl.SSLVerifyPresenter
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
            walletStatusPrefs = get()
        )
    }.bind(AppUtilAPI::class)

    single {
        AppLockTimer(
            application = get()
        )
    }.bind(LogoutTimer::class)

    single {
        NotificationsCenter(
            scope = get(applicationScope)
        )
    }.apply {
        bind(NotificationReceiver::class)
        bind(NotificationTransmitter::class)
    }

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
        WalletModeThemeProvider()
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
            BankPartnerCallbackProviderImpl()
        }.bind(BankPartnerCallbackProvider::class)

        factory { (activity: BlockchainActivity) -> DefiBackupNavigationImpl(activity = activity) }.apply {
            bind(DefiBackupNavigation::class)
        }

        factory { (activity: BlockchainActivity) -> AssetActionsNavigationImpl(activity = activity) }.apply {
            bind(AssetActionsNavigation::class)
        }

        factory { (activity: BlockchainActivity) -> RecurringBuyNavigationImpl(activity = activity) }.apply {
            bind(RecurringBuyNavigation::class)
        }

        factory { (activity: BlockchainActivity) -> SettingsNavigationImpl(activity = activity) }.apply {
            bind(SettingsNavigation::class)
        }

        factory { (activity: BlockchainActivity) -> WalletLinkAndOpenBankingNavImpl(activity = activity) }.apply {
            bind(WalletLinkAndOpenBankingNavigation::class)
        }
        factory { (activity: BlockchainActivity) ->
            FiatActionsNavigationImpl(activity = activity)
        }.bind(FiatActionsNavigation::class)

        factory { (activity: AppCompatActivity) ->
            TransactionFlowNavigationImpl(activity = activity)
        }.bind(TransactionFlowNavigation::class)

        factory { (activity: BlockchainActivity) ->
            AuthNavigationImpl(activity = activity, credentialsWiper = get())
        }.bind(AuthNavigation::class)

        factory { (activity: BlockchainActivity) ->
            QrScanNavigationImpl(
                activity = activity,
                qrScanResultProcessor = payloadScope.get(),
                walletConnectServiceAPI = get(),
                secureChannelService = get(),
                assetService = get(),
                assetCatalogue = get(),
                walletConnectV2Service = get(),
                walletConnectV1FeatureFlag = get(walletConnectV1FeatureFlag),
                walletConnectV2FeatureFlag = get(walletConnectV2FeatureFlag),
            )
        }.apply {
            bind(QrScanNavigation::class)
        }

        factory { (activity: BlockchainActivity) -> SupportNavigationImpl(activity = activity) }.apply {
            bind(SupportNavigation::class)
        }

        scoped { (activity: BlockchainActivity, assetActionsNavigation: AssetActionsNavigation) ->
            EarnNavigationImpl(
                activity = activity,
                assetActionsNavigation = assetActionsNavigation
            )
        }.apply {
            bind(EarnNavigation::class)
        }

        scoped { (lifecycle: Lifecycle, navController: NavHostController) ->
            WalletConnectV2NavigationImpl(
                lifecycle = lifecycle,
                navController = navController,
                walletConnectV2Service = get(),
                walletConnectV2FeatureFlag = get(walletConnectV2FeatureFlag),
            )
        }.apply {
            bind(WalletConnectV2Navigation::class)
        }

        scoped {
            CredentialsWiper(
                appUtil = get(),
                ethDataManager = get(),
                bchDataManager = get(),
                walletModeService = get(),
                metadataService = get(),
                notificationTransmitter = get(),
                nabuDataManager = get(),
                notificationTokenManager = get(),
                activityWebSocketService = get(),
                unifiedActivityService = get(),
                storeWiper = get(),
                intercomEnabledFF = get(intercomChatFeatureFlag),
                walletConnectV2Service = get()
            )
        }

        factory {
            OkHttpClient()
                .newBlockchainWebSocket(options = Options(url = BuildConfig.COINS_WEBSOCKET_URL))
                .autoRetry()
                .debugLog("COIN_SOCKET")
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
                nabuUserDataManager = get()
            )
        }

        viewModel {
            QuestionnaireModel(
                dataRemediationService = get(),
                stateMachine = QuestionnaireStateMachine(),
                analytics = get()
            )
        }

        factory<BackupWallet> {
            BackupWalletUtil(
                payloadDataManager = get()
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
        }.bind(DeeplinkService::class)

        factory {
            OpenBankingDeepLinkParser()
        }

        scoped {
            QrScanResultProcessor(
                bitPayDataManager = get(),
                walletConnectUrlValidator = get(),
                walletConnectV2UrlValidator = get(),
                analytics = get(),
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
                simpleBuyService = get(),
                bankLinkingPrefs = get(),
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
                rbExperimentFF = get(rbExperimentFeatureFlag),
                feynmanEnterAmountFF = get(feynmanEnterAmountFeatureFlag),
                feynmanCheckoutFF = get(feynmanCheckoutFeatureFlag),
                improvedPaymentUxFF = get(improvedPaymentUxFeatureFlag),
                remoteConfigRepository = get(),
                recurringBuyService = get(),
                dismissRecorder = get()
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
                recurringBuyService = get(),
                bankPartnerCallbackProvider = get(),
                userIdentity = get(),
                getSafeConnectTosLinkUseCase = payloadScope.get(),
                appRatingService = get(),
                cardPaymentAsyncFF = get(cardPaymentAsyncFeatureFlag),
                recurringBuyPrefs = get()
            )
        }

        factory {
            IsFirstTimeBuyerUseCase(
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
        }.bind(CancelOrderService::class)

        factory<TradeDataService> {
            TradeDataRepository(
                tradeService = get(),
                assetCatalogue = get(),
                remoteConfigService = get()
            )
        }

        scoped {
            RecurringBuyStore(
                recurringBuyApiService = get()
            )
        }

        scoped {
            RecurringBuyFrequencyConfigStore(
                recurringBuyApiService = get()
            )
        }

        scoped {
            CreateBuyOrderUseCase(
                cancelOrderUseCase = get(),
                brokerageDataManager = get(),
                custodialWalletManager = get(),
                _activityIndicator = lazy { get<AppUtil>().activityIndicator },
                buyQuoteRefreshFF = get(buyRefreshQuoteFeatureFlag)
            )
        }

        scoped {
            DefaultWalletModeStrategy(
                walletModePrefs = get(),
                eligibilityService = get()
            )
        }

        factory {
            SimpleBuyPrefsSerializerImpl(
                prefs = get(),
                json = get(kotlinJsonAssetTicker)
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
                vgsFeatureFlag = get(vgsFeatureFlag),
                vgsCardTokenizerService = get(),
                paymentsService = get(),
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
                simpleBuySyncFactory = get(),
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
        scoped {
            BankBuyNavigationImpl(
                interactor = get()
            )
        }.bind(BankBuyNavigation::class)

        scoped {
            SellRepository(
                userFeaturePermissionService = get(),
                kycService = get(),
                accountsSorting = get(sellOrder),
                simpleBuyService = get(),
                coincore = get(),
                custodialWalletManager = get(),
                currencyPrefs = get()
            )
        }

        scoped {
            ExchangeLinkingImpl(
                userService = get()
            )
        }.bind(ExchangeLinking::class)

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
                ethDataManager = get()
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
                homeActivityLauncher = get(),
                walletConnectV2Service = get(),
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
                nabuDataManager = get(),
                activityWebSocketService = get(),
                walletConnectServiceAPI = get(),
                assetActivityRepository = get(),
                walletPrefs = get(),
                payloadScopeWiper = get(),
                sessionInfo = SessionInfo,
                remoteLogger = get(),
                globalEventHandler = get(),
            )
        }.bind(DataWiper::class)

        viewModel {
            SellViewModel(
                sellRepository = get(),
                walletModeService = get()
            )
        }

        viewModel { (assetJustTransactedTicker: String) ->
            UpsellBuyViewModel(
                assetJustTransactedTicker = assetJustTransactedTicker,
                pricesService = get(),
                simpleBuyService = get()
            )
        }

        scoped {
            ChromePill
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
            resources = get()
        )
    }.bind(DefaultLabels::class)

    single {
        AssetResourcesImpl(
            resources = get()
        )
    }.bind(AssetResources::class)

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

    viewModel {
        SecurityCodeViewModel(
            paymentMethodsService = get()
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

    single {
        HomeActivityLauncher()
    }

    factory {
        CheckoutCardProcessor(
            checkoutFactory = get()
        )
    }.bind(CardProcessor::class)
}

fun getCardProcessors(): List<CardProcessor> {
    return payloadScope.getAll()
}
