package info.blockchain.wallet.api

import com.blockchain.utils.withBearerPrefix
import info.blockchain.wallet.ApiCode
import info.blockchain.wallet.api.data.Settings
import info.blockchain.wallet.api.data.Status
import info.blockchain.wallet.api.data.WalletOptions
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import java.net.URLEncoder
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.ResponseBody
import org.spongycastle.util.encoders.Hex
import retrofit2.Call
import retrofit2.Response

class WalletApi(
    private val explorerInstance: WalletExplorerEndpoints,
    private val api: ApiCode
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

    fun setAccess(key: String, value: String, pin: String): Observable<Response<Status>> {
        val hex = Hex.toHexString(value.toByteArray())
        return explorerInstance.pinStore(key, pin, hex, "put", api.apiCode)
    }

    fun validateAccess(key: String, pin: String): Observable<Response<Status>> {
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
    ): Call<ResponseBody> {
        val pipedAddresses = activeAddressList?.joinToString("|")

        return explorerInstance.syncWalletCall(
            "insert",
            guid,
            sharedKey,
            encryptedPayload,
            encryptedPayload.length,
            URLEncoder.encode(newChecksum, "utf-8"),
            pipedAddresses,
            email,
            device,
            null,
            api.apiCode,
            recaptchaToken
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
    ): Call<ResponseBody> {

        val pipedAddresses = activeAddressList?.joinToString("|") ?: ""

        return explorerInstance.syncWalletCall(
            "update",
            guid,
            sharedKey,
            encryptedPayload,
            encryptedPayload.length,
            URLEncoder.encode(newChecksum, "utf-8"),
            pipedAddresses,
            null,
            device,
            oldChecksum,
            api.apiCode,
            null
        )
    }

    fun fetchWalletData(guid: String, sharedKey: String): Call<ResponseBody> {
        return explorerInstance.fetchWalletData(
            "wallet.aes.json",
            guid,
            sharedKey,
            "json",
            api.apiCode
        )
    }

    fun submitTwoFactorCode(sessionId: String, guid: String?, twoFactorCode: String): Observable<ResponseBody> {
        val headerMap: MutableMap<String, String> =
            HashMap()
        headerMap["Authorization"] = sessionId.withBearerPrefix()
        return explorerInstance.submitTwoFactorCode(
            headerMap,
            "get-wallet",
            guid,
            twoFactorCode,
            twoFactorCode.length,
            "plain",
            api.apiCode
        )
    }

    fun getSessionId(guid: String): Observable<Response<ResponseBody>> {
        return explorerInstance.getSessionId(guid)
    }

    fun fetchEncryptedPayload(
        guid: String,
        sessionId: String,
        resend2FASms: Boolean
    ): Observable<Response<ResponseBody>> =
        explorerInstance.fetchEncryptedPayload(
            guid,
            "SID=$sessionId",
            "json",
            resend2FASms,
            api.apiCode
        )

    fun fetchPairingEncryptionPasswordCall(guid: String?): Call<ResponseBody> {
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
        return explorerInstance.updateSettings(
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

    fun updateSettings(
        method: String,
        guid: String,
        sharedKey: String,
        payload: String,
        context: String?,
        forceJson: Boolean?
    ): Single<Response<ResponseBody>> {
        return explorerInstance.updateSettings(
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

    val walletOptions: Observable<WalletOptions>
        get() = explorerInstance.getWalletOptions(api.apiCode)

    fun createSessionId(email: String): Single<ResponseBody> =
        explorerInstance.createSessionId(email, api.apiCode)

    fun authorizeSession(authToken: String, sessionId: String): Single<Response<ResponseBody>> =
        explorerInstance.authorizeSession(
            sessionId.withBearerPrefix(),
            authToken,
            api.apiCode,
            "authorize-approve",
            true
        )

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
    ): Single<ResponseBody> {
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
