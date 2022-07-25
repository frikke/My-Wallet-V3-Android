package piuk.blockchain.android.ui.kyc.autocomplete

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.commonarch.presentation.mvi.MviFragment
import com.blockchain.componentlib.alert.BlockchainSnackbar
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.componentlib.viewextensions.DEBOUNCE_TIMEOUT
import com.blockchain.componentlib.viewextensions.listenForTextChanges
import com.blockchain.componentlib.viewextensions.visibleIf
import com.blockchain.koin.scopedInject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.FragmentKycAutocompleteBinding
import piuk.blockchain.android.ui.kyc.ParentActivityDelegate
import piuk.blockchain.android.ui.kyc.navhost.KycProgressListener
import piuk.blockchain.android.ui.kyc.navigate
import piuk.blockchain.android.ui.kyc.profile.models.AddressDetailsModel
import piuk.blockchain.android.ui.kyc.profile.models.ProfileModel
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy

class KycAutocompleteAddressFragment :
    MviFragment<KycAutocompleteAddressModel, KycAutocompleteAddressIntents, KycAutocompleteAddressState,
        FragmentKycAutocompleteBinding>() {

    override val model: KycAutocompleteAddressModel by scopedInject()

    val profileModel: ProfileModel by unsafeLazy {
        KycAutocompleteAddressFragmentArgs.fromBundle(arguments ?: Bundle()).profileModel
    }

    private val progressListener: KycProgressListener by ParentActivityDelegate(
        this
    )

    private val adapter = KycAutocompleteAddressAdapter(onClick = this::onSearchResultClicked)

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentKycAutocompleteBinding =
        FragmentKycAutocompleteBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        progressListener.setupHostToolbar(R.string.kyc_address_title)
        setupRecyclerView()

        viewLifecycleOwner.lifecycleScope.launch {
            setupSearch()
        }

        binding.enterManuallyButton.setOnClickListener {
            navigateToAddress(null)
        }
    }

    private fun setupRecyclerView() {
        binding.searchResults.apply {
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            adapter = this@KycAutocompleteAddressFragment.adapter
            itemAnimator = null
        }
    }

    @OptIn(FlowPreview::class)
    private suspend fun setupSearch() =
        binding.fieldAddress.listenForTextChanges()
            .debounce(DEBOUNCE_TIMEOUT) // debounce every 500ms to prevent excessive API calls
            .collectLatest { query ->
                model.process(
                    KycAutocompleteAddressIntents.UpdateSearchText(
                        addressSearchText = query?.toString().orEmpty(),
                        countryCode = profileModel.countryCode
                    )
                )
            }

    private fun onSearchResultClicked(result: KycAddressResult) {
        model.process(KycAutocompleteAddressIntents.SelectAddress(result))
    }

    private fun navigateToAddress(addressDetails: AddressDetailsModel?) {
        navigate(
            KycAutocompleteAddressFragmentDirections
                .actionKycAutocompleteAddressFragmentToKycHomeAddressFragment(
                    profileModel.copy(addressDetails = addressDetails)
                )
        )
    }

    override fun render(newState: KycAutocompleteAddressState) {
        when (val step = newState.autocompleteAddressStep) {
            is AutocompleteAddressStep.Address -> navigateToAddress(step.addressDetailsModel)
            null -> {}
        }

        binding.enterManuallyButton.visibleIf {
            newState.shouldShowManualButton
        }

        adapter.submitList(newState.addresses)

        when (newState.toastType) {
            AutocompleteAddressToastType.ADDRESSES_ERROR -> {
                BlockchainSnackbar.make(
                    binding.root,
                    getString(R.string.kyc_autocomplete_addresses_error),
                    type = SnackbarType.Error
                ).show()
            }
            AutocompleteAddressToastType.SELECTED_ADDRESS_ERROR -> {
                BlockchainSnackbar.make(
                    binding.root,
                    getString(R.string.kyc_autocomplete_selected_address_error),
                    type = SnackbarType.Error
                ).show()
            }
            null -> {}
        }
    }
}
