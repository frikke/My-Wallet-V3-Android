package piuk.blockchain.android.ui.sell

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockchain.analytics.Analytics
import com.blockchain.api.NabuApiException
import com.blockchain.api.NabuApiExceptionFactory
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.Coincore
import com.blockchain.commonarch.presentation.base.trackProgress
import com.blockchain.core.price.ExchangeRate
import com.blockchain.koin.scopedInject
import com.blockchain.nabu.BlockedReason
import com.blockchain.nabu.Feature
import com.blockchain.nabu.FeatureAccess
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Money
import info.blockchain.balance.asAssetInfoOrThrow
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.databinding.BuyIntroFragmentBinding
import piuk.blockchain.android.simplebuy.ClientErrorAnalytics
import piuk.blockchain.android.simplebuy.SimpleBuyActivity
import piuk.blockchain.android.simplebuy.sheets.BuyPendingOrdersBottomSheet
import piuk.blockchain.android.ui.base.ViewPagerFragment
import piuk.blockchain.android.ui.customviews.IntroHeaderView
import piuk.blockchain.android.ui.customviews.account.HeaderDecoration
import piuk.blockchain.android.ui.dashboard.sheets.KycUpgradeNowSheet
import piuk.blockchain.android.ui.home.HomeNavigator
import piuk.blockchain.android.ui.home.HomeScreenFragment
import piuk.blockchain.android.ui.resources.AssetResources
import piuk.blockchain.android.urllinks.URL_RUSSIA_SANCTIONS_EU5
import piuk.blockchain.android.util.openUrl
import retrofit2.HttpException

class BuyIntroFragment :
    ViewPagerFragment(),
    BuyPendingOrdersBottomSheet.Host,
    HomeScreenFragment,
    KycUpgradeNowSheet.Host {

    private var _binding: BuyIntroFragmentBinding? = null
    private val binding: BuyIntroFragmentBinding
        get() = _binding!!

    private var isInitialLoading = true

    private val custodialWalletManager: CustodialWalletManager by scopedInject()
    private val compositeDisposable = CompositeDisposable()
    private val coincore: Coincore by scopedInject()
    private val assetResources: AssetResources by inject()
    private val analytics: Analytics by inject()
    private val userIdentity: UserIdentity by scopedInject()

    private val buyAdapter = BuyCryptoCurrenciesAdapter(
        assetResources = assetResources,
        onItemClick = ::onItemClick
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BuyIntroFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val introHeaderView = IntroHeaderView(requireContext())
        introHeaderView.setDetails(
            icon = R.drawable.ic_cart,
            label = R.string.select_crypto_you_want,
            title = R.string.buy_with_cash
        )
        with(binding.rvCryptos) {
            addItemDecoration(
                HeaderDecoration.with(requireContext())
                    .parallax(0.5f)
                    .setView(introHeaderView)
                    .build()
            )
            layoutManager = LinearLayoutManager(activity)
            adapter = buyAdapter
        }
    }

    override fun onResume() {
        checkEligibilityAndLoadBuyDetails(isInitialLoading)
        isInitialLoading = false
        super.onResume()
    }

    private fun checkEligibilityAndLoadBuyDetails(showLoading: Boolean = true) {
        compositeDisposable +=
            userIdentity.userAccessForFeature(Feature.Buy)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .trackProgress(activityIndicator.takeIf { showLoading })
                .subscribeBy(
                    onSuccess = { eligibility ->
                        when (val reason = (eligibility as? FeatureAccess.Blocked)?.reason) {
                            BlockedReason.NotEligible,
                            is BlockedReason.InsufficientTier -> renderKycUpgradeNow()
                            is BlockedReason.Sanctions -> renderBlockedDueToSanctions(reason)
                            is BlockedReason.TooManyInFlightTransactions,
                            null -> loadBuyDetails(showLoading)
                        }
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
                                error = ClientErrorAnalytics.NABU_ERROR,
                                source = if (exception is HttpException) {
                                    ClientErrorAnalytics.Companion.Source.NABU
                                } else {
                                    ClientErrorAnalytics.Companion.Source.CLIENT
                                },
                                title = ClientErrorAnalytics.OOPS_ERROR,
                                action = ClientErrorAnalytics.ACTION_BUY,
                                categories = nabuException?.getServerSideErrorInfo()?.categories ?: emptyList()
                            )
                        )
                    }
                )
    }

    private fun loadBuyDetails(showLoading: Boolean = true) {
        compositeDisposable += custodialWalletManager.getSupportedBuySellCryptoCurrencies()
            .map { pairs ->
                pairs.map {
                    it.source
                }.distinct()
                    .filterIsInstance<AssetInfo>()
            }.flatMapObservable { assets ->
                Observable.fromIterable(assets).flatMapMaybe { asset ->
                    coincore[asset].getPricesWith24hDelta().map { priceDelta ->
                        PricedAsset(
                            asset = asset,
                            priceHistory = PriceHistory(
                                currentExchangeRate = priceDelta.currentRate,
                                priceDelta = priceDelta.delta24h
                            )
                        )
                    }.toMaybe().onErrorResumeNext {
                        Maybe.empty()
                    }
                }
            }.toList()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .trackProgress(activityIndicator.takeIf { showLoading })
            .doOnSubscribe {
                buyAdapter.items = emptyList()
            }
            .subscribeBy(
                onSuccess = { items ->
                    renderBuyIntro(items)
                },
                onError = { renderErrorState() },
            )
    }

    private fun onItemClick(item: BuyCryptoItem) {
        compositeDisposable += userIdentity.userAccessForFeature(Feature.Buy).subscribeBy { accessState ->
            val blockedReason = (accessState as? FeatureAccess.Blocked)?.reason
            if (blockedReason is BlockedReason.TooManyInFlightTransactions) {
                showPendingOrdersBottomSheet(blockedReason.maxTransactions)
            } else {
                startActivity(
                    SimpleBuyActivity.newIntent(
                        activity as Context,
                        item.asset,
                        launchFromNavigationBar = true,
                        launchKycResume = false
                    )
                )
            }
        }
    }

    private fun showPendingOrdersBottomSheet(maxTransactions: Int) {
        BuyPendingOrdersBottomSheet.newInstance(maxTransactions).show(
            childFragmentManager,
            BuyPendingOrdersBottomSheet.TAG
        )
    }

    private fun renderKycUpgradeNow() {
        if (childFragmentManager.findFragmentById(R.id.fragment_container) == null) {
            childFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, KycUpgradeNowSheet.newInstance())
                .commitAllowingStateLoss()
        }
        binding.viewFlipper.displayedChild = ViewFlipperItem.KYC.ordinal
    }

    private fun renderBlockedDueToSanctions(reason: BlockedReason.Sanctions) {
        with(binding) {
            viewFlipper.displayedChild = ViewFlipperItem.EMPTY_STATE.ordinal
            customEmptyState.apply {
                title = R.string.account_restricted
                descriptionText = when (reason) {
                    BlockedReason.Sanctions.RussiaEU5 -> getString(R.string.russia_sanctions_eu5_sheet_subtitle)
                    is BlockedReason.Sanctions.Unknown -> reason.message
                }
                icon = R.drawable.ic_wallet_intro_image
                ctaText = R.string.learn_more
                ctaAction = { requireContext().openUrl(URL_RUSSIA_SANCTIONS_EU5) }
            }
        }
    }

    private fun renderBuyIntro(
        pricesAssets: List<PricedAsset>
    ) {
        binding.viewFlipper.displayedChild = ViewFlipperItem.INTRO.ordinal
        buyAdapter.items =
            pricesAssets.map { pricedAsset ->
                BuyCryptoItem(
                    asset = pricedAsset.asset,
                    price = pricedAsset.priceHistory
                        .currentExchangeRate
                        .price,
                    percentageDelta = pricedAsset.priceHistory.percentageDelta
                )
            }
    }

    private fun renderErrorState() {
        with(binding) {
            viewFlipper.displayedChild = ViewFlipperItem.ERROR.ordinal
            buyEmpty.setDetails {
                checkEligibilityAndLoadBuyDetails()
            }
        }
    }

    override fun startKycClicked() {
        navigator().launchKyc(CampaignType.SimpleBuy)
    }

    override fun onDestroyView() {
        compositeDisposable.clear()
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private enum class ViewFlipperItem {
            INTRO,
            ERROR,
            KYC,
            EMPTY_STATE
        }

        fun newInstance() = BuyIntroFragment()
    }

    override fun startActivityRequested() {
        navigator().performAssetActionFor(AssetAction.ViewActivity)
    }

    override fun onSheetClosed() {
        // do nothing
    }

    override fun navigator(): HomeNavigator =
        (activity as? HomeNavigator) ?: throw IllegalStateException("Parent must implement HomeNavigator")

    override fun onBackPressed(): Boolean = false
}

data class PriceHistory(
    val currentExchangeRate: ExchangeRate,
    val priceDelta: Double
) {
    val cryptoCurrency: AssetInfo
        get() = currentExchangeRate.from.asAssetInfoOrThrow()

    val percentageDelta: Double
        get() = priceDelta
}

data class BuyCryptoItem(
    val asset: AssetInfo,
    val price: Money,
    val percentageDelta: Double
)

private data class PricedAsset(
    val asset: AssetInfo,
    val priceHistory: PriceHistory
)
