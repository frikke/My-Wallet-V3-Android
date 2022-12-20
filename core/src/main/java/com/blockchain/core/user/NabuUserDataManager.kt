package com.blockchain.core.user

import com.blockchain.api.services.ContactPreference
import com.blockchain.api.services.ContactPreferenceUpdate
import com.blockchain.api.services.NabuUserService
import com.blockchain.core.kyc.domain.KycService
import com.blockchain.domain.common.model.CountryIso
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.map
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

interface NabuUserDataManager {
    fun saveUserInitialLocation(countryIsoCode: String, stateIsoCode: String?): Completable

    suspend fun getUserGeolocation(): Outcome<Exception, CountryIso>

    fun getContactPreferences(): Single<List<ContactPreference>>

    fun updateContactPreferences(updates: List<ContactPreferenceUpdate>): Completable
}

class NabuUserDataManagerImpl(
    private val nabuUserService: NabuUserService,
    private val kycService: KycService,
) : NabuUserDataManager {

    override fun saveUserInitialLocation(countryIsoCode: String, stateIsoCode: String?): Completable =
        nabuUserService.saveUserInitialLocation(
            countryIsoCode,
            stateIsoCode
        )

    override suspend fun getUserGeolocation(): Outcome<Exception, CountryIso> =
        nabuUserService.getUserGeolocation().map { it.countryCode }

    override fun getContactPreferences(): Single<List<ContactPreference>> =
        nabuUserService.getContactPreferences()

    override fun updateContactPreferences(updates: List<ContactPreferenceUpdate>) =
        nabuUserService.updateContactPreferences(updates)
}
