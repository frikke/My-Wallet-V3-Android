package piuk.blockchain.android.ui.auth.newlogin.data.repository

import com.blockchain.coreandroid.utils.pubKeyHash
import com.blockchain.domain.auth.SecureChannelBrowserMessage
import com.blockchain.domain.auth.SecureChannelLoginData
import com.blockchain.domain.auth.SecureChannelService
import com.blockchain.notifications.models.NotificationPayload
import com.blockchain.preferences.AuthPrefs
import com.blockchain.preferences.BrowserIdentity
import com.blockchain.preferences.SecureChannelPrefs
import info.blockchain.wallet.api.WalletApi
import info.blockchain.wallet.crypto.ECDHUtil
import info.blockchain.wallet.keys.SigningKey
import info.blockchain.wallet.payload.PayloadManager
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.schedulers.Schedulers
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import piuk.blockchain.android.ui.auth.newlogin.data.model.SecureChannelBrowserMessageDto
import piuk.blockchain.android.ui.auth.newlogin.data.model.SecureChannelMessageDto
import piuk.blockchain.android.ui.auth.newlogin.data.model.SecureChannelPairingCodeDto
import piuk.blockchain.android.ui.auth.newlogin.data.model.SecureChannelPairingResponseDto
import piuk.blockchain.android.ui.auth.newlogin.data.model.toDomain
import timber.log.Timber

class SecureChannelRepository(
    private val secureChannelPrefs: SecureChannelPrefs,
    private val authPrefs: AuthPrefs,
    private val payloadManager: PayloadManager,
    private val walletApi: WalletApi
) : SecureChannelService {

    private val _secureLogin: MutableSharedFlow<SecureChannelLoginData> = MutableSharedFlow(
        replay = 0
    )

    override val secureLoginAttempted: Flow<SecureChannelLoginData>
        get() = _secureLogin

    private val compositeDisposable = CompositeDisposable()

    private val jsonBuilder = Json {
        classDiscriminator = "class"
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    override fun sendErrorMessage(channelId: String, pubKeyHash: String) {
        sendMessage(SecureChannelMessageDto.Empty, channelId, pubKeyHash, false)
        secureChannelPrefs.removeBrowserIdentity(pubKeyHash)
    }

    override fun sendHandshake(json: String) {
        val secureChannelPairingCode = jsonBuilder.decodeFromString<SecureChannelPairingCodeDto>(json)

        val browserIdentity = BrowserIdentity(secureChannelPairingCode.pubkey)
        secureChannelPrefs.addBrowserIdentity(browserIdentity)

        val handshake = SecureChannelMessageDto.PairingHandshake(
            authPrefs.walletGuid,
            secureChannelPairingCode.pubkey
        )

        sendMessage(handshake, secureChannelPairingCode.channelId, browserIdentity.pubKeyHash(), true)
    }

    override fun sendLoginMessage(channelId: String, pubKeyHash: String) {
        val loginMessage = SecureChannelMessageDto.Login(
            guid = authPrefs.walletGuid,
            password = payloadManager.tempPassword,
            sharedKey = authPrefs.sharedKey,
            remember = true
        )

        sendMessage(loginMessage, channelId, pubKeyHash, true)
        secureChannelPrefs.updateBrowserIdentityUsedTimestamp(pubKeyHash)
    }

    override suspend fun secureChannelLogin(payload: Map<String, String?>) {
        val pubKeyHash = payload[NotificationPayload.PUB_KEY_HASH] ?: return

        val messageRawEncrypted = payload[NotificationPayload.DATA_MESSAGE] ?: return

        val message = decryptMessage(pubKeyHash, messageRawEncrypted) ?: return

        _secureLogin.emit(
            SecureChannelLoginData(
                pubKeyHash = pubKeyHash,
                message = message,
                originIp = payload[ORIGIN_IP] ?: "",
                originLocation = payload[ORIGIN_COUNTRY] ?: "",
                originBrowser = payload[ORIGIN_BROWSER] ?: ""
            )
        )
    }

    override fun decryptMessage(pubKeyHash: String, messageEncrypted: String): SecureChannelBrowserMessage? {
        val identity = secureChannelPrefs.getBrowserIdentity(pubKeyHash)
            ?: return null

        val json = ECDHUtil.getDecryptedMessage(
            getDeviceKey(),
            identity,
            messageEncrypted
        ).toString(Charset.defaultCharset())
        val message = jsonBuilder.decodeFromString<SecureChannelBrowserMessageDto>(json)

        if (System.currentTimeMillis() - message.timestamp > TimeUnit.MINUTES.toMillis(TIME_OUT_IN_MINUTES)) {
            return null
        }

        return message.toDomain()
    }

    private fun sendMessage(
        msg: SecureChannelMessageDto,
        channelId: String,
        pubKeyHash: String,
        success: Boolean
    ) {
        val browserIdentity = secureChannelPrefs.getBrowserIdentity(pubKeyHash)
            ?: throw RuntimeException() // If we get here, die
        val signingKey = getDeviceKey()

        val response = SecureChannelPairingResponseDto(
            channelId = channelId,
            pubkey = ECDHUtil.getPublicKeyAsHexString(signingKey),
            success = success,
            message = ECDHUtil.getEncryptedMessage(
                signingKey = signingKey,
                browserIdentity = browserIdentity,
                serializedMessage = jsonBuilder.encodeToString(msg)
            )
        )
        compositeDisposable +=
            walletApi.sendSecureChannel(
                jsonBuilder.encodeToString(response)
            )
                .ignoreElements()
                .subscribeOn(Schedulers.io())
                .subscribe({ /*no-op*/ }, { Timber.e(it) })
    }

    private fun getDeviceKey() = SigningKey.createSigningKeyFromPrivateKey(secureChannelPrefs.deviceKey)

    companion object {
        private const val TIME_OUT_IN_MINUTES: Long = 10
        private const val ORIGIN_IP = "origin_ip"
        private const val ORIGIN_COUNTRY = "origin_country"
        private const val ORIGIN_BROWSER = "origin_browser"
    }
}
