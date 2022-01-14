package piuk.blockchain.android.domain.usecases

import com.blockchain.core.payments.PaymentsDataManager
import com.blockchain.core.payments.model.BankState
import com.blockchain.nabu.Feature
import com.blockchain.nabu.Tier
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.custodialwalletimpl.CardStatus
import com.blockchain.preferences.DashboardPrefs
import com.blockchain.usecases.UseCase
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.android.domain.repositories.TradeDataManager

class GetDashboardOnboardingStepsUseCase(
    private val dashboardPrefs: DashboardPrefs,
    private val userIdentity: UserIdentity,
    private val paymentsDataManager: PaymentsDataManager,
    private val custodialWalletManager: CustodialWalletManager,
    private val tradeDataManager: TradeDataManager
) : UseCase<Unit, Single<List<CompletableDashboardOnboardingStep>>>() {

    override fun execute(parameter: Unit): Single<List<CompletableDashboardOnboardingStep>> =
        if (dashboardPrefs.isOnboardingComplete) {
            Single.just(
                DashboardOnboardingStep.values()
                    .map { step ->
                        CompletableDashboardOnboardingStep(step = step, isCompleted = true)
                    }
            )
        } else {
            Single.zip(
                isGoldVerified(),
                hasLinkedPaymentMethod(),
                hasBoughtCrypto()
            ) { isGoldVerified, hasLinkedPaymentMethod, hasBoughtCrypto ->
                if (isGoldVerified && hasLinkedPaymentMethod && hasBoughtCrypto) {
                    dashboardPrefs.isOnboardingComplete = true
                }
                DashboardOnboardingStep.values()
                    .map { step ->
                        CompletableDashboardOnboardingStep(
                            step = step,
                            isCompleted = when (step) {
                                DashboardOnboardingStep.UPGRADE_TO_GOLD -> isGoldVerified
                                DashboardOnboardingStep.LINK_PAYMENT_METHOD -> hasLinkedPaymentMethod
                                DashboardOnboardingStep.BUY -> hasBoughtCrypto
                            }
                        )
                    }
            }
        }

    private fun isGoldVerified(): Single<Boolean> = userIdentity.isVerifiedFor(Feature.TierLevel(Tier.GOLD))

    private fun hasLinkedPaymentMethod(): Single<Boolean> = Single.zip(
        paymentsDataManager.getLinkedBanks().map { banks ->
            banks.any { it.state == BankState.ACTIVE }
        },
        paymentsDataManager.getLinkedCards(CardStatus.ACTIVE, CardStatus.EXPIRED).map { it.isNotEmpty() }
    ) { hasLinkedBank, hasLinkedCard ->
        hasLinkedBank || hasLinkedCard
    }

    private fun hasBoughtCrypto(): Single<Boolean> =
        tradeDataManager.isFirstTimeBuyer().map { isFirstTimeBuyer -> !isFirstTimeBuyer }
}

data class CompletableDashboardOnboardingStep(val step: DashboardOnboardingStep, val isCompleted: Boolean)

enum class DashboardOnboardingStep {
    UPGRADE_TO_GOLD,
    LINK_PAYMENT_METHOD,
    BUY
}
