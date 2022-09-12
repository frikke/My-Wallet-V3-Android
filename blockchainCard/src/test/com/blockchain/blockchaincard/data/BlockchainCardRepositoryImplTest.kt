package com.blockchain.blockchaincard.data

import com.blockchain.api.blockchainCard.data.CardAccountDto
import com.blockchain.api.blockchainCard.data.CardAccountLinkDto
import com.blockchain.api.blockchainCard.data.CardDto
import com.blockchain.api.blockchainCard.data.CardWidgetTokenDto
import com.blockchain.api.blockchainCard.data.PriceDto
import com.blockchain.api.blockchainCard.data.ProductDto
import com.blockchain.api.blockchainCard.data.ResidentialAddressDto
import com.blockchain.api.blockchainCard.data.ResidentialAddressRequestDto
import com.blockchain.api.eligibility.data.StateResponse
import com.blockchain.api.services.BlockchainCardService
import com.blockchain.api.services.EligibilityApiService
import com.blockchain.blockchaincard.domain.models.BlockchainCard
import com.blockchain.blockchaincard.domain.models.BlockchainCardAddress
import com.blockchain.blockchaincard.domain.models.BlockchainCardBrand
import com.blockchain.blockchaincard.domain.models.BlockchainCardProduct
import com.blockchain.blockchaincard.domain.models.BlockchainCardStatus
import com.blockchain.blockchaincard.domain.models.BlockchainCardType
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.fiat.FiatCustodialAccount
import com.blockchain.coincore.impl.CustodialTradingAccount
import com.blockchain.domain.eligibility.model.Region
import com.blockchain.nabu.UserIdentity
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.getOrNull
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.FiatValue
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.reactivex.rxjava3.core.Single
import java.math.BigDecimal
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BlockchainCardRepositoryImplTest {

    private val blockchainCardService = mockk<BlockchainCardService>()

    private val eligibilityApiService = mockk<EligibilityApiService>()

    private val coincore = mockk<Coincore>()

    private val assetCatalogue = mockk<AssetCatalogue>()

    private val userIdentity = mockk<UserIdentity>()

    private val blockchainCardRepository = BlockchainCardRepositoryImpl(
        blockchainCardService = blockchainCardService,
        eligibilityApiService = eligibilityApiService,
        coincore = coincore,
        assetCatalogue = assetCatalogue,
        userIdentity = userIdentity
    )

    private val widgetToken = "widgetToken"
    private val cardWidgetUrl = "cardWidgetUrl"

    private val fiatCustodialAccount = mockk<FiatCustodialAccount>()
    private val custodialTradingAccount = mockk<CustodialTradingAccount>()

    private val price = PriceDto(
        "USD",
        "1.00"
    )
    private val balanceUsd = PriceDto(
        "USD",
        "5.00"
    )
    private val balanceBtc = PriceDto(
        "BTC",
        "1.00"
    )

    private val productResponseDto = ProductDto(
        productCode = "productCode",
        price = price,
        brand = "VISA",
        type = "VIRTUAL",
    )

    private val productResponseDomainModel = BlockchainCardProduct(
        productCode = "productCode",
        price = FiatValue.fromMajor(
            fiatCurrency = FiatCurrency.fromCurrencyCode(price.symbol),
            major = BigDecimal(price.value)
        ),
        brand = BlockchainCardBrand.VISA,
        type = BlockchainCardType.VIRTUAL,
    )

    private val cardResponseDto = CardDto(
        id = "cardId",
        type = "VIRTUAL",
        last4 = "1234",
        expiry = "01/01",
        brand = "VISA",
        status = "ACTIVE",
        createdAt = "2020-01-01",
    )

    private val cardLockedResponseDto = CardDto(
        id = "cardId",
        type = "VIRTUAL",
        last4 = "1234",
        expiry = "01/01",
        brand = "VISA",
        status = "LOCKED",
        createdAt = "2020-01-01",
    )

    private val cardResponseDomainModel = BlockchainCard(
        id = "cardId",
        type = BlockchainCardType.VIRTUAL,
        last4 = "1234",
        expiry = "01/01",
        brand = BlockchainCardBrand.VISA,
        status = BlockchainCardStatus.ACTIVE,
        createdAt = "2020-01-01"
    )

    private val cardLockedResponseDomainModel = BlockchainCard(
        id = "cardId",
        type = BlockchainCardType.VIRTUAL,
        last4 = "1234",
        expiry = "01/01",
        brand = BlockchainCardBrand.VISA,
        status = BlockchainCardStatus.LOCKED,
        createdAt = "2020-01-01"
    )

    private val cardWidgetTokenDto = CardWidgetTokenDto(
        token = widgetToken
    )

    private val cardAccountListDto = listOf(
        CardAccountDto(balance = balanceBtc),
        CardAccountDto(balance = balanceUsd)
    )

    private val cardAccountLinkDto = CardAccountLinkDto(
        accountCurrency = "BTC"
    )

    private val residentialAddressRequestDto = ResidentialAddressRequestDto(
        userId = "userId",
        address = ResidentialAddressDto(
            line1 = "line1",
            line2 = "line2",
            postCode = "postCode",
            city = "city",
            state = "state",
            country = "country"
        )
    )

    private val blockchainCardAddressDomainModel = BlockchainCardAddress(
        line1 = "line1",
        line2 = "line2",
        postCode = "postCode",
        city = "city",
        state = "state",
        country = "country"
    )

    private val usStateListDto = listOf(
        StateResponse(
            "US-FL",
            "Florida",
            emptyList(),
            "US"
        ),
        StateResponse(
            "US-GA",
            "Georgia",
            emptyList(),
            "US"
        ),
        StateResponse(
            "US-NC",
            "North Carolina",
            emptyList(),
            "US"
        )
    )

    @Before
    fun setUp() {
        coEvery { coincore.allWallets() } returns Single.just(
            mockk {
                every { accounts } returns listOf(fiatCustodialAccount, custodialTradingAccount)
            }
        )
        every { fiatCustodialAccount.currency.networkTicker } returns "USD"
        every { custodialTradingAccount.currency.networkTicker } returns "BTC"
        coEvery { eligibilityApiService.getStatesList(any(), any()) } returns Outcome.Success(usStateListDto)
    }

    @Test
    fun `WHEN getProducts api call succeeds, THEN response is correctly constructed into a domain object`() = runTest {

        coEvery { blockchainCardService.getProducts() } returns Outcome.Success(listOf(productResponseDto))

        val productListUnderTest = blockchainCardRepository.getProducts()

        assertEquals(productListUnderTest, Outcome.Success(listOf(productResponseDomainModel)))
    }

    @Test
    fun `WHEN getCards api call succeeds, THEN response is correctly constructed into a domain object`() = runTest {

        coEvery { blockchainCardService.getCards() } returns Outcome.Success(listOf(cardResponseDto))

        val cardListUnderTest = blockchainCardRepository.getCards()

        assertEquals(cardListUnderTest, Outcome.Success(listOf(cardResponseDomainModel)))
    }

    @Test
    fun `WHEN createCard gets called, THEN request body is correctly constructed AND response is correctly constructed into a domain object`() =
        runTest {
            coEvery { blockchainCardService.createCard(any(), any()) } returns Outcome.Success(cardResponseDto)

            val cardUnderTest = blockchainCardRepository.createCard(
                productCode = "productCode",
                ssn = "123456789"
            )

            assertEquals(cardUnderTest, Outcome.Success(cardResponseDomainModel))
        }

    @Test
    fun `WHEN deleteCard api call succeeds, THEN response is correctly constructed into a domain object`() = runTest {
        coEvery { blockchainCardService.deleteCard(any()) } returns Outcome.Success(cardResponseDto)

        val cardUnderTest = blockchainCardRepository.deleteCard(
            cardId = "cardId"
        )

        assertEquals(cardUnderTest, Outcome.Success(cardResponseDomainModel))
    }

    @Test
    fun `WHEN lockCard api call succeeds, THEN response is correctly constructed into a domain object`() = runTest {
        coEvery { blockchainCardService.lockCard(any()) } returns Outcome.Success(cardLockedResponseDto)

        val cardUnderTest = blockchainCardRepository.lockCard(
            cardId = "cardId"
        )

        assertEquals(cardUnderTest, Outcome.Success(cardLockedResponseDomainModel))
    }

    @Test
    fun `WHEN unlockCard api call succeeds, THEN response is correctly constructed into a domain object`() = runTest {
        coEvery { blockchainCardService.unlockCard(any()) } returns Outcome.Success(cardResponseDto)

        val cardUnderTest = blockchainCardRepository.unlockCard(
            cardId = "cardId"
        )

        assertEquals(cardUnderTest, Outcome.Success(cardResponseDomainModel))
    }

    @Test
    fun `WHEN getCardWidgetUrl api call succeeds, THEN response is correctly constructed into a domain object`() =
        runTest {
            coEvery { blockchainCardService.getCardWidgetToken(any()) } returns Outcome.Success(
                cardWidgetTokenDto
            )
            coEvery { blockchainCardService.getCardWidgetUrl(any(), any(), any()) } returns Outcome.Success(cardWidgetUrl)

            val cardWidgetUrlUnderTest = blockchainCardRepository.getCardWidgetUrl(
                cardId = "cardId",
                last4Digits = "1234",
                userFullName = "userFullName"
            )

            assertEquals(cardWidgetUrlUnderTest, Outcome.Success(cardWidgetUrl))
        }

    @Test
    fun `WHEN getEligibleTradingAccounts api call succeeds, THEN response include a valid list of TradingAccount objects`() =
        runTest {

            coEvery { blockchainCardService.getEligibleAccounts(any()) } returns Outcome.Success(
                cardAccountListDto
            )

            val eligibleTradingAccountsUnderTest = blockchainCardRepository.getEligibleTradingAccounts(
                cardId = "cardId"
            ).getOrNull()

            assert(
                eligibleTradingAccountsUnderTest?.any {
                    it is FiatCustodialAccount && it.currency.networkTicker == "USD"
                } == true
            )

            assert(
                eligibleTradingAccountsUnderTest?.any {
                    it is CustodialTradingAccount && it.currency.networkTicker == "BTC"
                } == true
            )
        }

    @Test
    fun `WHEN linkCardAccount api calls succeeds, THEN response must include the currency sent in the request`() =
        runTest {
            coEvery { blockchainCardService.linkCardAccount(any(), any()) } returns Outcome.Success(
                cardAccountLinkDto
            )

            val cardLinkCurrencyUnderTest = blockchainCardRepository.linkCardAccount(
                cardId = "cardId",
                accountCurrency = "BTC"
            )

            assertEquals(Outcome.Success(cardAccountLinkDto.accountCurrency), cardLinkCurrencyUnderTest)
        }

    @Test
    fun `WHEN getCardLinkedAccount api call succeeds, THEN response must include a valid TradingAccount`() = runTest {
        coEvery { blockchainCardService.getCardLinkedAccount(any()) } returns Outcome.Success(cardAccountLinkDto)

        val cardLinkCurrencyUnderTest = blockchainCardRepository.getCardLinkedAccount(
            cardId = "cardId"
        )

        assert(
            cardLinkCurrencyUnderTest.getOrNull()?.let {
                it is CustodialTradingAccount && it.currency.networkTicker == "BTC"
            } == true
        )
    }

    @Test
    fun `WHEN getResidentialAddress api call succeeds, THEN response must include a valid BlockchainCardAddress domain object`() =
        runTest {
            coEvery { blockchainCardService.getResidentialAddress() } returns Outcome.Success(
                residentialAddressRequestDto
            )

            val cardResidentialAddressUnderTest = blockchainCardRepository.getResidentialAddress()

            assertEquals(Outcome.Success(blockchainCardAddressDomainModel), cardResidentialAddressUnderTest)
        }

    @Test
    fun `WHEN updateResidentialAddress `() = runTest {
        coEvery { blockchainCardService.updateResidentialAddress(any()) } returns Outcome.Success(
            residentialAddressRequestDto
        )

        val cardResidentialAddressUnderTest = blockchainCardRepository.updateResidentialAddress(
            blockchainCardAddressDomainModel
        )

        assertEquals(residentialAddressRequestDto.address.line1, cardResidentialAddressUnderTest.getOrNull()?.line1)
        assertEquals(residentialAddressRequestDto.address.line2, cardResidentialAddressUnderTest.getOrNull()?.line2)
        assertEquals(
            residentialAddressRequestDto.address.postCode, cardResidentialAddressUnderTest.getOrNull()?.postCode
        )
        assertEquals(residentialAddressRequestDto.address.city, cardResidentialAddressUnderTest.getOrNull()?.city)
        assertEquals(residentialAddressRequestDto.address.state, cardResidentialAddressUnderTest.getOrNull()?.state)
        assertEquals(residentialAddressRequestDto.address.country, cardResidentialAddressUnderTest.getOrNull()?.country)
    }

    @Test
    fun `WHEN list of US states is requested, THEN response must include a valid list of US states`() = runTest {
        val usStates = listOf(
            Region.State("US", "Florida", false, "US-FL"),
            Region.State("US", "Georgia", false, "US-GA"),
            Region.State("US", "North Carolina", false, "US-NC")
        )
        val usStatesUnderTest = blockchainCardRepository.getStatesList("US").getOrNull()
        assertEquals(usStates, usStatesUnderTest)
    }
}
