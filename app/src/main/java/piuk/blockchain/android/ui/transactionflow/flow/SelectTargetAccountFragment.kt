package piuk.blockchain.android.ui.transactionflow.flow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.SingleAccount
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.visible
import io.reactivex.rxjava3.core.Single
import org.koin.android.ext.android.inject
import piuk.blockchain.android.databinding.FragmentTxAccountSelectorBinding
import piuk.blockchain.android.ui.customviews.account.AccountListViewItem
import piuk.blockchain.android.ui.transactionflow.engine.TransactionIntent
import piuk.blockchain.android.ui.transactionflow.engine.TransactionState
import piuk.blockchain.android.ui.transactionflow.flow.customisations.TargetSelectionCustomisations

class SelectTargetAccountFragment : TransactionFlowFragment<FragmentTxAccountSelectorBinding>() {

    private val customiser: TargetSelectionCustomisations by inject()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            accountList.onListLoaded = ::doOnListLoaded
            accountList.onLoadError = ::doOnLoadError
        }
    }

    override fun render(newState: TransactionState) {
        with(binding) {
            accountList.initialise(
                source = Single.just(
                    newState.availableTargets.map { transactionTarget ->
                        AccountListViewItem.create(transactionTarget as SingleAccount)
                    }
                ),
                status = customiser.selectTargetStatusDecorator(newState),
                assetAction = newState.action
            )
            if (customiser.selectTargetShouldShowSubtitle(newState)) {
                accountListSubtitle.text = customiser.selectTargetAccountDescription(newState)
                accountListSubtitle.visible()
            } else {
                accountListSubtitle.gone()
                accountListSeparator.gone()
            }
            accountList.onAccountSelected = { account: BlockchainAccount -> doOnAccountSelected(account, newState) }
        }
    }

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentTxAccountSelectorBinding =
        FragmentTxAccountSelectorBinding.inflate(inflater, container, false)

    private fun doOnListLoaded(isEmpty: Boolean) {
        binding.progress.gone()
    }

    private fun doOnAccountSelected(account: BlockchainAccount, state: TransactionState) {
        require(account is SingleAccount)
        model.process(TransactionIntent.TargetAccountSelected(account))
        analyticsHooks.onTargetAccountSelected(account, state)
    }

    private fun doOnLoadError(it: Throwable) {
        binding.accountListEmpty.visible()
        binding.progress.gone()
    }

    companion object {
        fun newInstance(): SelectTargetAccountFragment = SelectTargetAccountFragment()
    }
}
