package com.blockchain.api.nftwaitlist.data.model

import kotlin.test.assertEquals
import org.junit.Test

class NftWaitlistDtoTest {

    @Test
    fun testBuild() {
        val result = NftWaitlistDto.build("email")

        assertEquals("email", result.email)
        assertEquals("mobile_view_nft_support", result.feature)
        assertEquals(UserDataDto, result.userData)
    }
}
