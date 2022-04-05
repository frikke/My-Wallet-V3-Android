package info.blockchain.wallet.ethereum

import com.blockchain.outcome.Outcome
import com.blockchain.outcome.fold
import info.blockchain.wallet.ethereum.data.EthAddressResponse
import info.blockchain.wallet.ethereum.data.EthLatestBlock
import info.blockchain.wallet.ethereum.data.EthLatestBlockNumber
import info.blockchain.wallet.ethereum.data.EthTransaction
import info.blockchain.wallet.ethereum.node.EthChainError
import info.blockchain.wallet.ethereum.node.EthJsonRpcRequest
import info.blockchain.wallet.ethereum.node.EthJsonRpcResponse
import info.blockchain.wallet.ethereum.node.EthTransactionResponse
import info.blockchain.wallet.ethereum.node.RequestType
import info.blockchain.wallet.ethereum.util.EthUtils
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.rx3.rxSingle

class EthAccountApi internal constructor(
    private val ethEndpoints: EthEndpoints,
    private val ethNodeEndpoints: EthNodeEndpoints,
    private val apiCode: String
) {
    /**
     * Returns information about the latest block via a [EthLatestBlock] object.
     *
     * @return An [Single] wrapping an [EthLatestBlock]
     */
    val latestBlockNumber: Single<EthLatestBlockNumber>
        get() = rxSingle {
            ethNodeEndpoints.processRequest(
                request = EthJsonRpcRequest.create(
                    params = arrayOf(),
                    type = RequestType.LATEST_BLOCK_NUMBER
                )
            )
                .fold(
                    onSuccess = { response ->
                        EthLatestBlockNumber().apply {
                            number = EthUtils.convertHexToBigInteger(response.result)
                        }
                    },
                    onFailure = {
                        throw it.throwable
                    }
                )
        }

    /**
     * Returns an [EthAddressResponse] object for a list of given ETH addresses as an [ ].
     * An [EthAddressResponse] contains a list of transactions associated with
     * the accounts, as well as a final balance for each.
     *
     * @param addresses The ETH addresses to be queried
     * @return An [Observable] wrapping an [EthAddressResponse]
     */
    fun getEthAddress(addresses: List<String>): Observable<HashMap<String, EthAddressResponse>> {
        return ethEndpoints.getEthAccount(addresses.joinToString(","))
    }

    fun getEthTransactions(addresses: List<String>): Single<List<EthTransaction>> {
        return ethEndpoints.getTransactions(addresses.joinToString(",")).map { it.transactions }
    }

    fun getLastEthTransaction(addresses: List<String>): Maybe<EthTransaction> {
        return ethEndpoints.getTransactions(addresses.joinToString(","), 1)
            .flatMapMaybe {
                if (it.transactions.isNotEmpty())
                    Maybe.just(it.transactions[0])
                else Maybe.empty()
            }
    }

    /**
     * Returns an [EthTransactionResponse] containing information about a specific ETH transaction.
     *
     * @param hash The hash of the transaction you wish to check
     * @return An [Outcome] wrapping an [EthTransactionResponse]
     */
    suspend fun getTransaction(hash: String): Outcome<EthChainError, EthTransactionResponse> {
        return ethNodeEndpoints.getTransaction(
            request = EthJsonRpcRequest.create(
                params = arrayOf(hash),
                type = RequestType.GET_TRANSACTION
            )
        )
    }

    /**
     * Returns an [EthJsonRpcResponse] containing information about the specified ETH address or the blockchain itself.
     *
     * @param requestType The type of the request containing the name of the method to be called on the blockchain
     * @param params The parameters required for the request
     * @return An [Outcome] wrapping an [EthJsonRpcResponse]
     */
    suspend fun postEthNodeRequest(
        requestType: RequestType,
        vararg params: String
    ): Outcome<EthChainError, EthJsonRpcResponse> =
        ethNodeEndpoints.processRequest(
            request = EthJsonRpcRequest.create(
                params = params,
                type = requestType
            )
        )
}
