package com.blockchain.coincore.selfcustody

import com.blockchain.coincore.FeeLevel
import com.blockchain.coincore.PendingTx
import com.blockchain.coincore.TxEngine
import com.blockchain.coincore.TxResult
import com.blockchain.storedatasource.FlushableDataSource
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Single

// TODO(dtverdota): AND-6165 Send
class DynamicOnChanTxEngine : TxEngine() {
    override val flushableDataSources: List<FlushableDataSource>
        get() = listOf()

    override fun doBuildConfirmations(pendingTx: PendingTx): Single<PendingTx> {
        TODO("Not yet implemented")
    }

    override fun doInitialiseTx(): Single<PendingTx> {
        TODO("Not yet implemented")
    }

    override fun doUpdateAmount(amount: Money, pendingTx: PendingTx): Single<PendingTx> {
        TODO("Not yet implemented")
    }

    override fun doUpdateFeeLevel(pendingTx: PendingTx, level: FeeLevel, customFeeAmount: Long): Single<PendingTx> {
        TODO("Not yet implemented")
    }

    override fun doValidateAmount(pendingTx: PendingTx): Single<PendingTx> {
        TODO("Not yet implemented")
    }

    override fun doValidateAll(pendingTx: PendingTx): Single<PendingTx> {
        TODO("Not yet implemented")
    }

    override fun doExecute(pendingTx: PendingTx, secondPassword: String): Single<TxResult> {
        TODO("Not yet implemented")
    }
}
