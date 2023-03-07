package piuk.blockchain.android.ui.transactionflow.flow

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.TxConfirmationValue
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.componentlib.viewextensions.visibleIf
import com.blockchain.core.price.ExchangeRates
import com.blockchain.extensions.enumValueOfOrNull
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.presentation.customviews.BlockchainListDividerDecor
import com.blockchain.presentation.koin.scopedInject
import org.koin.android.ext.android.inject
import piuk.blockchain.android.databinding.FragmentTxFlowConfirmBinding
import piuk.blockchain.android.fraud.domain.service.FraudService
import piuk.blockchain.android.simplebuy.sheets.AchTermsAndConditionsBottomSheet
import piuk.blockchain.android.simplebuy.sheets.AchWithdrawalHoldInfoBottomSheet
import piuk.blockchain.android.ui.transactionflow.engine.TransactionIntent
import piuk.blockchain.android.ui.transactionflow.engine.TransactionState
import piuk.blockchain.android.ui.transactionflow.engine.TxExecutionStatus
import piuk.blockchain.android.ui.transactionflow.flow.adapter.ConfirmTransactionDelegateAdapter
import piuk.blockchain.android.ui.transactionflow.flow.customisations.TransactionConfirmationCustomisations
import piuk.blockchain.android.ui.transactionflow.plugin.TxFlowWidget
import timber.log.Timber

class ConfirmTransactionFragment : TransactionFlowFragment<FragmentTxFlowConfirmBinding>() {

    private val exchangeRates: ExchangeRates by scopedInject()
    private val prefs: CurrencyPrefs by scopedInject()
    private val mapper: TxConfirmReadOnlyMapperCheckout by scopedInject()
    private val customiser: TransactionConfirmationCustomisations by inject()
    private val fraudService: FraudService by inject()

    private var headerSlot: TxFlowWidget? = null
    private val assetAction: AssetAction? by lazy {
        enumValueOfOrNull<AssetAction>(arguments?.getString(ACTION).orEmpty())
    }

    private val listAdapter: ConfirmTransactionDelegateAdapter by lazy {
        ConfirmTransactionDelegateAdapter(
            model = model,
            mapper = mapper,
            selectedCurrency = prefs.selectedFiatCurrency,
            exchangeRates = exchangeRates,
            onTooltipClicked = { expandableType ->
                when (expandableType) {
                    is TxConfirmationValue.NetworkFee -> analyticsHooks.onFeesTooltipClicked(assetAction)
                    is TxConfirmationValue.ExchangePriceConfirmation ->
                        analyticsHooks.onPriceTooltipClicked(assetAction)
                    is TxConfirmationValue.AvailableToWithdraw ->
                        showBottomSheet(AchWithdrawalHoldInfoBottomSheet.newInstance())
                    is TxConfirmationValue.AchTermsAndConditions ->
                        showBottomSheet(
                            AchTermsAndConditionsBottomSheet.newInstance(
                                bankLabel = expandableType.bankLabel,
                                amount = expandableType.amount,
                                withdrawalLock = expandableType.withdrawalLock,
                                isRecurringBuyEnabled = false
                            )
                        )
                    else -> {
                        /*NO-OP*/
                    }
                }
            },
            coroutineScope = lifecycleScope
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        analyticsHooks.onViewCheckoutScreen(assetAction)
        with(binding.confirmDetailsList) {
            addItemDecoration(BlockchainListDividerDecor(requireContext()))

            layoutManager = LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.VERTICAL,
                false
            )
            adapter = listAdapter
            itemAnimator = null
        }
        model.process(TransactionIntent.ValidateTransaction)
        model.process(TransactionIntent.FetchConfirmationRates)
        model.process(TransactionIntent.LoadImprovedPaymentUxFeatureFlag)
        model.process(TransactionIntent.LoadDepositTerms)
    }

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentTxFlowConfirmBinding =
        FragmentTxFlowConfirmBinding.inflate(inflater, container, false)

    override fun render(newState: TransactionState) {
        Timber.d("!TRANSACTION!> Rendering! ConfirmTransactionFragment")
        // We _should_ always have a pending Tx when we get here
        newState.pendingTx?.let {
            listAdapter.items = newState.pendingTx.txConfirmations.toMutableList().apply {
                customiser.confirmAvailableToTradeBlurb(newState, newState.action, requireContext())?.let {
                    add(TxConfirmationValue.AvailableToTrade(it))
                }
                customiser.confirmAvailableToWithdrawBlurb(newState, newState.action, requireContext())?.let {
                    add(TxConfirmationValue.AvailableToWithdraw(it))
                }
                customiser.confirmAchDisclaimerBlurb(newState, newState.action, requireContext())?.let {
                    add(
                        TxConfirmationValue.AchTermsAndConditions(
                            value = it.value,
                            bankLabel = it.bankLabel,
                            amount = it.amount,
                            withdrawalLock = it.withdrawalLock
                        )
                    )
                }
            }
        }

        if (newState.executionStatus == TxExecutionStatus.Cancelled) {
            activity.finish()
        }

        with(binding) {
            with(confirmCtaButton) {
                text = customiser.confirmCtaText(newState)
                buttonState = if (newState.nextEnabled) ButtonState.Enabled else ButtonState.Disabled
                onClick = { onCtaClick(newState) }
            }

            with(buttonCancel) {
                text = customiser.cancelButtonText(newState.action)
                visibleIf { customiser.cancelButtonVisible(newState.action) }
                onClick = { onCancelClick() }
            }

            if (customiser.confirmDisclaimerVisibility(newState, newState.action)) {
                confirmDisclaimer.apply {
                    text = customiser.confirmDisclaimerBlurb(newState, requireContext())
                    visible()
                    movementMethod = LinkMovementMethod.getInstance()
                }
            }
            initialiseHeaderSlotIfNeeded(newState)
        }

        headerSlot?.update(newState)
    }

    private fun FragmentTxFlowConfirmBinding.initialiseHeaderSlotIfNeeded(state: TransactionState) {
        if (headerSlot == null) {
            headerSlot = customiser.confirmInstallHeaderView(
                requireContext(),
                confirmHeaderSlot,
                state
            ).apply {
                initControl(model, customiser, analyticsHooks)
            }
        }
    }

    private fun onCtaClick(state: TransactionState) {
        fraudService.endFlow()
        analyticsHooks.onConfirmationCtaClick(state)
        model.process(TransactionIntent.ExecuteTransaction)
    }

    private fun onCancelClick() {
        model.process(TransactionIntent.CancelTransaction)
    }

    companion object {
        private const val ACTION = "ASSET_ACTION"
        fun newInstance(assetAction: AssetAction): ConfirmTransactionFragment =
            ConfirmTransactionFragment().apply {
                arguments = Bundle().apply {
                    putString(ACTION, assetAction.name)
                }
            }
    }
}
