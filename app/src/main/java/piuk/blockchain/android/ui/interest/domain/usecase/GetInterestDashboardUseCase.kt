package piuk.blockchain.android.ui.interest.domain.usecase

import com.blockchain.outcome.Outcome
import piuk.blockchain.android.ui.interest.domain.model.InterestDashboard
import piuk.blockchain.android.ui.interest.domain.repository.AssetInterestService

class GetInterestDashboardUseCase(private val service: AssetInterestService) {
    suspend operator fun invoke(): Outcome<Throwable, InterestDashboard> = service.getInterestDashboard()
}
