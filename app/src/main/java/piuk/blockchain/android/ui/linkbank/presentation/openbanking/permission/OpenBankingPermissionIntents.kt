package piuk.blockchain.android.ui.linkbank.presentation.openbanking.permission

import com.blockchain.commonarch.presentation.mvi_v2.Intent

sealed interface OpenBankingPermissionIntents : Intent<OpenBankingPermissionModelState> {
    object GetTermsOfServiceLink : OpenBankingPermissionIntents
    object ApproveClicked : OpenBankingPermissionIntents
    object DenyClicked : OpenBankingPermissionIntents
}
