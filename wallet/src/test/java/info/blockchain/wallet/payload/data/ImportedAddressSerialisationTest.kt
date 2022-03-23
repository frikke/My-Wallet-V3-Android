package info.blockchain.wallet.payload.data

import com.blockchain.serialization.fromJson
import com.blockchain.serialization.toJson
import com.blockchain.testutils.getStringFromResource
import com.fasterxml.jackson.databind.ObjectMapper
import org.amshove.kluent.`should be equal to`
import org.junit.Test

class ImportedAddressSerialisationTest {

    @Test
    fun `jackson to kotlinx`() {
        val objectMapper = ObjectMapper()
        val json = getStringFromResource("serialisation/ImportedAddress.json")

        val object1 = objectMapper.readValue(json, ImportedAddress::class.java)
        val jsonA = object1.toJson(ImportedAddress.serializer())
        val object2 = objectMapper.readValue(jsonA, ImportedAddress::class.java)
        val jsonB = object2.toJson(ImportedAddress.serializer())

        jsonA `should be equal to` jsonB
    }

    @Test
    fun `kotlinx to jackson`() {
        val objectMapper = ObjectMapper()
        val json = getStringFromResource("serialisation/ImportedAddress.json")

        val object1 = ImportedAddress::class.fromJson(json)
        val jsonA = objectMapper.writeValueAsString(object1)
        val object2 = ImportedAddress::class.fromJson(jsonA)
        val jsonB = objectMapper.writeValueAsString(object2)

        jsonA `should be equal to` jsonB
    }
}
