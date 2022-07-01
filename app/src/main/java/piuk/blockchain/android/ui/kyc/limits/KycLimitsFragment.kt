package piuk.blockchain.android.ui.kyc.limits

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockchain.api.NabuApiExceptionFactory
import com.blockchain.commonarch.presentation.base.HostedBottomSheet
import com.blockchain.commonarch.presentation.mvi.MviFragment
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.componentlib.viewextensions.visibleIf
import com.blockchain.core.limits.Feature
import com.blockchain.core.limits.FeatureLimit
import com.blockchain.koin.scopedInject
import com.blockchain.nabu.Tier
import piuk.blockchain.android.R
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.databinding.FragmentKycLimitsBinding
import piuk.blockchain.android.ui.adapters.Diffable
import piuk.blockchain.android.ui.base.ErrorButtonCopies
import piuk.blockchain.android.ui.base.ErrorDialogData
import piuk.blockchain.android.ui.base.ErrorSlidingBottomDialog
import piuk.blockchain.android.ui.kyc.navhost.KycNavHostActivity
import retrofit2.HttpException

class KycLimitsFragment :
    MviFragment<KycLimitsModel, KycLimitsIntent, KycLimitsState, FragmentKycLimitsBinding>(),
    HostedBottomSheet.Host,
    ErrorSlidingBottomDialog.Host {

    override val model: KycLimitsModel by scopedInject()

    private val adapter by lazy {
        FeatureLimitAdapterDelegate(
            onHeaderCtaClicked = ::handleHeaderCtaClick
        )
    }

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentKycLimitsBinding =
        FragmentKycLimitsBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            recyclerviewFeatures.layoutManager = LinearLayoutManager(requireContext())
            recyclerviewFeatures.adapter = adapter

            errorBtnOk.setOnClickListener {
                requireActivity().finish()
            }
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

        handleErrorState(newState.errorState)
        handleActiveSheet(newState.activeSheet)
        handleNavigation(newState.navigationAction)
    }

    private fun handleErrorState(errorState: KycLimitsError) = when (errorState) {
        is KycLimitsError.SheetError -> {
            val nabuException = (errorState.exception as? HttpException)?.let {
                NabuApiExceptionFactory.fromResponseBody(errorState.exception)
            }

            showBottomSheet(
                ErrorSlidingBottomDialog.newInstance(
                    ErrorDialogData(
                        title = nabuException?.getServerSideErrorInfo()?.title ?: getString(
                            R.string.setting_limits_load_error_title
                        ),
                        description = nabuException?.getServerSideErrorInfo()?.description ?: getString(
                            R.string.setting_limits_load_error_description
                        ),
                        errorButtonCopies = ErrorButtonCopies(primaryButtonText = getString(R.string.common_ok)),
                        error = errorState.toString(),
                        nabuApiException = (errorState.exception as? HttpException)?.let {
                            NabuApiExceptionFactory.fromResponseBody(errorState.exception)
                        },
                        errorDescription = errorState.exception.message,
                        action = "KYC_LIMITS",
                        analyticsCategories = nabuException?.getServerSideErrorInfo()?.categories ?: emptyList()
                    )
                )
            )
        }
        is KycLimitsError.FullscreenError -> {
            // do nothing
        }
        KycLimitsError.None -> {
            // do nothing
        }
    }

    private fun handleActiveSheet(activeSheet: KycLimitsSheet) = when (activeSheet) {
        KycLimitsSheet.None -> {
        }
        is KycLimitsSheet.UpgradeNow ->
            showBottomSheet(KycLimitsUpgradeNowSheet.newInstance(activeSheet.isGoldPending))
    }

    private fun handleNavigation(navigationAction: KycLimitsNavigationAction) = when (navigationAction) {
        KycLimitsNavigationAction.None -> {
        }
        KycLimitsNavigationAction.StartKyc -> {
            KycNavHostActivity.start(requireContext(), CampaignType.None)
            model.process(KycLimitsIntent.ClearNavigation)
        }
    }

    private fun handleHeaderCtaClick(header: Header) {
        when (header) {
            Header.NEW_KYC -> model.process(KycLimitsIntent.NewKycHeaderCtaClicked)
            Header.UPGRADE_TO_GOLD -> model.process(KycLimitsIntent.UpgradeToGoldHeaderCtaClicked)
            Header.HIDDEN,
            Header.MAX_TIER_REACHED,
            -> {
            }
        }
    }

    private fun showLoading() {
        binding.progress.visible()
        binding.progress.playAnimation()
    }

    private fun hideLoading() {
        binding.progress.gone()
        binding.progress.pauseAnimation()
    }

    override fun onErrorPrimaryCta() {
        // do nothing
    }

    override fun onErrorSecondaryCta() {
        // do nothing
    }

    override fun onErrorTertiaryCta() {
        // do nothing
    }

    override fun onSheetClosed() {
        model.process(KycLimitsIntent.CloseSheet)
    }

    companion object {
        fun newInstance(): KycLimitsFragment = KycLimitsFragment()
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
