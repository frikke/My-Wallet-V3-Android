package piuk.blockchain.android.ui.interest.tbm.domain.usecase

import com.blockchain.outcome.Outcome
import piuk.blockchain.android.ui.interest.tbm.domain.repository.AssetInterestRepository
import piuk.blockchain.android.ui.interest.tbm.domain.model.InterestDetail

class GetInterestDetailUseCase(private val repository: AssetInterestRepository) {
    suspend operator fun invoke(): Outcome<Throwable, InterestDetail> = repository.getInterestDetail()
}