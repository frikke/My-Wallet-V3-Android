package piuk.blockchain.android.ui.transfer.receive

import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockchain.coincore.ActionState
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.CryptoAccount
import com.blockchain.commonarch.presentation.mvi.MviFragment
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.koin.scopedInject
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import info.blockchain.balance.Currency
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.databinding.FragmentReceiveBinding
import piuk.blockchain.android.ui.customviews.BlockchainListDividerDecor
import piuk.blockchain.android.ui.customviews.account.AccountListViewItem
import piuk.blockchain.android.ui.dashboard.sheets.KycUpgradeNowSheet
import piuk.blockchain.android.ui.kyc.navhost.KycNavHostActivity
import piuk.blockchain.android.ui.resources.AssetResources
import piuk.blockchain.android.ui.transactionflow.analytics.TxFlowAnalyticsAccountType
import piuk.blockchain.android.ui.transfer.analytics.TransferAnalyticsEvent
import piuk.blockchain.android.ui.transfer.receive.detail.ReceiveDetailSheet
import piuk.blockchain.android.util.AfterTextChangedWatcher

class ReceiveFragment :
    MviFragment<ReceiveModel, ReceiveIntent, ReceiveState, FragmentReceiveBinding>(),
    KycUpgradeNowSheet.Host {

    private val assetResources: AssetResources by inject()
    private val compositeDisposable = CompositeDisposable()
    private val walletModeService: WalletModeService by inject()

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
        if (walletModeService.enabledWalletMode() == WalletMode.UNIVERSAL) {
            renderUniversalMode(newState)
        } else renderSuperAppReceiveAccounts(newState)
    }

    private fun renderUniversalMode(newState: ReceiveState) {
        binding.assetList.visible()
        binding.supperAppAccountList.gone()
        binding.assetList.apply {
            layoutManager = LinearLayoutManager(activity)
            this.adapter = assetsAdapter
            addItemDecoration(BlockchainListDividerDecor(requireContext()))
        }

        assetsAdapter.items = newState.assets.filter { asset -> asset.filteredBy(newState.input) }.map { assetInfo ->
            ExpandableCryptoItem(
                assetInfo = assetInfo,
                newState.loadAccountsForAsset,
                ::doOnAccountSelected
            )
        }

        binding.searchBoxLayout.apply {
            updateResults(
                resultCount = newState.assets.size.toString(),
                shouldShow = newState.input.isNotEmpty()
            )
            updateLayoutState()
        }
    }

    private fun renderSuperAppReceiveAccounts(newState: ReceiveState) {
        binding.assetList.gone()
        binding.supperAppAccountList.apply {
            visible()
            initialise(
                source = Single.just(newState.allReceivableAccountsSource)
                    .map { accounts -> accounts.filter { it.currency.filteredBy(newState.input) } }
                    .map {
                        it.map { account ->
                            AccountListViewItem.create(account)
                        }
                    },
            )
            onAccountSelected = {
                (it as? CryptoAccount)?.let { cryptoAccount ->
                    doOnAccountSelected(cryptoAccount)
                }
            }
        }
        binding.searchBoxLayout.apply {
            updateLayoutState()
        }
    }

    private fun Currency.filteredBy(input: String) =
        input.isEmpty() ||
            networkTicker.contains(input, true) ||
            displayTicker.contains(input, true) ||
            name.contains(input, true)

    private fun initialiseAccountSelectorWithHeader() {
        with(binding) {
            header.setDetails(
                title = R.string.transfer_receive_crypto_title,
                label = R.string.transfer_receive_crypto_label,
                icon = R.drawable.ic_receive_blue_circle,
                showSeparator = false
            )
            model.process(ReceiveIntent.GetAvailableAssets)
        }
    }

    private fun setupSearchBox() {
        binding.searchBoxLayout.setDetails(
            hint = R.string.search_wallets_hint,
            textWatcher = object : AfterTextChangedWatcher() {
                override fun afterTextChanged(s: Editable?) {
                    s?.let { editable ->
                        model.process(ReceiveIntent.FilterAssets(editable.toString()))
                    }
                }
            }
        )
    }

    private fun doOnAccountSelected(account: CryptoAccount) {
        compositeDisposable += account.stateAwareActions
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { stateAwareActions ->
                val receiveAction = stateAwareActions.find { it.action == AssetAction.Receive }
                if (receiveAction?.state == ActionState.Available) {
                    ReceiveDetailSheet.newInstance(account).show(childFragmentManager, BOTTOM_SHEET)
                } else {
                    showBottomSheet(KycUpgradeNowSheet.newInstance())
                }
                analytics.logEvent(
                    TransferAnalyticsEvent.ReceiveAccountSelected(
                        TxFlowAnalyticsAccountType.fromAccount(account),
                        account.currency
                    )
                )
            }
    }

    override fun startKycClicked() {
        KycNavHostActivity.start(requireContext(), CampaignType.None)
    }

    override fun onSheetClosed() {
    }

    companion object {
        const val BOTTOM_SHEET = "BOTTOM_SHEET"

        fun newInstance() = ReceiveFragment()
    }
}
