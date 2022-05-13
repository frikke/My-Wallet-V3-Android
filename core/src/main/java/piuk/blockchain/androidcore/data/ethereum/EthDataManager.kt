package piuk.blockchain.androidcore.data.ethereum

import com.blockchain.api.adapters.ApiError
import com.blockchain.api.services.NonCustodialEvmService
import com.blockchain.core.chains.EvmNetwork
import com.blockchain.core.chains.EvmNetworksService
import com.blockchain.core.chains.erc20.isErc20
import com.blockchain.logging.LastTxUpdater
import com.blockchain.metadata.MetadataEntry
import com.blockchain.metadata.MetadataRepository
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.fold
import com.blockchain.outcome.map
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.ethereum.Erc20TokenData
import info.blockchain.wallet.ethereum.EthAccountApi
import info.blockchain.wallet.ethereum.EthUrls
import info.blockchain.wallet.ethereum.EthereumWallet
import info.blockchain.wallet.ethereum.data.EthLatestBlockNumber
import info.blockchain.wallet.ethereum.data.EthTransaction
import info.blockchain.wallet.ethereum.data.TransactionState
import info.blockchain.wallet.ethereum.node.EthJsonRpcRequest
import info.blockchain.wallet.ethereum.node.RequestType
import info.blockchain.wallet.ethereum.util.EthUtils
import info.blockchain.wallet.exceptions.HDWalletException
import info.blockchain.wallet.exceptions.InvalidCredentialsException
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import java.math.BigInteger
import java.util.HashMap
import kotlinx.coroutines.rx3.rxSingle
import org.web3j.crypto.RawTransaction
import piuk.blockchain.androidcore.data.ethereum.datastores.EthDataStore
import piuk.blockchain.androidcore.data.ethereum.models.CombinedEthModel
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.utils.extensions.applySchedulers

class EthDataManager(
    private val payloadDataManager: PayloadDataManager,
    private val ethAccountApi: EthAccountApi,
    private val ethDataStore: EthDataStore,
    private val metadataRepository: MetadataRepository,
    private val lastTxUpdater: LastTxUpdater,
    private val evmNetworksService: EvmNetworksService,
    private val nonCustodialEvmService: NonCustodialEvmService
) : EthMessageSigner {

    private val internalAccountAddress: String?
        get() = ethDataStore.ethWallet?.account?.address

    val accountAddress: String
        get() = internalAccountAddress ?: throw Exception("No ETH address found")

    val supportedNetworks: Single<List<EvmNetwork>>
        get() = evmNetworksService.getSupportedNetworks().map {
            listOf(listOf(ethChain), it).flatten()
        }
            .onErrorReturn { listOf(ethChain) }

    /**
     * Clears the currently stored ETH account from memory.
     */
    fun clearAccountDetails() {
        ethDataStore.clearData()
    }

    /**
     * Returns an [CombinedEthModel] object for a given ETH address as an [Observable]. An
     * [CombinedEthModel] contains a list of transactions associated with the account, as well
     * as a final balance. Calling this function also caches the [CombinedEthModel].
     *
     * @return An [Observable] wrapping an [CombinedEthModel]
     */
    fun fetchEthAddress(): Observable<CombinedEthModel> =
        ethAccountApi.getEthAddress(listOf(accountAddress))
            .map(::CombinedEthModel)
            .doOnNext {
                ethDataStore.ethAddressResponse = it
            }
            .subscribeOn(Schedulers.io())

    suspend fun getBalance(nodeUrl: String = EthUrls.ETH_NODES): Outcome<ApiError, BigInteger> {
        return ethAccountApi.postEthNodeRequest(
            nodeUrl = nodeUrl,
            requestType = RequestType.GET_BALANCE,
            accountAddress,
            EthJsonRpcRequest.defaultBlock
        ).map { response ->
            EthUtils.convertHexToBigInteger(response.result)
        }
    }

    fun updateAccountLabel(label: String): Completable {
        require(label.isNotEmpty())
        check(ethDataStore.ethWallet != null)
        ethDataStore.ethWallet?.renameAccount(label)
        return save()
    }

    /**
     * Returns the user's ETH account object if previously fetched.
     *
     * @return A nullable [CombinedEthModel] object
     */
    fun getEthResponseModel(): CombinedEthModel? = ethDataStore.ethAddressResponse

    /**
     * Returns the user's [EthereumWallet] object if previously fetched.
     *
     * @return A nullable [EthereumWallet] object
     */
    @Deprecated("This shouldn't be directly exposed to higher layers")
    fun getEthWallet(): EthereumWallet? = ethDataStore.ethWallet

    /**
     * Returns a stream of [EthTransaction] objects associated with a user's ETH address specifically
     * for displaying in the transaction list. These are cached and may be empty if the account
     * hasn't previously been fetched.
     *
     * @return An [Observable] stream of [EthTransaction] objects
     */
    fun getEthTransactions(): Single<List<EthTransaction>> =
        internalAccountAddress?.let {
            ethAccountApi.getEthTransactions(listOf(it)).applySchedulers()
        } ?: Single.just(emptyList())

    /**
     * Returns whether or not the user's ETH account currently has unconfirmed transactions, and
     * therefore shouldn't be allowed to send funds until confirmation.
     * We compare the last submitted tx hash with the newly created tx hash - if they match it means
     * that the previous tx has not yet been processed.
     *
     * @return An [Observable] wrapping a [Boolean]
     */
    fun isLastTxPending(): Single<Boolean> =
        internalAccountAddress?.let {
            ethAccountApi.getLastEthTransaction(listOf(it)).map { tx ->
                tx.state.toLocalState() == TransactionState.PENDING
            }.defaultIfEmpty(false)
        } ?: Single.just(false)

    /**
     * Returns a [Number] representing the most recently
     * mined block.
     *
     * @return An [Observable] wrapping a [Number]
     */
    fun getLatestBlockNumber(nodeUrl: String = EthUrls.ETH_NODES): Single<EthLatestBlockNumber> =
        rxSingle {
            ethAccountApi.getLatestBlockNumber(nodeUrl = nodeUrl)
                .fold(
                    onSuccess = { latestBlockNumber -> latestBlockNumber },
                    onFailure = { throw it.throwable }
                )
        }.applySchedulers()

    fun isContractAddress(address: String, nodeUrl: String = EthUrls.ETH_NODES): Single<Boolean> =
        rxSingle {
            ethAccountApi.postEthNodeRequest(
                nodeUrl = nodeUrl,
                requestType = RequestType.IS_CONTRACT,
                address,
                EthJsonRpcRequest.defaultBlock
            )
                .fold(
                    onSuccess = { response ->
                        // In order to distinguish between these two addresses we need to call eth_getCode,
                        // which will return contract code if it's a contract and nothing if it's a wallet
                        response.result.removePrefix(EthUtils.PREFIX).isNotEmpty()
                    },
                    onFailure = { error ->
                        throw error.throwable
                    }
                )
        }.applySchedulers()

    private fun String.toLocalState() =
        when (this) {
            "PENDING" -> TransactionState.PENDING
            "CONFIRMED" -> TransactionState.CONFIRMED
            "REPLACED" -> TransactionState.REPLACED
            else -> TransactionState.UNKNOWN
        }

    /**
     * Returns the transaction notes for a given transaction hash, or null if not found.
     */
    fun getTransactionNotes(hash: String): String? = ethDataStore.ethWallet?.txNotes?.get(hash)

    /**
     * Puts a given note in the [HashMap] of transaction notes keyed to a transaction hash. This
     * information is then saved in the metadata service.
     *
     * @return A [Completable] object
     */
    fun updateTransactionNotes(hash: String, note: String): Completable =
        ethDataStore.ethWallet?.let {
            it.txNotes?.set(hash, note)
            return@let save()
        } ?: Completable.error { IllegalStateException("ETH Wallet is null") }
            .applySchedulers()

    internal fun updateErc20TransactionNotes(
        asset: AssetInfo,
        hash: String,
        note: String
    ): Completable {
        require(asset.isErc20())

        return Completable.defer {
            getErc20TokenData(asset)?.let {
                it.putTxNote(hash, note)
                save()
            } ?: Completable.complete()
        }.applySchedulers()
    }

    /**
     * Fetches EthereumWallet stored in metadata. If metadata entry doesn't exists it will be created.
     *
     * @param label The default address default to be used if metadata entry doesn't exist
     * @return An [Completable]
     */
    fun initEthereumWallet(
        assetCatalogue: AssetCatalogue,
        label: String
    ): Completable =
        fetchOrCreateEthereumWallet(assetCatalogue, label)
            .flatMapCompletable { (wallet, needsSave) ->
                ethDataStore.ethWallet = wallet
                if (needsSave) {
                    save()
                } else {
                    Completable.complete()
                }
            }

    /**
     * @param gasPriceWei Represents the fee the sender is willing to pay for gas. One unit of gas
     *                 corresponds to the execution of one atomic instruction, i.e. a computational step
     * @param gasLimitGwei Represents the maximum number of computational steps the transaction
     *                 execution is allowed to take
     * @param weiValue The amount of wei to transfer from the sender to the recipient
     */
    fun createEthTransaction(
        nonce: BigInteger,
        to: String,
        gasPriceWei: BigInteger,
        gasLimitGwei: BigInteger,
        weiValue: BigInteger,
        data: String = ""
    ): RawTransaction = RawTransaction.createTransaction(
        nonce,
        gasPriceWei,
        gasLimitGwei,
        to,
        weiValue,
        data
    )

    fun getTransaction(hash: String): Observable<EthTransaction> =
        ethAccountApi.getTransaction(hash)
            .applySchedulers()

    fun getNonce(nodeUrl: String = EthUrls.ETH_NODES): Single<BigInteger> = rxSingle {
        ethAccountApi.postEthNodeRequest(
            nodeUrl = nodeUrl,
            requestType = RequestType.GET_NONCE,
            accountAddress,
            EthJsonRpcRequest.defaultBlock
        )
            .fold(
                onSuccess = { response ->
                    EthUtils.convertHexToBigInteger(response.result)
                },
                onFailure = { error ->
                    throw error.throwable
                }
            )
    }

    fun signEthTransaction(
        rawTransaction: RawTransaction,
        secondPassword: String = "",
        chainId: Int = ETH_CHAIN_ID
    ): Single<ByteArray> =
        Single.fromCallable {
            if (payloadDataManager.isDoubleEncrypted) {
                payloadDataManager.decryptHDWallet(secondPassword)
            }
            val account = ethDataStore.ethWallet?.account ?: throw IllegalStateException("No Eth wallet defined")
            account.signTransaction(rawTransaction, payloadDataManager.masterKey, chainId)
        }

    override fun signEthMessage(message: String, secondPassword: String): Single<ByteArray> =
        Single.fromCallable {
            val data = message.decodeHex()
            if (payloadDataManager.isDoubleEncrypted) {
                payloadDataManager.decryptHDWallet(secondPassword)
            }
            val account = ethDataStore.ethWallet?.account ?: throw IllegalStateException("No Eth wallet defined")
            account.signMessage(data, payloadDataManager.masterKey)
        }

    override fun signEthTypedMessage(message: String, secondPassword: String): Single<ByteArray> {
        return Single.fromCallable {
            if (payloadDataManager.isDoubleEncrypted) {
                payloadDataManager.decryptHDWallet(secondPassword)
            }
            val account = ethDataStore.ethWallet?.account ?: throw IllegalStateException("No Eth wallet defined")
            account.signEthTypedMessage(message, payloadDataManager.masterKey)
        }
    }

    private fun String.decodeHex(): ByteArray {
        check(length % 2 == 0) { "Must have an even length" }
        return removePrefix(EthUtils.PREFIX)
            .chunked(2)
            .map { it.toInt(EthUtils.RADIX).toByte() }
            .toByteArray()
    }

    fun pushTx(signedTxBytes: ByteArray): Single<String> =
        ethAccountApi.pushTx(EthUtils.decorateAndEncode(signedTxBytes))
            .flatMap {
                lastTxUpdater.updateLastTxTime()
                    .onErrorComplete()
                    .andThen(Single.just(it))
            }
            .applySchedulers()

    fun pushEvmTx(signedTxBytes: ByteArray, l1Chain: String): Single<String> =
        rxSingle {
            nonCustodialEvmService.pushTransaction(EthUtils.decorateAndEncode(signedTxBytes), l1Chain)
                .fold(
                    onFailure = { throw it.throwable },
                    onSuccess = { response -> response.txId }
                )
        }.flatMap {
            lastTxUpdater.updateLastTxTime()
                .onErrorComplete()
                .andThen(Single.just(it))
        }
            .applySchedulers()

    private fun fetchOrCreateEthereumWallet(
        assetCatalogue: AssetCatalogue,
        label: String
    ): Single<Pair<EthereumWallet, Boolean>> =
        metadataRepository.loadRawValue(MetadataEntry.METADATA_ETH).defaultIfEmpty("")
            .map { metadata ->
                val walletJson = if (metadata != "") metadata else null

                var ethWallet = EthereumWallet.load(walletJson)
                var needsSave = false

                if (ethWallet?.account == null || !ethWallet.account!!.isCorrect) {
                    try {
                        val masterKey = payloadDataManager.masterKey
                        ethWallet = EthereumWallet(masterKey, label)
                        needsSave = true
                    } catch (e: HDWalletException) {
                        // Wallet private key unavailable. First decrypt with second password.
                        throw InvalidCredentialsException(
                            e.message
                        )
                    }
                }

                ethWallet.account?.let { ethereumAccount ->
                    if (!ethereumAccount.isAddressChecksummed()) {
                        ethereumAccount.address = ethereumAccount.withChecksummedAddress()
                        needsSave = true
                    }
                }

                ethWallet to needsSave
            }

    fun save(): Completable =
        metadataRepository.saveRawValue(
            ethDataStore.ethWallet!!.toJson(),
            MetadataEntry.METADATA_ETH
        )

    fun getErc20TokenData(asset: AssetInfo): Erc20TokenData? {
        require(asset.isErc20())
        require(asset.l2identifier != null)
        val name = asset.networkTicker.lowercase()

        return getEthWallet()?.getErc20TokenData(name)
    }

    val requireSecondPassword: Boolean
        get() = payloadDataManager.isDoubleEncrypted

    // Exposing it for ERC20 and for testing
    fun extraGasLimitForMemo() = extraGasLimitForMemo

    companion object {
        // To account for the extra data we want to send
        private val extraGasLimitForMemo: BigInteger = 600.toBigInteger()
        const val ETH_CHAIN_ID: Int = 1
        val ethChain: EvmNetwork = EvmNetwork(
            CryptoCurrency.ETHER.networkTicker,
            CryptoCurrency.ETHER.name,
            ETH_CHAIN_ID,
            EthUrls.ETH_NODES
        )
    }
}
