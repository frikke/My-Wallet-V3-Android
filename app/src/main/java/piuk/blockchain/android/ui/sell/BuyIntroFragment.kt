package piuk.blockchain.android.ui.sell

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockchain.coincore.Coincore
import com.blockchain.core.price.ExchangeRate
import com.blockchain.koin.scopedInject
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Money
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.BuyIntroFragmentBinding
import piuk.blockchain.android.simplebuy.SimpleBuyActivity
import piuk.blockchain.android.ui.base.ViewPagerFragment
import piuk.blockchain.android.ui.customviews.IntroHeaderView
import piuk.blockchain.android.ui.customviews.account.HeaderDecoration
import piuk.blockchain.android.ui.resources.AssetResources
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.trackProgress
import piuk.blockchain.android.util.visible

class BuyIntroFragment : ViewPagerFragment() {

    private var _binding: BuyIntroFragmentBinding? = null
    private val binding: BuyIntroFragmentBinding
        get() = _binding!!

    private val custodialWalletManager: CustodialWalletManager by scopedInject()
    private val compositeDisposable = CompositeDisposable()
    private val coincore: Coincore by scopedInject()
    private val assetResources: AssetResources by inject()

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
        compositeDisposable +=
            custodialWalletManager.getSupportedBuySellCryptoCurrencies().map { pairs ->
                pairs.map { it.source }.distinct()
            }.flatMap { assets ->
                Single.zip(
                    assets.map { asset ->
                        coincore[asset].getPricesWith24hDelta().map { priceDelta ->
                            PriceHistory(
                                currentExchangeRate = priceDelta.currentRate as ExchangeRate.CryptoToFiat,
                                priceDelta = priceDelta.delta24h
                            )
                        }
                    }
                ) { t: Array<Any> ->
                    t.map { it as PriceHistory } to assets
                }
            }.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe {
                    binding.buyEmpty.gone()
                }
                .trackProgress(activityIndicator.takeIf { showLoading })
                .subscribeBy(
                    onSuccess = { (exchangeRates, assets) ->
                        renderBuyIntro(assets, exchangeRates)
                    },
                    onError = {
                        renderErrorState()
                    }
                )
    }

    private val adapter = BuyCryptoCurrenciesAdapter(
        assetResources = assetResources,
        onItemClick = {
            startActivity(
                SimpleBuyActivity.newIntent(
                    activity as Context,
                    it.asset,
                    launchFromNavigationBar = true,
                    launchKycResume = false
                )
            )
        }
    )

    private fun renderBuyIntro(
        assets: List<AssetInfo>,
        pricesHistory: List<PriceHistory>
    ) {
        with(binding) {
            rvCryptos.visible()
            buyEmpty.gone()
            adapter.items =
                assets.map { asset ->
                    BuyCryptoItem(
                        asset = asset,
                        price = pricesHistory.first { it.cryptoCurrency == asset }
                            .currentExchangeRate
                            .price(),
                        percentageDelta = pricesHistory.first {
                            it.cryptoCurrency == asset
                        }.percentageDelta
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
}

data class PriceHistory(
    val currentExchangeRate: ExchangeRate.CryptoToFiat,
    val priceDelta: Double
) {
    val cryptoCurrency: AssetInfo
        get() = currentExchangeRate.from

    val percentageDelta: Double
        get() = priceDelta
}

data class BuyCryptoItem(
    val asset: AssetInfo,
    val price: Money,
    val percentageDelta: Double
)
