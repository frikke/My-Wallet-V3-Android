package piuk.blockchain.android.ui.kyc.autocomplete

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.koin.scopedInject
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.TypeFilter
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse
import com.google.android.libraries.places.api.net.PlacesClient
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.FragmentKycAutocompleteBinding
import piuk.blockchain.android.ui.base.mvi.MviFragment
import piuk.blockchain.android.ui.kyc.address.KycHomeAddressFragmentArgs
import piuk.blockchain.android.ui.kyc.address.models.AddressModel
import piuk.blockchain.android.ui.kyc.profile.models.ProfileModel
import piuk.blockchain.android.util.AfterTextChangedWatcher
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy

class KycAutocompleteAddressFragment :
    MviFragment<KycAutocompleteAddressModel, KycAutocompleteAddressIntents, KycAutocompleteAddressState, FragmentKycAutocompleteBinding>() {

    override val model: KycAutocompleteAddressModel by scopedInject()

    val profileModel: ProfileModel by unsafeLazy {
        KycAutocompleteAddressFragmentArgs.fromBundle(arguments ?: Bundle()).profileModel
    }

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentKycAutocompleteBinding =
        FragmentKycAutocompleteBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Places.initialize(requireActivity().applicationContext, "AIzaSyCcMKbA10qBzIx8jT3rCFXNJAfbpzEwHNo")
        val placesClient = Places.createClient(requireContext())
        binding.fieldAddress.addTextChangedListener(object: AfterTextChangedWatcher() {
            override fun afterTextChanged(s: Editable?) {
                val token = AutocompleteSessionToken.newInstance()

                // Use the builder to create a FindAutocompletePredictionsRequest.
                val request =
                    FindAutocompletePredictionsRequest.builder()
                        .setCountries(profileModel.countryCode)
                        .setTypeFilter(TypeFilter.ADDRESS)
                        .setSessionToken(token)
                        .setQuery(s?.toString())
                        .build()
                placesClient.findAutocompletePredictions(request)
                    .addOnSuccessListener { response: FindAutocompletePredictionsResponse ->
                        for (prediction in response.autocompletePredictions) {
                            Log.i("", prediction.placeId)
                        }
                    }.addOnFailureListener { exception: Exception? ->
                        if (exception is ApiException) {
                        }
                    }

            }
        })
    }

    override fun render(newState: KycAutocompleteAddressState) {

    }
}