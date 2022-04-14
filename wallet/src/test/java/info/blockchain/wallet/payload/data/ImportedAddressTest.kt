package info.blockchain.wallet.payload.data

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test

class ImportedAddressTest {
    @Test fun fromJson_1() {
        val uri = javaClass.classLoader.getResource("wallet_body_1.txt").toURI()
        val body = String(Files.readAllBytes(Paths.get(uri)), StandardCharsets.UTF_8)
        val wallet = Wallet.fromJson(body)
        Assert.assertEquals(19, wallet.importedAddressList.size.toLong())
        var addressBody = wallet.importedAddressList[0]
        Assert.assertEquals("import 1", addressBody.label)
        Assert.assertEquals("19Axrcn8nsdZkSJtbnyM1rCs1PGwSzzzNn", addressBody.address)
        Assert.assertEquals(
            "g9rIjgOlfASQuJv38i1xdLmP1m2gMTPe96YzJJ9hjI2BBz5RErNOSeHPdeU2ZIOnsk+M1dfFw649MHXb7RAcZg==",
            addressBody.privateKey
        )
        Assert.assertEquals(1433495572L, addressBody.createdTime)
        Assert.assertEquals(0, addressBody.tag)
        Assert.assertEquals("android", addressBody.createdDeviceName)
        Assert.assertNull(addressBody.createdDeviceVersion)
        addressBody = wallet.importedAddressList[1]
        Assert.assertEquals("1rW486AbUx2LapYca7kddpULJVqMGMhTH", addressBody.label)
        Assert.assertEquals("1rW486AbUx2LapYca7kddpULJVqMGMhTH", addressBody.address)
        Assert.assertEquals(
            "TIMsVAyWiVcTmcwc6Xv5r494sZcMBRQue6DamkXzDAzOn0cGQSf2XDN+1ZKM/cHgsL0oeJqxrBs9c5TzsNHIZg==",
            addressBody.privateKey
        )
        Assert.assertEquals(1434379366L, addressBody.createdTime)
        Assert.assertEquals(2, addressBody.tag)
        Assert.assertEquals("web", addressBody.createdDeviceName)
        Assert.assertEquals("6.1.16", addressBody.createdDeviceVersion)
    }

    @Test fun testToJSON() {

        // Ensure toJson doesn't write any unintended fields
        val uri = javaClass.classLoader.getResource("wallet_body_1.txt").toURI()
        val body = String(Files.readAllBytes(Paths.get(uri)), StandardCharsets.UTF_8)
        val wallet = Wallet.fromJson(body)
        val addressBody = wallet.importedAddressList[0]
        val jsonString = Json { encodeDefaults = true }.encodeToString(addressBody)
        val jsonObject = JSONObject(jsonString)
        Assert.assertEquals(7, jsonObject.keySet().size.toLong())
    }
}
