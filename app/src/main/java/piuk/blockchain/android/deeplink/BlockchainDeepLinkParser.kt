package piuk.blockchain.android.deeplink

import android.net.Uri
import com.blockchain.deeplinking.processor.BlockchainLinkState
import piuk.blockchain.android.kyc.ignoreFragment

class BlockchainDeepLinkParser {
    private fun getQueryParameters(uri: Uri, stateClass: Class<*>): Map<String, String> {
        val hashMap = hashMapOf<String, String>()
        for (prop in stateClass.declaredFields) {
            uri.ignoreFragment().getQueryParameter(prop.name)?.let {
                hashMap[prop.name] = it
            }
        }
        return hashMap.toMap()
    }

    fun mapUri(uri: Uri): BlockchainLinkState {
        return when (uri.ignoreFragment().path) {
            SWAP_URL -> {
                BlockchainLinkState.Swap
            }
            ACTIVITIES_URL -> {
                BlockchainLinkState.Activities
            }
            BUY_URL -> {
                val queries = getQueryParameters(uri, BlockchainLinkState.Buy::class.java)
                BlockchainLinkState.Buy(ticker = queries["ticker"])
            }
            SELL_URL -> {
                val queries = getQueryParameters(uri, BlockchainLinkState.Sell::class.java)
                BlockchainLinkState.Sell(ticker = queries["ticker"])
            }
            SIMPLE_BUY_URL -> {
                val queries = getQueryParameters(uri, BlockchainLinkState.SimpleBuy::class.java)
                val ticker = queries["ticker"] ?: return BlockchainLinkState.NoUri
                BlockchainLinkState.SimpleBuy(ticker = ticker)
            }
            KYC_URL -> {
                val queries = getQueryParameters(uri, BlockchainLinkState.KycCampaign::class.java)
                BlockchainLinkState.KycCampaign(campaignType = queries["campaignType"].orEmpty())
            }
            TWO_FA_URL -> {
                BlockchainLinkState.TwoFa
            }
            VERIFY_EMAIL_URL -> {
                BlockchainLinkState.VerifyEmail
            }
            SETUP_FINGERPRINT_URL -> {
                BlockchainLinkState.SetupFingerprint
            }
            INTEREST_URL -> {
                BlockchainLinkState.Interest
            }
            RECEIVE_URL -> {
                BlockchainLinkState.Receive
            }
            SEND_URL -> {
                BlockchainLinkState.Send
            }
            else -> BlockchainLinkState.NoUri
        }
    }

    companion object {
        const val SWAP_URL = "/open/swap"
        const val ACTIVITIES_URL = "/open/activities"
        const val BUY_URL = "/open/buy"
        const val SELL_URL = "/open/sell"
        const val SIMPLE_BUY_URL = "/open/simplebuy"
        const val KYC_URL = "/open/kyc"
        const val TWO_FA_URL = "/open/twofa"
        const val VERIFY_EMAIL_URL = "/open/verifyemail"
        const val SETUP_FINGERPRINT_URL = "/open/setupfingerprint"
        const val INTEREST_URL = "/open/interest"
        const val RECEIVE_URL = "/open/receive"
        const val SEND_URL = "/open/send"
    }
}
