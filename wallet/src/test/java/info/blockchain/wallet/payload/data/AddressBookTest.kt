package info.blockchain.wallet.payload.data

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test

class AddressBookTest {
    @Test fun fromJson_1() {
        val uri = javaClass.classLoader.getResource("wallet_body_1.txt").toURI()
        val body = String(Files.readAllBytes(Paths.get(uri)), StandardCharsets.UTF_8)
        val wallet = Wallet.fromJson(body)
        Assert.assertEquals("QA first one", wallet.addressBooks!![0].label)
        Assert.assertEquals("17k7jQsewpru3uxMkaUMxahyvACVc7fjjb", wallet.addressBooks!![0].address)
        Assert.assertEquals("QA second one", wallet.addressBooks!![1].label)
        Assert.assertEquals("1DiJVG3oD3yeqW26qcVaghwTjvMaVoeghX", wallet.addressBooks!![1].address)
    }

    @Test fun testToJSON() {

        // Ensure toJson doesn't write any unintended fields
        val uri = javaClass.classLoader.getResource("wallet_body_1.txt").toURI()
        val body = String(Files.readAllBytes(Paths.get(uri)), StandardCharsets.UTF_8)
        val wallet = Wallet.fromJson(body)
        val jsonString = Json.encodeToString(wallet.addressBooks!![0])
        val jsonObject = JSONObject(jsonString)
        Assert.assertEquals(2, jsonObject.keySet().size.toLong())
    }
}
