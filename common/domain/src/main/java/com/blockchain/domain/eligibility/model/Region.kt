package com.blockchain.domain.eligibility.model

import com.blockchain.domain.common.model.CountryIso
import com.blockchain.domain.common.model.StateIso

sealed class Region(
    open val countryCode: CountryIso,
    open val name: String,
    open val isKycAllowed: Boolean
) {
    data class Country(
        override val countryCode: CountryIso,
        override val name: String,
        override val isKycAllowed: Boolean,
        val states: List<StateIso>
    ) : Region(countryCode, name, isKycAllowed)

    data class State(
        override val countryCode: CountryIso,
        override val name: String,
        override val isKycAllowed: Boolean,
        val stateCode: StateIso
    ) : Region(countryCode, name, isKycAllowed)
}
