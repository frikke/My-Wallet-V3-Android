package com.blockchain.koin.modules

import android.content.Context
import androidx.biometric.BiometricManager
import com.blockchain.appinfo.AppInfo
import com.blockchain.banking.BankPartnerCallbackProvider
import com.blockchain.biometrics.BiometricAuth
import com.blockchain.biometrics.BiometricDataRepository
import com.blockchain.biometrics.CryptographyManager
import com.blockchain.biometrics.CryptographyManagerImpl
import com.blockchain.core.Database
import com.blockchain.koin.eur
import com.blockchain.koin.explorerRetrofit
import com.blockchain.koin.gbp
import com.blockchain.koin.payloadScope
import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.koin.stripeAndCheckoutPaymentsFeatureFlag
import com.blockchain.koin.usd
import com.blockchain.koin.walletRedesignFeatureFlag
import com.blockchain.lifecycle.LifecycleInterestedComponent
import com.blockchain.lifecycle.LifecycleObservable
import com.blockchain.logging.DigitalTrust
import com.blockchain.nabu.datamanagers.custodialwalletimpl.PaymentAccountMapper
import com.blockchain.network.websocket.Options
import com.blockchain.network.websocket.autoRetry
import com.blockchain.network.websocket.debugLog
import com.blockchain.network.websocket.newBlockchainWebSocket
import com.blockchain.operations.AppStartUpFlushable
import com.blockchain.payments.checkoutcom.CheckoutCardProcessor
import com.blockchain.payments.checkoutcom.CheckoutFactory
import com.blockchain.payments.core.CardProcessor
import com.blockchain.payments.stripe.StripeCardProcessor
import com.blockchain.payments.stripe.StripeFactory
import com.blockchain.ui.password.SecondPasswordHandler
import com.blockchain.wallet.DefaultLabels
import com.blockchain.websocket.CoinsWebSocketInterface
import com.google.gson.GsonBuilder
import com.squareup.sqldelight.android.AndroidSqliteDriver
import com.squareup.sqldelight.db.SqlDriver
import info.blockchain.wallet.api.Environment
import info.blockchain.wallet.metadata.MetadataDerivation
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import java.io.File
import okhttp3.OkHttpClient
import org.koin.dsl.bind
import org.koin.dsl.binds
import org.koin.dsl.module
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.android.cards.CardModel
import piuk.blockchain.android.cards.partners.CardActivator
import piuk.blockchain.android.cards.partners.CardProviderActivator
import piuk.blockchain.android.data.GetAccumulatedInPeriodToIsFirstTimeBuyerMapper
import piuk.blockchain.android.data.GetNextPaymentDateListToFrequencyDateMapper
import piuk.blockchain.android.data.Mapper
import piuk.blockchain.android.data.TradeDataManagerImpl
import piuk.blockchain.android.data.biometrics.BiometricsController
import piuk.blockchain.android.data.biometrics.BiometricsControllerImpl
import piuk.blockchain.android.data.biometrics.BiometricsDataRepositoryImpl
import piuk.blockchain.android.data.biometrics.WalletBiometricData
import piuk.blockchain.android.data.biometrics.WalletBiometricDataFactory
import piuk.blockchain.android.data.coinswebsocket.service.CoinsWebSocketService
import piuk.blockchain.android.data.coinswebsocket.strategy.CoinsWebSocketStrategy
import piuk.blockchain.android.deeplink.BlockchainDeepLinkParser
import piuk.blockchain.android.deeplink.DeepLinkProcessor
import piuk.blockchain.android.deeplink.EmailVerificationDeepLinkHelper
import piuk.blockchain.android.deeplink.OpenBankingDeepLinkParser
import piuk.blockchain.android.domain.repositories.AssetActivityRepository
import piuk.blockchain.android.domain.repositories.TradeDataManager
import piuk.blockchain.android.domain.usecases.GetAvailableCryptoAssetsUseCase
import piuk.blockchain.android.domain.usecases.GetEligibilityAndNextPaymentDateUseCase
import piuk.blockchain.android.domain.usecases.GetReceiveAccountsForAssetUseCase
import piuk.blockchain.android.domain.usecases.IsFirstTimeBuyerUseCase
import piuk.blockchain.android.everypay.service.EveryPayCardService
import piuk.blockchain.android.identity.SiftDigitalTrust
import piuk.blockchain.android.kyc.KycDeepLinkHelper
import piuk.blockchain.android.scan.QRCodeEncoder
import piuk.blockchain.android.scan.QrCodeDataManager
import piuk.blockchain.android.scan.QrScanResultProcessor
import piuk.blockchain.android.simplebuy.BankPartnerCallbackProviderImpl
import piuk.blockchain.android.simplebuy.BuyFlowNavigator
import piuk.blockchain.android.simplebuy.EURPaymentAccountMapper
import piuk.blockchain.android.simplebuy.GBPPaymentAccountMapper
import piuk.blockchain.android.simplebuy.SimpleBuyInteractor
import piuk.blockchain.android.simplebuy.SimpleBuyModel
import piuk.blockchain.android.simplebuy.SimpleBuyPrefsSerializer
import piuk.blockchain.android.simplebuy.SimpleBuyPrefsSerializerImpl
import piuk.blockchain.android.simplebuy.SimpleBuyState
import piuk.blockchain.android.simplebuy.SimpleBuySyncFactory
import piuk.blockchain.android.simplebuy.USDPaymentAccountMapper
import piuk.blockchain.android.sunriver.SunriverDeepLinkHelper
import piuk.blockchain.android.thepit.PitLinkingImpl
import piuk.blockchain.android.thepit.ThePitDeepLinkParser
import piuk.blockchain.android.ui.addresses.AccountPresenter
import piuk.blockchain.android.ui.airdrops.AirdropCentrePresenter
import piuk.blockchain.android.ui.auth.FirebaseMobileNoticeRemoteConfig
import piuk.blockchain.android.ui.auth.MobileNoticeRemoteConfig
import piuk.blockchain.android.ui.auth.PinEntryPresenter
import piuk.blockchain.android.ui.auth.newlogin.SecureChannelManager
import piuk.blockchain.android.ui.backup.completed.BackupWalletCompletedPresenter
import piuk.blockchain.android.ui.backup.start.BackupWalletStartingInteractor
import piuk.blockchain.android.ui.backup.start.BackupWalletStartingModel
import piuk.blockchain.android.ui.backup.start.BackupWalletStartingState
import piuk.blockchain.android.ui.backup.verify.BackupVerifyPresenter
import piuk.blockchain.android.ui.backup.wordlist.BackupWalletWordListPresenter
import piuk.blockchain.android.ui.createwallet.CreateWalletPresenter
import piuk.blockchain.android.ui.customviews.SecondPasswordDialog
import piuk.blockchain.android.ui.home.CredentialsWiper
import piuk.blockchain.android.ui.home.MainPresenter
import piuk.blockchain.android.ui.home.MainScreenLauncher
import piuk.blockchain.android.ui.kyc.autocomplete.PlacesClientProvider
import piuk.blockchain.android.ui.kyc.email.entry.EmailVeriffModel
import piuk.blockchain.android.ui.kyc.email.entry.EmailVerifyInteractor
import piuk.blockchain.android.ui.kyc.settings.KycStatusHelper
import piuk.blockchain.android.ui.launcher.DeepLinkPersistence
import piuk.blockchain.android.ui.launcher.LauncherPresenter
import piuk.blockchain.android.ui.launcher.Prerequisites
import piuk.blockchain.android.ui.linkbank.BankAuthModel
import piuk.blockchain.android.ui.linkbank.BankAuthState
import piuk.blockchain.android.ui.onboarding.OnboardingPresenter
import piuk.blockchain.android.ui.pairingcode.PairingModel
import piuk.blockchain.android.ui.pairingcode.PairingState
import piuk.blockchain.android.ui.recover.AccountRecoveryInteractor
import piuk.blockchain.android.ui.recover.AccountRecoveryModel
import piuk.blockchain.android.ui.recover.AccountRecoveryState
import piuk.blockchain.android.ui.recover.RecoverFundsPresenter
import piuk.blockchain.android.ui.resources.AssetResources
import piuk.blockchain.android.ui.resources.AssetResourcesImpl
import piuk.blockchain.android.ui.sell.BuySellFlowNavigator
import piuk.blockchain.android.ui.settings.SettingsPresenter
import piuk.blockchain.android.ui.ssl.SSLVerifyPresenter
import piuk.blockchain.android.ui.thepit.PitPermissionsPresenter
import piuk.blockchain.android.ui.thepit.PitVerifyEmailPresenter
import piuk.blockchain.android.ui.transfer.receive.detail.ReceiveDetailIntentHelper
import piuk.blockchain.android.ui.upsell.KycUpgradePromptManager
import piuk.blockchain.android.util.AppUtil
import piuk.blockchain.android.util.BackupWalletUtil
import piuk.blockchain.android.util.CurrentContextAccess
import piuk.blockchain.android.util.FormatChecker
import piuk.blockchain.android.util.OSUtil
import piuk.blockchain.android.util.ResourceDefaultLabels
import piuk.blockchain.android.util.RootUtil
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidcore.data.access.PinRepository
import piuk.blockchain.androidcore.data.api.ConnectionApi
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.auth.metadata.WalletCredentialsMetadataUpdater
import piuk.blockchain.androidcore.utils.SSLVerifyUtil
import thepit.PitLinking

val applicationModule = module {

    factory { OSUtil(get()) }

    factory { StringUtils(get()) }

    single {
        AppUtil(
            context = get(),
            payloadManager = get(),
            prefs = get(),
            trust = get(),
            pinRepository = get()
        )
    }

    single {
        val ctx: Context = get()
        object : AppInfo {
            override val cacheDir: File =
                ctx.applicationContext.cacheDir
        }
    }.bind(AppInfo::class)

    factory { RootUtil() }

    single {
        CoinsWebSocketService(
            applicationContext = get()
        )
    }

    factory { get<Context>().resources }

    single { CurrentContextAccess() }

    single { LifecycleInterestedComponent() }
        .bind(LifecycleObservable::class)

    single {
        SiftDigitalTrust(
            accountId = BuildConfig.SIFT_ACCOUNT_ID,
            beaconKey = BuildConfig.SIFT_BEACON_KEY
        )
    }.bind(DigitalTrust::class)

    scope(payloadScopeQualifier) {

        factory {
            SecondPasswordDialog(contextAccess = get(), payloadManager = get())
        }.bind(SecondPasswordHandler::class)

        factory {
            KycStatusHelper(
                nabuDataManager = get(),
                nabuToken = get(),
                settingsDataManager = get(),
                tierService = get()
            )
        }

        factory {
            BankPartnerCallbackProviderImpl()
        }.bind(BankPartnerCallbackProvider::class)

        scoped {
            CredentialsWiper(
                payloadManagerWiper = get(),
                appUtil = get(),
                ethDataManager = get(),
                bchDataManager = get(),
                metadataManager = get(),
                walletOptionsState = get(),
                nabuDataManager = get(),
                notificationTokenManager = get()
            )
        }

        factory {
            MainPresenter(
                prefs = get(),
                assetCatalogue = get(),
                appUtil = get(),
                credentialsWiper = get(),
                payloadDataManager = get(),
                qrProcessor = get(),
                kycStatusHelper = get(),
                deepLinkProcessor = get(),
                sunriverCampaignRegistration = get(),
                xlmDataManager = get(),
                pitLinking = get(),
                simpleBuySync = get(),
                crashLogger = get(),
                analytics = get(),
                bankLinkingPrefs = get(),
                custodialWalletManager = get(),
                upsellManager = get(),
                secureChannelManager = get(),
                payloadManager = get()
            )
        }

        scoped {
            SecureChannelManager(
                secureChannelPrefs = get(),
                authPrefs = get(),
                payloadManager = get(),
                walletApi = get()
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

        scoped {
            CoinsWebSocketStrategy(
                coinsWebSocket = get(),
                ethDataManager = get(),
                erc20DataManager = get(),
                bchDataManager = get(),
                stringUtils = get(),
                gson = get(),
                payloadDataManager = get(),
                rxBus = get(),
                prefs = get(),
                appUtil = get(),
                pinRepository = get(),
                assetCatalogue = get()
            )
        }.bind(CoinsWebSocketInterface::class)

        factory {
            GsonBuilder().create()
        }

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
                crashLogger = get(),
                qrCodeDataManager = get(),
                payloadDataManager = get(),
                authDataManager = get(),
                analytics = get()
            )
        }

        factory {
            CreateWalletPresenter(
                payloadDataManager = get(),
                prefs = get(),
                appUtil = get(),
                analytics = get(),
                environmentConfig = get(),
                formatChecker = get(),
                nabuUserDataManager = get(),
                specificAnalytics = get(),
                eligibilityDataManager = get()
            )
        }

        factory {
            RecoverFundsPresenter(
                payloadDataManager = get(),
                prefs = get(),
                metadataInteractor = get(),
                metadataDerivation = MetadataDerivation(),
                moshi = get(),
                analytics = get()
            )
        }

        factory {
            BackupWalletStartingInteractor(
                prefs = get(),
                settingsDataManager = get()
            )
        }

        factory {
            BackupWalletStartingModel(
                initialState = BackupWalletStartingState(),
                mainScheduler = AndroidSchedulers.mainThread(),
                environmentConfig = get(),
                crashLogger = get(),
                interactor = get()
            )
        }

        factory {
            BackupWalletWordListPresenter(
                backupWalletUtil = get()
            )
        }

        factory {
            BackupWalletUtil(
                payloadDataManager = get()
            )
        }

        factory {
            BackupVerifyPresenter(
                payloadDataManager = get(),
                backupWalletUtil = get(),
                walletStatus = get()
            )
        }

        factory {
            AccountRecoveryModel(
                initialState = AccountRecoveryState(),
                mainScheduler = AndroidSchedulers.mainThread(),
                environmentConfig = get(),
                crashLogger = get(),
                interactor = get()
            )
        }

        factory {
            AccountRecoveryInteractor(
                payloadDataManager = get(),
                prefs = get(),
                metadataInteractor = get(),
                metadataDerivation = MetadataDerivation(),
                nabuDataManager = get()
            )
        }

        factory {
            SunriverDeepLinkHelper(
                linkHandler = get()
            )
        }

        factory {
            KycDeepLinkHelper(
                linkHandler = get()
            )
        }

        factory {
            ThePitDeepLinkParser()
        }

        factory {
            BlockchainDeepLinkParser()
        }

        factory { EmailVerificationDeepLinkHelper() }

        factory {
            DeepLinkProcessor(
                linkHandler = get(),
                kycDeepLinkHelper = get(),
                sunriverDeepLinkHelper = get(),
                emailVerifiedLinkHelper = get(),
                thePitDeepLinkParser = get(),
                openBankingDeepLinkParser = get(),
                blockchainDeepLinkParser = get()
            )
        }

        factory {
            OpenBankingDeepLinkParser()
        }

        scoped {
            QrScanResultProcessor(
                bitPayDataManager = get()
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
                tierService = get(),
                custodialWalletManager = get(),
                limitsDataManager = get(),
                coincore = get(),
                eligibilityProvider = get(),
                bankLinkingPrefs = get(),
                analytics = get(),
                exchangeRatesDataManager = get(),
                bankPartnerCallbackProvider = get(),
                stripeAndCheckoutPaymentsFeatureFlag = get(stripeAndCheckoutPaymentsFeatureFlag),
                brokerageDataManager = get(),
                cardProcessors = getCardProcessors().associateBy { it.acquirer }
            )
        }

        factory {
            SimpleBuyModel(
                interactor = get(),
                uiScheduler = AndroidSchedulers.mainThread(),
                initialState = SimpleBuyState(),
                ratingPrefs = get(),
                prefs = get(),
                simpleBuyPrefs = get(),
                serializer = get(),
                cardActivator = get(),
                _activityIndicator = lazy { get<AppUtil>().activityIndicator },
                environmentConfig = get(),
                crashLogger = get(),
                isFirstTimeBuyerUseCase = get(),
                getEligibilityAndNextPaymentDateUseCase = get(),
                bankPartnerCallbackProvider = get(),
                userIdentity = get()
            )
        }

        factory {
            IsFirstTimeBuyerUseCase(
                tradeDataManager = get()
            )
        }

        factory {
            GetEligibilityAndNextPaymentDateUseCase(
                tradeDataManager = get()
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
            TradeDataManagerImpl(
                tradeService = get(),
                authenticator = get(),
                accumulatedInPeriodMapper = get(),
                nextPaymentRecurringBuyMapper = get()
            )
        }.bind(TradeDataManager::class)

        factory {
            GetAccumulatedInPeriodToIsFirstTimeBuyerMapper()
        }.bind(Mapper::class)

        factory {
            GetNextPaymentDateListToFrequencyDateMapper()
        }.bind(Mapper::class)

        factory {
            SimpleBuyPrefsSerializerImpl(
                prefs = get(),
                assetCatalogue = get()
            )
        }.bind(SimpleBuyPrefsSerializer::class)

        factory {
            BankAuthModel(
                interactor = get(),
                uiScheduler = AndroidSchedulers.mainThread(),
                initialState = BankAuthState(),
                environmentConfig = get(),
                crashLogger = get()
            )
        }

        factory {
            CardModel(
                interactor = get(),
                currencyPrefs = get(),
                uiScheduler = AndroidSchedulers.mainThread(),
                cardActivator = get(),
                gson = get(),
                prefs = get(),
                environmentConfig = get(),
                crashLogger = get()
            )
        }

        factory {
            BuyFlowNavigator(
                simpleBuySyncFactory = get(),
                userIdentity = get(),
                currencyPrefs = get(),
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
                serializer = get()
            )
        }

        factory {
            KycUpgradePromptManager(
                identity = get()
            )
        }

        factory {
            SettingsPresenter(
                authDataManager = get(),
                settingsDataManager = get(),
                emailUpdater = get(),
                payloadManager = get(),
                payloadDataManager = get(),
                prefs = get(),
                pinRepository = get(),
                custodialWalletManager = get(),
                notificationTokenManager = get(),
                exchangeRates = get(),
                kycStatusHelper = get(),
                pitLinking = get(),
                analytics = get(),
                biometricsController = get(),
                ratingPrefs = get(),
                qrProcessor = get(),
                secureChannelManager = get()
            )
        }

        factory {
            PinEntryPresenter(
                authDataManager = get(),
                appUtil = get(),
                prefs = get(),
                payloadDataManager = get(),
                defaultLabels = get(),
                pinRepository = get(),
                walletOptionsDataManager = get(),
                specificAnalytics = get(),
                mobileNoticeRemoteConfig = get(),
                crashLogger = get(),
                analytics = get(),
                apiStatus = get(),
                credentialsWiper = get(),
                biometricsController = get()
            )
        }

        scoped {
            PitLinkingImpl(
                nabu = get(),
                nabuToken = get(),
                payloadDataManager = get(),
                ethDataManager = get(),
                bchDataManager = get(),
                xlmDataManager = get()
            )
        }.bind(PitLinking::class)

        factory {
            PitPermissionsPresenter(
                nabu = get(),
                nabuToken = get(),
                pitLinking = get(),
                analytics = get(),
                prefs = get()
            )
        }

        factory {
            PitVerifyEmailPresenter(
                nabuToken = get(),
                nabu = get(),
                emailSyncUpdater = get()
            )
        }

        factory {
            BackupWalletCompletedPresenter(
                walletStatus = get(),
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
                metadataManager = get(),
                settingsDataManager = get(),
                coincore = get(),
                exchangeRates = get(),
                crashLogger = get(),
                simpleBuySync = get(),
                rxBus = get(),
                flushables = getAll(AppStartUpFlushable::class),
                walletCredentialsUpdater = get()
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
                crashLogger = get()
            )
        }

        factory<BiometricDataRepository> {
            BiometricsDataRepositoryImpl(prefsUtil = get())
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
                crashLogger = get()
            )
        }.binds(arrayOf(BiometricAuth::class, BiometricsController::class))

        factory {
            CryptographyManagerImpl()
        }.bind(CryptographyManager::class)

        factory {
            EmailVeriffModel(
                interactor = get(),
                uiScheduler = AndroidSchedulers.mainThread(),
                environmentConfig = get(),
                crashLogger = get()
            )
        }

        factory {
            EmailVerifyInteractor(
                emailUpdater = get(),
                isRedesignEnabled = get(walletRedesignFeatureFlag)
            )
        }

        scoped {
            AssetActivityRepository(
                coincore = get()
            )
        }

        scoped {
            PlacesClientProvider(
                context = get()
            )
        }

        factory {
            MainScreenLauncher(
                walletRedesignFeatureFlag = get(walletRedesignFeatureFlag),
                crashLogger = get()
            )
        }

        factory {
            EveryPayCardService(
                everyPayService = get()
            )
        }

        factory {
            CardProviderActivator(
                custodialWalletManager = get(),
                submitEveryPayCardService = get()
            )
        }.bind(CardActivator::class)
    }

    factory {
        FirebaseMobileNoticeRemoteConfig(remoteConfig = get())
    }.bind(MobileNoticeRemoteConfig::class)

    factory {
        QrCodeDataManager()
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

    single<SqlDriver> {
        AndroidSqliteDriver(
            schema = Database.Schema,
            context = get(),
            name = "cache.db"
        )
    }

    factory {
        LauncherPresenter(
            appUtil = get(),
            prefs = get(),
            deepLinkPersistence = get(),
            envSettings = get(),
            authPrefs = get()
        )
    }

    factory {
        DeepLinkPersistence(
            prefs = get()
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
}

fun getCardProcessors(): List<CardProcessor> {
    return payloadScope.getAll()
}
