package piuk.blockchain.android.ui.dashboard.sheets

import android.net.Uri
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockchain.coincore.FiatAccount
import com.blockchain.commonarch.presentation.base.SlidingModalBottomDialog
import com.blockchain.componentlib.alert.abstract.SnackbarType
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.koin.scopedInject
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.preferences.CurrencyPrefs
import com.google.android.material.snackbar.Snackbar
import info.blockchain.balance.FiatCurrency
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.DialogSheetLinkBankAccountBinding
import piuk.blockchain.android.simplebuy.BankDetailField
import piuk.blockchain.android.simplebuy.CopyFieldListener
import piuk.blockchain.android.simplebuy.SimpleBuyAnalytics
import piuk.blockchain.android.simplebuy.linkBankEventWithCurrency
import piuk.blockchain.android.simplebuy.linkBankFieldCopied
import piuk.blockchain.android.ui.customviews.BlockchainSnackbar
import piuk.blockchain.android.urllinks.MODULAR_TERMS_AND_CONDITIONS
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy

class WireTransferAccountDetailsBottomSheet : SlidingModalBottomDialog<DialogSheetLinkBankAccountBinding>() {

    private val compositeDisposable = CompositeDisposable()
    private val custodialWalletManager: CustodialWalletManager by scopedInject()
    private val stringUtils: StringUtils by inject()
    private val currencyPrefs: CurrencyPrefs by scopedInject()

    private val fiatCurrency: FiatCurrency by unsafeLazy {
        arguments?.getSerializable(FIAT_CURRENCY) as? FiatCurrency
            ?: currencyPrefs.selectedFiatCurrency
    }

    private val isForLink: Boolean by unsafeLazy {
        arguments?.getBoolean(IS_FOR_LINK) ?: false
    }

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): DialogSheetLinkBankAccountBinding =
        DialogSheetLinkBankAccountBinding.inflate(inflater, container, false)

    override fun initControls(binding: DialogSheetLinkBankAccountBinding) {
        compositeDisposable += custodialWalletManager.getBankAccountDetails(fiatCurrency)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { bankAccount ->
                    binding.bankDetails.initWithBankDetailsAndAmount(
                        bankAccount.details.map {
                            BankDetailField(it.title, it.value, it.isCopyable)
                        },
                        copyListener
                    )
                    configureUi(fiatCurrency)

                    analytics.logEvent(
                        linkBankEventWithCurrency(
                            SimpleBuyAnalytics.WIRE_TRANSFER_SCREEN_SHOWN,
                            fiatCurrency.networkTicker
                        )
                    )
                },
                onError = {
                    renderErrorUi()
                    analytics.logEvent(
                        linkBankEventWithCurrency(
                            SimpleBuyAnalytics.WIRE_TRANSFER_LOADING_ERROR,
                            fiatCurrency.networkTicker
                        )
                    )
                }
            )
    }

    private fun renderErrorUi() {
        with(binding) {
            bankDetailsError.errorContainer.visible()
            bankDetailsError.errorButton.setOnClickListener {
                dismiss()
            }
            title.gone()
            subtitle.gone()
            bankDetails.gone()
            bankTransferOnly.gone()
            processingTime.gone()
            bankDepositInstruction.gone()
        }
    }

    private fun configureUi(fiatCurrency: FiatCurrency) {
        with(binding) {
            if (fiatCurrency.networkTicker == "GBP") {
                val linksMap = mapOf<String, Uri>(
                    "modular_terms_and_conditions" to Uri.parse(MODULAR_TERMS_AND_CONDITIONS)
                )
                bankDepositInstruction.text =
                    stringUtils.getStringWithMappedAnnotations(
                        R.string.by_depositing_funds_terms_and_conds,
                        linksMap,
                        requireActivity()
                    )
                bankDepositInstruction.movementMethod = LinkMovementMethod.getInstance()
            } else {
                bankDepositInstruction.gone()
            }

            processingTime.updateSubtitle(
                when (fiatCurrency.networkTicker) {
                    "GBP" -> getString(R.string.processing_time_subtitle_gbp)
                    "USD" -> getString(R.string.processing_time_subtitle_usd)
                    else -> getString(R.string.processing_time_subtitle_eur)
                }
            )
            title.text = if (isForLink) getString(R.string.add_bank_with_currency, fiatCurrency) else
                getString(R.string.deposit_currency, fiatCurrency)
            subtitle.text = if (fiatCurrency == FiatCurrency.Dollars) getString(R.string.wire_transfer) else
                getString(R.string.bank_transfer)

            bankTransferOnly.visible()
            processingTime.visible()
        }
    }

    override fun dismiss() {
        super.dismiss()
        compositeDisposable.dispose()
    }

    private val copyListener = object : CopyFieldListener {
        override fun onFieldCopied(field: String) {
            analytics.logEvent(linkBankFieldCopied(field, fiatCurrency.networkTicker))
            BlockchainSnackbar.make(
                binding.root,
                getString(R.string.simple_buy_copied_to_clipboard),
                duration = Snackbar.LENGTH_SHORT,
                type = SnackbarType.Success
            ).show()
        }
    }

    companion object {
        private const val FIAT_CURRENCY = "FIAT_CURRENCY_KEY"
        private const val IS_FOR_LINK = "IS_FOR_LINK"

        fun newInstance(fiatAccount: FiatAccount) =
            WireTransferAccountDetailsBottomSheet().apply {
                arguments = Bundle().apply {
                    putSerializable(FIAT_CURRENCY, fiatAccount.currency as FiatCurrency)
                    putBoolean(IS_FOR_LINK, !fiatAccount.isFunded)
                }
            }

        fun newInstance() = WireTransferAccountDetailsBottomSheet()

        fun newInstance(fiatCurrency: FiatCurrency) =
            WireTransferAccountDetailsBottomSheet().apply {
                arguments = Bundle().apply {
                    putSerializable(FIAT_CURRENCY, fiatCurrency)
                }
            }
    }
}
