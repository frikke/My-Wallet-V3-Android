package com.blockchain.coincore.fiat

import com.blockchain.core.payments.PaymentsDataManager
import com.blockchain.core.payments.model.BankState
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.custodialwalletimpl.PaymentMethodType
import info.blockchain.balance.FiatCurrency
import io.reactivex.rxjava3.core.Single

class LinkedBanksFactory(
    val custodialWalletManager: CustodialWalletManager,
    val paymentsDataManager: PaymentsDataManager
) {

    fun getAllLinkedBanks(): Single<List<LinkedBankAccount>> =
        paymentsDataManager.getLinkedBanks().map { banks ->
            banks.filter {
                it.state == BankState.ACTIVE
            }.map {
                LinkedBankAccount(
                    label = it.name,
                    accountNumber = it.accountEnding,
                    accountId = it.id,
                    accountType = it.toHumanReadableAccount(),
                    currency = it.currency,
                    custodialWalletManager = custodialWalletManager,
                    type = it.type
                )
            }
        }

    fun getNonWireTransferBanks(): Single<List<LinkedBankAccount>> =
        paymentsDataManager.getLinkedBanks().map { banks ->
            banks.filter { it.state == BankState.ACTIVE && it.type == PaymentMethodType.BANK_TRANSFER }
                .map { bank ->
                    LinkedBankAccount(
                        label = bank.name,
                        accountNumber = bank.accountEnding,
                        accountId = bank.id,
                        accountType = bank.toHumanReadableAccount(),
                        currency = bank.currency,
                        custodialWalletManager = custodialWalletManager,
                        type = bank.type
                    )
                }
        }

    fun eligibleBankPaymentMethods(fiat: FiatCurrency): Single<Set<PaymentMethodType>> =
        paymentsDataManager.getEligiblePaymentMethodTypes(fiat).map { methods ->
            methods.filter {
                it.type == PaymentMethodType.BANK_TRANSFER ||
                    it.type == PaymentMethodType.BANK_ACCOUNT
            }.map { it.type }.toSet()
        }
}
