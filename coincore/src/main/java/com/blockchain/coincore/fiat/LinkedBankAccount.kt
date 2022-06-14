package com.blockchain.coincore.fiat

import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.ActivitySummaryList
import com.blockchain.coincore.BankAccount
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.ReceiveAddress
import com.blockchain.coincore.StateAwareAction
import com.blockchain.coincore.TxSourceState
import com.blockchain.core.price.ExchangeRate
import com.blockchain.domain.paymentmethods.model.FiatWithdrawalFeeAndLimit
import com.blockchain.domain.paymentmethods.model.PaymentMethodType
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.Product
import com.blockchain.nabu.datamanagers.repositories.interest.IneligibilityReason
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

class LinkedBankAccount(
    override val label: String,
    val accountNumber: String,
    val accountId: String,
    val accountType: String,
    override val currency: FiatCurrency,
    val custodialWalletManager: CustodialWalletManager,
    val type: PaymentMethodType
) : FiatAccount, BankAccount {

    init {
        check(type == PaymentMethodType.BANK_ACCOUNT || type == PaymentMethodType.BANK_TRANSFER) {
            "Attempting to initialise a LinkedBankAccount with an incorrect PaymentMethodType of $type"
        }
    }

    fun getWithdrawalFeeAndMinLimit(): Single<FiatWithdrawalFeeAndLimit> =
        custodialWalletManager.fetchFiatWithdrawFeeAndMinLimit(currency, Product.BUY, paymentMethodType = type)

    override val balance: Observable<AccountBalance>
        get() = Money.zero(currency).let { zero ->
            Observable.just(
                AccountBalance(
                    total = zero,
                    pending = zero,
                    withdrawable = zero,
                    exchangeRate = ExchangeRate.zeroRateExchangeRate(currency)
                )
            )
        }

    override val receiveAddress: Single<ReceiveAddress>
        get() = Single.just(BankAccountAddress(accountId, label))

    override val isDefault: Boolean
        get() = false

    override val sourceState: Single<TxSourceState>
        get() = Single.just(TxSourceState.CAN_TRANSACT)

    override val activity: Single<ActivitySummaryList>
        get() = Single.just(emptyList())

    override val stateAwareActions: Single<Set<StateAwareAction>>
        get() = Single.just(emptySet())

    override val isFunded: Boolean
        get() = false

    override val hasTransactions: Boolean
        get() = false

    override val isEnabled: Single<Boolean>
        get() = Single.just(true)

    override val disabledReason: Single<IneligibilityReason>
        get() = Single.just(IneligibilityReason.NONE)

    override fun canWithdrawFunds(): Single<Boolean> = Single.just(false)
    fun isOpenBankingCurrency(): Boolean = listOf("GBP", "EUR").contains(currency.networkTicker)

    internal class BankAccountAddress(
        override val address: String,
        override val label: String = address
    ) : ReceiveAddress
}
