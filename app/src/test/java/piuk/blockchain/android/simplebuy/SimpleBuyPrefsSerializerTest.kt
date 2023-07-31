package piuk.blockchain.android.simplebuy

import com.blockchain.core.custodial.models.Availability
import com.blockchain.core.custodial.models.Promo
import com.blockchain.core.recurringbuy.domain.model.RecurringBuyFrequency
import com.blockchain.core.recurringbuy.domain.model.RecurringBuyState
import com.blockchain.domain.paymentmethods.model.DepositTerms
import com.blockchain.domain.paymentmethods.model.Partner
import com.blockchain.domain.paymentmethods.model.PaymentMethodType
import com.blockchain.domain.paymentmethods.model.SettlementReason
import com.blockchain.domain.paymentmethods.model.SettlementType
import com.blockchain.nabu.datamanagers.OrderState
import com.blockchain.payments.googlepay.manager.request.BillingAddressParameters
import com.blockchain.payments.googlepay.manager.request.defaultAllowedAuthMethods
import com.blockchain.payments.googlepay.manager.request.defaultAllowedCardNetworks
import com.blockchain.preferences.SimpleBuyPrefs
import com.blockchain.serializers.BigDecimalSerializer
import com.blockchain.serializers.BigIntSerializer
import com.blockchain.serializers.KZonedDateTimeSerializer
import com.blockchain.testutils.EUR
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.serializers.AssetInfoKSerializer
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import java.math.BigInteger
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import org.junit.Before
import org.junit.Test

class SimpleBuyPrefsSerializerTest {

    private val prefs: SimpleBuyPrefs = mockk()
    private val assetCatalogue: AssetCatalogue = mockk()
    private val json = Json {
        serializersModule = SerializersModule {
            contextual(BigDecimalSerializer)
            contextual(BigIntSerializer)
            contextual(KZonedDateTimeSerializer)
            contextual(AssetInfoKSerializer(assetCatalogue = assetCatalogue))
        }
    }

    private val simpleBuyPrefsSerializer: SimpleBuyPrefsSerializer = SimpleBuyPrefsSerializerImpl(
        prefs = prefs,
        json = json
    )

    private val simpleBuyStateObject = SimpleBuyState(
        id = "id_SimpleBuyState",
        amount = FiatValue.fromMinor(EUR, 1000.toBigInteger()),
        fiatCurrency = EUR,
        selectedCryptoAsset = CryptoCurrency.BTC,
        quotePrice = null,
        orderState = OrderState.AWAITING_FUNDS,
        kycVerificationState = KycState.PENDING,
        currentScreen = FlowScreen.KYC,
        kycStartedButNotCompleted = true,
        selectedPaymentMethod = SelectedPaymentMethod(
            id = "id_SelectedPaymentMethod",
            partner = Partner.CARDPROVIDER,
            label = "label_SelectedPaymentMethod",
            paymentMethodType = PaymentMethodType.BANK_ACCOUNT,
            isEligible = true
        ),
        quote = BuyQuote(
            id = "id_BuyQuote",
            price = FiatValue.fromMinor(EUR, 2000.toBigInteger()),
            availability = Availability.REGULAR,
            quoteMargin = 2000.0,
            feeDetails = BuyFees(
                feeBeforePromo = FiatValue.fromMinor(EUR, 2001.toBigInteger()),
                fee = FiatValue.fromMinor(EUR, 2002.toBigInteger()),
                promo = Promo.NEW_USER
            ),
            remainingTime = 119,
            chunksTimeCounter = mutableListOf(30, 30, 30, 29),
            createdAt = 1000,
            expiresAt = 1200,
            depositTerms = DepositTerms(
                creditCurrency = "USD",
                availableToTradeDisplayMode = DepositTerms.DisplayMode.MAX_DAY,
                availableToTradeMinutesMin = 1,
                availableToTradeMinutesMax = 2,
                availableToWithdrawDisplayMode = DepositTerms.DisplayMode.IMMEDIATELY,
                availableToWithdrawMinutesMin = 1,
                availableToWithdrawMinutesMax = 2,
                settlementType = SettlementType.INSTANT,
                settlementReason = SettlementReason.GENERIC
            )
        ),
        orderValue = CryptoValue(CryptoCurrency.BTC, BigInteger.TEN),
        paymentSucceeded = true,
        withdrawalLockPeriod = BigInteger.ONE,
        recurringBuyFrequency = RecurringBuyFrequency.DAILY,
        recurringBuyState = RecurringBuyState.ACTIVE,
        eligibleAndNextPaymentRecurringBuy = listOf(),
        googlePayDetails = GooglePayDetails(
            tokenizationInfo = mapOf("1" to "2"),
            beneficiaryId = "id_googlePayBeneficiaryId",
            merchantBankCountryCode = "cc_googlePayMerchantBankCountryCode",
            allowPrepaidCards = false,
            allowCreditCards = false,
            allowedAuthMethods = defaultAllowedAuthMethods,
            allowedCardNetworks = defaultAllowedCardNetworks,
            billingAddressRequired = true,
            billingAddressParameters = BillingAddressParameters()
        )
        // rest is @Transient

        // add any non transient here to make sure parsing is always successful
        // because one might miss @Contextual where it should be
    )

    private val simpleBuyStateKtxString =
        """{"id":"id_SimpleBuyState","fiatCurrency":{"currencyCode":"EUR"},"amount":{"currency":{"currencyCode":"EUR"},"amount":"10.00"},"selectedCryptoAsset":"BTC","orderState":"AWAITING_FUNDS","kycStartedButNotCompleted":true,"kycVerificationState":"PENDING","currentScreen":"KYC","selectedPaymentMethod":{"id":"id_SelectedPaymentMethod","partner":"CARDPROVIDER","label":"label_SelectedPaymentMethod","paymentMethodType":"BANK_ACCOUNT","isEligible":true},"quote":{"id":"id_BuyQuote","price":{"currency":{"currencyCode":"EUR"},"amount":"20.00"},"availability":"REGULAR","quoteMargin":2000.0,"feeDetails":{"feeBeforePromo":{"currency":{"currencyCode":"EUR"},"amount":"20.01"},"fee":{"currency":{"currencyCode":"EUR"},"amount":"20.02"},"promo":"NEW_USER"},"createdAt":1000,"expiresAt":1200,"remainingTime":119,"chunksTimeCounter":[30,30,30,29],"depositTerms":{"creditCurrency":"USD","availableToTradeMinutesMin":1,"availableToTradeMinutesMax":2,"availableToTradeDisplayMode":"MAX_DAY","availableToWithdrawMinutesMin":1,"availableToWithdrawMinutesMax":2,"availableToWithdrawDisplayMode":"IMMEDIATELY","settlementType":"INSTANT","settlementReason":"GENERIC"}},"orderValue":{"currency":"BTC","amount":"10"},"paymentSucceeded":true,"withdrawalLockPeriod":"1","recurringBuyFrequency":"DAILY","recurringBuyState":"ACTIVE","googlePayDetails":{"tokenizationInfo":{"1":"2"},"beneficiaryId":"id_googlePayBeneficiaryId","merchantBankCountryCode":"cc_googlePayMerchantBankCountryCode","allowPrepaidCards":false,"allowedAuthMethods":["PAN_ONLY","CRYPTOGRAM_3DS"],"allowedCardNetworks":["AMEX","MASTERCARD","VISA"],"billingAddressRequired":true,"billingAddressParameters":{}}}"""

    @Before
    fun setUp() {
        every { assetCatalogue.assetInfoFromNetworkTicker(any()) } returns CryptoCurrency.BTC

        every { prefs.updateSimpleBuyState(any()) } just runs
        every { prefs.clearBuyState() } just runs
    }

    @Test
    fun `GIVEN ktx enabled, WHEN fetch is called, THEN simpleBuyStateObject should be returned`() {
        every { prefs.simpleBuyState() } returns simpleBuyStateKtxString

        val result: SimpleBuyState? = simpleBuyPrefsSerializer.fetch()

        assertEquals(simpleBuyStateObject, result)
    }

    @Test
    fun `WHEN update is called with simpleBuyStateObject, THEN prefs_updateSimpleBuyState should be called with simpleBuyStateKtxString`() {
        simpleBuyPrefsSerializer.update(simpleBuyStateObject)

        verify(exactly = 1) { prefs.updateSimpleBuyState(simpleBuyStateKtxString) }
    }

    @Test
    fun `WHEN clear is called, THEN prefs_clearBuyState should be called`() {
        simpleBuyPrefsSerializer.clear()

        verify(exactly = 1) { prefs.clearBuyState() }
    }
}
