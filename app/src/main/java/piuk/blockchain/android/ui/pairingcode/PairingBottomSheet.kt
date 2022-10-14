package piuk.blockchain.android.ui.pairingcode

import android.graphics.Bitmap
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat.getColor
import com.blockchain.commonarch.presentation.mvi.MviBottomSheet
import com.blockchain.componentlib.alert.BlockchainSnackbar
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.visibleIf
import com.blockchain.presentation.koin.scopedInject
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.PairingSheetBinding
import piuk.blockchain.android.scan.QRCodeEncoder
import piuk.blockchain.android.urllinks.WEB_WALLET_LOGIN_URI

class PairingBottomSheet : MviBottomSheet<PairingModel, PairingIntents, PairingState, PairingSheetBinding>() {

    override val model: PairingModel by scopedInject()
    private val encoder: QRCodeEncoder by inject()

    private lateinit var currentState: PairingState

    override fun render(newState: PairingState) {

        currentState = newState

        with(binding) {
            when (newState.imageStatus) {
                QrCodeImageStatus.NotInitialised,
                QrCodeImageStatus.Loading,
                is QrCodeImageStatus.Hidden -> {
                    updateUI(newState)
                }
                is QrCodeImageStatus.Error -> {
                    progressBar.gone()
                    BlockchainSnackbar.make(
                        binding.root,
                        getString(R.string.unexpected_error),
                        type = SnackbarType.Error
                    ).show()
                }
                is QrCodeImageStatus.Ready -> {
                    if (qrCode.drawable == null) {
                        qrCode.setImageBitmap(getScaledQrCodeBitmap(newState.imageStatus))
                    }
                    updateUI(newState)
                }
            }
        }
    }

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): PairingSheetBinding =
        PairingSheetBinding.inflate(inflater, container, false)

    override fun initControls(binding: PairingSheetBinding) {
        with(binding) {

            pairingWalletHeaderLabel.text = prepareHighlightedText(R.string.pairing_wallet_description)

            showQrButton.setOnClickListener {
                model.process(
                    if (!currentState.showQrCode) {
                        PairingIntents.ShowQrImage
                    } else {
                        PairingIntents.HideQrImage
                    }
                )
            }
            stepOne.text = prepareHighlightedText(R.string.pairing_wallet_instruction_1)
            blockchainIcon.setImageResource(R.drawable.ic_blockchain_round_white_bg)
        }
    }

    private fun prepareHighlightedText(@StringRes textResId: Int) =
        SpannableString(getString(textResId, WEB_WALLET_LOGIN_URI)).apply {
            val span = indexOf(WEB_WALLET_LOGIN_URI)
            setSpan(
                ForegroundColorSpan(
                    getColor(requireContext(), R.color.blue_600)
                ),
                span,
                span + WEB_WALLET_LOGIN_URI.length,
                Spannable.SPAN_EXCLUSIVE_INCLUSIVE
            )
        }

    private fun getScaledQrCodeBitmap(imageStatus: QrCodeImageStatus.Ready): Bitmap? {
        val qrCodeBitmap = encoder.encodeAsBitmap(imageStatus.qrUri, QR_IMAGE_DIMENSION)
        return qrCodeBitmap?.let {
            val width = resources.displayMetrics.widthPixels / 2 // Scale it to half of display width
            val height = width * it.height / it.width
            Bitmap.createScaledBitmap(qrCodeBitmap, width, height, true)
        }
    }

    private fun PairingSheetBinding.updateUI(newState: PairingState) {
        stepOne.visibleIf { !newState.showQrCode }
        stepTwo.visibleIf { !newState.showQrCode }
        stepThree.visibleIf { !newState.showQrCode }
        showQrButton.text = if (newState.showQrCode) {
            resources.getString(R.string.pairing_wallet_hide_qr)
        } else {
            resources.getString(R.string.pairing_wallet_show_qr)
        }
        progressBar.visibleIf { newState.imageStatus == QrCodeImageStatus.Loading }
        qrCode.visibleIf { newState.showQrCode }
        blockchainIcon.visibleIf { newState.showQrCode }
    }

    companion object {
        private const val QR_IMAGE_DIMENSION = 600
    }
}
