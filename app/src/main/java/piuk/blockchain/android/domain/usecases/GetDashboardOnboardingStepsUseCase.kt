package piuk.blockchain.android.domain.usecases

import com.blockchain.core.kyc.domain.KycService
import com.blockchain.core.kyc.domain.model.KycTier
import com.blockchain.domain.onboarding.CompletableDashboardOnboardingStep
import com.blockchain.domain.onboarding.DashboardOnboardingStep
import com.blockchain.domain.onboarding.DashboardOnboardingStepState
import com.blockchain.domain.onboarding.OnBoardingStepsService
import com.blockchain.domain.paymentmethods.BankService
import com.blockchain.domain.paymentmethods.CardService
import com.blockchain.domain.paymentmethods.model.BankState
import com.blockchain.domain.paymentmethods.model.CardStatus
import com.blockchain.domain.trade.TradeDataService
import com.blockchain.nabu.Feature
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.api.getuser.domain.UserFeaturePermissionService
import com.blockchain.preferences.DashboardPrefs
import com.blockchain.store.asSingle
import com.blockchain.usecases.UseCase
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.rx3.await

class GetDashboardOnboardingStepsUseCase(
    private val dashboardPrefs: DashboardPrefs,
    private val userIdentity: UserIdentity,
    private val kycService: KycService,
    private val bankService: BankService,
    private val cardService: CardService,
    private val tradeDataService: TradeDataService,
    private val userFeaturePermissionService: UserFeaturePermissionService
) : OnBoardingStepsService, UseCase<Unit, Single<List<CompletableDashboardOnboardingStep>>>() {

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
                isEligibleForKyc(),
                isGoldVerified(),
                isGoldPending(),
                hasLinkedPaymentMethod(),
                hasBoughtCrypto()
            ) { isEligibleForKyc, isGoldVerified, isGoldPending, hasLinkedPaymentMethod, hasBoughtCrypto ->
                if (!isEligibleForKyc) {
                    emptyList()
                } else if (hasBoughtCrypto) {
                    dashboardPrefs.isOnboardingComplete = true
                    DashboardOnboardingStep.values().map { step ->
                        CompletableDashboardOnboardingStep(step, DashboardOnboardingStepState.COMPLETE)
                    }
                } else {
                    DashboardOnboardingStep.values()
                        .map { step ->
                            CompletableDashboardOnboardingStep(
                                step = step,
                                state = when (step) {
                                    DashboardOnboardingStep.UPGRADE_TO_GOLD ->
                                        if (isGoldVerified) {
                                            DashboardOnboardingStepState.COMPLETE
                                        } else if (isGoldPending) {
                                            DashboardOnboardingStepState.PENDING
                                        } else DashboardOnboardingStepState.INCOMPLETE
                                    DashboardOnboardingStep.LINK_PAYMENT_METHOD ->
                                        if (hasLinkedPaymentMethod) {
                                            DashboardOnboardingStepState.COMPLETE
                                        } else DashboardOnboardingStepState.INCOMPLETE
                                    DashboardOnboardingStep.BUY ->
                                        if (hasBoughtCrypto) {
                                            DashboardOnboardingStepState.COMPLETE
                                        } else DashboardOnboardingStepState.INCOMPLETE
                                }
                            )
                        }
                }
            }
        }

    private fun isEligibleForKyc(): Single<Boolean> = userFeaturePermissionService.isEligibleFor(Feature.Kyc).asSingle()

    private fun isGoldVerified(): Single<Boolean> = userIdentity.isVerifiedFor(Feature.TierLevel(KycTier.GOLD))

    private fun isGoldPending(): Single<Boolean> = kycService.isPendingFor(KycTier.GOLD)

    private fun hasLinkedPaymentMethod(): Single<Boolean> = Single.zip(
        bankService.getLinkedBanks().map { banks ->
            banks.any { it.state == BankState.ACTIVE }
        },
        cardService.getLinkedCardsLegacy(CardStatus.ACTIVE, CardStatus.EXPIRED).map { it.isNotEmpty() }
    ) { hasLinkedBank, hasLinkedCard ->
        hasLinkedBank || hasLinkedCard
    }

    private fun hasBoughtCrypto(): Single<Boolean> =
        tradeDataService.isFirstTimeBuyer().map { isFirstTimeBuyer -> !isFirstTimeBuyer }

    override suspend fun onBoardingSteps(): List<CompletableDashboardOnboardingStep> {
        return execute(Unit).onErrorReturn { emptyList() }.await()
    }
}
