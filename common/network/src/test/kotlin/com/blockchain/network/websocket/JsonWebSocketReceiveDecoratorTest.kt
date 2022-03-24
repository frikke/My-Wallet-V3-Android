package com.blockchain.network.websocket

import com.nhaarman.mockitokotlin2.mock
import io.reactivex.rxjava3.core.Observable
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.amshove.kluent.`should be equal to`
import org.junit.Test

class JsonWebSocketReceiveDecoratorTest {

    @Serializable
    data class TypeIn(
        val fieldC: String,
        val fieldD: Int
    )

    private val json = Json {}

    @Test
    fun `incoming message is formatted from json`() {
        val inner = mock<WebSocketReceive<String>> {
            on { responses }.thenReturn(
                Observable.just(
                    "{\"fieldC\":\"Message1\",\"fieldD\":1234}",
                    "{\"fieldC\":\"Message2\",\"fieldD\":5678}"
                )
            )
        }
        inner.toJsonReceive(json, TypeIn.serializer())
            .responses
            .test()
            .values() `should be equal to`
            listOf(
                TypeIn(fieldC = "Message1", fieldD = 1234),
                TypeIn(fieldC = "Message2", fieldD = 5678)
            )
    }
}
