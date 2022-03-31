package com.blockchain.api.blockchainCard

import com.blockchain.api.blockchainCard.data.CardCreationRequestBody
import com.blockchain.api.blockchainCard.data.CardsResponse
import com.blockchain.api.blockchainCard.data.ProductsResponse
import io.reactivex.rxjava3.core.Single
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

internal interface BlockchainCardApi {

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

    @DELETE("cards/{cardId}")
    fun deleteCard(
        @Path("cardId") cardId: String,
        @Header("authorization") authorization: String
    ): Single<CardsResponse>
}
