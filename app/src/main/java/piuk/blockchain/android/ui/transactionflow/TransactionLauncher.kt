package piuk.blockchain.android.ui.transactionflow

import android.app.Activity
import androidx.fragment.app.FragmentManager
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.NullCryptoAccount
import com.blockchain.coincore.TransactionTarget
import com.blockchain.remoteconfig.FeatureFlag
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import piuk.blockchain.android.ui.transactionflow.fullscreen.TransactionFlowActivity

class TransactionLauncher(private val fullScreenTxFeatureFlag: FeatureFlag) {

    fun startFlow(
        activity: Activity,
        fragmentManager: FragmentManager,
        flowHost: DialogFlow.FlowHost,
        action: AssetAction,
        sourceAccount: BlockchainAccount = NullCryptoAccount(),
        target: TransactionTarget = NullCryptoAccount(),
        compositeDisposable: CompositeDisposable
    ) {
        compositeDisposable += fullScreenTxFeatureFlag.enabled.subscribeBy(
            onSuccess = { enabled ->
                if (enabled) {
                    activity.startActivity(TransactionFlowActivity.newInstance(activity, sourceAccount, target, action))
                } else {
                    TransactionFlow(sourceAccount, target, action).also {
                        it.startFlow(fragmentManager, flowHost)
                    }
                }
            },
            onError = {
                TransactionFlow(sourceAccount, target, action).also {
                    it.startFlow(fragmentManager, flowHost)
                }
            }
        )
    }
}