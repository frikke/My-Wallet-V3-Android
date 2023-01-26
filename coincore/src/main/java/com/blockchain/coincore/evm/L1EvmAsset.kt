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
import com.blockchain.koin.scopedInject
import com.blockchain.preferences.WalletStatusPrefs
import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single

internal class L1EvmAsset(
    override val currency: AssetInfo,
    private val erc20DataManager: Erc20DataManager,
    private val labels: DefaultLabels,
    private val walletPreferences: WalletStatusPrefs,
    private val formatUtils: FormatUtilities,
    private val addressResolver: EthHotWalletAddressResolver,
    private val evmNetwork: EvmNetwork
) : CryptoAssetBase(),
    StandardL1Asset {
    private val erc20address
        get() = erc20DataManager.accountHash

    private val ethDataManager: EthDataManager by scopedInject()

    override fun loadNonCustodialAccounts(labels: DefaultLabels): Single<SingleAccountList> =
        Single.just(loadNonCustodialAccount())

    private fun loadNonCustodialAccount(): SingleAccountList {
        return listOf(getNonCustodialAccount(evmNetwork))
    }

    private fun getNonCustodialAccount(evmNetwork: EvmNetwork): L1EvmNonCustodialAccount =
        L1EvmNonCustodialAccount(
            asset = currency,
            erc20DataManager = erc20DataManager,
            address = erc20address,
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
                        l1Chain = evmNetwork.networkTicker
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
