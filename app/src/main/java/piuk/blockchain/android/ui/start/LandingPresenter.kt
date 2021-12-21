package piuk.blockchain.android.ui.start

import com.blockchain.coincore.loader.AssetCatalogueImpl
import com.blockchain.componentlib.price.PriceView
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.nabu.datamanagers.ApiStatus
import com.blockchain.preferences.SecurityPrefs
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.FiatCurrency.Companion.Dollars
import info.blockchain.balance.isCustodial
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import piuk.blockchain.android.ui.base.MvpPresenter
import piuk.blockchain.android.ui.base.MvpView
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.util.RootUtil
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import timber.log.Timber

interface LandingView : MvpView {
    fun showToast(message: String, @ToastCustom.ToastType toastType: String)
    fun showIsRootedWarning()
    fun showApiOutageMessage()
    fun onLoadPrices(assets: List<PriceView.Price>)
}

class LandingPresenter(
    private val environmentSettings: EnvironmentConfig,
    private val prefs: SecurityPrefs,
    private val rootUtil: RootUtil,
    private val apiStatus: ApiStatus,
    private val assetCatalogue: AssetCatalogueImpl,
    private val exchangeRatesDataManager: ExchangeRatesDataManager
) : MvpPresenter<LandingView>() {

    override val alwaysDisableScreenshots = false
    override val enableLogoutTimer = false

    var assetInfo: Map<String, AssetInfo> = emptyMap()
    val priceInfo: MutableMap<String, PriceView.Price> = mutableMapOf()

    override fun onViewAttached() {
        if (environmentSettings.isRunningInDebugMode()) {
            view?.showToast(
                "Current environment: ${environmentSettings.environment.getName()}",
                ToastCustom.TYPE_GENERAL
            )
        }
        checkApiStatus()
    }

    fun loadAssets() {
        compositeDisposable += assetCatalogue.initialise()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy { currencies ->
                val assets = currencies.filterIsInstance<AssetInfo>().filter {
                    it.isCustodial
                }

                assetInfo = assets.associateBy {
                    it.networkTicker
                }

                assets.forEach { assetInfo ->
                    priceInfo[assetInfo.networkTicker] = PriceView.Price(
                        icon = assetInfo.logo,
                        name = assetInfo.name,
                        displayTicker = assetInfo.displayTicker,
                        networkTicker = assetInfo.networkTicker
                    )
                }

                val priceList = priceInfo.values.toList()
                priceList.take(NUM_INITIAL_PRICES).forEach { price ->
                    getPrices(price)
                }
                view?.onLoadPrices(priceList)
            }
    }

    fun getPrices(price: PriceView.Price) {
        assetInfo[price.networkTicker]?.let {
            compositeDisposable += exchangeRatesDataManager.getPricesWith24hDelta(it, Dollars)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onNext = {
                        val currentPrice = priceInfo[price.networkTicker] ?: return@subscribeBy

                        priceInfo[price.networkTicker] = currentPrice.copy(
                            price = it.currentRate.price.toStringWithSymbol(),
                            gain = if (!it.delta24h.isNaN()) {
                                it.delta24h
                            } else {
                                0.0
                            }
                        )
                        view?.onLoadPrices(priceInfo.values.toList())
                    }
                )
        }
    }

    private fun checkApiStatus() {
        compositeDisposable += apiStatus.isHealthy()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onSuccess = { isHealthy ->
                if (isHealthy.not())
                    view?.showApiOutageMessage()
            }, onError = {
                Timber.e(it)
            })
    }

    override fun onViewDetached() { /* no-op */
    }

    internal fun checkForRooted() {
        if (rootUtil.isDeviceRooted && !prefs.disableRootedWarning) {
            view?.showIsRootedWarning()
        }
    }

    companion object {
        private const val NUM_INITIAL_PRICES = 10
        private const val DEFAULT_FIAT = "USD"
    }
}
