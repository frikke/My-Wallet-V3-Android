package piuk.blockchain.android.ui.transfer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.Coincore
import com.blockchain.componentlib.alert.BlockchainSnackbar
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.domain.paymentmethods.BankService
import com.blockchain.koin.scopedInject
import com.blockchain.preferences.CurrencyPrefs
import com.google.android.material.snackbar.Snackbar
import io.reactivex.rxjava3.core.Single
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.FragmentTransferAccountSelectorBinding
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
    private val accountsSorting: AccountsSorting by scopedInject()
    private val bankService: BankService by scopedInject()
    private val currencyPrefs: CurrencyPrefs by inject()
    private lateinit var introHeaderView: IntroHeaderView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
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
        onExtraAccountInfoClicked: (AccountLocks) -> Unit = {},
        @StringRes title: Int,
        @StringRes label: Int,
        @DrawableRes icon: Int,
    ) {
        introHeaderView.setDetails(title, label, icon)

        with(binding.accountSelectorAccountList) {
            this.onAccountSelected = onAccountSelected
            this.onLockItemSelected = onExtraAccountInfoClicked
            this.activityIndicator = this@AccountSelectorFragment.activityIndicator
            initialise(
                source = accounts(),
                status = statusDecorator,
                accountsLocks = showWithdrawalLocks(),
                introView = introHeaderView
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResumeFragment() {
        refreshItems(showLoader = true)
    }

    fun refreshItems(showLoader: Boolean = true) {
        binding.accountSelectorAccountList.loadItems(
            accountsSource = accounts(),
            showLoader = showLoader,
            accountsLocksSource = showWithdrawalLocks()
        )
    }

    private fun showWithdrawalLocks(): Single<List<AccountLocks>> =
        bankService.getWithdrawalLocks(currencyPrefs.selectedFiatCurrency)
            .map { listOf(AccountLocks(it)) }

    private fun accounts(): Single<List<AccountListViewItem>> =
        coincore.walletsWithActions(actions = setOf(fragmentAction), sorter = accountsSorting.sorter()).map {
            it.map(AccountListViewItem.Companion::create)
        }

    protected abstract val fragmentAction: AssetAction

    protected fun setEmptyStateDetails(
        @StringRes title: Int,
        @StringRes label: Int,
        @StringRes ctaText: Int,
        action: () -> Unit,
    ) {
        binding.accountSelectorEmptyView.setDetails(
            title = title, description = label, ctaText = ctaText
        ) {
            action()
        }
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
            getString(R.string.transfer_wallets_load_error),
            duration = Snackbar.LENGTH_SHORT,
            type = SnackbarType.Error
        ).show()
        doOnEmptyList()
    }

    private fun doOnListLoaded(isEmpty: Boolean) {
        if (isEmpty) doOnEmptyList() else doOnListLoaded()
    }
}
