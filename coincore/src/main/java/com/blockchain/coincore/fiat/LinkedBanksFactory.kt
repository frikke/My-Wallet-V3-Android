package com.blockchain.coincore.fiat

import com.blockchain.domain.paymentmethods.BankService
import com.blockchain.domain.paymentmethods.PaymentMethodService
import com.blockchain.domain.paymentmethods.model.BankState
import com.blockchain.domain.paymentmethods.model.PaymentMethodType
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import info.blockchain.balance.FiatCurrency
import io.reactivex.rxjava3.core.Single

class LinkedBanksFactory(
    val custodialWalletManager: CustodialWalletManager,
    val bankService: BankService,
    val paymentMethodService: PaymentMethodService
) {

    fun getAllLinkedBanks(): Single<List<LinkedBankAccount>> =
        bankService.getLinkedBanks().map { banks ->
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
                    type = it.type,
                    capabilities = it.capabilities,
                )
            }
        }

    fun getNonWireTransferBanks(): Single<List<LinkedBankAccount>> =
        bankService.getLinkedBanks().map { banks ->
            banks.filter { it.state == BankState.ACTIVE && it.type == PaymentMethodType.BANK_TRANSFER }
                .map { bank ->
                    LinkedBankAccount(
                        label = bank.name,
                        accountNumber = bank.accountEnding,
                        accountId = bank.id,
                        accountType = bank.toHumanReadableAccount(),
                        currency = bank.currency,
                        custodialWalletManager = custodialWalletManager,
                        type = bank.type,
                        capabilities = bank.capabilities,
                    )
                }
        }

    fun eligibleBankPaymentMethods(fiat: FiatCurrency): Single<Set<PaymentMethodType>> =
        paymentMethodService.getEligiblePaymentMethodTypes(fiat).map { methods ->
            methods.filter {
                it.type == PaymentMethodType.BANK_TRANSFER ||
                    it.type == PaymentMethodType.BANK_ACCOUNT
            }.map { it.type }.toSet()
        }
}
