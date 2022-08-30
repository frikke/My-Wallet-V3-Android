package com.blockchain.addressverification.ui

import android.os.Parcelable
import com.blockchain.domain.common.model.CountryIso
import com.blockchain.domain.common.model.StateIso
import kotlinx.parcelize.Parcelize

@Parcelize
data class AddressDetails(
    val firstLine: String,
    val secondLine: String?,
    val city: String,
    val postCode: String,
    val countryIso: CountryIso,
    val stateIso: StateIso?,
) : Parcelable
