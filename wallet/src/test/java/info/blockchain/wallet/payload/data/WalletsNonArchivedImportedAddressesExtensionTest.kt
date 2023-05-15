package info.blockchain.wallet.payload.data

import org.amshove.kluent.`should be equal to`
import org.junit.Test

class WalletsNonArchivedImportedAddressesExtensionTest {

    private fun importedAddressWithPrivateKey(address: String, privateKey: String = "PRIVATE_KEY") =
        ImportedAddress(
            privateKey = privateKey,
            address = address,
            _tag = 0
        )

    @Test
    fun `empty list`() {
        Wallet.fromJson(json, version).nonArchivedImportedAddressStrings() `should be equal to` emptyList()
    }

    @Test
    fun `one spendable`() {
        Wallet.fromJson(json, version).addImportedAddress(importedAddressWithPrivateKey("Address1"))
            .nonArchivedImportedAddressStrings() `should be equal to` listOf("Address1")
    }

    @Test
    fun `one archived`() {
        Wallet.fromJson(json, version)
            .addImportedAddress(importedAddressWithPrivateKey("Address1").updateArchivedState(true))
            .nonArchivedImportedAddressStrings() `should be equal to` emptyList()
    }

    @Test
    fun `one without private key`() {
        Wallet.fromJson(json, version).addImportedAddress(importedAddressWithPrivateKey("Address1"))
            .nonArchivedImportedAddressStrings() `should be equal to` listOf("Address1")
    }

    @Test
    fun `two spendable`() {
        Wallet.fromJson(json, version)
            .addImportedAddress(importedAddressWithPrivateKey("Address1", "PRIVATE_KEY1"))
            .addImportedAddress(importedAddressWithPrivateKey("Address2", "PRIVATE_KEY2"))
            .nonArchivedImportedAddressStrings() `should be equal to` listOf("Address1", "Address2")
    }

    @Test
    fun `repeated address`() {
        Wallet.fromJson(json, version).addImportedAddress(importedAddressWithPrivateKey("Address1", "PRIVATE_KEY1"))
            .addImportedAddress(importedAddressWithPrivateKey("Address1", "PRIVATE_KEY2"))
            .nonArchivedImportedAddressStrings() `should be equal to` listOf("Address1")
    }
}

private const val version = 4
private const val json = "{\n" +
    "  \"guid\": \"a09910d9-1906-4ea1-a956-2508c3fe0661\",\n" +
    "  \"sharedKey\": \"d14f3d2c-f883-40da-87e2-c8448521ee64\",\n" +
    "  \"double_encryption\": true,\n" +
    "  \"dpasswordhash\": \"1f7cb884545e89e4083c10522bf8b991e8e13551aa5816110cb9419277fb4652\",\n" +
    "  \"tx_notes\": {\n" +
    "    \"94a4934712fd40f2b91b7be256eacad49a50b850c949313b07046664d24c0e4c\": \"Bought Pizza\"\n" +
    "  },\n" +
    "  \"options\": {\n" +
    "    \"pbkdf2_iterations\": 5000,\n" +
    "    \"fee_per_kb\": 10000,\n" +
    "    \"html5_notifications\": false,\n" +
    "    \"logout_time\": 3600000\n" +
    "  },\n" +
    "  \"tag_names\": []\n" +
    "}"
