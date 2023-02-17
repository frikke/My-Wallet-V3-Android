package com.blockchain.coincore

import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.FiatCurrency
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.flow.flowOf

object NullCryptoAddress : CryptoAddress {
    override val asset: AssetInfo
        get() = error("unavailable for NullCryptoAddress")

    override val label: String
        get() = error("unavailable for NullCryptoAddress")

    override val address
        get() = error("unavailable for NullCryptoAddress")

    override val isDomain: Boolean
        get() = error("unavailable for NullCryptoAddress")
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

    override fun balanceRx(freshnessStrategy: FreshnessStrategy): Observable<AccountBalance> {
        return Observable.error(NotImplementedError())
    }

    override fun activity(freshnessStrategy: FreshnessStrategy): Observable<ActivitySummaryList> =
        Observable.just(emptyList())

    override val stateAwareActions: Single<Set<StateAwareAction>> = Single.just(emptySet())

    override fun stateOfAction(assetAction: AssetAction): Single<ActionState> {
        return Single.just(ActionState.Unavailable)
    }

    override fun requireSecondPassword(): Single<Boolean> = Single.just(false)

    override fun matches(other: CryptoAccount): Boolean =
        other is NullCryptoAccount
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

    override fun balanceRx(freshnessStrategy: FreshnessStrategy): Observable<AccountBalance> {
        return Observable.error(NotImplementedError())
    }

    override fun activity(freshnessStrategy: FreshnessStrategy): Observable<ActivitySummaryList> {
        return Observable.just(emptyList())
    }

    override val stateAwareActions: Single<Set<StateAwareAction>> = Single.just(emptySet())

    override fun stateOfAction(assetAction: AssetAction): Single<ActionState> {
        return Single.just(ActionState.Unavailable)
    }

    override fun canWithdrawFunds() = flowOf(DataResource.Data(false))
}
