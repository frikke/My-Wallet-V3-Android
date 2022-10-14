package piuk.blockchain.android.ui.settings.sheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockchain.commonarch.presentation.base.HostedBottomSheet
import com.blockchain.commonarch.presentation.base.SlidingModalBottomDialog
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.viewextensions.gone
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.BottomSheetTwoFactorInfoBinding

class TwoFactorInfoSheet : SlidingModalBottomDialog<BottomSheetTwoFactorInfoBinding>() {

    interface Host : HostedBottomSheet.Host {
        fun onEnableSMSTwoFa()
        fun onActionOnWebTwoFa()
    }

    override val host: Host by lazy {
        super.host as? Host ?: throw IllegalStateException(
            "Host fragment is not a TwoFactorInfoSheet.Host"
        )
    }

    private val sheetMode: TwoFaSheetMode by lazy {
        arguments?.getSerializable(SHEET_MODE) as TwoFaSheetMode
    }

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): BottomSheetTwoFactorInfoBinding =
        BottomSheetTwoFactorInfoBinding.inflate(inflater, container, false)

    override fun initControls(binding: BottomSheetTwoFactorInfoBinding) {
        with(binding) {
            sheetHeader.apply {
                title = getString(R.string.security_two_fa_title)
                onClosePress = {
                    this@TwoFactorInfoSheet.dismiss()
                }
            }

            when (sheetMode) {
                TwoFaSheetMode.ENABLE -> {
                    ctaEnable.apply {
                        text = getString(R.string.security_two_fa_sheet_cta_enable)
                        onClick = {
                            this@TwoFactorInfoSheet.dismiss()
                            host.onEnableSMSTwoFa()
                        }
                    }
                    twoFaBlurb.text = getString(R.string.security_two_fa_sheet_blurb)
                }
                TwoFaSheetMode.DISABLE_ON_WEB -> {
                    ctaEnable.gone()
                    twoFaBlurb.text = getString(R.string.security_two_fa_disable_on_web_blurb)
                }
            }

            ctaOther.apply {
                icon = ImageResource.Local(R.drawable.ic_open_in_external, "")
                text = when (sheetMode) {
                    TwoFaSheetMode.ENABLE -> getString(R.string.security_two_fa_sheet_cta_other)
                    TwoFaSheetMode.DISABLE_ON_WEB -> getString(R.string.security_two_fa_disable_on_web_cta)
                }
                onClick = {
                    this@TwoFactorInfoSheet.dismiss()
                    host.onActionOnWebTwoFa()
                }
            }
        }
    }

    companion object {
        private const val SHEET_MODE = "SHEET_MODE"
        fun newInstance(mode: TwoFaSheetMode): TwoFactorInfoSheet = TwoFactorInfoSheet().apply {
            arguments = Bundle().apply {
                putSerializable(SHEET_MODE, mode)
            }
        }

        enum class TwoFaSheetMode {
            ENABLE,
            DISABLE_ON_WEB
        }
    }
}
