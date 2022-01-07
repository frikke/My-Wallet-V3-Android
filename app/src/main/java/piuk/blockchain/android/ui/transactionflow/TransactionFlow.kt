package piuk.blockchain.android.ui.transactionflow

import androidx.annotation.CallSuper
import androidx.annotation.UiThread
import androidx.fragment.app.FragmentManager
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.NullCryptoAccount
import com.blockchain.coincore.TransactionTarget
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.koin.core.component.KoinComponent
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.android.ui.base.mvi.MviFragment.Companion.BOTTOM_SHEET

abstract class DialogFlow : SlidingModalBottomDialog.Host {

    private var fragmentManager: FragmentManager? = null
    private var host: FlowHost? = null
    private var bottomSheetTag: String = SHEET_FRAGMENT_TAG

    interface FlowHost {
        fun onFlowFinished()
    }

    @CallSuper
    open fun startFlow(
        fragmentManager: FragmentManager,
        host: FlowHost
    ) {
        this.fragmentManager = fragmentManager
        this.host = host
    }

    @CallSuper
    open fun finishFlow() {
        if (fragmentManager?.isDestroyed == true)
            return
        host?.onFlowFinished()
    }

    @UiThread
    protected fun replaceBottomSheet(bottomSheet: BottomSheetDialogFragment?) {
        fragmentManager?.let {
            if (it.isDestroyed || it.isStateSaved) return
            val oldSheet = it.findFragmentByTag(bottomSheetTag)
            it.beginTransaction().run {
                apply { oldSheet?.let { sheet -> remove(sheet) } }
                apply { bottomSheet?.let { sheet -> add(sheet, bottomSheetTag) } }
                commitNowAllowingStateLoss()
            }
        }
    }

    companion object {
        const val SHEET_FRAGMENT_TAG = BOTTOM_SHEET
    }
}

// this is now only used to start the flow from the dashboard and differentiate from the asset details sheet flow
class TransactionFlow(
    private val sourceAccount: BlockchainAccount = NullCryptoAccount(),
    private val target: TransactionTarget = NullCryptoAccount(),
    private val action: AssetAction
) : DialogFlow(), KoinComponent {
    // we need to persist these within the flow to access at launch
    val txSource: BlockchainAccount
        get() = sourceAccount
    val txTarget: TransactionTarget
        get() = target
    val txAction: AssetAction
        get() = action

    override fun onSheetClosed() {
        // do nothing
    }
}
