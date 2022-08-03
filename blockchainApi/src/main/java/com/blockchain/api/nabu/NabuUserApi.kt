package com.blockchain.api.nabu

import com.blockchain.api.nabu.data.InitialAddressRequest
import com.blockchain.api.nabu.data.contactpreferences.ContactPreferencesResponse
import com.blockchain.api.nabu.data.contactpreferences.PreferenceUpdates
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PUT

interface NabuUserApi {
    @PUT("users/current/address/initial")
    fun saveUserInitialLocation(
        @Header("authorization") authorization: String, // FLAG_AUTH_REMOVAL
        @Body initialAddressRequest: InitialAddressRequest
    ): Completable

    @GET("users/contact-preferences")
    fun getContactPreferences(
        @Header("authorization") authorization: String // FLAG_AUTH_REMOVAL
    ): Single<ContactPreferencesResponse>

    @PUT("users/contact-preferences")
    fun updateContactPreference(
        @Header("authorization") authorization: String, // FLAG_AUTH_REMOVAL
        @Body preferences: PreferenceUpdates
    ): Completable
}
