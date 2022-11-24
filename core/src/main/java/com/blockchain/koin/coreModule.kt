package com.blockchain.koin

import com.blockchain.api.services.SelfCustodyServiceAuthCredentials
import com.blockchain.core.SwapTransactionsCache
import com.blockchain.core.TransactionsCache
import com.blockchain.core.access.PinRepository
import com.blockchain.core.access.PinRepositoryImpl
import com.blockchain.core.asset.data.AssetRepository
import com.blockchain.core.asset.data.dataresources.AssetInformationStore
import com.blockchain.core.asset.domain.AssetService
import com.blockchain.core.auth.AuthDataManager
import com.blockchain.core.auth.WalletAuthService
import com.blockchain.core.buy.BuyOrdersCache
import com.blockchain.core.buy.BuyPairsCache
import com.blockchain.core.buy.data.SimpleBuyRepository
import com.blockchain.core.buy.data.dataresources.BuyPairsStore
import com.blockchain.core.buy.data.dataresources.SimpleBuyEligibilityStore
import com.blockchain.core.buy.domain.SimpleBuyService
import com.blockchain.core.chains.EvmNetworksService
import com.blockchain.core.chains.bitcoin.PaymentService
import com.blockchain.core.chains.bitcoin.SendDataManager
import com.blockchain.core.chains.bitcoincash.BchBalanceCache
import com.blockchain.core.chains.bitcoincash.BchDataManager
import com.blockchain.core.chains.bitcoincash.BchDataStore
import com.blockchain.core.chains.dynamicselfcustody.data.CoinTypeStore
import com.blockchain.core.chains.dynamicselfcustody.data.NonCustodialRepository
import com.blockchain.core.chains.dynamicselfcustody.data.NonCustodialSubscriptionsStore
import com.blockchain.core.chains.dynamicselfcustody.domain.NonCustodialService
import com.blockchain.core.chains.erc20.Erc20DataManager
import com.blockchain.core.chains.erc20.Erc20DataManagerImpl
import com.blockchain.core.chains.erc20.call.Erc20HistoryCallCache
import com.blockchain.core.chains.erc20.data.Erc20L2StoreRepository
import com.blockchain.core.chains.erc20.data.Erc20StoreRepository
import com.blockchain.core.chains.erc20.data.store.Erc20DataSource
import com.blockchain.core.chains.erc20.data.store.Erc20L2DataSource
import com.blockchain.core.chains.erc20.data.store.Erc20L2Store
import com.blockchain.core.chains.erc20.data.store.Erc20Store
import com.blockchain.core.chains.erc20.data.store.L1BalanceStore
import com.blockchain.core.chains.erc20.domain.Erc20L2StoreService
import com.blockchain.core.chains.erc20.domain.Erc20StoreService
import com.blockchain.core.chains.ethereum.EthDataManager
import com.blockchain.core.chains.ethereum.EthMessageSigner
import com.blockchain.core.chains.ethereum.datastores.EthDataStore
import com.blockchain.core.common.caching.StoreWiperImpl
import com.blockchain.core.connectivity.SSLPinningEmitter
import com.blockchain.core.connectivity.SSLPinningObservable
import com.blockchain.core.connectivity.SSLPinningSubject
import com.blockchain.core.custodial.BrokerageDataManager
import com.blockchain.core.custodial.data.TradingRepository
import com.blockchain.core.custodial.data.store.TradingStore
import com.blockchain.core.custodial.domain.TradingService
import com.blockchain.core.dataremediation.DataRemediationRepository
import com.blockchain.core.dynamicassets.DynamicAssetsDataManager
import com.blockchain.core.dynamicassets.impl.DynamicAssetsDataManagerImpl
import com.blockchain.core.eligibility.EligibilityRepository
import com.blockchain.core.eligibility.cache.ProductsEligibilityStore
import com.blockchain.core.fees.FeeDataManager
import com.blockchain.core.fiatcurrencies.FiatCurrenciesRepository
import com.blockchain.core.history.data.datasources.PaymentTransactionHistoryStore
import com.blockchain.core.interest.data.InterestRepository
import com.blockchain.core.interest.data.datasources.InterestAvailableAssetsStore
import com.blockchain.core.interest.data.datasources.InterestBalancesStore
import com.blockchain.core.interest.data.datasources.InterestEligibilityStore
import com.blockchain.core.interest.data.datasources.InterestLimitsStore
import com.blockchain.core.interest.data.datasources.InterestRateStore
import com.blockchain.core.interest.domain.InterestService
import com.blockchain.core.limits.LimitsDataManager
import com.blockchain.core.limits.LimitsDataManagerImpl
import com.blockchain.core.nftwaitlist.data.NftWailslitRepository
import com.blockchain.core.nftwaitlist.domain.NftWaitlistService
import com.blockchain.core.payload.DataManagerPayloadDecrypt
import com.blockchain.core.payload.PayloadDataManager
import com.blockchain.core.payload.PayloadDataManagerSeedAccessAdapter
import com.blockchain.core.payload.PayloadService
import com.blockchain.core.payload.PromptingSeedAccessAdapter
import com.blockchain.core.payments.PaymentsRepository
import com.blockchain.core.payments.WithdrawLocksCache
import com.blockchain.core.payments.cache.LinkedCardsStore
import com.blockchain.core.payments.cache.PaymentMethodsEligibilityStore
import com.blockchain.core.payments.cache.PaymentMethodsStore
import com.blockchain.core.referral.ReferralRepository
import com.blockchain.core.sdd.data.SddRepository
import com.blockchain.core.sdd.data.datasources.SddEligibilityStore
import com.blockchain.core.sdd.domain.SddService
import com.blockchain.core.sell.SellRepository
import com.blockchain.core.sell.domain.SellService
import com.blockchain.core.settings.EmailSyncUpdater
import com.blockchain.core.settings.PhoneNumberUpdater
import com.blockchain.core.settings.SettingsDataManager
import com.blockchain.core.settings.SettingsEmailAndSyncUpdater
import com.blockchain.core.settings.SettingsPhoneNumberUpdater
import com.blockchain.core.settings.SettingsService
import com.blockchain.core.settings.datastore.SettingsStore
import com.blockchain.core.user.NabuUserDataManager
import com.blockchain.core.user.NabuUserDataManagerImpl
import com.blockchain.core.user.WatchlistDataManager
import com.blockchain.core.user.WatchlistDataManagerImpl
import com.blockchain.core.utils.AESUtilWrapper
import com.blockchain.core.utils.UUIDGenerator
import com.blockchain.core.walletoptions.WalletOptionsDataManager
import com.blockchain.core.walletoptions.WalletOptionsState
import com.blockchain.core.watchlist.data.WatchlistRepository
import com.blockchain.core.watchlist.data.datasources.WatchlistStore
import com.blockchain.core.watchlist.domain.WatchlistService
import com.blockchain.domain.dataremediation.DataRemediationService
import com.blockchain.domain.eligibility.EligibilityService
import com.blockchain.domain.fiatcurrencies.FiatCurrenciesService
import com.blockchain.domain.paymentmethods.BankService
import com.blockchain.domain.paymentmethods.CardService
import com.blockchain.domain.paymentmethods.PaymentMethodService
import com.blockchain.domain.referral.ReferralService
import com.blockchain.logging.LastTxUpdateDateOnSettingsService
import com.blockchain.logging.LastTxUpdater
import com.blockchain.payload.PayloadDecrypt
import com.blockchain.storedatasource.StoreWiper
import com.blockchain.sunriver.XlmHorizonUrlFetcher
import com.blockchain.sunriver.XlmTransactionTimeoutFetcher
import com.blockchain.wallet.SeedAccess
import com.blockchain.wallet.SeedAccessWithoutPrompt
import info.blockchain.wallet.payload.WalletPayloadService
import info.blockchain.wallet.util.PrivateKeyFactory
import java.util.UUID
import org.koin.dsl.bind
import org.koin.dsl.module

val coreModule = module {

    single { SSLPinningSubject() }.apply {
        bind(SSLPinningObservable::class)
        bind(SSLPinningEmitter::class)
    }

    factory {
        WalletAuthService(
            walletApi = get(),
            sessionIdService = get()
        )
    }

    factory { PrivateKeyFactory() }

    scope(payloadScopeQualifier) {

        factory<DataRemediationService> {
            DataRemediationRepository(
                api = get(),
            )
        }

        scoped {
            TradingStore(
                balanceService = get(),
            )
        }

        scoped<TradingService> {
            TradingRepository(
                assetCatalogue = get(),
                tradingStore = get()
            )
        }

        scoped {
            BrokerageDataManager(
                brokerageService = get(),
            )
        }

        scoped {
            LimitsDataManagerImpl(
                limitsService = get(),
                exchangeRatesDataManager = get(),
                assetCatalogue = get(),
            )
        }.bind(LimitsDataManager::class)

        factory {
            ProductsEligibilityStore(
                productEligibilityApi = get()
            )
        }

        scoped {
            EligibilityRepository(
                productsEligibilityStore = get(),
                eligibilityApiService = get()
            )
        }.bind(EligibilityService::class)

        scoped {
            FiatCurrenciesRepository(
                getUserStore = get(),
                userService = get(),
                assetCatalogue = get(),
                currencyPrefs = get(),
                analytics = get(),
                api = get(),
            )
        }.bind(FiatCurrenciesService::class)

        scoped {
            InterestBalancesStore(
                interestApiService = get(),
            )
        }

        scoped {
            InterestAvailableAssetsStore(
                interestApiService = get(),
            )
        }

        scoped {
            InterestEligibilityStore(
                interestApiService = get(),
            )
        }

        scoped {
            InterestLimitsStore(
                interestApiService = get(),
                currencyPrefs = get()
            )
        }

        scoped {
            InterestRateStore(
                interestApiService = get(),
            )
        }

        scoped<InterestService> {
            InterestRepository(
                assetCatalogue = get(),
                interestBalancesStore = get(),
                interestEligibilityStore = get(),
                interestAvailableAssetsStore = get(),
                interestLimitsStore = get(),
                interestRateStore = get(),
                paymentTransactionHistoryStore = get(),
                currencyPrefs = get(),
                interestApiService = get()
            )
        }

        scoped {
            SddEligibilityStore(
                nabuService = get()
            )
        }

        scoped<SddService> {
            SddRepository(
                sddEligibilityStore = get()
            )
        }

        scoped {
            BuyPairsCache(nabuService = get())
        }

        scoped {
            BuyPairsStore(nabuService = get())
        }

        scoped {
            SimpleBuyEligibilityStore(
                nabuService = get()
            )
        }

        scoped<SimpleBuyService> {
            SimpleBuyRepository(
                simpleBuyEligibilityStore = get(),
                buyPairsStore = get()
            )
        }

        scoped {
            TransactionsCache(
                nabuService = get(),
            )
        }

        scoped {
            PaymentTransactionHistoryStore(
                nabuService = get(),
            )
        }

        scoped {
            SwapTransactionsCache(
                nabuService = get(),
            )
        }

        scoped {
            BuyOrdersCache(nabuService = get())
        }

        factory {
            EvmNetworksService(
                remoteConfig = get()
            )
        }

        scoped {
            EthDataManager(
                payloadDataManager = get(),
                ethAccountApi = get(),
                ethDataStore = get(),
                metadataRepository = get(),
                defaultLabels = get(),
                lastTxUpdater = get(),
                evmNetworksService = get(),
                nonCustodialEvmService = get()
            )
        }.bind(EthMessageSigner::class)

        scoped {
            L1BalanceStore(
                ethDataManager = get(),
                remoteLogger = get()
            )
        }

        scoped<Erc20DataSource> {
            Erc20Store(
                erc20Service = get(),
                ethDataManager = get()
            )
        }

        scoped<Erc20StoreService> {
            Erc20StoreRepository(
                assetCatalogue = get(),
                erc20DataSource = get()
            )
        }

        scoped<Erc20L2DataSource> {
            Erc20L2Store(
                evmService = get(),
                ethDataManager = get()
            )
        }

        scoped<Erc20L2StoreService> {
            Erc20L2StoreRepository(
                assetCatalogue = get(),
                ethDataManager = get(),
                erc20L2DataSource = get()
            )
        }

        factory {
            Erc20HistoryCallCache(
                ethDataManager = get(),
                erc20Service = get(),
                evmService = get(),
                assetCatalogue = get()
            )
        }

        scoped {
            Erc20DataManagerImpl(
                ethDataManager = get(),
                l1BalanceStore = get(),
                historyCallCache = get(),
                assetCatalogue = get(),
                erc20StoreService = get(),
                erc20DataSource = get(),
                erc20L2StoreService = get(),
                erc20L2DataSource = get(),
                ethLayerTwoFeatureFlag = get(ethLayerTwoFeatureFlag),
                evmWithoutL1BalanceFeatureFlag = get(evmWithoutL1BalanceFeatureFlag)
            )
        }.bind(Erc20DataManager::class)

        factory { BchDataStore() }

        scoped {
            BchDataManager(
                payloadDataManager = get(),
                bchDataStore = get(),
                bitcoinApi = get(),
                bchBalanceCache = get(),
                defaultLabels = get(),
                metadataRepository = get(),
                remoteLogger = get()
            )
        }

        scoped {
            BchBalanceCache(
                payloadDataManager = get()
            )
        }

        factory {
            PayloadService(
                payloadManager = get(),
                sessionIdService = get()
            )
        }

        factory {
            PayloadDataManager(
                payloadService = get(),
                privateKeyFactory = get(),
                bitcoinApi = get(),
                payloadManager = get(),
                remoteLogger = get()
            )
        }.apply {
            bind(WalletPayloadService::class)
            bind(SelfCustodyServiceAuthCredentials::class)
        }

        factory {
            DataManagerPayloadDecrypt(
                payloadDataManager = get(),
                bchDataManager = get()
            )
        }.bind(PayloadDecrypt::class)

        factory { PromptingSeedAccessAdapter(PayloadDataManagerSeedAccessAdapter(get()), get()) }.apply {
            bind(SeedAccessWithoutPrompt::class)
            bind(SeedAccess::class)
        }

        scoped { EthDataStore() }

        scoped { WalletOptionsState() }

        scoped {
            SettingsDataManager(
                settingsService = get(),
                settingsStore = get(),
                currencyPrefs = get(),
                walletSettingsService = get(),
                assetCatalogue = get()
            )
        }

        scoped { SettingsService(get()) }

        scoped {
            SettingsStore(
                settingsService = get()
            )
        }

        factory {
            WalletOptionsDataManager(
                authService = get(),
                walletOptionsState = get(),
                settingsDataManager = get(),
                explorerUrl = getProperty("explorer-api")
            )
        }.apply {
            bind(XlmTransactionTimeoutFetcher::class)
            bind(XlmHorizonUrlFetcher::class)
        }

        scoped { FeeDataManager(get()) }

        factory {
            AuthDataManager(
                authApiService = get(),
                walletAuthService = get(),
                pinRepository = get(),
                aesUtilWrapper = AESUtilWrapper,
                remoteLogger = get(),
                authPrefs = get(),
                walletStatusPrefs = get(),
                encryptedPrefs = get()
            )
        }

        factory { LastTxUpdateDateOnSettingsService(get()) }.bind(LastTxUpdater::class)

        factory {
            SendDataManager(
                paymentService = get(),
                lastTxUpdater = get()
            )
        }

        factory { SettingsPhoneNumberUpdater(get()) }.bind(PhoneNumberUpdater::class)

        factory { SettingsEmailAndSyncUpdater(get(), get()) }.bind(EmailSyncUpdater::class)

        scoped {
            NabuUserDataManagerImpl(
                nabuUserService = get(),
                kycService = get()
            )
        }.bind(NabuUserDataManager::class)

        scoped {
            LinkedCardsStore(
                paymentMethodsService = get()
            )
        }

        scoped {
            PaymentMethodsEligibilityStore(
                paymentMethodsService = get()
            )
        }

        scoped {
            WithdrawLocksCache(
                paymentsService = get(),
                currencyPrefs = get()
            )
        }

        scoped {
            PaymentMethodsStore(paymentsService = get())
        }
        scoped {
            PaymentsRepository(
                paymentsService = get(),
                paymentMethodsStore = get(),
                paymentMethodsService = get(),
                tradingService = get(),
                simpleBuyPrefs = get(),
                googlePayManager = get(),
                environmentConfig = get(),
                withdrawLocksCache = get(),
                assetCatalogue = get(),
                linkedCardsStore = get(),
                fiatCurrenciesService = get(),
                googlePayFeatureFlag = get(googlePayFeatureFlag),
                plaidFeatureFlag = get(plaidFeatureFlag)
            )
        }.apply {
            bind(BankService::class)
            bind(CardService::class)
            bind(PaymentMethodService::class)
        }

        scoped {
            WatchlistDataManagerImpl(
                watchlistService = get(),
                assetCatalogue = get()
            )
        }.bind(WatchlistDataManager::class)

        scoped<WatchlistService> {
            WatchlistRepository(
                watchlistStore = get(),
                watchlistApiService = get(),
                assetCatalogue = get()
            )
        }

        scoped {
            WatchlistStore(
                watchlistService = get()
            )
        }

        factory {
            ReferralRepository(
                referralApi = get(),
                currencyPrefs = get(),
            )
        }.bind(ReferralService::class)

        scoped<NftWaitlistService> {
            NftWailslitRepository(
                nftWaitlistApiService = get(),
                userIdentity = get()
            )
        }

        scoped<NonCustodialService> {
            NonCustodialRepository(
                dynamicSelfCustodyService = get(),
                currencyPrefs = get(),
                assetCatalogue = get(),
                remoteConfigService = get(),
                subscriptionsStore = get(),
                networkConfigsFF = get(coinNetworksFeatureFlag),
                coinTypeStore = get()
            )
        }

        scoped {
            CoinTypeStore(
                discoveryService = get()
            )
        }

        scoped {
            NonCustodialSubscriptionsStore(
                dynamicSelfCustodyService = get()
            )
        }

        scoped<SellService> {
            SellRepository(
                userFeaturePermissionService = get(),
                kycService = get(),
                simpleBuyService = get(),
                custodialWalletManager = get(),
                currencyPrefs = get()
            )
        }
    }

    single {
        DynamicAssetsDataManagerImpl(
            discoveryService = get(),
        )
    }.bind(DynamicAssetsDataManager::class)

    single {
        AssetInformationStore(
            discoveryService = get()
        )
    }

    single<AssetService> {
        AssetRepository(
            assetInformationStore = get()
        )
    }

    factory {
        object : UUIDGenerator {
            override fun generateUUID(): String = UUID.randomUUID().toString()
        }
    }.bind(UUIDGenerator::class)

    factory {
        PaymentService(
            payment = get(),
            dustService = get()
        )
    }

    single {
        PinRepositoryImpl()
    }.bind(PinRepository::class)

    single<StoreWiper> {
        StoreWiperImpl(
            inMemoryCacheWiper = get(),
            persistedJsonSqlDelightCacheWiper = get()
        )
    }
}
