package piuk.blockchain.android.domain.usecases

import com.blockchain.domain.trade.TradeDataService
import com.blockchain.domain.trade.model.EligibleAndNextPaymentRecurringBuy
import com.blockchain.usecases.UseCase
import io.reactivex.rxjava3.core.Single

class GetEligibilityAndNextPaymentDateUseCase(
    private val tradeDataService: TradeDataService
) : UseCase<Unit, Single<List<EligibleAndNextPaymentRecurringBuy>>>() {

    override fun execute(parameter: Unit): Single<List<EligibleAndNextPaymentRecurringBuy>> =
        tradeDataService.getEligibilityAndNextPaymentDate()
}
