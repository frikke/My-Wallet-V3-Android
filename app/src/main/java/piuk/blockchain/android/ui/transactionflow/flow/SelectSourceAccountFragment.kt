package piuk.blockchain.android.ui.transactionflow.flow

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.blockchain.api.NabuApiException
import com.blockchain.api.NabuErrorCodes
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.NullAddress
import com.blockchain.coincore.SingleAccount
import com.blockchain.componentlib.alert.BlockchainSnackbar
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.hideKeyboard
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.componentlib.viewextensions.visibleIf
import com.blockchain.extensions.enumValueOfOrNull
import info.blockchain.balance.FiatCurrency
import io.reactivex.rxjava3.core.Single
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.FragmentTxAccountSelectorBinding
import piuk.blockchain.android.simplebuy.SimpleBuyAnalytics
import piuk.blockchain.android.simplebuy.linkBankEventWithCurrency
import piuk.blockchain.android.ui.base.ErrorButtonCopies
import piuk.blockchain.android.ui.base.ErrorDialogData
import piuk.blockchain.android.ui.base.ErrorSlidingBottomDialog
import piuk.blockchain.android.ui.customviews.account.AccountListViewItem
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
import piuk.blockchain.android.util.StringLocalizationUtil

class SelectSourceAccountFragment :
    TransactionFlowFragment<FragmentTxAccountSelectorBinding>(),
    BankLinkingHost,
    ErrorSlidingBottomDialog.Host {

    private val customiser: SourceSelectionCustomisations by inject()
    private val assetAction: AssetAction? by lazy {
        enumValueOfOrNull<AssetAction>(arguments?.getString(ACTION).orEmpty())
    }

    private var availableSources: List<BlockchainAccount>? = null
    private var linkingBankState: BankLinkingState = BankLinkingState.NotStarted
    private var hasEnteredSearchTerm: Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        analyticsHooks.onFromSourcesAccountViewed(assetAction)
        with(binding) {
            accountList.apply {
                onListLoaded = ::doOnListLoaded
                onLoadError = ::doOnLoadError
                onListLoading = ::doOnListLoading
            }

            addMethod.apply {
                text = getString(R.string.add_payment_method)
                onClick = {
                    binding.progress.visible()
                    model.process(TransactionIntent.CheckAvailableOptionsForFiatDeposit)
                }
            }

            with(binding) {
                assetAction?.let {
                    if (customiser.selectSourceShouldHaveSearch(it)) {
                        setupSearch()
                    } else {
                        sourceSelectSearch.gone()
                    }

                    if (customiser.shouldShowSourceAccountWalletsSwitch(it)) {
                        pkwSwitchLayout.visible()
                        pkwAccountsSwitch.onCheckChanged = { isChecked ->
                            model.process(TransactionIntent.UpdatePrivateKeyFilter(isChecked))
                        }
                    }
                }
            }
        }
    }

    override fun render(newState: TransactionState) {
        binding.accountList.onAccountSelected = {
            require(it is SingleAccount)
            requireActivity().hideKeyboard()

            model.process(TransactionIntent.SourceAccountSelected(it))
            analyticsHooks.onSourceAccountSelected(it, newState)
        }

        if (availableSources != newState.availableSources) {
            updateSources(newState)
            binding.depositTooltip.root.apply {
                visibleIf { customiser.selectSourceShouldShowDepositTooltip(newState) }
                if (newState.selectedTarget != NullAddress) {
                    binding.depositTooltip.paymentMethodTitle.text = binding.root.context.getString(
                        StringLocalizationUtil.getBankDepositTitle(newState.receivingAsset.networkTicker)
                    )
                    setOnClickListener {
                        showBottomSheet(WireTransferAccountDetailsBottomSheet.newInstance())
                    }
                }
            }
        }

        if (newState.linkBankState != BankLinkingState.NotStarted && linkingBankState != newState.linkBankState) {
            handleBankLinking(newState)
        }

        renderDepositOptions(newState)

        availableSources = newState.availableSources
        linkingBankState = newState.linkBankState

        binding.pkwAccountsSwitch.isChecked = newState.isPkwAccountFilterActive
    }

    private fun FragmentTxAccountSelectorBinding.setupSearch() {
        sourceSelectSearch.visible()
        sourceSelectSearch.apply {
            label = getString(R.string.search_wallets_hint)
            onValueChange = { searchTerm ->
                this@setupSearch.onSearchTermUpdated(searchTerm)
            }
        }

        sourceListSearchEmpty.apply {
            text = getString(R.string.select_target_source_no_search_results)
            gravity = ComposeGravities.Centre
            style = ComposeTypographies.Body1
        }
    }

    private fun FragmentTxAccountSelectorBinding.onSearchTermUpdated(searchTerm: String) {
        hasEnteredSearchTerm = searchTerm.isNotEmpty()
        sourceListSearchEmpty.gone()

        with(accountList) {
            visible()
            loadItems(
                accountsSource = Single.just(
                    availableSources?.filter { account ->
                        if (searchTerm.isNotEmpty()) {
                            val singleAccountCurrency = (account as? SingleAccount)?.currency
                            singleAccountCurrency?.networkTicker?.contains(searchTerm, true) ?: false ||
                                singleAccountCurrency?.name?.contains(searchTerm, true) ?: false ||
                                account.label.contains(searchTerm, true)
                        } else {
                            true
                        }
                    }?.map(AccountListViewItem.Companion::create) ?: emptyList()
                ),
                accountsLocksSource = Single.just(emptyList()),
                showLoader = false
            )
        }
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
                    linkablePaymentMethodsForAction = LinkablePaymentMethodsForAction.LinkablePaymentMethodsForDeposit(
                        newState.depositOptionsState.linkablePaymentMethods
                    ),
                    transactionTarget = newState.selectedTarget
                ).show(childFragmentManager, BOTTOM_SHEET)
            }
            is DepositOptionsState.Error -> {
                displayErrorSnackbar()
            }
            DepositOptionsState.None -> {
            }
        }

        if (newState.depositOptionsState != DepositOptionsState.None) {
            model.process(TransactionIntent.FiatDepositOptionSelected(DepositOptionsState.None))
        }
    }

    private fun handleBankLinking(newState: TransactionState) {
        binding.progress.gone()
        when (newState.linkBankState) {
            is BankLinkingState.Success -> handleBankLinkSuccess(newState.linkBankState, newState)
            is BankLinkingState.Error -> handleBankLinkError(newState.linkBankState)
            else -> displayErrorSnackbar()
        }
    }

    private fun handleBankLinkSuccess(linkBankState: BankLinkingState.Success, newState: TransactionState) {
        startActivityForResult(
            BankAuthActivity.newInstance(
                linkBankState.bankTransferInfo,
                customiser.getLinkingSourceForAction(newState),
                requireActivity()
            ),
            BankAuthActivity.LINK_BANK_REQUEST_CODE
        )
    }

    private fun handleBankLinkError(linkBankState: BankLinkingState.Error) {
        val nabuError = linkBankState.e as? NabuApiException
        when (nabuError?.getErrorCode()) {
            NabuErrorCodes.MaxPaymentBankAccounts ->
                showBottomSheet(
                    ErrorSlidingBottomDialog.newInstance(
                        ErrorDialogData(
                            title = getString(R.string.bank_linking_max_accounts_title),
                            description = getString(R.string.bank_linking_max_accounts_subtitle),
                            errorButtonCopies = ErrorButtonCopies(primaryButtonText = getString(R.string.common_ok)),
                            error = nabuError.getErrorDescription(),
                            nabuApiException = nabuError,
                            analyticsCategories = nabuError.getServerSideErrorInfo()?.categories ?: emptyList()
                        )
                    )
                )
            NabuErrorCodes.MaxPaymentBankAccountLinkAttempts ->
                showBottomSheet(
                    ErrorSlidingBottomDialog.newInstance(
                        ErrorDialogData(
                            title = getString(R.string.bank_linking_max_attempts_title),
                            description = getString(R.string.bank_linking_max_attempts_subtitle),
                            errorButtonCopies = ErrorButtonCopies(primaryButtonText = getString(R.string.common_ok)),
                            error = nabuError.getErrorDescription(),
                            nabuApiException = nabuError,
                            analyticsCategories = nabuError.getServerSideErrorInfo()?.categories ?: emptyList()
                        )
                    )
                )
            else -> displayErrorSnackbar()
        }
    }

    private fun displayErrorSnackbar() {
        BlockchainSnackbar.make(
            binding.root, getString(R.string.common_error), type = SnackbarType.Error
        ).show()
    }

    private fun updateSources(newState: TransactionState) {
        with(binding) {
            accountList.initialise(
                source = Single.just(newState.availableSources.map(AccountListViewItem.Companion::create)),
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
            if (isEmpty) {
                if (hasEnteredSearchTerm) {
                    accountListEmpty.gone()
                    sourceListSearchEmpty.visible()
                } else {
                    accountListEmpty.visible()
                    accountList.visible()
                    sourceListSearchEmpty.gone()
                }
            }
            progress.gone()
        }
    }

    private fun doOnLoadError(it: Throwable) {
        with(binding) {
            accountListEmpty.visible()
            progress.gone()
        }
    }

    private fun doOnListLoading() {
        with(binding) {
            accountListEmpty.gone()
            sourceListSearchEmpty.gone()
            progress.visible()
        }
    }

    override fun onBankWireTransferSelected(currency: FiatCurrency) {
        WireTransferAccountDetailsBottomSheet.newInstance(currency).show(childFragmentManager, BOTTOM_SHEET)
        analytics.logEvent(linkBankEventWithCurrency(SimpleBuyAnalytics.WIRE_TRANSFER_CLICKED, currency.networkTicker))
    }

    override fun onLinkBankSelected(paymentMethodForAction: LinkablePaymentMethodsForAction) {
        binding.progress.visible()
        model.process(TransactionIntent.FiatDepositOptionSelected(DepositOptionsState.LaunchLinkBank))
    }

    override fun onErrorPrimaryCta() {
        // do nothing
    }

    override fun onErrorSecondaryCta() {
        // do nothing
    }

    override fun onErrorTertiaryCta() {
        // do nothing
    }

    override fun onSheetClosed() {}

    companion object {
        private const val ACTION = "ASSET_ACTION"
        fun newInstance(assetAction: AssetAction): SelectSourceAccountFragment =
            SelectSourceAccountFragment().apply {
                arguments = Bundle().apply {
                    putString(ACTION, assetAction.name)
                }
            }
    }
}
