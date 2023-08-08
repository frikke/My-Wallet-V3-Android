package piuk.blockchain.android.simplebuy.sheets

import android.animation.LayoutTransition
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockchain.commonarch.presentation.base.SlidingModalBottomDialog
import com.blockchain.componentlib.alert.BlockchainSnackbar
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.componentlib.viewextensions.visibleIf
import com.blockchain.domain.paymentmethods.BankService
import com.blockchain.domain.paymentmethods.model.LinkedPaymentMethod
import com.blockchain.presentation.getResolvedColor
import com.blockchain.presentation.getResolvedDrawable
import com.blockchain.presentation.koin.scopedInject
import com.blockchain.utils.unsafeLazy
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.RemoveBankBottomSheetBinding
import piuk.blockchain.android.simplebuy.SimpleBuyAnalytics

class RemoveLinkedBankBottomSheet : SlidingModalBottomDialog<RemoveBankBottomSheetBinding>() {

    interface Host : SlidingModalBottomDialog.Host {
        fun onLinkedBankRemoved(bankId: String)
    }

    private val compositeDisposable = CompositeDisposable()
    private val bankService: BankService by scopedInject()

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
            title.text = resources.getString(
                com.blockchain.stringResources.R.string.common_spaced_strings,
                bank.name,
                bank.currency.displayTicker
            )
            endDigits.text = resources.getString(
                com.blockchain.stringResources.R.string.dotted_suffixed_string,
                bank.accountEnding
            )
            accountInfo.text = getString(
                com.blockchain.stringResources.R.string.payment_method_type_account_info,
                bank.toHumanReadableAccount(),
                ""
            )
            rmvBankBtn.apply {
                text = getString(com.blockchain.stringResources.R.string.remove_bank)
                onClick = ::showConfirmation
            }
            rmvBankCancel.apply {
                text = getString(com.blockchain.stringResources.R.string.common_cancel)
                onClick = ::dismiss
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
            alertIcon?.setTint(requireContext().getResolvedColor(com.blockchain.common.R.color.orange_400))
            icon.setImageDrawable(alertIcon)

            endDigits.gone()
            title.text = getString(com.blockchain.stringResources.R.string.settings_bank_remove_check_title)
            accountInfo.text = getString(
                com.blockchain.stringResources.R.string.settings_bank_remove_check_subtitle,
                bank.name
            )
        }
    }

    private fun removeBank() {
        compositeDisposable += bankService.removeBank(bank)
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
                },
                onError = {
                    BlockchainSnackbar.make(
                        dialog?.window?.decorView ?: binding.root,
                        getString(com.blockchain.stringResources.R.string.settings_bank_remove_error),
                        type = SnackbarType.Error
                    ).show()
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
