package com.blockchain.domain.eligibility.model

import com.blockchain.domain.common.model.CountryIso
import com.blockchain.domain.common.model.StateIso
import kotlinx.serialization.Serializable

@Serializable
sealed class Region {

    abstract val countryCode: CountryIso
    abstract val name: String
    abstract val isKycAllowed: Boolean

    @Serializable
    data class Country(
        override val countryCode: CountryIso,
        override val name: String,
        override val isKycAllowed: Boolean,
        val states: List<StateIso>
    ) : Region()

    @Serializable
    data class State(
        override val countryCode: CountryIso,
        override val name: String,
        override val isKycAllowed: Boolean,
        val stateCode: StateIso
    ) : Region()
}
