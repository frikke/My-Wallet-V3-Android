package com.blockchain.api.addressverification

import com.blockchain.api.addressverification.model.AutocompleteAddressResponse
import com.blockchain.api.addressverification.model.CompleteAddressDto
import com.blockchain.outcome.Outcome
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface AddressVerificationApi {

    @GET("address-capture/find")
    suspend fun getAutocompleteAddresses(
        @Header("authorization") authToken: String, // FLAG_AUTH_REMOVAL
        @Query("text") searchQuery: String,
        @Query("country_code") countryIso: String?,
        @Query("province_code") stateIso: String?,
        @Query("id") containerId: String?,
    ): Outcome<Exception, AutocompleteAddressResponse>

    @GET("address-capture/retrieve")
    suspend fun getCompleteAddress(
        @Header("authorization") authToken: String, // FLAG_AUTH_REMOVAL
        @Query("id") id: String
    ): Outcome<Exception, CompleteAddressDto>
}
