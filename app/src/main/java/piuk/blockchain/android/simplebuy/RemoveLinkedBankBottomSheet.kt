package piuk.blockchain.android.simplebuy

import android.animation.LayoutTransition
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import com.blockchain.commonarch.presentation.base.SlidingModalBottomDialog
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.componentlib.viewextensions.visibleIf
import com.blockchain.core.payments.LinkedPaymentMethod
import com.blockchain.core.payments.PaymentsDataManager
import com.blockchain.koin.scopedInject
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.RemoveBankBottomSheetBinding
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.util.getResolvedColor
import piuk.blockchain.android.util.getResolvedDrawable
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy

class RemoveLinkedBankBottomSheet : SlidingModalBottomDialog<RemoveBankBottomSheetBinding>() {

    interface Host : SlidingModalBottomDialog.Host {
        fun onLinkedBankRemoved(bankId: String)
    }

    private val compositeDisposable = CompositeDisposable()
    private val paymentsDataManager: PaymentsDataManager by scopedInject()

    private val bank: LinkedPaymentMethod.Bank by unsafeLazy {
        arguments?.getSerializable(BANK_KEY) as LinkedPaymentMethod.Bank
    }

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): RemoveBankBottomSheetBinding =
        RemoveBankBottomSheetBinding.inflate(inflater, container, false)

    override fun initControls(binding: RemoveBankBottomSheetBinding) {
        // setting the transition like this prevents the bottom sheet from
        // jumping to the top of the screen when animating its contents
        val transition = LayoutTransition()
        transition.setAnimateParentHierarchy(false)
        binding.root.layoutTransition = transition

        with(binding) {
            title.text = resources.getString(R.string.common_spaced_strings, bank.name, bank.currency.displayTicker)
            endDigits.text = resources.getString(R.string.dotted_suffixed_string, bank.accountEnding)
            accountInfo.text = getString(R.string.payment_method_type_account_info, bank.toHumanReadableAccount(), "")
            rmvBankBtn.setOnClickListener {
                showConfirmation()
            }
            rmvBankCancel.setOnClickListener {
                dismiss()
            }
        }
    }

    private fun showConfirmation() {
        with(binding) {
            rmvBankCancel.visible()
            rmvBankBtn.setOnClickListener {
                removeBank()
            }

            val alertIcon = requireContext().getResolvedDrawable(R.drawable.ic_asset_error)
            alertIcon?.setTint(requireContext().getResolvedColor(R.color.orange_400))
            icon.setImageDrawable(alertIcon)

            endDigits.gone()
            title.text = getString(R.string.settings_bank_remove_check_title)
            accountInfo.text = getString(R.string.settings_bank_remove_check_subtitle, bank.name)
        }
    }

    private fun removeBank() {
        compositeDisposable += paymentsDataManager.removeBank(bank)
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                updateUi(true)
            }
            .doFinally {
                updateUi(false)
            }
            .subscribeBy(
                onComplete = {
                    analytics.logEvent(SimpleBuyAnalytics.REMOVE_BANK)
                    (host as? Host)?.onLinkedBankRemoved(bank.id)
                    dismiss()
                }, onError = {
                ToastCustom.makeText(
                    requireContext(), getString(R.string.settings_bank_remove_error), Toast.LENGTH_LONG,
                    ToastCustom.TYPE_ERROR
                )
            }
            )
    }

    private fun updateUi(isLoading: Boolean) {
        with(binding) {
            progress.visibleIf { isLoading }
            icon.visibleIf { !isLoading }
            rmvBankBtn.isEnabled = !isLoading
            rmvBankCancel.isEnabled = !isLoading
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        compositeDisposable.dispose()
    }

    companion object {
        private const val BANK_KEY = "BANK_KEY"

        fun newInstance(bank: LinkedPaymentMethod.Bank) =
            RemoveLinkedBankBottomSheet().apply {
                arguments = Bundle().apply {
                    putSerializable(BANK_KEY, bank)
                }
            }
    }
}
