package piuk.blockchain.android.fraud.domain.service

interface FraudService {
    fun updateSessionId(onSessionIdGenerated: (() -> Unit)? = null)
    fun updateUnauthenticatedUserFlows()
    fun updateAuthenticatedUserFlows()

    fun initMobileIntelligence(application: Any, clientId: String)
    fun trackFlow(flow: FraudFlow)
    fun endFlow(flow: FraudFlow? = null, onDataSubmitted: (() -> Unit)? = null)
    fun endFlows(vararg flows: FraudFlow)
}
