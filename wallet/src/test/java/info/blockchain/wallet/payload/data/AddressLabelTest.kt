package info.blockchain.wallet.payload.data

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test

class AddressLabelTest {
    @Test fun fromJson_1() {
        val uri = javaClass.classLoader.getResource("wallet_body_1.txt").toURI()
        val body = String(Files.readAllBytes(Paths.get(uri)), StandardCharsets.UTF_8)
        val wallet = Wallet.fromJson(body, 4)
        val walletBody = wallet.walletBody
        val accounts: List<Account> = walletBody!!.accounts
        val addressLabels: List<AddressLabel> = accounts!![1].addressLabels
        Assert.assertEquals(98, addressLabels[0].index.toLong())
        Assert.assertEquals("New Address", addressLabels[0].label)
        Assert.assertEquals(23, accounts[2].addressLabels[0].index.toLong())
        Assert.assertEquals("Contact request", accounts[2].addressLabels[0].label)
        Assert.assertEquals(24, accounts[2].addressLabels[1].index.toLong())
        Assert.assertEquals("Buy Sell", accounts[2].addressLabels[1].label)
    }

    @Test fun testToJSON() {
        // Ensure toJson doesn't write any unintended fields
        val uri = javaClass.classLoader.getResource("wallet_body_1.txt").toURI()
        val body = String(Files.readAllBytes(Paths.get(uri)), StandardCharsets.UTF_8)
        val wallet = Wallet.fromJson(body, 4)
        val walletBody = wallet.walletBodies!![0]
        val accounts: List<Account>? = walletBody.accounts
        val addressLabels: List<AddressLabel> = accounts!![1].addressLabels
        val jsonString = Json.encodeToString(addressLabels[0])
        val jsonObject = JSONObject(jsonString)
        Assert.assertEquals(2, jsonObject.keySet().size.toLong())
    }
}
