package com.blockchain.coincore.fiat

import info.blockchain.balance.Currency

fun Currency.isOpenBankingCurrency() = listOf("GBP", "EUR").contains(networkTicker)
