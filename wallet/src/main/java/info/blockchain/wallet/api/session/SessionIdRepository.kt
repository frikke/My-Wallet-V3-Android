package info.blockchain.wallet.api.session

import com.blockchain.domain.session.SessionIdService
import com.blockchain.preferences.AuthPrefs
import info.blockchain.wallet.ApiCode
import info.blockchain.wallet.api.WalletExplorerEndpoints
import io.reactivex.rxjava3.core.Single
import java.lang.IllegalArgumentException
import okhttp3.ResponseBody
import org.json.JSONObject

class SessionIdRepository(
    private val explorerEndpoints: WalletExplorerEndpoints,
    private val authPrefs: AuthPrefs,
    private val api: ApiCode,
) : SessionIdService {
    override fun sessionId(): Single<String> {
        authPrefs.sessionId.takeIf { it.isNotEmpty() }?.let {
            return Single.just(it)
        } ?: return createSessionId().flatMap {
            val response = JSONObject(it.string())
            if (response.has("token")) {
                Single.just(response.getString("token"))
            } else {
                Single.error(IllegalArgumentException("Session Token not found"))
            }
        }.doOnSuccess {
            authPrefs.sessionId = it
        }
    }

    private fun createSessionId(): Single<ResponseBody> =
        explorerEndpoints.createSessionId(api.apiCode)
}
