package piuk.blockchain.android.ui.kyc.reentry

import com.blockchain.nabu.datamanagers.kyc.KycDataManager
import com.blockchain.nabu.models.responses.nabu.Address
import com.blockchain.nabu.models.responses.nabu.KycQuestionnaireNode
import com.blockchain.nabu.models.responses.nabu.KycState
import com.blockchain.nabu.models.responses.nabu.NabuUser
import com.blockchain.nabu.models.responses.nabu.TierLevels
import com.blockchain.nabu.models.responses.nabu.UserState
import com.blockchain.outcome.Outcome
import io.mockk.coEvery
import io.mockk.mockk
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should be equal to`
import org.junit.Test
import piuk.blockchain.android.ui.kyc.questionnaire.TreeNode

class ReentryDecisionTest {

    private val kycDataManager: KycDataManager = mockk {
        coEvery { getQuestionnaire() } returns Outcome.Success(emptyList())
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
                    state = "",
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
                    state = "",
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
                    state = "",
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
                    state = "",
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
                    state = "",
                    postCode = "",
                    countryCode = ""
                )
            )
        ) `should be` ReentryPoint.CountrySelection
    }

    @Test
    fun `if user is tier 1, has questionnaire then go to questionnaire entry`() {
        val nodes = listOf(
            KycQuestionnaireNode.Selection("s1", "text1", emptyList(), false),
            KycQuestionnaireNode.Selection("s2", "text2", emptyList(), false),
        )
        coEvery { kycDataManager.getQuestionnaire() } returns Outcome.Success(nodes)
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
                    state = "",
                    postCode = "",
                    countryCode = "DE"
                ),
                dob = "dob",
                firstName = "A",
                lastName = "B"
            )
        ) `should be equal to` ReentryPoint.Questionnaire(root)
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
                    state = "",
                    postCode = "",
                    countryCode = "DE"
                ),
                dob = "dob",
                firstName = "A",
                lastName = "B"
            )
        ) `should be` ReentryPoint.MobileEntry
    }

    private fun whereNext(user: NabuUser) =
        TiersReentryDecision(kycDataManager).findReentryPoint(user).blockingGet()

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
            insertedAt = null
        )
}
