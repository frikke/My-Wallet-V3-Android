package com.blockchain.core.user

import com.blockchain.api.services.ContactPreference
import com.blockchain.api.services.ContactPreferenceUpdate
import com.blockchain.api.services.NabuUserService
import com.blockchain.auth.AuthHeaderProvider
import com.blockchain.nabu.models.responses.nabu.KycTiers
import com.blockchain.nabu.service.TierService
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

interface NabuUserDataManager {

    fun tiers(): Single<KycTiers>

    fun saveUserInitialLocation(countryIsoCode: String, stateIsoCode: String?): Completable

    fun getContactPreferences(): Single<List<ContactPreference>>

    fun updateContactPreferences(updates: List<ContactPreferenceUpdate>): Completable
}

class NabuUserDataManagerImpl(
    private val nabuUserService: NabuUserService,
    private val authenticator: AuthHeaderProvider,
    private val tierService: TierService,
) : NabuUserDataManager {

    override fun tiers(): Single<KycTiers> = tierService.tiers()

    override fun saveUserInitialLocation(countryIsoCode: String, stateIsoCode: String?): Completable =
        authenticator.getAuthHeader().map {
            nabuUserService.saveUserInitialLocation(
                it,
                countryIsoCode,
                stateIsoCode
            )
        }.flatMapCompletable { it }

    override fun getContactPreferences(): Single<List<ContactPreference>> =
        authenticator.getAuthHeader().flatMap {
            nabuUserService.getContactPreferences(it)
        }

    override fun updateContactPreferences(updates: List<ContactPreferenceUpdate>) =
        authenticator.getAuthHeader().flatMapCompletable {
            nabuUserService.updateContactPreferences(it, updates)
        }
}
