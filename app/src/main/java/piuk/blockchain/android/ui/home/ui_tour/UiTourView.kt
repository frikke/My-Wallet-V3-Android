package piuk.blockchain.android.ui.home.ui_tour

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
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
        fun startDashboardOnboarding()
        fun startBuy()
        fun dismiss()
    }

    lateinit var host: Host

    private val binding: ViewUiTourBinding =
        ViewUiTourBinding.inflate(LayoutInflater.from(context), this, true)

    private var currentStep: UiTourStep = UiTourStep.values().first()
        set(step) {
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
            else context.getString(R.string.next)
    }

    private fun updateNavigationIndicators(step: UiTourStep) {
        binding.bottomNavigation.invisibleIf { step == UiTourStep.BUYER_HANDHOLD }
        binding.cardOnboardingWrapper.visibleIf { step == UiTourStep.BUYER_HANDHOLD }

        binding.pagerIndicator.selectedIndex = step.ordinal
        val bottomNavigationSelectedNavigationItem = when (step) {
            UiTourStep.HOME -> NavigationItem.Home
            UiTourStep.PRICES -> NavigationItem.Prices
            UiTourStep.BUYER_HANDHOLD,
            UiTourStep.MIDDLE_BUTTON -> null
            UiTourStep.ACTIVITY -> NavigationItem.Activity
        }
        binding.bottomNavigation.selectedNavigationItem = bottomNavigationSelectedNavigationItem

        val cardTriangleIndex = when (step) {
            UiTourStep.HOME -> 0
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
            isPulseAnimationEnabled = true
            onNavigationItemClick = {
                val step = when (it) {
                    NavigationItem.Home -> UiTourStep.HOME
                    NavigationItem.Prices -> UiTourStep.PRICES
                    NavigationItem.BuyAndSell -> null
                    NavigationItem.Activity -> UiTourStep.ACTIVITY
                    else -> throw IllegalStateException("Illegal navigation state - unknown item $it")
                }
                if (step != null) currentStep = step
            }
            onMiddleButtonClick = {
                isPulseAnimationEnabled = false
                currentStep = UiTourStep.MIDDLE_BUTTON
            }
        }
        binding.buttonNext.onClick = {
            val nextStep = UiTourStep.values().getOrNull(currentStep.ordinal + 1)
            if (nextStep != null) currentStep = nextStep
            else host.startBuy()
        }
        binding.buttonClose.setOnClickListener {
            host.dismiss()
        }
        binding.viewDismissableZone.setOnClickListener {
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
            UiTourStep.HOME -> R.drawable.ui_tour_home
            UiTourStep.PRICES -> R.drawable.ui_tour_prices
            UiTourStep.MIDDLE_BUTTON -> R.drawable.ui_tour_middle_button
            UiTourStep.ACTIVITY -> R.drawable.ui_tour_activity
            UiTourStep.BUYER_HANDHOLD -> R.drawable.ui_tour_buyer_handhold
        }

    private val UiTourStep.title: String
        get() = context.getString(
            when (this) {
                UiTourStep.HOME -> R.string.ui_tour_home_title
                UiTourStep.PRICES -> R.string.ui_tour_prices_title
                UiTourStep.MIDDLE_BUTTON -> R.string.ui_tour_middle_button_title
                UiTourStep.ACTIVITY -> R.string.ui_tour_activity_title
                UiTourStep.BUYER_HANDHOLD -> R.string.ui_tour_buyer_handhold_title
            }
        )

    private val UiTourStep.subtitle: String
        get() = context.getString(
            when (this) {
                UiTourStep.HOME -> R.string.ui_tour_home_subtitle
                UiTourStep.PRICES -> R.string.ui_tour_prices_subtitle
                UiTourStep.MIDDLE_BUTTON -> R.string.ui_tour_middle_button_subtitle
                UiTourStep.ACTIVITY -> R.string.ui_tour_activity_subtitle
                UiTourStep.BUYER_HANDHOLD -> R.string.ui_tour_buyer_handhold_subtitle
            }
        )
}

enum class UiTourStep {
    BUYER_HANDHOLD,
    PRICES,
    MIDDLE_BUTTON,
    HOME,
    ACTIVITY
}
