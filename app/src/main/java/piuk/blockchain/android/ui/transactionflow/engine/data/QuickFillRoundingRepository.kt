package piuk.blockchain.android.ui.transactionflow.engine.data

import com.blockchain.coincore.AssetAction
import com.blockchain.domain.experiments.RemoteConfigService
import io.reactivex.rxjava3.core.Single
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import piuk.blockchain.android.ui.transactionflow.engine.domain.QuickFillRoundingService
import piuk.blockchain.android.ui.transactionflow.engine.domain.model.QuickFillRoundingData

class QuickFillRoundingRepository(
    private val remoteConfigService: RemoteConfigService,
    private val json: Json
) : QuickFillRoundingService {
    override fun getQuickFillRoundingForAction(action: AssetAction): Single<List<QuickFillRoundingData>> =
        when (action) {
            AssetAction.Sell -> remoteConfigService.getRawJson(CONFIG_SELL_ID).map { data ->
                json.decodeFromString<List<QuickFillRoundingData.SellSwapRoundingData>>(data)
            }
            AssetAction.Swap -> remoteConfigService.getRawJson(CONFIG_SWAP_ID).map { data ->
                json.decodeFromString<List<QuickFillRoundingData.SellSwapRoundingData>>(data)
            }
            AssetAction.Buy -> remoteConfigService.getRawJson(CONFIG_BUY_ID).map { data ->
                json.decodeFromString<List<QuickFillRoundingData.BuyRoundingData>>(data)
            }
            else -> Single.just(emptyList())
        }

    companion object {
        private const val BASE_PATH = "blockchain_app_configuration_transaction"
        private const val END_PATH = "quickfill_configuration"
        private const val CONFIG_SWAP_ID = "${BASE_PATH}_swap_$END_PATH"
        private const val CONFIG_SELL_ID = "${BASE_PATH}_sell_$END_PATH"
        private const val CONFIG_BUY_ID = "${BASE_PATH}_buy_$END_PATH"
    }
}
