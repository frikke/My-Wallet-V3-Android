package piuk.blockchain.android.ui.transfer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.annotation.StringRes
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.SingleAccountList
import com.blockchain.componentlib.alert.BlockchainSnackbar
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.data.asSingle
import com.blockchain.domain.paymentmethods.BankService
import com.blockchain.koin.defaultOrder
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.LocalSettingsPrefs
import com.blockchain.presentation.koin.scopedInject
import com.blockchain.utils.zipObservables
import com.google.android.material.snackbar.Snackbar
import io.reactivex.rxjava3.core.Single
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.FragmentTransferAccountSelectorBinding
import piuk.blockchain.android.support.SupportCentreActivity
import piuk.blockchain.android.ui.base.ViewPagerFragment
import piuk.blockchain.android.ui.customviews.IntroHeaderView
import piuk.blockchain.android.ui.customviews.account.AccountListViewItem
import piuk.blockchain.android.ui.customviews.account.AccountLocks
import piuk.blockchain.android.ui.customviews.account.StatusDecorator

abstract class AccountSelectorFragment : ViewPagerFragment() {

    private var _binding: FragmentTransferAccountSelectorBinding? = null
    private val binding: FragmentTransferAccountSelectorBinding
        get() = _binding!!

    private val coincore: Coincore by scopedInject()
    private val accountsSorting: AccountsSorting by scopedInject(defaultOrder)
    private val bankService: BankService by scopedInject()
    private val currencyPrefs: CurrencyPrefs by inject()
    private val localSettingsPrefs: LocalSettingsPrefs by scopedInject()

    private lateinit var introHeaderView: IntroHeaderView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransferAccountSelectorBinding.inflate(inflater, container, false)

        introHeaderView = IntroHeaderView(requireContext())

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding.accountSelectorAccountList) {
            onLoadError = ::doOnLoadError
            onListLoaded = ::doOnListLoaded
        }
    }

    fun initialiseAccountSelectorWithHeader(
        statusDecorator: StatusDecorator,
        onAccountSelected: (BlockchainAccount) -> Unit,
        onExtraAccountInfoClicked: (AccountLocks) -> Unit = {}
    ) {
        with(binding.accountSelectorAccountList) {
            this.onAccountSelected = onAccountSelected
            this.onLockItemSelected = onExtraAccountInfoClicked
            this.activityIndicator = this@AccountSelectorFragment.activityIndicator
            initialise(
                source = accounts(),
                status = statusDecorator,
                accountsLocks = showWithdrawalLocks()
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResumeFragment() {
        refreshItems()
    }

    private fun refreshItems() {
        binding.accountSelectorAccountList.loadItems(
            accountsSource = accounts(),
            showLoader = true,
            accountsLocksSource = showWithdrawalLocks()
        )
    }

    private fun showWithdrawalLocks(): Single<List<AccountLocks>> =
        bankService.getWithdrawalLocks(currencyPrefs.selectedFiatCurrency).asSingle()
            .map { listOf(AccountLocks(it)) }

    private fun accounts(): Single<List<AccountListViewItem>> =
        coincore.walletsWithAction(
            action = fragmentAction,
            sorter = accountsSorting.sorter()
        ).flatMap { list ->
            if (fragmentAction == AssetAction.Send) {
                checkAndFilterDustBalancesList(list)
            } else {
                Single.just(list)
            }.map {
                it.map { account ->
                    AccountListViewItem(account)
                }
            }
        }

    private fun checkAndFilterDustBalancesList(list: SingleAccountList) =
        if (localSettingsPrefs.hideSmallBalancesEnabled) {
            list.map { account ->
                account.balanceRx()
            }.zipObservables().map {
                list.mapIndexedNotNull { index, singleAccount ->
                    if (!it[index].totalFiat.isDust()) {
                        singleAccount
                    } else {
                        null
                    }
                }
            }.firstOrError()
        } else {
            Single.just(list)
        }

    protected abstract val fragmentAction: AssetAction

    protected fun setEmptyStateDetails(
        @StringRes title: Int,
        @StringRes label: Int,
        @StringRes ctaText: Int,
        action: () -> Unit
    ) {
        binding.accountSelectorEmptyView.setDetails(
            title = title,
            description = label,
            ctaText = ctaText,
            action = action,
            onContactSupport = { requireContext().startActivity(SupportCentreActivity.newIntent(requireContext())) }
        )
    }

    @CallSuper
    protected open fun doOnEmptyList() {
        with(binding) {
            accountSelectorAccountList.gone()
            accountSelectorEmptyView.visible()
        }
    }

    @CallSuper
    protected open fun doOnListLoaded() {
        with(binding) {
            accountSelectorAccountList.visible()
            accountSelectorEmptyView.gone()
        }
    }

    private fun doOnLoadError(t: Throwable) {
        BlockchainSnackbar.make(
            binding.root,
            getString(com.blockchain.stringResources.R.string.transfer_wallets_load_error),
            duration = Snackbar.LENGTH_SHORT,
            type = SnackbarType.Error
        ).show()
        doOnEmptyList()
    }

    private fun doOnListLoaded(accounts: List<AccountListViewItem>) {
        val isEmpty = accounts.isEmpty()
        if (isEmpty) doOnEmptyList() else doOnListLoaded()
    }
}
