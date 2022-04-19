package com.blockchain.walletconnect.domain

import com.blockchain.analytics.AnalyticsEvent
import com.blockchain.analytics.events.AnalyticsNames
import com.blockchain.analytics.events.LaunchOrigin
import com.blockchain.coincore.eth.EthereumSendTransactionTarget
import com.trustwallet.walletconnect.models.ethereum.WCEthereumSignMessage
import java.io.Serializable

sealed class WalletConnectAnalytics(
    override val event: String,
    override val params: Map<String, Serializable> = emptyMap()
) : AnalyticsEvent {

    class ConnectedDappClicked(
        dappName: String,
        override val origin: LaunchOrigin = LaunchOrigin.APPS_LIST
    ) : WalletConnectAnalytics(
        event = AnalyticsNames.CONNECTED_DAPP_CLICKED.eventName,
        params = mapOf(
            APP_NAME to dappName
        )
    )

    class ConnectedDappActioned(
        dappName: String,
        action: DappConnectionAction
    ) : WalletConnectAnalytics(
        event = AnalyticsNames.CONNECTED_DAPP_ACTIONED.eventName,
        params = mapOf(
            APP_NAME to dappName,
            ACTION to action.name
        )
    )

    class ConnectedDappsListClicked(
        override val origin: LaunchOrigin
    ) : WalletConnectAnalytics(
        event = AnalyticsNames.CONNECTED_DAPPS_LIST_CLICKED.eventName,
    )

    class DappConectionActioned(
        action: DappConnectionAction,
        appName: String
    ) : WalletConnectAnalytics(
        event = AnalyticsNames.DAPP_CONNECTION_ACTIONED.eventName,
        params = mapOf(
            ACTION to action.name,
            APP_NAME to appName
        )
    )

    object ConnectedDappsListViewed : WalletConnectAnalytics(
        event = AnalyticsNames.CONNECTED_DAPPS_LIST_VIEWED.eventName
    )

    class DappRequestActioned(
        action: DappConnectionAction,
        appName: String,
        method: DappRequestMethod
    ) : WalletConnectAnalytics(
        event = AnalyticsNames.CONNECTED_DAPPS_LIST_VIEWED.eventName,
        params = mapOf(
            APP_NAME to appName,
            METHOD to method.name,
            ACTION to action.name
        )
    )

    enum class DappConnectionAction {
        CANCEL, CONFIRM, DISCONNECT, DISCONNECT_INTENT
    }

    companion object {
        private const val APP_NAME = "app_name"
        private const val ACTION = "action"
        private const val METHOD = "method"
    }
}

internal fun WCEthereumSignMessage.toAnalyticsMethod(): DappRequestMethod =
    when (this.type) {
        WCEthereumSignMessage.WCSignType.MESSAGE -> DappRequestMethod.ETH_SIGN
        WCEthereumSignMessage.WCSignType.PERSONAL_MESSAGE -> DappRequestMethod.PERSONAL_SIGN
        WCEthereumSignMessage.WCSignType.TYPED_MESSAGE -> DappRequestMethod.ETH_SIGN_TYPED_DATA
    }

internal fun EthereumSendTransactionTarget.Method.toAnalyticsMethod(): DappRequestMethod =
    when (this) {
        EthereumSendTransactionTarget.Method.SIGN -> DappRequestMethod.ETH_SIGN_TRANSACTION
        EthereumSendTransactionTarget.Method.SEND -> DappRequestMethod.ETH_SEND_TRANSACTION
    }

enum class DappRequestMethod {
    ETH_SEND_TRANSACTION, ETH_SIGN, ETH_SIGN_TRANSACTION, ETH_SIGN_TYPED_DATA, PERSONAL_SIGN
}
