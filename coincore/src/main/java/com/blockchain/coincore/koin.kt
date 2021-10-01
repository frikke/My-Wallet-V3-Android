package com.blockchain.coincore

import com.blockchain.koin.payloadScope
import com.blockchain.koin.payloadScopeQualifier
import info.blockchain.balance.AssetCatalogue
import org.koin.dsl.bind
import org.koin.dsl.module
import com.blockchain.coincore.bch.BchAsset
import com.blockchain.coincore.btc.BtcAsset
import com.blockchain.coincore.eth.EthAsset
import com.blockchain.coincore.fiat.FiatAsset
import com.blockchain.coincore.fiat.LinkedBanksFactory
import com.blockchain.coincore.loader.AssetLoader
import com.blockchain.coincore.impl.BackendNotificationUpdater
import com.blockchain.coincore.impl.TxProcessorFactory
import com.blockchain.coincore.impl.txEngine.TransferQuotesEngine
import com.blockchain.coincore.loader.AssetCatalogueImpl
import com.blockchain.coincore.loader.AssetLoaderSwitcher
import com.blockchain.coincore.loader.AssetRemoteFeatureLookup
import com.blockchain.coincore.loader.DynamicAssetLoader
import com.blockchain.coincore.loader.StaticAssetLoader
import com.blockchain.coincore.xlm.XlmAsset

val coincoreModule = module {

    scope(payloadScopeQualifier) {

        scoped {
            BtcAsset(
                exchangeRates = get(),
                sendDataManager = get(),
                feeDataManager = get(),
                currencyPrefs = get(),
                payloadManager = get(),
                custodialManager = get(),
                tradingBalances = get(),
                interestBalances = get(),
                pitLinking = get(),
                crashLogger = get(),
                labels = get(),
                walletPreferences = get(),
                notificationUpdater = get(),
                coinsWebsocket = get(),
                identity = get(),
                features = get()
            )
        }.bind(CryptoAsset::class)

        scoped {
            BchAsset(
                payloadManager = get(),
                bchDataManager = get(),
                exchangeRates = get(),
                currencyPrefs = get(),
                crashLogger = get(),
                custodialManager = get(),
                interestBalances = get(),
                tradingBalances = get(),
                feeDataManager = get(),
                sendDataManager = get(),
                pitLinking = get(),
                labels = get(),
                walletPreferences = get(),
                beNotifyUpdate = get(),
                identity = get(),
                features = get()
            )
        }.bind(CryptoAsset::class)

        scoped {
            XlmAsset(
                payloadManager = get(),
                xlmDataManager = get(),
                xlmFeesFetcher = get(),
                walletOptionsDataManager = get(),
                exchangeRates = get(),
                currencyPrefs = get(),
                custodialManager = get(),
                tradingBalances = get(),
                interestBalances = get(),
                pitLinking = get(),
                crashLogger = get(),
                labels = get(),
                walletPreferences = get(),
                identity = get(),
                features = get()
            )
        }.bind(CryptoAsset::class)

        scoped {
            EthAsset(
                payloadManager = get(),
                ethDataManager = get(),
                feeDataManager = get(),
                exchangeRates = get(),
                currencyPrefs = get(),
                walletPrefs = get(),
                crashLogger = get(),
                custodialManager = get(),
                tradingBalances = get(),
                interestBalances = get(),
                pitLinking = get(),
                labels = get(),
                notificationUpdater = get(),
                identity = get(),
                features = get(),
                assetCatalogue = lazy { get() }
            )
        }.bind(CryptoAsset::class)

        scoped {
            FiatAsset(
                labels = get(),
                tradingBalanceDataManager = get(),
                exchangeRateDataManager = get(),
                custodialWalletManager = get(),
                currencyPrefs = get()
            )
        }

        scoped {
            Coincore(
                assetCatalogue = get(),
                payloadManager = get(),
                fiatAsset = get<FiatAsset>(),
                assetLoader = get(),
                txProcessorFactory = get(),
                defaultLabels = get(),
                crashLogger = get()
            )
        }

        scoped {
            AssetLoaderSwitcher(
                features = get(),
                staticLoader = get(),
                dynamicLoader = get()
            )
        }.bind(AssetLoader::class)

        scoped {
            val ncAssets: List<CryptoAsset> = payloadScope.getAll()
            DynamicAssetLoader(
                nonCustodialAssets = ncAssets,
                assetCatalogue = get(),
                featureConfig = get(),
                payloadManager = get(),
                erc20DataManager = get(),
                feeDataManager = get(),
                exchangeRates = get(),
                currencyPrefs = get(),
                custodialManager = get(),
                tradingBalances = get(),
                interestBalances = get(),
                crashLogger = get(),
                labels = get(),
                pitLinking = get(),
                walletPreferences = get(),
                identity = get(),
                features = get()
            )
        }

        scoped {
            val ncAssets: List<CryptoAsset> = payloadScope.getAll()
            StaticAssetLoader(
                nonCustodialAssets = ncAssets,
                assetCatalogue = get(),
                featureConfig = get(),
                payloadManager = get(),
                erc20DataManager = get(),
                feeDataManager = get(),
                exchangeRates = get(),
                currencyPrefs = get(),
                custodialManager = get(),
                tradingBalances = get(),
                interestBalances = get(),
                crashLogger = get(),
                labels = get(),
                pitLinking = get(),
                walletPreferences = get(),
                identity = get(),
                features = get()
            )
        }

        scoped {
            TxProcessorFactory(
                bitPayManager = get(),
                exchangeRates = get(),
                interestBalances = get(),
                walletManager = get(),
                walletPrefs = get(),
                quotesEngine = get(),
                analytics = get(),
                bankPartnerCallbackProvider = get(),
                kycTierService = get()
            )
        }

        scoped {
            AddressFactoryImpl(
                coincore = get(),
                addressResolver = get(),
                features = get()
            )
        }.bind(AddressFactory::class)

        scoped {
            BackendNotificationUpdater(
                prefs = get(),
                walletApi = get()
            )
        }

        factory {
            TransferQuotesEngine(quotesProvider = get())
        }

        factory {
            LinkedBanksFactory(
                custodialWalletManager = get()
            )
        }

        factory {
            SwapTrendingPairsProvider(
                coincore = get(),
                assetCatalogue = get(),
                identity = get()
            )
        }.bind(TrendingPairsProvider::class)
    }

    single {
        AssetRemoteFeatureLookup(
            remoteConfig = get()
        )
    }

    single {
        AssetCatalogueImpl(
            featureConfig = get(),
            assetsDataManager = get()
        )
    }.bind(AssetCatalogue::class)
}