package piuk.blockchain.android.fraud.domain.service

interface FraudService {
    fun updateSessionId()
    fun updateUnauthenticatedUserFlows()
    fun updateAuthenticatedUserFlows()

    fun initMobileIntelligence(application: Any, clientId: String)
    fun startFlow(flow: FraudFlow)
    fun endFlow(flow: FraudFlow? = null, onDataSubmitted: (() -> Unit)? = null)
    fun endFlows(vararg flows: FraudFlow)
}
