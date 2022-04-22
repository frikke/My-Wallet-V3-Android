package com.blockchain.keyboard

interface InputKeyboard {
    fun inputTypeForAmount(): Int
    fun validInputCharacters(): String
}
