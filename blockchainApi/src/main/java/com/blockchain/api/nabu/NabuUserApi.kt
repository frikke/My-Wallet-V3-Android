package com.blockchain.api.nabu

import com.blockchain.api.nabu.data.InitialAddressRequest
import com.blockchain.api.nabu.data.InterestEligibilityResponse
import com.blockchain.api.nabu.data.Language
import com.blockchain.api.nabu.data.LatestTermsAndConditionsResponse
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PUT
import retrofit2.http.Query

interface NabuUserApi {

    @GET("eligible/product/savings")
    fun getInterestEligibility(
        @Header("authorization") authorization: String
    ): Single<Map<String, InterestEligibilityResponse>>

    @PUT("users/current/address/initial")
    fun saveUserInitialLocation(
        @Header("authorization") authorization: String,
        @Body initialAddressRequest: InitialAddressRequest
    ): Completable

    @GET("user/terms-and-conditions")
    fun getLatestTermsAndConditions(
        @Header("authorization") authorization: String,
        @Query("with-controls") withControls: Boolean = true
    ): Single<LatestTermsAndConditionsResponse>

    @PUT("user/terms-and-conditions/sign-latest")
    fun signLatestTermsAndConditions(
        @Header("authorization") authorization: String
    ): Completable

    @PUT("users/current/lang")
    fun reportLanguage(
        @Header("authorization") authorization: String,
        @Body language: Language
    ): Completable
}
