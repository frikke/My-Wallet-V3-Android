package com.blockchain.nabu.api.getuser.data

import com.blockchain.analytics.TraitsService
import com.blockchain.extensions.filterNotNullValues
import com.blockchain.preferences.CountryPrefs
import com.blockchain.walletmode.WalletMode

class UserCountryRepository(private val countryPrefs: Lazy<CountryPrefs>) : TraitsService {
    override suspend fun traits(): Map<String, String> {
        return mapOf("country" to countryPrefs.value.country.takeIf { it.isNotEmpty() }).filterNotNullValues()
    }
}
