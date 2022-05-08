package com.blockchain.nabu.models

import com.blockchain.nabu.models.responses.nabu.UserState
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class UserStateAdapterTest {

    private val jsonBuilder = Json {
        ignoreUnknownKeys = true
        explicitNulls = true
    }

    @Test
    fun `userState serializer`() {
        @Serializable
        data class TestClass(
            val state: UserState
        )

        UserState::class.sealedSubclasses.map { it.objectInstance as UserState }.forEach { state ->
            println("Checking serialization for: ${state.javaClass.name}")
            val jsonString = jsonBuilder.encodeToString(TestClass(state))
            val testObject = jsonBuilder.decodeFromString<TestClass>(jsonString)

            testObject shouldBeEqualTo TestClass(state)
        }
    }
}
