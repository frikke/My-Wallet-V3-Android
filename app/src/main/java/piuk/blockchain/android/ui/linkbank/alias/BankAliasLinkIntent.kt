package piuk.blockchain.android.ui.linkbank.alias

import com.blockchain.commonarch.presentation.mvi_v2.Intent

sealed interface BankAliasLinkIntent : Intent<BankAliasLinkModelState> {
    data class AliasUpdated(val alias: String) : BankAliasLinkIntent
    data class LoadBeneficiaryInfo(val currency: String, val address: String) : BankAliasLinkIntent
    data class ActivateBeneficiary(val alias: String) : BankAliasLinkIntent
}
