package piuk.blockchain.android.fraud.data.repository

import android.app.Application
import com.blockchain.api.interceptors.SessionInfo
import com.blockchain.api.services.FraudRemoteService
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.outcome.fold
import com.sardine.ai.mdisdk.MobileIntelligence
import com.sardine.ai.mdisdk.MobileIntelligence.SubmitResponse
import com.sardine.ai.mdisdk.Options
import com.sardine.ai.mdisdk.UpdateOptions
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference
import kotlin.text.Charsets.UTF_8
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import piuk.blockchain.android.fraud.domain.service.FraudFlow
import piuk.blockchain.android.fraud.domain.service.FraudFlows
import piuk.blockchain.android.fraud.domain.service.FraudService
import timber.log.Timber

internal class FraudRepository(
    private val coroutineScope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher,
    private val fraudService: FraudRemoteService,
    private val sessionInfo: SessionInfo,
    private val fraudFlows: FraudFlows,
    private val environmentConfig: EnvironmentConfig,
    private val sessionIdFeatureFlag: FeatureFlag,
    private val sardineFeatureFlag: FeatureFlag
) : FraudService {

    private val currentFlow = AtomicReference<FraudFlow?>(null)

    override fun updateSessionId(onSessionIdGenerated: (() -> Unit)?) {
        sessionInfo.clearSessionId()

        coroutineScope.launch(dispatcher) {
            if (sessionIdFeatureFlag.coEnabled()) {
                sessionInfo.setSessionId(UUID.randomUUID().toString())
                withContext(Dispatchers.Main) {
                    onSessionIdGenerated?.invoke()
                }
            }
        }
    }

    override fun updateUnauthenticatedUserFlows() {
        coroutineScope.launch(dispatcher) {
            if (sardineFeatureFlag.coEnabled()) {
                fraudService.getFraudFlows()
                    .fold(
                        onSuccess = { response ->
                            fraudFlows.clearUnauthenticatedUserFlows()
                            fraudFlows.addUnauthenticatedUserFlows(
                                response.flows?.mapNotNull { it.name.toFraudFlow() }?.toSet() ?: emptySet()
                            )
                        },
                        onFailure = {
                            fraudFlows.clearUnauthenticatedUserFlows()
                        }
                    )
            }
        }
    }

    override fun updateAuthenticatedUserFlows() {
        coroutineScope.launch(dispatcher) {
            if (sardineFeatureFlag.coEnabled()) {
                fraudService.getFraudFlows()
                    .fold(
                        onSuccess = { response ->
                            fraudFlows.clearAuthenticatedUserFlows()
                            fraudFlows.addAuthenticatedUserFlows(
                                response.flows?.mapNotNull { it.name.toFraudFlow() }?.toSet() ?: emptySet()
                            )
                        },
                        onFailure = {
                            fraudFlows.clearAuthenticatedUserFlows()
                        }
                    )
            }
        }
    }

    override fun initMobileIntelligence(application: Any, clientId: String) {
        coroutineScope.launch(dispatcher) {
            if (sardineFeatureFlag.coEnabled()) {
                withContext(Dispatchers.Main) {
                    if (application is Application) {
                        val sessionId = sessionInfo.getSessionId() ?: UUID.randomUUID().toString()

                        val option: Options = Options.Builder()
                            .setClientID(clientId)
                            .enableBehaviorBiometrics(true)
                            .enableClipboardTracking(true)
                            .apply {
                                if (environmentConfig.isRunningInDebugMode()) {
                                    setEnvironment(Options.ENV_SANDBOX)
                                } else {
                                    setEnvironment(Options.ENV_PRODUCTION)
                                }
                            }
                            .setFlow("STARTUP")
                            .setSessionKey(sessionId.hash())
                            .setShouldAutoSubmitOnInit(true)
                            .build()
                        MobileIntelligence.init(application, option)
                    } else {
                        throw IllegalStateException("Unable to init Sardine. No application provided.")
                    }
                }
            }
        }
    }

    override fun trackFlow(flow: FraudFlow) {
        coroutineScope.launch(dispatcher) {
            if (sardineFeatureFlag.coEnabled()) {
                withContext(Dispatchers.Main) {
                    if (fraudFlows.getAllFlows().contains(flow)) {
                        currentFlow.set(flow)
                        Timber.i("Start tracking fraud flow: ${flow.name}.")
                        val options: UpdateOptions = UpdateOptions.Builder()
                            .setFlow(flow.name)
                            .apply {
                                sessionInfo.getSessionId()?.let { sessionKey -> setSessionKey(sessionKey.hash()) }
                                sessionInfo.getUserId()?.let { userId -> setUserId(userId.hash()) }
                            }
                            .build()

                        MobileIntelligence.updateOptions(options, onDataSubmittedCallback(flow))
                    }
                }
            }
        }
    }

    override fun endFlow(flow: FraudFlow?, onDataSubmitted: (() -> Unit)?) {
        coroutineScope.launch(dispatcher) {
            if (sardineFeatureFlag.coEnabled()) {
                withContext(Dispatchers.Main) {
                    submitData(flow, onDataSubmitted)
                }
            }
        }
    }

    override fun endFlows(vararg flows: FraudFlow) {
        coroutineScope.launch(dispatcher) {
            if (sardineFeatureFlag.coEnabled()) {
                withContext(Dispatchers.Main) {
                    flows.forEach { submitData(it) }
                }
            }
        }
    }

    private fun submitData(flow: FraudFlow?, onDataSubmitted: (() -> Unit)? = null) {
        try {
            if (currentFlow.get() == flow || flow == null) {
                currentFlow.set(null)

                if (!MobileIntelligence.options.flow.isNullOrEmpty()) {
                    Timber.i("Stop tracking fraud flow: ${flow?.name}[${MobileIntelligence.options.flow}].")
                    MobileIntelligence.submitData(onDataSubmittedCallback(flow, onDataSubmitted))
                }
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    private fun onDataSubmittedCallback(flow: FraudFlow?, onDataSubmitted: (() -> Unit)? = null) =
        object : MobileIntelligence.Callback<SubmitResponse> {
            override fun onSuccess(response: SubmitResponse) {
                onDataSubmitted?.invoke()
                Timber.i("Fraud data sent for: ${flow?.name ?: ""}[${MobileIntelligence.options.flow}].")
            }

            override fun onError(exception: Exception) {
                onDataSubmitted?.invoke()
                Timber.e(
                    exception,
                    "Error sending fraud data for: ${flow?.name ?: ""}[${MobileIntelligence.options.flow}]."
                )
            }
        }

    private fun String.toFraudFlow(): FraudFlow? =
        when {
            equals(FraudFlow.SIGNUP.name, ignoreCase = true) -> FraudFlow.SIGNUP
            equals(FraudFlow.LOGIN.name, ignoreCase = true) -> FraudFlow.LOGIN
            equals(FraudFlow.ONBOARDING.name, ignoreCase = true) -> FraudFlow.ONBOARDING
            equals(FraudFlow.KYC.name, ignoreCase = true) -> FraudFlow.KYC
            equals(FraudFlow.CARD_LINK.name, ignoreCase = true) -> FraudFlow.CARD_LINK
            equals(FraudFlow.ACH_LINK.name, ignoreCase = true) -> FraudFlow.ACH_LINK
            equals(FraudFlow.OB_LINK.name, ignoreCase = true) -> FraudFlow.OB_LINK
            equals(FraudFlow.CARD_DEPOSIT.name, ignoreCase = true) -> FraudFlow.CARD_DEPOSIT
            equals(FraudFlow.ACH_DEPOSIT.name, ignoreCase = true) -> FraudFlow.ACH_DEPOSIT
            equals(FraudFlow.OB_DEPOSIT.name, ignoreCase = true) -> FraudFlow.OB_DEPOSIT
            equals(FraudFlow.MOBILE_WALLET_DEPOSIT.name, ignoreCase = true) -> FraudFlow.MOBILE_WALLET_DEPOSIT
            equals(FraudFlow.WITHDRAWAL.name, ignoreCase = true) -> FraudFlow.WITHDRAWAL
            else -> null
        }

    private fun String.hash(): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(this.toByteArray(UTF_8))
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }
}
