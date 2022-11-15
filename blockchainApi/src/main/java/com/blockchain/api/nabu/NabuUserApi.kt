package com.blockchain.api.nabu

import com.blockchain.api.nabu.data.GeolocationResponse
import com.blockchain.api.nabu.data.InitialAddressRequest
import com.blockchain.api.nabu.data.contactpreferences.ContactPreferencesResponse
import com.blockchain.api.nabu.data.contactpreferences.PreferenceUpdates
import com.blockchain.network.interceptor.AuthenticationNotRequired
import com.blockchain.outcome.Outcome
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

interface NabuUserApi {
    @PUT("users/current/address/initial")
    fun saveUserInitialLocation(
        @Body initialAddressRequest: InitialAddressRequest
    ): Completable

    @GET("geolocation")
    @AuthenticationNotRequired
    suspend fun getUserGeolocation(): Outcome<Exception, GeolocationResponse>

    @GET("users/contact-preferences")
    fun getContactPreferences(): Single<ContactPreferencesResponse>

    @PUT("users/contact-preferences")
    fun updateContactPreference(
        @Body preferences: PreferenceUpdates
    ): Completable
}
