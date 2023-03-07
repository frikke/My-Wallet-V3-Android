package info.blockchain.wallet.api

import com.blockchain.domain.session.SessionIdService
import com.blockchain.utils.withBearerPrefix
import info.blockchain.wallet.ApiCode
import info.blockchain.wallet.api.data.Settings
import info.blockchain.wallet.api.data.Status
import info.blockchain.wallet.api.data.WalletOptions
import info.blockchain.wallet.payload.data.walletdto.WalletBaseDto
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import java.net.URLEncoder
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.ResponseBody
import org.spongycastle.util.encoders.Hex
import retrofit2.Response

class WalletApi(
    private val explorerInstance: WalletExplorerEndpoints,
    private val api: ApiCode,
    private val sessionIdService: SessionIdService,
    private val byPassCaptchaOrigin: String?,
    private val captchaSiteKey: String
) {
    fun updateFirebaseNotificationToken(
        token: String,
        guid: String,
        sharedKey: String
    ): Observable<ResponseBody> {
        return explorerInstance.postToWallet(
            "update-firebase",
            guid,
            sharedKey,
            token,
            token.length,
            api.apiCode
        )
    }

    fun removeFirebaseNotificationToken(
        guid: String,
        sharedKey: String
    ): Completable =
        explorerInstance.postToWallet(
            method = "revoke-firebase",
            guid = guid,
            sharedKey = sharedKey,
            payload = "",
            length = 0,
            apiCode = api.apiCode
        ).ignoreElements()

    fun sendSecureChannel(
        message: String
    ): Observable<ResponseBody> {
        return explorerInstance.postSecureChannel(
            "send-secure-channel",
            message,
            message.length,
            api.apiCode
        )
    }

    @Serializable
    class IPResponse {
        @SerialName("ip")
        var ip: String = ""
    }

    fun getExternalIP(): Single<String> {
        return explorerInstance.getExternalIp().map { it.ip }
    }

    fun setAccess(key: String, value: String, pin: String): Single<Response<Status>> {
        val hex = Hex.toHexString(value.toByteArray())
        return explorerInstance.pinStore(key, pin, hex, "put", api.apiCode)
    }

    fun validateAccess(key: String, pin: String): Single<Response<Status>> {
        return explorerInstance.pinStore(key, pin, null, "get", api.apiCode)
    }

    fun insertWallet(
        guid: String?,
        sharedKey: String?,
        activeAddressList: List<String>?,
        encryptedPayload: String,
        newChecksum: String?,
        email: String?,
        device: String?,
        recaptchaToken: String?
    ): Completable {
        val pipedAddresses = activeAddressList?.joinToString("|")

        return explorerInstance.syncWalletCall(
            sessionId = null,
            method = "insert",
            guid = guid,
            sharedKey = sharedKey,
            payload = encryptedPayload,
            length = encryptedPayload.length,
            checksum = URLEncoder.encode(newChecksum, "utf-8"),
            active = pipedAddresses,
            email = email,
            device = device,
            origin = byPassCaptchaOrigin,
            old_checksum = null,
            apiCode = api.apiCode,
            recaptchaToken = recaptchaToken,
            siteKey = captchaSiteKey
        )
    }

    fun submitCoinReceiveAddresses(guid: String, sharedKey: String, coinAddresses: String): Observable<ResponseBody> =
        explorerInstance.submitCoinReceiveAddresses(
            "subscribe-coin-addresses",
            sharedKey,
            guid,
            coinAddresses
        )

    fun updateWallet(
        guid: String?,
        sharedKey: String?,
        activeAddressList: List<String>?,
        encryptedPayload: String,
        newChecksum: String?,
        oldChecksum: String?,
        device: String?
    ): Completable {
        val pipedAddresses = activeAddressList?.joinToString("|") ?: ""
        return sessionIdService.sessionId().flatMapCompletable { sessionId ->
            explorerInstance.syncWalletCall(
                method = "update",
                guid = guid,
                sessionId = sessionId.withBearerPrefix(),
                sharedKey = sharedKey,
                payload = encryptedPayload,
                length = encryptedPayload.length,
                checksum = URLEncoder.encode(newChecksum, "utf-8"),
                email = pipedAddresses,
                origin = null,
                device = device,
                old_checksum = oldChecksum,
                apiCode = api.apiCode,
                recaptchaToken = null,
                siteKey = null,
                active = null
            )
        }
    }

    fun fetchWalletData(guid: String, sharedKey: String, sessionId: String): Single<WalletBaseDto> {
        return explorerInstance.fetchWalletData(
            "wallet.aes.json",
            guid,
            sessionId.withBearerPrefix(),
            sharedKey,
            "json",
            api.apiCode
        )
    }

    fun submitTwoFactorCode(guid: String?, twoFactorCode: String): Single<ResponseBody> {
        return sessionIdService.sessionId().flatMap {
            explorerInstance.submitTwoFactorCode(
                sessionId = it.withBearerPrefix(),
                method = "get-wallet",
                guid = guid,
                twoFactorCode = twoFactorCode,
                length = twoFactorCode.length,
                format = "plain",
                apiCode = api.apiCode
            )
        }
    }

    fun fetchEncryptedPayload(
        guid: String,
        sessionId: String,
        resend2FASms: Boolean
    ): Single<Response<ResponseBody>> =
        explorerInstance.fetchEncryptedPayload(
            guid,
            "SID=$sessionId",
            "json",
            resend2FASms,
            api.apiCode
        )

    fun fetchPairingEncryptionPasswordCall(guid: String?): Single<ResponseBody> {
        return explorerInstance.fetchPairingEncryptionPasswordCall(
            "pairing-encryption-password",
            guid,
            api.apiCode
        )
    }

    fun fetchPairingEncryptionPassword(guid: String?): Observable<ResponseBody> {
        return explorerInstance.fetchPairingEncryptionPassword(
            "pairing-encryption-password",
            guid,
            api.apiCode
        )
    }

    fun fetchSettings(method: String, guid: String, sharedKey: String): Observable<Settings> {
        return explorerInstance.fetchSettings(
            method,
            guid,
            sharedKey,
            "plain",
            api.apiCode
        )
    }

    fun updateSettings(
        method: String,
        guid: String,
        sharedKey: String,
        payload: String,
        context: String?
    ): Observable<ResponseBody> {
        return sessionIdService.sessionId().flatMapObservable { sessionId ->
            explorerInstance.updateSettings(
                sessionId.withBearerPrefix(),
                method,
                guid,
                sharedKey,
                payload,
                payload.length,
                "plain",
                context,
                api.apiCode
            )
        }
    }

    fun updateSettings(
        method: String,
        guid: String,
        sharedKey: String,
        payload: String,
        context: String?,
        forceJson: Boolean?
    ): Single<Response<ResponseBody>> {
        return sessionIdService.sessionId().flatMap { sessionId ->
            explorerInstance.updateSettings(
                sessionId.withBearerPrefix(),
                method,
                guid,
                sharedKey,
                payload,
                payload.length,
                "plain",
                context,
                api.apiCode,
                forceJson
            )
        }
    }

    val walletOptions: Observable<WalletOptions>
        get() = explorerInstance.getWalletOptions(api.apiCode)

    fun authorizeSession(authToken: String): Single<Response<ResponseBody>> =
        sessionIdService.sessionId().flatMap {
            explorerInstance.authorizeSession(
                it.withBearerPrefix(),
                authToken,
                api.apiCode,
                "authorize-approve",
                true
            )
        }

    fun updateMobileSetup(
        guid: String,
        sharedKey: String,
        isMobileSetup: Boolean,
        deviceType: Int
    ): Completable {
        return explorerInstance.updateMobileSetup(
            "update-mobile-setup",
            guid,
            sharedKey,
            isMobileSetup,
            deviceType
        )
    }

    fun updateMnemonicBackup(guid: String, sharedKey: String): Single<ResponseBody> {
        return explorerInstance.updateMnemonicBackup(
            "update-mnemonic-backup",
            guid,
            sharedKey
        )
    }

    fun verifyCloudBackup(
        guid: String,
        sharedKey: String,
        hasCloudBackup: Boolean,
        deviceType: Int
    ): Completable {
        return explorerInstance.verifyCloudBackup(
            "verify-cloud-backup",
            guid,
            sharedKey,
            hasCloudBackup,
            deviceType
        )
    }

    fun getDeeplinkPayload(
        sessionId: String
    ): Single<ResponseBody> = explorerInstance.getDeeplinkPayload(
        sessionId = sessionId.withBearerPrefix()
    )

    fun updateLoginApprovalStatus(
        sessionId: String,
        payload: String,
        confirmDevice: Boolean
    ): Completable = explorerInstance.updateDeeplinkApprovalStatus(
        method = "authorize-verify-device",
        sessionId = sessionId,
        payload = payload,
        confirmDevice = confirmDevice
    )
}
