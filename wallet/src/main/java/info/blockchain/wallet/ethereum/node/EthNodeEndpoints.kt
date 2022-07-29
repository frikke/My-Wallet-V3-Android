package info.blockchain.wallet.ethereum.node

import com.blockchain.api.adapters.ApiException
import com.blockchain.outcome.Outcome
import info.blockchain.wallet.ethereum.EthUrls
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Url

internal interface EthNodeEndpoints {

    @Headers("Accept: application/json")
    @POST
    suspend fun processRequest(
        @Url nodeUrl: String = EthUrls.ETH_NODES,
        @Body request: EthJsonRpcRequest
    ): Outcome<ApiException, EthJsonRpcResponse>
}
