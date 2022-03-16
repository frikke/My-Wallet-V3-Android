package info.blockchain.wallet.payload.data

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test

class CacheTest {
    @Test fun fromJson() {
        val uri = javaClass.classLoader.getResource("wallet_body_1.txt").toURI()
        val body = String(Files.readAllBytes(Paths.get(uri)), StandardCharsets.UTF_8)
        val wallet = Wallet.fromJson(body, true)
        Assert.assertEquals(
            "xpub6F2ehb9khoF6PZZxKS7vD8T2yDeDWuSR5RNH43b2wK5gY2ayWVApQezEzsFz7EpH2Jf6d6GYJzTrbfReT948CyxVgkhkkvmDBGkcY41MMnv",
            wallet.walletBody!!.getAccount(0).addressCache.changeAccount
        )
        Assert.assertEquals(
            "xpub6F2ehb9khoF6MW8WzyT8WdVhvW3RnZxXYdHDvt43LabqGKdpqt39QFgpRCMAcktZckGZJBUrVVP4uwYbrb98MdR8KujG4tu1B4sRHA9QVwE",
            wallet.walletBody!!.getAccount(0).addressCache.receiveAccount
        )
    }

    @Test fun testToJSON() {

        // Ensure toJson doesn't write any unintended fields
        val uri = javaClass.classLoader.getResource("wallet_body_1.txt").toURI()
        val body = String(Files.readAllBytes(Paths.get(uri)), StandardCharsets.UTF_8)
        val wallet = Wallet.fromJson(body, true)
        val jsonString: String = Json.encodeToString(wallet.walletBody!!.getAccount(0).addressCache)
        val jsonObject = JSONObject(jsonString)
        Assert.assertEquals(2, jsonObject.keySet().size.toLong())
    }
}
