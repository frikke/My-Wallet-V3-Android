package piuk.blockchain.android.ui.interest.tbm.domain.usecase

import com.blockchain.outcome.Outcome
import piuk.blockchain.android.ui.interest.tbm.domain.repository.AssetInterestService
import piuk.blockchain.android.ui.interest.tbm.domain.model.InterestDetail

class GetInterestDetailUseCase(private val service: AssetInterestService) {
    suspend operator fun invoke(): Outcome<Throwable, InterestDetail> = service.getInterestDetail()
}