package piuk.blockchain.android.ui.kyc.autocomplete

import android.graphics.Typeface
import android.text.style.StyleSpan
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.TypeFilter
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FetchPlaceResponse
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.android.ui.kyc.profile.models.AddressDetailsModel

class KycAutocompleteAddressInteractor(val placesClientProvider: PlacesClientProvider) {

    fun searchForAddresses(searchText: String, countryCode: String): Single<List<KycAddressResult>> {
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
                        KycAddressResult(it.getFullText(StyleSpan(Typeface.NORMAL)).toString(), searchText, it.placeId)
                    }
                    emitter.onSuccess(results)
                }.addOnFailureListener {
                    emitter.onError(it)
                }
        }
    }

    fun getAddressDetails(placeId: String): Single<AddressDetailsModel> {
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