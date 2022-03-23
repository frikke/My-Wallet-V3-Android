package com.blockchain.network.websocket

import io.reactivex.rxjava3.core.Observable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

inline fun <reified OUTGOING : Any, reified INCOMING : Any> WebSocket<String, String>.toJsonSocket(
    json: Json,
    outgoingAdapter: KSerializer<OUTGOING>,
    incomingAdapter: KSerializer<INCOMING>
): WebSocket<OUTGOING, INCOMING> {
    return JsonWebSocketDecorator(this, json, outgoingAdapter, incomingAdapter)
}

inline fun <reified INCOMING : Any> WebSocketReceive<String>.toJsonReceive(
    json: Json,
    incomingAdapter: KSerializer<INCOMING>
): WebSocketReceive<INCOMING> {
    return JsonWebSocketReceiveDecorator(this, json, incomingAdapter)
}

class JsonWebSocketDecorator<OUTGOING : Any, INCOMING : Any>(
    private val inner: WebSocket<String, String>,
    private val json: Json,
    private val outgoingAdapter: KSerializer<OUTGOING>,
    private val incomingAdapter: KSerializer<INCOMING>
) : WebSocket<OUTGOING, INCOMING>, WebSocketConnection by inner {

    override fun send(message: OUTGOING) {
        inner.send(json.encodeToString(outgoingAdapter, message))
    }

    override val responses: Observable<INCOMING>
        get() = inner.responses.map { json.decodeFromString(incomingAdapter, it) }
}

class JsonWebSocketReceiveDecorator<INCOMING : Any>(
    private val inner: WebSocketReceive<String>,
    private val json: Json,
    private val incomingAdapter: KSerializer<INCOMING>
) : WebSocketReceive<INCOMING> {

    override val responses: Observable<INCOMING>
        get() = inner.responses.map { json.decodeFromString(incomingAdapter, it) }
}
