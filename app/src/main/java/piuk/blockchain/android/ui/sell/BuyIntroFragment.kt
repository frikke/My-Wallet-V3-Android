package piuk.blockchain.android.ui.sell

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.Coincore
import com.blockchain.commonarch.presentation.base.trackProgress
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.core.price.ExchangeRate
import com.blockchain.extensions.exhaustive
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
import piuk.blockchain.android.databinding.BuyIntroFragmentBinding
import piuk.blockchain.android.simplebuy.SimpleBuyActivity
import piuk.blockchain.android.simplebuy.sheets.BuyPendingOrdersBottomSheet
import piuk.blockchain.android.ui.base.ViewPagerFragment
import piuk.blockchain.android.ui.customviews.IntroHeaderView
import piuk.blockchain.android.ui.customviews.account.HeaderDecoration
import piuk.blockchain.android.ui.home.HomeNavigator
import piuk.blockchain.android.ui.home.HomeScreenFragment
import piuk.blockchain.android.ui.resources.AssetResources

class BuyIntroFragment : ViewPagerFragment(), BuyPendingOrdersBottomSheet.Host, HomeScreenFragment {

    private var _binding: BuyIntroFragmentBinding? = null
    private val binding: BuyIntroFragmentBinding
        get() = _binding!!

    private val custodialWalletManager: CustodialWalletManager by scopedInject()
    private val compositeDisposable = CompositeDisposable()
    private val coincore: Coincore by scopedInject()
    private val assetResources: AssetResources by inject()
    private val userIdentity: UserIdentity by scopedInject()

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
        loadBuyDetails()
        val introHeaderView = IntroHeaderView(requireContext())
        introHeaderView.setDetails(
            icon = R.drawable.ic_cart,
            label = R.string.select_crypto_you_want,
            title = R.string.buy_with_cash
        )
        binding.rvCryptos.addItemDecoration(
            HeaderDecoration.with(requireContext())
                .parallax(0.5f)
                .setView(introHeaderView)
                .build()
        )
        binding.rvCryptos.layoutManager = LinearLayoutManager(activity)
        binding.rvCryptos.adapter = adapter
    }

    override fun onResume() {
        loadBuyDetails(false)
        super.onResume()
    }

    private fun loadBuyDetails(showLoading: Boolean = true) {

        custodialWalletManager.getSupportedBuySellCryptoCurrencies().map { pairs ->
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
                binding.buyEmpty.gone()
                adapter.items = emptyList()
            }
            .subscribeBy(
                onSuccess = { items ->
                    renderBuyIntro(
                        items
                    )
                },
                onError = { renderErrorState() },
            )
    }

    private val adapter = BuyCryptoCurrenciesAdapter(
        assetResources = assetResources,
        onItemClick = ::onItemClick
    )

    private fun onItemClick(item: BuyCryptoItem) {
        compositeDisposable += userIdentity.userAccessForFeature(Feature.SimpleBuy).subscribeBy { accessState ->
            val blockedState = accessState as? FeatureAccess.Blocked
            blockedState?.let {
                when (val reason = it.reason) {
                    is BlockedReason.TooManyInFlightTransactions -> showPendingOrdersBottomSheet(
                        reason.maxTransactions
                    )
                    BlockedReason.NotEligible -> throw IllegalStateException("Buy should not be accessible")
                }.exhaustive
            } ?: run {
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

    private fun renderBuyIntro(
        pricesAssets: List<PricedAsset>
    ) {
        with(binding) {
            rvCryptos.visible()
            buyEmpty.gone()
            adapter.items =
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
    }

    private fun renderErrorState() {
        with(binding) {
            rvCryptos.gone()
            buyEmpty.setDetails {
                loadBuyDetails()
            }
            buyEmpty.visible()
        }
    }

    override fun onDestroyView() {
        compositeDisposable.clear()
        super.onDestroyView()
        _binding = null
    }

    companion object {
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
