package piuk.blockchain.android.deeplink

import android.net.Uri
import com.blockchain.deeplinking.processor.LinkState
import com.blockchain.deeplinking.processor.OpenBankingLinkType
import piuk.blockchain.android.kyc.ignoreFragment

class OpenBankingDeepLinkParser {
    fun mapUri(uri: Uri): LinkState.OpenBankingLink? {
        val fragment = uri.encodedFragment?.let { Uri.parse(it) } ?: return null
        var consentToken: String? = uri.ignoreFragment().getQueryParameter(OTT).orEmpty()
        if (consentToken == "null") {
            consentToken = null
        }

        return LinkState.OpenBankingLink(
            when (fragment.path) {
                BANK_LINK -> {
                    OpenBankingLinkType.LINK_BANK
                }
                BANK_APPROVAL -> {
                    OpenBankingLinkType.PAYMENT_APPROVAL
                }
                else -> {
                    OpenBankingLinkType.UNKNOWN
                }
            },
            consentToken
        )
    }

    companion object {
        private const val OTT = "one-time-token"
        private const val BANK_LINK = "/open/ob-bank-link"
        private const val BANK_APPROVAL = "/open/ob-bank-approval"
    }
}
