package com.blockchain.nabu

import com.blockchain.nabu.models.responses.nabu.KycState
import com.blockchain.nabu.models.responses.nabu.NabuUser
import com.blockchain.nabu.models.responses.nabu.UserState
import info.blockchain.balance.FiatCurrency

fun getBlankNabuUser(kycState: KycState = KycState.None): NabuUser = NabuUser(
    firstName = "",
    lastName = "",
    email = "",
    emailVerified = false,
    dob = null,
    mobile = "",
    mobileVerified = false,
    address = null,
    state = UserState.None,
    kycState = kycState,
    insertedAt = "",
    updatedAt = ""
)

val USD = FiatCurrency.fromCurrencyCode("USD")
val EUR = FiatCurrency.fromCurrencyCode("EUR")
val GBP = FiatCurrency.fromCurrencyCode("GBP")
