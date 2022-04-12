package info.blockchain.wallet.payload.data

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test

class OptionsTest {
    @Test fun fromJson_1() {
        val uri = javaClass.classLoader.getResource("wallet_body_1.txt").toURI()
        val body = String(Files.readAllBytes(Paths.get(uri)), StandardCharsets.UTF_8)
        val wallet = Wallet.fromJson(body)
        val options = wallet.getOptions()
        Assert.assertEquals(10000, options.feePerKb)
        Assert.assertEquals(5000, options.pbkdf2Iterations.toLong())
        Assert.assertFalse(options.isHtml5Notifications)
        Assert.assertEquals(3600000, options.logoutTime)
    }

    @Test fun fromJson_2() {
        val uri = javaClass.classLoader.getResource("wallet_body_2.txt").toURI()
        val body = String(Files.readAllBytes(Paths.get(uri)), StandardCharsets.UTF_8)
        val wallet = Wallet.fromJson(body)
        val options = wallet.getOptions()
        Assert.assertEquals(0, options.feePerKb)

        // Expect iterations to default. 0 not allowed
        Assert.assertEquals(5000, options.pbkdf2Iterations.toLong())
        Assert.assertFalse(options.isHtml5Notifications)
        Assert.assertEquals(600000, options.logoutTime)
    }

    @Test fun testToJSON() {

        // Ensure toJson doesn't write any unintended fields
        val uri = javaClass.classLoader.getResource("wallet_body_1.txt").toURI()
        val body = String(Files.readAllBytes(Paths.get(uri)), StandardCharsets.UTF_8)
        val wallet = Wallet.fromJson(body)
        val options = wallet.getOptions()
        val jsonString: String = Json { encodeDefaults = true }.encodeToString(options)
        val jsonObject = JSONObject(jsonString)
        Assert.assertEquals(4, jsonObject.keySet().size.toLong())
    }
}
