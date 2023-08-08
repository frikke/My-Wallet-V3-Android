package piuk.blockchain.android.simplebuy.sheets

import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockchain.commonarch.presentation.base.SlidingModalBottomDialog
import com.blockchain.core.kyc.domain.KycService
import com.blockchain.core.kyc.domain.model.KycTier
import com.blockchain.presentation.koin.scopedInject
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.UnlockHigherLimitsLayoutBinding
import piuk.blockchain.android.kyc.KycAnalytics
import piuk.blockchain.android.ui.customviews.ButtonOptions
import piuk.blockchain.android.ui.customviews.VerifyIdentityIconedBenefitItem

class UnlockHigherLimitsBottomSheet : SlidingModalBottomDialog<UnlockHigherLimitsLayoutBinding>() {

    private val kycService: KycService by scopedInject()

    private val compositeDisposable = CompositeDisposable()

    interface Host : SlidingModalBottomDialog.Host {
        fun unlockHigherLimits()
    }

    override val host: Host by lazy {
        super.host as? Host ?: throw IllegalStateException(
            "Host fragment is not a UnlockHigherLimitsBottomSheet.Host"
        )
    }

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): UnlockHigherLimitsLayoutBinding =
        UnlockHigherLimitsLayoutBinding.inflate(inflater, container, false)

    override fun initControls(binding: UnlockHigherLimitsLayoutBinding) {
        compositeDisposable += kycService.getTiersLegacy().map {
            it.tierForLevel(KycTier.GOLD).kycLimits?.dailyLimit?.let { dailyLimit ->
                dailyLimit.toStringWithSymbol()
            } ?: getString(com.blockchain.stringResources.R.string.empty)
        }.onErrorReturn { getString(com.blockchain.stringResources.R.string.empty) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onSuccess = { limit ->
                binding.unlockMoreView.initWithBenefits(
                    benefits = listOf(
                        VerifyIdentityIconedBenefitItem(
                            title = getString(com.blockchain.stringResources.R.string.cash_accounts),
                            subtitle = getString(com.blockchain.stringResources.R.string.cash_accounts_description),
                            icon = R.drawable.ic_cash
                        ),
                        VerifyIdentityIconedBenefitItem(
                            title = getString(com.blockchain.stringResources.R.string.link_a_bank),
                            subtitle = getString(com.blockchain.stringResources.R.string.link_a_bank_description),
                            icon = R.drawable.ic_bank_details
                        ),
                        VerifyIdentityIconedBenefitItem(
                            title = getString(com.blockchain.stringResources.R.string.earn_rewards),
                            subtitle = getString(com.blockchain.stringResources.R.string.earn_rewards_description),
                            icon = R.drawable.ic_interest
                        )
                    ),
                    title = getString(com.blockchain.stringResources.R.string.unlock_gold_level_trading),
                    description = if (limit.isNotEmpty()) {
                        getString(
                            com.blockchain.stringResources.R.string.verify_your_identity_limits_1,
                            limit
                        )
                    } else {
                        getString(com.blockchain.stringResources.R.string.empty)
                    },
                    icon = R.drawable.ic_gold_square,
                    primaryButton = ButtonOptions(
                        true,
                        getString(com.blockchain.stringResources.R.string.apply_and_unlock)
                    ) {
                        dismiss()
                        host.unlockHigherLimits()
                    },
                    secondaryButton = ButtonOptions(false)
                )
            }, onError = {})

        analytics.logEvent(KycAnalytics.UPGRADE_TO_GOLD_SEEN)
    }
}
