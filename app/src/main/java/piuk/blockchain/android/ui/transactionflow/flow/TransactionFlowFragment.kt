package piuk.blockchain.android.ui.transactionflow.flow

import android.os.Bundle
import androidx.annotation.StringRes
import androidx.viewbinding.ViewBinding
import com.blockchain.commonarch.presentation.mvi.MviFragment
import org.koin.android.ext.android.inject
import org.koin.core.scope.Scope
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.ui.transactionflow.analytics.TxFlowAnalytics
import piuk.blockchain.android.ui.transactionflow.engine.TransactionIntent
import piuk.blockchain.android.ui.transactionflow.engine.TransactionModel
import piuk.blockchain.android.ui.transactionflow.engine.TransactionState

abstract class TransactionFlowFragment<T : ViewBinding> :
    MviFragment<TransactionModel, TransactionIntent, TransactionState, T>() {

    private val scope: Scope by lazy {
        (requireActivity() as TransactionFlowActivity).scope
    }

    override val model: TransactionModel
        get() = scope.get()

    protected val analyticsHooks: TxFlowAnalytics by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.let {
            model.process(TransactionIntent.ResetFlow)
        }
    }

    protected fun showErrorToast(@StringRes msgId: Int) {
        ToastCustom.makeText(
            activity,
            getString(msgId),
            ToastCustom.LENGTH_LONG,
            ToastCustom.TYPE_ERROR
        )
    }
}
