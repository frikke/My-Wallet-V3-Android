package piuk.blockchain.android.domain.usecases

import com.blockchain.usecases.UseCase
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.android.domain.repositories.TradeDataService

class IsFirstTimeBuyerUseCase(
    private val tradeDataService: TradeDataService
) : UseCase<Unit, Single<Boolean>>() {

    override fun execute(parameter: Unit): Single<Boolean> =
        tradeDataService.isFirstTimeBuyer()
}
