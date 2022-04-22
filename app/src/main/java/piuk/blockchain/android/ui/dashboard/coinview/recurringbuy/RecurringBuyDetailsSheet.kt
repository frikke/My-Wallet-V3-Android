package piuk.blockchain.android.ui.dashboard.coinview.recurringbuy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockchain.commonarch.presentation.base.SlidingModalBottomDialog
import com.blockchain.commonarch.presentation.mvi.MviBottomSheet
import com.blockchain.componentlib.alert.BlockchainSnackbar
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.koin.scopedInject
import com.blockchain.nabu.datamanagers.PaymentMethod
import com.blockchain.nabu.datamanagers.custodialwalletimpl.PaymentMethodType
import com.blockchain.nabu.models.data.RecurringBuy
import com.blockchain.nabu.models.data.RecurringBuyState
import com.blockchain.utils.toFormattedDateWithoutYear
import info.blockchain.balance.AssetInfo
import java.time.ZoneId
import java.time.ZonedDateTime
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.DialogSheetRecurringBuyInfoBinding
import piuk.blockchain.android.simplebuy.CheckoutAdapterDelegate
import piuk.blockchain.android.simplebuy.SimpleBuyCheckoutItem
import piuk.blockchain.android.simplebuy.toHumanReadableRecurringBuy
import piuk.blockchain.android.simplebuy.toHumanReadableRecurringDate
import piuk.blockchain.android.ui.customviews.BlockchainListDividerDecor
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy

class RecurringBuyDetailsSheet : MviBottomSheet<RecurringBuyModel,
    RecurringBuyIntent, RecurringBuyModelState, DialogSheetRecurringBuyInfoBinding>() {

    interface Host : SlidingModalBottomDialog.Host {
        fun onRecurringBuyDeleted(asset: AssetInfo)
    }

    override val host: Host by lazy {
        super.host as? Host ?: throw IllegalStateException("Host fragment is not a RecurringBuyDetailsSheet.Host")
    }

    private val listAdapter: CheckoutAdapterDelegate by lazy {
        CheckoutAdapterDelegate()
    }

    private val recurringBuy: RecurringBuy? by unsafeLazy {
        arguments?.getSerializable(RECURRING_BUY) as? RecurringBuy
    }

    override val model: RecurringBuyModel by scopedInject()

    lateinit var cacheState: RecurringBuyModelState

    override fun initControls(binding: DialogSheetRecurringBuyInfoBinding) {
        with(binding) {
            with(rbSheetItems) {
                adapter = listAdapter
                layoutManager = LinearLayoutManager(requireContext())
                addItemDecoration(BlockchainListDividerDecor(requireContext()))
            }

            rbSheetBack.setOnClickListener {
                dismiss()
            }
            rbSheetCancel.setOnClickListener {
                // TODO stopgap check while design make their mind up
                AlertDialog.Builder(requireContext())
                    .setTitle(R.string.settings_bank_remove_check_title)
                    .setMessage(R.string.recurring_buy_cancel_dialog_desc)
                    .setPositiveButton(R.string.common_ok) { di, _ ->
                        di.dismiss()
                        model.process(RecurringBuyIntent.DeleteRecurringBuy)
                    }
                    .setNegativeButton(R.string.common_cancel) { di, _ ->
                        di.dismiss()
                    }.show()
            }
        }

        recurringBuy?.let {
            model.process(RecurringBuyIntent.UpdateRecurringBuy(it))
        }
    }

    override fun render(newState: RecurringBuyModelState) {
        cacheState = newState
        if (newState.recurringBuy?.paymentDetails == null) {
            model.process(RecurringBuyIntent.GetPaymentDetails)
            return
        }
        newState.recurringBuy.let {
            when {
                it.state == RecurringBuyState.INACTIVE -> {
                    BlockchainSnackbar.make(
                        binding.root,
                        getString(R.string.recurring_buy_cancelled_toast),
                        type = SnackbarType.Success
                    ).show()
                    host.onRecurringBuyDeleted(it.asset)
                    dismiss()
                }
                newState.error == RecurringBuyError.RecurringBuyDelete -> {
                    BlockchainSnackbar.make(
                        binding.root,
                        getString(R.string.recurring_buy_cancelled_error_toast),
                        type = SnackbarType.Error
                    ).show()
                }
                newState.error is RecurringBuyError.HttpError -> {
                    BlockchainSnackbar.make(
                        binding.root,
                        newState.error.errorMessage,
                        type = SnackbarType.Error
                    ).show()
                }
                else ->
                    with(binding) {
                        rbSheetTitle.text = getString(R.string.recurring_buy_sheet_title_1)
                        rbSheetHeader.setDetails(
                            getString(
                                R.string.recurring_buy_header,
                                it.amount.toStringWithSymbol(),
                                it.asset.displayTicker
                            ),
                            ""
                        )
                        it.renderListItems()
                    }
            }
        }
    }

    private fun RecurringBuy.renderListItems() {
        listAdapter.items = listOf(
            if (paymentMethodType == PaymentMethodType.FUNDS) {
                SimpleBuyCheckoutItem.SimpleCheckoutItem(
                    getString(R.string.payment_method),
                    getString(R.string.recurring_buy_funds_label, amount.currencyCode)
                )
            } else {
                if (paymentMethodType == PaymentMethodType.PAYMENT_CARD) {
                    val paymentDetails = (paymentDetails as PaymentMethod.Card)
                    SimpleBuyCheckoutItem.ComplexCheckoutItem(
                        getString(R.string.payment_method),
                        paymentDetails.uiLabel(),
                        paymentDetails.endDigits
                    )
                } else {
                    val paymentDetails = (paymentDetails as PaymentMethod.Bank)
                    SimpleBuyCheckoutItem.ComplexCheckoutItem(
                        getString(R.string.payment_method),
                        paymentDetails.bankName,
                        paymentDetails.accountEnding
                    )
                }
            },
            SimpleBuyCheckoutItem.ComplexCheckoutItem(
                getString(R.string.recurring_buy_frequency_label_1),
                recurringBuyFrequency.toHumanReadableRecurringBuy(requireContext()),
                recurringBuyFrequency.toHumanReadableRecurringDate(
                    requireContext(),
                    ZonedDateTime.ofInstant(nextPaymentDate.toInstant(), ZoneId.systemDefault())
                )
            ),
            SimpleBuyCheckoutItem.SimpleCheckoutItem(
                getString(R.string.recurring_buy_info_purchase_label_1),
                nextPaymentDate.toFormattedDateWithoutYear()
            ),
            SimpleBuyCheckoutItem.SimpleCheckoutItem(
                getString(R.string.common_total),
                amount.toStringWithSymbol(),
                true
            )
        )
        listAdapter.notifyDataSetChanged()
    }

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): DialogSheetRecurringBuyInfoBinding =
        DialogSheetRecurringBuyInfoBinding.inflate(inflater, container, false)

    companion object {
        private const val RECURRING_BUY = "RECURRING_BUY"
        fun newInstance(recurringBuy: RecurringBuy): RecurringBuyDetailsSheet = RecurringBuyDetailsSheet().apply {
            arguments = Bundle().apply {
                putSerializable(RECURRING_BUY, recurringBuy)
            }
        }
    }
}
