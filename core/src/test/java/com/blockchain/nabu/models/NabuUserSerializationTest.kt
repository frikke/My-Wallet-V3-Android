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

    @Test
    fun `nabuUser - decode from string and encode the result to string should be successful`() {
        val originalJsonString =
            """{
  "id": "id",
  "firstName": "firstName",
  "lastName": "lastName",
  "email": "email@email.email",
  "emailVerified": true,
  "dob": "2000-10-10",
  "mobile": "+12121212",
  "mobileVerified": true,
  "address": {
    "city": "city",
    "line1": "line1",
    "line2": "line2",
    "state": "state",
    "country": "country",
    "postCode": "123"
  },
  "state": "ACTIVE",
  "kycState": "VERIFIED",
  "tiers": {
    "current": 2,
    "selected": 2,
    "next": 2
  },
  "limits": [],
  "tags": {
    "NO_BROKERAGE_FEES": {
      "rdcGlobalStatus": "PENDING"
    },
    "INTERNAL_TESTING": {}
  },
  "settings": {
    "MERCURY_SIGNUP_COUNTRY": "GB",
    "MERCURY_EMAIL_VERIFIED": false
  },
  "productsUsed": {
    "exchange": true
  },
  "walletAddresses": {
    "BCH": "BCHaddress",
  },
  "walletGuid": "walletGuid",
  "currencies": {
    "preferredFiatTradingCurrency": "GBP",
    "usableFiatCurrencies": [
      "GBP",
      "EUR",
      "USD"
    ],
    "defaultWalletCurrency": "GBP",
    "userFiatCurrencies": [
      "GBP"
    ]
  },
  "mercuryEmailVerified": false
}"""

        val userObject = jsonBuilder.decodeFromString<NabuUser>(originalJsonString)
        jsonBuilder.encodeToString(userObject)
    }
}
