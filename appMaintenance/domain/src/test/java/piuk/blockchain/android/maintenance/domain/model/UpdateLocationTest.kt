package piuk.blockchain.android.maintenance.domain.model

import kotlin.test.assertEquals
import org.junit.Test

class UpdateLocationTest {

    @Test
    fun `WHEN value is empty, THEN InAppUpdate should be returned`() {
        val result = UpdateLocation.fromUrl("")

        assertEquals(UpdateLocation.InAppUpdate, result)
    }

    @Test
    fun `WHEN value is null, THEN InAppUpdate should be returned`() {
        val result = UpdateLocation.fromUrl(null)

        assertEquals(UpdateLocation.InAppUpdate, result)
    }

    @Test
    fun `WHEN value is not null or empty, THEN ExternalUrl should be returned`() {
        val result = UpdateLocation.fromUrl("url")

        assertEquals(UpdateLocation.ExternalUrl("url"), result)
    }
}
