package com.blockchain.deeplinking.processor

import android.content.Intent
import com.blockchain.nabu.models.responses.nabu.CampaignData
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single

interface DeeplinkService {
    fun getLink(intent: Intent): Maybe<LinkState>
    fun getLink(link: String): Single<LinkState>
}

sealed class LinkState {
    data class BlockchainLink(val link: BlockchainLinkState) : LinkState()
    data class EmailVerifiedDeepLink(val link: EmailVerifiedLinkState) : LinkState()
    data class KycDeepLink(val link: KycLinkState) : LinkState()
    data class OpenBankingLink(val type: OpenBankingLinkType, val consentToken: String?) : LinkState()

    object NoUri : LinkState()
}

sealed class BlockchainLinkState {
    object NoUri : BlockchainLinkState()
    object Swap : BlockchainLinkState()
    object Activities : BlockchainLinkState()
    object Interest : BlockchainLinkState()
    object TwoFa : BlockchainLinkState()
    object VerifyEmail : BlockchainLinkState()
    object SetupFingerprint : BlockchainLinkState()
    object Receive : BlockchainLinkState()
    object Send : BlockchainLinkState()
    data class Buy(val ticker: String? = null) : BlockchainLinkState()
    data class Sell(val ticker: String? = null) : BlockchainLinkState()
    data class KycCampaign(val campaignType: String) : BlockchainLinkState()
    data class SimpleBuy(val ticker: String) : BlockchainLinkState()
}

enum class EmailVerifiedLinkState {

    /**
     * A deep link into the app when the user has just verified their email due
     * to trying to link their Wallet to the Pit.
     */
    FromPitLinking,

    /**
     * Not a valid email verified deep link URI
     */
    NoUri
}

sealed class KycLinkState {
    /**
     * Deep link into the email confirmation part of KYC
     */
    object EmailVerified : KycLinkState()

    /**
     * General deep link into KYC
     */
    data class General(val campaignData: CampaignData?) : KycLinkState()

    /**
     * Not a valid KYC deep link URI
     */
    object NoUri : KycLinkState()

    /**
     * Deep link into identity verification part of KYC
     */
    object Resubmit : KycLinkState()
}

enum class OpenBankingLinkType {
    LINK_BANK,
    PAYMENT_APPROVAL,
    UNKNOWN
}
