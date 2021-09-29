package info.blockchain.wallet.api

import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import retrofit2.http.GET
import info.blockchain.wallet.api.data.WalletOptions
import info.blockchain.wallet.api.data.SignedToken
import info.blockchain.wallet.api.WalletApi.IPResponse
import info.blockchain.wallet.api.data.Settings
import info.blockchain.wallet.api.data.Status
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.Header
import retrofit2.http.HeaderMap
import retrofit2.http.Path
import retrofit2.http.Query

interface WalletExplorerEndpoints {
    @FormUrlEncoded
    @POST("wallet")
    fun postToWallet(
        @Field("method") method: String,
        @Field("guid") guid: String,
        @Field("sharedKey") sharedKey: String,
        @Field("payload") payload: String,
        @Field("length") length: Int,
        @Field("api_code") apiCode: String
    ): Observable<ResponseBody>

    @FormUrlEncoded
    @POST("wallet")
    fun postSecureChannel(
        @Field("method") method: String,
        @Field("payload") payload: String,
        @Field("length") length: Int,
        @Field("api_code") apiCode: String
    ): Observable<ResponseBody>

    @FormUrlEncoded
    @POST("wallet")
    fun fetchSettings(
        @Field("method") method: String,
        @Field("guid") guid: String,
        @Field("sharedKey") sharedKey: String,
        @Field("format") format: String,
        @Field("api_code") apiCode: String
    ): Observable<Settings>

    @FormUrlEncoded
    @POST("wallet")
    fun updateSettings(
        @Field("method") method: String,
        @Field("guid") guid: String,
        @Field("sharedKey") sharedKey: String,
        @Field("payload") payload: String,
        @Field("length") length: Int,
        @Field("format") format: String,
        @Field("context") context: String?,
        @Field("api_code") apiCode: String
    ): Observable<ResponseBody>

    @FormUrlEncoded
    @POST("wallet")
    fun fetchWalletData(
        @Field("method") method: String,
        @Field("guid") guid: String,
        @Field("sharedKey") sharedKey: String,
        @Field("format") format: String,
        @Field("api_code") apiCode: String
    ): Call<ResponseBody>

    @FormUrlEncoded
    @POST("wallet")
    fun submitTwoFactorCode(
        @HeaderMap headers: Map<String, String>?,
        @Field("method") method: String,
        @Field("guid") guid: String?,
        @Field("payload") twoFactorCode: String,
        @Field("length") length: Int,
        @Field("format") format: String,
        @Field("api_code") apiCode: String
    ): Observable<ResponseBody>

    @FormUrlEncoded
    @POST("wallet")
    fun submitCoinReceiveAddresses(
        @Field("method") method: String,
        @Field("sharedKey") sharedKey: String,
        @Field("guid") guid: String,
        @Field("coin-addresses") coinAddresses: String
    ): Observable<ResponseBody>

    @FormUrlEncoded
    @POST("wallet")
    fun syncWalletCall(
        @Field("method") method: String,
        @Field("guid") guid: String?,
        @Field("sharedKey") sharedKey: String?,
        @Field("payload") payload: String,
        @Field("length") length: Int,
        @Field("checksum") checksum: String,
        @Field("active") active: String?,
        @Field("email") email: String?,
        @Field("device") device: String?,
        @Field("old_checksum") old_checksum: String?,
        @Field("api_code") apiCode: String
    ): Call<ResponseBody>

    @FormUrlEncoded
    @POST("wallet")
    fun fetchPairingEncryptionPasswordCall(
        @Field("method") method: String,
        @Field("guid") guid: String?,
        @Field("api_code") apiCode: String
    ): Call<ResponseBody>

    @FormUrlEncoded
    @POST("wallet")
    fun fetchPairingEncryptionPassword(
        @Field("method") method: String,
        @Field("guid") guid: String?,
        @Field("api_code") apiCode: String
    ): Observable<ResponseBody>

    @GET("wallet/{guid}?format=json&resend_code=false")
    fun getSessionId(
        @Path("guid") guid: String
    ): Observable<Response<ResponseBody>>

    @GET("wallet/{guid}")
    fun fetchEncryptedPayload(
        @Path("guid") guid: String,
        @Header("cookie") sessionId: String,
        @Query("format") format: String,
        @Query("resend_code") resendCode: Boolean,
        @Query("api_code") apiCode: String
    ): Observable<Response<ResponseBody>>

    @POST("pin-store")
    fun pinStore(
        @Query("key") key: String,
        @Query("pin") pin: String,
        @Query("value") value: String?,
        @Query("method") method: String,
        @Query("api_code") apiCode: String
    ): Observable<Response<Status>>

    @GET("Resources/wallet-options.json")
    fun getWalletOptions(
        @Query("api_code") apiCode: String
    ): Observable<WalletOptions>

    @GET("wallet/signed-token")
    fun getSignedJsonToken(
        @Query("guid") guid: String,
        @Query("sharedKey") sharedKey: String,
        @Query("fields") fields: String,
        @Query("partner") partner: String?,
        @Query("api_code") apiCode: String
    ): Single<SignedToken>

    @GET("wallet/get-ip")
    fun getExternalIp(): Single<IPResponse>

    @FormUrlEncoded @POST("wallet/sessions")
    fun createSessionId(
        @Field("email") email: String,
        @Field("api_code") apiCode: String
    ): Single<ResponseBody>

    @FormUrlEncoded
    @POST("wallet")
    fun authorizeSession(
        @Header("Authorization") sessionId: String,
        @Field("token") authCode: String,
        @Field("api_code") apiCode: String,
        @Field("method") method: String,
        @Field("confirm_approval") confirmApproval: Boolean
    ): Single<Response<ResponseBody>>

    @FormUrlEncoded
    @POST("wallet")
    fun sendEmailForVerification(
        @Header("Authorization") sessionId: String,
        @Field("method") method: String,
        @Field("api_code") apiCode: String,
        @Field("email") email: String,
        @Field("captcha") captcha: String,
        @Field("siteKey") siteKey: String
    ): Single<ResponseBody>

    @FormUrlEncoded
    @POST("wallet")
    fun updateMobileSetup(
        @Field("method") method: String,
        @Field("guid") guid: String,
        @Field("sharedKey") sharedKey: String,
        @Field("is_mobile_setup") isMobileSetup: Boolean,
        @Field("mobile_device_type") deviceType: Int
    ): Single<ResponseBody>

    @FormUrlEncoded
    @POST("wallet")
    fun updateMnemonicBackup(
        @Field("method") method: String,
        @Field("guid") guid: String,
        @Field("sharedKey") sharedKey: String
    ): Single<ResponseBody>

    @FormUrlEncoded
    @POST("wallet")
    fun verifyCloudBackup(
        @Field("method") method: String,
        @Field("guid") guid: String,
        @Field("sharedKey") sharedKey: String,
        @Field("has_cloud_backup") isMobileSetup: Boolean,
        @Field("mobile_device_type") deviceType: Int
    ): Single<ResponseBody>
}