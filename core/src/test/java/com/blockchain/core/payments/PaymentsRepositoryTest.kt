package com.blockchain.core.payments

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.blockchain.android.testutils.rxInit
import com.blockchain.api.adapters.ApiError
import com.blockchain.api.paymentmethods.models.ActivateCardResponse
import com.blockchain.api.paymentmethods.models.AddNewCardResponse
import com.blockchain.api.paymentmethods.models.CardProviderResponse
import com.blockchain.api.paymentmethods.models.CardResponse
import com.blockchain.api.paymentmethods.models.EveryPayCardCredentialsResponse
import com.blockchain.api.paymentmethods.models.GooglePayResponse
import com.blockchain.api.payments.data.BankInfoResponse
import com.blockchain.api.payments.data.BankTransferChargeAttributes
import com.blockchain.api.payments.data.BankTransferChargeResponse
import com.blockchain.api.payments.data.BankTransferFiatAmount
import com.blockchain.api.payments.data.BankTransferPaymentResponse
import com.blockchain.api.payments.data.CreateLinkBankResponse
import com.blockchain.api.payments.data.CreateLinkBankResponse.Companion.PLAID_PARTNER
import com.blockchain.api.payments.data.CreateLinkBankResponse.Companion.YAPILY_PARTNER
import com.blockchain.api.payments.data.CreateLinkBankResponse.Companion.YODLEE_PARTNER
import com.blockchain.api.payments.data.FastlinkParamsResponse
import com.blockchain.api.payments.data.LinkBankAttrsResponse
import com.blockchain.api.payments.data.LinkPlaidAccountBody
import com.blockchain.api.payments.data.LinkedBankTransferAttributesResponse
import com.blockchain.api.payments.data.LinkedBankTransferResponse
import com.blockchain.api.payments.data.OpenBankingTokenBody
import com.blockchain.api.payments.data.YapilyCountryResponse
import com.blockchain.api.payments.data.YapilyInstitutionResponse
import com.blockchain.api.payments.data.YapilyMediaResponse
import com.blockchain.api.services.CollateralLock
import com.blockchain.api.services.CollateralLocks
import com.blockchain.api.services.PaymentMethodsService
import com.blockchain.api.services.PaymentsService
import com.blockchain.auth.AuthHeaderProvider
import com.blockchain.core.custodial.TradingBalanceDataManager
import com.blockchain.core.payments.cache.LinkedCardsStore
import com.blockchain.domain.paymentmethods.model.BankPartner
import com.blockchain.domain.paymentmethods.model.BankProviderAccountAttributes
import com.blockchain.domain.paymentmethods.model.BankState
import com.blockchain.domain.paymentmethods.model.BankTransferStatus
import com.blockchain.domain.paymentmethods.model.BillingAddress
import com.blockchain.domain.paymentmethods.model.CardStatus
import com.blockchain.domain.paymentmethods.model.CardToBeActivated
import com.blockchain.domain.paymentmethods.model.EveryPayCredentials
import com.blockchain.domain.paymentmethods.model.FundsLock
import com.blockchain.domain.paymentmethods.model.FundsLocks
import com.blockchain.domain.paymentmethods.model.GooglePayInfo
import com.blockchain.domain.paymentmethods.model.InstitutionCountry
import com.blockchain.domain.paymentmethods.model.LinkBankTransfer
import com.blockchain.domain.paymentmethods.model.LinkedBankErrorState
import com.blockchain.domain.paymentmethods.model.LinkedBankState
import com.blockchain.domain.paymentmethods.model.LinkedPaymentMethod
import com.blockchain.domain.paymentmethods.model.Partner
import com.blockchain.domain.paymentmethods.model.PartnerCredentials
import com.blockchain.domain.paymentmethods.model.PaymentLimits
import com.blockchain.domain.paymentmethods.model.PaymentMethodDetails
import com.blockchain.domain.paymentmethods.model.PaymentMethodDetailsError
import com.blockchain.domain.paymentmethods.model.PaymentMethodType
import com.blockchain.domain.paymentmethods.model.PaymentMethodsError
import com.blockchain.domain.paymentmethods.model.PlaidAttributes
import com.blockchain.domain.paymentmethods.model.YapilyAttributes
import com.blockchain.domain.paymentmethods.model.YapilyInstitution
import com.blockchain.domain.paymentmethods.model.YodleeAttributes
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.doOnFailure
import com.blockchain.outcome.doOnSuccess
import com.blockchain.payments.googlepay.manager.GooglePayManager
import com.blockchain.preferences.SimpleBuyPrefs
import com.blockchain.store.StoreRequest
import com.blockchain.store.StoreResponse
import com.blockchain.testutils.CoroutineTestRule
import com.blockchain.testutils.MockKRule
import com.blockchain.utils.toZonedDateTime
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.Money
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import java.math.BigInteger
import java.util.Date
import junit.framework.Assert.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule

@OptIn(ExperimentalCoroutinesApi::class)
class PaymentsRepositoryTest {

    @get:Rule
    val initSchedulers = rxInit {
        mainTrampoline()
        computationTrampoline()
        ioTrampoline()
    }

    @get:Rule
    val mockKRule = MockKRule()

    @get:Rule
    val instantTaskExecutorRule: TestRule = InstantTaskExecutorRule()

    @ExperimentalCoroutinesApi
    @get:Rule
    var coroutineTestRule = CoroutineTestRule()

    private companion object {
        private const val AUTH = "auth"
        private const val ID = "id"
        private const val NETWORK_TICKER = "GBP"
        private const val APPLICATION_ID = "applicationId"
    }

    private val paymentsService: PaymentsService = mockk()
    private val paymentMethodsService: PaymentMethodsService = mockk(relaxed = true)
    private val linkedCardsStore: LinkedCardsStore = mockk(relaxed = true)
    private val tradingBalanceDataManager: TradingBalanceDataManager = mockk()
    private val assetCatalogue: AssetCatalogue = mockk()
    private val simpleBuyPrefs: SimpleBuyPrefs = mockk()
    private val authenticator: AuthHeaderProvider = mockk<AuthHeaderProvider>().apply {
        coEvery { getAuthHeader() } returns Single.just(AUTH)
    }
    private val googlePayManager: GooglePayManager = mockk()

    private val environmentConfig: EnvironmentConfig = mockk<EnvironmentConfig>().apply {
        every { applicationId } returns APPLICATION_ID
    }

    private val plaidFeatureFlag: FeatureFlag = mockk(relaxed = true)

    private lateinit var subject: PaymentsRepository

    @Before
    fun setUp() {
        subject = PaymentsRepository(
            paymentsService,
            paymentMethodsService,
            linkedCardsStore,
            tradingBalanceDataManager,
            assetCatalogue,
            simpleBuyPrefs,
            authenticator,
            googlePayManager,
            environmentConfig,
            plaidFeatureFlag
        )
    }

    // /////////////////////////////////////////
    // // BankService //////////////////////////
    // /////////////////////////////////////////

    @Test
    fun `getLinkedBank() success`() {
        // ARRANGE
        val linkedBankTransferResponse = LinkedBankTransferResponse(
            id = "id",
            partner = CreateLinkBankResponse.YAPILY_PARTNER,
            state = LinkedBankTransferResponse.ACTIVE,
            currency = NETWORK_TICKER,
            details = null,
            error = null,
            attributes = LinkedBankTransferAttributesResponse(null, null, null, "callback"),
            ux = null
        )
        val fiatCurrency: FiatCurrency = mockk()
        every { assetCatalogue.fiatFromNetworkTicker(NETWORK_TICKER) } returns fiatCurrency
        every { paymentMethodsService.getLinkedBank(AUTH, ID) } returns Single.just(linkedBankTransferResponse)

        // ASSERT
        subject.getLinkedBank(ID).test()
            .assertValue {
                it.id == "id" &&
                    it.currency == fiatCurrency &&
                    it.partner == BankPartner.YAPILY &&
                    it.state == LinkedBankState.ACTIVE &&
                    it.errorStatus == LinkedBankErrorState.NONE &&
                    it.callbackPath == "callback"
            }
    }

    @Test
    fun `getLinkedBank() YAPILY - missing callback path should throw error`() {
        // ARRANGE
        val linkedBankTransferResponse = LinkedBankTransferResponse(
            id = "id",
            partner = CreateLinkBankResponse.YAPILY_PARTNER,
            state = LinkedBankTransferResponse.ACTIVE,
            currency = NETWORK_TICKER,
            details = null,
            error = null,
            attributes = null,
            ux = null
        )
        val fiatCurrency: FiatCurrency = mockk()
        every { assetCatalogue.fiatFromNetworkTicker(NETWORK_TICKER) } returns fiatCurrency
        every { paymentMethodsService.getLinkedBank(AUTH, ID) } returns Single.just(linkedBankTransferResponse)

        // ASSERT
        subject.getLinkedBank(ID).test()
            .assertError { it is IllegalArgumentException }
    }

    @Test
    fun `getWithdrawalLocks() should return FundsLocks`() {
        // ARRANGE
        val date = Date().toInstant().toString()
        val locks = CollateralLocks(
            NETWORK_TICKER,
            "1000",
            listOf(
                CollateralLock(NETWORK_TICKER, "10", date)
            )
        )
        val localCurrency =
            mockk<FiatCurrency>(relaxed = true).apply { every { networkTicker } returns (NETWORK_TICKER) }
        every { paymentsService.getWithdrawalLocks(AUTH, NETWORK_TICKER) } returns Single.just(locks)

        // ASSERT
        subject.getWithdrawalLocks(localCurrency).test()
            .assertValue(
                FundsLocks(
                    Money.fromMinor(localCurrency, BigInteger("1000")),
                    listOf(
                        FundsLock(
                            Money.fromMinor(localCurrency, BigInteger("10")),
                            date.toZonedDateTime()
                        )
                    )
                )
            )
    }

    @Test
    fun `getLinkedBanks() should return list of banks`() {
        // ARRANGE
        val bankResponse = BankInfoResponse(
            id = ID,
            name = "name",
            currency = NETWORK_TICKER,
            state = BankInfoResponse.ACTIVE,
            isBankAccount = true,
            isBankTransferAccount = true,
            accountName = null,
            accountNumber = null,
            attributes = null,
            bankAccountType = null
        )
        val fiatCurrency: FiatCurrency = mockk()
        every { assetCatalogue.fiatFromNetworkTicker(NETWORK_TICKER) } returns fiatCurrency
        every { paymentMethodsService.getBanks(AUTH) } returns Single.just(listOf(bankResponse))

        // ASSERT
        subject.getLinkedBanks().test()
            .assertValue {
                val bank = it.first()
                bank.name == bankResponse.name &&
                    bank.accountEnding == "****" &&
                    bank.state == BankState.ACTIVE &&
                    bank.currency == fiatCurrency
            }
    }

    @Test
    fun `removeBank() - BANK_ACCOUNT`() {
        // ARRANGE
        val bank = mockk<LinkedPaymentMethod.Bank>(relaxed = true).apply {
            every { type } returns PaymentMethodType.BANK_ACCOUNT
            every { id } returns ID
        }
        every { paymentMethodsService.removeBeneficiary(AUTH, ID) } returns Completable.complete()

        // ASSERT
        subject.removeBank(bank).test().assertComplete()
    }

    @Test
    fun `removeBank() - invalid type should throw exception`() {
        // ARRANGE
        val bank = mockk<LinkedPaymentMethod.Bank>(relaxed = true).apply {
            every { type } returns PaymentMethodType.UNKNOWN
            every { id } returns ID
        }

        // ASSERT
        subject.removeBank(bank).test()
            .assertError { it is IllegalStateException }
    }

    @Test
    fun `startBankTransfer() should return paymentId`() {
        // ARRANGE
        val response = mockk<BankTransferPaymentResponse>(relaxed = true).apply {
            every { paymentId } returns ID
        }
        every { paymentMethodsService.startBankTransferPayment(any(), any(), any()) } returns Single.just(response)

        // ASSERT
        subject.startBankTransfer(ID, Money.zero(FiatCurrency.Dollars), NETWORK_TICKER).test()
            .assertValue(ID)
    }

    @Test
    fun `updateSelectedBankAccount() - success`() {
        // ARRANGE
        val domainAttrs = BankProviderAccountAttributes()

        every {
            paymentMethodsService.updateAccountProviderId(
                eq(AUTH), eq(ID), any()
            )
        } returns
            Completable.complete()

        // ASSERT
        subject.updateSelectedBankAccount(ID, "", "", domainAttrs).test()
            .assertComplete()
    }

    @Test
    fun `linkPlaidAccount() - success`() {
        // ARRANGE
        val attrs = LinkPlaidAccountBody.Attributes("accountId", "publicToken")
        every { paymentMethodsService.linkPLaidAccount(AUTH, ID, LinkPlaidAccountBody(attrs)) } returns
            Completable.complete()

        // ASSERT
        subject.linkPlaidBankAccount(ID, attrs.accountId, attrs.publicToken).test()
            .assertComplete()
    }

    @Test
    fun `linkBank() - Plaid`() {
        // ARRANGE
        every { plaidFeatureFlag.enabled } returns (Single.just(true))
        val currency = mockk<FiatCurrency>(relaxed = true).apply { every { networkTicker } returns (NETWORK_TICKER) }
        val createLinkBankResponse = CreateLinkBankResponse(
            partner = PLAID_PARTNER,
            id = ID,
            attributes = LinkBankAttrsResponse(
                linkToken = "linkToken",
                linkUrl = "linkUrl",
                tokenExpiresAt = "tokenExpiresAt",
                token = null,
                fastlinkUrl = null,
                fastlinkParams = null,
                institutions = null,
                entity = null
            )
        )
        every {
            paymentMethodsService.linkBank(
                authorization = AUTH,
                fiatCurrency = NETWORK_TICKER,
                supportedPartners = listOf(PLAID_PARTNER, YODLEE_PARTNER, YAPILY_PARTNER),
                applicationId = APPLICATION_ID
            )
        } returns Single.just(createLinkBankResponse)

        // ASSERT
        subject.linkBank(currency).test()
            .assertValue(
                LinkBankTransfer(
                    id = ID,
                    partner = BankPartner.PLAID,
                    attributes = PlaidAttributes(
                        createLinkBankResponse.attributes!!.linkToken!!,
                        createLinkBankResponse.attributes!!.linkUrl!!,
                        createLinkBankResponse.attributes!!.tokenExpiresAt!!
                    )
                )
            )
    }

    @Test
    fun `linkBank() - Yodlee`() {
        // ARRANGE
        every { plaidFeatureFlag.enabled } returns (Single.just(false))
        val currency = mockk<FiatCurrency>(relaxed = true).apply { every { networkTicker } returns (NETWORK_TICKER) }
        val createLinkBankResponse = CreateLinkBankResponse(
            partner = YODLEE_PARTNER,
            id = ID,
            attributes = LinkBankAttrsResponse(
                linkToken = null,
                linkUrl = null,
                tokenExpiresAt = null,
                token = "token",
                fastlinkUrl = "fastLinkUrl",
                fastlinkParams = FastlinkParamsResponse(configName = "configName"),
                institutions = null,
                entity = null
            )
        )
        every {
            paymentMethodsService.linkBank(
                authorization = AUTH,
                fiatCurrency = NETWORK_TICKER,
                supportedPartners = emptyList(),
                applicationId = APPLICATION_ID
            )
        } returns Single.just(createLinkBankResponse)

        // ASSERT
        subject.linkBank(currency).test()
            .assertValue(
                LinkBankTransfer(
                    id = ID,
                    partner = BankPartner.YODLEE,
                    attributes = YodleeAttributes(
                        createLinkBankResponse.attributes!!.fastlinkUrl!!,
                        createLinkBankResponse.attributes!!.token!!,
                        createLinkBankResponse.attributes!!.fastlinkParams!!.configName
                    )
                )
            )
    }

    @Test
    fun `linkBank() - Yapily`() {
        // ARRANGE
        every { plaidFeatureFlag.enabled } returns (Single.just(false))
        val currency = mockk<FiatCurrency>(relaxed = true).apply { every { networkTicker } returns (NETWORK_TICKER) }
        val createLinkBankResponse = CreateLinkBankResponse(
            partner = YAPILY_PARTNER,
            id = ID,
            attributes = LinkBankAttrsResponse(
                linkToken = null,
                linkUrl = null,
                tokenExpiresAt = null,
                token = null,
                fastlinkUrl = null,
                fastlinkParams = null,
                institutions = listOf(
                    YapilyInstitutionResponse(
                        countries = listOf(YapilyCountryResponse("countryCode2", "displayName")),
                        fullName = "fullName",
                        id = "id",
                        media = listOf(YapilyMediaResponse("source", BankPartner.ICON))
                    )
                ),
                entity = "entity"
            )
        )
        every {
            paymentMethodsService.linkBank(
                authorization = AUTH,
                fiatCurrency = NETWORK_TICKER,
                supportedPartners = emptyList(),
                applicationId = APPLICATION_ID
            )
        } returns Single.just(createLinkBankResponse)

        // ASSERT
        subject.linkBank(currency).test()
            .assertValue(
                LinkBankTransfer(
                    id = ID,
                    partner = BankPartner.YAPILY,
                    attributes = YapilyAttributes(
                        entity = createLinkBankResponse.attributes!!.entity!!,
                        institutionList = listOf(
                            YapilyInstitution(
                                operatingCountries = listOf(
                                    InstitutionCountry(
                                        createLinkBankResponse.attributes!!.institutions!![0].countries[0].countryCode2,
                                        createLinkBankResponse.attributes!!.institutions!![0].countries[0].displayName
                                    )
                                ),
                                name = createLinkBankResponse.attributes!!.institutions!![0].fullName,
                                id = createLinkBankResponse.attributes!!.institutions!![0].id,
                                iconLink = null
                            )
                        )
                    )
                )
            )
    }

    @Test
    fun `linkBank() - invalid partner should throw error`() {
        // ARRANGE
        every { plaidFeatureFlag.enabled } returns (Single.just(false))
        val currency = mockk<FiatCurrency>(relaxed = true).apply { every { networkTicker } returns (NETWORK_TICKER) }
        val createLinkBankResponse = CreateLinkBankResponse(
            partner = "",
            id = ID,
            attributes = LinkBankAttrsResponse(
                linkToken = null,
                linkUrl = null,
                tokenExpiresAt = null,
                token = "token",
                fastlinkUrl = "fastLinkUrl",
                fastlinkParams = FastlinkParamsResponse(configName = "configName"),
                institutions = null,
                entity = null
            )
        )
        every {
            paymentMethodsService.linkBank(
                authorization = AUTH,
                fiatCurrency = NETWORK_TICKER,
                supportedPartners = emptyList(),
                applicationId = APPLICATION_ID
            )
        } returns Single.just(createLinkBankResponse)

        // ASSERT
        subject.linkBank(currency).test()
            .assertError { it is IllegalStateException }
    }

    @Test
    fun `linkBank() - missing attributes should throw error`() {
        // ARRANGE
        every { plaidFeatureFlag.enabled } returns (Single.just(false))
        val currency = mockk<FiatCurrency>(relaxed = true).apply { every { networkTicker } returns (NETWORK_TICKER) }
        val createLinkBankResponse = CreateLinkBankResponse(
            partner = YODLEE_PARTNER,
            id = ID,
            attributes = null
        )
        every {
            paymentMethodsService.linkBank(
                authorization = AUTH,
                fiatCurrency = NETWORK_TICKER,
                supportedPartners = emptyList(),
                applicationId = APPLICATION_ID
            )
        } returns Single.just(createLinkBankResponse)

        // ASSERT
        subject.linkBank(currency).test()
            .assertError { it is IllegalStateException }
    }

    @Test
    fun `updateOpenBankingConsent() - success`() {
        // ARRANGE
        val url = "url"
        val token = "token"
        every { paymentMethodsService.updateOpenBankingToken(url, AUTH, OpenBankingTokenBody(token)) } returns
            Completable.complete()

        // ASSERT
        subject.updateOpenBankingConsent(url, token).test()
            .assertComplete()
    }

    @Test
    fun `getBankTransferCharge() should return BankTransferDetails`() {
        // ARRANGE
        val fiatCurrency: FiatCurrency = mockk()
        val response = BankTransferChargeResponse(
            beneficiaryId = ID,
            state = BankTransferChargeAttributes.PENDING,
            amountMinor = "100",
            amount = BankTransferFiatAmount(NETWORK_TICKER, "10"),
            extraAttributes = BankTransferChargeAttributes("url", "status"),
            null
        )
        every { assetCatalogue.fromNetworkTicker(NETWORK_TICKER) } returns FiatCurrency.Dollars
        every { paymentMethodsService.getBankTransferCharge(AUTH, ID) } returns Single.just(response)

        // ASSERT
        subject.getBankTransferCharge(ID).test()
            .assertValue {
                it.id == ID &&
                    it.authorisationUrl == response.extraAttributes.authorisationUrl &&
                    it.status == BankTransferStatus.PENDING
            }
    }

    @Test
    fun `canTransactWithBankMethods() - unsupported currency`() {
        // ARRANGE
        val fiatCurrency = mockk<FiatCurrency>().apply { every { networkTicker } returns "invalid" }

        // ASSERT
        subject.canTransactWithBankMethods(fiatCurrency).test()
            .assertValue(false)
    }

    // /////////////////////////////////////////
    // // CardService //////////////////////////
    // /////////////////////////////////////////

    @Test
    fun `getLinkedCards() should return cached data from LinkedCardsStore`() = runTest {
        // ARRANGE
        val storeRequest: StoreRequest = mockk()
        val linkedCardsStoreFlow = MutableSharedFlow<StoreResponse<PaymentMethodsError, List<CardResponse>>>(replay = 1)
        val cardResponse = CardResponse(
            id = "id",
            partner = "CARDPROVIDER",
            state = CardResponse.ACTIVE,
            currency = NETWORK_TICKER
        )
        val fiatCurrency: FiatCurrency = mockk()
        every { assetCatalogue.fiatFromNetworkTicker(NETWORK_TICKER) } returns fiatCurrency
        every { linkedCardsStore.stream(storeRequest) } returns linkedCardsStoreFlow
        linkedCardsStoreFlow.emit(StoreResponse.Data(listOf(cardResponse)))

        // ACT
        val result = subject.getLinkedCards(storeRequest, CardStatus.ACTIVE)

        // ASSERT
        result.test {
            val cardResult = (expectMostRecentItem() as StoreResponse.Data<List<LinkedPaymentMethod.Card>>).data.first()
            assertTrue(
                cardResult.cardId == "id" &&
                    cardResult.partner == Partner.CARDPROVIDER &&
                    cardResult.status == CardStatus.ACTIVE
            )
        }
    }

    @Test
    fun `getLinkedCards() with invalid state should return empty list`() = runTest {
        // ARRANGE
        val storeRequest: StoreRequest = mockk()
        val linkedCardsStoreFlow = MutableSharedFlow<StoreResponse<PaymentMethodsError, List<CardResponse>>>(replay = 1)
        val cardResponse = CardResponse(
            id = "id",
            partner = "CARDPROVIDER",
            state = CardResponse.UNKNOWN,
            currency = NETWORK_TICKER
        )
        val fiatCurrency: FiatCurrency = mockk()
        every { assetCatalogue.fiatFromNetworkTicker(NETWORK_TICKER) } returns fiatCurrency
        every { linkedCardsStore.stream(storeRequest) } returns linkedCardsStoreFlow
        linkedCardsStoreFlow.emit(StoreResponse.Data(listOf(cardResponse)))

        // ACT
        val result = subject.getLinkedCards(storeRequest, CardStatus.ACTIVE)

        // ASSERT
        result.test {
            val cardResult = (expectMostRecentItem() as StoreResponse.Data<List<LinkedPaymentMethod.Card>>).data
            assertTrue(cardResult.isEmpty())
        }
    }

    @Test
    fun `addNewCard() should add card and clear cache`() {
        // ARRANGE
        val billingAddress = BillingAddress(
            "countryCode", "fullName", "address1", "address2", "city", "postCode", null
        )
        val fiatCurrency: FiatCurrency = mockk<FiatCurrency>().apply { every { networkTicker } returns NETWORK_TICKER }
        every { paymentMethodsService.addNewCard(eq(AUTH), any()) } returns
            Single.just(AddNewCardResponse(id = ID, partner = "EVERYPAY"))

        // ASSERT
        subject.addNewCard(fiatCurrency, billingAddress).test()
            .assertValue(CardToBeActivated(cardId = ID, partner = Partner.EVERYPAY))
            .assertComplete()
        verify {
            linkedCardsStore.markAsStale()
        }
    }

    @Test
    fun `activateCard() - EVERYPAY`() {
        // ARRANGE
        val cardResponse = ActivateCardResponse(
            everypay = EveryPayCardCredentialsResponse("name", "token", "link"),
            cardProvider = null
        )
        every { paymentMethodsService.activateCard(eq(AUTH), any(), any()) } returns Single.just(cardResponse)

        // ASSERT
        subject.activateCard(ID, "redirectUrl", "cvv").test()
            .assertValue(
                PartnerCredentials.EverypayPartner(
                    EveryPayCredentials("name", "token", "link")
                )
            )
        verify {
            linkedCardsStore.markAsStale()
        }
    }

    @Test
    fun `activateCard() - CARD PROVIDER`() {
        // ARRANGE
        val cardProviderResponse = mockk<CardProviderResponse>(relaxed = true).apply {
            coEvery { cardAcquirerName } returns "STRIPE"
            coEvery { cardAcquirerAccountCode } returns "code"
        }
        val cardResponse = ActivateCardResponse(
            everypay = null,
            cardProvider = cardProviderResponse
        )
        every { paymentMethodsService.activateCard(eq(AUTH), any(), any()) } returns Single.just(cardResponse)

        // ASSERT
        subject.activateCard(ID, "redirectUrl", "cvv").test()
            .assertValue {
                val provider = (it as PartnerCredentials.CardProviderPartner).cardProvider

                provider.cardAcquirerName == cardProviderResponse.cardAcquirerName &&
                    provider.cardAcquirerAccountCode == cardProviderResponse.cardAcquirerAccountCode
            }
        verify {
            linkedCardsStore.markAsStale()
        }
    }

    @Test
    fun `activateCard() - CARD UNKNOWN`() {
        // ARRANGE
        val cardResponse = ActivateCardResponse(
            everypay = null,
            cardProvider = null
        )
        every { paymentMethodsService.activateCard(eq(AUTH), any(), any()) } returns Single.just(cardResponse)

        // ASSERT
        subject.activateCard(ID, "redirectUrl", "cvv").test()
            .assertValue(PartnerCredentials.Unknown)
        verify {
            linkedCardsStore.markAsStale()
        }
    }

    @Test
    fun `getCardDetails() should return card`() {
        // ARRANGE
        val cardResponse = CardResponse(
            id = "id",
            partner = "CARDPROVIDER",
            state = CardResponse.ACTIVE,
            currency = NETWORK_TICKER
        )
        every { assetCatalogue.fiatFromNetworkTicker(NETWORK_TICKER) } returns mockk()
        every { paymentMethodsService.getCardDetails(AUTH, ID) } returns Single.just(cardResponse)

        // ASSERT
        subject.getCardDetails(ID).test()
            .assertValue {
                it.limits == PaymentLimits(
                    BigInteger.ZERO,
                    BigInteger.ZERO,
                    FiatCurrency.fromCurrencyCode(NETWORK_TICKER)
                )
            }
    }

    @Test
    fun `deleteCard() should remove card and invalidate cache`() {
        // ARRANGE
        every { paymentMethodsService.deleteCard(AUTH, ID) } returns Completable.complete()

        // ASSERT
        subject.deleteCard(ID).test()
            .assertComplete()
        verify {
            linkedCardsStore.markAsStale()
        }
    }

    @Test
    fun `getGooglePayTokenizationParameters() should return GooglePayResponse`() {
        // ARRANGE
        val response = GooglePayResponse(
            beneficiaryID = "id",
            merchantBankCountryCode = "GB",
            googlePayParameters = "params",
            publishableApiKey = "apiKey",
            allowPrepaidCards = false
        )
        every { paymentMethodsService.getGooglePayInfo(AUTH, NETWORK_TICKER) } returns Single.just(response)

        // ASSERT
        subject.getGooglePayTokenizationParameters(NETWORK_TICKER).test()
            .assertValue(
                GooglePayInfo(
                    beneficiaryID = response.beneficiaryID,
                    merchantBankCountryCode = response.merchantBankCountryCode,
                    googlePayParameters = response.googlePayParameters,
                    publishableApiKey = response.publishableApiKey,
                    allowPrepaidCards = response.allowPrepaidCards,
                    allowCreditCards = response.allowCreditCards
                )
            )
    }

    // /////////////////////////////////////////
    // // PaymentMethodService /////////////////
    // /////////////////////////////////////////

    @Test
    fun `getPaymentMethodForId() - happy`() = runTest {
        // ARRANGE
        val paymentMethodDetails: PaymentMethodDetails = mockk()
        coEvery { paymentsService.getPaymentMethodDetailsForId(AUTH, ID) } returns
            Outcome.Success(paymentMethodDetails)

        // ACT
        val result = subject.getPaymentMethodDetailsForId(ID)

        // ASSERT
        result.doOnSuccess {
            assertEquals(paymentMethodDetails, it)
        }
    }

    @Test
    fun `getPaymentMethodForId() - HttpError`() = runTest {
        // ARRANGE
        coEvery { paymentsService.getPaymentMethodDetailsForId(AUTH, ID) } returns
            Outcome.Failure(ApiError.HttpError(Throwable()))

        // ACT
        val result = subject.getPaymentMethodDetailsForId(ID)

        // ASSERT
        result.doOnFailure {
            assertEquals(PaymentMethodDetailsError.REQUEST_FAILED, it)
        }
    }

    @Test
    fun `getPaymentMethodForId() - NetworkError`() = runTest {
        // ARRANGE
        coEvery { paymentsService.getPaymentMethodDetailsForId(AUTH, ID) } returns
            Outcome.Failure(ApiError.NetworkError(Throwable()))

        // ACT
        val result = subject.getPaymentMethodDetailsForId(ID)

        // ASSERT
        result.doOnFailure {
            assertEquals(PaymentMethodDetailsError.SERVICE_UNAVAILABLE, it)
        }
    }

    @Test
    fun `getPaymentMethodForId() - UnknownApiError`() = runTest {
        // ARRANGE
        coEvery { paymentsService.getPaymentMethodDetailsForId(AUTH, ID) } returns
            Outcome.Failure(ApiError.UnknownApiError(Throwable()))

        // ACT
        val result = subject.getPaymentMethodDetailsForId(ID)

        // ASSERT
        result.doOnFailure {
            assertEquals(PaymentMethodDetailsError.UNKNOWN, it)
        }
    }

    @Test
    fun `getPaymentMethodForId() - KnownError`() = runTest {
        // ARRANGE
        coEvery { paymentsService.getPaymentMethodDetailsForId(AUTH, ID) } returns
            Outcome.Failure(ApiError.KnownError(mockk(relaxed = true)))

        // ACT
        val result = subject.getPaymentMethodDetailsForId(ID)

        // ASSERT
        result.doOnFailure {
            assertEquals(PaymentMethodDetailsError.REQUEST_FAILED, it)
        }
    }
}
