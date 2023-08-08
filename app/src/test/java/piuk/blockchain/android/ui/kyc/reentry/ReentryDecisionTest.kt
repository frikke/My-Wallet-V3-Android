package piuk.blockchain.android.ui.kyc.reentry

import com.blockchain.data.DataResource
import com.blockchain.domain.dataremediation.DataRemediationService
import com.blockchain.domain.dataremediation.model.Questionnaire
import com.blockchain.domain.dataremediation.model.QuestionnaireContext
import com.blockchain.domain.dataremediation.model.QuestionnaireNode
import com.blockchain.nabu.Feature
import com.blockchain.nabu.api.getuser.domain.UserFeaturePermissionService
import com.blockchain.nabu.models.responses.nabu.Address
import com.blockchain.nabu.models.responses.nabu.CurrenciesResponse
import com.blockchain.nabu.models.responses.nabu.KycState
import com.blockchain.nabu.models.responses.nabu.NabuUser
import com.blockchain.nabu.models.responses.nabu.TierLevels
import com.blockchain.nabu.models.responses.nabu.UserState
import com.blockchain.outcome.Outcome
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should be equal to`
import org.junit.Test
import piuk.blockchain.android.ui.dataremediation.TreeNode

class ReentryDecisionTest {

    private val dataRemediationService: DataRemediationService = mockk {
        coEvery { getQuestionnaire(QuestionnaireContext.TIER_TWO_VERIFICATION) } returns Outcome.Success(null)
    }
    private val userFeaturePermissionService: UserFeaturePermissionService = mockk {
        every { isEligibleFor(Feature.Kyc) } returns flowOf(DataResource.Data(true))
    }

    @Test
    fun `if email is unverified - go to email entry`() {
        whereNext(
            createdNabuUser(selected = 1).copy(
                email = "abc@def.com",
                emailVerified = false
            )
        ) `should be` ReentryPoint.EmailEntry
    }

    @Test
    fun `if country code is unset - go to country code entry`() {
        whereNext(
            createdNabuUser(selected = 1).copy(
                email = "abc@def.com",
                emailVerified = true
            )
        ) `should be` ReentryPoint.CountrySelection
    }

    @Test
    fun `if profile is not set - go to profile`() {
        whereNext(
            createdNabuUser(selected = 1).copy(
                email = "abc@def.com",
                emailVerified = true,
                address = Address(
                    line1 = "",
                    line2 = "",
                    city = "",
                    stateIso = "",
                    postCode = "",
                    countryCode = "DE"
                )
            )
        ) `should be` ReentryPoint.Profile
    }

    @Test
    fun `if profile is set - go to address`() {
        whereNext(
            createdNabuUser(selected = 1).copy(
                email = "abc@def.com",
                emailVerified = true,
                address = Address(
                    line1 = "",
                    line2 = "",
                    city = "",
                    stateIso = "",
                    postCode = "",
                    countryCode = "DE"
                ),
                dob = "dob",
                firstName = "A",
                lastName = "B"
            )
        ) `should be` ReentryPoint.Address
    }

    @Test
    fun `if user is tier 2, and mobile is not verified - go to mobile`() {
        whereNext(
            createdNabuUser(selected = 2, tier = 1).copy(
                mobile = "123456",
                mobileVerified = false
            )
        ) `should be` ReentryPoint.MobileEntry
    }

    @Test
    fun `if user is tier 2, and mobile is verified - go to onfido`() {
        whereNext(
            createdNabuUser(selected = 2, tier = 1).copy(
                mobile = "123456",
                mobileVerified = true
            )
        ) `should be` ReentryPoint.Veriff
    }

    @Test
    fun `if user is tier 0, tier 1 all complete but upgraded go to mobile`() {
        whereNext(
            createdNabuUser(tier = 0, next = 2).copy(
                email = "abc@def.com",
                emailVerified = true,
                address = Address(
                    line1 = "",
                    line2 = "",
                    city = "",
                    stateIso = "",
                    postCode = "",
                    countryCode = "DE"
                ),
                dob = "dob",
                firstName = "A",
                lastName = "B"
            )
        ) `should be` ReentryPoint.MobileEntry
    }

    @Test
    fun `if user is tier 0, upgraded, but no email still go to email`() {
        whereNext(
            createdNabuUser(tier = 0, next = 2)
                .copy(emailVerified = false)
        ) `should be` ReentryPoint.EmailEntry
    }

    @Test
    fun `if user is tier 0, upgraded, but no country still go to country`() {
        whereNext(
            createdNabuUser(tier = 0, next = 2)
                .copy(emailVerified = true)
        ) `should be` ReentryPoint.CountrySelection
    }

    @Test
    fun `if user is tier 0, upgraded but no profile still and country is selected go to Profile`() {
        whereNext(
            createdNabuUser(tier = 0, next = 2).copy(
                email = "abc@def.com",
                emailVerified = true,
                address = Address(
                    line1 = "",
                    line2 = "",
                    city = "",
                    stateIso = "",
                    postCode = "",
                    countryCode = "DE"
                )
            )
        ) `should be` ReentryPoint.Profile
    }

    @Test
    fun `if user is tier 0, upgraded but no profile still and no country is selected go to CountrySelection`() {
        whereNext(
            createdNabuUser(tier = 0, next = 2).copy(
                email = "abc@def.com",
                emailVerified = true,
                address = Address(
                    line1 = "",
                    line2 = "",
                    city = "",
                    stateIso = "",
                    postCode = "",
                    countryCode = ""
                )
            )
        ) `should be` ReentryPoint.CountrySelection
    }

    @Test
    fun `if user is tier 1, has questionnaire then go to questionnaire entry`() {
        val questionnaire = Questionnaire(
            header = null,
            context = QuestionnaireContext.TIER_TWO_VERIFICATION,
            nodes = listOf(
                QuestionnaireNode.Selection("s1", "text1", emptyList(), false),
                QuestionnaireNode.Selection("s2", "text2", emptyList(), false)
            ),
            isMandatory = true
        )
        coEvery {
            dataRemediationService.getQuestionnaire(QuestionnaireContext.TIER_TWO_VERIFICATION)
        } returns Outcome.Success(questionnaire)
        val root = TreeNode.Root(
            listOf(
                TreeNode.Selection("s1", "text1", emptyList(), false),
                TreeNode.Selection("s2", "text2", emptyList(), false)
            )
        )
        whereNext(
            createdNabuUser(tier = 1, next = 2).copy(
                email = "abc@def.com",
                emailVerified = true,
                mobileVerified = true,
                address = Address(
                    line1 = "",
                    line2 = "",
                    city = "",
                    stateIso = "",
                    postCode = "",
                    countryCode = "DE"
                ),
                dob = "dob",
                firstName = "A",
                lastName = "B"
            )
        ) `should be equal to` ReentryPoint.Questionnaire(questionnaire)
    }

    @Test
    fun `if user is tier 0, upgraded then go to mobile`() {
        whereNext(
            createdNabuUser(tier = 0, next = 2).copy(
                email = "abc@def.com",
                emailVerified = true,
                address = Address(
                    line1 = "",
                    line2 = "",
                    city = "",
                    stateIso = "",
                    postCode = "",
                    countryCode = "DE"
                ),
                dob = "dob",
                firstName = "A",
                lastName = "B"
            )
        ) `should be` ReentryPoint.MobileEntry
    }

    @Test
    fun `if user is not eligible for KYC it should show TierCurrentState with KYC Rejected`() {
        every { userFeaturePermissionService.isEligibleFor(Feature.Kyc) }.returns(flowOf(DataResource.Data(false)))
        whereNext(
            createdNabuUser(tier = 0, next = 2).copy(
                email = "abc@def.com",
                emailVerified = true,
                address = Address(
                    line1 = "",
                    line2 = "",
                    city = "",
                    stateIso = "",
                    postCode = "",
                    countryCode = "DE"
                ),
                dob = "dob",
                firstName = "A",
                lastName = "B"
            )
        ) `should be equal to` ReentryPoint.TierCurrentState(KycState.Rejected)
    }

    private fun whereNext(user: NabuUser) =
        TiersReentryDecision(
            dataRemediationService,
            userFeaturePermissionService
        ).findReentryPoint(user).blockingGet()

    private fun createdNabuUser(
        selected: Int = 1,
        tier: Int = selected - 1,
        next: Int = selected
    ) =
        emptyNabuUser().copy(
            kycState = KycState.None,
            tiers = TierLevels(
                current = tier,
                next = next,
                selected = selected
            )
        )

    private fun emptyNabuUser() =
        NabuUser(
            id = "id",
            firstName = null,
            lastName = null,
            email = "",
            emailVerified = false,
            dob = null,
            mobile = null,
            mobileVerified = false,
            address = null,
            state = UserState.None,
            kycState = KycState.None,
            insertedAt = null,
            currencies = CurrenciesResponse(
                preferredFiatTradingCurrency = "EUR",
                usableFiatCurrencies = listOf("EUR", "USD", "GBP", "ARS"),
                defaultWalletCurrency = "BRL",
                userFiatCurrencies = listOf("EUR", "GBP")
            )
        )
}
