package piuk.blockchain.android.fraud.domain.service

import java.util.concurrent.atomic.AtomicReference

object FraudFlows {
    private val unauthenticatedUserFlowsValue = AtomicReference<Set<FraudFlow>>(emptySet())
    private val authenticatedUserFlowsValue = AtomicReference<Set<FraudFlow>>(emptySet())

    fun getUnauthenticatedUserFlows() =
        unauthenticatedUserFlowsValue.get()

    fun addUnauthenticatedUserFlows(userFlows: Set<FraudFlow>) {
        unauthenticatedUserFlowsValue.set(getUnauthenticatedUserFlows().toMutableSet().apply { addAll(userFlows) })
    }

    fun clearUnauthenticatedUserFlows() {
        unauthenticatedUserFlowsValue.set(emptySet())
    }

    fun getAuthenticatedUserFlows() = authenticatedUserFlowsValue.get()

    fun addAuthenticatedUserFlows(userFlows: Set<FraudFlow>) {
        authenticatedUserFlowsValue.set(getAuthenticatedUserFlows().toMutableSet().apply { addAll(userFlows) })
    }

    fun clearAuthenticatedUserFlows() {
        authenticatedUserFlowsValue.set(emptySet())
    }

    fun getAllFlows() = getUnauthenticatedUserFlows().toMutableSet().apply { addAll(getAuthenticatedUserFlows()) }
}

enum class FraudFlow {
    SIGNUP,
    LOGIN,
    ONBOARDING,
    KYC,
    CARD_LINK,
    ACH_LINK,
    OB_LINK,
    CARD_DEPOSIT,
    ACH_DEPOSIT,
    OB_DEPOSIT,
    MOBILE_WALLET_DEPOSIT,
    WITHDRAWAL
}
