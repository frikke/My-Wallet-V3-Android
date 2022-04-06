package piuk.blockchain.android.simplebuy

import com.blockchain.core.custodial.models.Availability
import com.blockchain.core.custodial.models.Promo
import com.blockchain.core.payments.model.Partner
import com.blockchain.nabu.datamanagers.OrderState
import com.blockchain.nabu.datamanagers.custodialwalletimpl.PaymentMethodType
import com.blockchain.nabu.models.data.RecurringBuyFrequency
import com.blockchain.nabu.models.data.RecurringBuyState
import com.blockchain.preferences.SimpleBuyPrefs
import com.blockchain.remoteconfig.FeatureFlag
import com.blockchain.serializers.BigDecimalSerializer
import com.blockchain.serializers.BigIntSerializer
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
            contextual(AssetInfoKSerializer(assetCatalogue = assetCatalogue))
        }
    }
    private val replaceGsonKtxFF: FeatureFlag = mockk()

    private val simpleBuyPrefsSerializer: SimpleBuyPrefsSerializer = SimpleBuyPrefsSerializerImpl(
        prefs = prefs,
        assetCatalogue = assetCatalogue,
        json = json,
        replaceGsonKtxFF = replaceGsonKtxFF
    )

    private val simpleBuyStateObject = SimpleBuyState(
        id = "id_SimpleBuyState",
        amount = FiatValue.fromMinor(EUR, 1000.toBigInteger()),
        fiatCurrency = EUR,
        selectedCryptoAsset = CryptoCurrency.BTC,
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
            )
        ),
        orderValue = CryptoValue(CryptoCurrency.BTC, BigInteger.TEN),
        supportedFiatCurrencies = listOf(EUR),
        paymentSucceeded = true,
        showRating = true,
        withdrawalLockPeriod = BigInteger.ONE,
        recurringBuyFrequency = RecurringBuyFrequency.DAILY,
        recurringBuyState = RecurringBuyState.ACTIVE,
        showRecurringBuyFirstTimeFlow = true,
        eligibleAndNextPaymentRecurringBuy = listOf(),
        googlePayTokenizationInfo = mapOf("1" to "2"),
        googlePayBeneficiaryId = "id_googlePayBeneficiaryId",
        googlePayMerchantBankCountryCode = "cc_googlePayMerchantBankCountryCode",
        googlePayAllowPrepaidCards = false,
        googlePayAllowCreditCards = false
        // rest is @Transient

        // add any non transient here to make sure parsing is always successful
        // because one might miss @Contextual where it should be
    )

    private val simpleBuyStateKtxString =
        """{"id":"id_SimpleBuyState","fiatCurrency":{"currencyCode":"EUR"},"amount":{"currency":{"currencyCode":"EUR"},"amount":"10.00"},"selectedCryptoAsset":"BTC","orderState":"AWAITING_FUNDS","kycStartedButNotCompleted":true,"kycVerificationState":"PENDING","currentScreen":"KYC","selectedPaymentMethod":{"id":"id_SelectedPaymentMethod","partner":"CARDPROVIDER","label":"label_SelectedPaymentMethod","paymentMethodType":"BANK_ACCOUNT","isEligible":true},"quote":{"id":"id_BuyQuote","price":{"currency":{"currencyCode":"EUR"},"amount":"20.00"},"availability":"REGULAR","quoteMargin":2000.0,"feeDetails":{"feeBeforePromo":{"currency":{"currencyCode":"EUR"},"amount":"20.01"},"fee":{"currency":{"currencyCode":"EUR"},"amount":"20.02"},"promo":"NEW_USER"}},"orderValue":{"currency":"BTC","amount":"10"},"supportedFiatCurrencies":[{"currencyCode":"EUR"}],"paymentSucceeded":true,"showRating":true,"withdrawalLockPeriod":"1","recurringBuyFrequency":"DAILY","recurringBuyState":"ACTIVE","showRecurringBuyFirstTimeFlow":true,"googlePayTokenizationInfo":{"1":"2"},"googlePayBeneficiaryId":"id_googlePayBeneficiaryId","googlePayMerchantBankCountryCode":"cc_googlePayMerchantBankCountryCode","googlePayAllowPrepaidCards":false,"googlePayAllowCreditCards":false}"""
    private val simpleBuyStateGsonString =
        """{"id":"id_SimpleBuyState","fiatCurrency":{"currencyCode":"EUR"},"amount":{"currency":{"currencyCode":"EUR"},"amount":10.00,"symbol":"€"},"selectedCryptoAsset":"BTC","orderState":"AWAITING_FUNDS","kycStartedButNotCompleted":true,"kycVerificationState":"PENDING","currentScreen":"KYC","selectedPaymentMethod":{"id":"id_SelectedPaymentMethod","partner":"CARDPROVIDER","label":"label_SelectedPaymentMethod","paymentMethodType":"BANK_ACCOUNT","isEligible":true},"quote":{"id":"id_BuyQuote","price":{"currency":{"currencyCode":"EUR"},"amount":20.00,"symbol":"€"},"availability":"REGULAR","quoteMargin":2000.0,"feeDetails":{"feeBeforePromo":{"currency":{"currencyCode":"EUR"},"amount":20.01,"symbol":"€"},"fee":{"currency":{"currencyCode":"EUR"},"amount":20.02,"symbol":"€"},"promo":"NEW_USER"}},"orderValue":{"currency":"BTC","amount":10,"maxDecimalPlaces":8,"userDecimalPlaces":8,"symbol":"BTC"},"supportedFiatCurrencies":[{"currencyCode":"EUR"}],"paymentSucceeded":true,"showRating":true,"withdrawalLockPeriod":1,"recurringBuyFrequency":"DAILY","recurringBuyState":"ACTIVE","showRecurringBuyFirstTimeFlow":true,"eligibleAndNextPaymentRecurringBuy":[],"googlePayTokenizationInfo":{"1":"2"},"googlePayBeneficiaryId":"id_googlePayBeneficiaryId","googlePayMerchantBankCountryCode":"cc_googlePayMerchantBankCountryCode","googlePayAllowPrepaidCards":false,"googlePayAllowCreditCards":false}"""

    @Before
    fun setUp() {
        every { assetCatalogue.assetInfoFromNetworkTicker(any()) } returns CryptoCurrency.BTC

        every { prefs.updateSimpleBuyState(any()) } just runs
        every { prefs.clearBuyState() } just runs
    }

    @Test
    fun `GIVEN ktx enabled, WHEN fetch is called, THEN simpleBuyStateObject should be returned`() {
        every { replaceGsonKtxFF.isEnabled } returns true
        every { prefs.simpleBuyState() } returns simpleBuyStateKtxString

        val result: SimpleBuyState? = simpleBuyPrefsSerializer.fetch()

        assertEquals(simpleBuyStateObject, result)
    }

    @Test
    fun `GIVEN ktx enabled, WHEN update is called with simpleBuyStateObject, THEN prefs_updateSimpleBuyState should be called with simpleBuyStateKtxString`() {
        every { replaceGsonKtxFF.isEnabled } returns true

        simpleBuyPrefsSerializer.update(simpleBuyStateObject)

        verify(exactly = 1) { prefs.updateSimpleBuyState(simpleBuyStateKtxString) }
    }

    @Test
    fun `WHEN clear is called, THEN prefs_clearBuyState should be called`() {
        simpleBuyPrefsSerializer.clear()

        verify(exactly = 1) { prefs.clearBuyState() }
    }

    @Test
    fun `GIVEN gson enabled, WHEN fetch is called, THEN simpleBuyStateObject should be returned`() {
        every { replaceGsonKtxFF.isEnabled } returns false
        every { prefs.simpleBuyState() } returns simpleBuyStateGsonString

        val result: SimpleBuyState? = simpleBuyPrefsSerializer.fetch()

        assertEquals(simpleBuyStateObject, result)
    }

    @Test
    fun `GIVEN gson enabled, WHEN update is called with simpleBuyStateObject, THEN prefs_updateSimpleBuyState should be called with simpleBuyStateGsonString`() {
        every { replaceGsonKtxFF.isEnabled } returns false

        simpleBuyPrefsSerializer.update(simpleBuyStateObject)

        verify(exactly = 1) { prefs.updateSimpleBuyState(simpleBuyStateGsonString) }
    }
}
