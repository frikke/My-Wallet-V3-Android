package com.blockchain.walletconnect.koin

import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.koin.walletConnectFeatureFlag
import com.blockchain.walletconnect.data.EthWalletAddressProvider
import com.blockchain.walletconnect.data.SignRequestHandler
import com.blockchain.walletconnect.data.WalletConnectMetadataRepository
import com.blockchain.walletconnect.data.WalletConnectService
import com.blockchain.walletconnect.domain.EthRequestSign
import com.blockchain.walletconnect.domain.EthSendTransactionRequest
import com.blockchain.walletconnect.domain.SessionRepository
import com.blockchain.walletconnect.domain.WalletConnectAddressProvider
import com.blockchain.walletconnect.domain.WalletConnectEthAccountProvider
import com.blockchain.walletconnect.domain.WalletConnectServiceAPI
import com.blockchain.walletconnect.domain.WalletConnectUrlValidator
import com.blockchain.walletconnect.ui.dapps.DappsListModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import org.koin.dsl.bind
import org.koin.dsl.module

val walletConnectModule = module {

    scope(payloadScopeQualifier) {

        scoped {
            WalletConnectService(
                sessionRepository = get(),
                client = get(),
                ethRequestSign = get(),
                lifecycleObservable = get(),
                featureFlag = get(walletConnectFeatureFlag),
                walletConnectAccountProvider = get(),
                ethSendTransactionRequest = get()
            )
        }.bind(WalletConnectServiceAPI::class).bind(WalletConnectUrlValidator::class)

        factory {
            SignRequestHandler(
                accountProvider = get()
            )
        }.bind(EthRequestSign::class)
            .bind(EthSendTransactionRequest::class)

        factory {
            EthWalletAddressProvider(
                coincore = get()
            )
        }.bind(WalletConnectAddressProvider::class)
            .bind(WalletConnectEthAccountProvider::class)

        factory {
            WalletConnectMetadataRepository(
                metadataManager = get(),
                featureFlag = get(walletConnectFeatureFlag),
            )
        }.bind(SessionRepository::class)

        factory {
            DappsListModel(
                uiSchedulers = AndroidSchedulers.mainThread(),
                enviromentConfig = get(),
                crashLogger = get(),
                sessionsRepository = get(),
                walletConnectServiceAPI = get()
            )
        }
    }
}
