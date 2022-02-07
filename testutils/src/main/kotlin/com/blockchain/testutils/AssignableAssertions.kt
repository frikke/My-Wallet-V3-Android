package com.blockchain.testutils

import kotlin.reflect.KClass
import org.amshove.kluent.`should be`

@Suppress("FunctionName")
infix fun KClass<*>.`should be assignable from`(kClass: KClass<*>) {
    this.java.isAssignableFrom(kClass.java) `should be` true
}
