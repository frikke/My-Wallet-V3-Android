package piuk.blockchain.android.ui.settings.v2

import com.blockchain.core.Database
import com.blockchain.core.featureflag.IntegratedFeatureFlag
import com.blockchain.core.payments.LinkedPaymentMethod
import com.blockchain.core.payments.PaymentsDataManager
import com.blockchain.core.payments.model.BankState
import com.blockchain.core.payments.model.LinkBankTransfer
import com.blockchain.domain.referral.ReferralService
import com.blockchain.domain.referral.model.ReferralInfo
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.PaymentLimits
import com.blockchain.nabu.datamanagers.PaymentMethod
import com.blockchain.nabu.datamanagers.custodialwalletimpl.CardStatus
import com.blockchain.nabu.datamanagers.custodialwalletimpl.PaymentMethodType
import com.blockchain.outcome.fold
import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.FiatCurrency
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.Singles
import java.math.BigInteger
import kotlinx.coroutines.rx3.rxSingle
import piuk.blockchain.android.domain.usecases.AvailablePaymentMethodType
import piuk.blockchain.android.domain.usecases.GetAvailablePaymentMethodsTypesUseCase
import piuk.blockchain.android.ui.home.CredentialsWiper

class SettingsInteractor internal constructor(
    private val userIdentity: UserIdentity,
    private val database: Database,
    private val credentialsWiper: CredentialsWiper,
    private val paymentsDataManager: PaymentsDataManager,
    private val getAvailablePaymentMethodsTypesUseCase: GetAvailablePaymentMethodsTypesUseCase,
    private val currencyPrefs: CurrencyPrefs,
    private val referralService: ReferralService,
    private val referralFeatureFlag: IntegratedFeatureFlag
) {
    private val userSelectedFiat: FiatCurrency
        get() = currencyPrefs.selectedFiatCurrency
    fun getUserFiat() = userSelectedFiat

    fun getSupportEligibilityAndBasicInfo(): Single<UserDetails> {
        return Singles.zip(
            userIdentity.getHighestApprovedKycTier(),
            userIdentity.getBasicProfileInformation(),
            getReferralDataSingleIfEnabled()
        ).map { (tier, basicInfo, referral) ->
            UserDetails(userTier = tier, userInfo = basicInfo, referralInfo = referral)
        }
    }

    private fun getReferralDataSingleIfEnabled(): Single<ReferralInfo> {
        return referralFeatureFlag.enabled
            .flatMap {
                if (it) {
                    rxSingle {
                        referralService.fetchReferralData()
                            .fold(
                                onSuccess = { it },
                                onFailure = { ReferralInfo.NotAvailable }
                            )
                    }
                } else {
                    Single.just(ReferralInfo.NotAvailable as ReferralInfo)
                }
            }
            .onErrorResumeWith { ReferralInfo.NotAvailable }
    }

    fun unpairWallet(): Completable = Completable.fromAction {
        credentialsWiper.wipe()
        database.historicRateQueries.clear()
    }

    fun getExistingPaymentMethods(): Single<PaymentMethods> {
        val fiatCurrency = userSelectedFiat
        return getAvailablePaymentMethodsTypes(fiatCurrency).flatMap { available ->
            val limitsInfo = available.find { it.type == PaymentMethodType.PAYMENT_CARD }?.limits ?: PaymentLimits(
                BigInteger.ZERO,
                BigInteger.ZERO,
                fiatCurrency
            )

            val getLinkedCards =
                if (available.any { it.type == PaymentMethodType.PAYMENT_CARD }) getLinkedCards(limitsInfo)
                else Single.just(emptyList())
            Singles.zip(
                getLinkedCards,
                getLinkedBanks(fiatCurrency, available)
            ).map { (linkedCards, linkedBanks) ->
                PaymentMethods(
                    availablePaymentMethodTypes = available,
                    linkedCards = linkedCards,
                    linkedBanks = linkedBanks
                )
            }
        }
    }

    fun getBankLinkingInfo(): Single<LinkBankTransfer> =
        paymentsDataManager.linkBank(userSelectedFiat)

    fun getAvailablePaymentMethodsTypes(): Single<List<AvailablePaymentMethodType>> =
        getAvailablePaymentMethodsTypes(userSelectedFiat)

    private fun getAvailablePaymentMethodsTypes(fiatCurrency: FiatCurrency): Single<List<AvailablePaymentMethodType>> =
        getAvailablePaymentMethodsTypesUseCase(
            GetAvailablePaymentMethodsTypesUseCase.Request(
                currency = fiatCurrency,
                onlyEligible = true,
                fetchSddLimits = false
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
        paymentsDataManager.getLinkedCards(CardStatus.ACTIVE, CardStatus.EXPIRED).map { cards ->
            cards.map { it.toPaymentCard(limits) }
        }

    private fun getLinkedBanks(
        fiatCurrency: FiatCurrency,
        available: List<AvailablePaymentMethodType>
    ): Single<List<BankItem>> =
        paymentsDataManager.getLinkedBanks()
            .map { banks ->
                val linkedBanks = banks.filter { it.state == BankState.ACTIVE }
                val availableBankPaymentMethodTypes = available.filter {
                    it.type == PaymentMethodType.BANK_TRANSFER ||
                        it.type == PaymentMethodType.BANK_ACCOUNT
                }.map { it.type }

                linkedBanks.map { bank ->
                    val canBeUsedToTransact = availableBankPaymentMethodTypes.contains(bank.type) &&
                        fiatCurrency == bank.currency
                    BankItem(bank, canBeUsedToTransact = canBeUsedToTransact)
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
        isEligible = true
    )
}
