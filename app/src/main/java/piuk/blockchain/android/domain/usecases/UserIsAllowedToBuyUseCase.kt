package piuk.blockchain.android.domain.usecases

import com.blockchain.nabu.Feature
import com.blockchain.nabu.Tier
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.OrderState
import com.blockchain.usecases.UseCase
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.android.simplebuy.SimpleBuySyncFactory

class UserIsAllowedToBuyUseCase(
    private val simpleBuySyncFactory: SimpleBuySyncFactory,
    private val userIdentity: UserIdentity
) : UseCase<Unit, Single<Boolean>>() {

    override fun execute(parameter: Unit): Single<Boolean> {
        val userIsGoldButNotEligible = userIdentity.isVerifiedFor(Feature.TierLevel(Tier.GOLD)).flatMap { isGold ->
            userIdentity.isEligibleFor(Feature.SimpleBuy).map { isBuyEligible ->
                isGold && !isBuyEligible
            }
        }
        val orderIsPendingExecutionOrFunds = simpleBuySyncFactory.currentState()?.let {
            it.orderState in OrderState.AWAITING_FUNDS..OrderState.PENDING_EXECUTION
        } ?: false

        return userIsGoldButNotEligible.map {
            if (it) {
                false
            } else {
                !orderIsPendingExecutionOrFunds
            }
        }
    }
}
