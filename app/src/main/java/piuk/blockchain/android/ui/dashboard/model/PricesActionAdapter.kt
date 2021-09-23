package piuk.blockchain.android.ui.dashboard.model

import com.blockchain.core.price.ExchangeRatesDataManager
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import com.blockchain.coincore.Coincore
import piuk.blockchain.android.ui.dashboard.assetdetails.AssetDetailsFlow

internal class PricesActionAdapter(
    private val coincore: Coincore,
    private val exchangeRates: ExchangeRatesDataManager
) {

    fun fetchAvailableAssets(model: PricesModel): Disposable? =
        Single.fromCallable {
            coincore.availableCryptoAssets()
        }.subscribeBy(
            onSuccess = {
                model.process(PricesIntent.AssetListUpdate(it))
            }
        )

    fun fetchAssetPrice(model: PricesModel, asset: AssetInfo): Disposable =
        exchangeRates.getPricesWith24hDelta(asset)
            .subscribeBy(
                onNext = {
                    model.process(
                        PricesIntent.AssetPriceUpdate(
                            asset = asset,
                            price = it
                        )
                    )
                }
            )

    fun getAssetDetailsFlow(model: PricesModel, asset: AssetInfo): Disposable? {
        model.process(
            PricesIntent.UpdateLaunchDetailsFlow(
                AssetDetailsFlow(
                    asset = asset,
                    coincore = coincore
                )
            )
        )
        return null
    }
}
