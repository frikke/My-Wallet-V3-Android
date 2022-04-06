package info.blockchain.wallet.ethereum

import info.blockchain.wallet.ethereum.data.EthAddressResponse
import info.blockchain.wallet.ethereum.data.EthTransactionsResponse
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path
import retrofit2.http.Query

internal interface EthEndpoints {
    @GET(EthUrls.ACCOUNT + "/{address}")
    fun getEthAccount(@Path("address") address: String): Observable<HashMap<String, EthAddressResponse>>

    @GET("${EthUrls.V2_DATA_ACCOUNT}/{address}/transactions")
    @Headers("Accept: application/json")
    fun getTransactions(
        @Path("address") address: String,
        @Query("size") size: Int = 50
    ): Single<EthTransactionsResponse>
}
