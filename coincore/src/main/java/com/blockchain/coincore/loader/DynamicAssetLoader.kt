package com.blockchain.coincore.loader

import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.CoincoreInitFailure
import com.blockchain.coincore.CryptoAsset
import com.blockchain.coincore.NonCustodialSupport
import com.blockchain.coincore.custodialonly.DynamicOnlyTradingAsset
import com.blockchain.coincore.erc20.Erc20Asset
import com.blockchain.coincore.impl.EthHotWalletAddressResolver
import com.blockchain.coincore.selfcustody.DynamicSelfCustodyAsset
import com.blockchain.coincore.selfcustody.StxAsset
import com.blockchain.coincore.wrap.FormatUtilities
import com.blockchain.core.chains.erc20.Erc20DataManager
import com.blockchain.core.chains.erc20.isErc20
import com.blockchain.core.custodial.TradingBalanceDataManager
import com.blockchain.core.interest.InterestBalanceDataManager
import com.blockchain.extensions.minus
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.logging.RemoteLogger
import com.blockchain.preferences.WalletStatus
import com.blockchain.wallet.DefaultLabels
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.Currency
import info.blockchain.balance.isCustodial
import info.blockchain.balance.isCustodialOnly
import info.blockchain.balance.isNonCustodial
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import java.lang.IllegalStateException
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
    private val walletModeService: WalletModeService,
    private val payloadManager: PayloadDataManager,
    private val erc20DataManager: Erc20DataManager,
    private val feeDataManager: FeeDataManager,
    private val walletPreferences: WalletStatus,
    private val tradingBalances: TradingBalanceDataManager,
    private val interestBalances: InterestBalanceDataManager,
    private val labels: DefaultLabels,
    private val remoteLogger: RemoteLogger,
    private val formatUtils: FormatUtilities,
    private val ethHotWalletAddressResolver: EthHotWalletAddressResolver,
    private val layerTwoFeatureFlag: FeatureFlag,
    private val stxForAllFeatureFlag: FeatureFlag,
    private val stxForAirdropFeatureFlag: FeatureFlag
) : AssetLoader {

    private val activeAssetMap = mutableMapOf<Currency, CryptoAsset>()
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
            if (walletModeService.enabledWalletMode().nonCustodialEnabled) {
                activeAssetMap.putAll(enabledNonCustodialAssets.associateBy { it.assetInfo })
            }

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
                                supportedAssets.filterIsInstance<AssetInfo>().toSet()
                                    .minus(activeAssetMap.values.map { it.assetInfo }.toSet())
                            )
                        }
                }
                .map { enabledNonCustodialAssets + it }
                .doOnSuccess { assetList -> assetMap.putAll(assetList.associateBy { it.assetInfo }) }
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
        dynamicAssets: Set<AssetInfo>
    ): Single<List<CryptoAsset>> {
        val erc20assets = dynamicAssets.filter { it.isErc20() }

        return loadErc20Assets(erc20assets).flatMap { loadedErc20 ->
            // Loading Custodial ERC20s even without a balance is necessary so they show up for swap
            val custodialAssets = dynamicAssets.filter { dynamicAsset ->
                dynamicAsset.isCustodial &&
                    loadedErc20.find { erc20 -> erc20.assetInfo == dynamicAsset } == null
            }
            // Those two sets should NOT overlap
            check(loadedErc20.intersect(custodialAssets.toSet()).isEmpty())
            loadCustodialOnlyAssets(custodialAssets).map { custodialList ->
                loadedErc20 + custodialList
            }
        }
    }

    private fun loadCustodialOnlyAssets(
        custodialAssets: Iterable<AssetInfo>
    ): Single<List<CryptoAsset>> {
        if (walletModeService.enabledWalletMode().custodialEnabled.not()) {
            return Single.just(emptyList())
        }
        return Single.zip(
            tradingBalances.getActiveAssets(),
            interestBalances.getActiveAssets()
        ) { activeTrading, activeInterest ->
            activeInterest + activeTrading
        }.map { activeAssets ->
            custodialAssets.map { asset ->
                val loadedAsset = loadCustodialOnlyAsset(asset)
                if (activeAssets.contains(asset)) {
                    activeAssetMap[asset] = loadedAsset
                }
                loadedAsset
            }
        }
    }

    private fun loadErc20Assets(
        erc20Assets: Iterable<AssetInfo>
    ): Single<List<CryptoAsset>> {

        val tradingBalancesAssets =
            tradingBalances.getActiveAssets()
        val interestBalancesAssets =
            interestBalances.getActiveAssets()

        // Assets with non custodial balance
        val erc20ActiveAssets =
            erc20DataManager.getActiveAssets()

        return Single.zip(
            tradingBalancesAssets,
            interestBalancesAssets,
            erc20ActiveAssets
        ) { activeTrading, activeInterest, activeNoncustodial ->
            Triple(activeInterest, activeTrading, activeNoncustodial)
        }
            .doOnSuccess { (activeTrading, activeInterest, activeNoncustodial) ->
                val potentialActiveAssets = when (walletModeService.enabledWalletMode()) {
                    WalletMode.NON_CUSTODIAL_ONLY -> activeNoncustodial
                    WalletMode.CUSTODIAL_ONLY -> activeInterest + activeTrading
                    WalletMode.UNIVERSAL -> activeTrading + activeInterest + activeNoncustodial
                }
                potentialActiveAssets.filter { erc20Assets.contains(it) }.forEach { currency ->
                    activeAssetMap[currency] = loadErc20Asset(currency)
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
                addressValidation = defaultCustodialAddressValidation,
                stxForAllFeatureFlag = stxForAllFeatureFlag,
                stxForAirdropFeatureFlag = stxForAirdropFeatureFlag
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
            payloadManager = payloadManager,
            erc20DataManager = erc20DataManager,
            feeDataManager = feeDataManager,
            labels = labels,
            walletPreferences = walletPreferences,
            availableNonCustodialActions = assetActions,
            formatUtils = formatUtils,
            addressResolver = ethHotWalletAddressResolver,
            layerTwoFeatureFlag = layerTwoFeatureFlag
        )
    }

    override val activeAssets: List<CryptoAsset>
        get() = activeAssetMap.values.toList()

    override val loadedAssets: List<CryptoAsset>
        get() = assetMap.values.toList()

    companion object {
        private val assetActions =
            setOf(
                AssetAction.Buy,
                AssetAction.Sell,
                AssetAction.Swap,
                AssetAction.Send,
                AssetAction.Receive,
                AssetAction.ViewActivity,
                AssetAction.InterestDeposit
            )
    }
}
