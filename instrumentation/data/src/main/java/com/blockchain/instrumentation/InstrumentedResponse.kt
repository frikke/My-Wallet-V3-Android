package com.blockchain.instrumentation

sealed class InstrumentedResponse {
    abstract val key: String

    data class Json(override val key: String, val code: Int, val json: String) : InstrumentedResponse()
    data class Model<M>(override val key: String, val model: M) : InstrumentedResponse()
}
