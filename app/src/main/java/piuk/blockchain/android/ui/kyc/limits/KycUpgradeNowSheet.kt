package piuk.blockchain.android.ui.kyc.limits

import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import piuk.blockchain.android.R
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.databinding.DialogSheetKycUpgradeNowBinding
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.android.ui.kyc.navhost.KycNavHostActivity
import piuk.blockchain.android.util.visibleIf

class KycUpgradeNowSheet : SlidingModalBottomDialog<DialogSheetKycUpgradeNowBinding>() {

    private val isGoldPending: Boolean by lazy {
        arguments?.getBoolean(ARG_IS_GOLD_PENDING) ?: false
    }

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): DialogSheetKycUpgradeNowBinding =
        DialogSheetKycUpgradeNowBinding.inflate(inflater, container, false)

    override fun initControls(binding: DialogSheetKycUpgradeNowBinding) {
        with(binding) {
            val isClickable = !isGoldPending

            imageGoldChevron.visibleIf { isClickable }
            imageSilverChevron.visibleIf { isClickable }
            clickableGold.visibleIf { isClickable }
            clickableSilver.visibleIf { isClickable }

            if (isClickable) {
                clickableGold.setOnClickListener { launchKyc() }
            } else {
                clickableGold.setOnClickListener(null)
            }
            if (isClickable) {
                clickableSilver.setOnClickListener { launchKyc() }
            } else {
                clickableSilver.setOnClickListener(null)
            }

            val goldInfo1 = getString(R.string.kyc_upgrade_now_gold_info_1)
            val goldInfo2 = getString(R.string.kyc_upgrade_now_gold_info_2)
            textGoldInfo.text = SpannableString("$goldInfo1 $goldInfo2").apply {
                setSpan(
                    ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.feature_limits_light_grey)),
                    length - goldInfo2.length,
                    length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            textGoldState.setBackgroundResource(
                if (isGoldPending) R.drawable.bkgd_blue_100_rounded
                else R.drawable.bkgd_green_100_rounded
            )
            textGoldState.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (isGoldPending) R.color.blue_600
                    else R.color.green_600
                )
            )
            textGoldState.setText(
                if (isGoldPending) R.string.kyc_upgrade_now_tier_state_under_review
                else R.string.most_popular
            )
        }
    }

    private fun launchKyc() {
        KycNavHostActivity.start(requireContext(), CampaignType.None)
        dismiss()
    }

    companion object {
        private const val ARG_IS_GOLD_PENDING = "ARG_IS_GOLD_PENDING"

        fun newInstance(isGoldPending: Boolean) = KycUpgradeNowSheet().apply {
            arguments = Bundle().apply {
                putBoolean(ARG_IS_GOLD_PENDING, isGoldPending)
            }
        }
    }
}
