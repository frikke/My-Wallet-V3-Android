package piuk.blockchain.android.ui.settings

import com.blockchain.core.kyc.domain.KycService
import com.blockchain.data.toObservable
import com.blockchain.domain.paymentmethods.BankService
import com.blockchain.domain.paymentmethods.CardService
import com.blockchain.domain.paymentmethods.model.BankState
import com.blockchain.domain.paymentmethods.model.CardStatus
import com.blockchain.domain.paymentmethods.model.LinkBankTransfer
import com.blockchain.domain.paymentmethods.model.LinkedPaymentMethod
import com.blockchain.domain.paymentmethods.model.PaymentLimits
import com.blockchain.domain.paymentmethods.model.PaymentMethod
import com.blockchain.domain.paymentmethods.model.PaymentMethodType
import com.blockchain.domain.referral.ReferralService
import com.blockchain.domain.referral.model.ReferralInfo
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.NabuUserIdentity
import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.FiatCurrency
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.Singles
import java.math.BigInteger
import piuk.blockchain.android.domain.usecases.AvailablePaymentMethodType
import piuk.blockchain.android.domain.usecases.GetAvailablePaymentMethodsTypesUseCase
import piuk.blockchain.android.ui.home.CredentialsWiper

class SettingsInteractor internal constructor(
    private val userIdentity: UserIdentity,
    private val kycService: KycService,
    private val credentialsWiper: CredentialsWiper,
    private val bankService: BankService,
    private val cardService: CardService,
    private val getAvailablePaymentMethodsTypesUseCase: GetAvailablePaymentMethodsTypesUseCase,
    private val currencyPrefs: CurrencyPrefs,
    private val referralService: ReferralService,
    private val nabuUserIdentity: NabuUserIdentity
) {
    val userSelectedFiat: FiatCurrency
        get() = currencyPrefs.selectedFiatCurrency

    fun getSupportEligibilityAndBasicInfo(): Single<UserDetails> {
        return Singles.zip(
            kycService.getHighestApprovedTierLevelLegacy(),
            userIdentity.getBasicProfileInformation(),
            getReferralData()
        ).map { (kycTier, basicInfo, referral) ->
            UserDetails(kycTier = kycTier, userInfo = basicInfo, referralInfo = referral)
        }
    }

    private fun getReferralData(): Single<ReferralInfo> {
        return referralService.fetchReferralData().toObservable().firstOrError()
            .onErrorResumeWith { Single.just(ReferralInfo.NotAvailable) }
    }

    fun unpairWallet(): Completable = Completable.fromAction {
        credentialsWiper.wipe()
    }

    fun getExistingPaymentMethods(): Single<PaymentMethods> {
        val fiatCurrency = userSelectedFiat
        return getAvailablePaymentMethodsTypes(fiatCurrency).flatMap { available ->
            val limitsInfoCards = available.find { it.type == PaymentMethodType.PAYMENT_CARD }?.limits ?: PaymentLimits(
                BigInteger.ZERO,
                BigInteger.ZERO,
                fiatCurrency
            )

            val limitsInfoBanks =
                available.find {
                    it.type == PaymentMethodType.BANK_ACCOUNT ||
                        it.type == PaymentMethodType.BANK_TRANSFER
                }?.limits
                    ?: PaymentLimits(
                        BigInteger.ZERO,
                        BigInteger.ZERO,
                        fiatCurrency
                    )

            val getLinkedCards =
                if (available.any { it.type == PaymentMethodType.PAYMENT_CARD }) {
                    getLinkedCards(limitsInfoCards)
                } else {
                    Single.just(emptyList())
                }

            val getLinkedBanks =
                if (available.any {
                    it.type == PaymentMethodType.BANK_TRANSFER ||
                        it.type == PaymentMethodType.BANK_ACCOUNT
                }
                ) {
                    getLinkedBanks(fiatCurrency, available, limitsInfoBanks)
                } else {
                    Single.just(emptyList())
                }

            Singles.zip(
                getLinkedCards,
                getLinkedBanks
            ).map { (linkedCards, linkedBanks) ->
                PaymentMethods(
                    availablePaymentMethodTypes = available,
                    linkedCards = linkedCards,
                    linkedBanks = linkedBanks
                )
            }
        }
    }

    fun canPayWithBind(): Single<Boolean> = nabuUserIdentity.isArgentinian()

    fun getBankLinkingInfo(): Single<LinkBankTransfer> =
        bankService.linkBank(userSelectedFiat)

    fun getAvailablePaymentMethodsTypes(): Single<List<AvailablePaymentMethodType>> =
        getAvailablePaymentMethodsTypes(userSelectedFiat)

    private fun getAvailablePaymentMethodsTypes(fiatCurrency: FiatCurrency): Single<List<AvailablePaymentMethodType>> =
        getAvailablePaymentMethodsTypesUseCase(
            GetAvailablePaymentMethodsTypesUseCase.Request(
                currency = fiatCurrency,
                onlyEligible = true
            )
        ).map { available ->
            val allowedTypes = listOf(
                PaymentMethodType.BANK_TRANSFER,
                PaymentMethodType.BANK_ACCOUNT,
                PaymentMethodType.PAYMENT_CARD
            )
            available.filter { it.currency == fiatCurrency && allowedTypes.contains(it.type) }
        }

    private fun getLinkedCards(limits: PaymentLimits): Single<List<PaymentMethod.Card>> =
        cardService.getLinkedCardsLegacy(CardStatus.ACTIVE, CardStatus.EXPIRED).map { cards ->
            cards.map { it.toPaymentCard(limits) }
        }

    private fun getLinkedBanks(
        fiatCurrency: FiatCurrency,
        available: List<AvailablePaymentMethodType>,
        limits: PaymentLimits
    ): Single<List<BankItem>> =
        bankService.getLinkedBanks()
            .map { banks ->
                val linkedBanks = banks.filter { it.state == BankState.ACTIVE }
                val availableBankPaymentMethodTypes = available.filter {
                    it.type == PaymentMethodType.BANK_TRANSFER ||
                        it.type == PaymentMethodType.BANK_ACCOUNT
                }.map { it.type }

                linkedBanks.map { bank ->
                    val canBeUsedToTransact = availableBankPaymentMethodTypes.contains(bank.type) &&
                        fiatCurrency == bank.currency
                    BankItem(bank, canBeUsedToTransact = canBeUsedToTransact, limits)
                }
            }

    private fun LinkedPaymentMethod.Card.toPaymentCard(limits: PaymentLimits) = PaymentMethod.Card(
        cardId = cardId,
        limits = limits,
        label = label,
        endDigits = endDigits,
        partner = partner,
        expireDate = expireDate,
        cardType = cardType,
        status = status,
        isEligible = true,
        cardRejectionState = cardRejectionState
    )
}
