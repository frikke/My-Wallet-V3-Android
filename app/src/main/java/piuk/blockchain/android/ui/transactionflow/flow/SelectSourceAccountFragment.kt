package piuk.blockchain.android.ui.transactionflow.flow

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.SingleAccount
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.componentlib.viewextensions.visibleIf
import info.blockchain.balance.FiatCurrency
import io.reactivex.rxjava3.core.Single
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.FragmentTxAccountSelectorBinding
import piuk.blockchain.android.simplebuy.SimpleBuyAnalytics
import piuk.blockchain.android.simplebuy.linkBankEventWithCurrency
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.ui.dashboard.model.LinkablePaymentMethodsForAction
import piuk.blockchain.android.ui.dashboard.sheets.LinkBankMethodChooserBottomSheet
import piuk.blockchain.android.ui.dashboard.sheets.WireTransferAccountDetailsBottomSheet
import piuk.blockchain.android.ui.linkbank.BankAuthActivity
import piuk.blockchain.android.ui.settings.BankLinkingHost
import piuk.blockchain.android.ui.transactionflow.engine.BankLinkingState
import piuk.blockchain.android.ui.transactionflow.engine.DepositOptionsState
import piuk.blockchain.android.ui.transactionflow.engine.TransactionIntent
import piuk.blockchain.android.ui.transactionflow.engine.TransactionState
import piuk.blockchain.android.ui.transactionflow.flow.customisations.SourceSelectionCustomisations

class SelectSourceAccountFragment : TransactionFlowFragment<FragmentTxAccountSelectorBinding>(), BankLinkingHost {

    private val customiser: SourceSelectionCustomisations by inject()

    private var availableSources: List<BlockchainAccount>? = null
    private var linkingBankState: BankLinkingState = BankLinkingState.NotStarted

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            with(accountList) {
                onListLoaded = ::doOnListLoaded
                onLoadError = ::doOnLoadError
                onListLoading = ::doOnListLoading
            }

            addMethod.setOnClickListener {
                binding.progress.visible()
                model.process(TransactionIntent.CheckAvailableOptionsForFiatDeposit)
            }
        }
    }

    override fun render(newState: TransactionState) {
        binding.accountList.onAccountSelected = {
            require(it is SingleAccount)
            model.process(TransactionIntent.SourceAccountSelected(it))
            analyticsHooks.onSourceAccountSelected(it, newState)
        }

        if (availableSources != newState.availableSources) {
            updateSources(newState)
            binding.depositTooltip.root.apply {
                visibleIf { customiser.selectSourceShouldShowDepositTooltip(newState) }
                binding.depositTooltip.paymentMethodTitle.text =
                    if (newState.receivingAsset.networkTicker == "USD") getString(R.string.payment_wire_transfer)
                    else getString(R.string.bank_transfer)
                setOnClickListener {
                    showBottomSheet(WireTransferAccountDetailsBottomSheet.newInstance())
                }
            }
        }

        if (newState.linkBankState != BankLinkingState.NotStarted && linkingBankState != newState.linkBankState) {
            handleBankLinking(newState)
        }

        renderDepositOptions(newState)

        availableSources = newState.availableSources
        linkingBankState = newState.linkBankState
    }

    private fun renderDepositOptions(newState: TransactionState) {
        when (newState.depositOptionsState) {
            DepositOptionsState.LaunchLinkBank -> {
                model.process(TransactionIntent.StartLinkABank)
            }
            is DepositOptionsState.LaunchWireTransfer -> {
                binding.progress.gone()
                onBankWireTransferSelected(newState.depositOptionsState.fiatCurrency)
            }
            is DepositOptionsState.ShowBottomSheet -> {
                binding.progress.gone()
                LinkBankMethodChooserBottomSheet.newInstance(
                    LinkablePaymentMethodsForAction.LinkablePaymentMethodsForDeposit(
                        newState.depositOptionsState.linkablePaymentMethods
                    )
                ).show(childFragmentManager, BOTTOM_SHEET)
            }
            is DepositOptionsState.Error -> {
                displayErrorMessage()
            }
            DepositOptionsState.None -> {}
        }

        if (newState.depositOptionsState != DepositOptionsState.None) {
            model.process(TransactionIntent.FiatDepositOptionSelected(DepositOptionsState.None))
        }
    }

    private fun handleBankLinking(newState: TransactionState) {
        binding.progress.gone()

        if (newState.linkBankState is BankLinkingState.Success) {
            startActivityForResult(
                BankAuthActivity.newInstance(
                    newState.linkBankState.bankTransferInfo,
                    customiser.getLinkingSourceForAction(newState),
                    requireActivity()
                ),
                BankAuthActivity.LINK_BANK_REQUEST_CODE
            )
        } else {
            displayErrorMessage()
        }
    }

    private fun displayErrorMessage() {
        ToastCustom.makeText(
            requireContext(), getString(R.string.common_error), Toast.LENGTH_SHORT,
            ToastCustom.TYPE_ERROR
        )
    }

    private fun updateSources(newState: TransactionState) {
        with(binding) {
            accountList.initialise(
                source = Single.just(newState.availableSources.map { it }),
                status = customiser.sourceAccountSelectionStatusDecorator(newState),
                assetAction = newState.action
            )
            if (customiser.selectSourceShouldShowSubtitle(newState)) {
                accountListSubtitle.text = customiser.selectSourceAccountSubtitle(newState)
                accountListSubtitle.visible()
            } else {
                accountListSubtitle.gone()
                accountListSeparator.gone()
            }

            addMethod.visibleIf { customiser.selectSourceShouldShowAddNew(newState) }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == BankAuthActivity.LINK_BANK_REQUEST_CODE && resultCode == RESULT_OK) {
            binding.progress.visible()
            model.process(
                TransactionIntent.RefreshSourceAccounts
            )
        }
    }

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentTxAccountSelectorBinding =
        FragmentTxAccountSelectorBinding.inflate(inflater, container, false)

    private fun doOnListLoaded(isEmpty: Boolean) {
        with(binding) {
            accountListEmpty.visibleIf { isEmpty }
            progress.gone()
        }
    }

    private fun doOnLoadError(it: Throwable) {
        binding.accountListEmpty.visible()
        binding.progress.gone()
    }

    private fun doOnListLoading() {
        binding.accountListEmpty.gone()
        binding.progress.visible()
    }

    override fun onBankWireTransferSelected(currency: FiatCurrency) {
        WireTransferAccountDetailsBottomSheet.newInstance(currency).show(childFragmentManager, BOTTOM_SHEET)
        analytics.logEvent(linkBankEventWithCurrency(SimpleBuyAnalytics.WIRE_TRANSFER_CLICKED, currency.networkTicker))
    }

    override fun onLinkBankSelected(paymentMethodForAction: LinkablePaymentMethodsForAction) {
        binding.progress.visible()
        model.process(TransactionIntent.FiatDepositOptionSelected(DepositOptionsState.LaunchLinkBank))
    }

    override fun onSheetClosed() {}

    companion object {
        fun newInstance(): SelectSourceAccountFragment = SelectSourceAccountFragment()
    }
}
