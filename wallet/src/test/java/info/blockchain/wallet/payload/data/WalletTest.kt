package info.blockchain.wallet.payload.data

import com.nhaarman.mockitokotlin2.mock
import info.blockchain.wallet.ImportedAddressHelper.getImportedAddress
import info.blockchain.wallet.WalletApiMockedResponseTest
import info.blockchain.wallet.crypto.AESUtil
import info.blockchain.wallet.exceptions.DecryptionException
import info.blockchain.wallet.exceptions.HDWalletException
import info.blockchain.wallet.exceptions.NoSuchAddressException
import info.blockchain.wallet.keys.SigningKeyImpl
import info.blockchain.wallet.payment.OutputType
import info.blockchain.wallet.payment.Payment
import info.blockchain.wallet.util.DoubleEncryptionFactory
import info.blockchain.wallet.util.parseUnspentOutputsAsUtxoList
import java.io.IOException
import java.math.BigInteger
import org.bitcoinj.core.Base58
import org.bitcoinj.core.ECKey
import org.bitcoinj.crypto.DeterministicKey
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.spongycastle.util.encoders.Hex

/*
WalletBase
   |
   |__WalletWrapper
           |
           |__Wallet
*/

class WalletTest : WalletApiMockedResponseTest() {

    private fun givenWalletFromResource(resourceName: String, version: Int = 3): Wallet {
        return try {
            Wallet.fromJson(loadResourceContent(resourceName), version)
        } catch (e: HDWalletException) {
            throw RuntimeException(e)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    @Test
    fun fromJson_v4_1() {
        val resourceName = "wallet_body_v4_1.json"
        val wallet = givenWalletFromResource(resourceName, 4)
        assertEquals("a09910d9-1906-4ea1-a956-2508c3fe0661", wallet.guid)
        assertEquals("d14f3d2c-f883-40da-87e2-c8448521ee64", wallet.sharedKey)
        assertTrue(wallet.isDoubleEncryption)
        assertEquals("1f7cb884545e89e4083c10522bf8b991e8e13551aa5816110cb9419277fb4652", wallet.dpasswordhash)
        for ((key, value) in wallet.txNotes) {
            assertEquals("94a4934712fd40f2b91b7be256eacad49a50b850c949313b07046664d24c0e4c", key)
            assertEquals("Bought Pizza", value)
        }

        // Options parsing tested in OptionsTest
        assertNotNull(wallet.options)

        // HdWallets parsing tested in HdWalletsBodyTest
        assertNotNull(wallet.walletBodies)
        for (account in wallet.walletBody!!.accounts) {
            assertNotNull(account.label)
            assertNotNull(account.getDefaultXpub())
            assertNotNull(account.xpriv)
        }

        // Keys parsing tested in KeysBodyTest
        assertNotNull(wallet.importedAddressList)
    }

    @Test
    fun fromJson_1() {
        val resourceName = "wallet_body_1.txt"
        val wallet = givenWalletFromResource(resourceName)
        assertEquals("a09910d9-1906-4ea1-a956-2508c3fe0661", wallet.guid)
        assertEquals("d14f3d2c-f883-40da-87e2-c8448521ee64", wallet.sharedKey)
        assertTrue(wallet.isDoubleEncryption)
        assertEquals("1f7cb884545e89e4083c10522bf8b991e8e13551aa5816110cb9419277fb4652", wallet.dpasswordhash)
        for ((key, value) in wallet.txNotes) {
            assertEquals("94a4934712fd40f2b91b7be256eacad49a50b850c949313b07046664d24c0e4c", key)
            assertEquals("Bought Pizza", value)
        }

        // Options parsing tested in OptionsTest
        assertNotNull(wallet.getOptions())

        // HdWallets parsing tested in HdWalletsBodyTest
        assertNotNull(wallet.walletBodies)

        // Keys parsing tested in KeysBodyTest
        assertNotNull(wallet.importedAddressList)
    }

    @Test
    fun fromJson_2() {
        val wallet = givenWalletFromResource("wallet_body_2.txt")
        assertEquals("9ebb4d4f-f36e-40d6-9a3e-5a3cca5f83d6", wallet.guid)
        assertEquals("41cf823f-2dcd-4967-88d1-ef9af8689fc6", wallet.sharedKey)
        assertFalse(wallet.isDoubleEncryption)
        assertTrue(wallet.dpasswordhash.isEmpty())

        // Options parsing tested in OptionsTest
        assertNotNull(wallet.options)

        // Keys parsing tested in KeysBodyTest
        assertNotNull(wallet.importedAddressList)
    }

    @Test
    fun fromJson_3() {
        val wallet = givenWalletFromResource("wallet_body_3.txt")
        assertEquals("2ca9b0e4-6b82-4dae-9fef-e8b300c72aa2", wallet.guid)
        assertEquals("e8553981-b196-47cc-8858-5b0d16284f61", wallet.sharedKey)
        assertFalse(wallet.isDoubleEncryption)
        assertTrue(wallet.dpasswordhash.isEmpty())

        // old wallet_options should have created new options
        assertNotNull(wallet.options)
        assertEquals(10, wallet.options.pbkdf2Iterations!!.toLong())

        // Keys parsing tested in KeysBodyTest
        assertNotNull(wallet.importedAddressList)
    }

    @Test
    fun fromJson_4() {
        val wallet = givenWalletFromResource("wallet_body_4.txt")
        assertEquals("4077b6d9-73b3-4d22-96d4-9f8810fec435", wallet.guid)
        assertEquals("fa1beb37-5836-41d1-9f73-09f292076eb9", wallet.sharedKey)
    }

    @Test
    fun testToJSON() {
        // Ensure toJson doesn't write any unintended fields
        val wallet = givenWalletFromResource("wallet_body_1.txt")
        val jsonString = wallet.toJson()
        val jsonObject = JSONObject(jsonString)
        assertEquals(9, jsonObject.keySet().size.toLong())
    }

    @Test
    fun validateSecondPassword() {
        val wallet = givenWalletFromResource("wallet_body_1.txt")
        wallet.validateSecondPassword("hello")
        assertTrue(true)
    }

    @Test(expected = DecryptionException::class)
    fun validateSecondPassword_fail() {
        val wallet = givenWalletFromResource("wallet_body_1.txt")
        wallet.validateSecondPassword("bogus")
    }

    @Test
    fun addAccount() {
        val wallet = givenWalletFromResource("wallet_body_6.txt")
        assertEquals(1, wallet.walletBody!!.accounts.size.toLong())

        val updatedWallet = wallet.addAccount("Some Label", null)
        assertEquals(2, updatedWallet.walletBody!!.accounts.size.toLong())

        val account = updatedWallet.walletBody!!.getAccount(updatedWallet.walletBody!!.accounts.lastIndex)
        assertEquals(
            "xpub6DTFzKMsjf1Tt9KwHMYnQxMLGuVRcobDZdz" +
                "Duhtc6xfvafsBFqsBS4RNM54kdJs9zK8RKkSbjSbwCeUJjxiySaBKTf8dmyXgUgVnFY7yS9x",
            account.xpubs.forDerivation(XPub.Format.LEGACY)!!.address
        )
        assertEquals(
            "xpub6BsCfcSjCNfohZn4QkmCiqXpM5LtdW5kUxjCvAHL4jNYzduSaaYYEAFw6ZvtuhuwRDByfqj2X83qEAxqYUcj9pjVT6QFGnoTLgoAZHmcP2Q",
            account.xpubs.forDerivation(XPub.Format.SEGWIT)!!.address
        )
        assertEquals(
            "xprv9xsrG6uqN17WV5hbJjECMhb5o3WQE3Mu7joc7msiWPqa7qaJ33EHgMwTFJir" +
                "WYw23vhdN8dRLgyVnzmHyKiFFKEPoyMG3KtbkXPsWTJJdtx",
            account.xpriv
        )
    }

    @Test(expected = DecryptionException::class)
    fun addAccount_doubleEncryptionError() {
        val wallet = givenWalletFromResource("wallet_body_6.txt")
        assertEquals(1, wallet.walletBody!!.accounts.size.toLong())
        wallet.addAccount("Some Label", "hello")
    }

    @Test
    fun addAccount_doubleEncrypted() {
        val wallet = givenWalletFromResource("wallet_body_7.txt")
        assertEquals(2, wallet.walletBody!!.accounts.size.toLong())

        val newWallet = wallet.addAccount("Some Label", "hello")
        assertEquals(3, newWallet.walletBody!!.accounts.size.toLong())
        /**
         * making sure we are immutable
         */
        assertEquals(2, wallet.walletBody!!.accounts.size.toLong())

        val account = newWallet.walletBody?.getAccount(newWallet.walletBody!!.accounts.lastIndex)
        assertEquals(
            "xpub6CD71szkXY38oQSXfwbYvi98PjbcPftH8rQ2hyXddeHRwuRuPBnDCtgE55hjyuPX8fx1yVMGTw5jqhJ5UeL7iz7Lp5QdGJtmuAmmFGkPrTe",
            account!!.getDefaultXpub()
        )

        // Private key will be encrypted
        val decryptedXpriv = DoubleEncryptionFactory.decrypt(
            account.xpriv,
            wallet.sharedKey,
            "hello",
            wallet.options.pbkdf2Iterations!!
        )
        /**
         * Segwit derivation
         */
        assertEquals(
            "xprv9yDkcNTrhAUqavN4Zv4YZaCPqhm7zDARmdURub825" +
                "JkT576kqeTxf6MkDoS1z4gUM8BvZ4QqQEJTPKc7kCmW3NowhVdHVyZTrAEqkQgm2uw",
            decryptedXpriv
        )
    }

    @Test
    fun addLegacyAddress() {
        val wallet = givenWalletFromResource("wallet_body_6.txt")
        assertEquals(0, wallet.importedAddressList.size.toLong())

        mockInterceptor?.setResponseString(
            "cb600366ef7a94b991aa04557fc1d9c272ba00df6b1d9791d71c66efa0ae7fe9"
        )
        val importedAddressWallet = wallet.addImportedAddress(getImportedAddress())
        assertEquals(1, importedAddressWallet.importedAddressList.size.toLong())

        val address = importedAddressWallet.importedAddressList.last()

        assertNotNull(address.privateKey)
        assertNotNull(address.address)
        assertEquals("1", address.address.substring(0, 1))
    }

    @Test
    fun addLegacyAddress_doubleEncrypted() {
        val wallet = givenWalletFromResource("wallet_body_1.txt")
        assertEquals(19, wallet.importedAddressList.size.toLong())
        mockInterceptor?.setResponseString(
            "cb600366ef7a94b991aa04557fc1d9c272ba00df6b1d9791d71c66efa0ae7fe9"
        )

        val address = wallet.importedAddressFromKey(
            SigningKeyImpl(
                DeterministicKey.fromPrivate(Base58.decode(getImportedAddress().privateKey))
            ),
            "hello",
            "lala",
            "apicode"
        )

        val walletWithNewAddress = wallet.addImportedAddress(address)
        assertEquals(20, walletWithNewAddress.importedAddressList.size.toLong())

        val newAddress = walletWithNewAddress.importedAddressList[wallet.importedAddressList.size - 1]
        assertNotNull(newAddress.privateKey)
        assertNotNull(newAddress.address)
        assertEquals("==", newAddress.privateKey!!.substring(newAddress.privateKey!!.length - 2))
        assertEquals("1", newAddress.address.substring(0, 1))
    }

    @Test
    fun setKeyForLegacyAddress() {
        val wallet = givenWalletFromResource("wallet_body_6.txt")
        mockInterceptor?.setResponseString(
            "cb600366ef7a94b991aa04557fc1d9c272ba00df6b1d9791d71c66efa0ae7fe9"
        )
        val withImportedAddressWallet = wallet.addImportedAddress(getImportedAddress())
        val address =
            withImportedAddressWallet.importedAddressList.last()
        val key = SigningKeyImpl(DeterministicKey.fromPrivate(Base58.decode(address.privateKey)))

        val updatedWallet = withImportedAddressWallet.updateKeyForImportedAddress(key, null).let {
            wallet.replaceOrAddImportedAddress(it)
        }
        assertEquals(updatedWallet.importedAddressList.last().privateKey, "tb1TutW9CCZUqsXQ9nhvatCW51sauRJapY5YpW3zddF")
    }

    @Test(expected = NoSuchAddressException::class)
    fun setKeyForLegacyAddress_NoSuchAddressException() {
        val wallet = givenWalletFromResource("wallet_body_6.txt")
        mockInterceptor?.setResponseString(
            "cb600366ef7a94b991aa04557fc1d9c272ba00df6b1d9791d71c66efa0ae7fe9"
        )
        val withImportedAddressWallet = wallet.addImportedAddress(getImportedAddress())

        // Try to set address key with ECKey not found in available addresses.
        val key = SigningKeyImpl(ECKey())
        withImportedAddressWallet.updateKeyForImportedAddress(key, null)
    }

    @Test
    fun setKeyForLegacyAddress_doubleEncrypted() {
        val walletFromResource = givenWalletFromResource("wallet_body_1.txt")
        mockInterceptor?.setResponseString(
            "cb600366ef7a94b991aa04557fc1d9c272ba00df6b1d9791d71c66efa0ae7fe9"
        )

        val address = walletFromResource.importedAddressFromKey(
            SigningKeyImpl(
                DeterministicKey.fromPrivate(Base58.decode(getImportedAddress().privateKey))
            ),
            "hello",
            "2312",
            "41231"
        )

        val wallet = walletFromResource.addImportedAddress(address)

        val decryptedOriginalPrivateKey = AESUtil
            .decrypt(
                address.privateKey,
                wallet.sharedKey + "hello",
                wallet.options.pbkdf2Iterations!!
            )

        // Same key for created address, but unencrypted
        val key = SigningKeyImpl(
            DeterministicKey.fromPrivate(Base58.decode(decryptedOriginalPrivateKey))
        )

        // Set private key
        val unecryptedAddressKey = walletFromResource.importedAddressFromKey(
            key,
            "hello",
            "2312",
            "41231"
        )
        val unecryptedAddressWallet = wallet.addImportedAddress(unecryptedAddressKey)

        // Get new set key
        val addr = unecryptedAddressWallet.importedAddressList[wallet.importedAddressList.size - 1]
        val decryptedSetPrivateKey = AESUtil
            .decrypt(
                addr.privateKey,
                unecryptedAddressWallet.sharedKey + "hello",
                unecryptedAddressWallet.options.pbkdf2Iterations!!
            )

        // Original private key must match newly set private key (unencrypted)
        assertEquals(decryptedOriginalPrivateKey, decryptedSetPrivateKey)
    }

    @Test(expected = DecryptionException::class)
    fun setKeyForLegacyAddress_DecryptionException() {
        val wallet = givenWalletFromResource("wallet_body_1.txt")
        mockInterceptor?.setResponseString(
            "cb600366ef7a94b991aa04557fc1d9c272ba00df6b1d9791d71c66efa0ae7fe9"
        )
        wallet.addImportedAddress(getImportedAddress())
        val address = wallet.importedAddressList[wallet.importedAddressList.size - 1]
        val decryptedOriginalPrivateKey = AESUtil
            .decrypt(
                address.privateKey,
                wallet.sharedKey + "hello",
                wallet.options.pbkdf2Iterations!!
            )

        // Same key for created address, but unencrypted
        val key = SigningKeyImpl(
            DeterministicKey.fromPrivate(Base58.decode(decryptedOriginalPrivateKey))
        )

        // Set private key
        val updatedWallet = wallet.updateKeyForImportedAddress(key, "bogus").let {
            wallet.replaceOrAddImportedAddress(it)
        }
        assertEquals("123", updatedWallet.importedAddressList.last().privateKey)
    }

    @Test
    fun decryptHDWallet() {
        val wallet = givenWalletFromResource("wallet_body_1.txt")
        wallet.decryptHDWallet("hello")
    }

    @Test(expected = DecryptionException::class)
    fun decryptHDWallet_DecryptionException() {
        val wallet = givenWalletFromResource("wallet_body_1.txt")
        wallet.decryptHDWallet("bogus")
    }

    @Test
    fun getMasterKey() {
        val wallet = givenWalletFromResource("wallet_body_1.txt")
        wallet.decryptHDWallet("hello")
        assertEquals(
            "4NPYyXS5fhyoTHgDPt81cQ4838j1tRwmeRbK8pGLB1Xg",
            Base58.encode(wallet.walletBody!!.getMasterKey()!!.toDeterministicKey().privKeyBytes)
        )
    }

    @Test
    fun getHdSeed() {
        val wallet = givenWalletFromResource("wallet_body_1.txt")
        wallet.decryptHDWallet("hello")
        val seedAccess = wallet.walletBody
        assertEquals(
            "a55d76ccbd8a996fc3ae734db75aacf7cfa6d52f8e9e279" +
                "2bbbdbd54ba14fae6a24f34a90f635cdb70b544dd65828cac857de70d6aacda09fa082ed4632e7ce0",
            Hex.toHexString(seedAccess!!.getHdSeed())
        )
    }

    @Test(expected = DecryptionException::class)
    fun getMasterKey_DecryptionException() {
        val wallet = givenWalletFromResource("wallet_body_1.txt")
        wallet.decryptHDWallet("bogus")
        wallet.walletBody!!.getMasterKey()
    }

    @Test
    fun getMnemonic() {
        val wallet = givenWalletFromResource("wallet_body_1.txt")
        wallet.decryptHDWallet("hello")
        assertEquals(
            "[car, region, outdoor, punch, poverty, shadow, insane, claim, one, whisper, learn, alert]",
            wallet.walletBody!!.getMnemonic().toString()
        )
    }

    @Test
    fun withUpdatedBodiesAndVersion() {
        val wallet = givenWalletFromResource("wallet_body_1.txt")
        val updateWallet = wallet.withUpdatedBodiesAndVersion(
            listOf(
                wallet.walletBody!!.replaceAccount(
                    wallet.walletBody!!.getAccount(0),
                    wallet.walletBody!!.getAccount(1).updateLabel("Whatever label")
                )
            ),
            4
        )
        assert(
            updateWallet.walletBody!!.accounts[0].label == "Whatever label" &&
                updateWallet.walletBody!!.accounts[0].xpubs == XPubs(
                listOf(
                    XPub(
                        "xpub6DEe2bJAU7GbQcGHvqgJ4T6pzZUU8j1WqLPyVtaWJFewfjChAKtUX5u" +
                            "Rza9rabc6rAgFhXptveBmaoy7ptVGgbYT8KKaJ9E7wmyj5o4aqvr",
                        XPub.Format.LEGACY
                    )
                )
            ) &&
                updateWallet.wrapperVersion == 4
        )
    }

    @Test
    fun updateArchivedStateAccount() {
        val wallet = givenWalletFromResource("wallet_body_1.txt")
        assert(!wallet.walletBody!!.accounts.get(0).isArchived)
        val updateWallet = wallet.updateArchivedState(wallet.walletBody!!.accounts.get(0), true)
        assert(updateWallet.walletBody!!.accounts.get(0).isArchived)
    }

    @Test
    fun updateArchivedStateImportedAddress() {
        val wallet = givenWalletFromResource("wallet_body_1.txt")
        assert(!wallet.importedAddressList.first().isArchived)
        val updateWallet = wallet.updateArchivedState(wallet.importedAddressList.first(), true)
        assert(updateWallet.importedAddressList.first().isArchived)
    }

    @Test
    fun updatePbkdf2Iterations() {
        val wallet = givenWalletFromResource("wallet_body_1.txt")
        assert(wallet.options.pbkdf2Iterations == 5000)
        val updateWallet = wallet.updatePbkdf2Iterations(21312)
        assert(updateWallet.options.pbkdf2Iterations == 21312)
    }

    @Test
    fun updateTxNotes() {
        val wallet = givenWalletFromResource("wallet_body_1.txt")
        assert(
            wallet.txNotes == mapOf(
                "94a4934712fd40f2b91b7be256eacad49a50b850c949313b07046664d24c0e4c" to "Bought Pizza"
            )
        )
        val updateWallet =
            wallet.updateTxNotes(
                "94a4934712fd40f2b91b7be256eacad49a50b850c949313b07046664d24c0e4c",
                "Bought Giros"
            )
        assert(
            updateWallet.txNotes["94a4934712fd40f2b91b7be256eacad49a50b850c949313b07046664d24c0e4c"] == "Bought Giros"
        )
    }

    @Test
    fun getUpdateAccount() {
        val wallet = givenWalletFromResource("wallet_body_1.txt")
        val updateAccount = wallet.updateAccount(
            wallet.walletBody!!.getAccount(0),
            wallet.walletBody!!.getAccount(0).updateLabel("label Randon").updateArchivedState(true)
        )
        assert(
            updateAccount.walletBody!!.accounts[0].label == "label Randon" &&
                updateAccount.walletBody!!.accounts[0].isArchived
        )
    }

    @Test(expected = DecryptionException::class)
    fun getMnemonic_DecryptionException() {
        val wallet = givenWalletFromResource("wallet_body_1.txt")
        wallet.decryptHDWallet("bogus")
        wallet.walletBody!!.getMnemonic()
    }

    @Test
    fun hDKeysForSigning() {
        val wallet = givenWalletFromResource("wallet_body_1.txt")

        // Available unspents: [8290, 4616, 5860, 3784, 2290, 13990, 8141]
        val resource = loadResourceContent("wallet_body_1_account1_unspent.txt")
        val unspentOutputs = parseUnspentOutputsAsUtxoList(resource)

        val payment = Payment(bitcoinApi = mock())
        val spendAmount: Long = 40108
        val paymentBundle = payment
            .getSpendableCoins(
                unspentOutputs,
                OutputType.P2PKH,
                OutputType.P2PKH,
                BigInteger.valueOf(spendAmount),
                BigInteger.valueOf(1000L),
                false
            )

        assertEquals(789, paymentBundle.absoluteFee.toLong())
        wallet.decryptHDWallet("hello")
        val keyList = wallet.walletBody?.getHDKeysForSigning(
            wallet.walletBody!!.getAccount(0),
            paymentBundle
        )

        // Contains 5 matching keys for signing
        assertEquals(5, keyList?.size)
    }

    @Test
    fun createNewWallet() {
        val label = "HDAccount 1"
        val payload = Wallet(label)
        assertEquals(36, payload.guid.length.toLong()) // GUIDs are 36 in length
        assertEquals(label, payload.walletBody!!.accounts[0].label)
        assertEquals(1, payload.walletBody!!.accounts.size.toLong())
        assertEquals(5000, payload.options.pbkdf2Iterations!!.toLong())
        assertEquals(600000, payload.options.logoutTime)
        assertEquals(10000L, payload.options.feePerKb)
    }
}

private fun Account.getDefaultXpub() =
    xpubs.default.address
