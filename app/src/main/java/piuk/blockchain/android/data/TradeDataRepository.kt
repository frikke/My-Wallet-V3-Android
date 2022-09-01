package piuk.blockchain.android.data

import com.blockchain.api.services.TradeService
import com.blockchain.api.trade.data.AccumulatedInPeriod
import com.blockchain.api.trade.data.NextPaymentRecurringBuy
import com.blockchain.api.trade.data.RecurringBuyResponse
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.FreshnessStrategy.Companion.withKey
import com.blockchain.nabu.Authenticator
import com.blockchain.nabu.models.data.EligibleAndNextPaymentRecurringBuy
import com.blockchain.nabu.models.data.RecurringBuy
import com.blockchain.store.mapData
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.flow.Flow
import piuk.blockchain.android.domain.repositories.TradeDataService

class TradeDataRepository(
    private val tradeService: TradeService,
    private val authenticator: Authenticator,
    private val accumulatedInPeriodMapper: Mapper<List<AccumulatedInPeriod>, Boolean>,
    private val nextPaymentRecurringBuyMapper:
        Mapper<List<NextPaymentRecurringBuy>, List<EligibleAndNextPaymentRecurringBuy>>,
    private val recurringBuyMapper: Mapper<List<RecurringBuyResponse>, List<RecurringBuy>>,
    private val getRecurringBuysStore: GetRecurringBuysStore
) : TradeDataService {

    override fun isFirstTimeBuyer(): Single<Boolean> {
        return authenticator.authenticate { tokenResponse ->
            tradeService.isFirstTimeBuyer(authHeader = tokenResponse.authHeader)
                .map {
                    accumulatedInPeriodMapper.map(it.tradesAccumulated)
                }
        }
    }

    override fun getEligibilityAndNextPaymentDate(): Single<List<EligibleAndNextPaymentRecurringBuy>> {
        return authenticator.authenticate { tokenResponse ->
            tradeService.getNextPaymentDate(authHeader = tokenResponse.authHeader)
                .map {
                    nextPaymentRecurringBuyMapper.map(it.nextPayments)
                }
        }
    }

    override fun getRecurringBuysForAsset(
        asset: AssetInfo,
        freshnessStrategy: FreshnessStrategy
    ): Flow<DataResource<List<RecurringBuy>>> {
        return getRecurringBuysStore
            .stream(
                freshnessStrategy.withKey(GetRecurringBuysStore.Key(asset.networkTicker))
            )
            .mapData {
                recurringBuyMapper.map(it)
            }
    }

    override fun getRecurringBuyForId(recurringBuyId: String): Single<RecurringBuy> {
        return authenticator.authenticate { tokenResponse ->
            tradeService.getRecurringBuyForId(authHeader = tokenResponse.authHeader, recurringBuyId)
                .map {
                    recurringBuyMapper.map(it).first()
                }
        }
    }

    override fun cancelRecurringBuy(recurringBuyId: String): Completable {
        return authenticator.authenticateCompletable { tokenResponse ->
            tradeService.cancelRecurringBuy(tokenResponse.authHeader, recurringBuyId)
        }
    }
}
