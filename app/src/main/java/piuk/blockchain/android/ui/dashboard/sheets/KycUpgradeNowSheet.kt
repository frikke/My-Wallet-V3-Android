package piuk.blockchain.android.ui.dashboard.sheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.blockchain.commonarch.presentation.base.SlidingModalBottomDialog
import com.blockchain.componentlib.navigation.NavigationBarButton
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.px
import com.blockchain.domain.eligibility.model.TransactionsLimit
import com.blockchain.koin.scopedInject
import com.blockchain.nabu.Feature
import com.blockchain.nabu.Tier
import com.blockchain.nabu.UserIdentity
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.RoundedCornerTreatment
import com.google.android.material.shape.ShapeAppearanceModel
import com.google.android.material.tabs.TabLayoutMediator
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.Singles
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.DialogSheetKycUpgradeNowBinding

class KycUpgradeNowSheet : SlidingModalBottomDialog<DialogSheetKycUpgradeNowBinding>() {

    interface Host : SlidingModalBottomDialog.Host {
        fun startKycClicked()
    }

    private val disposables = CompositeDisposable()

    private val userIdentity: UserIdentity by scopedInject()

    private val initialTab: ViewPagerTab = ViewPagerTab.VERIFIED
    private lateinit var tabLayoutMediator: TabLayoutMediator

    private val transactionsLimit: TransactionsLimit by lazy {
        arguments?.getSerializable(ARG_TRANSACTIONS_LIMIT) as TransactionsLimit
    }

    private var ctaClicked = false
    private val getHighestTierAndIsSdd: Single<Pair<Tier, Boolean>> by lazy {
        Singles.zip(
            userIdentity.getHighestApprovedKycTier(),
            userIdentity.isVerifiedFor(Feature.SimplifiedDueDiligence)
        ).cache()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // This is because we need to show this as a regular fragment as well as a BottomSheet
        if (!showsDialog) {
            val binding = initBinding(inflater, container)
            initControls(binding)
            return binding.root
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): DialogSheetKycUpgradeNowBinding =
        DialogSheetKycUpgradeNowBinding.inflate(inflater, container, false)

    override fun initControls(binding: DialogSheetKycUpgradeNowBinding): Unit = with(binding) {
        if (!showsDialog) {
            sheetIndicator.gone()
            toolbar.gone()
        } else {
            toolbar.apply {
                startNavigationButton = null
                endNavigationBarButtons = listOf(
                    NavigationBarButton.Icon(
                        drawable = R.drawable.ic_close_circle_v2,
                        color = null,
                        contentDescription = R.string.accessibility_close,
                        onIconClick = {
                            if (showsDialog) dismiss()
                        }
                    )
                )
            }
        }

        val indicatorDrawable = MaterialShapeDrawable(
            ShapeAppearanceModel()
                .toBuilder()
                .setAllCorners(RoundedCornerTreatment())
                .setAllCornerSizes(8.px.toFloat())
                .build()
        ).apply {
            initializeElevationOverlay(requireContext())
            elevation = 8f
            shadowCompatibilityMode = MaterialShapeDrawable.SHADOW_COMPAT_MODE_ALWAYS
        }

        tabLayout.setSelectedTabIndicator(indicatorDrawable)

        val viewPagerAdapter = KycCtaViewPagerAdapter(
            basicClicked = {
                ctaClicked = true
                logAnalytics(AnalyticsType.GetBasicClicked)
                startKycClicked()
            },
            verifyClicked = {
                ctaClicked = true
                logAnalytics(AnalyticsType.GetVerifiedClicked)
                startKycClicked()
            }
        ).apply {
            val initialItems = ViewPagerTab.values().toList().toItems(isBasicApproved = false)
            submitList(initialItems)
        }

        viewPager.adapter = viewPagerAdapter
        tabLayoutMediator = TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (ViewPagerTab.values()[position]) {
                ViewPagerTab.BASIC -> getString(R.string.kyc_upgrade_now_tab_basic)
                ViewPagerTab.VERIFIED -> getString(R.string.kyc_upgrade_now_tab_verified)
            }
        }
        tabLayoutMediator.attach()
        viewPager.setCurrentItem(ViewPagerTab.values().indexOf(initialTab), false)

        disposables +=
            getHighestTierAndIsSdd.subscribeBy(
                onSuccess = { (highestTier, _) ->
                    val isAtleastSilver = highestTier != Tier.BRONZE
                    val items = ViewPagerTab.values().toList().toItems(isBasicApproved = isAtleastSilver)
                    viewPagerAdapter.submitList(items)
                },
                onError = {}
            )

        logAnalytics(AnalyticsType.Viewed)
    }

    private fun startKycClicked() {
        (host as Host).startKycClicked()
        if (showsDialog) dismiss()
    }

    override fun onDestroyView() {
        if (!ctaClicked) logAnalytics(AnalyticsType.Dismissed)
        disposables.dispose()
        tabLayoutMediator.detach()
        super.onDestroyView()
    }

    enum class ViewPagerTab {
        BASIC,
        VERIFIED
    }

    private fun List<ViewPagerTab>.toItems(
        isBasicApproved: Boolean
    ): List<ViewPagerItem> = map {
        when (it) {
            ViewPagerTab.BASIC -> ViewPagerItem.Basic(isBasicApproved, transactionsLimit)
            ViewPagerTab.VERIFIED -> ViewPagerItem.Verified
        }
    }

    private fun logAnalytics(type: AnalyticsType) {
        disposables += getHighestTierAndIsSdd.subscribe { (highestTier, isSdd) ->
            val event = when (type) {
                AnalyticsType.GetBasicClicked -> KycUpgradeNowGetBasicClicked(highestTier, isSdd)
                AnalyticsType.GetVerifiedClicked -> KycUpgradeNowGetVerifiedClicked(highestTier, isSdd)
                AnalyticsType.Viewed -> KycUpgradeNowViewed(highestTier, isSdd)
                AnalyticsType.Dismissed -> KycUpgradeNowDismissed(highestTier, isSdd)
            }
            analytics.logEvent(event)
        }
    }

    private enum class AnalyticsType {
        GetBasicClicked,
        GetVerifiedClicked,
        Viewed,
        Dismissed,
    }

    companion object {
        private const val ARG_TRANSACTIONS_LIMIT = "ARG_TRANSACTIONS_LIMIT"

        fun newInstance(
            transactionsLimit: TransactionsLimit = TransactionsLimit.Unlimited,
        ): KycUpgradeNowSheet = KycUpgradeNowSheet().apply {
            arguments = Bundle().apply {
                putSerializable(ARG_TRANSACTIONS_LIMIT, transactionsLimit)
            }
        }
    }
}
