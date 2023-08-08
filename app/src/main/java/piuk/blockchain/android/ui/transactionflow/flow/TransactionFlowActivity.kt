package piuk.blockchain.android.ui.transactionflow.flow

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.NullCryptoAccount
import com.blockchain.coincore.SingleAccount
import com.blockchain.coincore.TransactionTarget
import com.blockchain.coincore.impl.CryptoNonCustodialAccount
import com.blockchain.coincore.impl.CustodialTradingAccount
import com.blockchain.commonarch.presentation.base.SlidingModalBottomDialog
import com.blockchain.commonarch.presentation.base.addTransactionAnimation
import com.blockchain.commonarch.presentation.mvi.MviActivity
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.databinding.ToolbarGeneralBinding
import com.blockchain.componentlib.navigation.NavigationBarButton
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.utils.openUrl
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.hideKeyboard
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.domain.dataremediation.DataRemediationService
import com.blockchain.domain.dataremediation.model.QuestionnaireContext
import com.blockchain.earn.activeRewards.ActiveRewardsWithdrawalWarningSheet
import com.blockchain.fiatActions.QuestionnaireSheetHost
import com.blockchain.logging.RemoteLogger
import com.blockchain.nabu.BlockedReason
import com.blockchain.outcome.doOnSuccess
import com.blockchain.preferences.DashboardPrefs
import com.blockchain.presentation.customviews.kyc.KycUpgradeNowSheet
import com.blockchain.presentation.extensions.getAccount
import com.blockchain.presentation.extensions.getTarget
import com.blockchain.presentation.extensions.putAccount
import com.blockchain.presentation.extensions.putTarget
import com.blockchain.presentation.koin.scopedInject
import com.blockchain.transactions.NewTransactionFlowActivity
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.isLayer2Token
import io.reactivex.rxjava3.disposables.CompositeDisposable
import org.koin.android.ext.android.inject
import org.koin.core.scope.Scope
import org.koin.java.KoinJavaComponent
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ActivityTransactionFlowBinding
import piuk.blockchain.android.fraud.domain.service.FraudFlow
import piuk.blockchain.android.fraud.domain.service.FraudService
import piuk.blockchain.android.ui.customviews.BlockedDueToNotEligibleSheet
import piuk.blockchain.android.ui.customviews.BlockedDueToSanctionsSheet
import piuk.blockchain.android.ui.dataremediation.QuestionnaireSheet
import piuk.blockchain.android.ui.kyc.navhost.KycNavHostActivity
import piuk.blockchain.android.ui.kyc.navhost.models.KycEntryPoint
import piuk.blockchain.android.ui.transactionflow.analytics.TxFlowAnalytics
import piuk.blockchain.android.ui.transactionflow.engine.TransactionIntent
import piuk.blockchain.android.ui.transactionflow.engine.TransactionModel
import piuk.blockchain.android.ui.transactionflow.engine.TransactionState
import piuk.blockchain.android.ui.transactionflow.engine.TransactionStep
import piuk.blockchain.android.ui.transactionflow.flow.customisations.BackNavigationState
import piuk.blockchain.android.ui.transactionflow.flow.customisations.TransactionFlowCustomisations
import piuk.blockchain.android.ui.transactionflow.flow.sheets.StakingAccountWithdrawWarning
import piuk.blockchain.android.ui.transactionflow.transactionFlowActivityScope
import piuk.blockchain.android.urllinks.ETH_STAKING_CONSIDERATIONS
import timber.log.Timber

class TransactionFlowActivity :
    MviActivity<TransactionModel, TransactionIntent, TransactionState, ActivityTransactionFlowBinding>(),
    SlidingModalBottomDialog.Host,
    QuestionnaireSheetHost,
    KycUpgradeNowSheet.Host,
    StakingAccountWithdrawWarning.Host,
    ActiveRewardsWithdrawalWarningSheet.Host {

    private val scopeId: String by lazy {
        "${TX_SCOPE_ID}_${this@TransactionFlowActivity.hashCode()}"
    }

    val scope: Scope by lazy {
        openScope()
        KoinJavaComponent.getKoin().getScope(scopeId)
    }

    override val model: TransactionModel by scope.inject()

    override val alwaysDisableScreenshots: Boolean
        get() = false

    override val toolbarBinding: ToolbarGeneralBinding
        get() = binding.toolbar

    private val analyticsHooks: TxFlowAnalytics by inject()
    private val customiser: TransactionFlowCustomisations by inject()
    private val remoteLogger: RemoteLogger by inject()
    private val dashboardPrefs: DashboardPrefs by inject()
    private val dataRemediationService: DataRemediationService by scopedInject()
    private val fraudService: FraudService by inject()
    private lateinit var startingIntent: TransactionIntent
    private val assetCatalogue: AssetCatalogue by inject()

    private val sourceAccount: SingleAccount by lazy {
        intent.extras?.getAccount(SOURCE) as? SingleAccount ?: kotlin.run {
            remoteLogger.logException(IllegalStateException(), "No source account specified for action $action")
            NullCryptoAccount()
        }
    }

    private val origin: String by lazy {
        intent.extras?.getString(ORIGIN).orEmpty()
    }

    private val transactionTarget: TransactionTarget by lazy {
        intent.extras?.getTarget(TARGET) ?: kotlin.run {
            remoteLogger.logException(IllegalStateException(), "No target account specified for action $action")
            NullCryptoAccount()
        }
    }

    private val action: AssetAction by lazy {
        intent.extras?.getSerializable(ACTION) as? AssetAction ?: throw IllegalStateException("No action specified")
    }

    private val startKycForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        startModel()
    }

    private val compositeDisposable = CompositeDisposable()
    private var currentStep: TransactionStep = TransactionStep.ZERO
    private lateinit var state: TransactionState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupBackPress()

        updateToolbarBackground(mutedBackground = true)

        updateToolbar(
            menuItems = listOf(
                NavigationBarButton.Icon(
                    drawable = R.drawable.ic_close,
                    contentDescription = com.blockchain.stringResources.R.string.accessibility_close
                ) { finish() }
            ),
            backAction = {
                onBackPressedDispatcher.onBackPressed()
                onBackPressedAnalytics(state)
            }
        )

        // header icon
        sourceAccount.takeIf { it !is NullCryptoAccount }?.let {
            val l2Network = (sourceAccount as? CryptoNonCustodialAccount)?.currency
                ?.takeIf { it.isLayer2Token }
                ?.coinNetwork

            val mainIcon = ImageResource.Remote(sourceAccount.currency.logo)
            val tagIcon = l2Network?.nativeAssetTicker
                ?.let { assetCatalogue.fromNetworkTicker(it)?.logo }
                ?.let { ImageResource.Remote(it) }
            val icon = tagIcon?.let {
                StackedIcon.SmallTag(
                    main = mainIcon,
                    tag = tagIcon
                )
            } ?: StackedIcon.SingleIcon(mainIcon)
            updateToolbarIcon(icon)
        }

        binding.txProgress.visible()
        startModel()
    }

    private fun onBackPressedAnalytics(state: TransactionState) {
        if (state.currentStep == TransactionStep.ENTER_AMOUNT) {
            analyticsHooks.onAmountScreenBackClicked(state)
        } else if (state.currentStep == TransactionStep.CONFIRM_DETAIL) {
            analyticsHooks.onCheckoutScreenBackClicked(state)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_tx_flow, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun initBinding(): ActivityTransactionFlowBinding =
        ActivityTransactionFlowBinding.inflate(layoutInflater)

    private fun startModel() {
        val intentMapper = TransactionFlowIntentMapper(
            sourceAccount = sourceAccount,
            target = transactionTarget,
            origin = origin,
            action = action
        )

        val transactionIntent = intentMapper.map(sourceAccount.requireSecondPassword())
        startingIntent = transactionIntent
        model.process(transactionIntent)

        // TODO(aromano): SWAP SELL keep this in
        if (action == AssetAction.Swap || action == AssetAction.Sell) {
            lifecycleScope.launchWhenResumed {
                dataRemediationService.getQuestionnaire(QuestionnaireContext.TRADING)
                    .doOnSuccess { questionnaire ->
                        if (questionnaire != null) {
                            showBottomSheet(QuestionnaireSheet.newInstance(questionnaire, true))
                        }
                    }
            }
        }
    }

    override fun render(newState: TransactionState) {
        handleStateChange(newState)
        state = newState
    }

    private fun handleStateChange(state: TransactionState) {
        if (currentStep == state.currentStep) {
            return
        }

        when (state.currentStep) {
            TransactionStep.ZERO -> {
                // do nothing
            }

            TransactionStep.CLOSED -> dismissFlow()
            else -> analyticsHooks.onStepChanged(state)
        }

        state.currentStep.takeIf { it != TransactionStep.ZERO }?.let { step ->
            showFlowStep(step, state.featureBlockedReason, state.action)
            customiser.getScreenTitle(state).takeIf { it.isNotEmpty() }?.let {
                updateToolbarTitle(it)
            } ?: updateToolbar()

            currentStep = step
        }

        if (!state.canGoBack) {
            updateToolbarBackAction(null)
        } else {
            updateToolbarBackAction { onBackPressedDispatcher.onBackPressed() }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                navigateOnBackPressed { finish() }
                true
            }

            R.id.action_close -> {
                dismissFlow()
                true
            }

            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(owner = this) {
            onBackPressedAnalytics(state)
            navigateOnBackPressed { finish() }
        }
    }

    private fun navigateOnBackPressed(finalAction: () -> Unit) {
        if (::state.isInitialized && state.canGoBack) {
            when (customiser.getBackNavigationAction(state)) {
                BackNavigationState.ClearTransactionTarget -> {
                    model.process(TransactionIntent.ClearSelectedTarget)
                    model.process(TransactionIntent.ReturnToPreviousStep)
                }

                BackNavigationState.ResetPendingTransaction -> {
                    hideKeyboard()
                    model.process(TransactionIntent.InvalidateTransaction)
                }

                BackNavigationState.ResetPendingTransactionKeepingTarget -> {
                    hideKeyboard()
                    binding.txProgress.visible()
                    model.process(TransactionIntent.InvalidateTransactionKeepingTarget)
                }

                BackNavigationState.NavigateToPreviousScreen -> model.process(TransactionIntent.ReturnToPreviousStep)
            }
        } else {
            finalAction()
        }
    }

    private fun showFlowStep(step: TransactionStep, featureBlockedReason: BlockedReason?, assetAction: AssetAction) {
        when (step) {
            TransactionStep.ZERO,
            TransactionStep.CLOSED -> null

            TransactionStep.FEATURE_BLOCKED -> when (featureBlockedReason) {
                is BlockedReason.Sanctions -> BlockedDueToSanctionsSheet.newInstance(featureBlockedReason)
                is BlockedReason.NotEligible -> BlockedDueToNotEligibleSheet.newInstance(featureBlockedReason)
                is BlockedReason.TooManyInFlightTransactions,
                is BlockedReason.InsufficientTier -> KycUpgradeNowSheet.newInstance()

                is BlockedReason.ShouldAcknowledgeStakingWithdrawal -> StakingAccountWithdrawWarning.newInstance(
                    featureBlockedReason.assetIconUrl,
                    featureBlockedReason.unbondingDays
                )

                is BlockedReason.ShouldAcknowledgeActiveRewardsWithdrawalWarning ->
                    ActiveRewardsWithdrawalWarningSheet.newInstance()

                null -> throw IllegalStateException(
                    "No featureBlockedReason provided for TransactionStep.FEATURE_BLOCKED, state $state"
                )
            }

            TransactionStep.ENTER_PASSWORD -> EnterSecondPasswordFragment.newInstance()
            TransactionStep.SELECT_SOURCE -> SelectSourceAccountFragment.newInstance(assetAction)
            TransactionStep.ENTER_ADDRESS -> EnterTargetAddressFragment.newInstance()
            TransactionStep.ENTER_AMOUNT -> {
                checkRemainingSendAttemptsWithoutBackup()
                EnterAmountFragment.newInstance(assetAction)
            }

            TransactionStep.SELECT_TARGET_ACCOUNT -> SelectTargetAccountFragment.newInstance()
            TransactionStep.CONFIRM_DETAIL -> ConfirmTransactionFragment.newInstance(assetAction)
            TransactionStep.IN_PROGRESS -> TransactionProgressFragment.newInstance()
        }?.let {
            binding.txProgress.gone()

            if (it is BottomSheetDialogFragment) {
                showBottomSheet(it)
            } else {
                val transaction = supportFragmentManager.beginTransaction()
                    .addTransactionAnimation()
                    .replace(R.id.tx_flow_content, it, it.toString())

                if (!supportFragmentManager.fragments.contains(it)) {
                    transaction.addToBackStack(it.toString())
                }

                transaction.commit()
            }
        }
    }

    private fun checkRemainingSendAttemptsWithoutBackup() {
        if (state.action == AssetAction.Send &&
            state.sendingAccount is CustodialTradingAccount &&
            dashboardPrefs.remainingSendsWithoutBackup > 0
        ) {
            dashboardPrefs.remainingSendsWithoutBackup = dashboardPrefs.remainingSendsWithoutBackup - 1
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        dismissFlow()
    }

    private fun dismissFlow() {
        fraudService.endFlows(FraudFlow.ACH_DEPOSIT, FraudFlow.OB_DEPOSIT, FraudFlow.WITHDRAWAL)

        compositeDisposable.clear()
        model.destroy()
        if (scope.isNotClosed()) {
            scope.close()
        }
        if (this.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            finish()
        }
    }

    private fun openScope() =
        try {
            KoinJavaComponent.getKoin().getOrCreateScope(
                scopeId,
                transactionFlowActivityScope
            )
        } catch (e: Throwable) {
            Timber.wtf("Error opening scope for id $scopeId - $e")
        }

    override fun startKycClicked() {
        val campaign = when (action) {
            AssetAction.Swap -> KycEntryPoint.Swap
            AssetAction.Buy -> KycEntryPoint.Buy
            AssetAction.InterestDeposit -> KycEntryPoint.Interest
            AssetAction.InterestWithdraw -> KycEntryPoint.Interest
            else -> KycEntryPoint.Other
        }
        startKycForResult.launch(KycNavHostActivity.newIntent(this, campaign))
    }

    override fun learnMoreClicked() {
        openUrl(ETH_STAKING_CONSIDERATIONS)
    }

    override fun onNextClicked() {
        model.process(TransactionIntent.ShowSourceSelection)
    }

    override fun onClose() {
        dismissFlow()
    }

    override fun onSheetClosed() {
        // do nothing
    }

    override fun questionnaireSubmittedSuccessfully() {
        // no op
    }

    override fun questionnaireSkipped() {
        // no op
    }

    override fun openExternalUrl(url: String) {
        openUrl(url)
    }

    companion object {
        private const val SOURCE = "SOURCE_ACCOUNT"
        private const val TARGET = "TARGET_ACCOUNT"
        private const val ACTION = "ASSET_ACTION"
        private const val ORIGIN = "ORIGIN"
        private const val TX_SCOPE_ID = "TRANSACTION_ACTIVITY_SCOPE_ID"

        fun newIntent(
            context: Context,
            sourceAccount: BlockchainAccount? = NullCryptoAccount(),
            target: TransactionTarget? = NullCryptoAccount(),
            action: AssetAction,
            origin: String = ""
        ): Intent {
            val bundle = Bundle().apply {
                putAccount(SOURCE, sourceAccount ?: NullCryptoAccount())
                putTarget(TARGET, target ?: NullCryptoAccount())
                putSerializable(ACTION, action)
                putString(ORIGIN, origin)
            }

            val activityClass = if (shouldUseNewFlow(action)) {
                NewTransactionFlowActivity::class.java
            } else {
                TransactionFlowActivity::class.java
            }

            return Intent(context, activityClass).apply {
                putExtras(bundle)
            }
        }

        private fun shouldUseNewFlow(action: AssetAction): Boolean =
            action in listOf(AssetAction.Swap, AssetAction.Sell)
    }
}
