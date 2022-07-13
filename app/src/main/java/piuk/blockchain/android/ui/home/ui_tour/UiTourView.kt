package piuk.blockchain.android.ui.home.ui_tour

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import com.blockchain.analytics.Analytics
import com.blockchain.componentlib.navigation.NavigationItem
import com.blockchain.componentlib.viewextensions.invisibleIf
import com.blockchain.componentlib.viewextensions.visibleIf
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ViewUiTourBinding
import piuk.blockchain.android.domain.usecases.DashboardOnboardingStep

class UiTourView @JvmOverloads constructor(
    ctx: Context,
    attr: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(ctx, attr, defStyle) {

    interface Host {
        val analytics: Analytics

        fun startDashboardOnboarding()
        fun startBuy()
        fun dismiss()
    }

    lateinit var host: Host

    private val binding: ViewUiTourBinding =
        ViewUiTourBinding.inflate(LayoutInflater.from(context), this, true)

    var currentStep: UiTourStep = UiTourStep.values().first()
        private set(step) {
            field = step
            updateNavigationIndicators(step)
            updateContent(step)
        }

    private fun updateContent(step: UiTourStep) {
        binding.banner.setImageResource(step.banner)
        binding.title.text = step.title
        binding.subtitle.text = step.subtitle
        binding.buttonNext.text =
            if (step == UiTourStep.values().last()) context.getString(R.string.buy_now)
            else context.getString(R.string.common_next)
    }

    private fun updateNavigationIndicators(step: UiTourStep) {
        binding.bottomNavigation.invisibleIf { step == UiTourStep.BUYER_HANDHOLD }
        binding.cardOnboardingWrapper.visibleIf { step == UiTourStep.BUYER_HANDHOLD }
        binding.viewDismissableZoneBottom.visibleIf { step == UiTourStep.BUYER_HANDHOLD }

        binding.pagerIndicator.selectedIndex = step.ordinal
        val bottomNavigationSelectedNavigationItem = when (step) {
            UiTourStep.BUY_AND_SELL -> NavigationItem.BuyAndSell
            UiTourStep.PRICES -> NavigationItem.Prices
            UiTourStep.BUYER_HANDHOLD,
            UiTourStep.MIDDLE_BUTTON -> null
            UiTourStep.ACTIVITY -> NavigationItem.Activity
        }
        binding.bottomNavigation.selectedNavigationItem = bottomNavigationSelectedNavigationItem

        val cardTriangleIndex = when (step) {
            UiTourStep.BUY_AND_SELL -> 3
            UiTourStep.PRICES -> 1
            UiTourStep.BUYER_HANDHOLD,
            UiTourStep.MIDDLE_BUTTON -> 2
            UiTourStep.ACTIVITY -> 4
        }
        binding.card.updateEdgeTriangle(cardTriangleIndex, 4)
    }

    init {
        binding.pagerIndicator.count = UiTourStep.values().size
        binding.pagerIndicator.selectedIndex = 0
        binding.bottomNavigation.apply {
            onNavigationItemClick = {
                val step = when (it) {
                    NavigationItem.Home -> null
                    NavigationItem.Prices -> UiTourStep.PRICES
                    NavigationItem.BuyAndSell -> UiTourStep.BUY_AND_SELL
                    NavigationItem.Activity -> UiTourStep.ACTIVITY
                    else -> throw IllegalStateException("Illegal navigation state - unknown item $it")
                }
                if (step != null) currentStep = step
            }
            onMiddleButtonClick = {
                currentStep = UiTourStep.MIDDLE_BUTTON
            }
        }
        binding.buttonNext.onClick = {
            val nextStep = UiTourStep.values().getOrNull(currentStep.ordinal + 1)
            if (nextStep != null) {
                host.analytics.logEvent(UiTourAnalytics.ProgressClicked(nextStep))
                currentStep = nextStep
            } else {
                host.analytics.logEvent(UiTourAnalytics.CtaClicked(currentStep))
                host.startBuy()
            }
        }
        binding.buttonClose.setOnClickListener {
            host.dismiss()
        }
        binding.viewDismissableZoneTop.setOnClickListener {
            host.dismiss()
        }
        binding.viewDismissableZoneBottom.setOnClickListener {
            host.dismiss()
        }
        binding.cardOnboarding.setOnClickListener {
            host.startDashboardOnboarding()
        }
        binding.cardOnboarding.setTotalSteps(DashboardOnboardingStep.values().size)
        binding.cardOnboarding.setCompleteSteps(0)
        currentStep = UiTourStep.values().first()
    }

    private val UiTourStep.banner: Int
        @DrawableRes get() = when (this) {
            UiTourStep.BUY_AND_SELL -> R.drawable.ui_tour_buy_and_sell
            UiTourStep.PRICES -> R.drawable.ui_tour_prices
            UiTourStep.MIDDLE_BUTTON -> R.drawable.ui_tour_middle_button
            UiTourStep.ACTIVITY -> R.drawable.ui_tour_activity
            UiTourStep.BUYER_HANDHOLD -> R.drawable.ui_tour_buyer_handhold
        }

    private val UiTourStep.title: String
        get() = context.getString(
            when (this) {
                UiTourStep.BUY_AND_SELL -> R.string.ui_tour_buy_and_sell_title
                UiTourStep.PRICES -> R.string.ui_tour_prices_title
                UiTourStep.MIDDLE_BUTTON -> R.string.ui_tour_middle_button_title
                UiTourStep.ACTIVITY -> R.string.ui_tour_activity_title
                UiTourStep.BUYER_HANDHOLD -> R.string.ui_tour_buyer_handhold_title
            }
        )

    private val UiTourStep.subtitle: String
        get() = context.getString(
            when (this) {
                UiTourStep.BUY_AND_SELL -> R.string.ui_tour_buy_and_sell_subtitle
                UiTourStep.PRICES -> R.string.ui_tour_prices_subtitle
                UiTourStep.MIDDLE_BUTTON -> R.string.ui_tour_middle_button_subtitle
                UiTourStep.ACTIVITY -> R.string.ui_tour_activity_subtitle
                UiTourStep.BUYER_HANDHOLD -> R.string.ui_tour_buyer_handhold_subtitle
            }
        )
}

enum class UiTourStep {
    BUY_AND_SELL,
    PRICES,
    MIDDLE_BUTTON,
    ACTIVITY,
    BUYER_HANDHOLD
}
