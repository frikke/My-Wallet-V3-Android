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
                        CompletableDashboardOnboardingStep(step = step, state = DashboardOnboardingStepState.COMPLETE)
                    }
            )
        } else {
            Single.zip(
                isGoldVerified(),
                isGoldPending(),
                hasLinkedPaymentMethod(),
                hasBoughtCrypto()
            ) { isGoldVerified, isGoldPending, hasLinkedPaymentMethod, hasBoughtCrypto ->
                if (isGoldVerified && hasLinkedPaymentMethod && hasBoughtCrypto) {
                    dashboardPrefs.isOnboardingComplete = true
                }
                DashboardOnboardingStep.values()
                    .map { step ->
                        CompletableDashboardOnboardingStep(
                            step = step,
                            state = when (step) {
                                DashboardOnboardingStep.UPGRADE_TO_GOLD ->
                                    if (isGoldVerified) DashboardOnboardingStepState.COMPLETE
                                    else if (isGoldPending) DashboardOnboardingStepState.PENDING
                                    else DashboardOnboardingStepState.INCOMPLETE
                                DashboardOnboardingStep.LINK_PAYMENT_METHOD ->
                                    if (hasLinkedPaymentMethod) DashboardOnboardingStepState.COMPLETE
                                    else DashboardOnboardingStepState.INCOMPLETE
                                DashboardOnboardingStep.BUY ->
                                    if (hasBoughtCrypto) DashboardOnboardingStepState.COMPLETE
                                    else DashboardOnboardingStepState.INCOMPLETE
                            }
                        )
                    }
            }
        }

    private fun isGoldVerified(): Single<Boolean> = userIdentity.isVerifiedFor(Feature.TierLevel(Tier.GOLD))

    private fun isGoldPending(): Single<Boolean> = userIdentity.isKycPending(Tier.GOLD)

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

data class CompletableDashboardOnboardingStep(
    val step: DashboardOnboardingStep,
    val state: DashboardOnboardingStepState
) {
    val isCompleted: Boolean = state == DashboardOnboardingStepState.COMPLETE
}

enum class DashboardOnboardingStepState {
    INCOMPLETE,
    PENDING,
    COMPLETE
}

enum class DashboardOnboardingStep {
    UPGRADE_TO_GOLD,
    LINK_PAYMENT_METHOD,
    BUY
}
