package com.blockchain.core.auth

import com.blockchain.api.services.AuthApiService
import com.blockchain.core.access.PinRepository
import com.blockchain.core.auth.model.AccountLockedException
import com.blockchain.core.auth.model.AuthRequiredException
import com.blockchain.core.auth.model.InitialErrorException
import com.blockchain.core.auth.model.UnknownErrorException
import com.blockchain.core.utils.AESUtilWrapper
import com.blockchain.core.utils.EncryptedPrefs
import com.blockchain.core.utils.schedulers.applySchedulers
import com.blockchain.logging.RemoteLogger
import com.blockchain.preferences.AuthPrefs
import com.blockchain.preferences.WalletStatusPrefs
import info.blockchain.wallet.api.data.WalletOptions
import info.blockchain.wallet.crypto.AESUtil
import info.blockchain.wallet.exceptions.InvalidCredentialsException
import info.blockchain.wallet.exceptions.ServerConnectionException
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import java.security.SecureRandom
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.ResponseBody
import org.spongycastle.util.encoders.Hex
import retrofit2.Response

class AuthDataManager(
    private val authApiService: AuthApiService,
    private val walletAuthService: WalletAuthService,
    private val pinRepository: PinRepository,
    private val aesUtilWrapper: AESUtilWrapper,
    private val remoteLogger: RemoteLogger,
    private val authPrefs: AuthPrefs,
    private val walletStatusPrefs: WalletStatusPrefs,
    private val encryptedPrefs: EncryptedPrefs
) {

    // VisibleForTesting
    internal var timer: Int = 0

    private var shouldVerifyCloudBackup = false

    /**
     * Returns a [WalletOptions] object from the website. This object is used to get the
     * current buy/sell partner info as well as a list of countries where buy/sell is rolled out.
     *
     * @return An [Observable] wrapping a [WalletOptions] object
     */
    fun getWalletOptions(): Observable<WalletOptions> =
        walletAuthService.getWalletOptions()
            .applySchedulers()

    /**
     * Attempts to retrieve an encrypted Payload from the server, but may also return just part of a
     * Payload or an error response.
     *
     * @param guid The user's unique GUID
     * @param sessionId The current session ID
     * @return An [Observable] wrapping a [<] which could notify
     * the user that authentication (ie checking your email, 2FA etc) is required
     * @see .getSessionId
     */
    fun getEncryptedPayload(
        guid: String,
        resend2FASms: Boolean
    ): Single<Response<ResponseBody>> =
        getSessionId().flatMap {
            walletAuthService.getEncryptedPayload(
                guid.trimAndTruncateGuid(),
                it,
                resend2FASms
            )
        }
            .applySchedulers()

    fun getEncryptedPayloadObject(guid: String, sessionId: String, resend2FASms: Boolean): Single<JsonObject> =
        walletAuthService.getEncryptedPayload(
            guid.trimAndTruncateGuid(),
            sessionId,
            resend2FASms
        )
            .applySchedulers()
            .flatMap {
                it.handleResponse()
            }

    private fun String.trimAndTruncateGuid() =
        trim().take(DESIRED_GUID_LENGTH)

    /**
     * Gets an ephemeral session ID from the server.
     *
     * @param guid The user's unique GUID
     * @return An [Observable] wrapping a session ID as a String
     */
    fun getSessionId(): Single<String> =
        walletAuthService.getSessionId()
            .applySchedulers()

    /**
     * Requests authorization for the session specified by the ID
     *
     * @param authToken The token required for auth from the email
     * @param sessionId The current session ID
     * @return A [Single] wrapping
     */
    fun authorizeSessionObject(authToken: String): Single<JsonObject> =
        walletAuthService.authorizeSession(authToken).flatMap { it.handleResponse() }

    /**
     * Submits a user's 2FA code to the server and returns a response. This response will contain
     * the user's encrypted Payload if successful, if not it will contain an error.
     *
     * @param sessionId The current session ID
     * @param guid The user's unique GUID
     * @param twoFactorCode A valid 2FA code generated from Google Authenticator or similar
     * @see .getSessionId
     */
    fun submitTwoFactorCode(
        guid: String,
        twoFactorCode: String
    ): Single<ResponseBody> = walletAuthService.submitTwoFactorCode(guid, twoFactorCode)
        .applySchedulers()

    /**
     * Creates a timer which counts down for two minutes and emits the remaining time on each count.
     * This is used to show the user how long they have to check their email before the login
     * request expires.
     *
     * @return An [Observable] where the emitted int is the number of seconds left
     */
    fun createCheckEmailTimer(): Observable<Int> {
        timer = 2 * 60

        return Observable.interval(0, 1, TimeUnit.SECONDS)
            .map { timer-- }
            .takeUntil { timer < 0 }
    }

    /**
     * Validates the passed PIN for the user's GUID and shared key and returns a decrypted password.
     *
     * @param passedPin The PIN to be used
     * @return An [Observable] where the wrapped String is the user's decrypted password
     */
    fun validatePin(passedPin: String): Single<String> {
        return getValidatePinObservable(passedPin)
            .applySchedulers()
    }

    /**
     * Creates a new PIN for a user
     *
     * @param password The user's password
     * @param passedPin The new chosen PIN
     * @return A [Completable] object
     */
    fun createPin(password: String, passedPin: String): Completable {
        return getCreatePinObservable(password, passedPin)
            .applySchedulers()
    }

    private fun getValidatePinObservable(passedPin: String): Single<String> {
        val key = authPrefs.pinId

        if (!passedPin.isValidPin()) {
            return Single.error(IllegalArgumentException("Invalid PIN"))
        } else {
            pinRepository.setPin(passedPin)
            remoteLogger.logEvent("validatePin. pin set. validity: ${passedPin.isValidPin()}")
        }

        return walletAuthService.validateAccess(key, passedPin)
            .map { response ->
                /*
                Note: Server side issue - If the incorrect PIN is supplied the server will respond
                with a 403 { code: 1, error: "Incorrect PIN you have x attempts left" }
                 */
                if (response.isSuccessful) {
                    walletStatusPrefs.isNewlyCreated = false
                    walletStatusPrefs.isRestored = false
                    val decryptionKey = response.body()!!.success

                    handleBackup(decryptionKey)

                    return@map aesUtilWrapper.decrypt(
                        authPrefs.encryptedPassword,
                        decryptionKey,
                        AESUtil.PIN_PBKDF2_ITERATIONS
                    )
                } else {
                    if (response.code() == 403) {
                        // Invalid PIN
                        throw InvalidCredentialsException(
                            "Validate access failed"
                        )
                    } else {
                        throw ServerConnectionException(
                            """${response.code()} ${response.message()}"""
                        )
                    }
                }
            }
    }

    /*
    This function takes care of saving the encrypted values into the special storage for
    the automatic backup, and also decrypts the values when necessary into local storage.
     */
    private fun handleBackup(decryptionKey: String) {
        shouldVerifyCloudBackup = when {
            // Just to make sure, if the user specifically opted out out of cloud backups,
            // always clear the backup over here.
            !encryptedPrefs.backupEnabled -> {
                encryptedPrefs.clearBackup()
                false
            }
            encryptedPrefs.hasBackup() && authPrefs.walletGuid.isEmpty() -> {
                encryptedPrefs.restoreFromBackup(decryptionKey)
                false
            }
            else -> {
                encryptedPrefs.backupCurrentPrefs(decryptionKey)
                true
            }
        }
    }

    private fun getCreatePinObservable(password: String, passedPin: String): Completable {
        if (!passedPin.isValidPin()) {
            return Completable.error(IllegalArgumentException("Invalid PIN"))
        } else {
            pinRepository.setPin(passedPin)
            remoteLogger.logEvent("createPin. pin set. validity: ${passedPin.isValidPin()}")
        }

        val bytes = ByteArray(16)
        val random = SecureRandom()
        random.nextBytes(bytes)
        val key = String(Hex.encode(bytes), Charsets.UTF_8)
        random.nextBytes(bytes)
        val value = String(Hex.encode(bytes), Charsets.UTF_8)

        return walletAuthService.setAccessKey(key, value, passedPin).flatMap { response ->
            if (response.isSuccessful) {
                Single.just(response)
            } else {
                Single.error(Throwable("Validate access failed: ${response.errorBody()?.string()}"))
            }
        }.doOnSuccess {
            val encryptionKey = Hex.toHexString(value.toByteArray(Charsets.UTF_8))
            val encryptedPassword = aesUtilWrapper.encrypt(
                password,
                encryptionKey,
                AESUtil.PIN_PBKDF2_ITERATIONS
            )
            authPrefs.encryptedPassword = encryptedPassword
            authPrefs.pinId = key

            handleBackup(encryptionKey)
        }.ignoreElement()
    }

    /**
     * Get the encryption password for pairing
     *
     * @param guid A user's GUID
     * @return [<] wrapping the pairing encryption password
     */
    fun getPairingEncryptionPassword(guid: String): Observable<ResponseBody> =
        walletAuthService.getPairingEncryptionPassword(guid)
            .applySchedulers()

    /**
     * Update the account model fields for mobile setup
     *
     * @param guid The user's GUID
     * @param sharedKey The shared key of the specified GUID
     * @param isMobileSetup has mobile device linked
     * @param deviceType the type of the linked device: 1 - iOS, 2 - Android
     * @return A [Single] wrapping the result
     */
    fun updateMobileSetup(
        guid: String,
        sharedKey: String,
        isMobileSetup: Boolean,
        deviceType: Int
    ): Completable = walletAuthService.updateMobileSetup(guid, sharedKey, isMobileSetup, deviceType)

    /**
     * Update the mnemonic backup date (calculated on the backend)
     *
     * @return A [Completable] wrapping the result
     */
    fun updateMnemonicBackup(): Completable =
        walletAuthService.updateMnemonicBackup(authPrefs.walletGuid, authPrefs.sharedKey)
            .applySchedulers()

    /**
     * Verify that the cloud backup has been completed
     *
     * @return A [Completable] wrapping the result
     */
    fun verifyCloudBackup(): Completable = if (shouldVerifyCloudBackup) {
        walletAuthService.verifyCloudBackup(
            guid = authPrefs.walletGuid,
            sharedKey = authPrefs.sharedKey,
            hasCloudBackup = true,
            deviceType = DEVICE_TYPE_ANDROID
        ).applySchedulers()
    } else {
        Completable.complete()
    }

    /**
     * Send email to verify device via the extended magic link (including the recovery token)
     *
     * @param sessionId The token for the current session
     * @param email The user's email
     * @param captcha Captcha token
     * @return A [Completable] wrapping the result
     */
    fun sendEmailForAuthentication(email: String, captcha: String) =
        authApiService.sendEmailForAuthentication(
            email = email,
            captcha = captcha
        )

    fun getDeeplinkPayload() = walletAuthService.getDeeplinkPayload()

    fun updateLoginApprovalStatus(
        sessionId: String,
        payload: String,
        confirmDevice: Boolean
    ): Completable = walletAuthService.updateLoginApprovalStatus(sessionId, payload, confirmDevice)

    companion object {
        // VisibleForTesting
        internal const val AUTHORIZATION_REQUIRED = "authorization_required"
        private const val DEVICE_TYPE_ANDROID = 2

        // Internal for test visibility
        internal fun Response<ResponseBody>.handleResponse(): Single<JsonObject> =
            if (isSuccessful) {
                body()?.let { responseBody ->
                    Single.just(Json.parseToJsonElement(responseBody.string()) as JsonObject)
                } ?: Single.error(UnknownErrorException())
            } else {
                val errorResponse = errorBody()?.string()

                errorResponse?.let {
                    if (it.contains(ACCOUNT_LOCKED)) {
                        Single.error(AccountLockedException())
                    } else {
                        val errorBody = Json.parseToJsonElement(it) as JsonObject
                        Single.error(
                            when {
                                errorBody.containsKey(INITIAL_ERROR) -> InitialErrorException()
                                errorBody.containsKey(KEY_AUTH_REQUIRED) -> AuthRequiredException()
                                else -> UnknownErrorException()
                            }
                        )
                    }
                } ?: kotlin.run {
                    Single.error(UnknownErrorException())
                }
            }

        private const val INITIAL_ERROR = "initial_error"
        private const val KEY_AUTH_REQUIRED = "authorization_required"
        private const val ACCOUNT_LOCKED = "locked"
        private const val DESIRED_GUID_LENGTH = 36
    }
}
