package info.blockchain.wallet.payload.data

import com.blockchain.serialization.JsonSerializableAccount
import info.blockchain.wallet.LabeledAccount
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject

@Serializable(with = AccountSerializer::class)
interface Account : JsonSerializableAccount, LabeledAccount {
    override val label: String
    val xpriv: String
    val xpubs: XPubs
    val addressLabels: List<AddressLabel>

    fun xpubForDerivation(derivation: String): String?

    fun containsXpub(xpub: String): Boolean

    fun withEncryptedPrivateKey(encryptedKey: String): Account
    fun addAddressLabel(index: Int, reserveLabel: String): Account
    fun updateLabel(label: String): Account
    override fun updateArchivedState(isArchived: Boolean): Account
}

object AccountSerializer : JsonContentPolymorphicSerializer<Account>(Account::class) {
    override fun selectDeserializer(element: JsonElement) = when {
        "derivations" in element.jsonObject -> AccountV4.serializer()
        else -> AccountV3.serializer()
    }
}
