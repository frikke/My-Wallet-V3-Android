package piuk.blockchain.android.ui.linkbank.yapily.permission

import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import java.io.File

sealed interface YapilyPermissionNavigationEvent : NavigationEvent {
    data class OpenFile(val file: File) : YapilyPermissionNavigationEvent
}
