package piuk.blockchain.android.ui.kyc.limits

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockchain.commonarch.presentation.base.HostedBottomSheet
import com.blockchain.commonarch.presentation.mvi.MviActivity
import com.blockchain.commonarch.presentation.mvi.MviFragment
import com.blockchain.componentlib.databinding.ToolbarGeneralBinding
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.componentlib.viewextensions.visibleIf
import com.blockchain.core.limits.Feature
import com.blockchain.core.limits.FeatureLimit
import com.blockchain.extensions.exhaustive
import com.blockchain.koin.scopedInject
import com.blockchain.nabu.Tier
import piuk.blockchain.android.R
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.databinding.ActivityKycLimitsBinding
import piuk.blockchain.android.ui.adapters.Diffable
import piuk.blockchain.android.ui.base.ErrorSlidingBottomDialog
import piuk.blockchain.android.ui.kyc.navhost.KycNavHostActivity

class KycLimitsActivity :
    MviActivity<KycLimitsModel, KycLimitsIntent, KycLimitsState, ActivityKycLimitsBinding>(),
    HostedBottomSheet.Host {

    override val alwaysDisableScreenshots: Boolean = false
    override val model: KycLimitsModel by scopedInject()

    private val adapter by lazy {
        FeatureLimitAdapterDelegate(
            onHeaderCtaClicked = ::handleHeaderCtaClick
        )
    }

    override fun initBinding(): ActivityKycLimitsBinding =
        ActivityKycLimitsBinding.inflate(layoutInflater)

    override val toolbarBinding: ToolbarGeneralBinding
        get() = binding.toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updateToolbar(
            toolbarTitle = getString(R.string.feature_limits_toolbar),
            backAction = { finish() }
        )

        binding.recyclerviewFeatures.layoutManager = LinearLayoutManager(this)
        binding.recyclerviewFeatures.adapter = adapter

        binding.errorBtnOk.setOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        model.process(KycLimitsIntent.FetchLimitsAndTiers)
    }

    override fun render(newState: KycLimitsState) {
        if (newState.isLoading) {
            showLoading()
        } else {
            hideLoading()
        }

        binding.recyclerviewFeatures.visibleIf { newState.errorState is KycLimitsError.None && !newState.isLoading }
        binding.errorContainer.visibleIf { newState.errorState is KycLimitsError.FullscreenError }

        if (
            newState.errorState is KycLimitsError.None &&
            !newState.isLoading &&
            newState.featuresWithLimits.isNotEmpty()
        ) {
            val items = mutableListOf<KycLimitsItem>()

            if (newState.header != Header.HIDDEN) items.add(KycLimitsItem.HeaderItem(newState.header))
            items.add(KycLimitsItem.FeaturesHeaderItem)
            when (newState.currentKycTierRow) {
                CurrentKycTierRow.SILVER -> items.add(KycLimitsItem.CurrentTierItem(Tier.SILVER))
                CurrentKycTierRow.GOLD -> items.add(KycLimitsItem.CurrentTierItem(Tier.GOLD))
                CurrentKycTierRow.HIDDEN -> {
                }
            }
            items.addAll(newState.featuresWithLimits.map { KycLimitsItem.FeatureWithLimitItem(it.feature, it.limit) })
            items.add(KycLimitsItem.FooterItem)

            adapter.submitList(items)
        }

        when (newState.errorState) {
            is KycLimitsError.FullscreenError -> {
            }
            is KycLimitsError.SheetError -> {
                ErrorSlidingBottomDialog.newInstance(this)
                    .show(supportFragmentManager, MviFragment.BOTTOM_SHEET)
            }
            KycLimitsError.None -> {
            }
        }.exhaustive

        when (val sheet = newState.activeSheet) {
            KycLimitsSheet.None -> {
            }
            is KycLimitsSheet.UpgradeNow ->
                KycUpgradeNowSheet.newInstance(sheet.isGoldPending)
                    .show(supportFragmentManager, MviFragment.BOTTOM_SHEET)
        }

        when (newState.navigationAction) {
            KycLimitsNavigationAction.None -> {
            }
            KycLimitsNavigationAction.StartKyc -> {
                KycNavHostActivity.start(this, CampaignType.None)
                model.process(KycLimitsIntent.ClearNavigation)
            }
        }
    }

    private fun handleHeaderCtaClick(header: Header) {
        when (header) {
            Header.NEW_KYC -> model.process(KycLimitsIntent.NewKycHeaderCtaClicked)
            Header.UPGRADE_TO_GOLD -> model.process(KycLimitsIntent.UpgradeToGoldHeaderCtaClicked)
            Header.HIDDEN,
            Header.MAX_TIER_REACHED -> {
            }
        }
    }

    override fun showLoading() {
        binding.progress.visible()
        binding.progress.playAnimation()
    }

    override fun hideLoading() {
        binding.progress.gone()
        binding.progress.pauseAnimation()
    }

    override fun onSheetClosed() {
        model.process(KycLimitsIntent.CloseSheet)
    }

    companion object {
        fun newIntent(context: Context): Intent = Intent(context, KycLimitsActivity::class.java)
    }
}

sealed class KycLimitsItem : Diffable<KycLimitsItem> {
    data class HeaderItem(val header: Header) : KycLimitsItem()
    object FeaturesHeaderItem : KycLimitsItem()
    data class CurrentTierItem(val tier: Tier) : KycLimitsItem()
    data class FeatureWithLimitItem(val feature: Feature, val limit: FeatureLimit) : KycLimitsItem() {
        override fun areItemsTheSame(otherItem: KycLimitsItem): Boolean =
            otherItem is FeatureWithLimitItem &&
                this.feature == otherItem.feature
    }

    object FooterItem : KycLimitsItem()

    override fun areItemsTheSame(otherItem: KycLimitsItem): Boolean =
        this::class == otherItem::class

    override fun areContentsTheSame(otherItem: KycLimitsItem): Boolean =
        this == otherItem
}
