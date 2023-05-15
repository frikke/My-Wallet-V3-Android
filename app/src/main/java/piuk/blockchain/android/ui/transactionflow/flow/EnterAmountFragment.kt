package piuk.blockchain.android.ui.transactionflow.flow

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.FeeLevel
import com.blockchain.coincore.NullCryptoAccount
import com.blockchain.coincore.PendingTx
import com.blockchain.coincore.SingleAccount
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.componentlib.viewextensions.visibleIf
import com.blockchain.domain.eligibility.model.TransactionsLimit
import com.blockchain.domain.paymentmethods.model.FundsLocks
import com.blockchain.extensions.enumValueOfOrNull
import com.blockchain.presentation.customviews.kyc.KycUpgradeNowSheet
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Currency
import info.blockchain.balance.CurrencyType
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.schedulers.Schedulers
import java.math.RoundingMode
import java.util.concurrent.TimeUnit
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.databinding.FragmentTxFlowEnterAmountBinding
import piuk.blockchain.android.fraud.domain.service.FraudFlow
import piuk.blockchain.android.fraud.domain.service.FraudService
import piuk.blockchain.android.simplebuy.SimpleBuyActivity
import piuk.blockchain.android.ui.customviews.inputview.FiatCryptoInputView
import piuk.blockchain.android.ui.customviews.inputview.FiatCryptoViewConfiguration
import piuk.blockchain.android.ui.customviews.inputview.PrefixedOrSuffixedEditText
import piuk.blockchain.android.ui.kyc.navhost.KycNavHostActivity
import piuk.blockchain.android.ui.locks.LocksInfoBottomSheet
import piuk.blockchain.android.ui.transactionflow.engine.TransactionErrorState
import piuk.blockchain.android.ui.transactionflow.engine.TransactionIntent
import piuk.blockchain.android.ui.transactionflow.engine.TransactionState
import piuk.blockchain.android.ui.transactionflow.flow.customisations.EnterAmountCustomisations
import piuk.blockchain.android.ui.transactionflow.flow.customisations.InfoActionType
import piuk.blockchain.android.ui.transactionflow.flow.customisations.InfoBottomSheetType
import piuk.blockchain.android.ui.transactionflow.flow.customisations.TransactionFlowBottomSheetInfo
import piuk.blockchain.android.ui.transactionflow.flow.customisations.TransactionFlowInfoBottomSheetCustomiser
import piuk.blockchain.android.ui.transactionflow.flow.sheets.TransactionFlowInfoBottomSheet
import piuk.blockchain.android.ui.transactionflow.flow.sheets.TransactionFlowInfoHost
import piuk.blockchain.android.ui.transactionflow.flow.sheets.TxFeeExplanationBottomSheet
import piuk.blockchain.android.ui.transactionflow.plugin.AvailableBalanceView
import piuk.blockchain.android.ui.transactionflow.plugin.BalanceAndFeeView
import piuk.blockchain.android.ui.transactionflow.plugin.ExpandableTxFlowWidget
import piuk.blockchain.android.ui.transactionflow.plugin.TxFlowWidget
import timber.log.Timber

class EnterAmountFragment :
    TransactionFlowFragment<FragmentTxFlowEnterAmountBinding>(),
    TransactionFlowInfoHost,
    KycUpgradeNowSheet.Host {

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentTxFlowEnterAmountBinding =
        FragmentTxFlowEnterAmountBinding.inflate(inflater, container, false)

    private val customiser: EnterAmountCustomisations by inject()
    private val bottomSheetInfoCustomiser: TransactionFlowInfoBottomSheetCustomiser by inject()

    private val fraudService: FraudService by inject()
    private var selectedFraudFlow: FraudFlow? = null

    private val compositeDisposable = CompositeDisposable()
    private var state: TransactionState = TransactionState()

    private var lowerSlot: TxFlowWidget? = null
    private var upperSlot: TxFlowWidget? = null
    private var upperSecondSlot: TxFlowWidget? = null

    private var infoActionCallback: () -> Unit = {}

    private var initialValueSet: Boolean = false
    private val assetAction: AssetAction? by lazy {
        enumValueOfOrNull<AssetAction>(arguments?.getString(ACTION).orEmpty())
    }

    private val errorContainer by lazy {
        binding.errorLayout.errorContainer
    }

    private val inputCurrency: Currency
        get() = binding.amountSheetInput.configuration.inputCurrency

    private val imm: InputMethodManager by lazy {
        requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        analyticsHooks.onViewAmountScreen(assetAction)

        binding.amountSheetCtaButton.apply {
            buttonState = ButtonState.Disabled
            text = getString(com.blockchain.stringResources.R.string.common_preview)
        }

        compositeDisposable += binding.amountSheetInput.amount
            .debounce(AMOUNT_DEBOUNCE_TIME_MS, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .subscribe { amount ->
                state.fiatRate?.let { rate ->
                    check(state.pendingTx != null) { "Px is not initialised yet" }
                    model.process(
                        TransactionIntent.AmountChanged(
                            when {
                                !state.allowFiatInput && amount is FiatValue -> {
                                    amount.convertFiatToCrypto(rate, state).also {
                                        binding.amountSheetInput.updateExchangeAmount(it)
                                    }
                                }
                                amount is FiatValue &&
                                    state.amount is FiatValue &&
                                    amount.currencyCode != state.amount.currencyCode -> {
                                    rate.inverse().convert(amount)
                                }
                                else -> {
                                    amount
                                }
                            }
                        )
                    )
                }
            }

        compositeDisposable += binding.amountSheetInput
            .onImeAction
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .subscribe {
                when (it) {
                    PrefixedOrSuffixedEditText.ImeOptions.NEXT -> {
                        if (state.nextEnabled) {
                            onCtaClick(state)
                        }
                    }
                    PrefixedOrSuffixedEditText.ImeOptions.BACK -> {
                        hideKeyboard()
                    }
                    else -> {
                        // do nothing
                    }
                }
            }

        compositeDisposable += binding.amountSheetInput.onInputToggle
            .subscribe {
                analyticsHooks.onCryptoToggle(it, state)
                model.process(TransactionIntent.DisplayModeChanged(it.type))
            }
    }

    @SuppressLint("SetTextI18n")
    override fun render(newState: TransactionState) {
        Timber.d("!TRANSACTION!> Rendering! EnterAmountFragment")
        customiser.getFraudFlowForTransaction(state)?.let { selectedFraudFlow = it }
        trackFraudFlow()

        if (newState.action.requiresDisplayLocks()) {
            model.process(TransactionIntent.LoadFundsLocked)
        }

        with(binding) {
            enterAmountLoading.visibleIf { newState.currencyType == null }

            amountSheetCtaButton.apply {
                buttonState = if (newState.nextEnabled) {
                    ButtonState.Enabled
                } else {
                    ButtonState.Disabled
                }
                onClick = { onCtaClick(newState) }
                text = customiser.enterAmountCtaText(newState)
            }

            if (!amountSheetInput.configured) {
                newState.pendingTx?.let { pTx ->
                    val startingCurrencyType = customiser.defInputType(newState, pTx.selectedFiat)
                    amountSheetInput.configure(newState, startingCurrencyType)
                    model.process(TransactionIntent.DisplayModeChanged(startingCurrencyType.type))
                }
            }

            val availableBalance = newState.availableBalance
            if (availableBalance.isPositive || availableBalance.isZero) {
                // The maxLimit set here controls the number of digits that can be entered,
                // but doesn't restrict the input to be always under that value. Which might be
                // strange UX, but is currently by design.
                if (amountSheetInput.configured) {
                    if (customiser.shouldShowMaxLimit(newState) && newState.sendingAccount !is NullCryptoAccount) {
                        amountSheetInput.maxLimit = newState.availableBalance
                    }
                    newState.fiatRate?.takeIf { it != amountSheetInput.customInternalExchangeRate }?.let {
                        amountSheetInput.customInternalExchangeRate = it
                    }
                }

                if (newState.setMax) {
                    amountSheetInput.updateValue(newState.maxSpendable)
                    model.process(TransactionIntent.ResetUseMaxSpendable)
                } else if (!initialValueSet) {
                    newState.initialAmountToSet()?.let {
                        amountSheetInput.updateValue(it)
                        initialValueSet = true
                    }
                }

                initialiseLowerSlotIfNeeded(newState)
                initialiseUpperSlotIfNeeded(newState)
                initialiseUpperSecondSlotIfNeeded(newState)

                newState.locks?.let { fundLocks ->
                    displayFundsLockInfo(
                        fundsLocks = fundLocks,
                        state = newState
                    )
                }

                lowerSlot?.update(newState)
                upperSlot?.update(newState)
                upperSecondSlot?.update(newState)

                (frameUpperSecondSlot.getChildAt(0) as? AvailableBalanceView)?.onClick {
                    newState.pendingTx?.let { pTx ->
                        showBottomSheet(
                            TxFeeExplanationBottomSheet.newInstance(
                                title = customiser.getFeeSheetTitle(newState),
                                displayTicker = newState.maxSpendable.currencyCode,
                                availableLabel = customiser.getFeeSheetAvailableLabel(newState),
                                totalBalance = state.convertBalanceToFiat(pTx.totalBalance, state.fiatRate),
                                estimatedFee = state.convertBalanceToFiat(pTx.feeAmount, state.fiatRate),
                                availableBalance = state.convertBalanceToFiat(pTx.availableBalance, state.fiatRate),
                                isTransactionFree = pTx.feeSelection.selectedLevel == FeeLevel.None
                            )
                        )
                    }
                }

                updateInputStateUI(newState)
                showCtaOrError(newState)
            }

            newState.amountsToPrefill?.let { amounts ->
                with(binding.amountSheetInput) {
                    if (newState.currencyType == CurrencyType.FIAT) {
                        updateValue(amounts.fiatValue)
                        updateExchangeAmount(amounts.cryptoValue)
                    } else {
                        updateValue(amounts.cryptoValue)
                        updateExchangeAmount(amounts.fiatValue)
                    }

                    model.process(TransactionIntent.ResetPrefillAmount)
                }
            }

            newState.pendingTx?.let { pTx ->
                val transactionsLimit = pTx.transactionsLimit
                if (transactionsLimit is TransactionsLimit.Limited) {
                    amountSheetInput.showInfo(
                        getString(
                            com.blockchain.stringResources.R.string.tx_enter_amount_orders_limit_info,
                            transactionsLimit.maxTransactionsLeft
                        )
                    ) {
                        val info = bottomSheetInfoCustomiser.info(
                            InfoBottomSheetType.TRANSACTIONS_LIMIT,
                            newState,
                            binding.amountSheetInput.configuration.inputCurrency.type
                        )
                        if (info != null) {
                            showBottomSheet(TransactionFlowInfoBottomSheet.newInstance(info))
                            infoActionCallback = handlePossibleInfoAction(info, newState)
                        }
                    }
                } else {
                    amountSheetInput.hideInfo()
                }

                if (pTx.feeSelection.selectedLevel == FeeLevel.None) {
                    frameLowerSlot.setOnClickListener(null)
                } else {
                    if (pTx.feeSelection.availableLevels.size > 1 &&
                        frameLowerSlot.getChildAt(0) is BalanceAndFeeView
                    ) {
                        root.setOnClickListener {
                            showBottomSheet(FeeSelectionBottomSheet.newInstance())
                        }
                    }
                }
            }
        }
        state = newState
    }

    override fun startKycClicked() {
        startKyc()
    }

    private fun updateInputStateUI(newState: TransactionState) {
        binding.amountSheetInput.onAmountValidationUpdated(newState.isAmountValid())
    }

    private fun showCtaOrError(state: TransactionState) {
        val errorState = state.errorState
        val isAmountPositive = state.amount.isPositive

        when {
            state.pendingTx?.isLowOnBalance() == true && customiser.shouldDisplayFeesErrorMessage(state) -> {
                showError(state, customiser.issueFeesTooHighMessage(state))
            }
            errorState == TransactionErrorState.NONE -> {
                showCta()
            }
            errorState.isAmountRelated() -> {
                if (isAmountPositive) {
                    showError(state, customiser.issueFlashMessage(state, inputCurrency.type))
                } else {
                    showCta()
                }
            }
            !errorState.isAmountRelated() -> {
                showError(state, customiser.issueFlashMessage(state, inputCurrency.type))
            }
        }
    }

    private fun showCta() {
        errorContainer.gone()
        binding.amountSheetCtaButton.visible()
    }

    private fun showError(state: TransactionState, message: String?) {
        message?.let {
            binding.amountSheetCtaButton.gone()
            binding.errorLayout.errorMessage.text = it
            errorContainer.visible()

            val infoType = when (state.errorState) {
                TransactionErrorState.INSUFFICIENT_FUNDS -> InfoBottomSheetType.INSUFFICIENT_FUNDS
                TransactionErrorState.BELOW_MIN_PAYMENT_METHOD_LIMIT,
                TransactionErrorState.BELOW_MIN_LIMIT -> InfoBottomSheetType.BELOW_MIN_LIMIT
                // we need to keep those for working with the feature flag off, otherwise we would be based only on the
                // suggested upgrade
                TransactionErrorState.OVER_GOLD_TIER_LIMIT,
                TransactionErrorState.OVER_SILVER_TIER_LIMIT -> InfoBottomSheetType.OVER_MAX_LIMIT
                TransactionErrorState.ABOVE_MAX_PAYMENT_METHOD_LIMIT ->
                    InfoBottomSheetType.ABOVE_MAX_PAYMENT_METHOD_LIMIT
                else -> null
            }

            val bottomSheetInfo = infoType?.let { type ->
                bottomSheetInfoCustomiser.info(
                    type,
                    state,
                    binding.amountSheetInput.configuration.inputCurrency.type
                )
            }
            bottomSheetInfo?.let { info ->
                errorContainer.setOnClickListener {
                    showBottomSheet(TransactionFlowInfoBottomSheet.newInstance(info))
                    infoActionCallback = handlePossibleInfoAction(info, state)
                }
            } ?: errorContainer.setOnClickListener {}
        }
    }

    private fun handlePossibleInfoAction(
        info: TransactionFlowBottomSheetInfo,
        state: TransactionState
    ): () -> Unit {
        analyticsHooks.onInfoBottomSheetActionClicked(info, state)
        info.action?.actionType?.let { type ->
            when (type) {
                InfoActionType.BUY -> {
                    val asset = (state.sendingAccount as? CryptoAccount)?.currency
                    require(asset != null)
                    return { startBuyForCurrency(asset) }
                }
                InfoActionType.KYC_UPGRADE -> return {
                    showBottomSheet(
                        KycUpgradeNowSheet.newInstance()
                    )
                }
            }
        } ?: return { }
    }

    private fun startKyc() {
        KycNavHostActivity.start(requireActivity(), CampaignType.None)
    }

    private fun startBuyForCurrency(asset: AssetInfo) {
        startActivity(
            SimpleBuyActivity.newIntent(
                context = requireActivity(),
                launchFromNavigationBar = true,
                asset = asset
            )
        )
    }

    private fun FragmentTxFlowEnterAmountBinding.initialiseUpperSlotIfNeeded(state: TransactionState) {
        if (upperSlot == null) {
            upperSlot = customiser.installEnterAmountUpperSlotView(
                requireContext(),
                frameUpperSlot,
                state
            ).apply {
                initControl(model, customiser, analyticsHooks)
            }
            (lowerSlot as? ExpandableTxFlowWidget)?.let {
                it.expanded.observeOn(AndroidSchedulers.mainThread()).subscribe {
                    configureCtaButton()
                }
            }
        }
    }

    private fun FragmentTxFlowEnterAmountBinding.initialiseUpperSecondSlotIfNeeded(state: TransactionState) {
        if (upperSecondSlot == null) {
            upperSecondSlot = customiser.installEnterAmountUpperSecondSlotView(
                requireContext(),
                frameUpperSecondSlot,
                state
            )?.apply {
                initControl(model, customiser, analyticsHooks)
            }
        }
    }

    private fun FragmentTxFlowEnterAmountBinding.displayFundsLockInfo(
        fundsLocks: FundsLocks,
        state: TransactionState
    ) {
        if (fundsLocks.locks.isNotEmpty() && state.action.requiresDisplayLocks()) {
            val available = state.convertBalanceToFiat(
                state.availableBalance,
                state.fiatRate
            )
            onHoldCell.apply {
                totalAmountLocked.text = fundsLocks.onHoldTotalAmount.toStringWithSymbol()
                root.visible()
                root.setOnClickListener { onExtraAccountInfoClicked(state.action, fundsLocks, available) }
            }
            onHoldCellSeparator.visible()
        }
    }

    private fun AssetAction.requiresDisplayLocks(): Boolean =
        this == AssetAction.FiatWithdraw || this == AssetAction.Send

    private fun onExtraAccountInfoClicked(action: AssetAction, locks: FundsLocks, availableBalance: Money) {
        val origin = if (action == AssetAction.Send) {
            LocksInfoBottomSheet.OriginScreenLocks.ENTER_AMOUNT_SEND_SCREEN
        } else LocksInfoBottomSheet.OriginScreenLocks.ENTER_AMOUNT_WITHDRAW_SCREEN

        showBottomSheet(
            LocksInfoBottomSheet.newInstance(
                originScreen = origin,
                available = availableBalance.toStringWithSymbol(),
                fundsLocks = locks
            )
        )
    }

    private fun configureCtaButton() {
        val layoutParams: ViewGroup.MarginLayoutParams =
            binding.amountSheetCtaButton.layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.bottomMargin = resources.getDimension(com.blockchain.componentlib.R.dimen.standard_spacing).toInt()
        binding.amountSheetCtaButton.layoutParams = layoutParams
    }

    private fun FragmentTxFlowEnterAmountBinding.initialiseLowerSlotIfNeeded(newState: TransactionState) {
        if (lowerSlot == null) {
            lowerSlot = customiser.installEnterAmountLowerSlotView(
                requireContext(),
                frameLowerSlot,
                newState
            ).apply {
                initControl(model, customiser, analyticsHooks)
            }
        }
    }

    private fun onCtaClick(state: TransactionState) {
        trackFraudFlow()

        hideKeyboard()
        model.process(TransactionIntent.PrepareTransaction)
        analyticsHooks.onEnterAmountCtaClick(state)
    }

    private fun trackFraudFlow() {
        selectedFraudFlow?.let {
            fraudService.trackFlow(it)
        }
    }

    private fun hideKeyboard() {
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }

    private fun FiatCryptoInputView.configure(
        state: TransactionState,
        inputCurrency: Currency
    ) {
        val selectedFiat = state.pendingTx?.selectedFiat ?: return
        // Input currency is configured as crypto or we are coming back from the checkout screen
        when {
            inputCurrency.type ==
                CurrencyType.CRYPTO || state.amount.takeIf { it is CryptoValue }?.isPositive == true -> {
                configuration = FiatCryptoViewConfiguration(
                    inputCurrency = state.sendingAsset,
                    exchangeCurrency = selectedFiat,
                    predefinedAmount = state.amount
                )
            }
            // both input and selected fiat are fiats (Deposit and withdraw fiat from/to external)
            inputCurrency.type == CurrencyType.FIAT && inputCurrency != selectedFiat -> {
                configuration = FiatCryptoViewConfiguration(
                    inputCurrency = inputCurrency,
                    outputCurrency = inputCurrency,
                    exchangeCurrency = selectedFiat,
                    predefinedAmount = state.amount
                )
            }
            else -> {
                val fiatRate = state.fiatRate ?: return
                val isCryptoWithFiatExchange =
                    state.amount is CryptoValue &&
                        (
                            fiatRate.from.type == CurrencyType.CRYPTO &&
                                fiatRate.to.type == CurrencyType.FIAT
                            )
                configuration = FiatCryptoViewConfiguration(
                    inputCurrency = selectedFiat,
                    outputCurrency = selectedFiat,
                    exchangeCurrency = (state.sendingAccount as SingleAccount).currency,
                    predefinedAmount = if (isCryptoWithFiatExchange) {
                        fiatRate.convert(state.amount)
                    } else {
                        state.amount
                    }
                )
            }
        }
        showKeyboard()
    }

    private fun showKeyboard() {
        val inputView = binding.amountSheetInput.findViewById<PrefixedOrSuffixedEditText>(
            R.id.enter_amount
        )

        inputView?.run {
            requestFocus()
            imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    companion object {
        private const val AMOUNT_DEBOUNCE_TIME_MS = 300L
        private const val ACTION = "ASSET_ACTION"

        fun newInstance(assetAction: AssetAction): EnterAmountFragment =
            EnterAmountFragment().apply {
                arguments = Bundle().apply {
                    putString(ACTION, assetAction.name)
                }
            }
    }

    override fun onActionInfoTriggered() {
        infoActionCallback()
    }

    override fun onSheetClosed() {
        // do nothing
    }

    override fun onSheetClosed(sheet: BottomSheetDialogFragment) {
        super<TransactionFlowInfoHost>.onSheetClosed(sheet)
        if (sheet is TransactionFlowInfoBottomSheet) {
            analyticsHooks.onInfoBottomSheetDismissed(sheet.info, state)
        }
    }
}

private fun TransactionErrorState.isAmountRelated(): Boolean =
    when (this) {
        TransactionErrorState.NONE,
        TransactionErrorState.INVALID_ADDRESS,
        TransactionErrorState.INVALID_DOMAIN,
        TransactionErrorState.ADDRESS_IS_CONTRACT,
        TransactionErrorState.PENDING_ORDERS_LIMIT_REACHED,
        TransactionErrorState.TRANSACTION_IN_FLIGHT,
        TransactionErrorState.INVALID_PASSWORD,
        TransactionErrorState.TX_OPTION_INVALID -> false

        TransactionErrorState.INSUFFICIENT_FUNDS,
        TransactionErrorState.INVALID_AMOUNT,
        TransactionErrorState.BELOW_MIN_LIMIT,
        TransactionErrorState.OVER_SILVER_TIER_LIMIT,
        TransactionErrorState.OVER_GOLD_TIER_LIMIT,
        TransactionErrorState.NOT_ENOUGH_GAS,
        TransactionErrorState.BELOW_MIN_PAYMENT_METHOD_LIMIT,
        TransactionErrorState.ABOVE_MAX_PAYMENT_METHOD_LIMIT -> true
    }

private fun TransactionState.isAmountValid(): Boolean {
    if (amount.isZero) return true
    return !errorState.isAmountRelated()
}

private fun PendingTx.isLowOnBalance() =
    feeSelection.selectedLevel != FeeLevel.None &&
        availableBalance.isZero && totalBalance.isPositive

// in this method we try to convert the fiat value coming out from
// the view to a crypto which is withing the min and max limits allowed.
// We use floor rounding for max and ceiling for min just to make sure that we wont have problem with rounding once
// the amount reach the engine where the comparison with limits will happen.
fun Money.convertFiatToCrypto(
    rate: ExchangeRate,
    state: TransactionState
): Money {
    val min = state.pendingTx?.limits?.minAmount ?: return rate.inverse().convert(this)
    val max = state.maxSpendable
    val roundedUpAmount = rate.inverse(RoundingMode.CEILING, CryptoValue.DISPLAY_DP)
        .convert(this)
    val roundedDownAmount = rate.inverse(RoundingMode.FLOOR, CryptoValue.DISPLAY_DP)
        .convert(this)
    return if (roundedUpAmount in min..max) {
        roundedUpAmount
    } else {
        roundedDownAmount
    }
}
