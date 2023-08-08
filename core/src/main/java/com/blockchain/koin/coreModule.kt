package com.blockchain.koin

import com.blockchain.api.services.SelfCustodyServiceAuthCredentials
import com.blockchain.core.TransactionsStore
import com.blockchain.core.access.PinRepository
import com.blockchain.core.access.PinRepositoryImpl
import com.blockchain.core.asset.data.AssetRepository
import com.blockchain.core.asset.data.dataresources.AssetInformationStore
import com.blockchain.core.asset.domain.AssetService
import com.blockchain.core.auth.AuthDataManager
import com.blockchain.core.auth.VerifyCloudBackupStorage
import com.blockchain.core.auth.WalletAuthService
import com.blockchain.core.buy.data.SimpleBuyRepository
import com.blockchain.core.buy.data.dataresources.BuyOrdersStore
import com.blockchain.core.buy.data.dataresources.BuyPairsStore
import com.blockchain.core.buy.data.dataresources.SimpleBuyEligibilityStore
import com.blockchain.core.buy.domain.SimpleBuyService
import com.blockchain.core.chains.bitcoin.PaymentService
import com.blockchain.core.chains.bitcoin.SendDataManager
import com.blockchain.core.chains.bitcoincash.BchBalanceCache
import com.blockchain.core.chains.bitcoincash.BchDataManager
import com.blockchain.core.chains.bitcoincash.BchDataStore
import com.blockchain.core.chains.dynamicselfcustody.data.CoinTypeStore
import com.blockchain.core.chains.dynamicselfcustody.data.NonCustodialRepository
import com.blockchain.core.chains.dynamicselfcustody.data.NonCustodialSubscriptionsStore
import com.blockchain.core.chains.dynamicselfcustody.domain.NonCustodialService
import com.blockchain.core.chains.erc20.call.Erc20HistoryCallCache
import com.blockchain.core.chains.erc20.data.store.Erc20L2DataSource
import com.blockchain.core.chains.erc20.data.store.Erc20L2Store
import com.blockchain.core.chains.erc20.data.store.L1BalanceStore
import com.blockchain.core.chains.ethereum.EthDataManager
import com.blockchain.core.chains.ethereum.EthLastTxCache
import com.blockchain.core.chains.ethereum.EthMessageSigner
import com.blockchain.core.chains.ethereum.EvmNetworkPreImageSigner
import com.blockchain.core.chains.ethereum.datastores.EthDataStore
import com.blockchain.core.common.caching.StoreWiperImpl
import com.blockchain.core.connectivity.SSLPinningEmitter
import com.blockchain.core.connectivity.SSLPinningObservable
import com.blockchain.core.connectivity.SSLPinningSubject
import com.blockchain.core.custodial.BrokerageDataManager
import com.blockchain.core.custodial.data.TradingRepository
import com.blockchain.core.custodial.data.store.FiatAssetsStore
import com.blockchain.core.custodial.data.store.TradingStore
import com.blockchain.core.custodial.domain.TradingService
import com.blockchain.core.custodial.fees.WithdrawFeesStore
import com.blockchain.core.dataremediation.DataRemediationRepository
import com.blockchain.core.dynamicassets.DynamicAssetsDataManager
import com.blockchain.core.dynamicassets.impl.DynamicAssetsDataManagerImpl
import com.blockchain.core.eligibility.EligibilityRepository
import com.blockchain.core.eligibility.cache.ProductsEligibilityStore
import com.blockchain.core.fees.FeeDataManager
import com.blockchain.core.fiatcurrencies.FiatCurrenciesRepository
import com.blockchain.core.history.data.datasources.PaymentTransactionHistoryStore
import com.blockchain.core.limits.LimitsDataManager
import com.blockchain.core.limits.LimitsDataManagerImpl
import com.blockchain.core.mercuryexperiments.MercuryExperimentsRepository
import com.blockchain.core.payload.DataManagerPayloadDecrypt
import com.blockchain.core.payload.PayloadDataManager
import com.blockchain.core.payload.PayloadDataManagerSeedAccessAdapter
import com.blockchain.core.payload.PayloadService
import com.blockchain.core.payload.PromptingSeedAccessAdapter
import com.blockchain.core.payments.PaymentsRepository
import com.blockchain.core.payments.WithdrawLocksStore
import com.blockchain.core.payments.cache.CardDetailsStore
import com.blockchain.core.payments.cache.LinkedBankStore
import com.blockchain.core.payments.cache.LinkedCardsStore
import com.blockchain.core.payments.cache.PaymentMethodsEligibilityStore
import com.blockchain.core.payments.cache.PaymentMethodsStore
import com.blockchain.core.recurringbuy.data.RecurringBuyRepository
import com.blockchain.core.recurringbuy.domain.RecurringBuyService
import com.blockchain.core.referral.ReferralRepository
import com.blockchain.core.referral.dataresource.ReferralStore
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
import com.blockchain.core.walletoptions.WalletOptionsStore
import com.blockchain.core.watchlist.data.WatchlistRepository
import com.blockchain.core.watchlist.data.datasources.WatchlistStore
import com.blockchain.core.watchlist.domain.WatchlistService
import com.blockchain.domain.dataremediation.DataRemediationService
import com.blockchain.domain.eligibility.EligibilityService
import com.blockchain.domain.fiatcurrencies.FiatCurrenciesService
import com.blockchain.domain.mercuryexperiments.MercuryExperimentsService
import com.blockchain.domain.paymentmethods.BankService
import com.blockchain.domain.paymentmethods.CardService
import com.blockchain.domain.paymentmethods.PaymentMethodService
import com.blockchain.domain.referral.ReferralService
import com.blockchain.logging.LastTxUpdateDateOnSettingsService
import com.blockchain.logging.LastTxUpdater
import com.blockchain.nabu.datamanagers.repositories.swap.SwapTransactionsStore
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

    single {
        EthLastTxCache(
            ethAccountApi = get()
        )
    }

    single {
        WithdrawFeesStore(
            withdrawFeesService = get()
        )
    }

    single {
        FiatAssetsStore(
            discoveryService = get()
        )
    }

    factory { PrivateKeyFactory() }

    scope(payloadScopeQualifier) {

        factory<DataRemediationService> {
            DataRemediationRepository(
                api = get()
            )
        }

        scoped {
            TradingStore(
                balanceService = get()
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
                brokerageService = get()
            )
        }

        scoped {
            LimitsDataManagerImpl(
                limitsService = get(),
                exchangeRatesDataManager = get(),
                assetCatalogue = get()
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
                api = get()
            )
        }.bind(FiatCurrenciesService::class)

        scoped<RecurringBuyService> {
            RecurringBuyRepository(
                rbStore = get(),
                rbFrequencyConfigStore = get(),
                recurringBuyApiService = get(),
                assetCatalogue = get(),
                userFeaturePermissionService = get()
            )
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
                buyPairsStore = get(),
                buyOrdersStore = get(),
                swapOrdersStore = get(),
                transactionsStore = get(),
                assetCatalogue = get(),
                dismissRecorder = get()
            )
        }

        scoped {
            PaymentTransactionHistoryStore(
                nabuService = get()
            )
        }

        scoped {
            SwapTransactionsStore(
                nabuService = get()
            )
        }

        scoped {
            BuyOrdersStore(
                nabuService = get()
            )
        }

        scoped {
            TransactionsStore(
                nabuService = get()
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
                ethLastTxCache = get(),
                nonCustodialEvmService = get()
            )
        }.apply {
            bind(EthMessageSigner::class)
            bind(EvmNetworkPreImageSigner::class)
        }

        scoped {
            L1BalanceStore(
                ethDataManager = get(),
                remoteLogger = get()
            )
        }

        scoped<Erc20L2DataSource> {
            Erc20L2Store(
                evmService = get(),
                ethDataManager = get()
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
                walletOptionsStore = get()
            )
        }.apply {
            bind(XlmTransactionTimeoutFetcher::class)
            bind(XlmHorizonUrlFetcher::class)
        }

        scoped {
            WalletOptionsStore(walletAuthService = get())
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
                verifyCloudBackupStorage = get(),
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
            LinkedBankStore(
                paymentMethodsService = get()
            )
        }

        scoped {
            PaymentMethodsEligibilityStore(
                paymentMethodsService = get()
            )
        }

        scoped {
            WithdrawLocksStore(
                paymentsService = get(),
                currencyPrefs = get()
            )
        }

        scoped {
            PaymentMethodsStore(paymentsService = get())
        }

        scoped {
            CardDetailsStore(paymentMethodsService = get())
        }

        scoped {
            PaymentsRepository(
                paymentsService = get(),
                paymentMethodsStore = get(),
                paymentMethodsService = get(),
                cardDetailsStore = get(),
                linkedBankStore = get(),
                tradingService = get(),
                simpleBuyPrefs = get(),
                paymentMethodsEligibilityStore = get(),
                googlePayManager = get(),
                environmentConfig = get(),
                withdrawLocksStore = get(),
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

        scoped {
            ReferralStore(
                referralApi = get()
            )
        }
        factory {
            ReferralRepository(
                referralStore = get(),
                referralApi = get(),
                currencyPrefs = get()
            )
        }.bind(ReferralService::class)

        scoped<NonCustodialService> {
            NonCustodialRepository(
                dynamicSelfCustodyService = get(),
                assetCatalogue = get(),
                subscriptionsStore = get(),
                coinTypeStore = get(),
                remoteConfigService = get()
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

        scoped<MercuryExperimentsService> {
            MercuryExperimentsRepository(
                mercuryExperimentsApiService = get()
            )
        }
    }

    single {
        DynamicAssetsDataManagerImpl(
            discoveryService = get(),
            fiatAssetsStore = get()
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

    single {
        VerifyCloudBackupStorage(
            walletApi = get()
        )
    }
}
