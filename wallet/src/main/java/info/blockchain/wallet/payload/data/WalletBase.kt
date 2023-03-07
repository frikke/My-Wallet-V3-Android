package info.blockchain.wallet.payload.data

import com.blockchain.serialization.JsonSerializableAccount
import info.blockchain.wallet.crypto.AESUtil
import info.blockchain.wallet.exceptions.DecryptionException
import info.blockchain.wallet.payload.data.WalletWrapper.Companion.wrap
import info.blockchain.wallet.payload.data.walletdto.WalletBaseDto
import info.blockchain.wallet.util.FormatsUtil.isValidJson
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import org.json.JSONObject
import org.spongycastle.crypto.paddings.ISO10126d2Padding
import org.spongycastle.crypto.paddings.ISO7816d4Padding
import org.spongycastle.crypto.paddings.ZeroBytePadding
import org.spongycastle.util.encoders.Hex

class WalletBase constructor(private val walletBaseDto: WalletBaseDto) {
    lateinit var wallet: Wallet

    constructor(wallet: Wallet) : this(walletBaseDto = WalletBaseDto.withDefaults()) {
        this.wallet = wallet
    }

    private constructor(walletBaseDto: WalletBaseDto, walletBody: Wallet) : this(walletBaseDto) {
        this.wallet = walletBody
    }

    fun withWalletBody(walletBody: Wallet): WalletBase {
        return WalletBase(walletBaseDto, walletBody)
    }

    fun withUpdatedDerivationsForAccounts(accounts: List<Account>): WalletBase {
        return withWalletBody(wallet.withUpdatedDerivationsForAccounts(accounts))
    }

    fun withUpdatedDefaultDerivationTypeForAccounts(accounts: List<Account>): WalletBase {
        return withWalletBody(wallet.withUpdatedDefaultDerivationTypeForAccounts(accounts))
    }

    private fun decryptPayload(password: String): Wallet {
        return if (!isV1Wallet()) {
            decryptV3OrV4Wallet(password)
        } else {
            decryptV1Wallet(password)
        }
    }

    private fun decryptV3OrV4Wallet(password: String): Wallet {
        val walletWrapperBody = WalletWrapper.fromJson(walletBaseDto.payload)
        return walletWrapperBody.decryptPayload(password)
    }

    /*
        No need to encrypt V1 wallet again. We will force user to upgrade to V3
     */
    private fun decryptV1Wallet(password: String): Wallet {
        var decrypted: String? = null
        var succeededIterations = -1000
        val iterations =
            intArrayOf(DEFAULT_PBKDF2_ITERATIONS_V1_A, DEFAULT_PBKDF2_ITERATIONS_V1_B)
        val modes = intArrayOf(AESUtil.MODE_CBC, AESUtil.MODE_OFB)
        val paddings = arrayOf(
            ISO10126d2Padding(),
            ISO7816d4Padding(),
            ZeroBytePadding(),
            null // NoPadding
        )
        outerloop@ for (iteration in iterations) {
            for (mode in modes) {
                for (padding in paddings) {
                    try {
                        decrypted = AESUtil.decryptWithSetMode(
                            walletBaseDto.payload,
                            password,
                            iteration,
                            mode,
                            padding
                        )
                        // Ensure it's parsable
                        JSONObject(decrypted)
                        succeededIterations = iteration
                        break@outerloop
                    } catch (e: Exception) {
                    }
                }
            }
        }
        if (decrypted == null || succeededIterations < 0) {
            throw DecryptionException("Failed to decrypt")
        }
        val decryptedPayload = decrypted
        return Wallet.fromJson(decryptedPayload, 1)
    }

    fun withUpdatedChecksum(checksum: String): WalletBase {
        return WalletBase(
            walletBaseDto.withUpdatedPayloadCheckSum(checksum),
            wallet
        )
    }

    fun withDecryptedPayload(password: String): WalletBase {
        return WalletBase(
            walletBaseDto, decryptPayload(password)
        )
    }

    fun withSyncedPubKeys(): WalletBase {
        return WalletBase(
            walletBaseDto.withSyncedKeys(),
            wallet
        )
    }

    fun isV1Wallet(): Boolean {
        return !isValidJson(walletBaseDto.payload)
    }

    fun toJson() =
        walletBaseDto.toJson()

    fun encryptAndWrapPayload(password: String): Pair<String, WalletWrapper> {
        val version = wallet.wrapperVersion
        val iterations = wallet.options.pbkdf2Iterations!!
        val encryptedPayload = AESUtil.encrypt(wallet.toJson(), password, iterations)
        val wrapperBody = wrap(encryptedPayload, version, iterations)
        val checkSum = String(
            Hex.encode(
                MessageDigest.getInstance("SHA-256")
                    .digest(wrapperBody.toJson().toByteArray(StandardCharsets.UTF_8))
            )
        )
        return checkSum to wrapperBody
    }

    val payloadChecksum: String?
        get() = walletBaseDto.payloadChecksum

    val syncPubkeys: Boolean
        get() = walletBaseDto.syncPubkeys

    fun withUpdatedLabel(jsonSerializableAccount: JsonSerializableAccount, label: String?): WalletBase {
        if (jsonSerializableAccount is Account) {
            return withWalletBody(wallet.updateAccountLabel(jsonSerializableAccount, label))
        }
        if (jsonSerializableAccount is ImportedAddress) {
            return withWalletBody(wallet.updateImportedAddressLabel(jsonSerializableAccount, label))
        }
        throw UnsupportedOperationException("Cannot update label of " + jsonSerializableAccount.javaClass.name)
    }

    fun withUpdatedAccountsLabel(accounts: Map<Account, String>): WalletBase {
        return withWalletBody(wallet.updateAccountsLabel(accounts))
    }

    fun withUpdatedAccountState(account: JsonSerializableAccount?, isArchived: Boolean): WalletBase {
        return withWalletBody(wallet.updateArchivedState(account, isArchived))
    }

    fun withMnemonicState(verified: Boolean): WalletBase {
        return withWalletBody(wallet.updateMnemonicVerifiedState(verified))
    }

    fun isInitialised(): Boolean =
        this::wallet.isInitialized

    companion object {
        fun fromJson(json: String): WalletBase {
            return WalletBase(WalletBaseDto.fromJson(json))
        }
    }
}

private const val DEFAULT_PBKDF2_ITERATIONS_V1_A = 1
private const val DEFAULT_PBKDF2_ITERATIONS_V1_B = 10
