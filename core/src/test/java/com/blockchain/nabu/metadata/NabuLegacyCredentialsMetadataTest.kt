package com.blockchain.nabu.metadata

import org.amshove.kluent.`should be equal to`
import org.junit.Test

class NabuLegacyCredentialsMetadataTest {

    @Test
    fun `should be valid`() {
        NabuLegacyCredentialsMetadata("userId", "lifeTimeToken").isValid() `should be equal to` true
    }

    @Test
    fun `empty id, should not be valid`() {
        NabuLegacyCredentialsMetadata("", "lifeTimeToken").isValid() `should be equal to` false
    }

    @Test
    fun `empty token, should not be valid`() {
        NabuLegacyCredentialsMetadata("userId", "").isValid() `should be equal to` false
    }
}
