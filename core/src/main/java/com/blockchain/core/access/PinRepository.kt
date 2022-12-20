package com.blockchain.core.access

import com.blockchain.core.auth.isValidPin

interface PinRepository {
    val pin: String

    fun clearPin()
    fun setPin(pin: String)
}

internal class PinRepositoryImpl : PinRepository {
    private var thePin: String = ""

    override val pin: String
        get() = thePin

    override fun clearPin() {
        thePin = ""
    }

    override fun setPin(pin: String) {
        if (!pin.isValidPin()) {
            IllegalArgumentException("setting invalid pin!").let {
                throw it
            }
        }
        thePin = pin
    }
}
