package com.blockchain.nabu.util.fakefactory.nabu

import com.blockchain.nabu.models.responses.nabu.CurrenciesResponse
import com.blockchain.nabu.models.responses.nabu.KycState
import com.blockchain.nabu.models.responses.nabu.NabuUser
import com.blockchain.nabu.models.responses.nabu.UserState

object FakeNabuUserFactory {
    val satoshi = NabuUser(
        id = "id",
        firstName = "Satoshi",
        lastName = "Nakamoto",
        email = "satoshi@btc.com",
        emailVerified = false,
        dob = null,
        mobile = "",
        mobileVerified = false,
        address = FakeAddressFactory.any,
        state = UserState.None,
        kycState = KycState.None,
        insertedAt = "",
        updatedAt = "",
        currencies = CurrenciesResponse(
            preferredFiatTradingCurrency = "EUR",
            usableFiatCurrencies = listOf("EUR", "USD", "GBP", "ARS"),
            defaultWalletCurrency = "BRL",
            userFiatCurrencies = listOf("EUR", "GBP")
        )
    )
}
