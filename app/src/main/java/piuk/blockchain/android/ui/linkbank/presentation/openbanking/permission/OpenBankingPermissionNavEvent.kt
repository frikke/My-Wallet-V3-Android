package piuk.blockchain.android.ui.linkbank.presentation.openbanking.permission

import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.core.payments.model.YapilyInstitution

sealed interface OpenBankingPermissionNavEvent : NavigationEvent {
    data class AgreementAccepted(val institution: YapilyInstitution) : OpenBankingPermissionNavEvent
    object AgreementDenied : OpenBankingPermissionNavEvent
}
