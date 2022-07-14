package info.blockchain.wallet.payload.data

import org.amshove.kluent.`should be`
import org.junit.Test

class ImportedAddressExtensionsTest {

    @Test
    fun `is not archived initially`() {
        ImportedAddress(
            address = "",
            _tag = 0
        ).isArchived `should be` false
    }

    @Test
    fun `tag is not set initially`() {
        ImportedAddress(
            address = "",
            _tag = 0
        ).tag `should be` 0
    }

    @Test
    fun `archive marks address as archived`() {
        val arcived = ImportedAddress(
            address = "",
            _tag = 0
        ).updateArchivedState(true)
        arcived.isArchived `should be` true
    }

    @Test
    fun `_tag is set by archive`() {
        val arcived = ImportedAddress(
            address = "",
            _tag = 0
        ).updateArchivedState(true)
        arcived.tag `should be` 2
    }

    @Test
    fun `tag set marks address as archived`() {
        ImportedAddress(address = "", _tag = 2).apply {
            isArchived `should be` true
        }
    }

    @Test
    fun `tag set to normal, clears archived`() {
        ImportedAddress(address = "", _tag = 0).apply {
            isArchived `should be` false
        }
    }

    @Test
    fun `unarchive, clears archived`() {
        val address = ImportedAddress(
            address = "",
            _tag = 0
        ).updateArchivedState(true).updateArchivedState(false)
        address.isArchived `should be` false
    }
}
