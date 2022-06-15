package com.blockchain.coincore

import com.blockchain.nabu.datamanagers.repositories.interest.IneligibilityReason
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.FiatCurrency
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

object NullCryptoAddress : CryptoAddress {
    override val asset: AssetInfo = CryptoCurrency.BTC
    override val label: String = ""
    override val address = ""
    override val isDomain: Boolean = false
}

// Stub invalid accounts; use as an initialisers to avoid nulls.
class NullCryptoAccount(
    override val label: String = ""
) : CryptoAccount {
    override val receiveAddress: Single<ReceiveAddress>
        get() = Single.just(NullAddress)

    override val isDefault: Boolean
        get() = false

    override val currency: AssetInfo
        get() = CryptoCurrency.BTC

    override val sourceState: Single<TxSourceState>
        get() = Single.just(TxSourceState.NOT_SUPPORTED)

    override val balance: Observable<AccountBalance>
        get() = Observable.error(NotImplementedError())

    override val activity: Single<ActivitySummaryList>
        get() = Single.just(emptyList())

    override val stateAwareActions: Single<Set<StateAwareAction>> = Single.just(emptySet())

    override val isFunded: Boolean = false

    override val hasTransactions: Boolean = false

    override fun requireSecondPassword(): Single<Boolean> = Single.just(false)

    override fun matches(other: CryptoAccount): Boolean =
        other is NullCryptoAccount

    override val disabledReason: Single<IneligibilityReason>
        get() = Single.just(IneligibilityReason.NONE)
}

object NullFiatAccount : FiatAccount {
    override val currency: FiatCurrency
        get() = throw IllegalStateException("")

    override val receiveAddress: Single<ReceiveAddress>
        get() = Single.just(NullAddress)

    override val isDefault: Boolean
        get() = false

    override val sourceState: Single<TxSourceState>
        get() = Single.just(TxSourceState.NOT_SUPPORTED)

    override val label: String = ""

    override val balance: Observable<AccountBalance>
        get() = Observable.error(NotImplementedError())

    override val activity: Single<ActivitySummaryList>
        get() = Single.just(emptyList())

    override val stateAwareActions: Single<Set<StateAwareAction>> = Single.just(emptySet())

    override val isFunded: Boolean = false
    override val hasTransactions: Boolean = false

    override val disabledReason: Single<IneligibilityReason>
        get() = Single.just(IneligibilityReason.NONE)

    override fun canWithdrawFunds(): Single<Boolean> = Single.just(false)
}

object NullAccountGroup : AccountGroup {
    override val accounts: SingleAccountList = emptyList()

    override fun includes(account: BlockchainAccount): Boolean = false
    override val label: String = ""

    override val balance: Observable<AccountBalance> = Observable.error(NotImplementedError())
    override val activity: Single<ActivitySummaryList> = Single.just(emptyList())
    override val stateAwareActions: Single<Set<StateAwareAction>> = Single.just(emptySet())
    override val isFunded: Boolean = false
    override val hasTransactions: Boolean = false

    override val receiveAddress: Single<ReceiveAddress> =
        Single.error(NotImplementedError())
}
