package com.blockchain.domain.paymentmethods

import com.blockchain.domain.paymentmethods.model.BankProviderAccountAttributes
import com.blockchain.domain.paymentmethods.model.BankTransferDetails
import com.blockchain.domain.paymentmethods.model.FundsLocks
import com.blockchain.domain.paymentmethods.model.LinkBankTransfer
import com.blockchain.domain.paymentmethods.model.LinkedBank
import com.blockchain.domain.paymentmethods.model.LinkedPaymentMethod
import info.blockchain.balance.Currency
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

interface BankService {

    fun getWithdrawalLocks(localCurrency: Currency): Single<FundsLocks>

    fun getLinkedBank(id: String): Single<LinkedBank>

    fun getLinkedBanks(): Single<List<LinkedPaymentMethod.Bank>>

    fun removeBank(bank: LinkedPaymentMethod.Bank): Completable

    fun linkBank(currency: FiatCurrency): Single<LinkBankTransfer>

    fun updateSelectedBankAccount(
        linkingId: String,
        providerAccountId: String,
        accountId: String,
        attributes: BankProviderAccountAttributes
    ): Completable

    fun linkPlaidBankAccount(
        linkingId: String,
        accountId: String,
        publicToken: String
    ): Completable

    fun startBankTransfer(
        id: String,
        amount: Money,
        currency: String,
        callback: String? = null
    ): Single<String>

    fun updateOpenBankingConsent(url: String, token: String): Completable

    fun getBankTransferCharge(paymentId: String): Single<BankTransferDetails>

    fun canTransactWithBankMethods(fiatCurrency: FiatCurrency): Single<Boolean>
}
