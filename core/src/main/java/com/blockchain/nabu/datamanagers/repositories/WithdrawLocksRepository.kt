package com.blockchain.nabu.datamanagers.repositories

import com.blockchain.caching.ParameteredSingleTimedCacheRequest
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.custodialwalletimpl.PaymentMethodType
import info.blockchain.balance.FiatCurrency
import io.reactivex.rxjava3.core.Single
import java.math.BigInteger
import timber.log.Timber

class WithdrawLocksRepository(custodialWalletManager: CustodialWalletManager) {

    private val cache = ParameteredSingleTimedCacheRequest<WithdrawalData, BigInteger>(
        cacheLifetimeSeconds = 100L,
        refreshFn = { data ->
            custodialWalletManager.fetchWithdrawLocksTime(
                data.paymentMethodType, data.fiatCurrency
            )
                .doOnSuccess { it1 -> Timber.d("Withdrawal lock: $it1") }
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
