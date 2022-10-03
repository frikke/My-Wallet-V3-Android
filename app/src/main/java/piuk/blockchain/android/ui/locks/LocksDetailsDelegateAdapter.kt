package piuk.blockchain.android.ui.locks

import com.blockchain.domain.paymentmethods.model.FundsLock
import piuk.blockchain.android.ui.adapters.AdapterDelegatesManager
import piuk.blockchain.android.ui.adapters.DelegationAdapter

class LocksDetailsDelegateAdapter : DelegationAdapter<FundsLock>(AdapterDelegatesManager(), emptyList()) {

    init {
        with(delegatesManager) {
            addAdapterDelegate(LockItemDelegate())
        }
    }
}
