package com.blockchain.coincore.eth

import com.blockchain.core.chains.erc20.Erc20DataManager
import com.blockchain.core.chains.erc20.call.Erc20HistoryCallCache
import com.blockchain.core.chains.erc20.domain.model.Erc20HistoryList
import com.blockchain.core.chains.ethereum.EthDataManager
import com.blockchain.core.common.caching.ParameteredSingleTimedCacheRequest
import com.blockchain.logging.Logger
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CoinNetwork
import info.blockchain.balance.isLayer2Token
import info.blockchain.wallet.api.data.FeeOptions
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import java.math.BigInteger
import org.web3j.abi.TypeEncoder
import org.web3j.abi.datatypes.Address
import org.web3j.crypto.RawTransaction

internal class Erc20DataManagerImpl(
    private val ethDataManager: EthDataManager,
    private val historyCallCache: Erc20HistoryCallCache
) : Erc20DataManager {

    override val accountHash: String
        get() = ethDataManager.accountAddress

    override val requireSecondPassword: Boolean
        get() = ethDataManager.requireSecondPassword

    private val latestBlockCacheRequest: ParameteredSingleTimedCacheRequest<CoinNetwork, BigInteger> by lazy {
        ParameteredSingleTimedCacheRequest(
            cacheLifetimeSeconds = NODE_CALLS_CACHE_TTL_SECONDS,
            refreshFn = ::refreshLatestBlockNumber
        )
    }

    override fun getErc20History(asset: AssetInfo, evmNetwork: CoinNetwork): Single<Erc20HistoryList> {
        return historyCallCache.fetch(accountHash, asset, evmNetwork.networkTicker)
    }

    override fun supportsErc20TxNote(asset: AssetInfo): Boolean {
        require(asset.isLayer2Token)
        return try {
            // TODO (dtverdota): this try catch is here because there's no token data on the
            // Ethereum blockchain for L2 Erc20 coins like USDC.MATIC. Even though we check null here,
            // there's a crash further down on erc20Tokens in the EthereumWallet when the user only got
            // coins on an L2 chain. Deal with it as part of the L2 + ETH -> EVM refactoring.
            ethDataManager.getErc20TokenData(asset) != null
        } catch (ex: Exception) {
            Logger.e(ex)
            return false
        }
    }

    override fun getErc20TxNote(asset: AssetInfo, txHash: String): String? {
        require(asset.isLayer2Token)
        return ethDataManager.getErc20TokenData(asset)?.txNotes?.get(txHash)
    }

    override fun putErc20TxNote(asset: AssetInfo, txHash: String, note: String): Completable {
        require(asset.isLayer2Token)
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
        require(asset.isLayer2Token)
        val l1Chain = asset.coinNetwork?.networkTicker
        require(l1Chain != null)

        return getNonce(l1Chain)
            .map { nonce ->
                val contractAddress = asset.l2identifier
                checkNotNull(contractAddress)

                // If we couldn't find a hot wallet address for any reason (in which case the HotWalletService is
                // returning an empty address) fall back to the usual path.
                val useHotWallet = hotWalletAddress.isNotEmpty()

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
        return getNonce(evmNetwork)
            .map { nonce ->
                // If we couldn't find a hot wallet address for any reason (in which case the HotWalletService is
                // returning an empty address) fall back to the usual path.
                val useHotWallet = hotWalletAddress.isNotEmpty()

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
                ethDataManager.signEthTransaction(rawTransaction, secondPassword, evmNetwork.chainId!!)
            } ?: throw IllegalAccessException("Unsupported EVM Network")
        }

    override fun getFeesForEvmTransaction(l1Chain: String): Single<FeeOptions> =
        ethDataManager.getFeesForEvmTx(l1Chain)

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
                latestBlockCacheRequest.getCachedSingle(evmNetwork)
            } ?: throw IllegalAccessException("Unsupported EVM Network")
        }

    private fun refreshLatestBlockNumber(evmNetwork: CoinNetwork): Single<BigInteger> {
        return ethDataManager.getLatestBlockNumber(evmNetwork.nodeUrls.first()).map { it.number }
    }

    companion object {
        private const val NODE_CALLS_CACHE_TTL_SECONDS = 10L
    }
}
