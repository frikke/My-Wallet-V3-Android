package piuk.blockchain.android.ui.dashboard.coinview

import com.blockchain.commonarch.presentation.mvi.MviModel
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
            is CoinViewIntent.LoadAssetInformation -> {
                interactor.loadAssetDetails(intent.assetTicker)
                    .subscribeBy(
                        onSuccess = { (asset, displayMap) ->
                            process(CoinViewIntent.AssetInfoLoaded(asset, displayMap))
                        },
                        onError = {
                        }
                    )
            }
            CoinViewIntent.ResetErrorState,
            CoinViewIntent.ResetViewState,
            is CoinViewIntent.AssetInfoLoaded,
            is CoinViewIntent.UpdateErrorState,
            is CoinViewIntent.UpdateViewState -> null
        }
}
