package com.blockchain.api.services

import com.blockchain.api.nabu.NabuUserApi
import com.blockchain.api.nabu.data.InitialAddressRequest
import com.blockchain.api.nabu.data.InterestEligibilityResponse
import com.blockchain.api.nabu.data.LatestTermsAndConditionsResponse
import com.blockchain.api.nabu.data.contactpreferences.ContactPreferencesResponse
import com.blockchain.api.nabu.data.contactpreferences.NotificationMethod
import com.blockchain.api.nabu.data.contactpreferences.PreferenceUpdate
import com.blockchain.api.nabu.data.contactpreferences.PreferenceUpdates
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import java.util.Locale

data class InterestEligibility(
    private val eligibilityMap: Map<String, InterestEligibilityResponse>
) {
    fun getEligibleFor(assetTicker: String): InterestEligibilityResponse {
        return eligibilityMap[assetTicker.toUpperCase(Locale.ROOT)] ?: InterestEligibilityResponse(
            isEligible = false,
            reason = InterestEligibilityResponse.DEFAULT_FAILURE_REASON
        )
    }
}

// TODO: Add nabu User and User Capability calls to this service (and the underlying interface)
class NabuUserService internal constructor(
    private val api: NabuUserApi
) {
    fun getInterestEligibility(
        authHeader: String
    ): Single<InterestEligibility> =
        api.getInterestEligibility(authorization = authHeader)
            .onErrorReturn { emptyMap() }
            .map { it.toDomain() }

    fun saveUserInitialLocation(
        authHeader: String,
        countryIsoCode: String,
        stateIsoCode: String?
    ): Completable =
        api.saveUserInitialLocation(authHeader, InitialAddressRequest(countryIsoCode, stateIsoCode))

    fun getLatestTermsAndConditions(authHeader: String): Single<LatestTermsAndConditions> =
        api.getLatestTermsAndConditions(authHeader).map { it.toDomain() }

    fun signLatestTermsAndConditions(authHeader: String) = api.signLatestTermsAndConditions(authHeader)

    fun getContactPreferences(authHeader: String) = api.getContactPreferences(authHeader).map { it.toDomain() }

    fun updateContactPreferences(authHeader: String, preferenceUpdates: List<ContactPreferenceUpdate>) =
        api.updateContactPreference(
            authHeader,
            PreferenceUpdates(
                preferenceUpdates.map { it.toPreferenceUpdate() }
            )
        )
}

private fun Map<String, InterestEligibilityResponse>.toDomain(): InterestEligibility =
    InterestEligibility(this)

private fun LatestTermsAndConditionsResponse.toDomain() = LatestTermsAndConditions(termsAndConditionsUrl)

data class LatestTermsAndConditions(val termsAndConditionsUrl: String?)

private fun ContactPreferencesResponse.toDomain(): List<ContactPreference> {
    val availableMethods = mutableMapOf<String, NotificationMethod>()
    notificationMethods.forEach { availableMethods[it.method] = it }

    return preferences.map {
        ContactPreference(
            title = it.title,
            subtitle = it.subtitle,
            description = it.description,
            methods = createContactMethodList(
                availableMethods,
                it.requiredMethods,
                it.optionalMethods,
                it.enabledMethods
            ),
            channel = it.type
        )
    }
}

private fun createContactMethodList(
    availableMethods: MutableMap<String, NotificationMethod>,
    requiredMethods: List<String>,
    optionalMethods: List<String>,
    enabledMethods: List<String>
): List<ContactMethod> {
    val enabledSet = enabledMethods.toHashSet()

    return requiredMethods.mapNotNull { method ->
        availableMethods[method]?.toContactMethod(
            isRequired = true,
            isEnabled = enabledSet.contains(method)
        )
    } + optionalMethods.mapNotNull { method ->
        availableMethods[method]?.toContactMethod(
            isRequired = false,
            isEnabled = enabledSet.contains(method)
        )
    }
}

data class ContactPreference(
    val title: String,
    val subtitle: String,
    val description: String,
    val channel: String,
    val methods: List<ContactMethod>
)

data class ContactMethod(
    val method: String,
    val title: String,
    var enabled: Boolean,
    val required: Boolean,
    val configured: Boolean,
    val verified: Boolean
)

data class ContactPreferenceUpdate(
    val method: String,
    val channel: String,
    val action: ContactPreferenceAction
)

enum class ContactPreferenceAction {
    ENABLE, DISABLE
}

private fun ContactPreferenceUpdate.toPreferenceUpdate() = PreferenceUpdate(
    contactMethod = method,
    channel = channel,
    action = action.name
)

fun NotificationMethod.toContactMethod(isRequired: Boolean, isEnabled: Boolean) =
    ContactMethod(
        method = method,
        title = title,
        required = isRequired,
        enabled = isEnabled,
        configured = configured,
        verified = verified
    )
