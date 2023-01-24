package piuk.blockchain.android.ui.auth.newlogin.presentation

import com.blockchain.commonarch.presentation.mvi.MviModel
import com.blockchain.coreandroid.utils.pubKeyHash
import com.blockchain.domain.auth.SecureChannelBrowserMessage
import com.blockchain.domain.auth.SecureChannelService
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.logging.RemoteLogger
import com.blockchain.preferences.Authorization
import com.blockchain.preferences.BrowserIdentity
import com.blockchain.preferences.SecureChannelPrefs
import info.blockchain.wallet.api.WalletApi
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import timber.log.Timber

class AuthNewLoginModel(
    initialState: AuthNewLoginState,
    mainScheduler: Scheduler,
    environmentConfig: EnvironmentConfig,
    remoteLogger: RemoteLogger,
    private val secureChannelService: SecureChannelService,
    private val secureChannelPrefs: SecureChannelPrefs,
    private val walletApi: WalletApi
) : MviModel<AuthNewLoginState, AuthNewLoginIntents>(initialState, mainScheduler, environmentConfig, remoteLogger) {

    override fun performAction(previousState: AuthNewLoginState, intent: AuthNewLoginIntents): Disposable? {
        return when (intent) {
            is AuthNewLoginIntents.InitAuthInfo ->
                parseMessage(
                    pubKeyHash = intent.pubKeyHash,
                    message = intent.message,
                    originIp = intent.originIp
                )
            is AuthNewLoginIntents.ProcessBrowserMessage ->
                processIp(
                    browserIdentity = intent.browserIdentity,
                    message = intent.message,
                    originIp = intent.originIp
                )
            is AuthNewLoginIntents.LoginDenied -> processLoginDenied(previousState)
            is AuthNewLoginIntents.LoginApproved -> processLoginApproved(previousState)
            is AuthNewLoginIntents.EnableApproval -> null
        }
    }

    private fun processLoginApproved(previousState: AuthNewLoginState): Nothing? {
        secureChannelService.sendLoginMessage(
            channelId = previousState.message.channelId,
            pubKeyHash = previousState.browserIdentity.pubKeyHash()
        )
        secureChannelPrefs.addBrowserIdentityAuthorization(
            pubkeyHash = previousState.browserIdentity.pubKeyHash(),
            authorization = getRequestedAuthorization(previousState.message)!!
        )
        return null
    }

    private fun processLoginDenied(previousState: AuthNewLoginState): Nothing? {
        secureChannelService.sendErrorMessage(
            channelId = previousState.message.channelId,
            pubKeyHash = previousState.browserIdentity.pubKeyHash()
        )
        return null
    }

    private fun parseMessage(pubKeyHash: String, message: SecureChannelBrowserMessage, originIp: String): Disposable? {
        process(
            AuthNewLoginIntents.ProcessBrowserMessage(
                originIp = originIp,
                browserIdentity = secureChannelPrefs.getBrowserIdentity(pubKeyHash)!!,
                message = message
            )
        )
        return null
    }

    private fun processIp(browserIdentity: BrowserIdentity, message: SecureChannelBrowserMessage, originIp: String) =
        walletApi.getExternalIP().subscribeBy(
            onSuccess = { deviceIp ->
                process(
                    AuthNewLoginIntents.EnableApproval(
                        enableApproval = isAuthorized(browserIdentity, message) ||
                            originIp == deviceIp,
                        errorState = NewLoginState.NONE
                    )
                )
            },
            onError = {
                Timber.e(it)
                process(
                    AuthNewLoginIntents.EnableApproval(
                        enableApproval = false,
                        errorState = NewLoginState.IP_MISMATCH
                    )
                )
            }
        )

    private fun getRequestedAuthorization(message: SecureChannelBrowserMessage): Authorization? {
        return try {
            Authorization.valueOf(message.type.toUpperCase())
        } catch (e: Exception) {
            null
        }
    }

    private fun isAuthorized(browserIdentity: BrowserIdentity, message: SecureChannelBrowserMessage): Boolean =
        browserIdentity.authorized.contains(getRequestedAuthorization(message))
}
