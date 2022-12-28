package piuk.blockchain.android.ui.dashboard.onboarding

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockchain.api.NabuApiExceptionFactory
import com.blockchain.commonarch.presentation.mvi.MviActivity
import com.blockchain.componentlib.databinding.ToolbarGeneralBinding
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.componentlib.viewextensions.visibleIf
import com.blockchain.domain.onboarding.CompletableDashboardOnboardingStep
import com.blockchain.domain.onboarding.DashboardOnboardingStep
import com.blockchain.domain.onboarding.DashboardOnboardingStepState
import com.blockchain.domain.paymentmethods.model.BankAuthSource
import com.blockchain.domain.paymentmethods.model.CardRejectionState
import com.blockchain.domain.paymentmethods.model.PaymentMethod
import com.blockchain.extensions.exhaustive
import com.blockchain.presentation.koin.scopedInject
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.core.parameter.parametersOf
import piuk.blockchain.android.R
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.cards.CardDetailsActivity
import piuk.blockchain.android.databinding.ActivityDashboardOnboardingBinding
import piuk.blockchain.android.simplebuy.paymentmethods.PaymentMethodChooserBottomSheet
import piuk.blockchain.android.ui.base.ErrorButtonCopies
import piuk.blockchain.android.ui.base.ErrorDialogData
import piuk.blockchain.android.ui.base.ErrorSlidingBottomDialog
import piuk.blockchain.android.ui.dashboard.sheets.WireTransferAccountDetailsBottomSheet
import piuk.blockchain.android.ui.kyc.navhost.KycNavHostActivity
import piuk.blockchain.android.ui.linkbank.BankAuthActivity
import retrofit2.HttpException

class DashboardOnboardingActivity :
    MviActivity<
        DashboardOnboardingModel,
        DashboardOnboardingIntent,
        DashboardOnboardingState,
        ActivityDashboardOnboardingBinding
        >(),
    PaymentMethodChooserBottomSheet.Host,
    ErrorSlidingBottomDialog.Host {

    private var analyticsCurrentStepIndex: Int? = null
    private var analyticsNextStepButtonClicked = false

    override val alwaysDisableScreenshots: Boolean = false

    override val model: DashboardOnboardingModel by scopedInject {
        parametersOf(intent.argInitialSteps())
    }

    private val adapter: OnboardingStepAdapter by lazy {
        OnboardingStepAdapter(
            onStepClicked = {
                model.process(DashboardOnboardingIntent.StepClicked(it.step))
                analyticsNextStepButtonClicked = false
            }
        )
    }

    override val toolbarBinding: ToolbarGeneralBinding
        get() = binding.toolbar

    private val isSuperappDesignEnabled: Boolean by lazy {
        intent.argIsSuperappDesignEnabled()
    }
    private val viewState = MutableStateFlow(DashboardOnboardingState())

    override fun initBinding(): ActivityDashboardOnboardingBinding =
        ActivityDashboardOnboardingBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (isSuperappDesignEnabled) {
            setContent {
                val state by viewState.collectAsStateLifecycleAware()
                DashboardOnboardingScreen(
                    state = state,
                    onIntent = model::process,
                    backClicked = { finish() },
                    analyticsNextStepButtonClicked = { analyticsNextStepButtonClicked = true },
                )
            }
        } else {
            binding.root.visible()
            updateToolbar(backAction = { finish() })
            binding.recyclerviewSteps.layoutManager = LinearLayoutManager(this@DashboardOnboardingActivity)
            binding.recyclerviewSteps.adapter = adapter
        }

        intent.argInitialSteps().toCurrentStepIndex()?.let {
            analytics.logEvent(DashboardOnboardingAnalytics.Viewed(it))
        }
    }

    override fun onResume() {
        super.onResume()
        model.process(DashboardOnboardingIntent.FetchSteps)
    }

    override fun render(newState: DashboardOnboardingState) {
        viewState.value = newState
        analyticsCurrentStepIndex = newState.steps.toCurrentStepIndex()

        if (newState.navigationAction != null) handleNavigation(newState.navigationAction)
        if (newState.error != null) handleError(newState.error)
        val totalSteps = newState.steps.size
        val completeSteps = newState.steps.count { it.isCompleted }

        if (!isSuperappDesignEnabled) {
            adapter.submitList(newState.steps)
            updateCtaButton(newState.steps)
            binding.progressSteps.setProgress((completeSteps.toFloat() / totalSteps.toFloat()) * 100f)
            binding.textSteps.text = getString(R.string.dashboard_onboarding_steps_counter, completeSteps, totalSteps)
        }

        if (totalSteps == completeSteps) {
            Handler(Looper.getMainLooper()).postDelayed({
                finish()
            }, 500)
        }
    }

    private fun handleError(error: Throwable) {
        val nabuException = (error as? HttpException)?.let {
            NabuApiExceptionFactory.fromResponseBody(error)
        }

        showBottomSheet(
            ErrorSlidingBottomDialog.newInstance(
                ErrorDialogData(
                    title = nabuException?.getServerSideErrorInfo()?.title ?: getString(
                        R.string.dashboard_onboarding_error_title
                    ),
                    description = nabuException?.getServerSideErrorInfo()?.description ?: getString(
                        R.string.dashboard_onboarding_error_description
                    ),
                    errorButtonCopies = ErrorButtonCopies(primaryButtonText = getString(R.string.common_ok)),
                    error = error.message,
                    nabuApiException = (error as? HttpException)?.let {
                        NabuApiExceptionFactory.fromResponseBody(error)
                    },
                    errorDescription = error.message,
                    action = "DASHBOARD",
                    analyticsCategories = nabuException?.getServerSideErrorInfo()?.categories ?: emptyList()
                )
            )
        )
        model.process(DashboardOnboardingIntent.ClearError)
    }

    @Suppress("IMPLICIT_CAST_TO_ANY")
    private fun handleNavigation(action: DashboardOnboardingNavigationAction) {
        when (action) {
            DashboardOnboardingNavigationAction.StartKyc -> {
                analyticsCurrentStepIndex?.let {
                    analytics.logEvent(
                        DashboardOnboardingAnalytics.StepLaunched(
                            it, DashboardOnboardingStep.UPGRADE_TO_GOLD, analyticsNextStepButtonClicked
                        )
                    )
                }
                KycNavHostActivity.start(this, CampaignType.None)
            }
            is DashboardOnboardingNavigationAction.AddPaymentMethod -> {
                analyticsCurrentStepIndex?.let {
                    analytics.logEvent(
                        DashboardOnboardingAnalytics.StepLaunched(
                            it, DashboardOnboardingStep.LINK_PAYMENT_METHOD, analyticsNextStepButtonClicked
                        )
                    )
                }
                showBottomSheet(
                    PaymentMethodChooserBottomSheet.newInstance(
                        paymentMethods = action.eligiblePaymentMethods,
                        mode = PaymentMethodChooserBottomSheet.DisplayMode.PAYMENT_METHOD_TYPES,
                        canAddNewPayment = true
                    )
                )
            }
            DashboardOnboardingNavigationAction.OpenBuy -> {
                analyticsCurrentStepIndex?.let {
                    analytics.logEvent(
                        DashboardOnboardingAnalytics.StepLaunched(
                            it, DashboardOnboardingStep.BUY, analyticsNextStepButtonClicked
                        )
                    )
                }
                val intent = Intent()
                intent.putExtra(RESULT_LAUNCH_BUY_FLOW, true)
                setResult(RESULT_OK, intent)
                finish()
            }
            DashboardOnboardingNavigationAction.AddCard -> {
                val intent = Intent(this, CardDetailsActivity::class.java)
                startActivity(intent)
            }
            is DashboardOnboardingNavigationAction.WireTransferAccountDetails -> {
                showBottomSheet(WireTransferAccountDetailsBottomSheet.newInstance(action.currency))
            }
            is DashboardOnboardingNavigationAction.LinkBank -> {
                val intent = BankAuthActivity.newInstance(
                    action.linkBankTransfer,
                    BankAuthSource.SIMPLE_BUY,
                    this
                )
                startActivity(intent)
            }
        }.exhaustive
        model.process(DashboardOnboardingIntent.ClearNavigation)
    }

    override fun onPaymentMethodChanged(paymentMethod: PaymentMethod) {
        model.process(DashboardOnboardingIntent.PaymentMethodClicked(paymentMethod.type))
    }

    override fun showAvailableToAddPaymentMethods() {
        // The paymentmethodchooser bottomsheet should be correctly configured so not to show Add Payment method button
        throw UnsupportedOperationException()
    }

    override fun onRejectableCardSelected(cardInfo: CardRejectionState) {
        // do nothing
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
        // do nothing
    }

    private fun updateCtaButton(steps: List<CompletableDashboardOnboardingStep>) {
        val firstIncompleteStep: DashboardOnboardingStep? = steps.find { !it.isCompleted }?.step
        binding.buttonCta.visibleIf { firstIncompleteStep != null }
        if (firstIncompleteStep == null) return

        binding.buttonCta.apply {
            setText(firstIncompleteStep.titleRes)
            backgroundTintList = firstIncompleteStep.ctaButtonTint
            setOnClickListener {
                model.process(DashboardOnboardingIntent.StepClicked(firstIncompleteStep))
                analyticsNextStepButtonClicked = true
            }
        }
    }

    override fun finish() {
        analyticsCurrentStepIndex?.let {
            analytics.logEvent(DashboardOnboardingAnalytics.Dismissed(it))
        }
        super.finish()
    }

    // We have not been provided button states from design, so we're dynamically creating them
    private val DashboardOnboardingStep.ctaButtonTint: ColorStateList
        get() {
            val base = ContextCompat.getColor(this@DashboardOnboardingActivity, colorRes)
            val lighten = ColorUtils.blendARGB(base, Color.WHITE, 0.35f)
            val darken = ColorUtils.blendARGB(base, Color.BLACK, 0.35f)
            val states = arrayOf(
                intArrayOf(-android.R.attr.state_enabled),
                intArrayOf(android.R.attr.state_pressed),
                intArrayOf(android.R.attr.state_enabled)
            )
            val colors = intArrayOf(
                lighten,
                darken,
                base
            )
            return ColorStateList(states, colors)
        }

    private fun Intent.argIsSuperappDesignEnabled(): Boolean =
        getBooleanExtra(ARG_IS_SUPERAPP_DESIGN_ENABLED, false)

    private fun Intent.argInitialSteps(): List<CompletableDashboardOnboardingStep> {
        val statesArray = getStringArrayExtra(ARG_INITIAL_STEPS_STATES)

        return DashboardOnboardingStep.values().mapIndexed { index, step ->
            val state = statesArray?.getOrNull(index)?.let { DashboardOnboardingStepState.valueOf(it) }
                ?: DashboardOnboardingStepState.INCOMPLETE
            CompletableDashboardOnboardingStep(step, state)
        }
    }

    companion object {
        private const val ARG_IS_SUPERAPP_DESIGN_ENABLED = "ARG_IS_SUPERAPP_DESIGN_ENABLED"
        private const val ARG_INITIAL_STEPS_STATES = "ARG_INITIAL_STEPS_STATES"
        private const val RESULT_LAUNCH_BUY_FLOW = "RESULT_LAUNCH_BUY_FLOW"

        private fun newIntent(
            context: Context,
            isSuperappDesignEnabled: Boolean,
            initialSteps: List<CompletableDashboardOnboardingStep>,
        ): Intent = Intent(context, DashboardOnboardingActivity::class.java).apply {
            if (initialSteps.isNotEmpty()) {
                putExtra(ARG_IS_SUPERAPP_DESIGN_ENABLED, isSuperappDesignEnabled)
                putExtra(ARG_INITIAL_STEPS_STATES, initialSteps.map { it.state.name }.toTypedArray())
            }
        }
    }

    data class ActivityArgs(
        val isSuperappDesignEnabled: Boolean,
        val initialSteps: List<CompletableDashboardOnboardingStep>,
    )

    sealed class ActivityResult {
        object LaunchBuyFlow : ActivityResult()
    }

    class BlockchainActivityResultContract : ActivityResultContract<ActivityArgs, ActivityResult?>() {
        override fun createIntent(context: Context, input: ActivityArgs): Intent = newIntent(
            context = context,
            isSuperappDesignEnabled = input.isSuperappDesignEnabled,
            initialSteps = input.initialSteps,
        )

        override fun parseResult(resultCode: Int, intent: Intent?): ActivityResult? {
            val launchBuyFlow = intent?.getBooleanExtra(RESULT_LAUNCH_BUY_FLOW, false) ?: false

            return when {
                resultCode != RESULT_OK -> null
                launchBuyFlow -> ActivityResult.LaunchBuyFlow
                else -> null
            }
        }
    }
}

val DashboardOnboardingStep.oldIconRes: Int
    @DrawableRes get() = when (this) {
        DashboardOnboardingStep.UPGRADE_TO_GOLD -> R.drawable.ic_onboarding_step_upgrade_to_gold
        DashboardOnboardingStep.LINK_PAYMENT_METHOD -> R.drawable.ic_onboarding_step_link_paymentmethod
        DashboardOnboardingStep.BUY -> R.drawable.ic_onboarding_step_buy
    }

val DashboardOnboardingStep.iconRes: Int
    @DrawableRes get() = when (this) {
        DashboardOnboardingStep.UPGRADE_TO_GOLD -> R.drawable.ic_identification_filled
        DashboardOnboardingStep.LINK_PAYMENT_METHOD -> R.drawable.ic_bank_details
        DashboardOnboardingStep.BUY -> R.drawable.ic_cart
    }

val DashboardOnboardingStep.colorRes: Int
    @ColorRes get() = when (this) {
        DashboardOnboardingStep.UPGRADE_TO_GOLD -> R.color.onboarding_step_upgrade_to_gold
        DashboardOnboardingStep.LINK_PAYMENT_METHOD -> R.color.onboarding_step_link_payment_method
        DashboardOnboardingStep.BUY -> R.color.onboarding_step_buy
    }

val DashboardOnboardingStep.titleRes: Int
    @StringRes get() = when (this) {
        DashboardOnboardingStep.UPGRADE_TO_GOLD -> R.string.dashboard_onboarding_step_upgrade_to_gold
        DashboardOnboardingStep.LINK_PAYMENT_METHOD -> R.string.dashboard_onboarding_step_link_payment_method
        DashboardOnboardingStep.BUY -> R.string.dashboard_onboarding_step_link_buy
    }

val DashboardOnboardingStep.subtitleRes: Int
    @StringRes get() = when (this) {
        DashboardOnboardingStep.UPGRADE_TO_GOLD -> R.string.dashboard_onboarding_step_upgrade_to_gold_time
        DashboardOnboardingStep.LINK_PAYMENT_METHOD -> R.string.dashboard_onboarding_step_link_payment_method_time
        DashboardOnboardingStep.BUY -> R.string.dashboard_onboarding_step_link_buy_time
    }
