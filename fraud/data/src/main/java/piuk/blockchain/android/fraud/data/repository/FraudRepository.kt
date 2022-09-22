package piuk.blockchain.android.fraud.data.repository

import com.blockchain.api.services.SessionService
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.outcome.fold
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import piuk.blockchain.android.fraud.domain.service.FraudService
import piuk.blockchain.androidcore.data.api.interceptors.SessionId

internal class FraudRepository(
    private val coroutineScope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher,
    private val sessionService: SessionService,
    private val sessionId: SessionId,
    private val sessionIdFeatureFlag: FeatureFlag
) : FraudService {

    override fun updateSessionId() {
        coroutineScope.launch(dispatcher) {
            sessionId.clearSessionId()

            if (sessionIdFeatureFlag.coEnabled()) {
                sessionService.getSessionId()
                    .fold(
                        onSuccess = {
                            sessionId.setSessionId(it.xSessionId)
                        },
                        onFailure = {
                            sessionId.clearSessionId()
                        }
                    )
            }
        }
    }
}
