package piuk.blockchain.android.ui.pairingcode

import com.blockchain.commonarch.presentation.mvi.MviState

sealed class QrCodeImageStatus {
    object NotInitialised : QrCodeImageStatus()
    object Loading : QrCodeImageStatus()
    object Error : QrCodeImageStatus()
    data class Ready(val qrUri: String) : QrCodeImageStatus()
    data class Hidden(val qrUri: String) : QrCodeImageStatus()
}

data class PairingState(
    val imageStatus: QrCodeImageStatus = QrCodeImageStatus.NotInitialised
) : MviState {
    val showQrCode: Boolean
        get() = imageStatus !is QrCodeImageStatus.NotInitialised && imageStatus !is QrCodeImageStatus.Hidden
}
