package com.blockchain.commonarch.presentation.mvi

import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import androidx.annotation.UiThread
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import com.blockchain.analytics.Analytics
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.commonarch.presentation.base.SlidingModalBottomDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import kotlinx.coroutines.cancel
import org.koin.core.error.ClosedScopeException
import timber.log.Timber

abstract class MviComposeFragment<M : MviModel<S, I>, I : MviIntent<S>, S : MviState> : Fragment() {
    protected abstract val model: M

    var subscription: Disposable? = null

    override fun onResume() {
        super.onResume()
        subscription?.dispose()
        subscription = model.state.subscribeBy(
            onNext = { render(it) },
            onError = {
                throw it
            },
            onComplete = { Timber.d("***> State on complete!!") }
        )
    }

    override fun onPause() {
        subscription?.dispose()
        subscription = null
        super.onPause()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (activity.processDeathOccurredAndThisIsNotLauncherActivity) {
            model.disablePermanently()
            lifecycleScope.cancel()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (activity.processDeathOccurredAndThisIsNotLauncherActivity) {
            viewLifecycleOwner.lifecycleScope.cancel()
        }
    }

    override fun onDestroy() {
        try {
            model.destroy()
        } catch (e: ClosedScopeException) {
            Timber.e("Attempting to access model on a closed scope")
        }
        super.onDestroy()
    }

    private fun render(newState: S) {}

    protected open fun renderError(t: Throwable) {
        Timber.e(t)
    }

    protected val activity: BlockchainActivity
        get() = requireActivity() as? BlockchainActivity
            ?: throw IllegalStateException("Root activity is not a BlockchainActivity")

    protected val analytics: Analytics
        get() = activity.analytics

    @UiThread
    protected fun showAlert(dlg: AlertDialog) = activity.showAlert(dlg)

    @UiThread
    protected fun clearAlert() = activity.clearAlert()

    @UiThread
    fun showProgressDialog(@StringRes messageId: Int, onCancel: (() -> Unit)? = null) =
        activity.showProgressDialog(messageId, onCancel)

    @UiThread
    fun dismissProgressDialog() = activity.dismissProgressDialog()

    @UiThread
    fun updateProgressDialog(msg: String) = activity.updateProgressDialog(msg)

    @UiThread
    fun showBottomSheet(bottomSheet: BottomSheetDialogFragment?) =
        bottomSheet?.show(childFragmentManager, BOTTOM_SHEET)

    @UiThread
    fun clearBottomSheet() {
        val dlg = childFragmentManager.findFragmentByTag(BOTTOM_SHEET)

        dlg?.let {
            (it as? SlidingModalBottomDialog<ViewBinding>)?.dismiss()
                ?: throw IllegalStateException("Fragment is not a $BOTTOM_SHEET")
        }
    }

    companion object {
        const val BOTTOM_SHEET = "BOTTOM_SHEET"
    }
}
