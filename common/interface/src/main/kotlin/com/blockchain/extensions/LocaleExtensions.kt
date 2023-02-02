package com.blockchain.extensions

import java.util.Locale

val Locale.range: String
    get() = Locale.LanguageRange(this.toLanguageTag()).range
