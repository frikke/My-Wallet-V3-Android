package com.blockchain.coincore.loader

import com.blockchain.coincore.Asset
import com.blockchain.coincore.CoincoreInitFailure
import com.blockchain.coincore.CryptoAsset
import com.blockchain.coincore.IdentityAddressResolver
import com.blockchain.coincore.NonCustodialSupport
import com.blockchain.coincore.bch.BchAsset
import com.blockchain.coincore.btc.BtcAsset
import com.blockchain.coincore.custodialonly.DynamicOnlyTradingAsset
import com.blockchain.coincore.erc20.Erc20Asset
import com.blockchain.coincore.eth.EthAsset
import com.blockchain.coincore.evm.MaticAsset
import com.blockchain.coincore.fiat.FiatAsset
import com.blockchain.coincore.impl.EthHotWalletAddressResolver
import com.blockchain.coincore.selfcustody.DynamicSelfCustodyAsset
import com.blockchain.coincore.selfcustody.StxAsset
import com.blockchain.coincore.wrap.FormatUtilities
import com.blockchain.coincore.xlm.XlmAsset
import com.blockchain.core.chains.dynamicselfcustody.NonCustodialService
import com.blockchain.core.chains.erc20.Erc20DataManager
import com.blockchain.core.chains.erc20.isErc20
import com.blockchain.core.custodial.TradingBalanceDataManager
import com.blockchain.core.interest.InterestBalanceDataManager
import com.blockchain.extensions.minus
import com.blockchain.extensions.replace
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.logging.RemoteLogger
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.outcome.getOrDefault
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.WalletStatusPrefs
import com.blockchain.rx.printTime
import com.blockchain.wallet.DefaultLabels
import com.blockchain.walletmode.WalletMode
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.Currency
import info.blockchain.balance.isCustodialOnly
import info.blockchain.balance.isNonCustodial
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.rx3.await
import kotlinx.coroutines.rx3.rxSingle
import piuk.blockchain.androidcore.data.fees.FeeDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.utils.extensions.thenSingle
import piuk.blockchain.androidcore.utils.extensions.zipSingles
import timber.log.Timber

// This is a rubbish regex, but it'll do until I'm provided a better one
private const val defaultCustodialAddressValidation = "[a-zA-Z0-9]{15,}"

internal class DynamicAssetLoader(
    private val nonCustodialAssets: Set<CryptoAsset>,
    private val experimentalL1EvmAssets: Set<CryptoCurrency>,
    private val assetCatalogue: AssetCatalogueImpl,
    private val payloadManager: PayloadDataManager,
    private val erc20DataManager: Erc20DataManager,
    private val feeDataManager: FeeDataManager,
    private val walletPreferences: WalletStatusPrefs,
    private val tradingBalances: TradingBalanceDataManager,
    private val interestBalances: InterestBalanceDataManager,
    private val labels: DefaultLabels,
    private val custodialWalletManager: CustodialWalletManager,
    private val currencyPrefs: CurrencyPrefs,
    private val remoteLogger: RemoteLogger,
    private val formatUtils: FormatUtilities,
    private val identityAddressResolver: IdentityAddressResolver,
    private val ethHotWalletAddressResolver: EthHotWalletAddressResolver,
    private val selfCustodyService: NonCustodialService,
    private val layerTwoFeatureFlag: FeatureFlag,
    private val stxForAllFeatureFlag: FeatureFlag,
) : AssetLoader {

    private val _custodialActiveCurrencies = mutableSetOf<Currency>()
    private val _nonCustodialActiveCurrencies = mutableSetOf<Currency>()

    private val nonCustodialActiveAssets: Map<Currency, Asset>
        get() = assetMap.filterKeys { _nonCustodialActiveCurrencies.contains(it) }

    private val custodialActiveAssets: Map<Currency, Asset>
        get() = assetMap.filterKeys { _custodialActiveCurrencies.contains(it) }

    private val allActive: Map<Currency, Asset>
        get() = custodialActiveAssets + nonCustodialActiveAssets

    private val assetMap = mutableMapOf<Currency, Asset>()
    override operator fun get(asset: Currency): Asset =
        assetMap[asset] ?: attemptLoadAsset(asset)

    private fun attemptLoadAsset(assetInfo: Currency): Asset =
        when {
            assetInfo.isErc20() -> loadErc20Asset(assetInfo)
            (assetInfo as? AssetInfo)?.isCustodialOnly == true -> loadCustodialOnlyAsset(assetInfo)
            (assetInfo as? AssetInfo)?.isNonCustodial == true -> loadSelfCustodialAsset(assetInfo)
            else -> throw IllegalStateException("Unknown asset type enabled: ${assetInfo.networkTicker}")
        }.also {
            check(!assetMap.containsKey(assetInfo)) { "Asset already loaded" }
            assetMap[assetInfo] = it
        }

    private val standardL1Assets: Single<Set<CryptoAsset>>
        get() = layerTwoFeatureFlag.enabled.map { isEnabled ->
            if (isEnabled) {
                nonCustodialAssets
            } else {
                nonCustodialAssets.minus { cryptoAsset ->
                    experimentalL1EvmAssets.contains(cryptoAsset.currency)
                }
            }
        }

    /*
    * This methods responsibility is discover and persist the available assets that the application uses
    * More specifically after it discovers it persists two sets of currencies (Assets). The first list
    * consists of all the available assets that the app supports and the second persists the active assets of our
    * application. As active we consider any of the following assets:
    * - All the big standard PKWs L1s that are currently hardcoded in the App [BTC,BCH,ETH,XLM,MATIC]
    * - All the PKW erc20s that user has a balance [erc20Datamanager.getActive()]
    * - All custodial currencies that user has a trading OR an interest balance
    * - All PKW subscriptions [selfCustodyService.getSubscriptions()]
    * Every asset loads the corresponding accounts, based on what it supports.
    * */
    override fun initAndPreload(): Completable {
        return standardL1Assets.doOnSuccess { enabledNonCustodialAssets ->
            _nonCustodialActiveCurrencies.addAll(enabledNonCustodialAssets.map { it.currency })
        }.flatMapCompletable { enabledNonCustodialAssets ->
            assetCatalogue.initialise()
                .doOnSubscribe { remoteLogger.logEvent("Coincore init started") }
                .flatMap { supportedAssets ->
                    initNonCustodialAssets(enabledNonCustodialAssets)
                        .thenSingle {
                            doLoadAssets(
                                supportedAssets = supportedAssets.filterIsInstance<AssetInfo>().toSet()
                            )
                        }
                }
                .map { assets ->
                    /*
                    * Now we need to keep only 1 type of every asset.
                    * The order is:
                    * 1.XXXAsset the local ones
                    * 2.Erc20 Asset
                    * 3.DynamicSelfCustody asset
                    * 4.CustodialAsset
                    *
                    * If the same currency belongs to more than 1, then we need to keep the more dominant
                    * */
                    (assets + enabledNonCustodialAssets).keepStrongest()
                }.flatMap { allCryptos ->
                    initSupportedFiatAssets().doOnSuccess {
                        _custodialActiveCurrencies.addAll(it.map { asset -> asset.currency })
                    }.map { allFiats ->
                        allCryptos + allFiats
                    }
                }
                .doOnSuccess { assetList ->
                    assetMap.putAll(assetList.associateBy { it.currency })
                }
                .doOnError { Timber.e("init failed") }
                .ignoreElement()
        }
    }

    private fun initSupportedFiatAssets(): Single<List<FiatAsset>> =
        custodialWalletManager.getSupportedFundsFiats(currencyPrefs.selectedFiatCurrency)
            .map { supportedFiatCurrencies ->
                supportedFiatCurrencies.map { fiatCurrency ->
                    FiatAsset(
                        currency = fiatCurrency
                    )
                }
            }

    /*
    * Init the local non custodial assets currently BCH and ETH that require
    * metadata initialisation.
    * We need to make sure ETH is initialised before we request the supported erc20s
    * */
    private fun initNonCustodialAssets(assetList: Set<CryptoAsset>): Completable =
        assetList.filterIsInstance<NonCustodialSupport>().map {
            Single.defer { it.initToken().toSingle { } }.doOnError {
                remoteLogger.logException(
                    CoincoreInitFailure(
                        "Failed init: ${(it as CryptoAsset).currency.networkTicker}", it
                    )
                )
            }
        }.zipSingles().subscribeOn(Schedulers.io()).ignoreElement()

    private fun loadSelfCustodialAssets(allSupportedAssets: Set<Currency>): Single<List<CryptoAsset>> {
        return rxSingle {
            if (stxForAllFeatureFlag.isEnabled) {
                val subscriptions = selfCustodyService.getSubscriptions().getOrDefault(emptyList())
                // map the supported only subscriptions to Dynamic asset.
                allSupportedAssets.filterIsInstance<AssetInfo>()
                    .filter { it.isNonCustodial && subscriptions.contains(it.networkTicker) }
                    .map { assetInfo ->
                        loadSelfCustodialAsset(assetInfo)
                    }
            } else {
                emptyList()
            }
        }
    }

    /*
    * We need to request:
    * - All erc20 with balance.
    * - All trading with balance.
    * - All interest with balance.
    *
    * */

    private fun doLoadAssets(
        supportedAssets: Set<AssetInfo>,
    ): Single<List<CryptoAsset>> {

        val activePKWErc20s =
            erc20DataManager.getActiveAssets(false).map { assets ->
                assets.filter {
                    it.isErc20() &&
                        supportedAssets.contains(it)
                }
            }.doOnSuccess { _nonCustodialActiveCurrencies.addAll(it) }
                .map { assets -> assets.map { loadErc20Asset(it) } }
                .printTime("----- ::Erc20Assets - erc20DataManager")
        val activeTrading =
            tradingBalances.getActiveAssets(false).map { assets -> assets.filter { supportedAssets.contains(it) } }
                .doOnSuccess { _custodialActiveCurrencies.addAll(it) }
                .map { assets -> assets.filterIsInstance<AssetInfo>().map { loadCustodialOnlyAsset(it) } }
                .printTime("----- ::CustodialAssets - tradingBalances")

        val activeInterest =
            interestBalances.getActiveAssets(false).map { assets -> assets.filter { supportedAssets.contains(it) } }
                .doOnSuccess { _custodialActiveCurrencies.addAll(it) }
                .map { assets -> assets.map { loadCustodialOnlyAsset(it) } }
                .printTime("----- ::CustodialAssets - interestBalances")

        val dynamicSelfCustodyAssets = loadSelfCustodialAssets(supportedAssets).doOnSuccess {
            _nonCustodialActiveCurrencies.addAll(it.map { cryptoAsset -> cryptoAsset.currency })
        }

        return Single.zip(activePKWErc20s, activeTrading, activeInterest, dynamicSelfCustodyAssets) { a, b, c, d ->
            a + b + c + d
        }
    }

    private fun loadCustodialOnlyAsset(assetInfo: AssetInfo): CryptoAsset {
        return DynamicOnlyTradingAsset(
            currency = assetInfo,
            addressValidation = defaultCustodialAddressValidation
        )
    }

    private fun loadSelfCustodialAsset(assetInfo: AssetInfo): CryptoAsset {
        // TODO(dtverdota): Remove Stx-specific code once it is enabled for all users
        return if (assetInfo.networkTicker == "STX") {
            StxAsset(
                currency = assetInfo,
                payloadManager = payloadManager,
                addressValidation = defaultCustodialAddressValidation,
                addressResolver = identityAddressResolver,
                selfCustodyService = selfCustodyService,
                stxForAllFeatureFlag = stxForAllFeatureFlag,
                walletPreferences = walletPreferences
            )
        } else {
            DynamicSelfCustodyAsset(
                currency = assetInfo
            )
        }
    }

    private fun loadErc20Asset(assetInfo: Currency): CryptoAsset {
        require(assetInfo is AssetInfo)
        require(assetInfo.isErc20())
        return Erc20Asset(
            currency = assetInfo,
            erc20DataManager = erc20DataManager,
            feeDataManager = feeDataManager,
            labels = labels,
            walletPreferences = walletPreferences,
            formatUtils = formatUtils,
            addressResolver = ethHotWalletAddressResolver,
            layerTwoFeatureFlag = layerTwoFeatureFlag
        )
    }

    override fun activeAssets(walletMode: WalletMode): List<Asset> = when (walletMode) {
        WalletMode.CUSTODIAL_ONLY -> custodialActiveAssets
        WalletMode.NON_CUSTODIAL_ONLY -> nonCustodialActiveAssets
        WalletMode.UNIVERSAL -> allActive
    }.values.toList()

    override fun reactiveActiveAssets(walletMode: WalletMode): Flow<List<Asset>> {
        return when (walletMode) {
            WalletMode.CUSTODIAL_ONLY -> flow {
                emit(custodialActiveAssets.values.toList())
                /* emit(getFreshCustodialActiveAssets())*/
            }
            WalletMode.NON_CUSTODIAL_ONLY -> flow {
                emit(nonCustodialActiveAssets.values.toList())
                /*                emit(freshNonCustodialAssets())*/
            }
            WalletMode.UNIVERSAL -> flow { emit(allActive.values.toList()) }
        }
    }

    private suspend fun freshNonCustodialAssets(): List<Asset> {
        val standardL1s = standardL1Assets.await()
        // Assets with non custodial balance
        val erc20ActiveAssets = erc20DataManager.getActiveAssets(true).map {
            it.mapNotNull { assetInfo ->
                assetMap[assetInfo]
            }
        }.await()
        val selfCustodial = loadSelfCustodialAssets(assetMap.keys.filterIsInstance<AssetInfo>().toSet()).await()
        return (standardL1s + erc20ActiveAssets + selfCustodial).toList()
    }

    private suspend fun getFreshCustodialActiveAssets(): List<Asset> {
        val trading = tradingBalances.getActiveAssets(true).await()
        val interest = interestBalances.getActiveAssets(true).await()
        val tradingAndInterest = trading + interest
        return tradingAndInterest.mapNotNull {
            assetMap[it]
        }
    }

    override val loadedAssets: List<Asset>
        get() = assetMap.values.toList()
}

private fun List<CryptoAsset>.keepStrongest(): List<CryptoAsset> {
    var strongest = listOf<CryptoAsset>()
    this.forEach { asset ->
        if (!strongest.containsAsset(asset.currency)) {
            strongest = strongest.plus(asset)
        } else if (asset.strength() > strongest.first { it.currency == asset.currency }.strength()) {
            strongest = strongest.replace(strongest.first { it.currency == asset.currency }, asset)
        }
    }
    return strongest
}

/**
 * TODO(antonis-bc) Remove the hardcoded assets
 */
private fun Asset.strength(): Int =
    when (this) {
        is BtcAsset -> Int.MAX_VALUE
        is EthAsset -> Int.MAX_VALUE
        is XlmAsset -> Int.MAX_VALUE
        is BchAsset -> Int.MAX_VALUE
        is MaticAsset -> Int.MAX_VALUE
        is StxAsset -> Int.MAX_VALUE
        is Erc20Asset -> Int.MAX_VALUE
        is FiatAsset -> Int.MAX_VALUE
        is DynamicSelfCustodyAsset -> Int.MAX_VALUE - 1
        is DynamicOnlyTradingAsset -> Int.MAX_VALUE - 2
        // TODO(antonis-bc) after fiat asset pr Make Asset sealed and put all here.
        else -> throw IllegalStateException("Undefined asset type $this")
    }

private fun List<CryptoAsset>.containsAsset(assetInfo: AssetInfo): Boolean =
    this.any { it.currency == assetInfo }
