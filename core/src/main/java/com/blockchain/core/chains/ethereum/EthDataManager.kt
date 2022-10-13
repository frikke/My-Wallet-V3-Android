package com.blockchain.core.chains.ethereum

import com.blockchain.api.ethereum.evm.FeeLevel
import com.blockchain.api.services.NonCustodialEvmService
import com.blockchain.core.chains.EvmNetwork
import com.blockchain.core.chains.EvmNetworksService
import com.blockchain.core.chains.erc20.isErc20
import com.blockchain.core.chains.ethereum.datastores.EthDataStore
import com.blockchain.core.chains.ethereum.models.CombinedEthModel
import com.blockchain.logging.LastTxUpdater
import com.blockchain.metadata.MetadataEntry
import com.blockchain.metadata.MetadataRepository
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.map
import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.api.data.FeeLimits
import info.blockchain.wallet.api.data.FeeOptions
import info.blockchain.wallet.ethereum.Erc20TokenData
import info.blockchain.wallet.ethereum.EthAccountApi
import info.blockchain.wallet.ethereum.EthUrls
import info.blockchain.wallet.ethereum.EthereumAccount
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
import java.math.BigDecimal
import java.math.BigInteger
import java.util.HashMap
import org.web3j.crypto.RawTransaction
import org.web3j.utils.Convert
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.utils.extensions.applySchedulers
import piuk.blockchain.androidcore.utils.extensions.rxSingleOutcome

class EthDataManager(
    private val payloadDataManager: PayloadDataManager,
    private val ethAccountApi: EthAccountApi,
    private val defaultLabels: DefaultLabels,
    private val ethDataStore: EthDataStore,
    private val metadataRepository: MetadataRepository,
    private val lastTxUpdater: LastTxUpdater,
    private val evmNetworksService: EvmNetworksService,
    private val nonCustodialEvmService: NonCustodialEvmService
) : EthMessageSigner {

    val ehtAccount: EthereumAccount
        get() = ethDataStore.ethWallet?.account ?: throw IllegalStateException("Eth account is not initialised yet")

    private val internalAccountAddress: String?
        get() = ethDataStore.ethWallet?.account?.address

    val accountAddress: String
        get() = internalAccountAddress ?: throw Exception("No ETH address found")

    val supportedNetworks: Single<Set<EvmNetwork>>
        get() = evmNetworksService.getSupportedNetworks().map {
            it.plus(ethChain).toSet()
        }
            .onErrorReturn { setOf(ethChain) }

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

    suspend fun getBalance(nodeUrl: String = EthUrls.ETH_NODES): Outcome<Exception, BigInteger> {
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
        return save(ethDataStore.ethWallet!!.renameAccount(label))
    }

    /**
     * Returns the user's ETH account object if previously fetched.
     *
     * @return A nullable [CombinedEthModel] object
     */
    fun getEthResponseModel(): CombinedEthModel? = ethDataStore.ethAddressResponse

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
        rxSingleOutcome {
            ethAccountApi.getLatestBlockNumber(nodeUrl = nodeUrl)
        }.applySchedulers()

    fun isContractAddress(address: String, nodeUrl: String = EthUrls.ETH_NODES): Single<Boolean> =
        rxSingleOutcome {
            val addressWithPrefix = if (address.startsWith(EthUtils.PREFIX)) {
                address
            } else {
                "${EthUtils.PREFIX}$address"
            }
            ethAccountApi.postEthNodeRequest(
                nodeUrl = nodeUrl,
                requestType = RequestType.IS_CONTRACT,
                addressWithPrefix,
                EthJsonRpcRequest.defaultBlock
            ).map { response ->
                // In order to distinguish between these two addresses we need to call eth_getCode,
                // which will return contract code if it's a contract and nothing if it's a wallet
                response.result.removePrefix(EthUtils.PREFIX).isNotEmpty()
            }
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
    fun getTransactionNotes(hash: String): String? = ethDataStore.ethWallet?.getTxNotes()?.get(hash)

    /**
     * Puts a given note in the [HashMap] of transaction notes keyed to a transaction hash. This
     * information is then saved in the metadata service.
     *
     * @return A [Completable] object
     */
    fun updateTransactionNotes(hash: String, note: String): Completable {
        return Completable.fromCallable {
            save(ethDataStore.ethWallet!!.withUpdatedTxNotes(hash, note))
        }.applySchedulers()
    }

    internal fun updateErc20TransactionNotes(
        asset: AssetInfo,
        hash: String,
        note: String
    ): Completable {
        require(asset.isErc20())

        return Completable.defer {
            getErc20TokenData(asset)?.let {
                save(ethDataStore.ethWallet!!.updateTxNoteForErc20(hash, note, it))
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
        label: String = defaultLabels.getDefaultNonCustodialWalletLabel()
    ): Completable =
        fetchOrCreateEthereumWallet(label)
            .flatMapCompletable { (wallet, needsSave) ->
                ethDataStore.ethWallet = wallet
                if (needsSave) {
                    save(wallet)
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

    fun getNonce(nodeUrl: String = EthUrls.ETH_NODES): Single<BigInteger> = rxSingleOutcome {
        ethAccountApi.postEthNodeRequest(
            nodeUrl = nodeUrl,
            requestType = RequestType.GET_NONCE,
            accountAddress,
            EthJsonRpcRequest.defaultBlock
        )
            .map { response ->
                EthUtils.convertHexToBigInteger(response.result)
            }
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

    fun getFeesForEvmTx(l1Chain: String) = rxSingleOutcome {
        val defaultFeeForEvm = FeeOptions.defaultForEvm(l1Chain)
        nonCustodialEvmService.getFeeLevels(l1Chain).map { feeLevels ->
            FeeOptions(
                gasLimit = defaultFeeForEvm.gasLimit,
                regularFee = getFeeForLevel(
                    feeLevels = feeLevels,
                    feeLevel = FeeLevel.NORMAL,
                    defaultFeeForLevel = defaultFeeForEvm.regularFee
                ),
                gasLimitContract = defaultFeeForEvm.gasLimitContract,
                priorityFee = getFeeForLevel(
                    feeLevels = feeLevels,
                    feeLevel = FeeLevel.HIGH,
                    defaultFeeForLevel = defaultFeeForEvm.priorityFee
                ),
                limits = FeeLimits(
                    min = getFeeForLevel(
                        feeLevels = feeLevels,
                        feeLevel = FeeLevel.LOW,
                        defaultFeeForLevel = defaultFeeForEvm.limits?.min ?: DEFAULT_MIN_FEE
                    ),
                    max = getFeeForLevel(
                        feeLevels = feeLevels,
                        feeLevel = FeeLevel.HIGH,
                        defaultFeeForLevel = defaultFeeForEvm.limits?.max ?: DEFAULT_MAX_FEE
                    )
                )
            )
        }
    }
        .applySchedulers()

    private fun getFeeForLevel(feeLevels: Map<FeeLevel, BigDecimal>, feeLevel: FeeLevel, defaultFeeForLevel: Long) =
        feeLevels[feeLevel]?.let { Convert.fromWei(it, Convert.Unit.GWEI).toLong() } ?: defaultFeeForLevel

    fun pushEvmTx(signedTxBytes: ByteArray, l1Chain: String): Single<String> =
        rxSingleOutcome {
            nonCustodialEvmService.pushTransaction(EthUtils.decorateAndEncode(signedTxBytes), l1Chain)
        }.flatMap { response ->
            lastTxUpdater.updateLastTxTime()
                .onErrorComplete()
                .andThen(Single.just(response.txId))
        }
            .applySchedulers()

    private fun fetchOrCreateEthereumWallet(
        label: String
    ): Single<Pair<EthereumWallet, Boolean>> =
        metadataRepository.loadRawValue(MetadataEntry.METADATA_ETH).defaultIfEmpty("")
            .map { metadata ->
                val walletJson = if (metadata != "") metadata else null

                var ethWallet = EthereumWallet.load(walletJson)
                var needsSave = false

                if (ethWallet?.account == null || !ethWallet.account.ethAccountDto.isCorrect ||
                    ethWallet.account.ethAccountDto.publicKey == null
                ) {
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
                if (!ethWallet.account.isAddressChecksummed()) {
                    ethWallet = ethWallet.withCheckSummedAccount()
                    needsSave = true
                }

                ethWallet to needsSave
            }

    private fun save(wallet: EthereumWallet): Completable =
        metadataRepository.saveRawValue(
            wallet.toJson(),
            MetadataEntry.METADATA_ETH
        ).doOnComplete {
            ethDataStore.ethWallet = wallet
        }

    fun getErc20TokenData(asset: AssetInfo): Erc20TokenData? {
        require(asset.isErc20())
        require(asset.l2identifier != null)
        val name = asset.networkTicker.lowercase()

        return ethDataStore.ethWallet?.getErc20TokenData(name)
    }

    val requireSecondPassword: Boolean
        get() = payloadDataManager.isDoubleEncrypted

    // Exposing it for ERC20 and for testing
    fun extraGasLimitForMemo() = extraGasLimitForMemo

    companion object {
        // To account for the extra data we want to send
        private val extraGasLimitForMemo: BigInteger = 600.toBigInteger()
        const val ETH_CHAIN_ID: Int = 1
        private const val DEFAULT_MIN_FEE = 23L
        private const val DEFAULT_MAX_FEE = 26L
        val ethChain: EvmNetwork = EvmNetwork(
            networkTicker = CryptoCurrency.ETHER.networkTicker,
            networkName = CryptoCurrency.ETHER.name,
            chainId = ETH_CHAIN_ID,
            nodeUrl = EthUrls.ETH_NODES
        )
    }
}
