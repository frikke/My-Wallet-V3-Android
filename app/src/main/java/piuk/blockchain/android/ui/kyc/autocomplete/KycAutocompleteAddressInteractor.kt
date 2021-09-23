package piuk.blockchain.android.ui.kyc.autocomplete

import android.graphics.Typeface
import android.text.style.StyleSpan
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.TypeFilter
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FetchPlaceResponse
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.android.ui.kyc.navigate
import piuk.blockchain.android.ui.kyc.profile.models.AddressDetailsModel
import piuk.blockchain.android.ui.kyc.profile.models.ProfileModel

class KycAutocompleteAddressInteractor(val placesManager: PlacesManager) {

    fun searchForAddresses(searchText: String, countryCode: String): Single<List<KycAddressResult>> {
        return Single.create { emitter ->
            val request =
                FindAutocompletePredictionsRequest.builder()
                    .setCountries(countryCode)
                    .setTypeFilter(TypeFilter.ADDRESS)
                    .setSessionToken(placesManager.token)
                    .setQuery(searchText)
                    .build()
            placesManager.client.findAutocompletePredictions(request)
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
        return Single.create { emitter ->
            val placeFields = listOf(Place.Field.ADDRESS, Place.Field.ADDRESS_COMPONENTS, Place.Field.LAT_LNG)
            val request =
                FetchPlaceRequest.builder(placeId, placeFields)
                    .setSessionToken(placesManager.token)
                    .build()
            placesManager.client.fetchPlace(request)
                .addOnSuccessListener { response: FetchPlaceResponse ->
                    val addressComponents = response.place.addressComponents?.asList()
                    val postalCode = addressComponents?.find {
                        it.types.contains("postal_code")
                    }

                    val locality = addressComponents?.find {
                        it.types.contains("locality")
                    }

                    val streetNumber = addressComponents?.find {
                        it.types.contains("street_number")
                    }?.name ?: ""

                    val route = addressComponents?.find {
                        it.types.contains("route")
                    }?.name ?: ""

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
}