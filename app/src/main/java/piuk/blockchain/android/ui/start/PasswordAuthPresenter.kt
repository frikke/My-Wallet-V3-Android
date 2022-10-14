package piuk.blockchain.android.ui.start

import androidx.annotation.CallSuper
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.core.auth.AuthDataManager
import com.blockchain.core.payload.PayloadDataManager
import com.blockchain.logging.RemoteLogger
import com.blockchain.network.PollResult
import com.blockchain.network.PollService
import com.blockchain.preferences.AuthPrefs
import info.blockchain.wallet.api.data.Settings
import info.blockchain.wallet.exceptions.DecryptionException
import info.blockchain.wallet.exceptions.HDWalletException
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONObject
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.base.MvpPresenter
import piuk.blockchain.android.ui.base.MvpView
import piuk.blockchain.android.util.AppUtil
import retrofit2.Response
import timber.log.Timber

interface PasswordAuthView : MvpView {
    fun goToPinPage()
    fun showSnackbar(@StringRes messageId: Int, type: SnackbarType)
    fun showErrorSnackbarWithParameter(@StringRes messageId: Int, message: String)
    fun updateWaitingForAuthDialog(secondsRemaining: Int)
    fun resetPasswordField()
    fun showTwoFactorCodeNeededDialog(
        responseObject: JSONObject,
        sessionId: String,
        authType: Int,
        guid: String,
        password: String
    )
}

abstract class PasswordAuthPresenter<T : PasswordAuthView> : MvpPresenter<T>() {

    protected abstract val appUtil: AppUtil
    protected abstract val authDataManager: AuthDataManager
    protected abstract val payloadDataManager: PayloadDataManager
    protected abstract val authPrefs: AuthPrefs
    protected abstract val remoteLogger: RemoteLogger

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    val timerDisposable = CompositeDisposable()
    private lateinit var pollService: PollService<Response<ResponseBody>>

    override fun onViewCreated() {}

    override fun onViewAttached() {
        if (authComplete) {
            view?.goToPinPage()
        }
    }

    override fun onViewDetached() { /* no-op */
    }

    override val alwaysDisableScreenshots = true
    override val enableLogoutTimer = false

    private var sessionId: String? = null

    private var authComplete = false

    internal fun submitTwoFactorCode(
        responseObject: JSONObject,
        sessionId: String,
        guid: String,
        password: String,
        code: String?
    ) {
        if (code.isNullOrEmpty()) {
            view?.showSnackbar(R.string.two_factor_null_error, SnackbarType.Error)
        } else {
            compositeDisposable += authDataManager.submitTwoFactorCode(sessionId, guid, code)
                .doOnSubscribe {
                    view?.showProgressDialog(R.string.please_wait)
                }
                .doAfterTerminate { view?.dismissProgressDialog() }
                .subscribe(
                    { response ->
                        // This is slightly hacky, but if the user requires 2FA login,
                        // the payload comes in two parts. Here we combine them and
                        // parse/decrypt normally.
                        responseObject.put("payload", response.string())
                        val responseBody = responseObject.toString()
                            .toResponseBody("application/json".toMediaTypeOrNull())

                        val payload = Response.success(responseBody)
                        handleResponse(password, guid, payload)
                    },
                    {
                        showErrorSnackbar(R.string.two_factor_incorrect_error)
                    }
                )
        }
    }

    private fun getSessionId(guid: String): Observable<String> =
        sessionId?.let { Observable.just(it) } ?: authDataManager.getSessionId(guid)

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    fun verifyPassword(password: String, guid: String) {
        compositeDisposable += getSessionId(guid)
            .doOnSubscribe {
                view?.showProgressDialog(R.string.validating_password)
            }
            .doOnNext { s -> sessionId = s }
            .flatMap { sessionId -> authDataManager.getEncryptedPayload(guid, sessionId, resend2FASms = false) }
            .subscribeBy(
                onNext = { response -> handleResponse(password, guid, response) },
                onError = { throwable -> handleSessionError(throwable) }
            )
    }

    fun requestNew2FaCode(password: String, guid: String) {
        compositeDisposable += getSessionId(guid)
            .doOnSubscribe {
                view?.showProgressDialog(R.string.two_fa_new_request)
            }
            .doOnNext { s -> sessionId = s }
            .flatMap { sessionId -> authDataManager.getEncryptedPayload(guid, sessionId, resend2FASms = true) }
            .subscribeBy(
                onNext = { response -> handleResponse(password, guid, response) },
                onError = { throwable -> handleSessionError(throwable) }
            )
    }

    private fun handleSessionError(throwable: Throwable) {
        Timber.e(throwable)
        sessionId = null
        onAuthFailed()
    }

    private fun handleResponse(password: String, guid: String, response: Response<ResponseBody>) {
        val errorBody = if (response.errorBody() != null) response.errorBody()!!.string() else ""

        when {
            errorBody.contains(KEY_AUTH_REQUIRED) -> {
                waitForEmailAuth(password, guid)
                startTimer()
            }
            errorBody.contains(INITIAL_ERROR) -> {
                decodeBodyAndShowError(errorBody)
            }
            else -> {
                // No 2FA
                checkTwoFactor(password, guid, response)
            }
        }
    }

    private fun decodeBodyAndShowError(errorBody: String) {
        view?.dismissProgressDialog()
        try {
            val json = JSONObject(errorBody)
            val errorReason = json.getString(INITIAL_ERROR)
            remoteLogger.logState(INITIAL_ERROR, errorReason)
            view?.showErrorSnackbarWithParameter(R.string.common_replaceable_value, errorReason)
        } catch (e: Exception) {
            remoteLogger.logState(INITIAL_ERROR, e.message!!)
            view?.showSnackbar(R.string.common_error, SnackbarType.Error)
        }
    }

    private fun pollAuthStatus(guid: String): Single<PollResult<Response<ResponseBody>>> {
        pollService = PollService(
            authDataManager.getEncryptedPayload(guid, sessionId!!, resend2FASms = false).firstOrError()
        ) {
            val errorString = it.errorBody()?.toString()
            val bodyString = it.body()?.toString()

            errorString == null && bodyString != null && bodyString.isNotEmpty()
        }
        return pollService.start(timerInSec = INTERVAL, retries = Int.MAX_VALUE)
    }

    fun cancelPollAuthStatus() {
        if (::pollService.isInitialized) {
            pollService.cancel.onNext(true)
        }
    }

    private fun startTimer() {
        timerDisposable += authDataManager.createCheckEmailTimer()
            .doOnSubscribe {
                view?.showProgressDialog(R.string.check_email_to_auth_login, ::onProgressCancelled)
            }
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext { integer ->
                if (integer > 1) {
                    view?.updateWaitingForAuthDialog(integer)
                }
            }
            .doOnComplete {
                cancelPollAuthStatus()
                throw RuntimeException("Timeout")
            }
            .subscribeBy(
                onError = {
                    showErrorSnackbar(R.string.auth_failed)
                }
            )
    }

    fun waitForEmailAuth(password: String, guid: String) {
        pollAuthStatus(guid)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = {
                    when (it) {
                        is PollResult.FinalResult -> {
                            checkTwoFactor(password, guid, it.value)
                        }
                        is PollResult.Cancel,
                        is PollResult.TimeOut -> {
                        }
                    }
                },
                onError = {
                    Timber.e(it)
                }
            )
    }

    private fun checkTwoFactor(password: String, guid: String, response: Response<ResponseBody>) {
        val responseBody = response.body()!!.string()
        val jsonObject = try {
            JSONObject(responseBody)
        } catch (e: Exception) {
            Timber.e("checkTwoFactor" + e.message)
            JSONObject()
        }
        // Check if the response has a 2FA Auth Type but is also missing the payload,
        // as it comes in two parts if 2FA enabled.
        if (jsonObject.isAuth() && (jsonObject.isGoogleAuth() || jsonObject.isSMSAuth())) {
            view?.dismissProgressDialog()
            view?.showTwoFactorCodeNeededDialog(
                jsonObject,
                sessionId!!,
                jsonObject.getInt("auth_type"),
                guid,
                password
            )
        } else {
            attemptDecryptPayload(password, responseBody)
        }
    }

    private fun attemptDecryptPayload(password: String, payload: String) {
        compositeDisposable += payloadDataManager.initializeFromPayload(payload, password)
            .doOnComplete {
                authPrefs.apply {
                    sharedKey = payloadDataManager.wallet!!.sharedKey
                    walletGuid = payloadDataManager.wallet!!.guid
                    emailVerified = true
                    pinId = ""
                }
            }
            .subscribeBy(
                onComplete = {
                    onAuthComplete()
                },
                onError = { throwable ->
                    when (throwable) {
                        is HDWalletException -> showErrorSnackbar(R.string.pairing_failed)
                        is DecryptionException -> showErrorSnackbar(R.string.invalid_password)
                        else -> showErrorSnackbarAndRestartApp(R.string.auth_failed)
                    }
                }
            )
    }

    @CallSuper
    protected open fun onAuthFailed() {
    }

    @CallSuper
    protected open fun onAuthComplete() {
        authComplete = true
        view?.goToPinPage()
    }

    internal fun onProgressCancelled() {
        compositeDisposable.clear()
        timerDisposable.clear()
    }

    protected fun showErrorSnackbar(@StringRes message: Int) {
        view?.apply {
            dismissProgressDialog()
            resetPasswordField()
            showSnackbar(message, SnackbarType.Error)
        }
    }

    protected fun showErrorSnackbarAndRestartApp(@StringRes message: Int) {
        view?.apply {
            resetPasswordField()
            dismissProgressDialog()
            showSnackbar(message, SnackbarType.Error)
        }
        cancelPollAuthStatus()
        cancelAuthTimer()
    }

    fun cancelAuthTimer() {
        timerDisposable.clear()
    }

    fun hasTimerStarted(): Boolean = timerDisposable.size() > 0

    companion object {
        @VisibleForTesting
        internal val KEY_AUTH_REQUIRED = "authorization_required"
        internal val INITIAL_ERROR = "initial_error"
        private const val INTERVAL: Long = 2
    }
}

private fun JSONObject.isAuth(): Boolean =
    has("auth_type") && !has("payload")

private fun JSONObject.isGoogleAuth(): Boolean =
    getInt("auth_type") == Settings.AUTH_TYPE_GOOGLE_AUTHENTICATOR

private fun JSONObject.isSMSAuth(): Boolean =
    getInt("auth_type") == Settings.AUTH_TYPE_SMS
