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
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.koin.earnTabFeatureFlag
import com.blockchain.koin.scopedInject
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import org.koin.core.component.KoinComponent
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ViewUiTourBinding
import piuk.blockchain.android.domain.usecases.DashboardOnboardingStep

class UiTourView @JvmOverloads constructor(
    ctx: Context,
    attr: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(ctx, attr, defStyle), KoinComponent {

    interface Host {
        val analytics: Analytics

        fun startDashboardOnboarding()
        fun startBuy()
        fun dismiss()
    }

    lateinit var host: Host

    private val compositeDisposable = CompositeDisposable()
    private val isEarnOnNavBarFlag by scopedInject<FeatureFlag>(earnTabFeatureFlag)
    private var isEarnOnNavBar: Boolean = false

    private val binding: ViewUiTourBinding =
        ViewUiTourBinding.inflate(LayoutInflater.from(context), this, true)

    var currentStep: UiTourStep = UiTourStep.values().first()
        private set(step) {
            field = step
            updateNavigationIndicators(step)
            updateContent(step)
        }

    var newTourCurrentStep: NewUiTourStep = NewUiTourStep.values().first()
        private set(step) {
            field = step
            updateNewNavigationIndicators(step)
            updateNewContent(step)
        }

    init {
        checkEarnFeatureFlag()
        with(binding) {
            buttonClose.setOnClickListener {
                host.dismiss()
            }
            viewDismissableZoneTop.setOnClickListener {
                host.dismiss()
            }
            viewDismissableZoneBottom.setOnClickListener {
                host.dismiss()
            }
            cardOnboarding.setOnClickListener {
                host.startDashboardOnboarding()
            }
            cardOnboarding.setTotalSteps(DashboardOnboardingStep.values().size)
            cardOnboarding.setCompleteSteps(0)
        }
    }

    private fun checkEarnFeatureFlag() {
        compositeDisposable += isEarnOnNavBarFlag.enabled.subscribeBy { enabled ->
            isEarnOnNavBar = enabled
            with(binding) {
                if (enabled) {
                    pagerIndicator.count = NewUiTourStep.values().size
                    pagerIndicator.selectedIndex = 0
                    bottomNavigation.apply {
                        navigationItems = listOf(
                            NavigationItem.Home,
                            NavigationItem.Prices,
                            NavigationItem.Earn,
                            NavigationItem.Activity
                        )
                        hasMiddleButton = true
                        onNavigationItemClick = {
                            val step = when (it) {
                                NavigationItem.Home -> null
                                NavigationItem.Prices -> NewUiTourStep.PRICES
                                NavigationItem.Earn -> NewUiTourStep.EARN
                                NavigationItem.Activity -> NewUiTourStep.ACTIVITY
                                else -> throw IllegalStateException("Illegal navigation state - unknown item $it")
                            }
                            if (step != null) newTourCurrentStep = step
                        }
                        onMiddleButtonClick = {
                            newTourCurrentStep = NewUiTourStep.MIDDLE_BUTTON
                        }
                    }
                    buttonNext.onClick = {
                        val nextStep = NewUiTourStep.values().getOrNull(newTourCurrentStep.ordinal + 1)
                        if (nextStep != null) {
                            host.analytics.logEvent(UiTourAnalytics.NewTourProgressClicked(nextStep))
                            newTourCurrentStep = nextStep
                        } else {
                            host.analytics.logEvent(UiTourAnalytics.NewTourCtaClicked(newTourCurrentStep))
                            host.startBuy()
                        }
                    }
                    newTourCurrentStep = NewUiTourStep.values().first()
                } else {
                    pagerIndicator.count = UiTourStep.values().size
                    pagerIndicator.selectedIndex = 0
                    bottomNavigation.apply {
                        navigationItems = listOf(
                            NavigationItem.Home,
                            NavigationItem.Prices,
                            NavigationItem.BuyAndSell,
                            NavigationItem.Activity
                        )
                        hasMiddleButton = true
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
                    buttonNext.onClick = {
                        val nextStep = UiTourStep.values().getOrNull(currentStep.ordinal + 1)
                        if (nextStep != null) {
                            host.analytics.logEvent(UiTourAnalytics.ProgressClicked(nextStep))
                            currentStep = nextStep
                        } else {
                            host.analytics.logEvent(UiTourAnalytics.CtaClicked(currentStep))
                            host.startBuy()
                        }
                    }
                    currentStep = UiTourStep.values().first()
                }
            }
        }
    }

    private fun updateNewContent(step: NewUiTourStep) {
        binding.banner.setImageResource(step.banner)
        binding.title.text = step.title
        binding.subtitle.text = step.subtitle
        binding.buttonNext.text =
            if (step == NewUiTourStep.values().last()) {
                context.getString(R.string.buy_now)
            } else {
                context.getString(R.string.common_next)
            }
    }

    private fun updateNewNavigationIndicators(step: NewUiTourStep) {
        binding.bottomNavigation.invisibleIf { step == NewUiTourStep.BUYER_HANDHOLD }
        binding.cardOnboardingWrapper.visibleIf { step == NewUiTourStep.BUYER_HANDHOLD }
        binding.viewDismissableZoneBottom.visibleIf { step == NewUiTourStep.BUYER_HANDHOLD }

        binding.pagerIndicator.selectedIndex = step.ordinal
        val bottomNavigationSelectedNavigationItem = when (step) {
            NewUiTourStep.EARN -> NavigationItem.Earn
            NewUiTourStep.PRICES -> NavigationItem.Prices
            NewUiTourStep.BUYER_HANDHOLD,
            NewUiTourStep.MIDDLE_BUTTON -> null
            NewUiTourStep.ACTIVITY -> NavigationItem.Activity
        }
        binding.bottomNavigation.selectedNavigationItem = bottomNavigationSelectedNavigationItem

        val cardTriangleIndex = when (step) {
            NewUiTourStep.EARN -> 3
            NewUiTourStep.PRICES -> 1
            NewUiTourStep.BUYER_HANDHOLD,
            NewUiTourStep.MIDDLE_BUTTON -> 2
            NewUiTourStep.ACTIVITY -> 4
        }
        binding.card.updateEdgeTriangle(cardTriangleIndex, 4)
    }

    private fun updateContent(step: UiTourStep) {
        binding.banner.setImageResource(step.banner)
        binding.title.text = step.title
        binding.subtitle.text = step.subtitle
        binding.buttonNext.text =
            if (step == UiTourStep.values().last()) {
                context.getString(R.string.buy_now)
            } else {
                context.getString(R.string.common_next)
            }
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

    private val NewUiTourStep.banner: Int
        @DrawableRes get() = when (this) {
            NewUiTourStep.EARN -> R.drawable.ui_tour_earn
            NewUiTourStep.PRICES -> R.drawable.ui_tour_prices
            NewUiTourStep.MIDDLE_BUTTON -> R.drawable.ui_tour_buy_and_sell
            NewUiTourStep.ACTIVITY -> R.drawable.ui_tour_activity
            NewUiTourStep.BUYER_HANDHOLD -> R.drawable.ui_tour_buyer_handhold
        }

    private val NewUiTourStep.title: String
        get() = context.getString(
            when (this) {
                NewUiTourStep.EARN -> R.string.ui_tour_earn_title
                NewUiTourStep.PRICES -> R.string.ui_tour_prices_title
                NewUiTourStep.MIDDLE_BUTTON -> R.string.ui_tour_buy_and_sell_title
                NewUiTourStep.ACTIVITY -> R.string.ui_tour_activity_title
                NewUiTourStep.BUYER_HANDHOLD -> R.string.ui_tour_buyer_handhold_title
            }
        )

    private val NewUiTourStep.subtitle: String
        get() = context.getString(
            when (this) {
                NewUiTourStep.EARN -> R.string.ui_tour_earn_subtitle
                NewUiTourStep.PRICES -> R.string.ui_tour_prices_subtitle
                NewUiTourStep.MIDDLE_BUTTON -> R.string.ui_tour_buy_and_sell_subtitle
                NewUiTourStep.ACTIVITY -> R.string.ui_tour_activity_subtitle
                NewUiTourStep.BUYER_HANDHOLD -> R.string.ui_tour_buyer_handhold_subtitle
            }
        )

    private val UiTourStep.banner: Int
        @DrawableRes get() = when (this) {
            UiTourStep.BUY_AND_SELL -> R.drawable.ui_tour_buy_and_sell
            UiTourStep.PRICES -> R.drawable.ui_tour_prices
            UiTourStep.MIDDLE_BUTTON -> R.drawable.ui_tour_buy_and_sell
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

    fun logHideUi() {
        if (isEarnOnNavBar) {
            host.analytics.logEvent(UiTourAnalytics.NewTourDismissed(newTourCurrentStep))
        } else {
            host.analytics.logEvent(UiTourAnalytics.Dismissed(currentStep))
        }
    }
}

enum class UiTourStep {
    BUY_AND_SELL,
    PRICES,
    MIDDLE_BUTTON,
    ACTIVITY,
    BUYER_HANDHOLD
}

enum class NewUiTourStep {
    PRICES,
    MIDDLE_BUTTON,
    EARN,
    ACTIVITY,
    BUYER_HANDHOLD
}
