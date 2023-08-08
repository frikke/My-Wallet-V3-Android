package piuk.blockchain.android.ui.transactionflow.flow

import android.content.Context
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.RadioButton
import android.widget.TextView
import androidx.annotation.IdRes
import com.blockchain.coincore.FeeLevel
import com.blockchain.coincore.FeeSelection
import com.blockchain.coincore.FeeState
import com.blockchain.coincore.toUserFiat
import com.blockchain.commonarch.presentation.mvi.MviBottomSheet
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.componentlib.viewextensions.visibleIf
import com.blockchain.core.price.ExchangeRates
import org.koin.android.ext.android.inject
import org.koin.core.scope.Scope
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.DialogSheetFeeSelectionBinding
import piuk.blockchain.android.ui.transactionflow.analytics.TxFlowAnalytics
import piuk.blockchain.android.ui.transactionflow.engine.TransactionIntent
import piuk.blockchain.android.ui.transactionflow.engine.TransactionModel
import piuk.blockchain.android.ui.transactionflow.engine.TransactionState
import piuk.blockchain.android.util.AfterTextChangedWatcher

class FeeSelectionBottomSheet :
    MviBottomSheet<TransactionModel, TransactionIntent, TransactionState, DialogSheetFeeSelectionBinding>() {
    private val scope: Scope by lazy {
        (requireContext() as TransactionFlowActivity).scope
    }

    private val imm: InputMethodManager by lazy {
        requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    }

    override val model: TransactionModel
        get() = scope.get()

    private val exchangeRates: ExchangeRates by inject()
    private val txAnalytics: TxFlowAnalytics by inject()

    private var state: TransactionState = TransactionState()

    private lateinit var radioButtons: Set<RadioButton>

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): DialogSheetFeeSelectionBinding =
        DialogSheetFeeSelectionBinding.inflate(inflater, container, false)

    override fun initControls(binding: DialogSheetFeeSelectionBinding) {
        with(binding) {
            radioButtons = setOf(feeRegularRadio, feePriorityRadio, feeCustomRadio)
            feeCustomInput.addTextChangedListener(object : AfterTextChangedWatcher() {
                override fun afterTextChanged(s: Editable) {
                    val input = s.toString()
                    if (input.isNotEmpty()) {
                        sendFeeUpdate(model, FeeLevel.Custom, input.toLong())
                    } else {
                        binding.feeCustomError.text = ""
                    }
                }
            })

            regularFeeGroup.referencedIds.map {
                root.findViewById<View>(it).setOnClickListener {
                    selectRadioButton(feeRegularRadio.id)
                }
            }
            priorityFeeGroup.referencedIds.map {
                root.findViewById<View>(it).setOnClickListener {
                    selectRadioButton(feePriorityRadio.id)
                }
            }
            customFeeGroup.referencedIds.map {
                root.findViewById<View>(it).setOnClickListener {
                    selectRadioButton(feeCustomRadio.id)
                }
            }

            feeRegularTitle.text = getString(
                com.blockchain.stringResources.R.string.fee_options_label,
                getString(com.blockchain.stringResources.R.string.fee_options_regular),
                getString(com.blockchain.stringResources.R.string.fee_options_regular_time)
            )
            feePriorityTitle.text = getString(
                com.blockchain.stringResources.R.string.fee_options_label,
                getString(com.blockchain.stringResources.R.string.fee_options_priority),
                getString(com.blockchain.stringResources.R.string.fee_options_priority_time)
            )

            feeCustomContinue.apply {
                text = getString(com.blockchain.stringResources.R.string.btn_continue)
                onClick = {
                    hideKeyboard()
                    dismiss()
                }
            }
        }
    }

    override fun render(newState: TransactionState) {
        newState.pendingTx?.feeSelection?.let {
            check(it.selectedLevel != FeeLevel.None) { "Fee level None not supported" }

            selectRadioButton(it.selectedLevel.mapFeeLevelToRadioButton().id)
            with(binding) {
                it.feesForLevels.map { levels ->
                    when (levels.key) {
                        FeeLevel.Regular -> {
                            feeRegularCrypto.text = levels.value.toStringWithSymbol()
                            feeRegularFiat.text = levels.value.toUserFiat(exchangeRates).toStringWithSymbol()
                        }
                        FeeLevel.Priority -> {
                            feePriorityCrypto.text = levels.value.toStringWithSymbol()
                            feePriorityFiat.text = levels.value.toUserFiat(exchangeRates).toStringWithSymbol()
                        }
                        FeeLevel.None,
                        FeeLevel.Custom -> {
                            // do nothing
                        }
                    }
                }

                customFeeGroup.visibleIf { it.availableLevels.contains(FeeLevel.Custom) }
            }

            showFeeDetails(newState.pendingTx.feeSelection)
        }

        state = newState
    }

    private fun selectRadioButton(@IdRes radioId: Int) {
        val previousLevel = radioButtons.firstOrNull { it.isChecked }?.toFeeLevel()

        radioButtons.map {
            it.isChecked = it.id == radioId
        }

        val selectedLevel = radioButtons.first { it.isChecked }.toFeeLevel()
        if (radioId == binding.feeCustomRadio.id) {
            showCustomUI()
        } else {
            sendFeeUpdate(model, selectedLevel)
            hideCustomUi()
        }

        previousLevel?.let {
            txAnalytics.onFeeLevelChanged(oldLevel = it, newLevel = selectedLevel)
        }
    }

    private fun showCustomUI() {
        with(binding) {
            state.pendingTx?.feeSelection?.customLevelRates?.let {
                feeCustomBounds.text = getString(
                    com.blockchain.stringResources.R.string.fee_options_sat_byte_inline_hint,
                    it.regularFee.toString(),
                    it.priorityFee.toString()
                )
            }
            groupFeeCustomInput.visible()
            showKeyboard(feeCustomInput)
        }
    }

    private fun hideCustomUi() {
        with(binding) {
            groupFeeCustomInput.gone()
            hideKeyboard()
        }
    }

    private fun showFeeDetails(feeSelection: FeeSelection) {
        feeSelection.feeState?.let {
            when (it) {
                is FeeState.FeeUnderMinLimit -> {
                    setCustomFeeValues(
                        feeSelection.customAmount,
                        getString(com.blockchain.stringResources.R.string.fee_options_sat_byte_min_error),
                        feeSelection.feeState.toString()
                    )
                }
                is FeeState.FeeUnderRecommended -> {
                    setCustomFeeValues(
                        feeSelection.customAmount,
                        getString(com.blockchain.stringResources.R.string.fee_options_fee_too_low),
                        feeSelection.feeState.toString()
                    )
                }
                is FeeState.FeeOverRecommended -> {
                    setCustomFeeValues(
                        feeSelection.customAmount,
                        getString(com.blockchain.stringResources.R.string.fee_options_fee_too_high),
                        feeSelection.feeState.toString()
                    )
                }
                is FeeState.ValidCustomFee -> {
                    setCustomFeeValues(
                        customFee = feeSelection.customAmount,
                        feeSelection = feeSelection.feeState.toString()
                    )
                }
                is FeeState.FeeTooHigh -> {
                    setCustomFeeValues(
                        feeSelection.customAmount,
                        getString(com.blockchain.stringResources.R.string.send_confirmation_insufficient_funds_for_fee),
                        feeSelection.feeState.toString()
                    )
                }
                is FeeState.FeeDetails -> {
                    setCustomFeeValues(
                        customFee = feeSelection.customAmount,
                        feeSelection = feeSelection.feeState.toString()
                    )
                }
            }
        }
    }

    private fun setCustomFeeValues(customFee: Long, error: String = "", feeSelection: String) {
        with(binding) {
            if (customFee != -1L) {
                val fee = customFee.toString()
                feeCustomInput.setText(fee, TextView.BufferType.EDITABLE)
                feeCustomInput.setSelection(fee.length)
            } else {
                feeCustomInput.setText("", TextView.BufferType.EDITABLE)
            }
            feeCustomError.text = error
        }
    }

    private fun sendFeeUpdate(model: TransactionModel, level: FeeLevel, customFeeAmount: Long? = null) {
        model.process(
            TransactionIntent.SetFeeLevel(
                feeLevel = level,
                customFeeAmount = customFeeAmount
            )
        )
    }

    private fun showKeyboard(inputView: View) {
        inputView.requestFocus()
        imm.showSoftInput(inputView, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyboard() {
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }

    private fun RadioButton.toFeeLevel(): FeeLevel =
        when (this) {
            binding.feeRegularRadio -> FeeLevel.Regular
            binding.feePriorityRadio -> FeeLevel.Priority
            binding.feeCustomRadio -> FeeLevel.Custom
            else -> FeeLevel.None
        }

    private fun FeeLevel.mapFeeLevelToRadioButton(): RadioButton =
        when (this) {
            FeeLevel.Regular -> binding.feeRegularRadio
            FeeLevel.Priority -> binding.feePriorityRadio
            FeeLevel.Custom -> binding.feeCustomRadio
            else -> throw java.lang.IllegalStateException("Cannot map FeeLevel $this to UI element")
        }

    companion object {
        fun newInstance(): FeeSelectionBottomSheet = FeeSelectionBottomSheet()
    }
}
