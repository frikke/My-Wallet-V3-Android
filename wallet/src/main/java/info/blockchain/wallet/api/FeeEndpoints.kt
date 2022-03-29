package info.blockchain.wallet.api

import info.blockchain.wallet.api.data.FeeOptions
import io.reactivex.rxjava3.core.Observable
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface FeeEndpoints {
    @GET("mempool/fees/btc")
    fun getBtcFeeOptions(): Observable<FeeOptions>

    @GET("mempool/fees/eth")
    fun getEthFeeOptions(): Observable<FeeOptions>

    @GET("mempool/fees/bch")
    fun getBchFeeOptions(): Observable<FeeOptions>

    @GET("mempool/fees/{currency}")
    fun getFeeOptions(@Path("currency") currency: String?): Observable<FeeOptions>

    @GET("mempool/fees/eth")
    fun getErc20FeeOptions(
        @Query("contractAddress") contractAddress: String?
    ): Observable<FeeOptions>
}
