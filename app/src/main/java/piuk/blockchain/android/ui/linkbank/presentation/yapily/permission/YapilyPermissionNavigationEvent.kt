package piuk.blockchain.android.ui.linkbank.presentation.yapily.permission

import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.core.payments.model.YapilyInstitution

sealed interface YapilyPermissionNavigationEvent : NavigationEvent {
    data class AgreementAccepted(val institution: YapilyInstitution) : YapilyPermissionNavigationEvent
    object AgreementDenied : YapilyPermissionNavigationEvent
}
