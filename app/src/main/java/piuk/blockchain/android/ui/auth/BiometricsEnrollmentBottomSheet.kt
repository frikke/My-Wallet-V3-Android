package piuk.blockchain.android.ui.auth

import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockchain.commonarch.presentation.base.SlidingModalBottomDialog
import com.blockchain.componentlib.icons.ArrowLeft
import com.blockchain.componentlib.icons.Icons
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.DialogSheetEnrollBiometricsBinding

class BiometricsEnrollmentBottomSheet : SlidingModalBottomDialog<DialogSheetEnrollBiometricsBinding>() {
    interface Host : SlidingModalBottomDialog.Host {
        fun enrollBiometrics()
        fun cancel()
    }

    override val host: Host by lazy {
        super.host as? Host ?: throw IllegalStateException(
            "Host fragment is not a BiometricsEnrollmentBottomSheet.Host"
        )
    }

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): DialogSheetEnrollBiometricsBinding =
        DialogSheetEnrollBiometricsBinding.inflate(inflater, container, false)

    override fun initControls(binding: DialogSheetEnrollBiometricsBinding) {
        binding.biometricEnable.apply {
            text = getString(com.blockchain.stringResources.R.string.common_ok)
            onClick = {
                host.enrollBiometrics()
            }
        }

        binding.biometricCancel.apply {
            text = getString(com.blockchain.stringResources.R.string.not_now)
            onClick =  {
                dismiss()
                host.cancel()
            }
        }
    }

    companion object {
        fun newInstance(): BiometricsEnrollmentBottomSheet = BiometricsEnrollmentBottomSheet()
    }
}
