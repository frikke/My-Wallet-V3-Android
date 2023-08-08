package info.blockchain.wallet.payload.data

import info.blockchain.wallet.MockedResponseTest
import info.blockchain.wallet.payload.data.WalletBase.Companion.fromJson
import java.lang.Exception
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test

class WalletBaseTest : MockedResponseTest() {

    @Test
    fun fromJson_v3_1f() {
        val uri = javaClass.classLoader.getResource("wallet_v3_1.txt").toURI()
        val walletBase = String(Files.readAllBytes(Paths.get(uri)), StandardCharsets.UTF_8)
        val walletBaseBody = fromJson(walletBase).withDecryptedPayload("MyTestWallet")
        Assert.assertEquals(
            "d78bf97da866cdda7271a8de0f2d101caf43ae6280b3c69b85bf82d367649ea7",
            walletBaseBody.payloadChecksum
        )
        Assert.assertNotNull(walletBaseBody.wallet.options.pbkdf2Iterations)
        Assert.assertEquals(5000, walletBaseBody.wallet.options.pbkdf2Iterations)
        Assert.assertNotNull(walletBaseBody.wallet)
        Assert.assertEquals("a09910d9-1906-4ea1-a956-2508c3fe0661", walletBaseBody.wallet.guid)
    }

    @Test
    fun fromJson_v3_2() {
        val uri = javaClass.classLoader.getResource("wallet_v3_2.txt").toURI()
        val walletBase = String(Files.readAllBytes(Paths.get(uri)), StandardCharsets.UTF_8)
        val walletBaseBody = fromJson(walletBase).withDecryptedPayload("SomeTestPassword")
        Assert.assertEquals(
            "7416cd440f7b15182beb15614a63d5e53b3a6f65634d2b160884c131ab336b01",
            walletBaseBody.payloadChecksum
        )
        Assert.assertNotNull(walletBaseBody.wallet.options.pbkdf2Iterations)
        Assert.assertEquals(5000, walletBaseBody.wallet.options.pbkdf2Iterations)
        Assert.assertNotNull(walletBaseBody.wallet)
        Assert.assertEquals("e5eba801-c8bc-4a64-99ba-094e12a80766", walletBaseBody.wallet.guid)
    }

    @Test
    fun fromJson_v3_3() {
        val uri = javaClass.classLoader.getResource("wallet_v3_3.txt").toURI()
        val walletBase = String(Files.readAllBytes(Paths.get(uri)), StandardCharsets.UTF_8)
        val walletBaseBody = fromJson(walletBase).withDecryptedPayload("SomeTestPassword")
        Assert.assertEquals(
            "fc631f8434f45c43e7040f1192b6676a8bd49e0fd00fb4848acdc0dcaa665400",
            walletBaseBody.payloadChecksum
        )
        Assert.assertNotNull(walletBaseBody.wallet.options.pbkdf2Iterations)
        Assert.assertEquals(7520, walletBaseBody.wallet.options.pbkdf2Iterations)
        Assert.assertNotNull(walletBaseBody.wallet)
        Assert.assertEquals("e5eba801-c8bc-4a64-99ba-094e12a80766", walletBaseBody.wallet.guid)
    }

    @Test
    fun fromJson_v2_3() {
        val uri = javaClass.classLoader.getResource("wallet_v2_1.txt").toURI()
        val walletBase = String(Files.readAllBytes(Paths.get(uri)), StandardCharsets.UTF_8)
        val walletBaseBody = fromJson(walletBase).withDecryptedPayload("SomeTestPassword")
        Assert.assertEquals(
            "110764d05c020d4818e2529ca28df9d8b96d50c694650348f885fc075f9366d5",
            walletBaseBody.payloadChecksum
        )
        Assert.assertNotNull(walletBaseBody.wallet.options.pbkdf2Iterations)
        Assert.assertEquals(5000, walletBaseBody.wallet.options.pbkdf2Iterations)
        Assert.assertNotNull(walletBaseBody.wallet)
        Assert.assertEquals("5f071985-01b5-4bd4-9d5f-c7cf570b1a2d", walletBaseBody.wallet.guid)
    }

    @Test
    fun fromJson_v2_2() {
        val uri = javaClass.classLoader.getResource("wallet_v2_2.txt").toURI()
        val walletBase = String(Files.readAllBytes(Paths.get(uri)), StandardCharsets.UTF_8)
        val walletBaseBody = fromJson(walletBase).withDecryptedPayload("SomeTestPassword")
        Assert.assertEquals(
            "31b162d3e1fd0b57d8b7dd1202c16604be221bde2fe0192fc0a4e7ce704d3446",
            walletBaseBody.payloadChecksum
        )
        Assert.assertNotNull(walletBaseBody.wallet.options.pbkdf2Iterations)
        Assert.assertEquals(1000, walletBaseBody.wallet.options.pbkdf2Iterations)
        Assert.assertNotNull(walletBaseBody.wallet)
        Assert.assertEquals("5f071985-01b5-4bd4-9d5f-c7cf570b1a2d", walletBaseBody.wallet.guid)
    }

    @Test
    @Throws(Exception::class)
    fun fromJson_v1_1() {
        val uri = javaClass.classLoader.getResource("wallet_v1_1.txt").toURI()
        val walletBase = String(Files.readAllBytes(Paths.get(uri)), StandardCharsets.UTF_8)
        val walletBaseBody = fromJson(walletBase).withDecryptedPayload("mypassword")
        Assert.assertEquals(
            "26c0477b045655bb7ba3e81fb99d7e8ce16f4571400223026169ba8e207677a4",
            walletBaseBody.payloadChecksum
        )
        Assert.assertNotNull(walletBaseBody.wallet)
        Assert.assertEquals("9ebb4d4f-f36e-40d6-9a3e-5a3cca5f83d6", walletBaseBody.wallet.guid)
    }

    @Test
    fun fromJson_v1_2() {
        val uri = javaClass.classLoader.getResource("wallet_v1_2.txt").toURI()
        val walletBase = String(Files.readAllBytes(Paths.get(uri)), StandardCharsets.UTF_8)
        val walletBaseBody = fromJson(walletBase).withDecryptedPayload("mypassword")
        Assert.assertEquals(
            "57f97ace89c105c19c43a15f2d6e3091d457dec804243b15772d2062a32f8b7d",
            walletBaseBody.payloadChecksum
        )
        Assert.assertNotNull(walletBaseBody.wallet)
        Assert.assertEquals("2ca9b0e4-6b82-4dae-9fef-e8b300c72aa2", walletBaseBody.wallet.guid)
    }

    @Test
    fun fromJson_v1_3() {
        val uri = javaClass.classLoader.getResource("wallet_v1_3.txt").toURI()
        val walletBase = String(Files.readAllBytes(Paths.get(uri)), StandardCharsets.UTF_8)
        val walletBaseBody = fromJson(walletBase).withDecryptedPayload("mypassword")
        Assert.assertEquals(
            "a4b67f406268dced75ac5c628da854898c9a3134b7e3755311f199723d426765",
            walletBaseBody.payloadChecksum
        )
        Assert.assertNotNull(walletBaseBody.wallet)
        Assert.assertEquals("4077b6d9-73b3-4d22-96d4-9f8810fec435", walletBaseBody.wallet.guid)
    }

    @Test
    fun encryptAndWrapPayload() {
        val uri = javaClass.classLoader.getResource("wallet_v3_1.txt").toURI()
        val walletBase = String(Files.readAllBytes(Paths.get(uri)), StandardCharsets.UTF_8)

        // ///////////
        // Decrypt
        var walletBaseBody = fromJson(walletBase).withDecryptedPayload("MyTestWallet")

        // Encrypt
        var pair = walletBaseBody.encryptAndWrapPayload("MyTestWallet")

        // Check wallet wrapper
        var encryptedwalletWrapper = pair.second
        Assert.assertEquals(5000, encryptedwalletWrapper.pbkdf2Iterations)
        Assert.assertEquals(3, encryptedwalletWrapper.version.toLong())

        // Decrypt again to check payload intact
        var walletBody = encryptedwalletWrapper.decryptPayload("MyTestWallet")
        Assert.assertEquals("a09910d9-1906-4ea1-a956-2508c3fe0661", walletBody.guid)

        // /////Encrypt with different iterations//////
        // Decrypt
        walletBaseBody = fromJson(walletBase).withDecryptedPayload("MyTestWallet")
        walletBaseBody = walletBaseBody.withWalletBody(walletBaseBody.wallet.updatePbkdf2Iterations(7500))

        // Encrypt
        pair = walletBaseBody.encryptAndWrapPayload("MyTestWallet")

        // Check wallet wrapper
        encryptedwalletWrapper = pair.second
        Assert.assertEquals(7500, encryptedwalletWrapper.pbkdf2Iterations.toLong())
        Assert.assertEquals(3, encryptedwalletWrapper.version.toLong())

        // Decrypt again to check payload intact
        walletBody = encryptedwalletWrapper.decryptPayload("MyTestWallet")
        Assert.assertEquals("a09910d9-1906-4ea1-a956-2508c3fe0661", walletBody.guid)

        // /////Encrypt with different password//////
        // Decrypt
        walletBaseBody = fromJson(walletBase).withDecryptedPayload("MyTestWallet")
        walletBaseBody = walletBaseBody.withWalletBody(walletBaseBody.wallet.updatePbkdf2Iterations(7500))

        // Encrypt
        pair = walletBaseBody.encryptAndWrapPayload("MyNewTestWallet")

        // Check wallet wrapper
        encryptedwalletWrapper = pair.second
        Assert.assertEquals(7500, encryptedwalletWrapper.pbkdf2Iterations.toLong())
        Assert.assertEquals(3, encryptedwalletWrapper.version.toLong())

        // Decrypt again to check payload intact
        walletBody = encryptedwalletWrapper.decryptPayload("MyNewTestWallet")
        Assert.assertEquals("a09910d9-1906-4ea1-a956-2508c3fe0661", walletBody.guid)
    }

    @Test
    fun testToJSON() {
        // Ensure toJson doesn't write any unintended fields
        val uri = javaClass.classLoader.getResource("wallet_v3_1.txt").toURI()
        val walletBase = String(Files.readAllBytes(Paths.get(uri)), StandardCharsets.UTF_8)
        val walletBaseBody = fromJson(walletBase)
        val jsonString = walletBaseBody.toJson()
        val jsonObject = JSONObject(jsonString)
        Assert.assertEquals(7, jsonObject.keySet().size.toLong())
    }
}
