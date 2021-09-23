package piuk.blockchain.android.ui.kyc.autocomplete

import piuk.blockchain.android.ui.base.mvi.MviIntent

sealed class KycAutocompleteAddressIntents : MviIntent<KycAutocompleteAddressState> {

    data class SelectAddress(val selectedAddress: KycAddressResult) : KycAutocompleteAddressIntents() {
        override fun reduce(oldState: KycAutocompleteAddressState) = oldState
    }

    data class UpdateSearchText(val addressSearchText: String, val countryCode: String) :
        KycAutocompleteAddressIntents() {
        override fun reduce(oldState: KycAutocompleteAddressState) = oldState
    }
}