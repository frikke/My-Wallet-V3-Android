package piuk.blockchain.android.ui.brokerage

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.blockchain.analytics.Analytics
import com.blockchain.api.NabuApiException
import com.blockchain.api.NabuApiExceptionFactory
import com.blockchain.coincore.AssetAction
import com.blockchain.commonarch.presentation.base.SlidingModalBottomDialog
import com.blockchain.commonarch.presentation.base.trackProgress
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.domain.common.model.BuySellViewType
import com.blockchain.koin.payloadScope
import com.blockchain.presentation.koin.scopedInject
import com.blockchain.utils.unsafeLazy
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import org.koin.android.ext.android.inject
import piuk.blockchain.android.databinding.FragmentBuySellBinding
import piuk.blockchain.android.simplebuy.ClientErrorAnalytics
import piuk.blockchain.android.simplebuy.SimpleBuyActivity
import piuk.blockchain.android.simplebuy.SimpleBuySyncFactory
import piuk.blockchain.android.support.SupportCentreActivity
import piuk.blockchain.android.ui.base.showFragment
import piuk.blockchain.android.ui.brokerage.buy.BuySelectAssetFragment
import piuk.blockchain.android.ui.brokerage.sell.SellIntroFragment
import piuk.blockchain.android.ui.home.HomeNavigator
import piuk.blockchain.android.ui.home.HomeScreenFragment
import piuk.blockchain.android.ui.home.WalletClientAnalytics
import piuk.blockchain.android.ui.transactionflow.flow.TransactionFlowActivity
import piuk.blockchain.android.util.AppUtil
import retrofit2.HttpException

class BuySellFragment :
    HomeScreenFragment,
    Fragment(),
    SellIntroFragment.SellIntroHost,
    SlidingModalBottomDialog.Host {

    private var _binding: FragmentBuySellBinding? = null
    private val binding: FragmentBuySellBinding
        get() = _binding!!

    private val compositeDisposable = CompositeDisposable()
    private val appUtil: AppUtil by inject()
    private val analytics: Analytics by inject()
    private val simpleBuySync: SimpleBuySyncFactory by scopedInject()
    private val assetCatalogue: AssetCatalogue by inject()

    private val buySellFlowNavigator: BuySellFlowNavigator
        get() = payloadScope.get()

    private val showView: BuySellViewType by unsafeLazy {
        arguments?.getSerializable(VIEW_TYPE) as? BuySellViewType
            ?: BuySellViewType.TYPE_BUY
    }

    private val selectedAsset: AssetInfo? by unsafeLazy {
        arguments?.getString(SELECTED_ASSET)?.let {
            assetCatalogue.assetInfoFromNetworkTicker(it)
        }
    }

    private var hasReturnedFromBuyActivity = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBuySellBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        analytics.logEvent(WalletClientAnalytics.WalletBuySellViewed)
    }

    override fun onResume() {
        super.onResume()
        if (isHidden) return
        subscribeForNavigation()
    }

    private fun subscribeForNavigation(showLoader: Boolean = true) {
        val activityIndicator = if (showLoader) appUtil.activityIndicator else null
        compositeDisposable +=
            simpleBuySync.performSync()
                .onErrorComplete()
                .toSingleDefault(false)
                .flatMap {
                    buySellFlowNavigator.navigateTo(selectedAsset)
                }
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe {
                    binding.buySellEmpty.gone()
                }
                .trackProgress(activityIndicator)
                .subscribeBy(
                    onSuccess = { introAction ->
                        renderBuySellFragments(introAction)
                    },
                    onError = { exception ->
                        renderErrorState()
                        val nabuException: NabuApiException? = (exception as? HttpException)?.let { httpException ->
                            NabuApiExceptionFactory.fromResponseBody(httpException)
                        }

                        analytics.logEvent(
                            ClientErrorAnalytics.ClientLogError(
                                nabuApiException = nabuException,
                                errorDescription = exception.message,
                                error = if (exception is HttpException) {
                                    ClientErrorAnalytics.NABU_ERROR
                                } else ClientErrorAnalytics.UNKNOWN_ERROR,
                                source = if (exception is HttpException) {
                                    ClientErrorAnalytics.Companion.Source.NABU
                                } else {
                                    ClientErrorAnalytics.Companion.Source.CLIENT
                                },
                                title = ClientErrorAnalytics.OOPS_ERROR,
                                action = ClientErrorAnalytics.ACTION_SELL,
                                categories = nabuException?.getServerSideErrorInfo()?.categories ?: emptyList()
                            )
                        )
                    }
                )
    }

    private fun renderBuySellFragments(
        action: BuySellIntroAction?
    ) {
        with(binding) {
            buySellEmpty.gone()
            when (action) {
                BuySellIntroAction.DisplayBuySellIntro -> {
                    renderBuySellUi()
                }

                BuySellIntroAction.UserNotEligible -> renderNotEligibleUi()
                is BuySellIntroAction.StartBuyWithSelectedAsset -> {
                    renderBuySellUi()
                    if (!hasReturnedFromBuyActivity) {
                        hasReturnedFromBuyActivity = false
                        startActivityForResult(
                            SimpleBuyActivity.newIntent(
                                context = activity as Context,
                                asset = action.selectedAsset,
                                launchFromNavigationBar = true
                            ),
                            SB_ACTIVITY
                        )
                    } else {
                        SimpleBuyActivity.newIntent(
                            context = activity as Context,
                            asset = action.selectedAsset,
                            launchFromNavigationBar = true
                        )
                    }
                }

                else -> startActivity(
                    SimpleBuyActivity.newIntent(
                        context = activity as Context,
                        launchFromNavigationBar = true,
                        launchKycResume = false
                    )
                )
            }
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            subscribeForNavigation()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SB_ACTIVITY && resultCode == Activity.RESULT_CANCELED) {
            hasReturnedFromBuyActivity = true
        }
    }

    private fun renderErrorState() {
        with(binding) {
            buySellFragmentContainer.gone()
            buySellEmpty.setDetails(
                action = ::subscribeForNavigation,
                onContactSupport = { requireContext().startActivity(SupportCentreActivity.newIntent(requireContext())) }
            )
            buySellEmpty.visible()
        }
    }

    private fun renderNotEligibleUi() {
        with(binding) {
            buySellFragmentContainer.gone()
            notEligibleIcon.visible()
            notEligibleTitle.visible()
            notEligibleDescription.visible()
        }
    }

    private fun renderBuySellUi() {
        with(binding) {
            showBuyOrSell(showView)
            buySellFragmentContainer.visible()
            notEligibleIcon.gone()
            notEligibleTitle.gone()
            notEligibleDescription.gone()
        }
    }

    private fun showBuyOrSell(view: BuySellViewType) {
        if (view == BuySellViewType.TYPE_SELL) {
            startActivity(
                TransactionFlowActivity.newIntent(
                    context = requireActivity(),
                    action = AssetAction.Sell,
                    origin = "",
                )
            )
            requireActivity().finish()
            return
        }
        childFragmentManager.showFragment(
            fragment =
            BuySelectAssetFragment.newInstance(
                fromRecurringBuy = arguments?.getBoolean(ARG_FROM_RECURRING_BUY) ?: false
            ),
            containerId = binding.buySellFragmentContainer.id,
            reloadFragment = false
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        compositeDisposable.clear()
        _binding = null
    }

    companion object {
        private const val VIEW_TYPE = "VIEW_TYPE"
        private const val SELECTED_ASSET = "SELECTED_ASSET"
        private const val SB_ACTIVITY = 321
        private const val ARG_FROM_RECURRING_BUY = "ARG_FROM_RECURRING_BUY"

        fun newInstance(
            asset: AssetInfo? = null,
            viewType: BuySellViewType = BuySellViewType.TYPE_BUY,
            fromRecurringBuy: Boolean = false
        ) = BuySellFragment().apply {
            arguments = Bundle().apply {
                putSerializable(VIEW_TYPE, viewType)
                asset?.let {
                    putString(SELECTED_ASSET, it.networkTicker)
                }
                putBoolean(ARG_FROM_RECURRING_BUY, fromRecurringBuy)
            }
        }
    }

    override fun onSheetClosed() = subscribeForNavigation(showLoader = false)

    override fun onSellFinished() = subscribeForNavigation(showLoader = false)

    override fun onSellInfoClicked() = navigator().launchBuySell(BuySellViewType.TYPE_SELL)

    override fun onSellListEmptyCta() {
        // TOOD
    }

    override fun navigator(): HomeNavigator =
        (activity as? HomeNavigator) ?: throw IllegalStateException("Parent must implement HomeNavigator")
}
