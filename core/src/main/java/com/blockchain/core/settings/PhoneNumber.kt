package com.blockchain.core.settings

class PhoneNumber(val raw: String) {
    val sanitized = "+${raw.replace("[^\\d.]".toRegex(), "")}"
    val isValid = sanitized.length >= 9
}
