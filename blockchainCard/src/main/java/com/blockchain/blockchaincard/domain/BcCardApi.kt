package com.blockchain.blockchaincard.domain

import io.reactivex.rxjava3.core.Single
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface BcCardApi {

    @GET("products")
    fun getProducts(
        @Header("authorization") authorization: String,
    ): Single<List<ProductsResponse>>

    @GET("cards")
    fun getCards(
        @Header("authorization") authorization: String,
    ): Single<List<CardsResponse>>

    @POST("cards")
    fun createCard(
        @Header("authorization") authorization: String,
        @Body cardCreationRequest: CardCreationRequestBody
    ): Single<CardsResponse>

}

@Serializable
data class ProductsResponse(
    @SerialName("productCode")
    val productCode: String,
    @SerialName("price")
    val price: Price,
    @SerialName("brand")
    val brand: String,
    @SerialName("type")
    val type: String
)

@Serializable
data class CardsResponse(
    @SerialName("cardId")
    val cardId: String,

    @SerialName("type")
    val type: String,

    @SerialName("last4")
    val last4: String,

    @SerialName("expiry")
    val expiry: String,

    @SerialName("brand")
    val brand: String,

    @SerialName("cardStatus")
    val cardStatus: String,

    @SerialName("createdAt")
    val createdAt: String
)

@Serializable
data class Price(
    @SerialName("symbol")
    val symbol: String,
    @SerialName("value")
    val value: String,
)

@Serializable
class CardCreationRequestBody(
    val productCode: String,
    val ssn: String
)
