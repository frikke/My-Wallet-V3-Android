package piuk.blockchain.android.domain.usecases

import com.blockchain.core.payments.PaymentsDataManager
import com.blockchain.nabu.Feature
import com.blockchain.nabu.Tier
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.PaymentLimits
import com.blockchain.nabu.datamanagers.custodialwalletimpl.CardStatus
import com.blockchain.nabu.datamanagers.custodialwalletimpl.PaymentMethodType
import com.blockchain.usecases.UseCase
import info.blockchain.balance.FiatCurrency
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.zipWith

class GetAvailablePaymentMethodsTypesUseCase(
    private val userIdentity: UserIdentity,
    private val paymentsDataManager: PaymentsDataManager
) : UseCase<GetAvailablePaymentMethodsTypesUseCase.Request, Single<List<AvailablePaymentMethodType>>>() {

    data class Request(
        val currency: FiatCurrency,
        val onlyEligible: Boolean,
        val fetchSddLimits: Boolean = false
    )

    override fun execute(parameter: Request): Single<List<AvailablePaymentMethodType>> =
        paymentsDataManager.getAvailablePaymentMethodsTypes(
            fiatCurrency = parameter.currency,
            fetchSddLimits = parameter.fetchSddLimits,
            onlyEligible = parameter.onlyEligible
        ).zipWith(userIdentity.getHighestApprovedKycTier()).flatMap { (availableTypes, tier) ->
            val isSilver = tier == Tier.SILVER
            val cardType = availableTypes.find { it.type == PaymentMethodType.PAYMENT_CARD }
            if (cardType == null || !isSilver) {
                Single.just(
                    availableTypes.map {
                        AvailablePaymentMethodType(
                            canBeUsedForPayment = it.eligible,
                            type = it.type,
                            limits = it.limits,
                            currency = it.currency,
                            linkAccess = linkAccessForTier(it.eligible, tier)
                        )
                    }
                )
            } else {
                userIdentity.isVerifiedFor(Feature.SimplifiedDueDiligence)
                    .flatMap { isSDD ->
                        if (isSDD) {
                            paymentsDataManager.getLinkedCards(CardStatus.ACTIVE).map { cards ->
                                availableTypes.map {
                                    AvailablePaymentMethodType(
                                        canBeUsedForPayment = it.eligible,
                                        type = it.type,
                                        limits = it.limits,
                                        currency = it.currency,
                                        linkAccess = if (
                                            cards.isNotEmpty() &&
                                            it.type == PaymentMethodType.PAYMENT_CARD
                                        ) LinkAccess.BLOCKED
                                        else linkAccessForTier(it.eligible, tier)
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
                                        linkAccess = linkAccessForTier(it.eligible, tier)
                                    )
                                }
                            )
                        }
                    }
            }
        }

    private fun linkAccessForTier(eligible: Boolean, tier: Tier): LinkAccess =
        when (tier) {
            Tier.GOLD -> if (eligible) LinkAccess.GRANTED else LinkAccess.BLOCKED
            Tier.SILVER,
            Tier.BRONZE -> if (eligible) LinkAccess.GRANTED else LinkAccess.NEEDS_UPGRADE
        }
}

data class AvailablePaymentMethodType(
    val canBeUsedForPayment: Boolean,
    val linkAccess: LinkAccess,
    val currency: FiatCurrency,
    val type: PaymentMethodType,
    val limits: PaymentLimits
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
