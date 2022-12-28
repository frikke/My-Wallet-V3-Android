package piuk.blockchain.android.ui.linkbank

import com.blockchain.commonarch.presentation.mvi.MviState
import com.blockchain.domain.paymentmethods.model.BankAuthError
import com.blockchain.domain.paymentmethods.model.BankLinkingProcessState
import com.blockchain.domain.paymentmethods.model.LinkBankTransfer
import com.blockchain.domain.paymentmethods.model.LinkedBank
import com.blockchain.domain.paymentmethods.model.RefreshBankInfo
import piuk.blockchain.android.simplebuy.SelectedPaymentMethod

data class BankAuthState(
    val id: String? = null,
    val linkedBank: LinkedBank? = null,
    val linkBankTransfer: LinkBankTransfer? = null,
    val linkBankUrl: String? = null,
    val linkBankAccountId: String? = null,
    val linkBankToken: String? = null,
    val bankLinkingProcessState: BankLinkingProcessState = BankLinkingProcessState.NONE,
    val errorState: BankAuthError? = null,
    val selectedPaymentMethod: SelectedPaymentMethod? = null,
    val callbackPathUrl: String = "",
    val refreshBankAccountId: String? = null,
    val refreshBankInfo: RefreshBankInfo? = null
) : MviState
