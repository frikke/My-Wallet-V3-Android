package com.blockchain.presentation.balance

import com.blockchain.coincore.AccountBalance
import com.blockchain.componentlib.tablerow.AsyncBalanceUi
import com.blockchain.componentlib.tablerow.CryptoAndFiatBalance
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

fun Flow<AccountBalance>.toAsyncBalanceUi(): AsyncBalanceUi {
    return AsyncBalanceUi(
        fetcher = map {
            CryptoAndFiatBalance(
                crypto = it.total.toStringWithSymbol(),
                fiat = it.totalFiat?.toStringWithSymbol().orEmpty()
            )
        }
    )
}
