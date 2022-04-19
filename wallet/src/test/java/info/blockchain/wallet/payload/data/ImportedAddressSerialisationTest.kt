package info.blockchain.wallet.payload.data

import com.blockchain.serialization.fromJson
import com.blockchain.serialization.toJson
import com.blockchain.testutils.getStringFromResource
import org.amshove.kluent.`should be equal to`
import org.junit.Test

class ImportedAddressSerialisationTest {

    @Test
    fun `kotlin serializer test`() {
        val json = getStringFromResource("serialisation/ImportedAddress.json")

        val object1 = ImportedAddress::class.fromJson(json)
        val jsonA = object1.toJson(ImportedAddress.serializer())
        val object2 = ImportedAddress::class.fromJson(jsonA)
        val jsonB = object2.toJson(ImportedAddress.serializer())

        jsonA `should be equal to` jsonB
    }
}
