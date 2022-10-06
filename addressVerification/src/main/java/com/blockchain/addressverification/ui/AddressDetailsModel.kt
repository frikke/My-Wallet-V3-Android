package com.blockchain.addressverification.ui

import android.os.Parcelable
import com.blockchain.domain.common.model.StateIso
import kotlinx.parcelize.Parcelize

// TODO(aromano): remove when loqate ff is removed and use CompleteAddress data class instead
@Parcelize
data class AddressDetailsModel(
    val address: String? = null,
    val postalCode: String? = null,
    val locality: String? = null,
    val stateIso: StateIso? = null,
) : Parcelable
