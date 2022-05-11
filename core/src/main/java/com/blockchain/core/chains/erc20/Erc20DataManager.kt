package com.blockchain.core.chains.erc20

import com.blockchain.api.services.AssetDiscoveryService
import com.blockchain.core.chains.EvmNetwork
import com.blockchain.core.chains.erc20.call.Erc20BalanceCallCache
import com.blockchain.core.chains.erc20.call.Erc20HistoryCallCache
import com.blockchain.core.chains.erc20.model.Erc20Balance
import com.blockchain.core.chains.erc20.model.Erc20HistoryList
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.outcome.fold
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Currency
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.Singles
import java.math.BigInteger
import kotlin.IllegalStateException
import kotlinx.coroutines.rx3.rxSingle
import org.web3j.abi.TypeEncoder
import org.web3j.abi.datatypes.Address
import org.web3j.crypto.RawTransaction
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.utils.extensions.zipSingles

interface Erc20DataManager {
    val accountHash: String
    val requireSecondPassword: Boolean

    fun getL1TokenBalance(asset: AssetInfo): Single<CryptoValue>

    fun getErc20History(asset: AssetInfo, evmNetwork: EvmNetwork): Single<Erc20HistoryList>

    fun createErc20Transaction(
        asset: AssetInfo,
        to: String,
        amount: BigInteger,
        gasPriceWei: BigInteger,
        gasLimitGwei: BigInteger,
        hotWalletAddress: String
    ): Single<RawTransaction>

    fun createEvmTransaction(
        asset: AssetInfo,
        evmNetwork: String,
        to: String,
        amount: BigInteger,
        gasPriceWei: BigInteger,
        gasLimitGwei: BigInteger,
        hotWalletAddress: String
    ): Single<RawTransaction>

    fun signErc20Transaction(
        rawTransaction: RawTransaction,
        secondPassword: String = "",
        l1Chain: String
    ): Single<ByteArray>

    fun pushErc20Transaction(signedTxBytes: ByteArray, l1Chain: String): Single<String>

    fun supportsErc20TxNote(asset: AssetInfo): Boolean
    fun getErc20TxNote(asset: AssetInfo, txHash: String): String?
    fun putErc20TxNote(asset: AssetInfo, txHash: String, note: String): Completable

    fun hasUnconfirmedTransactions(): Single<Boolean>
    fun latestBlockNumber(l1Chain: String? = null): Single<BigInteger>
    fun isContractAddress(address: String, l1Chain: String? = null): Single<Boolean>

    fun getErc20Balance(asset: AssetInfo): Observable<Erc20Balance>
    fun getActiveAssets(): Single<Set<AssetInfo>>

    fun getSupportedNetworks(): Single<List<EvmNetwork>>

    // TODO: Get assets with balance
    fun flushCaches(asset: AssetInfo)

    fun getL1AssetFor(asset: AssetInfo): Single<AssetInfo>
}

internal class Erc20DataManagerImpl(
    private val ethDataManager: EthDataManager,
    private val balanceCallCache: Erc20BalanceCallCache,
    private val historyCallCache: Erc20HistoryCallCache,
    private val assetCatalogue: AssetCatalogue,
    private val ethMemoForHotWalletFeatureFlag: FeatureFlag,
    private val ethLayerTwoFeatureFlag: FeatureFlag
) : Erc20DataManager {

    override val accountHash: String
        get() = ethDataManager.accountAddress

    override val requireSecondPassword: Boolean
        get() = ethDataManager.requireSecondPassword

    override fun getL1TokenBalance(asset: AssetInfo): Single<CryptoValue> =
        Singles.zip(
            ethDataManager.supportedNetworks,
            getL1AssetFor(asset)
        )
            .flatMap { (supportedNetworks, l1Asset) ->
                rxSingle {
                    supportedNetworks.firstOrNull { evmNetwork ->
                        // Fall back to the network ticker in case of an L1 coin
                        evmNetwork.networkTicker == (asset.l1chainTicker ?: asset.networkTicker)
                    }?.let { evmNetwork ->
                        ethDataManager.getBalance(evmNetwork.nodeUrl)
                    }?.fold(
                        onFailure = { throw it.throwable },
                        onSuccess = { value -> CryptoValue(l1Asset, value) }
                    ) ?: throw IllegalStateException("L1 chain is missing or not supported")
                }
            }

    override fun getErc20Balance(asset: AssetInfo): Observable<Erc20Balance> {
        require(asset.isErc20())
        requireNotNull(asset.l1chainTicker)
        requireNotNull(asset.l2identifier)

        return ethLayerTwoFeatureFlag.enabled.flatMapObservable { isEnabled ->
            if (isEnabled) {
                ethDataManager.supportedNetworks.flatMapObservable { supportedNetworks ->
                    supportedNetworks.firstOrNull { it.networkTicker == asset.l1chainTicker }?.let { evmNetwork ->
                        rxSingle {
                            // Get the balance of the native token for example Matic in Polygon's case. Only load
                            // the balances of the other tokens on that network if the native token balance is positive.
                            ethDataManager.getBalance(evmNetwork.nodeUrl)
                                .fold(
                                    onFailure = { throw it.throwable },
                                    onSuccess = { value -> Pair(evmNetwork, value) }
                                )
                        }
                            .flatMapObservable { (evmNetwork, value) -> getErc20Balance(asset, evmNetwork, value) }
                    } ?: Observable.just(Erc20Balance.zero(asset))
                }
            } else {
                balanceCallCache.getBalances(accountHash)
                    .map { it.getOrDefault(asset, Erc20Balance.zero(asset)) }
                    .toObservable()
            }
        }
    }

    override fun getActiveAssets(): Single<Set<AssetInfo>> =
        ethLayerTwoFeatureFlag.enabled.flatMap { isEnabled ->
            balanceCallCache.getBalances(accountHash).map { it.keys }.flatMap { baseErc20Assets ->
                if (isEnabled) {
                    getSupportedNetworks().flatMap { supportedNetworks ->
                        supportedNetworks.map { evmNetwork ->
                            balanceCallCache.getBalances(accountHash, evmNetwork.networkTicker).map { it.keys }
                        }.zipSingles().map {
                            (baseErc20Assets + it.flatten()).toSet()
                        }
                    }
                } else {
                    Single.just(baseErc20Assets)
                }
            }
        }

    override fun getErc20History(asset: AssetInfo, evmNetwork: EvmNetwork): Single<Erc20HistoryList> {
        require(asset.isErc20())
        return historyCallCache.fetch(accountHash, asset, evmNetwork.networkTicker)
    }

    override fun supportsErc20TxNote(asset: AssetInfo): Boolean {
        require(asset.isErc20())
        return ethDataManager.getErc20TokenData(asset) != null
    }

    override fun getErc20TxNote(asset: AssetInfo, txHash: String): String? {
        require(asset.isErc20())
        return ethDataManager.getErc20TokenData(asset)?.txNotes?.get(txHash)
    }

    override fun putErc20TxNote(asset: AssetInfo, txHash: String, note: String): Completable {
        require(asset.isErc20())
        return ethDataManager.updateErc20TransactionNotes(asset, txHash, note)
    }

    override fun isContractAddress(address: String, l1Chain: String?): Single<Boolean> =
        ethDataManager.supportedNetworks.flatMap { supportedL2Networks ->
            supportedL2Networks.firstOrNull { it.networkTicker == l1Chain }?.let { evmNetwork ->
                ethDataManager.isContractAddress(address, evmNetwork.nodeUrl)
            } ?: ethDataManager.isContractAddress(address)
        }

    override fun createErc20Transaction(
        asset: AssetInfo,
        to: String,
        amount: BigInteger,
        gasPriceWei: BigInteger,
        gasLimitGwei: BigInteger,
        hotWalletAddress: String
    ): Single<RawTransaction> {
        require(asset.isErc20())
        val l1Chain = asset.l1chainTicker
        require(l1Chain != null)

        return Singles.zip(
            getNonce(l1Chain),
            ethMemoForHotWalletFeatureFlag.enabled
        )
            .map { (nonce, enabled) ->
                val contractAddress = asset.l2identifier
                checkNotNull(contractAddress)

                // If we couldn't find a hot wallet address for any reason (in which case the HotWalletService is
                // returning an empty address) fall back to the usual path.
                val useHotWallet = enabled && hotWalletAddress.isNotEmpty()

                RawTransaction.createTransaction(
                    nonce,
                    gasPriceWei,
                    if (useHotWallet) {
                        gasLimitGwei + ethDataManager.extraGasLimitForMemo()
                    } else {
                        gasLimitGwei
                    },
                    contractAddress,
                    0.toBigInteger(),
                    erc20TransferMethod(to, amount, hotWalletAddress, useHotWallet)
                )
            }
    }

    override fun createEvmTransaction(
        asset: AssetInfo,
        evmNetwork: String,
        to: String,
        amount: BigInteger,
        gasPriceWei: BigInteger,
        gasLimitGwei: BigInteger,
        hotWalletAddress: String
    ): Single<RawTransaction> {

        return Singles.zip(
            getNonce(evmNetwork),
            ethMemoForHotWalletFeatureFlag.enabled
        )
            .map { (nonce, enabled) ->
                // If we couldn't find a hot wallet address for any reason (in which case the HotWalletService is
                // returning an empty address) fall back to the usual path.
                val useHotWallet = enabled && hotWalletAddress.isNotEmpty()

                ethDataManager.createEthTransaction(
                    nonce = nonce,
                    to = if (useHotWallet) hotWalletAddress else to,
                    gasPriceWei = gasPriceWei,
                    gasLimitGwei = if (useHotWallet) {
                        gasLimitGwei + ethDataManager.extraGasLimitForMemo()
                    } else {
                        gasLimitGwei
                    },
                    weiValue = amount,
                    data = if (useHotWallet) to else ""
                )
            }
    }

    private fun getNonce(l1Chain: String): Single<BigInteger> {
        return ethDataManager.supportedNetworks.flatMap { supportedNetworks ->
            supportedNetworks.firstOrNull { evmNetwork ->
                evmNetwork.networkTicker == l1Chain
            }?.let { evmNetwork ->
                ethDataManager.getNonce(evmNetwork.nodeUrl)
            } ?: throw IllegalAccessException("Unsupported EVM Network")
        }
    }

    private fun erc20TransferMethod(
        to: String,
        amount: BigInteger,
        hotWalletAddress: String,
        useHotWallet: Boolean
    ): String {
        val transferMethodHex = "0xa9059cbb"

        return if (useHotWallet) {
            transferMethodHex + TypeEncoder.encode(Address(hotWalletAddress)) +
                TypeEncoder.encode(org.web3j.abi.datatypes.generated.Uint256(amount)) +
                TypeEncoder.encode(Address(to))
        } else {
            transferMethodHex + TypeEncoder.encode(Address(to)) +
                TypeEncoder.encode(org.web3j.abi.datatypes.generated.Uint256(amount))
        }
    }

    override fun signErc20Transaction(
        rawTransaction: RawTransaction,
        secondPassword: String,
        l1Chain: String
    ): Single<ByteArray> =
        ethDataManager.supportedNetworks.flatMap { supportedL2Networks ->
            supportedL2Networks.firstOrNull { it.networkTicker == l1Chain }?.let { evmNetwork ->
                ethDataManager.signEthTransaction(rawTransaction, secondPassword, evmNetwork.chainId)
            } ?: throw IllegalAccessException("Unsupported EVM Network")
        }

    override fun pushErc20Transaction(signedTxBytes: ByteArray, l1Chain: String): Single<String> =
        ethDataManager.supportedNetworks.flatMap { supportedNetworks ->
            supportedNetworks.firstOrNull { it.networkTicker == l1Chain }?.let { evmNetwork ->
                if (evmNetwork.chainId == EthDataManager.ETH_CHAIN_ID) {
                    ethDataManager.pushTx(signedTxBytes)
                } else {
                    ethDataManager.pushEvmTx(signedTxBytes, l1Chain)
                }
            } ?: throw IllegalAccessException("Unsupported EVM Network")
        }

    override fun hasUnconfirmedTransactions(): Single<Boolean> =
        ethDataManager.isLastTxPending()

    override fun latestBlockNumber(l1Chain: String?): Single<BigInteger> =
        ethDataManager.supportedNetworks.flatMap { supportedNetworks ->
            supportedNetworks.firstOrNull { it.networkTicker == l1Chain }?.let { evmNetwork ->
                ethDataManager.getLatestBlockNumber(evmNetwork.nodeUrl).map { it.number }
            } ?: throw IllegalAccessException("Unsupported EVM Network")
        }

    override fun getSupportedNetworks(): Single<List<EvmNetwork>> = ethDataManager.supportedNetworks

    override fun flushCaches(asset: AssetInfo) {
        require(asset.isErc20())

        balanceCallCache.flush(asset)
        historyCallCache.flush(asset)
    }

    override fun getL1AssetFor(asset: AssetInfo): Single<AssetInfo> {
        return ethDataManager.supportedNetworks.map { supportedNetworks ->
            assetCatalogue.fromNetworkTicker(
                supportedNetworks.firstOrNull {
                    // Fall back to the network ticker in case of an L1 coin
                    it.networkTicker == (asset.l1chainTicker ?: asset.networkTicker)
                }?.networkTicker ?: ""
            ) as? AssetInfo ?: throw IllegalAccessException("Unsupported EVM Network")
        }
    }

    private fun getErc20Balance(
        asset: AssetInfo,
        evmNetwork: EvmNetwork,
        balance: BigInteger
    ): Observable<Erc20Balance> {
        val hasNativeTokenBalance = balance > BigInteger.ZERO
        val isOnOtherEvm = evmNetwork.chainId != EthDataManager.ETH_CHAIN_ID
        return when {
            // Only load L2 balances if we have a balance of the network's native token
            isOnOtherEvm && hasNativeTokenBalance -> balanceCallCache.getBalances(accountHash, evmNetwork.networkTicker)
                .map { it.getOrDefault(asset, Erc20Balance.zero(asset)) }.toObservable()
            !isOnOtherEvm -> balanceCallCache.getBalances(accountHash)
                .map { it.getOrDefault(asset, Erc20Balance.zero(asset)) }
                .toObservable()
            else -> Observable.just(Erc20Balance.zero(asset))
        }
    }
}

// TODO this has to scale, need to find a way to get the networks from the remote config
fun Currency.isErc20() =
    (this as? AssetInfo)?.l1chainTicker?.equals(CryptoCurrency.ETHER.networkTicker) == true ||
        (this as? AssetInfo)?.l1chainTicker?.equals(AssetDiscoveryService.MATIC) == true ||
        (this as? AssetInfo)?.displayTicker?.equals(AssetDiscoveryService.MATIC) == true
