package piuk.blockchain.android.ui.kyc.autocomplete

import piuk.blockchain.android.ui.base.mvi.MviState

data class KycAutocompleteAddressState(
    val addressSearchText: String = "",
    val foundAddresses: List<String> = emptyList(),
    val selectedAddress: String? = null
) : MviState