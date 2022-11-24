package piuk.blockchain.android.domain.usecases

import com.blockchain.core.recurringbuy.EligibleAndNextPaymentRecurringBuy
import com.blockchain.usecases.UseCase
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.android.domain.repositories.TradeDataService

class GetEligibilityAndNextPaymentDateUseCase(
    private val tradeDataService: TradeDataService
) : UseCase<Unit, Single<List<EligibleAndNextPaymentRecurringBuy>>>() {

    override fun execute(parameter: Unit): Single<List<EligibleAndNextPaymentRecurringBuy>> =
        tradeDataService.getEligibilityAndNextPaymentDate()
}
