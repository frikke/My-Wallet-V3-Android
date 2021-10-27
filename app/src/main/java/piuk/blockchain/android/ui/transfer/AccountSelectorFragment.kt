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
import com.blockchain.core.payments.PaymentsDataManager
import com.blockchain.featureflags.GatedFeature
import com.blockchain.featureflags.InternalFeatureFlagApi
import com.blockchain.koin.scopedInject
import com.blockchain.preferences.CurrencyPrefs
import io.reactivex.rxjava3.core.Single
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.FragmentTransferAccountSelectorBinding
import piuk.blockchain.android.ui.base.ViewPagerFragment
import piuk.blockchain.android.ui.customviews.IntroHeaderView
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.ui.customviews.account.AccountLocks
import piuk.blockchain.android.ui.customviews.account.StatusDecorator
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.visible

abstract class AccountSelectorFragment : ViewPagerFragment() {

    private var _binding: FragmentTransferAccountSelectorBinding? = null
    private val binding: FragmentTransferAccountSelectorBinding
        get() = _binding!!

    private val coincore: Coincore by scopedInject()
    private val accountsSorting: AccountsSorting by scopedInject()
    private val gatedFeatures: InternalFeatureFlagApi by inject()
    private val paymentsDataManager: PaymentsDataManager by scopedInject()
    private val currencyPrefs: CurrencyPrefs by inject()
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
        onExtraAccountInfoClicked: ((AccountLocks) -> Unit)? = null,
        @StringRes title: Int,
        @StringRes label: Int,
        @DrawableRes icon: Int
    ) {
        introHeaderView.setDetails(title, label, icon)

        with(binding.accountSelectorAccountList) {
            this.onAccountSelected = onAccountSelected
            this.onLockItemSelected = onExtraAccountInfoClicked!!
            this.activityIndicator = this@AccountSelectorFragment.activityIndicator
            initialise(
                source = accounts(),
                status = statusDecorator,
                introView = introHeaderView,
                accountsLocks = showWithdrawalLocks()
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResumeFragment() {
        refreshItems(showLoader = false)
    }

    fun refreshItems(showLoader: Boolean = true) {
        binding.accountSelectorAccountList.loadItems(
            accountsSource = accounts(),
            showLoader = showLoader,
            accountsLocksSource = showWithdrawalLocks()
        )
    }

    private fun showWithdrawalLocks(): Single<List<AccountLocks>> =
        if (gatedFeatures.isFeatureEnabled(GatedFeature.WITHDRAWAL_LOCKS)) {
            paymentsDataManager.getWithdrawalLocks(currencyPrefs.selectedFiatCurrency)
                .map { listOf(AccountLocks(it)) }
        } else Single.just(emptyList())

    private fun accounts(): Single<List<BlockchainAccount>> =
        coincore.allWalletsWithActions(setOf(fragmentAction), accountsSorting.sorter()).map {
            it.map { account -> account }
        }

    protected abstract val fragmentAction: AssetAction

    protected fun setEmptyStateDetails(
        @StringRes title: Int,
        @StringRes label: Int,
        @StringRes ctaText: Int,
        action: () -> Unit
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
        ToastCustom.makeText(
            requireContext(),
            getString(R.string.transfer_wallets_load_error),
            ToastCustom.LENGTH_SHORT,
            ToastCustom.TYPE_ERROR
        )
        doOnEmptyList()
    }

    private fun doOnListLoaded(isEmpty: Boolean) {
        if (isEmpty) doOnEmptyList() else doOnListLoaded()
    }
}