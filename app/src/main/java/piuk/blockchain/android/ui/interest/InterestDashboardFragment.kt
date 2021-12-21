package piuk.blockchain.android.ui.interest

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.SingleAccount
import com.blockchain.coincore.impl.CryptoInterestAccount
import com.blockchain.core.interest.InterestBalanceDataManager
import com.blockchain.koin.scopedInject
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.models.responses.nabu.KycTierLevel
import com.blockchain.nabu.models.responses.nabu.KycTiers
import com.blockchain.nabu.service.TierService
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.Singles
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.FragmentInterestDashboardBinding
import piuk.blockchain.android.ui.resources.AssetResources
import piuk.blockchain.android.ui.transactionflow.flow.TransactionFlowActivity
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.visible
import timber.log.Timber

class InterestDashboardFragment : Fragment() {

    interface InterestDashboardHost {
        fun startKyc()
        fun showInterestSummarySheet(account: CryptoAccount)
        fun startAccountSelection(filter: Single<List<BlockchainAccount>>, toAccount: SingleAccount)
    }

    val host: InterestDashboardHost by lazy {
        activity as? InterestDashboardHost ?: throw IllegalStateException(
            "Host fragment is not a InterestDashboardFragment.InterestDashboardHost"
        )
    }

    private var _binding: FragmentInterestDashboardBinding? = null
    private val binding: FragmentInterestDashboardBinding
        get() = _binding!!

    private val compositeDisposable = CompositeDisposable()
    private val custodialWalletManager: CustodialWalletManager by scopedInject()
    private val interestBalances: InterestBalanceDataManager by scopedInject()
    private val kycTierService: TierService by scopedInject()
    private val coincore: Coincore by scopedInject()
    private val assetResources: AssetResources by inject()

    private val listAdapter: InterestDashboardAdapter by lazy {
        InterestDashboardAdapter(
            assetResources = assetResources,
            disposables = compositeDisposable,
            custodialWalletManager = custodialWalletManager,
            interestBalance = interestBalances,
            verificationClicked = ::startKyc,
            itemClicked = ::interestItemClicked
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInterestDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.interestDashboardList.apply {
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
            adapter = listAdapter
        }

        loadInterestDetails()
    }

    private fun loadInterestDetails() {
        compositeDisposable +=
            Singles.zip(
                kycTierService.tiers(),
                custodialWalletManager.getInterestEnabledAssets()
            ).observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe {
                    binding.interestError.gone()
                    binding.interestDashboardProgress.visible()
                }
                .subscribeBy(
                    onSuccess = { (tiers, enabledAssets) ->
                        renderInterestDetails(tiers, enabledAssets)
                    },
                    onError = {
                        renderErrorState()
                        Timber.e("Error loading interest summary details $it")
                    }
                )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        compositeDisposable.clear()
        _binding = null
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun renderInterestDetails(
        tiers: KycTiers,
        enabledAssets: List<AssetInfo>
    ) {
        val items = mutableListOf<InterestDashboardItem>()

        val isKycGold = tiers.isApprovedFor(KycTierLevel.GOLD)
        if (!isKycGold) {
            items.add(InterestIdentityVerificationItem)
        }

        // we load balance per item, so at least ensure some consistency when loading the list
        enabledAssets.sortedBy {
            it.name
        }.map {
            items.add(InterestAssetInfoItem(isKycGold, it))
        }

        listAdapter.items = items
        listAdapter.notifyDataSetChanged()

        with(binding) {
            interestDashboardProgress.gone()
            interestDashboardList.visible()
        }
    }

    private fun renderErrorState() {
        with(binding) {
            interestDashboardList.gone()
            interestDashboardProgress.gone()

            interestError.setDetails(
                title = R.string.rewards_error_title,
                description = R.string.rewards_error_desc,
                contactSupportEnabled = true
            ) {
                loadInterestDetails()
            }
            interestError.visible()
        }
    }

    fun refreshBalances() {
        // force redraw, so balances update
        listAdapter.notifyDataSetChanged()
    }

    private fun interestItemClicked(cryptoCurrency: AssetInfo, hasBalance: Boolean) {
        compositeDisposable += coincore[cryptoCurrency].accountGroup(AssetFilter.Interest).subscribe {
            val interestAccount = it.accounts.first() as CryptoInterestAccount
            if (hasBalance) {
                host.showInterestSummarySheet(interestAccount)
            } else {
                startActivity(
                    TransactionFlowActivity.newInstance(
                        context = requireContext(),
                        target = it.accounts.first(),
                        action = AssetAction.InterestDeposit
                    )
                )
            }
        }
    }

    private fun startKyc() {
        host.startKyc()
    }

    companion object {
        fun newInstance(): InterestDashboardFragment = InterestDashboardFragment()
    }
}
