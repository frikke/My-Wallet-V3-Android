package piuk.blockchain.android.ui.customviews

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockchain.commonarch.presentation.base.SlidingModalBottomDialog
import com.blockchain.fiatActions.fiatactions.KycBenefitsSheetHost
import java.io.Serializable
import kotlinx.parcelize.Parcelize
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.KycBenefitsBottomSheetBinding

class KycBenefitsBottomSheet : SlidingModalBottomDialog<KycBenefitsBottomSheetBinding>() {

    private val benefitsDetails: BenefitsDetails by lazy {
        arguments?.getParcelable(BENEFITS_DETAILS) ?: BenefitsDetails()
    }

    override val host: KycBenefitsSheetHost by lazy {
        super.host as? KycBenefitsSheetHost ?: throw IllegalStateException(
            "Host fragment is not a KycBenefitsBottomSheet.Host"
        )
    }

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): KycBenefitsBottomSheetBinding =
        KycBenefitsBottomSheetBinding.inflate(inflater, container, false)

    override fun initControls(binding: KycBenefitsBottomSheetBinding) {
        with(binding) {
            benefitsView.initWithBenefits(
                benefits = benefitsDetails.listOfBenefits,
                title = benefitsDetails.title,
                description = benefitsDetails.description,
                icon = benefitsDetails.icon,
                primaryButton = ButtonOptions(true) {
                    dismiss()
                    host.verificationCtaClicked()
                },
                secondaryButton = ButtonOptions(true) {
                    dismiss()
                }
            )
        }
    }

    companion object {
        private const val BENEFITS_DETAILS = "BENEFITS_DETAILS"

        fun newInstance(
            details: BenefitsDetails
        ): KycBenefitsBottomSheet = KycBenefitsBottomSheet().apply {
            arguments = Bundle().apply {
                putParcelable(BENEFITS_DETAILS, details)
            }
        }
    }

    @Parcelize
    data class BenefitsDetails(
        val title: String = "",
        val description: String = "",
        val listOfBenefits: List<VerifyIdentityNumericBenefitItem> = emptyList(),
        val icon: Serializable = com.blockchain.common.R.drawable.ic_verification_badge
    ) : Parcelable
}
