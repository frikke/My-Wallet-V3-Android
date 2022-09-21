package piuk.blockchain.android.ui.start

import com.blockchain.coincore.loader.AssetCatalogueImpl
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.componentlib.price.PriceView
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.core.price.Prices24HrWithDelta
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.nabu.datamanagers.ApiStatus
import com.blockchain.preferences.OnboardingPrefs
import com.blockchain.preferences.SecurityPrefs
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.FiatCurrency.Companion.Dollars
import info.blockchain.balance.isCustodial
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.kotlin.merge
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import piuk.blockchain.android.ui.base.MvpPresenter
import piuk.blockchain.android.ui.base.MvpView
import piuk.blockchain.android.util.RootUtil
import timber.log.Timber

interface LandingView : MvpView {
    fun showSnackbar(message: String, type: SnackbarType)
    fun showLandingCta()
    fun showIsRootedWarning()
    fun showApiOutageMessage()
    fun onLoadPrices(assets: List<PriceView.Price>)
}

class LandingPresenter(
    private val environmentSettings: EnvironmentConfig,
    private val prefs: SecurityPrefs,
    private val onboardingPrefs: OnboardingPrefs,
    private val rootUtil: RootUtil,
    private val apiStatus: ApiStatus,
    private val assetCatalogue: AssetCatalogueImpl,
    private val exchangeRatesDataManager: ExchangeRatesDataManager
) : MvpPresenter<LandingView>() {

    override val alwaysDisableScreenshots = false
    override val enableLogoutTimer = false

    private val statePricesViews = ConcurrentHashMap<String, PriceView.Price>()

    override fun onViewCreated() {}

    override fun onViewAttached() {
        if (environmentSettings.isRunningInDebugMode()) {
            view?.showSnackbar(
                "Current environment: ${environmentSettings.environment.name}",
                SnackbarType.Info
            )
        }
        checkApiStatus()
        loadAssets()
    }

    fun checkShouldShowLandingCta() {
        if (onboardingPrefs.isLandingCtaDismissed) return
        view?.showLandingCta()
    }

    private fun loadAssets() {
        compositeDisposable += assetCatalogue.initialise()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .map { currencies ->
                currencies.filterIsInstance<AssetInfo>().filter {
                    it.isCustodial
                }
            }
            .doOnSuccess { assets ->
                val pricesViews = assets.map { assetInfo ->
                    val view = PriceView.Price(
                        icon = assetInfo.logo,
                        name = assetInfo.name,
                        displayTicker = assetInfo.displayTicker,
                        networkTicker = assetInfo.networkTicker
                    )

                    @Suppress("ReplacePutWithAssignment")
                    statePricesViews.put(assetInfo.networkTicker, view)

                    view
                }

                view?.onLoadPrices(pricesViews)
            }
            .flatMapObservable { assets ->
                getPricesMerged(assets)
            }
            .subscribeBy(
                onNext = { (asset, price) ->
                    val priceView = PriceView.Price(
                        icon = asset.logo,
                        name = asset.name,
                        displayTicker = asset.displayTicker,
                        networkTicker = asset.networkTicker,
                        price = price.currentRate.price.toStringWithSymbol(),
                        gain = if (!price.delta24h.isNaN()) {
                            price.delta24h
                        } else {
                            0.0
                        }
                    )

                    @Suppress("ReplacePutWithAssignment")
                    statePricesViews.put(asset.networkTicker, priceView)

                    view?.onLoadPrices(statePricesViews.values.toList())
                },
                onError = { Timber.e("initialise $it") }
            )
    }

    private fun getPricesMerged(assets: List<AssetInfo>): Observable<Pair<AssetInfo, Prices24HrWithDelta>> {
        if (assets.isEmpty()) return Observable.empty()
        val pricesObservables = assets.map { asset ->
            exchangeRatesDataManager.getPricesWith24hDeltaLegacy(asset, Dollars)
                // Throttling to reduce UI changes
                .throttleLatest(1L, TimeUnit.SECONDS)
                .map { price -> asset to price }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
        }

        // Every 10 seconds we request again the prices for the assets, and kill the old observables by using switchMap rather than flatMap
        return Observable.interval(0L, 10L, TimeUnit.SECONDS).switchMap {
            pricesObservables.merge()
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
}
