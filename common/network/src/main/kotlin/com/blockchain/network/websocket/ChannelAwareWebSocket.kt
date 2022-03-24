package com.blockchain.network.websocket

import com.blockchain.serialization.JsonSerializable
import com.blockchain.serializers.PrimitiveSerializer
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface ChannelAwareWebSocket {
    fun openChannel(name: String, params: JsonSerializable? = null): WebSocketChannel<String>
}

interface WebSocketChannel<INCOMING> : WebSocketReceive<INCOMING> {
    fun close(params: JsonSerializable? = null)
}

private val json = Json {
    explicitNulls = false
    encodeDefaults = false
    ignoreUnknownKeys = true
}

fun StringWebSocket.channelAware(): ChannelAwareWebSocket = WebSocketChannelAdapter(this)

private class WebSocketChannelAdapter(private val underlingSocket: StringWebSocket) : ChannelAwareWebSocket {

    override fun openChannel(name: String, params: JsonSerializable?): WebSocketChannel<String> {
        underlingSocket.send(
            json.encodeToString(
                SubscribeUnsubscribeJson(
                    action = "subscribe", channel = name, params = params
                )
            )
        )
        return underlingSocket.asChannel(name)
    }
}

class ErrorFromServer(val fullJson: String) : Exception("Server returned error")

private fun StringWebSocket.asChannel(
    name: String
): WebSocketChannel<String> {

    return object : WebSocketChannel<String> {

        val channelMessageFilter = this@asChannel.channelMessageFilter(name, throwErrors = true)

        private val closed = PublishSubject.create<Any>()

        override fun close(params: JsonSerializable?) {
            this@asChannel.send(
                json.encodeToString(
                    SubscribeUnsubscribeJson(
                        action = "unsubscribe", channel = name, params = params
                    )
                )
            )
            closed.onNext(Any())
        }

        override val responses: Observable<String>
            get() = channelMessageFilter.responses.takeUntil(closed)
    }
}

/**
 * Filters messages to those that match the channel name and are not subscribe/unsubscribe messages.
 */
fun WebSocketReceive<String>.channelMessageFilter(name: String, throwErrors: Boolean = true): WebSocketReceive<String> {

    return object : WebSocketReceive<String> {

        override val responses: Observable<String>
            get() = this@channelMessageFilter.responses.filter { jsonString ->
                json.decodeFromString(IncomingMessage.serializer(), jsonString)
                    .let {
                        it.channel == name &&
                            it.event != "subscribed" &&
                            it.event != "unsubscribed" &&
                            !handleError(it, jsonString)
                    }
            }

        private fun handleError(message: IncomingMessage, json: String): Boolean {
            return when {
                message.event != "error" -> false
                throwErrors -> throw ErrorFromServer(json)
                else -> true
            }
        }
    }
}

@Serializable
private class IncomingMessage(
    @SerialName("channel")
    val channel: String,

    @SerialName("event")
    val event: String?
) : JsonSerializable

@Serializable
private class SubscribeUnsubscribeJson(
    @Suppress("unused")
    @SerialName("action")
    val action: String,

    @Suppress("unused")
    @SerialName("channel")
    val channel: String,

    @SerialName("params")
    @Serializable(with = PrimitiveSerializer::class)
    val params: Any?
) : JsonSerializable
