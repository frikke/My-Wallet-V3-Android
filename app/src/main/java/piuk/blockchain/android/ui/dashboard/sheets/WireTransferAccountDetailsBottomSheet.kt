package piuk.blockchain.android.ui.dashboard.sheets

import android.net.Uri
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.material.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import com.blockchain.api.NabuApiException
import com.blockchain.coincore.FiatAccount
import com.blockchain.commonarch.presentation.base.SlidingModalBottomDialog
import com.blockchain.componentlib.alert.BlockchainSnackbar
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.domain.common.model.ServerSideUxErrorInfo
import com.blockchain.domain.dataremediation.DataRemediationService
import com.blockchain.domain.dataremediation.model.Questionnaire
import com.blockchain.domain.dataremediation.model.QuestionnaireContext
import com.blockchain.fiatActions.QuestionnaireSheetHost
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.presentation.koin.scopedInject
import com.blockchain.utils.rxMaybeOutcome
import com.blockchain.utils.unsafeLazy
import com.google.android.material.snackbar.Snackbar
import info.blockchain.balance.FiatCurrency
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.DialogSheetLinkBankAccountBinding
import piuk.blockchain.android.simplebuy.BankDetailField
import piuk.blockchain.android.simplebuy.CopyFieldListener
import piuk.blockchain.android.simplebuy.SimpleBuyAnalytics
import piuk.blockchain.android.simplebuy.linkBankEventWithCurrency
import piuk.blockchain.android.simplebuy.linkBankFieldCopied
import piuk.blockchain.android.ui.dataremediation.QuestionnaireSheet
import piuk.blockchain.android.urllinks.MODULAR_TERMS_AND_CONDITIONS
import piuk.blockchain.android.util.StringUtils

class WireTransferAccountDetailsBottomSheet :
    SlidingModalBottomDialog<DialogSheetLinkBankAccountBinding>(),
    QuestionnaireSheetHost {

    private val compositeDisposable = CompositeDisposable()
    private val custodialWalletManager: CustodialWalletManager by scopedInject()
    private val dataRemediationService: DataRemediationService by scopedInject()
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
        compositeDisposable += rxMaybeOutcome {
            dataRemediationService.getQuestionnaire(QuestionnaireContext.FIAT_DEPOSIT)
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                binding.loading.visible()
            }
            .subscribeBy(
                onSuccess = { questionnaire ->
                    binding.loading.gone()
                    renderQuestionnaire(questionnaire)
                },
                onComplete = {
                    if (fiatCurrency.networkTicker in listOf("USD", "GBP")) {
                        fetchAndDisplayAccountDetails()
                    } else {
                        fetchAndDisplayOldAccountDetails()
                    }
                },
                onError = {
                    val uxError = (it as? NabuApiException)?.getServerSideErrorInfo()
                    renderErrorUi(uxError)
                    binding.loading.gone()
                }
            )
    }

    private fun fetchAndDisplayAccountDetails() {
        compositeDisposable += custodialWalletManager.getWireTransferDetails(fiatCurrency)
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                binding.fragmentContainer.gone()
                binding.loading.visible()
            }
            .subscribeBy(
                onSuccess = { bankAccount ->
                    binding.loading.gone()
                    binding.composeView.visible()
                    binding.composeView.setContent {
                        Surface(
                            modifier = Modifier.nestedScroll(rememberNestedScrollInteropConnection())
                        ) {
                            WireTransferAccountDetailsScreen(
                                isForLink = isForLink,
                                currency = fiatCurrency.networkTicker,
                                details = bankAccount,
                                backClicked = {
                                    dismiss()
                                },
                                onEntryCopied = { entry ->
                                    copyListener.onFieldCopied(entry.title)
                                },
                            )
                        }
                    }

                    analytics.logEvent(
                        linkBankEventWithCurrency(
                            SimpleBuyAnalytics.WIRE_TRANSFER_SCREEN_SHOWN,
                            fiatCurrency.networkTicker
                        )
                    )
                },
                onError = {
                    binding.loading.gone()
                    val uxError = (it as? NabuApiException)?.getServerSideErrorInfo()
                    renderErrorUi(uxError)
                    analytics.logEvent(
                        linkBankEventWithCurrency(
                            SimpleBuyAnalytics.WIRE_TRANSFER_LOADING_ERROR,
                            fiatCurrency.networkTicker
                        )
                    )
                }
            )
    }

    private fun fetchAndDisplayOldAccountDetails() {
        compositeDisposable += custodialWalletManager.getBankAccountDetails(fiatCurrency)
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                binding.fragmentContainer.gone()
                binding.loading.visible()
            }
            .subscribeBy(
                onSuccess = { bankAccount ->
                    binding.loading.gone()
                    binding.containerBankDetails.visible()
                    binding.bankDetails.initWithBankDetailsAndAmount(
                        bankAccount.details.map {
                            BankDetailField(it.title, it.value, it.isCopyable, it.tooltip)
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
                    binding.loading.gone()
                    val uxError = (it as? NabuApiException)?.getServerSideErrorInfo()
                    renderErrorUi(uxError)
                    analytics.logEvent(
                        linkBankEventWithCurrency(
                            SimpleBuyAnalytics.WIRE_TRANSFER_LOADING_ERROR,
                            fiatCurrency.networkTicker
                        )
                    )
                }
            )
    }

    private fun renderQuestionnaire(questionnaire: Questionnaire) {
        binding.fragmentContainer.visible()
        if (childFragmentManager.findFragmentById(R.id.fragment_container) == null) {
            childFragmentManager.beginTransaction()
                .add(
                    R.id.fragment_container,
                    QuestionnaireSheet.newInstance(questionnaire)
                ).commitAllowingStateLoss()
        }
    }

    private fun renderErrorUi(uxError: ServerSideUxErrorInfo?) {
        with(binding) {
            bankDetailsError.apply {
                errorContainer.visible()
                errorButton.setOnClickListener {
                    dismiss()
                }
                errorTitle.text = uxError?.title ?: getString(R.string.common_oops_bank)
                errorMessage.text = uxError?.description ?: getString(R.string.unable_to_load_bank_details)
            }
            composeView.gone()
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
                    "ARS" -> getString(R.string.processing_time_subtitle_ars)
                    else -> getString(R.string.processing_time_subtitle_eur)
                }
            )
            title.text = if (isForLink) {
                getString(R.string.add_bank_with_currency, fiatCurrency)
            } else {
                getString(R.string.deposit_currency, fiatCurrency)
            }
            subtitle.text = if (fiatCurrency == FiatCurrency.Dollars) {
                getString(R.string.wire_transfer)
            } else {
                getString(R.string.bank_transfer)
            }
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
                view = binding.root,
                message = if (field.isNotEmpty()) {
                    String.format(getString(R.string.simple_buy_copied_to_clipboard), field)
                } else {
                    getString(R.string.copied_to_clipboard)
                },
                duration = Snackbar.LENGTH_SHORT,
                type = SnackbarType.Success
            ).show()
        }
    }

    override fun questionnaireSubmittedSuccessfully() {
        fetchAndDisplayAccountDetails()
    }

    override fun questionnaireSkipped() {
        fetchAndDisplayAccountDetails()
    }

    override fun onSheetClosed() {
    }

    companion object {
        private const val FIAT_CURRENCY = "FIAT_CURRENCY_KEY"
        private const val IS_FOR_LINK = "IS_FOR_LINK"

        fun newInstance(fiatAccount: FiatAccount, isForLink: Boolean) =
            WireTransferAccountDetailsBottomSheet().apply {
                arguments = Bundle().apply {
                    putSerializable(FIAT_CURRENCY, fiatAccount.currency)
                    putBoolean(IS_FOR_LINK, isForLink)
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
