package piuk.blockchain.android.ui.login.auth

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.DialogSheetAccountUnificationBinding
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.android.ui.customviews.ButtonOptions
import piuk.blockchain.android.ui.customviews.VerifyIdentityNumericBenefitItem

class AccountUnificationBottomSheet : SlidingModalBottomDialog<DialogSheetAccountUnificationBinding>() {
    interface Host : SlidingModalBottomDialog.Host {
        fun upgradeAccountClicked()
        fun doLaterClicked()
    }

    override val host: Host by lazy {
        super.host as? Host ?: throw IllegalStateException(
            "Host fragment is not a AccountUnificationBottomSheet.Host"
        )
    }

    private val benefitsList: List<VerifyIdentityNumericBenefitItem> by lazy {
        arguments?.getParcelableArrayList<VerifyIdentityNumericBenefitItem>(
            BENEFITS
        ) as? List<VerifyIdentityNumericBenefitItem> ?: emptyList()
    }

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): DialogSheetAccountUnificationBinding =
        DialogSheetAccountUnificationBinding.inflate(inflater, container, false)

    override fun initControls(binding: DialogSheetAccountUnificationBinding) {
        with(binding.unificationBenefits) {
            initWithBenefits(
                benefits = benefitsList,
                title = getString(R.string.unification_sheet_header),
                description = getString(R.string.unification_sheet_blurb),
                primaryButton = ButtonOptions(
                    true,
                    getString(R.string.unification_sheet_now_cta)
                ) {
                    host.upgradeAccountClicked()
                    dismiss()
                },
                secondaryButton = ButtonOptions(
                    true,
                    getString(R.string.unification_sheet_later_cta)
                ) {
                    host.doLaterClicked()
                    dismiss()
                },
                showSheetIndicator = false,
                titleGravity = Gravity.CENTER,
                descriptionGravity = Gravity.CENTER
            )
        }
    }

    companion object {
        fun newInstance(benefitsList: ArrayList<VerifyIdentityNumericBenefitItem>): AccountUnificationBottomSheet =
            AccountUnificationBottomSheet().apply {
                arguments = Bundle().apply {
                    putParcelableArrayList(BENEFITS, benefitsList)
                }
            }

        private const val BENEFITS = "BENEFITS"
    }
}