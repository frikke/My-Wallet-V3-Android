package piuk.blockchain.android.ui.launcher

import android.app.Application
import android.content.Intent
import com.blockchain.coincore.AssetAction
import com.blockchain.remoteconfig.IntegratedFeatureFlag
import com.blockchain.walletconnect.domain.WalletConnectServiceAPI
import com.blockchain.walletconnect.domain.WalletConnectUserEvent
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import piuk.blockchain.android.ui.transactionflow.flow.TransactionFlowActivity

class GlobalEventHandler(
    private val application: Application,
    private val walletConnectServiceAPI: WalletConnectServiceAPI,
    private val wcFeatureFlag: IntegratedFeatureFlag
) {

    private val compositeDisposable = CompositeDisposable()

    fun init() {
        compositeDisposable.clear()
        compositeDisposable += wcFeatureFlag.enabled.flatMapObservable { enabled ->
            if (enabled) walletConnectServiceAPI.userEvents
            else Observable.empty()
        }.subscribe { event ->
            when (event) {
                is WalletConnectUserEvent.SignMessage -> startTransactionFlowForSingMessage(event)
                is WalletConnectUserEvent.SendTransaction,
                is WalletConnectUserEvent.SignTransaction -> throw NotImplementedError("Not yet implemented")
            }
        }
    }

    private fun startTransactionFlowForSingMessage(event: WalletConnectUserEvent.SignMessage) {
        val intent = TransactionFlowActivity.newInstance(
            application,
            sourceAccount = event.source,
            target = event.target,
            action = AssetAction.Sign
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        application.startActivity(intent)
    }
}
