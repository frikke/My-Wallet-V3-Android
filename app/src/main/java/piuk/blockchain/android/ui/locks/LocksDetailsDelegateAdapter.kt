package piuk.blockchain.android.ui.locks

import com.blockchain.core.payments.model.WithdrawalLock
import piuk.blockchain.android.ui.adapters.AdapterDelegatesManager
import piuk.blockchain.android.ui.adapters.DelegationAdapter

class LocksDetailsDelegateAdapter : DelegationAdapter<WithdrawalLock>(AdapterDelegatesManager(), emptyList()) {

    init {
        with(delegatesManager) {
            addAdapterDelegate(LockItemDelegate())
        }
    }
}