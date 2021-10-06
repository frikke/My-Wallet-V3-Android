package piuk.blockchain.android.ui.transfer.receive

import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.CryptoAccount
import com.blockchain.koin.scopedInject
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.FragmentReceiveBinding
import piuk.blockchain.android.ui.base.mvi.MviFragment
import piuk.blockchain.android.ui.customviews.BlockchainListDividerDecor
import piuk.blockchain.android.ui.resources.AssetResources
import piuk.blockchain.android.ui.transactionflow.analytics.TxFlowAnalyticsAccountType
import piuk.blockchain.android.ui.transfer.analytics.TransferAnalyticsEvent
import piuk.blockchain.android.ui.transfer.receive.detail.ReceiveDetailSheet
import piuk.blockchain.android.ui.upsell.KycUpgradePromptManager
import piuk.blockchain.android.util.AfterTextChangedWatcher
import piuk.blockchain.android.util.visibleIf

class ReceiveFragment : MviFragment<ReceiveModel, ReceiveIntent, ReceiveState, FragmentReceiveBinding>() {

    private val assetResources: AssetResources by inject()
    private val compositeDisposable = CompositeDisposable()
    private val upsellManager: KycUpgradePromptManager by scopedInject()

    override val model: ReceiveModel by scopedInject()

    private val assetsAdapter: ExpandableAssetsAdapter =
        ExpandableAssetsAdapter(
            assetResources,
            compositeDisposable
        )

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentReceiveBinding =
        FragmentReceiveBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initialiseAccountSelectorWithHeader()

        setupSearchBox()
    }

    override fun onStop() {
        super.onStop()
        compositeDisposable.clear()
    }

    override fun render(newState: ReceiveState) {
        val assetsToShow = newState.assets
            .filter { asset ->
                asset.displayTicker.contains(newState.filterBy, ignoreCase = true) ||
                    asset.name.contains(newState.filterBy, ignoreCase = true)
            }
        assetsAdapter.items = assetsToShow.map { assetInfo ->
            ExpandableCryptoItem(
                assetInfo = assetInfo,
                newState.loadAccountsForAsset,
                ::doOnAccountSelected
            )
        }
        binding.searchResultsLabel.apply {
            text = getString(R.string.search_wallets_result, assetsToShow.size.toString())
            visibleIf { newState.filterBy.isNotEmpty() }
        }
    }

    private fun initialiseAccountSelectorWithHeader() {
        with(binding) {
            header.setDetails(
                title = R.string.transfer_receive_crypto_title,
                label = R.string.transfer_receive_crypto_label,
                icon = R.drawable.ic_receive_blue_circle,
                showSeparator = false
            )
            assetList.apply {
                layoutManager = LinearLayoutManager(activity)
                this.adapter = assetsAdapter
                addItemDecoration(BlockchainListDividerDecor(requireContext()))
            }
            model.process(ReceiveIntent.GetAvailableAssets)
        }
    }

    private fun setupSearchBox() {
        binding.searchEditText.addTextChangedListener(object : AfterTextChangedWatcher() {
            override fun afterTextChanged(s: Editable?) {
                s?.let { editable ->
                    model.process(ReceiveIntent.FilterAssets(editable.toString()))
                }
            }
        })
    }

    private fun doOnAccountSelected(account: CryptoAccount) {
        compositeDisposable += upsellManager.queryUpsell(AssetAction.Receive, account)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { type ->
                if (type == KycUpgradePromptManager.Type.NONE) {
                    ReceiveDetailSheet.newInstance(account).show(childFragmentManager, BOTTOM_SHEET)
                } else {
                    KycUpgradePromptManager.getUpsellSheet(type).show(childFragmentManager,
                        BOTTOM_SHEET
                    )
                }
                analytics.logEvent(
                    TransferAnalyticsEvent.ReceiveAccountSelected(
                        TxFlowAnalyticsAccountType.fromAccount(account),
                        account.asset
                    )
                )
            }
    }

    companion object {
        const val BOTTOM_SHEET = "BOTTOM_SHEET"

        fun newInstance() = ReceiveFragment()
    }
}