package piuk.blockchain.android.deeplink

import android.content.Intent
import android.net.Uri
import com.blockchain.deeplinking.processor.EmailVerifiedLinkState
import com.blockchain.deeplinking.processor.KycLinkState
import com.blockchain.deeplinking.processor.LinkState
import com.blockchain.nabu.models.responses.nabu.CampaignData
import com.blockchain.notifications.links.PendingLink
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import io.reactivex.rxjava3.core.Maybe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import piuk.blockchain.android.BlockchainTestApplication
import piuk.blockchain.android.FakeCoreClient
import piuk.blockchain.android.FakeWeb3Wallet
import piuk.blockchain.android.kyc.KycDeepLinkHelper

@Config(sdk = [26], application = BlockchainTestApplication::class, shadows = [FakeCoreClient::class, FakeWeb3Wallet::class])
@RunWith(RobolectricTestRunner::class)
class DeepLinkProcessorTest {

    @Test
    fun `unknown uri`() {
        givenUriExpect("https://login.blockchain.com/", LinkState.NoUri)
    }

    @Test
    fun `kyc resubmit uri`() {
        givenUriExpect(
            "https://login.blockchain.com/login?deep_link_path=verification",
            LinkState.KycDeepLink(
                KycLinkState.Resubmit
            )
        )
    }

    @Test
    fun `kyc email verified uri`() {
        givenUriExpect(
            "https://login.blockchain.com/login?deep_link_path=email_verified&context=kyc",
            LinkState.KycDeepLink(
                KycLinkState.EmailVerified
            )
        )
    }

    @Test
    fun `pit email verified uri`() {
        givenUriExpect(
            "https://login.blockchain.com/login?deep_link_path=email_verified&context=pit_signup",
            LinkState.EmailVerifiedDeepLink(EmailVerifiedLinkState.FromPitLinking)
        )
    }

    @Test
    fun `general kyc uri with campaign`() {
        val url = "https://login.blockchain.com/#/open/kyc?tier=2&deep_link_path=kyc&campaign=sunriver"
        givenUriExpect(
            url,
            LinkState.KycDeepLink(
                KycLinkState.General(CampaignData("sunriver", false))
            )
        )
    }

    @Test
    fun `general kyc uri without campaign`() {
        givenUriExpect(
            "https://login.blockchain.com/#/open/kyc?tier=2&deep_link_path=kyc",
            LinkState.KycDeepLink(
                KycLinkState.General(null)
            )
        )
    }

    companion object {
        private const val LINK_ID = "11111111-2222-3333-4444-555555556666"
    }
}

private fun givenUriExpect(uri: String, expected: LinkState) {
    val i: Intent = mock()

    DeepLinkProcessor(
        linkHandler = givenPendingUri(uri),
        emailVerifiedLinkHelper = EmailVerificationDeepLinkHelper(),
        kycDeepLinkHelper = KycDeepLinkHelper(mock()),
        openBankingDeepLinkParser = OpenBankingDeepLinkParser(),
        blockchainDeepLinkParser = BlockchainDeepLinkParser()
    ).getLink(i)
        .test()
        .assertNoErrors()
        .assertValue(expected)
}

private fun givenPendingUri(uri: String): PendingLink =
    mock {
        on { getPendingLinks(any()) }.thenReturn(Maybe.just(Uri.parse(uri)))
    }
