package com.blockchain.koin

import com.blockchain.auth.AuthHeaderProvider
import com.blockchain.nabu.Authenticator
import com.blockchain.nabu.CreateNabuToken
import com.blockchain.nabu.NabuToken
import com.blockchain.nabu.NabuUserSync
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.api.getuser.data.GetUserStore
import com.blockchain.nabu.api.getuser.data.GetUserStoreRepository
import com.blockchain.nabu.api.getuser.domain.GetUserStoreService
import com.blockchain.nabu.api.kyc.data.KycStoreRepository
import com.blockchain.nabu.api.kyc.data.store.KycDataSource
import com.blockchain.nabu.api.kyc.data.store.KycStore
import com.blockchain.nabu.api.kyc.domain.KycStoreService
import com.blockchain.nabu.api.nabu.Nabu
import com.blockchain.nabu.cache.CustodialAssetsEligibilityCache
import com.blockchain.nabu.cache.UserCache
import com.blockchain.nabu.datamanagers.AnalyticsNabuUserReporterImpl
import com.blockchain.nabu.datamanagers.AnalyticsWalletReporter
import com.blockchain.nabu.datamanagers.CreateNabuTokenAdapter
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.NabuAuthenticator
import com.blockchain.nabu.datamanagers.NabuCachedEligibilityProvider
import com.blockchain.nabu.datamanagers.NabuDataManager
import com.blockchain.nabu.datamanagers.NabuDataManagerImpl
import com.blockchain.nabu.datamanagers.NabuDataUserProvider
import com.blockchain.nabu.datamanagers.NabuDataUserProviderNabuDataManagerAdapter
import com.blockchain.nabu.datamanagers.NabuUserIdentity
import com.blockchain.nabu.datamanagers.NabuUserReporter
import com.blockchain.nabu.datamanagers.NabuUserSyncUpdateUserWalletInfoWithJWT
import com.blockchain.nabu.datamanagers.SimpleBuyEligibilityProvider
import com.blockchain.nabu.datamanagers.TransactionErrorMapper
import com.blockchain.nabu.datamanagers.UniqueAnalyticsNabuUserReporter
import com.blockchain.nabu.datamanagers.UniqueAnalyticsWalletReporter
import com.blockchain.nabu.datamanagers.WalletReporter
import com.blockchain.nabu.datamanagers.custodialwalletimpl.LiveCustodialWalletManager
import com.blockchain.nabu.datamanagers.repositories.QuotesProvider
import com.blockchain.nabu.datamanagers.repositories.WithdrawLocksRepository
import com.blockchain.nabu.datamanagers.repositories.interest.InterestAvailabilityProvider
import com.blockchain.nabu.datamanagers.repositories.interest.InterestAvailabilityProviderImpl
import com.blockchain.nabu.datamanagers.repositories.interest.InterestEligibilityProvider
import com.blockchain.nabu.datamanagers.repositories.interest.InterestEligibilityProviderImpl
import com.blockchain.nabu.datamanagers.repositories.interest.InterestLimitsProvider
import com.blockchain.nabu.datamanagers.repositories.interest.InterestLimitsProviderImpl
import com.blockchain.nabu.datamanagers.repositories.interest.InterestRepository
import com.blockchain.nabu.datamanagers.repositories.swap.CustodialRepository
import com.blockchain.nabu.datamanagers.repositories.swap.SwapActivityProvider
import com.blockchain.nabu.datamanagers.repositories.swap.SwapActivityProviderImpl
import com.blockchain.nabu.datamanagers.repositories.swap.TradingPairsProvider
import com.blockchain.nabu.datamanagers.repositories.swap.TradingPairsProviderImpl
import com.blockchain.nabu.metadata.AccountCredentialsMetadata
import com.blockchain.nabu.metadata.MetadataRepositoryNabuTokenAdapter
import com.blockchain.nabu.service.NabuService
import com.blockchain.nabu.service.NabuTierService
import com.blockchain.nabu.service.RetailWalletTokenService
import com.blockchain.nabu.service.TierService
import com.blockchain.nabu.service.TierUpdater
import com.blockchain.nabu.stores.NabuSessionTokenStore
import org.koin.dsl.bind
import org.koin.dsl.module
import retrofit2.Retrofit

val nabuModule = module {

    scope(payloadScopeQualifier) {

        scoped {
            MetadataRepositoryNabuTokenAdapter(
                createNabuToken = get(),
                accountCredentialsMetadata = get()
            )
        }.bind(NabuToken::class)

        factory {
            AccountCredentialsMetadata(
                metadataRepository = get(),
                accountMetadataMigrationFF = get(metadataMigrationFeatureFlag),
                remoteLogger = get()
            )
        }

        factory {
            NabuDataManagerImpl(
                nabuService = get(),
                retailWalletTokenService = get(),
                nabuTokenStore = get(),
                appVersion = getProperty("app-version"),
                settingsDataManager = get(),
                payloadDataManager = get(),
                prefs = get(),
                walletReporter = get(uniqueId),
                userReporter = get(uniqueUserAnalytics),
                trust = get()
            )
        }.bind(NabuDataManager::class)

        scoped {
            UserCache(
                nabuService = get()
            )
        }

        scoped {
            GetUserStore(
                nabuService = get(),
                authenticator = get(),
                userReporter = get(uniqueUserAnalytics),
                trust = get(),
                walletReporter = get(uniqueId),
                payloadDataManager = get()
            )
        }

        scoped<GetUserStoreService> {
            GetUserStoreRepository(
                getUserStore = get()
            )
        }

        factory {
            LiveCustodialWalletManager(
                assetCatalogue = get(),
                nabuService = get(),
                authenticator = get(),
                paymentAccountMapperMappers = mapOf(
                    "EUR" to get(eur), "GBP" to get(gbp), "USD" to get(usd), "ARS" to get(ars)
                ),
                transactionsCache = get(),
                interestRepository = get(),
                custodialRepository = get(),
                transactionErrorMapper = get(),
                currencyPrefs = get(),
                buyOrdersCache = get(),
                pairsCache = get(),
                swapOrdersCache = get(),
                paymentMethodsEligibilityStore = get(),
                fiatCurrenciesService = get()
            )
        }.bind(CustodialWalletManager::class)

        factory {
            TransactionErrorMapper()
        }

        scoped {
            NabuUserIdentity(
                custodialWalletManager = get(),
                nabuUserDataManager = get(),
                simpleBuyEligibilityProvider = get(),
                interestEligibilityProvider = get(),
                nabuDataProvider = get(),
                eligibilityService = get(),
                nabuDataUserProvider = get(),
                bindFeatureFlag = get(bindFeatureFlag)
            )
        }.bind(UserIdentity::class)

        factory {
            NabuCachedEligibilityProvider(
                nabuService = get(),
                authenticator = get()
            )
        }.bind(SimpleBuyEligibilityProvider::class)

        factory {
            InterestLimitsProviderImpl(
                assetCatalogue = get(),
                nabuService = get(),
                authenticator = get(),
                currencyPrefs = get()
            )
        }.bind(InterestLimitsProvider::class)

        factory {
            InterestAvailabilityProviderImpl(
                assetCatalogue = get(),
                nabuService = get(),
                authenticator = get()
            )
        }.bind(InterestAvailabilityProvider::class)

        scoped {
            CustodialAssetsEligibilityCache(
                authenticator = get(),
                assetCatalogue = get(),
                service = get()
            )
        }

        factory {
            InterestEligibilityProviderImpl(
                custodialAssetsEligibilityCache = get()
            )
        }.bind(InterestEligibilityProvider::class)

        factory {
            TradingPairsProviderImpl(
                assetCatalogue = get(),
                nabuService = get(),
                authenticator = get()
            )
        }.bind(TradingPairsProvider::class)

        factory {
            SwapActivityProviderImpl(
                assetCatalogue = get(),
                nabuService = get(),
                authenticator = get()
            )
        }.bind(SwapActivityProvider::class)

        factory(uniqueUserAnalytics) {
            UniqueAnalyticsNabuUserReporter(
                nabuUserReporter = get(userAnalytics),
                prefs = get()
            )
        }.bind(NabuUserReporter::class)

        factory(userAnalytics) {
            AnalyticsNabuUserReporterImpl(
                userAnalytics = get()
            )
        }.bind(NabuUserReporter::class)

        factory(uniqueId) {
            UniqueAnalyticsWalletReporter(get(walletAnalytics), prefs = get())
        }.bind(WalletReporter::class)

        factory(walletAnalytics) {
            AnalyticsWalletReporter(userAnalytics = get())
        }.bind(WalletReporter::class)

        scoped<KycStoreService> {
            KycStoreRepository(
                kycDataSource = get(),
                assetCatalogue = get()
            )
        }

        scoped<KycDataSource> {
            KycStore(
                endpoint = get(),
                authenticator = get()
            )
        }

        factory {
            NabuTierService(
                assetCatalogue = get(),
                authenticator = get(),
                kycStoreService = get(),
                speedUpLoginKycFF = get(speedUpLoginKycFeatureFlag),
                endpoint = get()
            )
        }.apply {
            bind(TierService::class)
            bind(TierUpdater::class)
        }

        factory {
            CreateNabuTokenAdapter(get())
        }.bind(CreateNabuToken::class)

        factory<NabuDataUserProvider> {
            NabuDataUserProviderNabuDataManagerAdapter(
                authenticator = get(),
                userCache = get(),
                userReporter = get(uniqueUserAnalytics),
                trust = get(),
                walletReporter = get(uniqueId),
                payloadDataManager = get(),
                getUserStoreService = get(),
                speedUpLoginUserFF = get(speedUpLoginUserFeatureFlag)
            )
        }

        factory {
            NabuUserSyncUpdateUserWalletInfoWithJWT(
                authenticator = get(),
                nabuDataManager = get(),
                nabuService = get(),
                getUserStore = get(),
            )
        }.bind(NabuUserSync::class)

        scoped {
            CustodialRepository(
                pairsProvider = get(),
                activityProvider = get()
            )
        }

        scoped {
            InterestRepository(
                interestAvailabilityProvider = get(),
                interestEligibilityProvider = get(),
                interestLimitsProvider = get()
            )
        }

        scoped {
            WithdrawLocksRepository(custodialWalletManager = get())
        }

        factory {
            QuotesProvider(
                nabuService = get(),
                authenticator = get()
            )
        }
    }

    single { NabuSessionTokenStore() }

    single {
        NabuService(
            nabu = get(),
            remoteConfigPrefs = get(),
            environmentConfig = get()
        )
    }

    factory {
        get<Retrofit>(nabu).create(Nabu::class.java)
    }

    single {
        RetailWalletTokenService(
            explorerPath = getProperty("explorer-api"),
            apiCode = getProperty("api-code"),
            retrofit = get(serializerExplorerRetrofit)
        )
    }
}

val authenticationModule = module {
    scope(payloadScopeQualifier) {
        factory {
            NabuAuthenticator(
                nabuToken = get(),
                nabuDataManager = get(),
                remoteLogger = get()
            )
        }.bind(Authenticator::class).bind(AuthHeaderProvider::class)
    }
}
