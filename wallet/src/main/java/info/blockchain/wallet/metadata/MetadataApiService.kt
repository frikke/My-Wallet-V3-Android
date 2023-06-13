package info.blockchain.wallet.metadata

import info.blockchain.wallet.metadata.data.Auth
import info.blockchain.wallet.metadata.data.MetadataBody
import info.blockchain.wallet.metadata.data.MetadataResponse
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import java.util.HashMap
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface MetadataApiService {
    // AUTH
    @GET(MetadataUrls.AUTH)
    fun nonce(): Call<Any>

    @POST(MetadataUrls.AUTH)
    fun getToken(@Body body: HashMap<String, String>): Call<Auth>

    // CRUD OPERATIONS
    @PUT(MetadataUrls.METADATA + "/{addr}")
    fun putMetadata(
        @Path("addr") address: String,
        @Body body: MetadataBody
    ): Completable

    @GET(MetadataUrls.METADATA + "/{addr}")
    fun getMetadata(@Path("addr") address: String): Single<MetadataResponse>
}
