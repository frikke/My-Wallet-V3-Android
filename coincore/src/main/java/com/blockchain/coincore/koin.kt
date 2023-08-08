package com.blockchain.coincore

import com.blockchain.DefiWalletReceiveAddressService
import com.blockchain.coincore.bch.BchAsset
import com.blockchain.coincore.btc.BtcAsset
import com.blockchain.coincore.eth.Erc20DataManagerImpl
import com.blockchain.coincore.eth.EthAsset
import com.blockchain.coincore.eth.GasFeeCalculator
import com.blockchain.coincore.fiat.LinkedBanksFactory
import com.blockchain.coincore.impl.BackendNotificationUpdater
import com.blockchain.coincore.impl.CoinAddressesStore
import com.blockchain.coincore.impl.DefiWalletReceiveAddressRepository
import com.blockchain.coincore.impl.EthHotWalletAddressResolver
import com.blockchain.coincore.impl.HotWalletService
import com.blockchain.coincore.impl.TxProcessorFactory
import com.blockchain.coincore.impl.txEngine.TransferQuotesEngine
import com.blockchain.coincore.loader.ActiveBalancesStore
import com.blockchain.coincore.loader.AssetCatalogueImpl
import com.blockchain.coincore.loader.AssetLoader
import com.blockchain.coincore.loader.CoinNetworksStore
import com.blockchain.coincore.loader.CustodialAssetsStore
import com.blockchain.coincore.loader.DynamicAssetLoader
import com.blockchain.coincore.loader.DynamicAssetsService
import com.blockchain.coincore.loader.EthErc20sStore
import com.blockchain.coincore.loader.HistoricActiveBalancesRepository
import com.blockchain.coincore.loader.L1CoinsStore
import com.blockchain.coincore.loader.NonCustodialL2sDynamicAssetRepository
import com.blockchain.coincore.loader.OtherNetworksErc20sStore
import com.blockchain.coincore.loader.UniversalDynamicAssetRepository
import com.blockchain.coincore.wrap.FormatUtilities
import com.blockchain.coincore.xlm.XlmAsset
import com.blockchain.core.chains.erc20.Erc20DataManager
import com.blockchain.core.chains.ethereum.EvmNetworksService
import com.blockchain.koin.activeRewardsBalanceStore
import com.blockchain.koin.dynamicEthHotWalletAddressFeatureFlag
import com.blockchain.koin.interestBalanceStore
import com.blockchain.koin.payloadScope
import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.koin.plaidFeatureFlag
import com.blockchain.koin.sellSwapBrokerageQuoteFeatureFlag
import com.blockchain.koin.stakingBalanceStore
import com.blockchain.logging.Logger
import com.blockchain.unifiedcryptowallet.domain.balances.CoinNetworksService
import com.blockchain.unifiedcryptowallet.domain.balances.NetworkAccountsService
import info.blockchain.balance.AssetCatalogue
import org.koin.dsl.bind
import org.koin.dsl.module

val coincoreModule = module {

    scope(payloadScopeQualifier) {
        scoped {
            BtcAsset(
                payloadManager = get(),
                sendDataManager = get(),
                feeDataManager = get(),
                walletPreferences = get(),
                notificationUpdater = get(),
                addressResolver = get()
            )
        }.bind(CryptoAsset::class)

        scoped {
            BchAsset(
                payloadManager = get(),
                bchDataManager = get(),
                labels = get(),
                feeDataManager = get(),
                sendDataManager = get(),
                walletPreferences = get(),
                beNotifyUpdate = get(),
                addressResolver = get(),
                bchBalanceCache = get()
            )
        }.bind(CryptoAsset::class)

        scoped {
            XlmAsset(
                payloadManager = get(),
                xlmDataManager = get(),
                xlmFeesFetcher = get(),
                walletOptionsDataManager = get(),
                walletPreferences = get(),
                addressResolver = get()
            )
        }.bind(CryptoAsset::class)

        scoped {
            EthAsset(
                ethDataManager = get(),
                feeDataManager = get(),
                walletPrefs = get(),
                labels = get(),
                notificationUpdater = get(),
                formatUtils = get(),
                addressResolver = get()
            )
        }.bind(CryptoAsset::class)

        scoped {
            Coincore(
                assetCatalogue = get(),
                payloadManager = get(),
                assetLoader = get(),
                txProcessorFactory = get(),
                defaultLabels = get(),
                currencyPrefs = get(),
                remoteLogger = get(),
                bankService = get(),
                walletModeService = get()
            )
        }
        scoped {
            NetworkAccountsRepository(
                coincore = get(),
                assetCatalogue = get(),
                coinsNetworksRepository = get(),
            )
        }.bind(NetworkAccountsService::class)

        scoped {
            CoinNetworksRepository(
                dynamicAssetService = get()
            )
        }.bind(CoinNetworksService::class)

        scoped {
            val ncAssets: List<CryptoAsset> = payloadScope.getAll()

            // For some unknown reason `getAll()` adds the last element twice. Which means
            // that last element calls init() twice. So make it a set, to remove any duplicates.
            DynamicAssetLoader(
                nonCustodialAssets = ncAssets.toSet(), // All the non custodial L1s that we support
                assetCatalogue = get(),
                payloadManager = get(),
                historicActiveBalancesRepository = get(),
                erc20DataManager = get(),
                feeDataManager = get(),
                unifiedBalancesService = lazy { get() },
                tradingService = get(),
                interestService = get(),
                remoteLogger = get(),
                labels = get(),
                walletPreferences = get(),
                formatUtils = get(),
                identityAddressResolver = get(),
                selfCustodyService = get(),
                ethHotWalletAddressResolver = get(),
                custodialWalletManager = get(),
                stakingService = get(),
                currencyPrefs = get(),
                activeRewardsService = get()
            )
        }.bind(AssetLoader::class)

        scoped {
            HotWalletService(
                walletOptionsStore = get()
            )
        }

        scoped {
            IdentityAddressResolver()
        }

        scoped {
            EthHotWalletAddressResolver(
                hotWalletService = get(),
                nabuService = get(),
                logger = Logger,
                dynamicEthHotWalletAddressFF = get(dynamicEthHotWalletAddressFeatureFlag)
            )
        }

        scoped {
            TxProcessorFactory(
                bitPayManager = get(),
                exchangeRates = get(),
                interestBalanceStore = get(interestBalanceStore),
                interestService = get(),
                tradingStore = get(),
                walletManager = get(),
                bankService = get(),
                ethMessageSigner = get(),
                limitsDataManager = get(),
                walletPrefs = get(),
                quotesEngineFactory = get(),
                analytics = get(),
                fees = get(),
                ethDataManager = get(),
                bankPartnerCallbackProvider = get(),
                userIdentity = get(),
                withdrawLocksRepository = get(),
                plaidFeatureFlag = get(plaidFeatureFlag),
                swapTransactionsStore = get(),
                stakingBalanceStore = get(stakingBalanceStore),
                stakingService = get(),
                activeRewardsBalanceStore = get(activeRewardsBalanceStore),
                activeRewardsService = get()
            )
        }

        scoped {
            AddressFactoryImpl(
                coincore = get(),
                addressResolver = get()
            )
        }.bind(AddressFactory::class)

        scoped {
            BackendNotificationUpdater(
                coinAddressesStore = get(),
                json = get()
            )
        }

        scoped {
            CoinAddressesStore(
                prefs = get(),
                walletApi = get()
            )
        }

        scoped {
            ActiveBalancesStore(
                currencyPrefs = get(),
                selfCustodyService = get()
            )
        }

        scoped {
            HistoricActiveBalancesRepository(activeBalancesStore = get())
        }

        scoped {
            DefiWalletReceiveAddressRepository(
                coincore = get(),
                assetCatalogue = get()
            )
        }.bind(DefiWalletReceiveAddressService::class)

        scoped {
            Erc20DataManagerImpl(
                ethDataManager = get(),
                historyCallCache = get()
            )
        }.bind(Erc20DataManager::class)

        factory {
            TransferQuotesEngine.Factory(
                quotesProvider = get(),
                custodialWalletManager = get(),
                brokerageDataManager = get(),
                tradeDataService = get(),
                sellSwapBrokerageQuoteFF = get(sellSwapBrokerageQuoteFeatureFlag)
            )
        }

        factory {
            GasFeeCalculator()
        }

        factory {
            LinkedBanksFactory(
                custodialWalletManager = get(),
                bankService = get(),
                paymentMethodService = get()
            )
        }

        factory {
            SwapTrendingPairsProvider(
                coincore = get(),
                walletModeService = get(),
                assetCatalogue = get()
            )
        }.bind(TrendingPairsProvider::class)

        scoped<OneTimeAccountPersistenceService> {
            OneTimeAccountPersistenceRepository
        }
    }

    single {
        AssetCatalogueImpl(
            assetsService = get(),
            assetsDataManager = get()
        )
    }.bind(AssetCatalogue::class)

    single {
        UniversalDynamicAssetRepository(
            coinNetworksStore = get(),
            otherErc20sStore = get(),
            ethErc20sStore = get(),
            l1CoinsStore = get(),
            l2sDynamicAssetRepository = get(),
            custodialAssetsStore = get()
        )
    }.bind(DynamicAssetsService::class)

    single {
        NonCustodialL2sDynamicAssetRepository(
            coinNetworksStore = get()
        )
    }.bind(EvmNetworksService::class)

    single {
        CoinNetworksStore(
            discoveryService = get()
        )
    }

    single {
        L1CoinsStore(
            discoveryService = get()
        )
    }

    single {
        CustodialAssetsStore(
            discoveryService = get()
        )
    }

    single {
        EthErc20sStore(
            discoveryService = get()
        )
    }

    single {
        OtherNetworksErc20sStore(
            discoveryService = get()
        )
    }

    factory {
        FormatUtilities()
    }
}
