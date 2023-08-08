package com.blockchain.nabu.models.responses.nabu

import com.blockchain.serializers.IsoDateSerializer
import java.util.Date
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class AirdropStatusSerializationTest {

    private val jsonBuilder = Json {
        ignoreUnknownKeys = true
        explicitNulls = true
        serializersModule = SerializersModule { contextual(IsoDateSerializer) }
    }

    @Test
    fun `UserCampaignState serializer`() {
        @Serializable
        data class TestClass(
            val state: UserCampaignState
        )

        UserCampaignState::class.sealedSubclasses.map { it.objectInstance as UserCampaignState }.forEach { state ->
            println("Checking serialization for: ${state.javaClass.name}")
            val jsonString = jsonBuilder.encodeToString(TestClass(state))
            val testObject = jsonBuilder.decodeFromString<TestClass>(jsonString)

            testObject shouldBeEqualTo TestClass(state)
        }
    }

    @Test
    fun `CampaignState serializer`() {
        @Serializable
        data class TestClass(
            val state: CampaignState
        )

        CampaignState::class.sealedSubclasses.map { it.objectInstance as CampaignState }.forEach { state ->
            println("Checking serialization for: ${state.javaClass.name}")
            val jsonString = jsonBuilder.encodeToString(TestClass(state))
            val testObject = jsonBuilder.decodeFromString<TestClass>(jsonString)

            testObject shouldBeEqualTo TestClass(state)
        }
    }

    @Test
    fun `CampaignTransactionState serializer`() {
        @Serializable
        data class TestClass(
            val state: CampaignTransactionState
        )

        CampaignTransactionState::class.sealedSubclasses.map {
            it.objectInstance as CampaignTransactionState
        }.forEach { state ->
            println("Checking serialization for: ${state.javaClass.name}")
            val jsonString = jsonBuilder.encodeToString(TestClass(state))
            val testObject = jsonBuilder.decodeFromString<TestClass>(jsonString)

            testObject shouldBeEqualTo TestClass(state)
        }
    }

    @Test
    fun `AirdropStatus - happy`() {
        val airdropStatus = AirdropStatus(
            campaignName = "campaignName",
            campaignEndDate = Date(),
            campaignState = CampaignState.Started,
            userState = UserCampaignState.Registered,
            attributes = CampaignAttributes(
                campaignAddress = "address",
                campaignCode = "code",
                campaignEmail = "email",
                rejectReason = "reason"
            ),
            updatedAt = Date(),
            txResponseList = listOf(
                CampaignTransaction(
                    fiatValue = 100L,
                    fiatCurrency = "currency",
                    withdrawalQuantity = 120L,
                    withdrawalCurrency = "w_currency",
                    withdrawalAt = Date(),
                    transactionState = CampaignTransactionState.PendingDeposit
                )
            )
        )

        val jsonString = jsonBuilder.encodeToString(AirdropStatusList(listOf(airdropStatus)))
        val testObject = jsonBuilder.decodeFromString<AirdropStatusList>(jsonString)

        testObject.toString() shouldBeEqualTo AirdropStatusList(listOf(airdropStatus)).toString()
    }

    @Test
    fun `AirdropStatus - missing fields`() {
        val airdropStatus = AirdropStatus(
            campaignName = "campaignName",
            campaignState = CampaignState.Started,
            userState = UserCampaignState.Registered,
            updatedAt = Date(),
            txResponseList = listOf(
                CampaignTransaction(
                    fiatValue = 100L,
                    fiatCurrency = "currency",
                    withdrawalQuantity = 120L,
                    withdrawalCurrency = "w_currency",
                    withdrawalAt = Date(),
                    transactionState = CampaignTransactionState.PendingDeposit
                )
            )
        )

        val jsonString = jsonBuilder.encodeToString(AirdropStatusList(listOf(airdropStatus)))
        val testObject = jsonBuilder.decodeFromString<AirdropStatusList>(jsonString)

        testObject.toString() shouldBeEqualTo AirdropStatusList(listOf(airdropStatus)).toString()
    }
}
