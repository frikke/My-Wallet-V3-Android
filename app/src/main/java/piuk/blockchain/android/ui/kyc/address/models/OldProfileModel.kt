package piuk.blockchain.android.ui.kyc.address.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class OldProfileModel(
    val firstName: String,
    val lastName: String,
    val countryCode: String,
    val stateCode: String?,
    val stateName: String?,
    val addressDetails: OldAddressDetailsModel? = null
) : Parcelable

@Parcelize
data class OldAddressDetailsModel(
    val address: String? = null,
    val postalCode: String? = null,
    val locality: String? = null
) : Parcelable
