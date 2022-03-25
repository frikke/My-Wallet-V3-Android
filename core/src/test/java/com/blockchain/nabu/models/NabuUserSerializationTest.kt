package com.blockchain.nabu.models

import com.blockchain.nabu.models.responses.nabu.Address
import com.blockchain.nabu.models.responses.nabu.KycState
import com.blockchain.nabu.models.responses.nabu.NabuSettings
import com.blockchain.nabu.models.responses.nabu.NabuUser
import com.blockchain.nabu.models.responses.nabu.ProductsUsed
import com.blockchain.nabu.models.responses.nabu.ResubmissionResponse
import com.blockchain.nabu.models.responses.nabu.TierLevels
import com.blockchain.nabu.models.responses.nabu.UserState
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class NabuUserSerializationTest {
    private val jsonBuilder = Json {
        ignoreUnknownKeys = true
        explicitNulls = true
    }

    @Test
    fun `nabuUser - happy`() {
        val user = NabuUser(
            firstName = "firstName",
            lastName = "lastName",
            email = "email",
            emailVerified = true,
            dob = "dob",
            mobile = "mobile",
            mobileVerified = true,
            address = Address(
                line1 = "line1",
                line2 = "line2",
                city = "city",
                state = "state",
                postCode = "postCode",
                countryCode = "countryCode"
            ),
            state = UserState.Active,
            kycState = KycState.Verified,
            productsUsed = ProductsUsed(true),
            settings = NabuSettings(true),
            resubmission = ResubmissionResponse(15, "details"),
            insertedAt = "insertedAt",
            updatedAt = "updatedAt",
            tags = mapOf("tag1" to mapOf("sub_tag_1" to 12)),
            userName = "userName",
            tiers = TierLevels(1, 2, 3),
            walletGuid = "walletId"
        )

        val jsonString = jsonBuilder.encodeToString(user)
        val testObject = jsonBuilder.decodeFromString<NabuUser>(jsonString)

        testObject.toString() shouldBeEqualTo user.toString()
    }

    @Test
    fun `nabuUser - missing fields`() {
        val user = NabuUser(
            firstName = null,
            lastName = null,
            email = "email",
            emailVerified = true,
            dob = null,
            mobile = null,
            mobileVerified = true,
            address = null,
            state = UserState.Active,
            kycState = KycState.Verified,
            productsUsed = ProductsUsed(),
            settings = null,
            resubmission = null,
            insertedAt = null,
            updatedAt = null,
            tags = null,
            userName = null,
            tiers = null,
            walletGuid = "walletId"
        )

        val testObject = jsonBuilder.decodeFromString<NabuUser>(
            "{\n" +
                "   \"email\":\"email\",\n" +
                "   \"emailVerified\":true,\n" +
                "   \"mobileVerified\":true,\n" +
                "   \"state\":\"ACTIVE\",\n" +
                "   \"kycState\":\"VERIFIED\",\n" +
                "   \"productsUsed\":{},\n" +
                "   \"walletGuid\":\"walletId\"\n" +
                "}"
        )

        testObject shouldBeEqualTo user
    }
}
