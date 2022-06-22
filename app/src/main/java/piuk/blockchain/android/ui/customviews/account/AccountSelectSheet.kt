package piuk.blockchain.android.ui.customviews.account

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.StringRes
import com.blockchain.analytics.events.activityShown
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.Coincore
import com.blockchain.commonarch.presentation.base.HostedBottomSheet
import com.blockchain.commonarch.presentation.base.SlidingModalBottomDialog
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.componentlib.viewextensions.visibleIf
import com.blockchain.koin.scopedInject
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.DialogSheetAccountSelectorBinding

class AccountSelectSheet(
    override val host: HostedBottomSheet.Host,
) : SlidingModalBottomDialog<DialogSheetAccountSelectorBinding>() {

    interface SelectionHost : HostedBottomSheet.Host {
        fun onAccountSelected(account: BlockchainAccount)
    }

    interface SelectAndBackHost : SelectionHost {
        fun onAccountSelectorBack()
    }

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): DialogSheetAccountSelectorBinding =
        DialogSheetAccountSelectorBinding.inflate(inflater, container, false)

    private val coincore: Coincore by scopedInject()
    private val disposables = CompositeDisposable()

    private var accountList: Single<List<AccountListViewItem>> =
        coincore.allWalletsInActiveMode()
            .map { listOf(it) + it.accounts }
            .map { it.filter(BlockchainAccount::hasTransactions).map(AccountListViewItem.Companion::create) }

    private var sheetTitle: Int = R.string.select_account_sheet_title
    private var sheetSubtitle: Int = R.string.empty
    private var statusDecorator: StatusDecorator = { DefaultCellDecorator() }

    private fun doOnAccountSelected(account: BlockchainAccount) {
        analytics.logEvent(activityShown(account.label))
        (host as SelectionHost).onAccountSelected(account)
        dismiss()
    }

    private fun doOnListLoaded(isEmpty: Boolean) {
        binding.accountListEmpty.visibleIf { isEmpty }
        binding.progress.gone()
    }

    private fun doOnLoadError(it: Throwable) {
        binding.progress.gone()
        dismiss()
    }

    private fun doOnListLoading() {
        binding.progress.visible()
    }

    override fun initControls(binding: DialogSheetAccountSelectorBinding) {
        with(binding) {
            accountList.apply {
                onAccountSelected = ::doOnAccountSelected
                onListLoaded = ::doOnListLoaded
                onLoadError = ::doOnLoadError
                onListLoading = ::doOnListLoading
            }
            accountListTitle.text = getString(sheetTitle)
            accountListSubtitle.text = getString(sheetSubtitle)
            accountListSubtitle.visibleIf { getString(sheetSubtitle).isNotEmpty() }
        }
        if (host is SelectAndBackHost) {
            showBackArrow()
        } else {
            binding.accountListBack.gone()
        }

        binding.accountList.initialise(
            source = accountList,
            status = statusDecorator
        )
    }

    private fun showBackArrow() {
        binding.accountListBack.visible()
        binding.accountListBack.setOnClickListener {
            (host as SelectAndBackHost).onAccountSelectorBack()
        }
    }

    override fun onSheetHidden() {
        super.onSheetHidden()
        disposables.dispose()
    }

    companion object {
        fun newInstance(host: HostedBottomSheet.Host): AccountSelectSheet = AccountSelectSheet(host)

        fun newInstance(
            host: SelectionHost,
            accountList: Single<List<BlockchainAccount>>,
            @StringRes sheetTitle: Int,
        ): AccountSelectSheet =
            AccountSelectSheet(host).apply {
                this.accountList = accountList.map { accounts ->
                    accounts.map(AccountListViewItem.Companion::create)
                }
                this.sheetTitle = sheetTitle
            }

        fun newInstance(
            host: HostedBottomSheet.Host,
            accountList: Single<List<BlockchainAccount>>,
            @StringRes sheetTitle: Int,
            @StringRes sheetSubtitle: Int,
            statusDecorator: StatusDecorator,
        ): AccountSelectSheet =
            AccountSelectSheet(host).apply {
                this.accountList = accountList.map { list -> list.map(AccountListViewItem.Companion::create) }
                this.sheetTitle = sheetTitle
                this.sheetSubtitle = sheetSubtitle
                this.statusDecorator = statusDecorator
            }
    }
}
