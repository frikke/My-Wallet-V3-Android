package info.blockchain.wallet.ethereum

import com.blockchain.outcome.Outcome
import info.blockchain.wallet.ethereum.node.EthChainError
import info.blockchain.wallet.ethereum.node.EthJsonRpcRequest
import info.blockchain.wallet.ethereum.node.EthJsonRpcResponse
import info.blockchain.wallet.ethereum.node.EthTransactionResponse
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

internal interface EthNodeEndpoints {

    @Headers("Accept: application/json")
    @POST(EthUrls.ETH_NODES)
    suspend fun processRequest(@Body request: EthJsonRpcRequest): Outcome<EthChainError, EthJsonRpcResponse>

    @Headers("Accept: application/json")
    @POST(EthUrls.ETH_NODES)
    suspend fun getTransaction(@Body request: EthJsonRpcRequest): Outcome<EthChainError, EthTransactionResponse>
}
