package piuk.blockchain.android.domain.usecases

import com.blockchain.core.kyc.domain.KycService
import com.blockchain.core.kyc.domain.model.KycTier
import com.blockchain.domain.paymentmethods.CardService
import com.blockchain.domain.paymentmethods.PaymentMethodService
import com.blockchain.domain.paymentmethods.model.CardStatus
import com.blockchain.domain.paymentmethods.model.PaymentLimits
import com.blockchain.domain.paymentmethods.model.PaymentMethodType
import com.blockchain.nabu.Feature
import com.blockchain.nabu.UserIdentity
import com.blockchain.usecases.UseCase
import info.blockchain.balance.FiatCurrency
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.zipWith

class GetAvailablePaymentMethodsTypesUseCase(
    private val kycService: KycService,
    private val userIdentity: UserIdentity,
    private val paymentMethodService: PaymentMethodService,
    private val cardService: CardService
) : UseCase<GetAvailablePaymentMethodsTypesUseCase.Request, Single<List<AvailablePaymentMethodType>>>() {

    data class Request(
        val currency: FiatCurrency,
        val onlyEligible: Boolean,
        val fetchSddLimits: Boolean = false
    )

    override fun execute(parameter: Request): Single<List<AvailablePaymentMethodType>> =
        paymentMethodService.getAvailablePaymentMethodsTypes(
            fiatCurrency = parameter.currency,
            fetchSddLimits = parameter.fetchSddLimits,
            onlyEligible = parameter.onlyEligible
        ).zipWith(kycService.getHighestApprovedTierLevelLegacy()).flatMap { (availableTypes, tier) ->
            val isSilver = tier == KycTier.SILVER
            val cardType = availableTypes.find { it.type == PaymentMethodType.PAYMENT_CARD }
            if (cardType == null || !isSilver) {
                Single.just(
                    availableTypes.map {
                        AvailablePaymentMethodType(
                            canBeUsedForPayment = it.eligible,
                            type = it.type,
                            limits = it.limits,
                            currency = it.currency,
                            linkAccess = linkAccessForTier(it.eligible, tier),
                            cardFundSources = it.cardFundSources
                        )
                    }
                )
            } else {
                userIdentity.isVerifiedFor(Feature.SimplifiedDueDiligence)
                    .flatMap { isSDD ->
                        if (isSDD) {
                            cardService.getLinkedCards(CardStatus.ACTIVE).map { cards ->
                                availableTypes.map {
                                    AvailablePaymentMethodType(
                                        canBeUsedForPayment = it.eligible,
                                        type = it.type,
                                        limits = it.limits,
                                        currency = it.currency,
                                        linkAccess =
                                        if (cards.isNotEmpty() && it.type == PaymentMethodType.PAYMENT_CARD) {
                                            LinkAccess.BLOCKED
                                        } else {
                                            linkAccessForTier(it.eligible, tier)
                                        },
                                        cardFundSources = it.cardFundSources
                                    )
                                }
                            }
                        } else {
                            Single.just(
                                availableTypes.map {
                                    AvailablePaymentMethodType(
                                        canBeUsedForPayment = it.eligible,
                                        type = it.type,
                                        limits = it.limits,
                                        currency = it.currency,
                                        linkAccess = linkAccessForTier(it.eligible, tier),
                                        cardFundSources = it.cardFundSources
                                    )
                                }
                            )
                        }
                    }
            }
        }

    private fun linkAccessForTier(eligible: Boolean, tier: KycTier): LinkAccess =
        when (tier) {
            KycTier.GOLD -> if (eligible) LinkAccess.GRANTED else LinkAccess.BLOCKED
            KycTier.SILVER,
            KycTier.BRONZE -> if (eligible) LinkAccess.GRANTED else LinkAccess.NEEDS_UPGRADE
        }
}

data class AvailablePaymentMethodType(
    val canBeUsedForPayment: Boolean,
    val linkAccess: LinkAccess,
    val currency: FiatCurrency,
    val type: PaymentMethodType,
    val limits: PaymentLimits,
    val cardFundSources: List<String>? = null
)

/**
 * Represents the user permission to link/add a new payment method of this type
 *
 * Eg: Link a bank, perform a bank transfer or add a credit card
 */
enum class LinkAccess {
    GRANTED,
    BLOCKED,
    NEEDS_UPGRADE
}
