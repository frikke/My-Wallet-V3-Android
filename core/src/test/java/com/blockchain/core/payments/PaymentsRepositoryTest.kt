package com.blockchain.core.payments

import app.cash.turbine.test
import com.blockchain.api.NabuApiException
import com.blockchain.api.NabuUxErrorResponse
import com.blockchain.api.brokerage.data.DepositTermsResponse
import com.blockchain.api.paymentmethods.models.ActivateCardResponse
import com.blockchain.api.paymentmethods.models.AddNewCardResponse
import com.blockchain.api.paymentmethods.models.AliasInfoResponse
import com.blockchain.api.paymentmethods.models.CardProviderResponse
import com.blockchain.api.paymentmethods.models.CardResponse
import com.blockchain.api.paymentmethods.models.DepositTermsRequestBody
import com.blockchain.api.paymentmethods.models.EveryPayCardCredentialsResponse
import com.blockchain.api.paymentmethods.models.GooglePayResponse
import com.blockchain.api.payments.data.BankInfoResponse
import com.blockchain.api.payments.data.BankTransferChargeAttributes
import com.blockchain.api.payments.data.BankTransferChargeResponse
import com.blockchain.api.payments.data.BankTransferFiatAmount
import com.blockchain.api.payments.data.BankTransferPaymentResponse
import com.blockchain.api.payments.data.CardDetailsResponse
import com.blockchain.api.payments.data.CreateLinkBankResponse
import com.blockchain.api.payments.data.CreateLinkBankResponse.Companion.PLAID_PARTNER
import com.blockchain.api.payments.data.CreateLinkBankResponse.Companion.YAPILY_PARTNER
import com.blockchain.api.payments.data.CreateLinkBankResponse.Companion.YODLEE_PARTNER
import com.blockchain.api.payments.data.ExtraAttributes
import com.blockchain.api.payments.data.FastlinkParamsResponse
import com.blockchain.api.payments.data.LinkBankAttrsResponse
import com.blockchain.api.payments.data.LinkPlaidAccountBody
import com.blockchain.api.payments.data.LinkedBankDetailsResponse
import com.blockchain.api.payments.data.LinkedBankTransferAttributesResponse
import com.blockchain.api.payments.data.LinkedBankTransferResponse
import com.blockchain.api.payments.data.OpenBankingTokenBody
import com.blockchain.api.payments.data.PaymentAccountResponse
import com.blockchain.api.payments.data.PaymentMethodDetailsResponse
import com.blockchain.api.payments.data.RefreshPlaidRequestBody
import com.blockchain.api.payments.data.RefreshPlaidResponse
import com.blockchain.api.payments.data.SettlementBody
import com.blockchain.api.payments.data.SettlementResponse
import com.blockchain.api.payments.data.YapilyCountryResponse
import com.blockchain.api.payments.data.YapilyInstitutionResponse
import com.blockchain.api.payments.data.YapilyMediaResponse
import com.blockchain.api.services.CollateralLock
import com.blockchain.api.services.CollateralLocks
import com.blockchain.api.services.PaymentMethodsService
import com.blockchain.api.services.PaymentsService
import com.blockchain.core.custodial.domain.TradingService
import com.blockchain.core.payments.cache.CardDetailsStore
import com.blockchain.core.payments.cache.LinkedBankStore
import com.blockchain.core.payments.cache.LinkedCardsStore
import com.blockchain.core.payments.cache.PaymentMethodsEligibilityStore
import com.blockchain.core.payments.cache.PaymentMethodsStore
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.domain.fiatcurrencies.FiatCurrenciesService
import com.blockchain.domain.fiatcurrencies.model.TradingCurrencies
import com.blockchain.domain.paymentmethods.model.AliasInfo
import com.blockchain.domain.paymentmethods.model.BankPartner
import com.blockchain.domain.paymentmethods.model.BankProviderAccountAttributes
import com.blockchain.domain.paymentmethods.model.BankState
import com.blockchain.domain.paymentmethods.model.BankTransferStatus
import com.blockchain.domain.paymentmethods.model.BillingAddress
import com.blockchain.domain.paymentmethods.model.CardStatus
import com.blockchain.domain.paymentmethods.model.CardToBeActivated
import com.blockchain.domain.paymentmethods.model.DepositTerms
import com.blockchain.domain.paymentmethods.model.EveryPayCredentials
import com.blockchain.domain.paymentmethods.model.FundsLock
import com.blockchain.domain.paymentmethods.model.FundsLocks
import com.blockchain.domain.paymentmethods.model.GooglePayInfo
import com.blockchain.domain.paymentmethods.model.InstitutionCountry
import com.blockchain.domain.paymentmethods.model.LinkBankTransfer
import com.blockchain.domain.paymentmethods.model.LinkedBankErrorState
import com.blockchain.domain.paymentmethods.model.LinkedBankState
import com.blockchain.domain.paymentmethods.model.LinkedPaymentMethod
import com.blockchain.domain.paymentmethods.model.MobilePaymentType
import com.blockchain.domain.paymentmethods.model.Partner
import com.blockchain.domain.paymentmethods.model.PartnerCredentials
import com.blockchain.domain.paymentmethods.model.PaymentLimits
import com.blockchain.domain.paymentmethods.model.PaymentMethodDetails
import com.blockchain.domain.paymentmethods.model.PaymentMethodType
import com.blockchain.domain.paymentmethods.model.PlaidAttributes
import com.blockchain.domain.paymentmethods.model.RefreshBankInfo
import com.blockchain.domain.paymentmethods.model.SettlementInfo
import com.blockchain.domain.paymentmethods.model.SettlementReason
import com.blockchain.domain.paymentmethods.model.SettlementType
import com.blockchain.domain.paymentmethods.model.YapilyAttributes
import com.blockchain.domain.paymentmethods.model.YapilyInstitution
import com.blockchain.domain.paymentmethods.model.YodleeAttributes
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.doOnFailure
import com.blockchain.outcome.doOnSuccess
import com.blockchain.payments.googlepay.manager.GooglePayManager
import com.blockchain.payments.googlepay.manager.request.defaultAllowedAuthMethods
import com.blockchain.payments.googlepay.manager.request.defaultAllowedCardNetworks
import com.blockchain.preferences.SimpleBuyPrefs
import com.blockchain.testutils.CoroutineTestRule
import com.blockchain.testutils.GBP
import com.blockchain.testutils.MockKRule
import com.blockchain.testutils.USD
import com.blockchain.testutils.rxInit
import com.blockchain.testutils.usd
import com.blockchain.utils.toZonedDateTime
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.CryptoCurrency
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

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

    @ExperimentalCoroutinesApi
    @get:Rule
    var coroutineTestRule = CoroutineTestRule()

    private companion object {
        private const val AUTH = "auth"
        private const val ID = "id"
        private const val NETWORK_TICKER = "GBP"
        private const val BUY_NETWORK_TICKER = "BTC"
        private const val APPLICATION_ID = "applicationId"
    }

    private val paymentsService: PaymentsService = mockk()
    private val paymentMethodsStore: PaymentMethodsStore = mockk()
    private val paymentMethodsEligibilityStore: PaymentMethodsEligibilityStore = mockk()
    private val paymentMethodsService: PaymentMethodsService = mockk(relaxed = true)
    private val linkedCardsStore: LinkedCardsStore = mockk(relaxed = true)
    private val cardDetailsStore: CardDetailsStore = mockk(relaxed = true)
    private val linkedBankStore: LinkedBankStore = mockk(relaxed = true)
    private val tradingService: TradingService = mockk()
    private val assetCatalogue: AssetCatalogue = mockk()
    private val simpleBuyPrefs: SimpleBuyPrefs = mockk()
    private val googlePayManager: GooglePayManager = mockk()

    private val environmentConfig: EnvironmentConfig = mockk<EnvironmentConfig>().apply {
        every { applicationId } returns APPLICATION_ID
    }
    private val googlePayFeatureFlag: FeatureFlag = mockk<FeatureFlag>().apply {
        every { enabled } returns Single.just(true)
    }
    private val plaidFeatureFlag: FeatureFlag = mockk(relaxed = true)
    private val withdrawLocksStore: WithdrawLocksStore = mockk()
    private val fiatCurrenciesService: FiatCurrenciesService = mockk()

    private lateinit var subject: PaymentsRepository

    @Before
    fun setUp() {
        subject = PaymentsRepository(
            paymentsService = paymentsService,
            paymentMethodsStore = paymentMethodsStore,
            paymentMethodsEligibilityStore = paymentMethodsEligibilityStore,
            paymentMethodsService = paymentMethodsService,
            cardDetailsStore = cardDetailsStore,
            linkedCardsStore = linkedCardsStore,
            linkedBankStore = linkedBankStore,
            tradingService = tradingService,
            assetCatalogue = assetCatalogue,
            simpleBuyPrefs = simpleBuyPrefs,
            withdrawLocksStore = withdrawLocksStore,
            googlePayManager = googlePayManager,
            environmentConfig = environmentConfig,
            fiatCurrenciesService = fiatCurrenciesService,
            googlePayFeatureFlag = googlePayFeatureFlag,
            plaidFeatureFlag = plaidFeatureFlag
        )
    }

    // /////////////////////////////////////////
    // // BankService //////////////////////////
    // /////////////////////////////////////////

    @Test
    fun `getLinkedBankLegacy() success`() = runTest {
        // ARRANGE
        val linkedBankStoreFlow = MutableSharedFlow<DataResource<LinkedBankTransferResponse>>(replay = 1)
        val linkedBankTransferResponse = LinkedBankTransferResponse(
            id = "id",
            partner = YAPILY_PARTNER,
            state = LinkedBankTransferResponse.ACTIVE,
            currency = NETWORK_TICKER,
            details = null,
            error = null,
            attributes = LinkedBankTransferAttributesResponse(null, null, null, "callback"),
            ux = null
        )
        val fiatCurrency: FiatCurrency = mockk()
        every { assetCatalogue.fiatFromNetworkTicker(NETWORK_TICKER) } returns fiatCurrency
        every { linkedBankStore.stream(any()) } returns linkedBankStoreFlow
        linkedBankStoreFlow.emit(DataResource.Data(linkedBankTransferResponse))

        // ASSERT
        subject.getLinkedBankLegacy(ID).test()
            .await()
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
    fun `getLinkedBank() success`() = runTest {
        // ARRANGE
        val linkedBankStoreFlow = MutableSharedFlow<DataResource<LinkedBankTransferResponse>>(replay = 1)
        val linkedBankTransferResponse = LinkedBankTransferResponse(
            id = "id",
            partner = YAPILY_PARTNER,
            state = LinkedBankTransferResponse.ACTIVE,
            currency = NETWORK_TICKER,
            details = null,
            error = null,
            attributes = LinkedBankTransferAttributesResponse(null, null, null, "callback"),
            ux = null
        )
        val fiatCurrency: FiatCurrency = mockk()
        every { assetCatalogue.fiatFromNetworkTicker(NETWORK_TICKER) } returns fiatCurrency
        every { linkedBankStore.stream(any()) } returns linkedBankStoreFlow
        linkedBankStoreFlow.emit(DataResource.Data(linkedBankTransferResponse))

        // ASSERT
        subject.getLinkedBank(ID).test {
            awaitItem().run {
                assertTrue { this is DataResource.Data }
                (this as DataResource.Data).data.run {
                    assertTrue {
                        id == "id" &&
                            currency == fiatCurrency &&
                            partner == BankPartner.YAPILY &&
                            state == LinkedBankState.ACTIVE &&
                            errorStatus == LinkedBankErrorState.NONE &&
                            callbackPath == "callback"
                    }
                }
            }
        }
    }

    @Test
    fun `getLinkedBankLegacy() YAPILY - missing callback path should throw error`() = runTest {
        // ARRANGE
        val linkedBankStoreFlow = MutableSharedFlow<DataResource<LinkedBankTransferResponse>>(replay = 1)
        val linkedBankTransferResponse = LinkedBankTransferResponse(
            id = "id",
            partner = YAPILY_PARTNER,
            state = LinkedBankTransferResponse.ACTIVE,
            currency = NETWORK_TICKER,
            details = null,
            error = null,
            attributes = null,
            ux = null
        )
        val fiatCurrency: FiatCurrency = mockk()
        every { assetCatalogue.fiatFromNetworkTicker(NETWORK_TICKER) } returns fiatCurrency
        every { linkedBankStore.stream(any()) } returns linkedBankStoreFlow
        linkedBankStoreFlow.emit(DataResource.Data(linkedBankTransferResponse))

        // ASSERT
        subject.getLinkedBankLegacy(ID).test()
            .await()
            .assertError { it is Exception }
    }

    @Test
    fun `getLinkedBank() YAPILY - missing callback path should throw error`() = runTest {
        // ARRANGE
        val linkedBankStoreFlow = MutableSharedFlow<DataResource<LinkedBankTransferResponse>>(replay = 1)
        val linkedBankTransferResponse = LinkedBankTransferResponse(
            id = "id",
            partner = YAPILY_PARTNER,
            state = LinkedBankTransferResponse.ACTIVE,
            currency = NETWORK_TICKER,
            details = null,
            error = null,
            attributes = null,
            ux = null
        )
        val fiatCurrency: FiatCurrency = mockk()
        every { assetCatalogue.fiatFromNetworkTicker(NETWORK_TICKER) } returns fiatCurrency
        every { linkedBankStore.stream(any()) } returns linkedBankStoreFlow
        linkedBankStoreFlow.emit(DataResource.Data(linkedBankTransferResponse))

        // ASSERT
        subject.getLinkedBank(ID).test {
            expectMostRecentItem().run {
                assertTrue { this is DataResource.Error }
            }
        }
    }

    @Test
    fun `getWithdrawalLocks() should return FundsLocks`() = runTest {
        // ARRANGE
        val date = Date().toInstant().toString()
        val locks = CollateralLocks(
            NETWORK_TICKER,
            "1000",
            listOf(
                CollateralLock(NETWORK_TICKER, "10", date, BUY_NETWORK_TICKER, "1000")
            )
        )
        val localCurrency =
            mockk<FiatCurrency>(relaxed = true) { every { networkTicker } returns (NETWORK_TICKER) }
        val buyCurrency =
            mockk<CryptoCurrency>(relaxed = true) { every { networkTicker } returns (BUY_NETWORK_TICKER) }
        every { withdrawLocksStore.stream(any()) } returns flowOf(DataResource.Data(locks))
        every { assetCatalogue.fromNetworkTicker(BUY_NETWORK_TICKER) } returns buyCurrency

        // ASSERT
        subject.getWithdrawalLocks(localCurrency).test {
            expectMostRecentItem().run {
                assertEquals(
                    DataResource.Data(
                        FundsLocks(
                            Money.fromMinor(localCurrency, BigInteger("1000")),
                            listOf(
                                FundsLock(
                                    Money.fromMinor(localCurrency, BigInteger("10")),
                                    date.toZonedDateTime(),
                                    Money.fromMinor(buyCurrency, BigInteger("1000")),
                                )
                            )
                        )
                    ),
                    this
                )
            }
        }
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
            bankAccountType = null,
            capabilities = null,
        )
        val fiatCurrency: FiatCurrency = mockk()
        every { assetCatalogue.fiatFromNetworkTicker(NETWORK_TICKER) } returns fiatCurrency
        every { paymentMethodsService.getBanks() } returns Single.just(listOf(bankResponse))

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
        every { paymentMethodsService.removeBeneficiary(ID) } returns Completable.complete()

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
        every { paymentMethodsService.startBankTransferPayment(any(), any()) } returns Single.just(response)

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
                eq(ID), any()
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
        every { paymentMethodsService.linkPLaidAccount(ID, LinkPlaidAccountBody(attrs)) } returns
            Completable.complete()

        // ASSERT
        subject.linkPlaidBankAccount(ID, attrs.accountId, attrs.publicToken).test()
            .assertComplete()
    }

    @Test
    fun `refreshPlaidBankAccount() should return RefreshBankInfo`() {
        // ARRANGE
        val requestBody = RefreshPlaidRequestBody(packageName = APPLICATION_ID)
        val response = RefreshPlaidResponse(
            PLAID_PARTNER,
            ID,
            RefreshPlaidResponse.Attributes("linkToken", "linkUrl", "tokenExpiresAt")
        )
        every { paymentMethodsService.refreshPlaidAccount(ID, requestBody) } returns Single.just(response)

        // ASSERT
        subject.refreshPlaidBankAccount(ID).test()
            .assertValue(
                RefreshBankInfo(BankPartner.PLAID, ID, "linkToken", "linkUrl", "tokenExpiresAt")
            )
    }

    @Test
    fun `getBeneficiaryInfo() - success`() = runTest {
        // Arrange
        val currency = "ARS"
        val alias = "alias"
        val mockAgent: AliasInfoResponse.Agent = mockk {
            every { bankName } returns "bankName"
            every { label } returns "alias"
            every { name } returns "accountHolder"
            every { accountType } returns "accountType"
            every { address } returns "cbu"
            every { holderDocument } returns "cuil"
        }
        val aliasInfoResponse: AliasInfoResponse = mockk {
            every { agent } returns mockAgent
            every { ux } returns null
        }
        coEvery {
            paymentMethodsService.getBeneficiaryInfo(
                currency = currency,
                address = alias
            )
        } returns Outcome.Success(aliasInfoResponse)

        // Act
        val result = subject.getBeneficiaryInfo(currency, alias)

        // Assert
        assertEquals(
            Outcome.Success(
                AliasInfo(
                    bankName = "bankName",
                    alias = "alias",
                    accountHolder = "accountHolder",
                    accountType = "accountType",
                    cbu = "cbu",
                    cuil = "cuil"
                )
            ),
            result
        )
    }

    @Test
    fun `getBeneficiaryInfo() - ux error`() = runTest {
        // Arrange
        val currency = "ARS"
        val address = "alias"
        val uxError: NabuUxErrorResponse = mockk(relaxed = true) {
            every { title } returns "title"
            every { message } returns "message"
        }
        val aliasInfoResponse: AliasInfoResponse = mockk {
            every { ux } returns uxError
        }
        coEvery {
            paymentMethodsService.getBeneficiaryInfo(
                currency = currency,
                address = address
            )
        } returns Outcome.Success(aliasInfoResponse)

        // Act
        val result = subject.getBeneficiaryInfo(currency, address)

        // Assert
        result.doOnFailure {
            assertTrue(it is NabuApiException)
        }
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
        every { paymentMethodsService.updateOpenBankingToken(url, OpenBankingTokenBody(token)) } returns
            Completable.complete()

        // ASSERT
        subject.updateOpenBankingConsent(url, token).test()
            .assertComplete()
    }

    @Test
    fun `getBankTransferCharge() should return BankTransferDetails`() {
        // ARRANGE
        val response = BankTransferChargeResponse(
            beneficiaryId = ID,
            state = BankTransferChargeAttributes.PENDING,
            amountMinor = "100",
            amount = BankTransferFiatAmount(NETWORK_TICKER, "10"),
            extraAttributes = BankTransferChargeAttributes("url", "status"),
            null,
            null
        )
        every { assetCatalogue.fromNetworkTicker(NETWORK_TICKER) } returns FiatCurrency.Dollars
        every { paymentMethodsService.getBankTransferCharge(ID) } returns Single.just(response)

        // ASSERT
        subject.getBankTransferCharge(ID).test()
            .assertValue {
                it.id == ID &&
                    it.authorisationUrl == response.extraAttributes?.authorisationUrl &&
                    it.status == BankTransferStatus.Pending
            }
    }

    @Test
    fun `canTransactWithBankMethods() - unsupported currency`() = runTest {
        // ARRANGE
        val tradingCurrencies = TradingCurrencies(
            selected = USD,
            allRecommended = listOf(GBP),
            allAvailable = listOf(GBP)
        )
        coEvery { fiatCurrenciesService.getTradingCurrencies() } returns
            Outcome.Success(tradingCurrencies)
        val fiatCurrency = mockk<FiatCurrency>().apply { every { networkTicker } returns "invalid" }

        // ASSERT
        subject.canTransactWithBankMethods(fiatCurrency).test()
            .assertValue(false)
    }

    @Test
    fun `checkSettlement() - valid SettlementReason`() {
        // ARRANGE
        val amount = 10.usd()
        val mockResponse = mockk<SettlementResponse.Attributes.SettlementResponse>(relaxed = true) {
            every { settlementType } returns SettlementType.INSTANT.toString()
            every { reason } returns SettlementReason.GENERIC.toString()
        }
        val mockAttributes = mockk<SettlementResponse.Attributes>(relaxed = true) {
            every { settlementResponse } returns mockResponse
        }
        val settlementResponse = mockk<SettlementResponse>(relaxed = true) {
            every { partner } returns PLAID_PARTNER
            every { state } returns BankInfoResponse.ACTIVE
            every { attributes } returns mockAttributes
        }
        every {
            paymentMethodsService.checkSettlement(
                ID,
                SettlementBody(
                    SettlementBody.Attributes(
                        SettlementBody.Attributes.SettlementRequest(amount = amount.toNetworkString())
                    )
                )
            )
        } returns Single.just(settlementResponse)

        // ASSERT
        subject.checkSettlement(ID, amount).test()
            .assertValue(
                SettlementInfo(
                    partner = BankPartner.PLAID,
                    state = BankState.ACTIVE,
                    settlementType = SettlementType.INSTANT,
                    settlementReason = SettlementReason.GENERIC
                )
            )
    }

    @Test
    fun `checkSettlement() - no SettlementReason`() {
        // ARRANGE
        val amount = 10.usd()
        val mockResponse = mockk<SettlementResponse.Attributes.SettlementResponse>(relaxed = true) {
            every { settlementType } returns SettlementType.INSTANT.toString()
            every { reason } returns null
        }
        val mockAttributes = mockk<SettlementResponse.Attributes>(relaxed = true) {
            every { settlementResponse } returns mockResponse
        }
        val settlementResponse = mockk<SettlementResponse>(relaxed = true) {
            every { partner } returns PLAID_PARTNER
            every { state } returns BankInfoResponse.ACTIVE
            every { attributes } returns mockAttributes
        }
        every {
            paymentMethodsService.checkSettlement(
                ID,
                SettlementBody(
                    SettlementBody.Attributes(
                        SettlementBody.Attributes.SettlementRequest(amount = amount.toNetworkString())
                    )
                )
            )
        } returns Single.just(settlementResponse)

        // ASSERT
        subject.checkSettlement(ID, amount).test()
            .assertValue(
                SettlementInfo(
                    partner = BankPartner.PLAID,
                    state = BankState.ACTIVE,
                    settlementType = SettlementType.INSTANT,
                    settlementReason = SettlementReason.NONE
                )
            )
    }

    @Test
    fun `getDepositTerms() success should return DepositTerms`() = runTest {
        // ARRANGE
        val response = DepositTermsResponse(
            creditCurrency = "USD",
            availableToTradeMinutesMin = 1,
            availableToTradeMinutesMax = 2,
            availableToTradeDisplayMode = "DAY_RANGE",
            availableToWithdrawMinutesMin = 3,
            availableToWithdrawMinutesMax = 4,
            availableToWithdrawDisplayMode = "MINUTE_RANGE",
            settlementType = "INSTANT",
            settlementReason = "REQUIRES_UPDATE"
        )
        val amount = Money.fromMinor(FiatCurrency.fromCurrencyCode("USD"), BigInteger.ONE)
        val requestBody = DepositTermsRequestBody(
            amount = DepositTermsRequestBody.Amount(
                value = amount.toBigInteger().toString(),
                symbol = amount.currencyCode
            ),
            paymentMethodId = "paymentMethodId"
        )

        coEvery { paymentMethodsService.getDepositTerms(requestBody) } returns Outcome.Success(response)

        // ACT
        val result = subject.getDepositTerms("paymentMethodId", amount)

        // ASSERT
        result.doOnSuccess {
            assertEquals(
                DepositTerms(
                    creditCurrency = "USD",
                    availableToTradeDisplayMode = DepositTerms.DisplayMode.DAY_RANGE,
                    availableToTradeMinutesMin = 1,
                    availableToTradeMinutesMax = 2,
                    availableToWithdrawDisplayMode = DepositTerms.DisplayMode.MINUTE_RANGE,
                    availableToWithdrawMinutesMin = 3,
                    availableToWithdrawMinutesMax = 4,
                    settlementType = SettlementType.INSTANT,
                    settlementReason = SettlementReason.REQUIRES_UPDATE
                ),
                it
            )
        }
    }

    @Test
    fun `getDepositTerms() failure should return error`() = runTest {
        // ARRANGE
        val error = Exception()
        val amount = Money.fromMinor(FiatCurrency.fromCurrencyCode("USD"), BigInteger.ONE)
        val requestBody = DepositTermsRequestBody(
            amount = DepositTermsRequestBody.Amount(
                value = amount.toBigInteger().toString(),
                symbol = amount.currencyCode
            ),
            paymentMethodId = "paymentMethodId"
        )

        coEvery { paymentMethodsService.getDepositTerms(requestBody) } returns Outcome.Failure(error)

        // ACT
        val result = subject.getDepositTerms("paymentMethodId", amount)

        // ASSERT
        result.doOnFailure {
            assertEquals(error, it)
        }
    }

    // /////////////////////////////////////////
    // // CardService //////////////////////////
    // /////////////////////////////////////////

    @Test
    fun `getLinkedCards() should return cached data from LinkedCardsStore`() = runTest {
        // ARRANGE
        val freshness: FreshnessStrategy = mockk()
        val linkedCardsStoreFlow = MutableSharedFlow<DataResource<List<CardResponse>>>(replay = 1)
        val cardResponse = CardResponse(
            id = "id",
            partner = "CARDPROVIDER",
            state = CardResponse.ACTIVE,
            currency = NETWORK_TICKER
        )
        val fiatCurrency: FiatCurrency = mockk()
        every { assetCatalogue.fiatFromNetworkTicker(NETWORK_TICKER) } returns fiatCurrency
        every { linkedCardsStore.stream(freshness) } returns linkedCardsStoreFlow
        linkedCardsStoreFlow.emit(DataResource.Data(listOf(cardResponse)))

        // ACT
        val result = subject.getLinkedCards(freshness, CardStatus.ACTIVE)

        // ASSERT
        result.test {
            val cardResult = (expectMostRecentItem() as DataResource.Data<List<LinkedPaymentMethod.Card>>).data.first()
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
        val freshness: FreshnessStrategy = mockk()
        val linkedCardsStoreFlow = MutableSharedFlow<DataResource<List<CardResponse>>>(replay = 1)
        val cardResponse = CardResponse(
            id = "id",
            partner = "CARDPROVIDER",
            state = CardResponse.UNKNOWN,
            currency = NETWORK_TICKER
        )
        val fiatCurrency: FiatCurrency = mockk()
        every { assetCatalogue.fiatFromNetworkTicker(NETWORK_TICKER) } returns fiatCurrency
        every { linkedCardsStore.stream(freshness) } returns linkedCardsStoreFlow
        linkedCardsStoreFlow.emit(DataResource.Data(listOf(cardResponse)))

        // ACT
        val result = subject.getLinkedCards(freshness, CardStatus.ACTIVE)

        // ASSERT
        result.test {
            val cardResult = (expectMostRecentItem() as DataResource.Data<List<LinkedPaymentMethod.Card>>).data
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
        every { paymentMethodsService.addNewCard(any()) } returns
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
        every { paymentMethodsService.activateCard(any(), any()) } returns Single.just(cardResponse)

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
        every { paymentMethodsService.activateCard(any(), any()) } returns Single.just(cardResponse)

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
        every { paymentMethodsService.activateCard(any(), any()) } returns Single.just(cardResponse)

        // ASSERT
        subject.activateCard(ID, "redirectUrl", "cvv").test()
            .assertValue(PartnerCredentials.Unknown)
        verify {
            linkedCardsStore.markAsStale()
        }
    }

    @Test
    fun `getCardDetailsLegacy() should return card`() = runTest {
        // ARRANGE
        val cardDetailsStoreFlow = MutableSharedFlow<DataResource<CardResponse>>(replay = 1)
        val cardResponse = CardResponse(
            id = "id",
            partner = "CARDPROVIDER",
            state = CardResponse.ACTIVE,
            currency = NETWORK_TICKER
        )
        every { assetCatalogue.fiatFromNetworkTicker(NETWORK_TICKER) } returns mockk()
        every { cardDetailsStore.stream(any()) } returns cardDetailsStoreFlow
        cardDetailsStoreFlow.emit(DataResource.Data(cardResponse))

        // ASSERT
        subject.getCardDetailsLegacy(ID).test()
            .await()
            .assertValue {
                it.limits == PaymentLimits(
                    BigInteger.ZERO,
                    BigInteger.ZERO,
                    FiatCurrency.fromCurrencyCode(NETWORK_TICKER)
                )
            }
    }

    @Test
    fun `getCardDetails() should return card`() = runTest {
        // ARRANGE
        val cardDetailsStoreFlow = MutableSharedFlow<DataResource<CardResponse>>(replay = 1)
        val cardResponse = CardResponse(
            id = "id",
            partner = "CARDPROVIDER",
            state = CardResponse.ACTIVE,
            currency = NETWORK_TICKER
        )
        every { assetCatalogue.fiatFromNetworkTicker(NETWORK_TICKER) } returns mockk()
        every { cardDetailsStore.stream(any()) } returns cardDetailsStoreFlow
        cardDetailsStoreFlow.emit(DataResource.Data(cardResponse))

        // ASSERT
        subject.getCardDetails(ID).test {
            awaitItem().run {
                assertTrue { this is DataResource.Data }
                (this as DataResource.Data).data.run {
                    assertTrue {
                        limits == PaymentLimits(
                            BigInteger.ZERO,
                            BigInteger.ZERO,
                            FiatCurrency.fromCurrencyCode(NETWORK_TICKER)
                        )
                    }
                }
            }
        }
    }

    @Test
    fun `getCardDetailsCo() should return card`() = runTest {
        // ARRANGE
        val cardResponse = CardResponse(
            id = "id",
            partner = "CARDPROVIDER",
            state = CardResponse.ACTIVE,
            currency = NETWORK_TICKER
        )
        every { assetCatalogue.fiatFromNetworkTicker(NETWORK_TICKER) } returns mockk()
        coEvery { paymentMethodsService.getCardDetailsCo(ID) } returns Outcome.Success(cardResponse)

        // ASSERT
        subject.getCardDetailsCo(ID).doOnSuccess {
            assertEquals(
                it.limits,
                PaymentLimits(
                    BigInteger.ZERO,
                    BigInteger.ZERO,
                    FiatCurrency.fromCurrencyCode(NETWORK_TICKER)
                )
            )
        }
    }

    @Test
    fun `deleteCard() should remove card and invalidate cache`() {
        // ARRANGE
        every { paymentMethodsService.deleteCard(ID) } returns Completable.complete()

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
            allowPrepaidCards = false,
            allowCreditCards = false,
            allowedAuthMethods = defaultAllowedAuthMethods,
            allowedCardNetworks = defaultAllowedCardNetworks,
            billingAddressRequired = true,
            billingAddressParameters = GooglePayResponse.BillingAddressParameters()
        )
        every { paymentMethodsService.getGooglePayInfo(NETWORK_TICKER) } returns Single.just(response)

        // ASSERT
        subject.getGooglePayTokenizationParameters(NETWORK_TICKER).test()
            .assertValue(
                GooglePayInfo(
                    beneficiaryID = response.beneficiaryID,
                    merchantBankCountryCode = response.merchantBankCountryCode,
                    googlePayParameters = response.googlePayParameters,
                    publishableApiKey = response.publishableApiKey,
                    allowPrepaidCards = response.allowPrepaidCards,
                    allowCreditCards = response.allowCreditCards,
                    allowedAuthMethods = response.allowedAuthMethods,
                    allowedCardNetworks = response.allowedCardNetworks,
                    billingAddressRequired = response.billingAddressRequired,
                    billingAddressParameters = GooglePayInfo.BillingAddressParameters(
                        format = response.billingAddressParameters?.format,
                        phoneNumberRequired = response.billingAddressParameters?.phoneNumberRequired
                    )
                )
            )
    }

    // /////////////////////////////////////////
    // // PaymentMethodService /////////////////
    // /////////////////////////////////////////

    @Test
    fun `getPaymentMethodForId() - happy PAYMENT_CARD`() = runTest {
        // ARRANGE
        val paymentMethodDetailsResponse = PaymentMethodDetailsResponse(
            paymentMethodType = PaymentMethodDetailsResponse.PAYMENT_CARD,
            cardDetails = com.blockchain.api.payments.data.CardResponse(
                card = CardDetailsResponse(
                    number = "number",
                    label = "label",
                    type = "type"
                ),
                mobilePaymentType = CardResponse.GOOGLE_PAY
            )
        )
        coEvery { paymentsService.getPaymentMethodDetailsForId(ID) } returns
            Outcome.Success(paymentMethodDetailsResponse)

        // ACT
        val result = subject.getPaymentMethodDetailsForIdLegacy(ID)

        // ASSERT
        val expected = PaymentMethodDetails(
            label = "label",
            endDigits = "number",
            mobilePaymentType = MobilePaymentType.GOOGLE_PAY
        )

        result.doOnSuccess {
            assertEquals(expected, it)
        }
    }

    @Test
    fun `getPaymentMethodForId() - happy BANK_TRANSFER`() = runTest {
        // ARRANGE
        val paymentMethodDetailsResponse = PaymentMethodDetailsResponse(
            paymentMethodType = PaymentMethodDetailsResponse.BANK_TRANSFER,
            bankTransferAccountDetails = LinkedBankTransferResponse(
                id = "", partner = "", currency = "", state = "",
                details = LinkedBankDetailsResponse(
                    accountNumber = "accountNumber", accountName = "accountName",
                    bankName = null, bankAccountType = null, sortCode = null, iban = null, bic = null
                ),
                error = null, attributes = null, ux = null
            )
        )
        coEvery { paymentsService.getPaymentMethodDetailsForId(ID) } returns
            Outcome.Success(paymentMethodDetailsResponse)

        // ACT
        val result = subject.getPaymentMethodDetailsForIdLegacy(ID)

        // ASSERT
        val expected = PaymentMethodDetails(
            label = "accountName",
            endDigits = "accountNumber"
        )

        result.doOnSuccess {
            assertEquals(expected, it)
        }
    }

    @Test
    fun `getPaymentMethodForId() - happy BANK_ACCOUNT`() = runTest {
        // ARRANGE
        val paymentMethodDetailsResponse = PaymentMethodDetailsResponse(
            paymentMethodType = PaymentMethodDetailsResponse.BANK_ACCOUNT,
            bankAccountDetails = PaymentAccountResponse(
                extraAttributes = ExtraAttributes(
                    name = "name", type = null, address = "address"
                )
            )
        )
        coEvery { paymentsService.getPaymentMethodDetailsForId(ID) } returns
            Outcome.Success(paymentMethodDetailsResponse)

        // ACT
        val result = subject.getPaymentMethodDetailsForIdLegacy(ID)

        // ASSERT
        val expected = PaymentMethodDetails(
            label = "name",
            endDigits = "address"
        )

        result.doOnSuccess {
            assertEquals(expected, it)
        }
    }

    @Test
    fun `getPaymentMethodForId() - happy Default`() = runTest {
        // ARRANGE
        val paymentMethodDetailsResponse = PaymentMethodDetailsResponse(
            paymentMethodType = "any"
        )
        coEvery { paymentsService.getPaymentMethodDetailsForId(ID) } returns
            Outcome.Success(paymentMethodDetailsResponse)

        // ACT
        val result = subject.getPaymentMethodDetailsForIdLegacy(ID)

        // ASSERT
        val expected = PaymentMethodDetails()

        result.doOnSuccess {
            assertEquals(expected, it)
        }
    }
}
