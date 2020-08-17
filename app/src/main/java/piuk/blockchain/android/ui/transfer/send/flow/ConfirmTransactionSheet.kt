package piuk.blockchain.android.ui.transfer.send.flow

import android.graphics.Typeface.BOLD
import android.net.Uri
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.view.View
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockchain.koin.scopedInject
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.ui.urllinks.INTEREST_PRIVACY_POLICY
import com.blockchain.ui.urllinks.INTEREST_TERMS_OF_SERVICE
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.ExchangeRates
import kotlinx.android.synthetic.main.dialog_send_confirm.view.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.FeeLevel
import piuk.blockchain.android.coincore.TxOption
import piuk.blockchain.android.coincore.TxOptionValue
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.android.ui.transfer.send.FlowInputSheet
import piuk.blockchain.android.ui.transfer.send.SendIntent
import piuk.blockchain.android.ui.transfer.send.SendState
import piuk.blockchain.android.ui.transfer.send.SendStep
import piuk.blockchain.android.ui.transfer.send.flow.adapter.ConfirmAgreementTextItem
import piuk.blockchain.android.ui.transfer.send.flow.adapter.ConfirmAgreementWithLinksItem
import piuk.blockchain.android.ui.transfer.send.flow.adapter.ConfirmInfoItem
import piuk.blockchain.android.ui.transfer.send.flow.adapter.ConfirmItemType
import piuk.blockchain.android.ui.transfer.send.flow.adapter.ConfirmNoteItem
import piuk.blockchain.android.ui.transfer.send.flow.adapter.ConfirmTransactionDelegateAdapter
import piuk.blockchain.android.util.StringUtils
import timber.log.Timber

class ConfirmTransactionSheet(
    host: SlidingModalBottomDialog.Host
) : FlowInputSheet(host) {
    override val layoutResource: Int = R.layout.dialog_send_confirm

    private val stringUtils: StringUtils by inject()
    private val exchangeRates: ExchangeRates by scopedInject()
    private val prefs: CurrencyPrefs by scopedInject()

    private var state = SendState()

    private val listAdapter: ConfirmTransactionDelegateAdapter by lazy {
        ConfirmTransactionDelegateAdapter(
            onAgreementWithLinksActionClicked = { isChecked -> updateLinkAgreement(isChecked) },
            onAgreementTextActionClicked = { isChecked -> updateTextAgreement(isChecked) },
            onNoteItemUpdated = { note ->
                updateNoteOption(note)
            },
            stringUtils = stringUtils,
            activityContext = requireActivity()
        )
    }

    override fun render(newState: SendState) {
        Timber.d("!SEND!> Rendering! ConfirmTransactionSheet")
        require(newState.currentStep == SendStep.CONFIRM_DETAIL)

        // We _should_ always have a pending Tx when we get here
        require(newState.pendingTx != null)

        val itemList = mutableListOf<ConfirmItemType>(
            ConfirmInfoItem(getString(R.string.common_from), newState.sendingAccount.label),
            ConfirmInfoItem(getString(R.string.common_to), newState.sendTarget.label)
        )

        getFeeItem(newState)?.let {
            itemList.add(it)
        }

        itemList.add(ConfirmInfoItem(getString(R.string.common_total), getTotalAmount(newState)))

        updateOptions(newState, itemList)

        showActionSpecificUi(newState, itemList)

        listAdapter.items = itemList
        listAdapter.notifyDataSetChanged()

        dialogView.confirm_cta_button.isEnabled = newState.nextEnabled

        state = newState
    }

    override fun initControls(view: View) {
        view.confirm_cta_button.setOnClickListener { onCtaClick() }

        with(view.confirm_details_list) {
            addItemDecoration(
                DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
            )

            layoutManager = LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.VERTICAL,
                false
            )
            adapter = listAdapter
        }

        view.confirm_sheet_back.setOnClickListener {
            model.process(SendIntent.ReturnToPreviousStep)
        }
    }

    private fun getTotalAmount(newState: SendState): String {
        val fee = newState.feeAmount
        val amount = newState.sendAmount

        return if (amount.symbol == fee.symbol) {
            (amount + fee).toStringWithSymbol()
        } else {
            "${amount.toStringWithSymbol()} (${fee.toStringWithSymbol()})"
        }
    }

    private fun updateOptions(
        state: SendState,
        itemList: MutableList<ConfirmItemType>
    ) {
        val note = state.pendingTx?.getOption<TxOptionValue.TxTextOption>(TxOption.DESCRIPTION)
        note?.let { opt ->
            itemList.add(ConfirmNoteItem(opt.text))
        }

        val linkAgreement =
            state.pendingTx?.getOption<TxOptionValue.TxBooleanOption>(TxOption.AGREEMENT_WITH_LINKS)
        linkAgreement?.let {
            setupTosAndPPLinks(itemList)
        }

        val textAgreement =
            state.pendingTx?.getOption<TxOptionValue.TxBooleanOption>(TxOption.TEXT_AGREEMENT)
        textAgreement?.let {
            setupHoldingValues(state.sendAmount, itemList)
        }
    }

    // TODO use AssetActions differently
    private fun showActionSpecificUi(
        state: SendState,
        itemList: MutableList<ConfirmItemType>
    ) {
        when (state.action) {
            AssetAction.ViewActivity -> TODO()
            AssetAction.Send -> showSendUi(state, itemList)
            AssetAction.NewSend -> showSendUi(state, itemList)
            AssetAction.Receive -> TODO()
            AssetAction.Swap -> TODO()
            AssetAction.Summary -> TODO()
            AssetAction.Deposit -> showDepositUi(state, itemList)
        }
    }

    private fun showDepositUi(
        state: SendState,
        itemList: MutableList<ConfirmItemType>
    ) {
        dialogView.confirm_cta_button.text =
            getString(R.string.send_confirmation_deposit_cta_button)
        itemList.add(0, ConfirmInfoItem(getString(R.string.common_deposit),
            state.sendAmount.toStringWithSymbol()))
    }

    private fun showSendUi(
        state: SendState,
        itemList: MutableList<ConfirmItemType>
    ) {
        dialogView.confirm_cta_button.text = getString(R.string.send_confirmation_cta_button,
            getTotalAmount(state))
        itemList.add(0,
            ConfirmInfoItem(getString(R.string.common_send), state.sendAmount.toStringWithSymbol()))
    }

    private fun getFeeItem(state: SendState): ConfirmInfoItem? {
        state.pendingTx?.let {
            if (it.feeLevel != FeeLevel.None) {
                val feeTitle = getString(
                    R.string.common_spaced_strings,
                    getString(R.string.send_confirmation_fee),
                    getString(R.string.send_confirmation_regular_estimation)
                )
                return ConfirmInfoItem(feeTitle, state.feeAmount.toStringWithSymbol())
            }
        }
        return null
    }

    private fun setupTosAndPPLinks(itemList: MutableList<ConfirmItemType>) {
        val linksMap = mapOf<String, Uri>(
            "interest_tos" to Uri.parse(INTEREST_TERMS_OF_SERVICE),
            "interest_pp" to Uri.parse(INTEREST_PRIVACY_POLICY)
        )

        itemList.add(
            ConfirmAgreementWithLinksItem(linksMap, R.string.send_confirmation_interest_tos_pp))
    }

    private fun setupHoldingValues(
        sendAmount: CryptoValue,
        itemList: MutableList<ConfirmItemType>
    ) {
        val introToHolding = getString(R.string.send_confirmation_interest_holding_period_1)
        val amountInBold =
            sendAmount.toFiat(exchangeRates, prefs.selectedFiatCurrency).toStringWithSymbol()
        val outroToHolding = getString(R.string.send_confirmation_interest_holding_period_2,
            sendAmount.toStringWithSymbol())
        val sb = SpannableStringBuilder()
        sb.append(introToHolding)
        sb.append(amountInBold)
        sb.setSpan(StyleSpan(BOLD), introToHolding.length,
            introToHolding.length + amountInBold.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        sb.append(outroToHolding)

        itemList.add(ConfirmAgreementTextItem(sb))
    }

    private fun updateLinkAgreement(isChecked: Boolean) {
        state.pendingTx?.getOption<TxOptionValue.TxBooleanOption>(TxOption.AGREEMENT_WITH_LINKS)?.let {
            model.process(SendIntent.ModifyTxOption(it.copy(value = isChecked)))
        }
    }

    private fun updateTextAgreement(isChecked: Boolean) {
        state.pendingTx?.getOption<TxOptionValue.TxBooleanOption>(TxOption.TEXT_AGREEMENT)?.let {
            model.process(SendIntent.ModifyTxOption(it.copy(value = isChecked)))
        }
    }

    private fun updateNoteOption(note: String) {
        state.pendingTx?.getOption<TxOptionValue.TxTextOption>(TxOption.DESCRIPTION)?.let {
            model.process(SendIntent.ModifyTxOption(it.copy(text = note)))
        }
    }

    private fun onCtaClick() {
        model.process(SendIntent.ExecuteTransaction)
    }
}
