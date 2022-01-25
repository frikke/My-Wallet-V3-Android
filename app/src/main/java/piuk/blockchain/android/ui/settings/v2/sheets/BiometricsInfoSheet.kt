package piuk.blockchain.android.ui.settings.v2.sheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockchain.commonarch.presentation.base.HostedBottomSheet
import com.blockchain.commonarch.presentation.base.SlidingModalBottomDialog
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.BottomSheetBiometricsBinding

class BiometricsInfoSheet : SlidingModalBottomDialog<BottomSheetBiometricsBinding>() {

    interface Host : HostedBottomSheet.Host {
        fun onPositiveActionClicked(sheetMode: BiometricSheetMode)
    }

    override val host: Host by lazy {
        super.host as? Host ?: throw IllegalStateException(
            "Host fragment is not a BiometricsInfoSheet.Host"
        )
    }

    private val sheetMode: BiometricSheetMode by lazy {
        arguments?.getSerializable(SHEET_MODE) as BiometricSheetMode
    }

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): BottomSheetBiometricsBinding =
        BottomSheetBiometricsBinding.inflate(inflater, container, false)

    override fun initControls(binding: BottomSheetBiometricsBinding) {
        with(binding) {
            sheetHeader.apply {
                title = when (sheetMode) {
                    BiometricSheetMode.DISABLE_CONFIRMATION -> getString(R.string.security_biometrics_disable_title)
                    BiometricSheetMode.NO_BIOMETRICS_ADDED -> getString(R.string.security_biometrics_add_title)
                }
                onClosePress = {
                    this@BiometricsInfoSheet.dismiss()
                }
            }

            positiveCta.apply {
                text = when (sheetMode) {
                    BiometricSheetMode.DISABLE_CONFIRMATION -> getString(R.string.security_biometrics_cta_disable)
                    BiometricSheetMode.NO_BIOMETRICS_ADDED -> getString(R.string.security_biometrics_cta_add)
                }
                onClick = {
                    this@BiometricsInfoSheet.dismiss()
                    host.onPositiveActionClicked(sheetMode)
                }
            }

            negativeCta.apply {
                text = getString(R.string.common_cancel)
                onClick = {
                    this@BiometricsInfoSheet.dismiss()
                }
            }

            biometricsBlurb.text = when (sheetMode) {
                BiometricSheetMode.DISABLE_CONFIRMATION -> getString(R.string.biometric_disable_message)
                BiometricSheetMode.NO_BIOMETRICS_ADDED -> getString(R.string.security_biometrics_add_blurb)
            }
        }
    }

    companion object {
        private const val SHEET_MODE = "SHEET_MODE"
        fun newInstance(mode: BiometricSheetMode): BiometricsInfoSheet = BiometricsInfoSheet().apply {
            arguments = Bundle().apply {
                putSerializable(SHEET_MODE, mode)
            }
        }

        enum class BiometricSheetMode {
            DISABLE_CONFIRMATION,
            NO_BIOMETRICS_ADDED
        }
    }
}
