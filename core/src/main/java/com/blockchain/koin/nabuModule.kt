package com.blockchain.koin

import com.blockchain.analytics.TraitsService
import com.blockchain.api.interceptors.SessionInfo
import com.blockchain.api.nabuApi
import com.blockchain.core.experiments.cache.ExperimentsStore
import com.blockchain.core.kyc.data.KycRepository
import com.blockchain.core.kyc.data.datasources.KycTiersStore
import com.blockchain.core.kyc.domain.KycService
import com.blockchain.domain.tags.TagsService
import com.blockchain.nabu.CreateNabuToken
import com.blockchain.nabu.NabuToken
import com.blockchain.nabu.NabuUserSync
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.api.getuser.data.GetUserStore
import com.blockchain.nabu.api.getuser.data.UserCountryRepository
import com.blockchain.nabu.api.getuser.data.UserFeaturePermissionRepository
import com.blockchain.nabu.api.getuser.data.UserRepository
import com.blockchain.nabu.api.getuser.domain.UserFeaturePermissionService
import com.blockchain.nabu.api.getuser.domain.UserService
import com.blockchain.nabu.api.nabu.Nabu
import com.blockchain.nabu.datamanagers.AnalyticsNabuUserReporterImpl
import com.blockchain.nabu.datamanagers.AnalyticsWalletReporter
import com.blockchain.nabu.datamanagers.CreateNabuTokenAdapter
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.NabuDataManager
import com.blockchain.nabu.datamanagers.NabuDataManagerImpl
import com.blockchain.nabu.datamanagers.NabuUserIdentity
import com.blockchain.nabu.datamanagers.NabuUserReporter
import com.blockchain.nabu.datamanagers.NabuUserSyncUpdateUserWalletInfoWithJWT
import com.blockchain.nabu.datamanagers.TransactionErrorMapper
import com.blockchain.nabu.datamanagers.UniqueAnalyticsNabuUserReporter
import com.blockchain.nabu.datamanagers.UniqueAnalyticsWalletReporter
import com.blockchain.nabu.datamanagers.WalletReporter
import com.blockchain.nabu.datamanagers.custodialwalletimpl.LiveCustodialWalletManager
import com.blockchain.nabu.datamanagers.repositories.QuotesProvider
import com.blockchain.nabu.datamanagers.repositories.WithdrawLocksRepository
import com.blockchain.nabu.datamanagers.repositories.swap.CustodialRepository
import com.blockchain.nabu.datamanagers.repositories.swap.CustodialSwapActivityStore
import com.blockchain.nabu.datamanagers.repositories.swap.CustodialTradingPairsStore
import com.blockchain.nabu.metadata.AccountCredentialsMetadata
import com.blockchain.nabu.metadata.MetadataRepositoryNabuTokenAdapter
import com.blockchain.nabu.service.NabuService
import com.blockchain.nabu.service.RetailWalletTokenService
import com.blockchain.nabu.service.UserTagsRepository
import com.blockchain.nabu.stores.NabuSessionTokenStore
import kotlinx.coroutines.Dispatchers
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
                trust = get(),
                userService = get()
            )
        }.bind(NabuDataManager::class)

        scoped {
            GetUserStore(
                nabuService = get(),
                userReporter = get(uniqueUserAnalytics),
                trust = get(),
                remoteLogger = get(),
                countryPrefs = get(),
                walletReporter = get(uniqueId),
                sessionInfo = SessionInfo,
                payloadDataManager = get()
            )
        }

        scoped<UserService> {
            UserRepository(
                getUserStore = get()
            )
        }

        scoped<UserFeaturePermissionService> {
            UserFeaturePermissionRepository(
                kycService = get(),
                interestService = get(),
                eligibilityService = get(),
                coroutinesDispatcher = Dispatchers.IO,
                simpleBuyService = get()
            )
        }

        factory {
            LiveCustodialWalletManager(
                assetCatalogue = get(),
                nabuService = get(),
                transactionsCache = get(),
                custodialRepository = get(),
                transactionErrorMapper = get(),
                currencyPrefs = get(),
                simpleBuyService = get(),
                paymentMethodsEligibilityStore = get(),
                fiatCurrenciesService = get(),
                recurringBuyService = get(),
                recurringBuyStore = get()
            )
        }.bind(CustodialWalletManager::class)

        factory {
            TransactionErrorMapper()
        }

        scoped {
            NabuUserIdentity(
                interestService = get(),
                kycService = get(),
                simpleBuyService = get(),
                eligibilityService = get(),
                userService = get(),
                bindFeatureFlag = get(bindFeatureFlag)
            )
        }.bind(UserIdentity::class)

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

        scoped<KycService> {
            KycRepository(
                kycTiersStore = get(),
                userService = get(),
                assetCatalogue = get(),
                kycApiService = get(),
                proveFeatureFlag = get(proveFeatureFlag)
            )
        }

        scoped {
            KycTiersStore(
                kycApiService = get()
            )
        }

        factory {
            CreateNabuTokenAdapter(get())
        }.bind(CreateNabuToken::class)

        factory {
            NabuUserSyncUpdateUserWalletInfoWithJWT(
                nabuDataManager = get(),
                nabuService = get(),
                getUserStore = get()
            )
        }.bind(NabuUserSync::class)

        scoped {
            CustodialRepository(
                pairsStore = get(),
                swapActivityStore = get(),
                assetCatalogue = get()
            )
        }

        scoped {
            CustodialSwapActivityStore(
                nabuService = get()
            )
        }

        scoped {
            CustodialTradingPairsStore(
                nabuService = get()
            )
        }

        scoped {
            WithdrawLocksRepository(custodialWalletManager = get())
        }

        factory {
            QuotesProvider(
                nabuService = get()
            )
        }
    }

    single { NabuSessionTokenStore() }

    single {
        NabuService(
            nabu = get(),
            tagsService = get(),
            remoteConfigPrefs = get(),
            environmentConfig = get()
        )
    }

    single {
        UserTagsRepository()
    }.bind(TagsService::class)

    factory {
        get<Retrofit>(nabuApi).create(Nabu::class.java)
    }

    single {
        RetailWalletTokenService(
            explorerPath = getProperty("explorer-api"),
            apiCode = getProperty("api-code"),
            retrofit = get(serializerExplorerRetrofit)
        )
    }

    single {
        UserCountryRepository(
            countryPrefs = lazy { get() }
        )
    }.bind(TraitsService::class)

    single {
        ExperimentsStore(
            experimentsApiService = get()
        )
    }
}
