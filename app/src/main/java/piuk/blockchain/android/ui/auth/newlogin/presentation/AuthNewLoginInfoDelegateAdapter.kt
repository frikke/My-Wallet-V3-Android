package piuk.blockchain.android.ui.auth.newlogin.presentation

import piuk.blockchain.android.ui.adapters.AdapterDelegatesManager
import piuk.blockchain.android.ui.adapters.DelegationAdapter
import piuk.blockchain.android.ui.auth.newlogin.AuthNewLoginDetailsType

class AuthNewLoginInfoDelegateAdapter :
    DelegationAdapter<AuthNewLoginDetailsType>(AdapterDelegatesManager(), emptyList()) {

    init {
        with(delegatesManager) {
            addAdapterDelegate(NewLoginAuthInfoItemDelegate())
        }
    }
}
