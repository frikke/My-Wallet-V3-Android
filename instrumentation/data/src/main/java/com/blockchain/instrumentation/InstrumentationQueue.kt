package com.blockchain.instrumentation

import com.blockchain.extensions.minus
import java.util.Optional
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

object InstrumentationQueue {
    data class Item(
        val requestId: UUID,
        val url: String,
        val canPassThrough: Boolean,
        val responses: List<InstrumentedResponse>,
        // NULL means uninitialized, Optional.ABSENT means the request shouldn't be instrumented and should sent to the backend
        val pickedResponse: Optional<InstrumentedResponse>? = null,
    )

    private val _queue = MutableStateFlow<List<Item>>(emptyList())
    val queue: StateFlow<List<Item>> = _queue

    fun add(requestId: UUID, url: String, canPassThrough: Boolean, responses: List<InstrumentedResponse>) {
        _queue.update { current ->
            current + Item(requestId, url, canPassThrough, responses)
        }
    }

    fun remove(requestId: UUID) {
        _queue.update { current ->
            current.minus { it.requestId == requestId }
        }
    }

    /**
     * @param response pass null to send the request to the backend
     */
    fun pickResponse(requestId: UUID, response: InstrumentedResponse?) {
        _queue.update { current ->
            current.map {
                if (it.requestId == requestId) {
                    it.copy(pickedResponse = Optional.ofNullable(response))
                } else {
                    it
                }
            }
        }
    }
}
