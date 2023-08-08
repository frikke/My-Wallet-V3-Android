package piuk.blockchain.android.ui.login.auth

import com.blockchain.commonarch.presentation.mvi.MviModel
import com.blockchain.core.auth.model.AccountLockedException
import com.blockchain.core.auth.model.AuthRequiredException
import com.blockchain.core.auth.model.InitialErrorException
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.logging.RemoteLogger
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import timber.log.Timber

class LoginAuthModel(
    initialState: LoginAuthState,
    mainScheduler: Scheduler,
    environmentConfig: EnvironmentConfig,
    remoteLogger: RemoteLogger,
    private val interactor: LoginAuthInteractor
) : MviModel<LoginAuthState, LoginAuthIntents>(initialState, mainScheduler, environmentConfig, remoteLogger) {

    override fun performAction(previousState: LoginAuthState, intent: LoginAuthIntents): Disposable? {
        return when (intent) {
            is LoginAuthIntents.InitLoginAuthInfo -> initLoginAuthInfo(intent.json)
            is LoginAuthIntents.GetSessionId -> getSessionId()
            is LoginAuthIntents.AuthorizeApproval ->
                authorizeApproval(
                    authToken = previousState.authToken
                )
            is LoginAuthIntents.GetPayload -> getPayload(guid = previousState.guid, sessionId = previousState.sessionId)
            is LoginAuthIntents.VerifyPassword ->
                verifyPassword(
                    payload = if (intent.payloadJson.isNotEmpty()) {
                        intent.payloadJson
                    } else {
                        previousState.payloadJson
                    },
                    password = intent.password
                )
            is LoginAuthIntents.SubmitTwoFactorCode ->
                submitCode(
                    guid = previousState.guid,
                    password = intent.password,
                    code = intent.code,
                    payloadJson = previousState.payloadJson
                )
            is LoginAuthIntents.UpdateMobileSetup -> updateAccount(
                isMobileSetup = intent.isMobileSetup,
                deviceType = intent.deviceType,
                shouldRequestUpgrade = previousState.shouldRequestAccountUnification
            )
            is LoginAuthIntents.ShowAuthComplete -> clearSessionId()
            is LoginAuthIntents.RequestNew2FaCode -> requestNew2FaCode(previousState)
            is LoginAuthIntents.Reset2FARetries -> reset2FaRetries()
            else -> null
        }
    }

    private fun initLoginAuthInfo(json: String): Disposable =
        interactor.getAuthInfo(json)
            .subscribeBy(
                onSuccess = { loginAuthInfo ->
                    process(LoginAuthIntents.GetSessionId(loginAuthInfo))
                },
                onError = { throwable ->
                    Timber.e(throwable)
                    process(LoginAuthIntents.ShowError(throwable))
                }
            )

    private fun reset2FaRetries() =
        interactor.reset2FaRetries()
            .subscribeBy(
                onComplete = {
                    process(LoginAuthIntents.Update2FARetryCount(interactor.getRemaining2FaRetries()))
                },
                onError = {
                    process(LoginAuthIntents.New2FaCodeTimeLock)
                }
            )

    private fun requestNew2FaCode(previousState: LoginAuthState) =
        interactor.requestNew2FaCode(previousState.guid, previousState.sessionId)
            .subscribeBy(
                onSuccess = {
                    process(LoginAuthIntents.Update2FARetryCount(interactor.getRemaining2FaRetries()))
                },
                onError = { throwable ->
                    processError(throwable)
                }
            )

    private fun getSessionId(): Disposable? {
        process(LoginAuthIntents.AuthorizeApproval(interactor.getSessionId()))
        return null
    }

    private fun clearSessionId(): Disposable? {
        interactor.clearSessionId()
        return null
    }

    private fun authorizeApproval(authToken: String): Disposable {
        return interactor.authorizeApproval(authToken)
            .subscribeBy(
                onSuccess = { process(LoginAuthIntents.GetPayload) },
                onError = { throwable ->
                    process(LoginAuthIntents.ShowError(throwable))
                }
            )
    }

    private fun getPayload(guid: String, sessionId: String): Disposable {
        return interactor.getPayload(guid, sessionId)
            .doOnSubscribe {
                process(LoginAuthIntents.Reset2FARetries)
            }
            .subscribeBy(
                onSuccess = { jsonObject ->
                    process(LoginAuthIntents.Update2FARetryCount(interactor.getRemaining2FaRetries()))
                    process(LoginAuthIntents.SetPayload(payloadJson = jsonObject))
                },
                onError = { throwable ->
                    processError(throwable)
                }
            )
    }

    private fun verifyPassword(payload: String, password: String): Disposable {
        return interactor.verifyPassword(payload, password)
            .subscribeBy(
                onComplete = {
                    process(
                        LoginAuthIntents.UpdateMobileSetup(
                            isMobileSetup = true,
                            deviceType = DEVICE_TYPE_ANDROID
                        )
                    )
                },
                onError = { throwable ->
                    process(LoginAuthIntents.ShowError(throwable))
                }
            )
    }

    private fun submitCode(
        guid: String,
        password: String,
        code: String,
        payloadJson: String
    ): Disposable {
        return interactor.submitCode(guid, code, payloadJson)
            .subscribeBy(
                onSuccess = { responseBody ->
                    process(LoginAuthIntents.VerifyPassword(password, responseBody.string()))
                },
                onError = { process(LoginAuthIntents.Show2FAFailed) }
            )
    }

    private fun updateAccount(isMobileSetup: Boolean, deviceType: Int, shouldRequestUpgrade: Boolean) =
        // SSO - if [shouldRequestUpgrade] then user is eligible to have an Exchange account with the same email
        interactor.updateMobileSetup(isMobileSetup, deviceType)
            .subscribeBy(
                onComplete = {
                    process(LoginAuthIntents.ShowAuthComplete)
                },
                onError = { throwable ->
                    process(LoginAuthIntents.ShowError(throwable))
                }
            )

    private fun processError(throwable: Throwable) =
        when (throwable) {
            is InitialErrorException -> process(LoginAuthIntents.ShowInitialError)
            is AuthRequiredException -> process(LoginAuthIntents.ShowAuthRequired)
            is TimeLockException -> process(LoginAuthIntents.New2FaCodeTimeLock)
            is AccountLockedException -> process(LoginAuthIntents.ShowAccountLockedError)
            else -> process(LoginAuthIntents.ShowError(throwable))
        }

    companion object {
        private const val DEVICE_TYPE_ANDROID = 2
    }

    class TimeLockException : Exception()
}
