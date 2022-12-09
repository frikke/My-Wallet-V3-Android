package piuk.blockchain.android.ui.home

import androidx.appcompat.app.AppCompatActivity
import com.blockchain.chrome.navigation.TransactionFlowNavigation
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.NullCryptoAccount
import com.blockchain.coincore.TransactionTarget
import piuk.blockchain.android.ui.transactionflow.flow.TransactionFlowActivity

class TransactionFlowNavigationImpl(private val activity: AppCompatActivity) : TransactionFlowNavigation {
    override fun startTransactionFlow(
        action: AssetAction,
        sourceAccount: BlockchainAccount?,
        target: TransactionTarget?
    ) {
        activity.startActivity(
            TransactionFlowActivity.newIntent(
                context = activity,
                action = action,
                target = target ?: NullCryptoAccount(),
                sourceAccount = sourceAccount ?: NullCryptoAccount(),
            )
        )
    }
}
