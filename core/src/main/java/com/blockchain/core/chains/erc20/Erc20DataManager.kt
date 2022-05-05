package com.blockchain.core.chains.erc20

import com.blockchain.api.services.AssetDiscoveryService
import com.blockchain.core.chains.EthL2Chain
import com.blockchain.core.chains.erc20.call.Erc20BalanceCallCache
import com.blockchain.core.chains.erc20.call.Erc20HistoryCallCache
import com.blockchain.core.chains.erc20.model.Erc20Balance
import com.blockchain.core.chains.erc20.model.Erc20HistoryList
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.outcome.fold
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Currency
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.Singles
import java.math.BigInteger
import kotlinx.coroutines.rx3.rxSingle
import org.web3j.abi.TypeEncoder
import org.web3j.abi.datatypes.Address
import org.web3j.crypto.RawTransaction
import piuk.blockchain.androidcore.data.ethereum.EthDataManager

interface Erc20DataManager {
    val accountHash: String
    val requireSecondPassword: Boolean

    fun getEthBalance(): Single<CryptoValue>

    fun getErc20History(asset: AssetInfo): Single<Erc20HistoryList>

    fun createErc20Transaction(
        asset: AssetInfo,
        to: String,
        amount: BigInteger,
        gasPriceWei: BigInteger,
        gasLimitGwei: BigInteger,
        hotWalletAddress: String
    ): Single<RawTransaction>

    fun signErc20Transaction(
        rawTransaction: RawTransaction,
        secondPassword: String = ""
    ): Single<ByteArray>

    fun pushErc20Transaction(signedTxBytes: ByteArray): Single<String>

    fun supportsErc20TxNote(asset: AssetInfo): Boolean
    fun getErc20TxNote(asset: AssetInfo, txHash: String): String?
    fun putErc20TxNote(asset: AssetInfo, txHash: String, note: String): Completable

    fun hasUnconfirmedTransactions(): Single<Boolean>
    fun latestBlockNumber(parentChain: String? = null): Single<BigInteger>
    fun isContractAddress(address: String, parentChain: String? = null): Single<Boolean>

    fun getErc20Balance(asset: AssetInfo): Observable<Erc20Balance>
    fun getActiveAssets(): Single<Set<AssetInfo>>

    fun getSupportedNetworks(): Single<List<EthL2Chain>>

    // TODO: Get assets with balance
    fun flushCaches(asset: AssetInfo)
}

internal class Erc20DataManagerImpl(
    private val ethDataManager: EthDataManager,
    private val balanceCallCache: Erc20BalanceCallCache,
    private val historyCallCache: Erc20HistoryCallCache,
    private val ethMemoForHotWalletFeatureFlag: FeatureFlag,
    private val ethLayerTwoFeatureFlag: FeatureFlag
) : Erc20DataManager {

    override val accountHash: String
        get() = ethDataManager.accountAddress

    override val requireSecondPassword: Boolean
        get() = ethDataManager.requireSecondPassword

    override fun getEthBalance(): Single<CryptoValue> =
        rxSingle {
            ethDataManager.getBalance()
                .fold(
                    onFailure = { throw it.throwable },
                    onSuccess = { value ->
                        CryptoValue(CryptoCurrency.ETHER, value)
                    }
                )
        }

    override fun getErc20Balance(asset: AssetInfo): Observable<Erc20Balance> {
        require(asset.isErc20())
        requireNotNull(asset.l1chainTicker)
        requireNotNull(asset.l2identifier)

        return ethLayerTwoFeatureFlag.enabled.flatMapObservable { isEnabled ->
            if (isEnabled) {
                ethDataManager.supportedNetworks.flatMapObservable { supportedL2Networks ->
                    supportedL2Networks.firstOrNull { it.networkTicker == asset.l1chainTicker }?.let { ethL2Chain ->
                        rxSingle {
                            // Get the balance of the native token for example Matic in Polygon's case. Only load
                            // the balances of the other tokens on that network if the native token balance is positive.
                            ethDataManager.getBalance(ethL2Chain.nodeUrl)
                                .fold(
                                    onFailure = { throw it.throwable },
                                    onSuccess = { value -> Pair(ethL2Chain, value) }
                                )
                        }
                            .flatMapObservable { (ethL2Chain, value) -> getErc20Balance(asset, ethL2Chain, value) }
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
        balanceCallCache.getBalances(accountHash)
            .map { it.keys }

    override fun getErc20History(asset: AssetInfo): Single<Erc20HistoryList> {
        require(asset.isErc20())
        return historyCallCache.fetch(accountHash, asset)
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

    override fun isContractAddress(address: String, parentChain: String?): Single<Boolean> =
        ethDataManager.supportedNetworks.flatMap { supportedL2Networks ->
            supportedL2Networks.firstOrNull { it.networkTicker == parentChain }?.let { ethL2Chain ->
                ethDataManager.isContractAddress(address, ethL2Chain.nodeUrl)
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

        return Singles.zip(
            ethDataManager.getNonce(),
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
        secondPassword: String
    ): Single<ByteArray> =
        ethDataManager.signEthTransaction(rawTransaction, secondPassword)

    override fun pushErc20Transaction(signedTxBytes: ByteArray): Single<String> =
        ethDataManager.pushTx(signedTxBytes)

    override fun hasUnconfirmedTransactions(): Single<Boolean> =
        ethDataManager.isLastTxPending()

    override fun latestBlockNumber(parentChain: String?): Single<BigInteger> =
        ethDataManager.supportedNetworks.flatMap { supportedL2Networks ->
            supportedL2Networks.firstOrNull { it.networkTicker == parentChain }?.let { ethL2Chain ->
                ethDataManager.getLatestBlockNumber(ethL2Chain.nodeUrl).map { it.number }
            } ?: ethDataManager.getLatestBlockNumber().map { it.number }
        }

    override fun getSupportedNetworks(): Single<List<EthL2Chain>> = ethDataManager.supportedNetworks

    override fun flushCaches(asset: AssetInfo) {
        require(asset.isErc20())

        balanceCallCache.flush(asset)
        historyCallCache.flush(asset)
    }

    private fun getErc20Balance(
        asset: AssetInfo,
        ethL2Chain: EthL2Chain,
        balance: BigInteger
    ): Observable<Erc20Balance> {
        val hasNativeTokenBalance = balance > BigInteger.ZERO
        val isL2 = ethL2Chain.chainId != EthDataManager.ETH_CHAIN_ID
        return when {
            // Only load L2 balances if we have a balance of the network's native token
            isL2 && hasNativeTokenBalance -> balanceCallCache.getBalances(accountHash, ethL2Chain.networkTicker)
                .map { it.getOrDefault(asset, Erc20Balance.zero(asset)) }.toObservable()
            !isL2 -> balanceCallCache.getBalances(accountHash)
                .map { it.getOrDefault(asset, Erc20Balance.zero(asset)) }
                .toObservable()
            else -> Observable.just(Erc20Balance.zero(asset))
        }
    }
}

// TODO this has to scale, need to find a way to get the networks from the remote config
fun Currency.isErc20() =
    (this as? AssetInfo)?.l1chainTicker?.equals(CryptoCurrency.ETHER.networkTicker) == true ||
        (this as? AssetInfo)?.l1chainTicker?.equals(AssetDiscoveryService.MATIC) == true
