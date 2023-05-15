package com.blockchain.nabu.datamanagers.repositories

import com.blockchain.core.common.caching.ParameteredSingleTimedCacheRequest
import com.blockchain.domain.paymentmethods.model.PaymentMethodType
import com.blockchain.logging.Logger
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import info.blockchain.balance.FiatCurrency
import io.reactivex.rxjava3.core.Single
import java.math.BigInteger

class WithdrawLocksRepository(custodialWalletManager: CustodialWalletManager) {

    private val cache = ParameteredSingleTimedCacheRequest<WithdrawalData, BigInteger>(
        cacheLifetimeSeconds = 100L,
        refreshFn = { data ->
            custodialWalletManager.fetchWithdrawLocksTime(
                data.paymentMethodType,
                data.fiatCurrency
            )
                .doOnSuccess { it1 -> Logger.d("Withdrawal lock: $it1") }
        }
    )

    fun getWithdrawLockTypeForPaymentMethod(
        paymentMethodType: PaymentMethodType,
        fiatCurrency: FiatCurrency
    ): Single<BigInteger> =
        cache.getCachedSingle(
            WithdrawalData(paymentMethodType, fiatCurrency)
        )
            .onErrorReturn { BigInteger.ZERO }

    private data class WithdrawalData(
        val paymentMethodType: PaymentMethodType,
        val fiatCurrency: FiatCurrency
    )
}
