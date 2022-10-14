package com.blockchain.ui.password

import io.reactivex.rxjava3.core.Maybe

interface SecondPasswordHandler {

    val hasSecondPasswordSet: Boolean
    val verifiedPassword: String?

    interface ResultListener {
        fun onNoSecondPassword()
        fun onSecondPasswordValidated(validatedSecondPassword: String)
        fun onCancelled() {}
    }

    fun secondPassword(): Maybe<String>
}
