package com.blockchain.core

import io.reactivex.rxjava3.core.Single
import java.util.Locale

// ISO 3166-1 alpha-2
typealias CountryIso = String

interface EligibilityDataManager {
    fun getCustodialEligibleCountries(): Single<List<CountryIso>>
}

class EligibilityDataManagerImpl : EligibilityDataManager {
    override fun getCustodialEligibleCountries(): Single<List<CountryIso>> = Single.just(
        Locale.getISOCountries()
            .toList()
            .filterNot { SANCTIONED_COUNTRIES_ISO.contains(it) }
    )

    companion object {
        private val SANCTIONED_COUNTRIES_ISO: List<CountryIso> = listOf("CU", "IR", "KP", "SY")
    }
}
