package piuk.blockchain.android.ui.transfer.analytics

import com.blockchain.analytics.AnalyticsEvent
import com.blockchain.analytics.events.AnalyticsNames
import com.blockchain.analytics.events.LaunchOrigin
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.SingleAccount
import java.io.Serializable
import piuk.blockchain.android.ui.transactionflow.analytics.toCategory

sealed class TransferAnalyticsEvent(
    override val event: String,
    override val params: Map<String, String> = emptyMap()
) : AnalyticsEvent {

    object NoBalanceViewDisplayed : TransferAnalyticsEvent("send_no_balance_seen")
    object NoBalanceCtaClicked : TransferAnalyticsEvent("send_no_balance_buy_clicked")

    data class SourceWalletSelected(
        val wallet: CryptoAccount
    ) : TransferAnalyticsEvent(
        "send_wallet_select",
        mapOf(
            PARAM_ASSET to wallet.currency.networkTicker,
            PARAM_WALLET to (wallet as SingleAccount).toCategory()
        )
    )

    class TransferClicked(
        override val origin: LaunchOrigin,
        private val type: AnalyticsTransferType
    ) : AnalyticsEvent {
        override val event: String
            get() = AnalyticsNames.SEND_RECEIVE_CLICKED.eventName
        override val params: Map<String, Serializable>
            get() = mapOf(
                "type" to type.name
            )
    }

    object TransferViewed : AnalyticsEvent {
        override val event: String
            get() = AnalyticsNames.SEND_RECEIVE_VIEWED.eventName
        override val params: Map<String, Serializable>
            get() = mapOf()
    }

    companion object {
        private const val PARAM_ASSET = "asset"
        private const val PARAM_WALLET = "wallet"
    }

    enum class AnalyticsTransferType {
        SEND, RECEIVE
    }
}
