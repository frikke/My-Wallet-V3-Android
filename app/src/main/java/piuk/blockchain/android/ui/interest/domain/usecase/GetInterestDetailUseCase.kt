package piuk.blockchain.android.ui.interest.domain.usecase

import com.blockchain.outcome.Outcome
import piuk.blockchain.android.ui.interest.domain.model.InterestDetail
import piuk.blockchain.android.ui.interest.domain.repository.AssetInterestService

class GetInterestDetailUseCase(private val service: AssetInterestService) {
    suspend operator fun invoke(): Outcome<Throwable, InterestDetail> = service.getInterestDetail()
}
