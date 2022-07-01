package com.blockchain.coincore.loader

import com.blockchain.coincore.CoincoreInitFailure
import com.blockchain.coincore.CryptoAsset
import com.blockchain.coincore.IdentityAddressResolver
import com.blockchain.coincore.NonCustodialSupport
import com.blockchain.coincore.custodialonly.DynamicOnlyTradingAsset
import com.blockchain.coincore.erc20.Erc20Asset
import com.blockchain.coincore.impl.EthHotWalletAddressResolver
import com.blockchain.coincore.selfcustody.DynamicSelfCustodyAsset
import com.blockchain.coincore.selfcustody.StxAsset
import com.blockchain.coincore.wrap.FormatUtilities
import com.blockchain.core.chains.dynamicselfcustody.NonCustodialService
import com.blockchain.core.chains.erc20.Erc20DataManager
import com.blockchain.core.chains.erc20.isErc20
import com.blockchain.core.custodial.TradingBalanceDataManager
import com.blockchain.core.interest.InterestBalanceDataManager
import com.blockchain.extensions.minus
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.logging.RemoteLogger
import com.blockchain.preferences.WalletStatus
import com.blockchain.rx.printTime
import com.blockchain.wallet.DefaultLabels
import com.blockchain.walletmode.WalletMode
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.Currency
import info.blockchain.balance.isCustodial
import info.blockchain.balance.isCustodialOnly
import info.blockchain.balance.isNonCustodial
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
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
    private val walletPreferences: WalletStatus,
    private val tradingBalances: TradingBalanceDataManager,
    private val interestBalances: InterestBalanceDataManager,
    private val labels: DefaultLabels,
    private val remoteLogger: RemoteLogger,
    private val formatUtils: FormatUtilities,
    private val identityAddressResolver: IdentityAddressResolver,
    private val ethHotWalletAddressResolver: EthHotWalletAddressResolver,
    private val selfCustodyService: NonCustodialService,
    private val layerTwoFeatureFlag: FeatureFlag,
    private val stxForAllFeatureFlag: FeatureFlag,
) : AssetLoader {

    private val custodialActiveAssets = mutableMapOf<Currency, CryptoAsset>()
    private val nonCustodialActiveAssets = mutableMapOf<Currency, CryptoAsset>()

    private val allActive: Map<Currency, CryptoAsset>
        get() = custodialActiveAssets + nonCustodialActiveAssets

    private val assetMap = mutableMapOf<Currency, CryptoAsset>()
    override operator fun get(asset: AssetInfo): CryptoAsset =
        assetMap[asset] ?: attemptLoadAsset(asset)

    private fun attemptLoadAsset(assetInfo: Currency): CryptoAsset =
        when {
            assetInfo.isErc20() -> loadErc20Asset(assetInfo)
            (assetInfo as? AssetInfo)?.isCustodialOnly == true -> loadCustodialOnlyAsset(assetInfo)
            (assetInfo as? AssetInfo)?.isNonCustodial == true -> loadSelfCustodialAsset(assetInfo)
            else -> throw IllegalStateException("Unknown asset type enabled: ${assetInfo.networkTicker}")
        }.also {
            check(!assetMap.containsKey(assetInfo)) { "Asset already loaded" }
            assetMap[assetInfo] = it
        }

    override fun initAndPreload(): Completable {
        return layerTwoFeatureFlag.enabled.flatMapCompletable { isEnabled ->
            val enabledNonCustodialAssets = if (isEnabled) {
                nonCustodialAssets
            } else {
                nonCustodialAssets.minus { cryptoAsset ->
                    experimentalL1EvmAssets.contains(cryptoAsset.assetInfo)
                }
            }
            nonCustodialActiveAssets.putAll(enabledNonCustodialAssets.associateBy { it.assetInfo })

            assetCatalogue.initialise()
                .doOnSubscribe { remoteLogger.logEvent("Coincore init started") }
                .flatMap { supportedAssets ->
                    // We need to make sure than any l1 assets - notably ETH - is initialised before
                    // create any l2s. So that things like balance calls will work
                    initNonCustodialAssets(enabledNonCustodialAssets)
                        // Do not load the non-custodial assets here otherwise they become DynamicOnlyTradingAsset
                        // and the non-custodial accounts won't show up.
                        .thenSingle {
                            doLoadAssets(
                                dynamicAssets = supportedAssets.filterIsInstance<AssetInfo>().toSet()
                            )
                        }
                }
                .map { assets ->
                    // Local are the dominants
                    val onlyDynamicAssets = assets.toMutableList().apply {
                        removeAll { asset ->
                            enabledNonCustodialAssets.map { it.assetInfo }.contains(asset.assetInfo)
                        }
                    }
                    enabledNonCustodialAssets + onlyDynamicAssets
                }
                .doOnSuccess { assetList ->
                    assetMap.putAll(assetList.associateBy { it.assetInfo })
                }
                .doOnError { Timber.e("init failed") }
                .ignoreElement()
        }
    }

    private fun initNonCustodialAssets(assetList: Set<CryptoAsset>): Completable =
        assetList.filterIsInstance<NonCustodialSupport>().map {
            Single.defer { it.initToken().toSingle { } }.doOnError {
                remoteLogger.logException(
                    CoincoreInitFailure(
                        "Failed init: ${(it as CryptoAsset).assetInfo.networkTicker}", it
                    )
                )
            }
        }.zipSingles().subscribeOn(Schedulers.io()).ignoreElement()

    private fun doLoadAssets(
        dynamicAssets: Set<AssetInfo>,
    ): Single<List<CryptoAsset>> {
        val erc20assets = dynamicAssets.filter { it.isErc20() }

        return loadErc20Assets(erc20Assets = erc20assets).flatMap { loadedErc20 ->
            // Loading Custodial ERC20s even without a balance is necessary so they show up for swap
            val custodialAssets = dynamicAssets.filter { dynamicAsset ->
                dynamicAsset.isCustodial &&
                    loadedErc20.find { erc20 -> erc20.assetInfo == dynamicAsset } == null
            }
            // Those two sets should NOT overlap
            check(loadedErc20.intersect(custodialAssets.toSet()).isEmpty())
            loadCustodialAssets(
                custodialAssets = custodialAssets
            ).map { custodialList ->
                loadedErc20 + custodialList
            }
        }
    }

    private fun loadCustodialAssets(
        custodialAssets: Iterable<AssetInfo>,
    ): Single<List<CryptoAsset>> {
        return Single.zip(
            tradingBalances.getActiveAssets().printTime("----- ::CustodialAssets - tradingBalances"),
            interestBalances.getActiveAssets().printTime("----- ::CustodialAssets - interestBalances")
        ) { activeTrading, activeInterest ->
            activeInterest + activeTrading
        }.map { activeAssets ->
            custodialAssets.map { asset ->
                val loadedAsset = when {
                    asset.isNonCustodial -> loadSelfCustodialAsset(asset)
                    else -> loadCustodialOnlyAsset(asset)
                }
                if (activeAssets.contains(asset)) {
                    custodialActiveAssets[asset] = loadedAsset
                }
                loadedAsset
            }
        }
    }

    private fun loadErc20Assets(
        erc20Assets: Iterable<AssetInfo>,
    ): Single<List<CryptoAsset>> {

        val tradingBalancesAssets = tradingBalances.getActiveAssets()
            .printTime("----- ::Erc20Assets - tradingBalances")
        val interestBalancesAssets = interestBalances.getActiveAssets()
            .printTime("----- ::Erc20Assets - interestBalances")

        // Assets with non custodial balance
        val erc20ActiveAssets = erc20DataManager.getActiveAssets()
            .printTime("----- ::Erc20Assets - erc20DataManager")

        return Single.zip(
            tradingBalancesAssets,
            interestBalancesAssets,
            erc20ActiveAssets
        ) { activeTrading, activeInterest, activeNoncustodial ->
            Triple(activeInterest, activeTrading, activeNoncustodial)
        }
            .doOnSuccess { (activeTrading, activeInterest, activeNoncustodial) ->
                (activeInterest + activeTrading).filter { erc20Assets.contains(it) }.forEach { currency ->
                    custodialActiveAssets[currency] = loadErc20Asset(currency)
                }
                activeNoncustodial.filter { erc20Assets.contains(it) }.forEach { currency ->
                    nonCustodialActiveAssets[currency] = loadErc20Asset(currency)
                }
            }.map { (activeTrading, activeInterest, activeNoncustodial) ->
                val allAssets = activeTrading + activeInterest + activeNoncustodial
                // Always load the fully supported ERC20s
                val erc20WithFullSupport = erc20Assets.filter { dynamicAsset ->
                    dynamicAsset.isNonCustodial &&
                        dynamicAsset.isCustodial
                }
                allAssets + erc20WithFullSupport
            }.map { activeAndFullSupportAssets ->
                erc20Assets.filter { activeAndFullSupportAssets.contains(it) }
            }.map { activeSupportedAssets ->
                activeSupportedAssets.map { asset ->
                    loadErc20Asset(asset)
                }
            }
    }

    private fun loadCustodialOnlyAsset(assetInfo: AssetInfo): CryptoAsset {
        return DynamicOnlyTradingAsset(
            assetInfo = assetInfo,
            addressValidation = defaultCustodialAddressValidation
        )
    }

    private fun loadSelfCustodialAsset(assetInfo: AssetInfo): CryptoAsset {
        // TODO(dtverdota): Remove Stx-specific code once it is enabled for all users
        return if (assetInfo.networkTicker == "STX") {
            StxAsset(
                assetInfo = assetInfo,
                payloadManager = payloadManager,
                addressValidation = defaultCustodialAddressValidation,
                addressResolver = identityAddressResolver,
                selfCustodyService = selfCustodyService,
                stxForAllFeatureFlag = stxForAllFeatureFlag
            )
        } else {
            DynamicSelfCustodyAsset(
                assetInfo = assetInfo
            )
        }
    }

    private fun loadErc20Asset(assetInfo: Currency): CryptoAsset {
        require(assetInfo is AssetInfo)
        require(assetInfo.isErc20())
        return Erc20Asset(
            assetInfo = assetInfo,
            erc20DataManager = erc20DataManager,
            feeDataManager = feeDataManager,
            labels = labels,
            walletPreferences = walletPreferences,
            formatUtils = formatUtils,
            addressResolver = ethHotWalletAddressResolver,
            layerTwoFeatureFlag = layerTwoFeatureFlag
        )
    }

    override fun activeAssets(walletMode: WalletMode): List<CryptoAsset> = when (walletMode) {
        WalletMode.CUSTODIAL_ONLY -> custodialActiveAssets
        WalletMode.NON_CUSTODIAL_ONLY -> nonCustodialActiveAssets
        WalletMode.UNIVERSAL -> allActive
    }.values.toList()

    override val loadedAssets: List<CryptoAsset>
        get() = assetMap.values.toList()
}
