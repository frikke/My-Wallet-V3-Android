package piuk.blockchain.android.domain.repositories

import com.blockchain.core.recurringbuy.domain.EligibleAndNextPaymentRecurringBuy
import com.blockchain.core.recurringbuy.domain.RecurringBuy
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.flow.Flow
import piuk.blockchain.android.data.QuotePrice

interface TradeDataService {

    fun isFirstTimeBuyer(): Single<Boolean>

    fun getEligibilityAndNextPaymentDate(): Single<List<EligibleAndNextPaymentRecurringBuy>>

    fun getRecurringBuysForAsset(
        asset: AssetInfo,
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(RefreshStrategy.ForceRefresh)
    ): Flow<DataResource<List<RecurringBuy>>>

    fun getRecurringBuyForId(recurringBuyId: String): Single<RecurringBuy>

    fun cancelRecurringBuy(recurringBuyId: String): Completable

    fun getQuotePrice(
        currencyPair: String,
        amount: String,
        paymentMethod: String,
        orderProfileName: String
    ): Observable<QuotePrice>
}
