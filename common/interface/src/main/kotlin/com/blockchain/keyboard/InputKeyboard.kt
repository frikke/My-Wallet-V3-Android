package com.blockchain.keyboard

interface InputKeyboard {
    fun specialInputForAmounts(): Int?
    fun validInputCharacters(): String
}
