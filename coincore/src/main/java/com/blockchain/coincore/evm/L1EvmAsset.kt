package com.blockchain.coincore.evm

import com.blockchain.annotations.CommonCode
import com.blockchain.coincore.CryptoAddress
import com.blockchain.coincore.ReceiveAddress
import com.blockchain.coincore.SingleAccountList
import com.blockchain.coincore.TxResult
import com.blockchain.coincore.impl.CryptoAssetBase
import com.blockchain.coincore.impl.EthHotWalletAddressResolver
import com.blockchain.coincore.impl.StandardL1Asset
import com.blockchain.coincore.wrap.FormatUtilities
import com.blockchain.core.chains.EvmNetwork
import com.blockchain.core.chains.erc20.Erc20DataManager
import com.blockchain.core.chains.ethereum.EthDataManager
import com.blockchain.core.fees.FeeDataManager
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.preferences.WalletStatusPrefs
import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.AssetCategory
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.Singles

internal class L1EvmAsset(
    override val currency: AssetInfo,
    private val erc20DataManager: Erc20DataManager,
    private val feeDataManager: FeeDataManager,
    private val ethDataManager: EthDataManager,
    private val labels: DefaultLabels,
    private val walletPreferences: WalletStatusPrefs,
    private val formatUtils: FormatUtilities,
    private val addressResolver: EthHotWalletAddressResolver,
    private val layerTwoFeatureFlag: FeatureFlag,
    private val coinNetworksFlag: FeatureFlag,
    private val evmNetworks: Single<List<EvmNetwork>>
) : CryptoAssetBase(),
    StandardL1Asset {
    private val erc20address
        get() = erc20DataManager.accountHash

    // For example to get MATIC from MATIC.MATIC
    private val nativeNetworkTicker = currency.networkTicker.split(".").first()

    override fun loadNonCustodialAccounts(labels: DefaultLabels): Single<SingleAccountList> =
        Singles.zip(
            layerTwoFeatureFlag.enabled,
            coinNetworksFlag.enabled
        )
            .flatMap { (isL2Enabled, isCoinNetworksEnabled) ->
                when {
                    isL2Enabled && isCoinNetworksEnabled -> {
                        // Find the correct network from the new coin networks endpoint
                        evmNetworks.map { networks ->
                            loadNonCustodialAccount(networks)
                        }
                    }
                    isL2Enabled -> {
                        // Get the network from the remote config
                        erc20DataManager.getSupportedNetworks().map { supportedNetworks ->
                            loadNonCustodialAccount(supportedNetworks.toList())
                        }
                    }
                    else -> Single.just(emptyList())
                }
            }

    private fun loadNonCustodialAccount(availableNetworks: List<EvmNetwork>): SingleAccountList {
        return if (currency.categories.contains(AssetCategory.NON_CUSTODIAL)) {
            availableNetworks.firstOrNull { evmNetwork ->
                evmNetwork.networkTicker == nativeNetworkTicker
            }?.let { evmNetwork ->
                listOf(getNonCustodialAccount(evmNetwork))
            } ?: emptyList()
        } else {
            emptyList()
        }
    }

    private fun getNonCustodialAccount(evmNetwork: EvmNetwork): L1EvmNonCustodialAccount =
        L1EvmNonCustodialAccount(
            asset = currency,
            erc20DataManager = erc20DataManager,
            address = erc20address,
            fees = feeDataManager,
            label = labels.getDefaultNonCustodialWalletLabel(),
            exchangeRates = exchangeRates,
            ethDataManager = ethDataManager,
            walletPreferences = walletPreferences,
            addressResolver = addressResolver,
            l1Network = evmNetwork
        )

    @CommonCode("Exists in EthAsset")
    override fun parseAddress(address: String, label: String?, isDomainAddress: Boolean): Maybe<ReceiveAddress> {

        val normalisedAddress = address.removePrefix(FormatUtilities.ETHEREUM_PREFIX)

        return Single.just(isValidAddress(normalisedAddress))
            .flatMapMaybe { isValid ->
                if (isValid) {
                    erc20DataManager.isContractAddress(
                        address = normalisedAddress,
                        l1Chain = nativeNetworkTicker
                    )
                        .flatMapMaybe { isContract ->
                            Maybe.just(
                                L1EvmAddress(
                                    asset = currency,
                                    address = normalisedAddress,
                                    label = label ?: normalisedAddress,
                                    isDomain = isDomainAddress,
                                    isContract = isContract
                                )
                            )
                        }
                } else {
                    Maybe.empty()
                }
            }
    }

    override fun isValidAddress(address: String): Boolean =
        formatUtils.isValidEthereumAddress(address)
}

internal class L1EvmAddress(
    override val asset: AssetInfo,
    override val address: String,
    override val label: String = address,
    override val isDomain: Boolean = false,
    override val onTxCompleted: (TxResult) -> Completable = { Completable.complete() },
    override val amount: CryptoValue? = null,
    val isContract: Boolean = false,
) : CryptoAddress
