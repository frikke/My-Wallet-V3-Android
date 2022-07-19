package com.blockchain.coincore

import com.blockchain.coincore.bch.BchAsset
import com.blockchain.coincore.btc.BtcAsset
import com.blockchain.coincore.eth.EthAsset
import com.blockchain.coincore.evm.MaticAsset
import com.blockchain.coincore.fiat.LinkedBanksFactory
import com.blockchain.coincore.impl.BackendNotificationUpdater
import com.blockchain.coincore.impl.EthHotWalletAddressResolver
import com.blockchain.coincore.impl.HotWalletService
import com.blockchain.coincore.impl.TxProcessorFactory
import com.blockchain.coincore.impl.txEngine.TransferQuotesEngine
import com.blockchain.coincore.loader.AssetCatalogueImpl
import com.blockchain.coincore.loader.AssetLoader
import com.blockchain.coincore.loader.DynamicAssetLoader
import com.blockchain.coincore.loader.DynamicAssetsService
import com.blockchain.coincore.loader.NonCustodialL2sDynamicAssetRepository
import com.blockchain.coincore.loader.UniversalDynamicAssetRepository
import com.blockchain.coincore.wrap.FormatUtilities
import com.blockchain.coincore.xlm.XlmAsset
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.koin.ethLayerTwoFeatureFlag
import com.blockchain.koin.experimentalL1EvmAssetList
import com.blockchain.koin.payloadScope
import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.koin.plaidFeatureFlag
import com.blockchain.koin.stxForAllFeatureFlag
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import org.koin.dsl.bind
import org.koin.dsl.module

val coincoreModule = module {

    scope(payloadScopeQualifier) {

        scoped {
            BtcAsset(
                payloadManager = get(),
                sendDataManager = get(),
                feeDataManager = get(),
                coinsWebsocket = get(),
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
                addressResolver = get()
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
                assetCatalogue = lazy { get() },
                formatUtils = get(),
                addressResolver = get()
            )
        }.bind(CryptoAsset::class)

        scoped {
            MaticAsset(
                ethDataManager = get(),
                erc20DataManager = get(),
                feeDataManager = get(),
                walletPreferences = get(),
                labels = get(),
                formatUtils = get(),
                addressResolver = get(),
                layerTwoFeatureFlag = get(ethLayerTwoFeatureFlag)
            )
        }.bind(CryptoAsset::class)

        scoped {
            val flag: FeatureFlag = get(ethLayerTwoFeatureFlag)
            val ncAssetList = if (flag.isEnabled) {
                emptyList<AssetInfo>()
            } else {
                experimentalL1EvmAssetList()
            }
            Coincore(
                assetCatalogue = get(),
                payloadManager = get(),
                assetLoader = get(),
                txProcessorFactory = get(),
                defaultLabels = get(),
                remoteLogger = get(),
                bankService = get(),
                walletModeService = get(),
                currencyPrefs = get(),
                disabledEvmAssets = ncAssetList.toList(),
                custodialWalletManager = get(),
                exchangeRateDataManager = get(),
                tradingBalances = get()
            )
        }

        scoped {
            val ncAssets: List<CryptoAsset> = payloadScope.getAll()

            // For some unknown reason `getAll()` adds the last element twice. Which means
            // that last element calls init() twice. So make it a set, to remove any duplicates.
            DynamicAssetLoader(
                nonCustodialAssets = ncAssets.toSet(), // All the non custodial L1s that we support
                experimentalL1EvmAssets = experimentalL1EvmAssetList(), // Only Matic ATM
                assetCatalogue = get(),
                payloadManager = get(),
                erc20DataManager = get(),
                feeDataManager = get(),
                tradingBalances = get(),
                interestService = get(),
                remoteLogger = get(),
                labels = get(),
                walletPreferences = get(),
                formatUtils = get(),
                identityAddressResolver = get(),
                selfCustodyService = get(),
                ethHotWalletAddressResolver = get(),
                layerTwoFeatureFlag = get(ethLayerTwoFeatureFlag),
                stxForAllFeatureFlag = get(stxForAllFeatureFlag)
            )
        }.bind(AssetLoader::class)

        scoped {
            HotWalletService(
                walletApi = get()
            )
        }

        scoped {
            IdentityAddressResolver()
        }

        scoped {
            EthHotWalletAddressResolver(
                hotWalletService = get()
            )
        }

        scoped {
            TxProcessorFactory(
                bitPayManager = get(),
                exchangeRates = get(),
                interestStore = get(),
                tradingDataSource = get(),
                walletManager = get(),
                bankService = get(),
                ethMessageSigner = get(),
                limitsDataManager = get(),
                walletPrefs = get(),
                quotesEngine = get(),
                analytics = get(),
                fees = get(),
                ethDataManager = get(),
                bankPartnerCallbackProvider = get(),
                userIdentity = get(),
                withdrawLocksRepository = get(),
                plaidFeatureFlag = get(plaidFeatureFlag),
                swapTransactionsCache = get()
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
                prefs = get(),
                walletApi = get(),
                json = get(),
            )
        }

        factory {
            TransferQuotesEngine(quotesProvider = get())
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
                assetCatalogue = get()
            )
        }.bind(TrendingPairsProvider::class)
    }

    single {
        AssetCatalogueImpl(
            assetsService = get(),
            assetsDataManager = get()
        )
    }.bind(AssetCatalogue::class)

    single {
        UniversalDynamicAssetRepository(
            l1EvmAssets = experimentalL1EvmAssetList(),
            discoveryService = get(),
            l2sDynamicAssetRepository = get()
        )
    }.bind(DynamicAssetsService::class)

    single {
        NonCustodialL2sDynamicAssetRepository(
            l1EvmAssets = experimentalL1EvmAssetList(),
            discoveryService = get(),
            layerTwoFeatureFlag = lazy { get(ethLayerTwoFeatureFlag) }
        )
    }

    factory {
        FormatUtilities()
    }
}
