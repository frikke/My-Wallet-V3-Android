package com.blockchain.coincore.loader

import com.blockchain.coincore.Asset
import com.blockchain.coincore.CoincoreInitFailure
import com.blockchain.coincore.CryptoAsset
import com.blockchain.coincore.IdentityAddressResolver
import com.blockchain.coincore.NonCustodialSupport
import com.blockchain.coincore.custodialonly.DynamicOnlyTradingAsset
import com.blockchain.coincore.erc20.Erc20Asset
import com.blockchain.coincore.evm.NetworkNativeAsset
import com.blockchain.coincore.fiat.FiatAsset
import com.blockchain.coincore.impl.EthHotWalletAddressResolver
import com.blockchain.coincore.selfcustody.DynamicSelfCustodyAsset
import com.blockchain.coincore.wrap.FormatUtilities
import com.blockchain.core.chains.dynamicselfcustody.domain.NonCustodialService
import com.blockchain.core.chains.erc20.Erc20DataManager
import com.blockchain.core.custodial.domain.TradingService
import com.blockchain.core.fees.FeeDataManager
import com.blockchain.core.payload.PayloadDataManager
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.onErrorReturn
import com.blockchain.earn.domain.service.ActiveRewardsService
import com.blockchain.earn.domain.service.InterestService
import com.blockchain.earn.domain.service.StakingService
import com.blockchain.logging.RemoteLogger
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.WalletStatusPrefs
import com.blockchain.store.mapData
import com.blockchain.unifiedcryptowallet.domain.balances.UnifiedBalancesService
import com.blockchain.utils.filterListItemIsInstance
import com.blockchain.utils.mapList
import com.blockchain.utils.zipSingles
import com.blockchain.wallet.DefaultLabels
import com.blockchain.walletmode.WalletMode
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Currency
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.isCustodialOnly
import info.blockchain.balance.isDelegatedNonCustodial
import info.blockchain.balance.isLayer2Token
import info.blockchain.balance.isNetworkNativeAsset
import info.blockchain.balance.isNonCustodial
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

// This is a rubbish regex, but it'll do until I'm provided a better one
private const val defaultCustodialAddressValidation = "[a-zA-Z0-9]{15,}"

internal class DynamicAssetLoader(
    private val nonCustodialAssets: Set<CryptoAsset>,
    private val assetCatalogue: AssetCatalogueImpl,
    private val payloadManager: PayloadDataManager,
    private val historicActiveBalancesRepository: HistoricActiveBalancesRepository,
    private val erc20DataManager: Erc20DataManager,
    private val feeDataManager: FeeDataManager,
    private val walletPreferences: WalletStatusPrefs,
    private val tradingService: TradingService,
    private val interestService: InterestService,
    private val labels: DefaultLabels,
    private val custodialWalletManager: CustodialWalletManager,
    private val remoteLogger: RemoteLogger,
    private val formatUtils: FormatUtilities,
    private val unifiedBalancesService: Lazy<UnifiedBalancesService>,
    private val identityAddressResolver: IdentityAddressResolver,
    private val ethHotWalletAddressResolver: EthHotWalletAddressResolver,
    private val selfCustodyService: NonCustodialService,
    private val stakingService: StakingService,
    private val currencyPrefs: CurrencyPrefs,
    private val activeRewardsService: ActiveRewardsService
) : AssetLoader {

    private val assetMap = mutableMapOf<Currency, Asset>()
    override operator fun get(asset: Currency): Asset =
        assetMap[asset] ?: attemptLoadAsset(asset)

    private fun attemptLoadAsset(currency: Currency): Asset {
        return currency.loadAsset()?.let {
            it.also {
                check(it.currency !in assetMap.keys) { "Asset already loaded ${currency.networkTicker}" }
                assetMap[currency] = it
            }
        } ?: throw IllegalStateException("Unknown asset type enabled: ${currency.networkTicker}")
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
        return assetCatalogue.initialise()
            .doOnSubscribe { remoteLogger.logEvent("Coincore init started") }
            .map { currencies ->
                currencies.mapNotNull { it.loadAsset() }
            }.doOnSuccess { assetList ->
                assetList.map { it.currency.networkTicker }.let { ids ->
                    check(
                        ids.size == ids.toSet().size
                    ) {
                        val assets =
                            assetList.map { it.currency.networkTicker }.groupBy { it }.filter { it.value.size > 1 }
                        assets.keys.joinToString()
                    }
                }
            }
            .flatMapCompletable { supportedAssets ->
                initNonCustodialAssets(supportedAssets).doOnComplete {
                    assetMap.apply {
                        putAll(supportedAssets.associateBy { it.currency })
                    }
                }
            }
    }

    private fun Currency.loadAsset(): Asset? {
        val dominantAsset = nonCustodialAssets.firstOrNull { it.currency.networkTicker == networkTicker }
        if (dominantAsset != null) {
            return dominantAsset
        }
        if ((this as? AssetInfo)?.isDelegatedNonCustodial == true) return loadSelfCustodialAsset(this)
        if ((this as? AssetInfo)?.isNonCustodial == true) return loadNonCustodialAsset(this)
        if ((this as? AssetInfo)?.isCustodialOnly == true) {
            return DynamicOnlyTradingAsset(
                currency = this,
                addressValidation = defaultCustodialAddressValidation
            )
        }
        if (this is FiatCurrency) {
            return FiatAsset(
                this
            )
        }
        return null
    }

    private fun loadNonCustodialAsset(assetInfo: AssetInfo): Asset? {
        if (assetInfo.isNetworkNativeAsset()) return loadNetworkNativeAsset(assetInfo)
        if (assetInfo.isLayer2Token) return loadErc20Asset(assetInfo)
        return null
    }

    /*
     * Init the local non custodial assets currently BCH and ETH that require
     * metadata initialisation.
     * We need to make sure ETH is initialised before we request the supported erc20s
     * */
    private fun initNonCustodialAssets(assetList: List<Asset>): Completable =
        assetList.filterIsInstance<NonCustodialSupport>().map { asset ->
            Single.defer { asset.initToken().toSingle { } }.doOnError {
                remoteLogger.logException(
                    CoincoreInitFailure(
                        "Failed init: ${(asset as CryptoAsset).currency.networkTicker}",
                        it
                    )
                )
            }
        }.zipSingles().subscribeOn(Schedulers.io()).ignoreElement()

    private fun loadNetworkNativeAsset(currency: AssetInfo): CryptoAsset {
        return NetworkNativeAsset(
            currency = currency,
            erc20DataManager = erc20DataManager,
            walletPreferences = walletPreferences,
            labels = labels,
            formatUtils = formatUtils,
            addressResolver = ethHotWalletAddressResolver,
            historicActiveBalancesRepository = historicActiveBalancesRepository,
            coinNetwork = currency.coinNetwork!!
        )
    }

    /**
     * We need to request:
     * - All erc20 with balance.
     * - All trading with balance.
     * - All interest with balance.
     * */

    private fun loadNonCustodialActiveAssets(freshnessStrategy: FreshnessStrategy): Flow<List<Asset>> {
        return flow {
            emitAll(loadNonCustodialAssetsUsingUnifiedBalances(freshnessStrategy))
        }
    }

    private fun loadNonCustodialAssetsUsingUnifiedBalances(freshnessStrategy: FreshnessStrategy): Flow<List<Asset>> {
        val selfCustodialAssetsFlow =
            unifiedBalancesService.value.balances(freshnessStrategy = freshnessStrategy).mapData { balances ->
                balances.map {
                    get(it.currency)
                }
            }.onErrorReturn {
                emptyList()
            }.filterIsInstance<DataResource.Data<List<Asset>>>()

        return combine(
            selfCustodialAssetsFlow,
            flowOf(nonCustodialAssets)
        ) { dynamicSelfCustodyAssets, standardAssets ->
            dynamicSelfCustodyAssets.data.filter {
                it.currency.networkTicker !in standardAssets.map { asset -> asset.currency.networkTicker }
            } + standardAssets
        }
    }

    private fun loadCustodialActiveAssets(freshnessStrategy: FreshnessStrategy): Flow<List<Asset>> {
        val activeTradingFlow = tradingService.getActiveAssets(freshnessStrategy)
            .filterListItemIsInstance<AssetInfo>()
            .mapList { loadCustodialOnlyAsset(it) }
            .catch { emit(emptyList()) }

        val activeInterestFlow = interestService.getActiveAssets(freshnessStrategy)
            .mapList { loadCustodialOnlyAsset(it) }
            .catch { emit(emptyList()) }

        val activeStakingFlow = stakingService.getActiveAssets(freshnessStrategy)
            .mapList { loadCustodialOnlyAsset(it) }
            .catch { emit(emptyList()) }

        val activeActiveRewardsFlow = activeRewardsService.getActiveAssets(freshnessStrategy)
            .mapList { loadCustodialOnlyAsset(it) }
            .catch { emit(emptyList()) }

        val supportedFiatsFlow = custodialWalletManager.getSupportedFundsFiats(freshnessStrategy = freshnessStrategy)
            .mapList { FiatAsset(currency = it) }
            .catch { emit(emptyList()) }

        return combine(
            activeTradingFlow,
            activeInterestFlow,
            supportedFiatsFlow,
            activeStakingFlow,
            activeActiveRewardsFlow
        ) { activeTrading, activeInterest, supportedFiats, activeStaking, activeActive ->
            val allActives = setOf(activeTrading, activeInterest, activeStaking, activeActive)
                .flatten()
                .distinctBy { it.currency.networkTicker }
            allActives + supportedFiats
        }
    }

    private fun loadCustodialOnlyAsset(assetInfo: AssetInfo): CryptoAsset {
        return DynamicOnlyTradingAsset(
            currency = assetInfo,
            addressValidation = defaultCustodialAddressValidation
        )
    }

    private fun loadSelfCustodialAsset(assetInfo: AssetInfo): CryptoAsset {
        return DynamicSelfCustodyAsset(
            currency = assetInfo,
            payloadManager = payloadManager,
            historicActiveBalancesRepository = historicActiveBalancesRepository,
            addressValidation = defaultCustodialAddressValidation,
            addressResolver = identityAddressResolver,
            selfCustodyService = selfCustodyService,
            walletPreferences = walletPreferences
        )
    }

    private fun loadErc20Asset(assetInfo: Currency): CryptoAsset {
        require(assetInfo is AssetInfo)
        require(assetInfo.isLayer2Token)
        return Erc20Asset(
            currency = assetInfo,
            erc20DataManager = erc20DataManager,
            feeDataManager = feeDataManager,
            labels = labels,
            historicActiveBalancesRepository = historicActiveBalancesRepository,
            currencyPrefs = currencyPrefs,
            walletPreferences = walletPreferences,
            formatUtils = formatUtils,
            addressResolver = ethHotWalletAddressResolver
        )
    }

    override fun activeAssets(walletMode: WalletMode, freshnessStrategy: FreshnessStrategy): Flow<List<Asset>> {
        return when (walletMode) {
            WalletMode.CUSTODIAL -> loadCustodialActiveAssets(freshnessStrategy)
            WalletMode.NON_CUSTODIAL -> loadNonCustodialActiveAssets(freshnessStrategy)
        }
    }

    override fun activeAssets(freshnessStrategy: FreshnessStrategy): Flow<List<Asset>> {
        return allActive(freshnessStrategy)
    }

    private fun allActive(freshnessStrategy: FreshnessStrategy): Flow<List<Asset>> {
        val nonCustodialFlow = loadNonCustodialActiveAssets(freshnessStrategy)
        val custodialFlow = loadCustodialActiveAssets(freshnessStrategy)

        return combine(nonCustodialFlow, custodialFlow) { nonCustodial, custodial ->
            // remove any asset from custodial list that already exists in non custodial
            val uniqueCustodial = custodial.filter {
                it.currency.networkTicker !in nonCustodial.map { asset -> asset.currency.networkTicker }
            }
            // merge all
            (nonCustodial.map { it.currency } + uniqueCustodial.map { it.currency })
                .map { this[it] }
        }
    }

    override val loadedAssets: List<Asset>
        get() = assetMap.values.toList()
}
