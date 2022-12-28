package piuk.blockchain.android.deeplink

import android.content.Intent
import android.net.Uri
import com.blockchain.deeplinking.processor.BlockchainLinkState
import com.blockchain.deeplinking.processor.DeeplinkService
import com.blockchain.deeplinking.processor.EmailVerifiedLinkState
import com.blockchain.deeplinking.processor.KycLinkState
import com.blockchain.deeplinking.processor.LinkState
import com.blockchain.deeplinking.processor.OpenBankingLinkType
import com.blockchain.notifications.links.PendingLink
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.android.kyc.KycDeepLinkHelper

internal class DeepLinkProcessor(
    private val linkHandler: PendingLink,
    private val emailVerifiedLinkHelper: EmailVerificationDeepLinkHelper,
    private val kycDeepLinkHelper: KycDeepLinkHelper,
    private val openBankingDeepLinkParser: OpenBankingDeepLinkParser,
    private val blockchainDeepLinkParser: BlockchainDeepLinkParser
) : DeeplinkService {
    override fun getLink(intent: Intent): Maybe<LinkState> =
        linkHandler.getPendingLinks(intent).flatMapSingle {
            urlProcessor(it)
        }

    override fun getLink(link: String): Single<LinkState> =
        urlProcessor(Uri.parse(link))

    private fun urlProcessor(uri: Uri): Single<LinkState> =
        Maybe.fromCallable {
            val emailVerifiedUri = emailVerifiedLinkHelper.mapUri(uri)
            if (emailVerifiedUri != EmailVerifiedLinkState.NoUri) {
                return@fromCallable LinkState.EmailVerifiedDeepLink(emailVerifiedUri)
            }
            val kyc = kycDeepLinkHelper.mapUri(uri)
            if (kyc != KycLinkState.NoUri) {
                return@fromCallable LinkState.KycDeepLink(kyc)
            }

            val openBankingDeepLink = openBankingDeepLinkParser.mapUri(uri)
            if (openBankingDeepLink != null && openBankingDeepLink.type != OpenBankingLinkType.UNKNOWN) {
                return@fromCallable openBankingDeepLink
            }
            val blockchainLink = blockchainDeepLinkParser.mapUri(uri)
            if (blockchainLink != BlockchainLinkState.NoUri) {
                return@fromCallable LinkState.BlockchainLink(blockchainLink)
            }
            LinkState.NoUri
        }.switchIfEmpty(Maybe.just(LinkState.NoUri))
            .toSingle()
            .onErrorResumeNext { Single.just(LinkState.NoUri) }
}
