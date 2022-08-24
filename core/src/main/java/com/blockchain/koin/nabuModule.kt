package com.blockchain.koin

import com.blockchain.api.nabuApi
import com.blockchain.auth.AuthHeaderProvider
import com.blockchain.core.kyc.data.KycRepository
import com.blockchain.core.kyc.data.datasources.KycTiersStore
import com.blockchain.core.kyc.domain.KycService
import com.blockchain.nabu.Authenticator
import com.blockchain.nabu.CreateNabuToken
import com.blockchain.nabu.NabuToken
import com.blockchain.nabu.NabuUserSync
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.api.getuser.data.GetUserStore
import com.blockchain.nabu.api.getuser.data.UserFeaturePermissionRepository
import com.blockchain.nabu.api.getuser.data.UserRepository
import com.blockchain.nabu.api.getuser.domain.UserFeaturePermissionService
import com.blockchain.nabu.api.getuser.domain.UserService
import com.blockchain.nabu.api.nabu.Nabu
import com.blockchain.nabu.datamanagers.AnalyticsNabuUserReporterImpl
import com.blockchain.nabu.datamanagers.AnalyticsWalletReporter
import com.blockchain.nabu.datamanagers.CreateNabuTokenAdapter
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.NabuAuthenticator
import com.blockchain.nabu.datamanagers.NabuCachedEligibilityProvider
import com.blockchain.nabu.datamanagers.NabuDataManager
import com.blockchain.nabu.datamanagers.NabuDataManagerImpl
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
import com.blockchain.nabu.datamanagers.repositories.swap.CustodialRepository
import com.blockchain.nabu.datamanagers.repositories.swap.SwapActivityProvider
import com.blockchain.nabu.datamanagers.repositories.swap.SwapActivityProviderImpl
import com.blockchain.nabu.datamanagers.repositories.swap.TradingPairsProvider
import com.blockchain.nabu.datamanagers.repositories.swap.TradingPairsProviderImpl
import com.blockchain.nabu.metadata.AccountCredentialsMetadata
import com.blockchain.nabu.metadata.MetadataRepositoryNabuTokenAdapter
import com.blockchain.nabu.service.NabuService
import com.blockchain.nabu.service.RetailWalletTokenService
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
                trust = get(),
            )
        }.bind(NabuDataManager::class)

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

        scoped<UserService> {
            UserRepository(
                getUserStore = get()
            )
        }

          scoped<UserFeaturePermissionService> {
              UserFeaturePermissionRepository(
                  kycService = get(),
                  interestService = get(),
                  sddService = get(),
                  eligibilityService = get(),
                  simpleBuyService = get()
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
                custodialRepository = get(),
                transactionErrorMapper = get(),
                currencyPrefs = get(),
                buyOrdersCache = get(),
                pairsCache = get(),
                simpleBuyService = get(),
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
                interestService = get(),
                kycService = get(),
                simpleBuyEligibilityProvider = get(),
                eligibilityService = get(),
                userService = get(),
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

        scoped<KycService> {
            KycRepository(
                kycTiersStore = get(),
                userService = get(),
                assetCatalogue = get()
            )
        }

        scoped {
            KycTiersStore(
                kycApiService = get(),
                authenticator = get()
            )
        }

        factory {
            CreateNabuTokenAdapter(get())
        }.bind(CreateNabuToken::class)

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
        get<Retrofit>(nabuApi).create(Nabu::class.java)
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
                remoteLogger = get(),
                authInterceptorFeatureFlag = get(authInterceptorFeatureFlag),
            )
        }.bind(Authenticator::class).bind(AuthHeaderProvider::class)
    }
}
