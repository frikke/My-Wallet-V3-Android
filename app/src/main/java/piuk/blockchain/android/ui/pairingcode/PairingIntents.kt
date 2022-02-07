package piuk.blockchain.android.ui.pairingcode

import com.blockchain.commonarch.presentation.mvi.MviIntent

sealed class PairingIntents : MviIntent<PairingState> {

    object LoadQrImage : PairingIntents() {
        override fun reduce(oldState: PairingState): PairingState =
            oldState.copy(
                imageStatus = QrCodeImageStatus.Loading
            )
    }

    object ShowQrError : PairingIntents() {
        override fun reduce(oldState: PairingState): PairingState =
            oldState.copy(
                imageStatus = QrCodeImageStatus.Error
            )
    }

    data class CompleteQrImageLoading(private val qrUri: String) : PairingIntents() {
        override fun reduce(oldState: PairingState): PairingState =
            oldState.copy(
                imageStatus = QrCodeImageStatus.Ready(qrUri)
            )
    }

    object ShowQrImage : PairingIntents() {
        override fun reduce(oldState: PairingState): PairingState =
            oldState.copy(
                imageStatus = when (oldState.imageStatus) {
                    QrCodeImageStatus.Error,
                    QrCodeImageStatus.Loading,
                    QrCodeImageStatus.NotInitialised -> QrCodeImageStatus.Loading
                    is QrCodeImageStatus.Hidden -> QrCodeImageStatus.Ready(oldState.imageStatus.qrUri)
                    is QrCodeImageStatus.Ready -> QrCodeImageStatus.Ready(oldState.imageStatus.qrUri)
                }
            )
    }

    object HideQrImage : PairingIntents() {
        override fun reduce(oldState: PairingState): PairingState =
            oldState.copy(
                imageStatus = when (oldState.imageStatus) {
                    QrCodeImageStatus.Error,
                    QrCodeImageStatus.Loading,
                    QrCodeImageStatus.NotInitialised -> QrCodeImageStatus.NotInitialised
                    is QrCodeImageStatus.Hidden -> QrCodeImageStatus.Hidden(oldState.imageStatus.qrUri)
                    is QrCodeImageStatus.Ready -> QrCodeImageStatus.Hidden(oldState.imageStatus.qrUri)
                }
            )
    }
}
