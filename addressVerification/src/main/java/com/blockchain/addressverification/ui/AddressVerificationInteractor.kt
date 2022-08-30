package com.blockchain.addressverification.ui

import android.graphics.Typeface
import android.text.style.StyleSpan
import com.blockchain.addressverification.domain.AddressVerificationService
import com.blockchain.addressverification.domain.model.AutocompleteAddress
import com.blockchain.addressverification.domain.model.AutocompleteAddressType
import com.blockchain.domain.common.model.CountryIso
import com.blockchain.domain.common.model.StateIso
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.outcome.map
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.TypeFilter
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FetchPlaceResponse
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.androidcore.utils.extensions.rxSingleOutcome

class AddressVerificationInteractor(
    val placesClientProvider: PlacesClientProvider,
    private val addressVerificationService: AddressVerificationService,
    private val loqateFeatureFlag: FeatureFlag,
) {

    fun searchForAddresses(
        searchText: String,
        countryIso: CountryIso,
        stateIso: StateIso?,
        containerId: String?
    ): Single<List<AutocompleteAddress>> =
        loqateFeatureFlag.enabled.flatMap { enabled ->
            if (enabled) searchForAddressesLoqate(searchText, countryIso, stateIso, containerId)
            else searchForAddressesGooglePlaces(searchText, countryIso)
        }

    private fun searchForAddressesLoqate(
        searchText: String,
        countryIso: CountryIso,
        stateIso: StateIso?,
        containerId: String?
    ): Single<List<AutocompleteAddress>> = rxSingleOutcome {
        addressVerificationService.getAutocompleteAddresses(searchText, countryIso, stateIso, containerId)
    }

    private fun searchForAddressesGooglePlaces(
        searchText: String,
        countryCode: String
    ): Single<List<AutocompleteAddress>> {
        val client = placesClientProvider.getClient()
        val token = AutocompleteSessionToken.newInstance()

        return Single.create { emitter ->
            val request =
                FindAutocompletePredictionsRequest.builder()
                    .setCountries(countryCode)
                    .setTypeFilter(TypeFilter.ADDRESS)
                    .setSessionToken(token)
                    .setQuery(searchText)
                    .build()
            client.findAutocompletePredictions(request)
                .addOnSuccessListener { response: FindAutocompletePredictionsResponse ->
                    val results = response.autocompletePredictions.map {
                        val primary = it.getPrimaryText(StyleSpan(Typeface.NORMAL)).toString()
                        val secondary = it.getSecondaryText(StyleSpan(Typeface.NORMAL)).toString()

                        val primaryStartHighlight = primary.indexOf(searchText, ignoreCase = true)
                        val primaryEndHighlight = primaryStartHighlight + searchText.length
                        val primaryHighlightRange = primaryStartHighlight until primaryEndHighlight

                        val secondaryStartHighlight = secondary.indexOf(searchText, ignoreCase = true)
                        val secondaryEndHighlight = secondaryStartHighlight + searchText.length
                        val secondaryHighlightRange = secondaryStartHighlight until secondaryEndHighlight

                        AutocompleteAddress(
                            id = it.placeId,
                            type = AutocompleteAddressType.ADDRESS,
                            title = primary,
                            titleHighlightRanges = listOf(primaryHighlightRange),
                            description = secondary,
                            descriptionHighlightRanges = listOf(secondaryHighlightRange),
                            containedAddressesCount = null,
                        )
                    }
                    emitter.onSuccess(results)
                }.addOnFailureListener {
                    emitter.onError(it)
                }
        }
    }

    fun getAddressDetails(placeId: String): Single<AddressDetailsModel> = loqateFeatureFlag.enabled.flatMap { enabled ->
        if (enabled) getAddressDetailsLoqate(placeId)
        else getAddressDetailsGooglePlaces(placeId)
    }

    private fun getAddressDetailsLoqate(placeId: String): Single<AddressDetailsModel> = rxSingleOutcome {
        addressVerificationService.getCompleteAddress(placeId)
            .map { address ->
                AddressDetailsModel(
                    address = address.line1,
                    postalCode = address.postalCode,
                    locality = address.city,
                    stateIso = address.provinceCode,
                )
            }
    }

    private fun getAddressDetailsGooglePlaces(placeId: String): Single<AddressDetailsModel> {
        val client = placesClientProvider.getClient()
        val token = AutocompleteSessionToken.newInstance()

        return Single.create { emitter ->
            val placeFields = listOf(Place.Field.ADDRESS, Place.Field.ADDRESS_COMPONENTS, Place.Field.LAT_LNG)
            val request =
                FetchPlaceRequest.builder(placeId, placeFields)
                    .setSessionToken(token)
                    .build()
            client.fetchPlace(request)
                .addOnSuccessListener { response: FetchPlaceResponse ->
                    val addressComponents = response.place.addressComponents?.asList()
                    val postalCode = addressComponents?.find {
                        it.types.contains(POSTAL_CODE)
                    }

                    val locality = addressComponents?.find {
                        it.types.contains(LOCALITY)
                    }

                    val streetNumber = addressComponents?.find {
                        it.types.contains(STREET_NUMBER)
                    }?.name.orEmpty()

                    val route = addressComponents?.find {
                        it.types.contains(ROUTE)
                    }?.name.orEmpty()

                    emitter.onSuccess(
                        AddressDetailsModel(
                            address = "$streetNumber $route", postalCode = postalCode?.name, locality = locality?.name
                        )
                    )
                }
                .addOnFailureListener {
                    emitter.onError(it)
                }
        }
    }

    companion object {
        private const val POSTAL_CODE = "postal_code"
        private const val LOCALITY = "locality"
        private const val STREET_NUMBER = "street_number"
        private const val ROUTE = "route"
    }
}
