package piuk.blockchain.android.ui.dashboard.coinview

import com.blockchain.charts.ChartEntry
import com.blockchain.commonarch.presentation.mvi.MviModel
import com.blockchain.core.price.HistoricalTimeSpan
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.logging.CrashLogger
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy

class CoinViewModel(
    initialState: CoinViewState,
    mainScheduler: Scheduler,
    private val interactor: CoinViewInteractor,
    environmentConfig: EnvironmentConfig,
    crashLogger: CrashLogger
) : MviModel<CoinViewState, CoinViewIntent>(
    initialState,
    mainScheduler,
    environmentConfig,
    crashLogger
) {
    override fun performAction(previousState: CoinViewState, intent: CoinViewIntent): Disposable? =
        when (intent) {
            is CoinViewIntent.LoadAsset -> {
                interactor.loadAssetDetails(intent.assetTicker)?.let {
                    process(CoinViewIntent.AssetLoaded(it))
                    process(CoinViewIntent.LoadAssetInformation(it))
                    process(CoinViewIntent.LoadAssetChart(it))
                } ?: process(CoinViewIntent.UpdateErrorState(CoinViewError.UnknownAsset))
                null
            }
            is CoinViewIntent.LoadAssetInformation -> {
                interactor.loadAccountDetails(intent.asset)
                    .subscribeBy(
                        onSuccess = { displayMap ->
                            process(CoinViewIntent.UpdateViewState(CoinViewViewState.ShowAccountInfo(displayMap)))
                        },
                        onError = {
                            process(CoinViewIntent.UpdateErrorState(CoinViewError.WalletLoadError))
                        }
                    )
            }
            is CoinViewIntent.LoadAssetChart -> {
                interactor.loadHistoricPrices(intent.asset, HistoricalTimeSpan.DAY)
                    .subscribeBy(
                        onSuccess = { list ->
                            process(
                                CoinViewIntent.UpdateViewState(
                                    CoinViewViewState.ShowChartInfo(
                                        list.map { point ->
                                            ChartEntry(
                                                point.timestamp.toFloat(),
                                                point.rate.toFloat()
                                            )
                                        }
                                    )
                                )
                            )
                        },
                        onError = {
                            process(CoinViewIntent.UpdateErrorState(CoinViewError.ChartLoadError))
                        }
                    )
            }
            is CoinViewIntent.LoadNewChartPeriod ->
                previousState.asset?.let {
                    interactor.loadHistoricPrices(it, intent.timePeriod)
                        .subscribeBy(
                            onSuccess = { list ->
                                process(
                                    CoinViewIntent.UpdateViewState(
                                        CoinViewViewState.ShowChartInfo(
                                            list.map { point ->
                                                ChartEntry(
                                                    point.timestamp.toFloat(),
                                                    point.rate.toFloat()
                                                )
                                            }
                                        )
                                    )
                                )
                            },
                            onError = {
                                process(CoinViewIntent.UpdateErrorState(CoinViewError.ChartLoadError))
                            }
                        )
                }
            CoinViewIntent.ResetErrorState,
            CoinViewIntent.ResetViewState,
            is CoinViewIntent.UpdateErrorState,
            is CoinViewIntent.UpdateViewState,
            is CoinViewIntent.AssetLoaded -> null
        }
}
