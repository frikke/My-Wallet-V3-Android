package piuk.blockchain.android.domain.usecases

import com.blockchain.android.testutils.rxInit
import com.blockchain.api.services.MobilePaymentType
import com.blockchain.core.payments.LinkedPaymentMethod
import com.blockchain.core.payments.PaymentMethodTypeWithEligibility
import com.blockchain.core.payments.PaymentsDataManager
import com.blockchain.core.payments.model.Partner
import com.blockchain.nabu.Feature
import com.blockchain.nabu.Tier
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.PaymentLimits
import com.blockchain.nabu.datamanagers.custodialwalletimpl.CardStatus
import com.blockchain.nabu.datamanagers.custodialwalletimpl.PaymentMethodType
import com.braintreepayments.cardform.utils.CardType
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.FiatCurrency
import io.reactivex.rxjava3.core.Single
import java.math.BigInteger
import java.util.Date
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class GetAvailablePaymentMethodsTypesUseCaseTest {

    @get:Rule
    val rx = rxInit {
        ioTrampoline()
        computationTrampoline()
    }

    private val userIdentity: UserIdentity = mock()
    private val paymentsDataManager: PaymentsDataManager = mock()

    private lateinit var subject: GetAvailablePaymentMethodsTypesUseCase

    @Before
    fun setUp() {
        subject = GetAvailablePaymentMethodsTypesUseCase(
            userIdentity,
            paymentsDataManager
        )
    }

    @Test
    fun `given gold tier should just return available payments method types and have BLOCKED link access for non eligible`() {
        whenever(userIdentity.getHighestApprovedKycTier()).thenReturn(Single.just(Tier.GOLD))
        whenever(paymentsDataManager.getAvailablePaymentMethodsTypes(any(), any(), any()))
            .thenReturn(Single.just(AVAILABLE))

        val test = subject.invoke(REQUEST).test()

        val expected = listOf(
            AvailablePaymentMethodType(
                true, LinkAccess.GRANTED, FIAT, PaymentMethodType.PAYMENT_CARD, NULL_LIMITS, CARD_FUND_SOURCES
            ),
            AvailablePaymentMethodType(
                true, LinkAccess.GRANTED, FIAT, PaymentMethodType.BANK_ACCOUNT, NULL_LIMITS, CARD_FUND_SOURCES
            ),
            AvailablePaymentMethodType(
                false, LinkAccess.BLOCKED, FIAT, PaymentMethodType.BANK_TRANSFER, NULL_LIMITS, CARD_FUND_SOURCES
            )
        )
        test.assertValue(expected)

        verify(paymentsDataManager)
            .getAvailablePaymentMethodsTypes(REQUEST.currency, REQUEST.fetchSddLimits, REQUEST.onlyEligible)
    }

    @Test
    fun `given bronze tier should just return available payment method types and have NEEDS_UPGRADE for non eligible`() {
        whenever(userIdentity.getHighestApprovedKycTier()).thenReturn(Single.just(Tier.BRONZE))
        whenever(paymentsDataManager.getAvailablePaymentMethodsTypes(any(), any(), any()))
            .thenReturn(Single.just(AVAILABLE))

        val test = subject.invoke(REQUEST).test()

        val expected = listOf(
            AvailablePaymentMethodType(
                true, LinkAccess.GRANTED, FIAT, PaymentMethodType.PAYMENT_CARD, NULL_LIMITS, CARD_FUND_SOURCES
            ),
            AvailablePaymentMethodType(
                true, LinkAccess.GRANTED, FIAT, PaymentMethodType.BANK_ACCOUNT, NULL_LIMITS, CARD_FUND_SOURCES
            ),
            AvailablePaymentMethodType(
                false, LinkAccess.NEEDS_UPGRADE, FIAT, PaymentMethodType.BANK_TRANSFER, NULL_LIMITS, CARD_FUND_SOURCES
            )
        )
        test.assertValue(expected)

        verify(paymentsDataManager)
            .getAvailablePaymentMethodsTypes(REQUEST.currency, REQUEST.fetchSddLimits, REQUEST.onlyEligible)
    }

    @Test
    fun `given silver tier with sdd and card type eligible should fetch cards and block linkaccess if user has active cards`() {
        whenever(userIdentity.isVerifiedFor(Feature.SimplifiedDueDiligence)).thenReturn(Single.just(true))
        whenever(userIdentity.getHighestApprovedKycTier()).thenReturn(Single.just(Tier.SILVER))
        whenever(paymentsDataManager.getAvailablePaymentMethodsTypes(any(), any(), any()))
            .thenReturn(Single.just(AVAILABLE))
        whenever(paymentsDataManager.getLinkedCards(CardStatus.ACTIVE)).thenReturn(Single.just(listOf(CARD)))

        val test = subject.invoke(REQUEST).test()

        val expected = listOf(
            AvailablePaymentMethodType(
                true, LinkAccess.BLOCKED, FIAT, PaymentMethodType.PAYMENT_CARD, NULL_LIMITS, CARD_FUND_SOURCES
            ),
            AvailablePaymentMethodType(
                true, LinkAccess.GRANTED, FIAT, PaymentMethodType.BANK_ACCOUNT, NULL_LIMITS, CARD_FUND_SOURCES
            ),
            AvailablePaymentMethodType(
                false, LinkAccess.NEEDS_UPGRADE, FIAT, PaymentMethodType.BANK_TRANSFER, NULL_LIMITS, CARD_FUND_SOURCES
            )
        )
        test.assertValue(expected)

        verify(paymentsDataManager)
            .getAvailablePaymentMethodsTypes(REQUEST.currency, REQUEST.fetchSddLimits, REQUEST.onlyEligible)
        verify(userIdentity).isVerifiedFor(Feature.SimplifiedDueDiligence)
        verify(paymentsDataManager).getLinkedCards(CardStatus.ACTIVE)
    }

    companion object {
        private val FIAT = FiatCurrency.Dollars
        private val NULL_LIMITS = PaymentLimits(
            BigInteger.ZERO,
            BigInteger.ZERO,
            FIAT
        )
        private val CARD_FUND_SOURCES = listOf("CREDIT", "DEBIT", "PREPAID")
        private val REQUEST = GetAvailablePaymentMethodsTypesUseCase.Request(FIAT, false, true)
        private val AVAILABLE = listOf(
            PaymentMethodTypeWithEligibility(
                true, FIAT, PaymentMethodType.PAYMENT_CARD, NULL_LIMITS, CARD_FUND_SOURCES
            ),
            PaymentMethodTypeWithEligibility(
                true, FIAT, PaymentMethodType.BANK_ACCOUNT, NULL_LIMITS, CARD_FUND_SOURCES
            ),
            PaymentMethodTypeWithEligibility(
                false, FIAT, PaymentMethodType.BANK_TRANSFER, NULL_LIMITS, CARD_FUND_SOURCES
            )
        )
        private val CARD =
            LinkedPaymentMethod.Card(
                cardId = "id",
                label = "",
                endDigits = "",
                partner = Partner.CARDPROVIDER,
                expireDate = Date(),
                cardType = CardType.AMEX,
                status = CardStatus.ACTIVE,
                cardFundSources = CARD_FUND_SOURCES,
                mobilePaymentType = MobilePaymentType.GOOGLE_PAY,
                currency = FIAT
            )
    }
}
