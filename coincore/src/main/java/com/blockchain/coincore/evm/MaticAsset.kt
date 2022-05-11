package com.blockchain.coincore.evm

import com.blockchain.annotations.CommonCode
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.CryptoAddress
import com.blockchain.coincore.ReceiveAddress
import com.blockchain.coincore.SingleAccountList
import com.blockchain.coincore.TxResult
import com.blockchain.coincore.impl.CryptoAssetBase
import com.blockchain.coincore.impl.CustodialTradingAccount
import com.blockchain.coincore.impl.EthHotWalletAddressResolver
import com.blockchain.coincore.wrap.FormatUtilities
import com.blockchain.core.chains.EvmNetwork
import com.blockchain.core.chains.erc20.Erc20DataManager
import com.blockchain.core.custodial.TradingBalanceDataManager
import com.blockchain.core.interest.InterestBalanceDataManager
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.logging.RemoteLogger
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.WalletStatus
import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.AssetCategory
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.isCustodialOnly
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.fees.FeeDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import thepit.PitLinking

internal class MaticAsset(
    private val ethDataManager: EthDataManager,
    private val erc20DataManager: Erc20DataManager,
    private val feeDataManager: FeeDataManager,
    private val walletPreferences: WalletStatus,
    payloadManager: PayloadDataManager,
    custodialManager: CustodialWalletManager,
    interestBalances: InterestBalanceDataManager,
    tradingBalances: TradingBalanceDataManager,
    exchangeRates: ExchangeRatesDataManager,
    currencyPrefs: CurrencyPrefs,
    labels: DefaultLabels,
    pitLinking: PitLinking,
    remoteLogger: RemoteLogger,
    identity: UserIdentity,
    private val availableNonCustodialActions: Set<AssetAction>,
    private val formatUtils: FormatUtilities,
    addressResolver: EthHotWalletAddressResolver,
    private val layerTwoFeatureFlag: FeatureFlag
) : CryptoAssetBase(
    payloadManager,
    exchangeRates,
    currencyPrefs,
    labels,
    custodialManager,
    interestBalances,
    tradingBalances,
    pitLinking,
    remoteLogger,
    identity,
    addressResolver
) {
    private val erc20address
        get() = erc20DataManager.accountHash

    private val nativeNetworkTicker = CryptoCurrency.MATIC.networkTicker.removeSuffix(".MATIC")

    override val assetInfo: AssetInfo = CryptoCurrency.MATIC

    override val isCustodialOnly: Boolean = assetInfo.isCustodialOnly
    override val multiWallet: Boolean = false

    override fun loadNonCustodialAccounts(labels: DefaultLabels): Single<SingleAccountList> =
        layerTwoFeatureFlag.enabled.flatMap { isEnabled ->
            if (isEnabled) {
                erc20DataManager.getSupportedNetworks().map { supportedNetworks ->
                    if (assetInfo.categories.contains(AssetCategory.NON_CUSTODIAL)) {
                        supportedNetworks.firstOrNull { evmNetwork ->
                            evmNetwork.networkTicker == nativeNetworkTicker
                        }?.let { evmNetwork ->
                            listOf(getNonCustodialAccount(evmNetwork))
                        } ?: emptyList()
                    } else {
                        emptyList()
                    }
                }
            } else {
                Single.just(emptyList())
            }
        }

    override fun loadCustodialAccounts(): Single<SingleAccountList> =
        if (assetInfo.categories.contains(AssetCategory.CUSTODIAL)) {
            Single.just(
                listOf(
                    CustodialTradingAccount(
                        currency = assetInfo,
                        label = labels.getDefaultCustodialWalletLabel(),
                        exchangeRates = exchangeRates,
                        custodialWalletManager = custodialManager,
                        tradingBalances = tradingBalances,
                        identity = identity
                    )
                )
            )
        } else {
            Single.just(emptyList())
        }

    private fun getNonCustodialAccount(evmNetwork: EvmNetwork): L1EvmNonCustodialAccount =
        L1EvmNonCustodialAccount(
            payloadManager,
            assetInfo,
            ethDataManager,
            erc20DataManager,
            erc20address,
            feeDataManager,
            labels.getDefaultNonCustodialWalletLabel(),
            exchangeRates,
            walletPreferences,
            custodialManager,
            availableNonCustodialActions,
            identity,
            addressResolver,
            evmNetwork
        )

    @CommonCode("Exists in EthAsset")
    override fun parseAddress(address: String, label: String?, isDomainAddress: Boolean): Maybe<ReceiveAddress> {

        return Single.just(isValidAddress(address))
            .flatMapMaybe { isValid ->
                if (isValid) {
                    erc20DataManager.isContractAddress(
                        address = address,
                        l1Chain = nativeNetworkTicker
                    )
                        .flatMapMaybe { isContract ->
                            Maybe.just(
                                MaticAddress(
                                    address = address,
                                    label = label ?: address,
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

internal class MaticAddress(
    override val address: String,
    override val label: String = address,
    override val isDomain: Boolean = false,
    override val onTxCompleted: (TxResult) -> Completable = { Completable.complete() },
    override val amount: CryptoValue? = null,
    val isContract: Boolean = false
) : CryptoAddress {
    override val asset: AssetInfo = CryptoCurrency.MATIC
}
