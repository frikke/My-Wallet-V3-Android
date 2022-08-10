package piuk.blockchain.android.ui.sell

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.ViewPager
import com.blockchain.analytics.Analytics
import com.blockchain.api.NabuApiException
import com.blockchain.api.NabuApiExceptionFactory
import com.blockchain.commonarch.presentation.base.SlidingModalBottomDialog
import com.blockchain.commonarch.presentation.base.trackProgress
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.koin.payloadScope
import com.blockchain.koin.scopedInject
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.FragmentBuySellBinding
import piuk.blockchain.android.simplebuy.ClientErrorAnalytics
import piuk.blockchain.android.simplebuy.SimpleBuyActivity
import piuk.blockchain.android.simplebuy.SimpleBuySyncFactory
import piuk.blockchain.android.ui.home.HomeNavigator
import piuk.blockchain.android.ui.home.HomeScreenFragment
import piuk.blockchain.android.ui.home.WalletClientAnalytics
import piuk.blockchain.android.util.AppUtil
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
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
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentBuySellBinding.inflate(inflater, container, false).apply {
            this.redesignTabLayout.items = listOf(getString(R.string.common_buy), getString(R.string.common_sell))
        }
        return binding.root
    }

    fun goToPage(position: Int) {
        binding.pager.setCurrentItem(position, true)
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
        action: BuySellIntroAction?,
    ) {
        with(binding) {
            buySellEmpty.gone()
            pager.visible()
            when (action) {
                is BuySellIntroAction.DisplayBuySellIntro -> {
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
            pager.gone()
            buySellEmpty.setDetails {
                subscribeForNavigation()
            }
            buySellEmpty.visible()
        }
    }

    private fun renderNotEligibleUi() {
        with(binding) {
            pager.gone()
            notEligibleIcon.visible()
            notEligibleTitle.visible()
            notEligibleDescription.visible()
        }
    }

    private val pagerAdapter: ViewPagerAdapter by lazy {
        ViewPagerAdapter(
            listOf(getString(R.string.common_buy), getString(R.string.common_sell)),
            childFragmentManager
        )
    }

    private fun renderBuySellUi() {
        with(binding) {
            redesignTabLayout.apply {
                visible()
                items = listOf(getString(R.string.common_buy), getString(R.string.common_sell))
                onItemSelected = {
                    pager.setCurrentItem(it, true)
                }
                showBottomShadow = true
            }
            pager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
                override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                    redesignTabLayout.selectedItemIndex = position
                }

                override fun onPageSelected(position: Int) {
                    redesignTabLayout.selectedItemIndex = position
                }

                override fun onPageScrollStateChanged(state: Int) {
                    // do nothing
                }
            })

            if (pager.adapter == null) {
                pager.adapter = pagerAdapter
                when (showView) {
                    BuySellViewType.TYPE_BUY -> pager.setCurrentItem(
                        BuySellViewType.TYPE_BUY.ordinal, true
                    )
                    BuySellViewType.TYPE_SELL -> pager.setCurrentItem(
                        BuySellViewType.TYPE_SELL.ordinal, true
                    )
                }
            }

            pager.visible()
            notEligibleIcon.gone()
            notEligibleTitle.gone()
            notEligibleDescription.gone()
        }
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

        fun newInstance(
            asset: AssetInfo? = null,
            viewType: BuySellViewType = BuySellViewType.TYPE_BUY,
        ) = BuySellFragment().apply {
            arguments = Bundle().apply {
                putSerializable(VIEW_TYPE, viewType)
                asset?.let {
                    putString(SELECTED_ASSET, it.networkTicker)
                }
            }
        }
    }

    enum class BuySellViewType {
        TYPE_BUY,
        TYPE_SELL
    }

    override fun onSheetClosed() = subscribeForNavigation(showLoader = false)

    override fun onSellFinished() = subscribeForNavigation(showLoader = false)

    override fun onSellInfoClicked() = navigator().launchBuySell(BuySellViewType.TYPE_SELL)

    override fun onSellListEmptyCta() {
        binding.pager.setCurrentItem(BuySellViewType.TYPE_BUY.ordinal, true)
    }

    override fun navigator(): HomeNavigator =
        (activity as? HomeNavigator) ?: throw IllegalStateException("Parent must implement HomeNavigator")
}

@SuppressLint("WrongConstant")
internal class ViewPagerAdapter(
    private val titlesList: List<String>,
    fragmentManager: FragmentManager,
) : FragmentStatePagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    override fun getCount(): Int = titlesList.size

    override fun getPageTitle(position: Int): CharSequence =
        titlesList[position]

    override fun getItemPosition(`object`: Any): Int {
        return POSITION_NONE
    }

    override fun getItem(position: Int): Fragment = when (position) {
        0 -> BuyIntroFragment.newInstance()
        else -> SellIntroFragment.newInstance()
    }
}
