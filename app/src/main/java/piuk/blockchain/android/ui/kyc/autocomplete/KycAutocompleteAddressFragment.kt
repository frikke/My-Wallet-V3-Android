package piuk.blockchain.android.ui.kyc.autocomplete

import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.koin.scopedInject
import piuk.blockchain.android.databinding.FragmentKycAutocompleteBinding
import piuk.blockchain.android.ui.base.mvi.MviFragment
import piuk.blockchain.android.ui.kyc.navigate
import piuk.blockchain.android.ui.kyc.profile.models.ProfileModel
import piuk.blockchain.android.util.AfterTextChangedWatcher
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy

class KycAutocompleteAddressFragment :
    MviFragment<KycAutocompleteAddressModel, KycAutocompleteAddressIntents, KycAutocompleteAddressState,
        FragmentKycAutocompleteBinding>() {

    override val model: KycAutocompleteAddressModel by scopedInject()

    val profileModel: ProfileModel by unsafeLazy {
        KycAutocompleteAddressFragmentArgs.fromBundle(arguments ?: Bundle()).profileModel
    }

    private val adapter = KycAutocompleteAddressAdapter(onClick = this::onSearchResultClicked)

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentKycAutocompleteBinding =
        FragmentKycAutocompleteBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSearch()
        model.actions.handleEvents(this, this::handleActions)
    }

    private fun setupRecyclerView() {
        binding.searchResults.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        binding.searchResults.adapter = adapter
        binding.searchResults.itemAnimator = null
    }

    private fun setupSearch() {
        binding.fieldAddress.addTextChangedListener(object : AfterTextChangedWatcher() {
            override fun afterTextChanged(s: Editable?) {
                model.process(
                    KycAutocompleteAddressIntents.UpdateSearchText(s?.toString() ?: "", profileModel.countryCode)
                )
            }
        })
    }

    private fun onSearchResultClicked(result: KycAddressResult) {
        model.process(KycAutocompleteAddressIntents.SelectAddress(result))
    }

    override fun render(newState: KycAutocompleteAddressState) {}

    private fun handleActions(action: KycAutocompleteAddressModel.Action) {
        when (action) {
            is KycAutocompleteAddressModel.Action.NavigateToAddressFragment -> {
                navigate(
                    KycAutocompleteAddressFragmentDirections
                        .actionKycAutocompleteAddressFragmentToKycHomeAddressFragment(
                            profileModel.copy(addressDetails = action.addressDetails)
                        )
                )
            }
            is KycAutocompleteAddressModel.Action.UpdateSearchList -> adapter.submitList(action.addresses)
        }
    }
}