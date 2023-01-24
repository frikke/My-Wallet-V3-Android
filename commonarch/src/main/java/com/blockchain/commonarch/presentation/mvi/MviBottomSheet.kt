package com.blockchain.commonarch.presentation.mvi

import android.os.Bundle
import android.view.View
import androidx.annotation.CallSuper
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.commonarch.presentation.base.SlidingModalBottomDialog
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import kotlinx.coroutines.cancel
import timber.log.Timber

abstract class MviBottomSheet<M : MviModel<S, I>, I : MviIntent<S>, S : MviState, E : ViewBinding> :
    SlidingModalBottomDialog<E>() {

    protected abstract val model: M

    private var subscription: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if ((requireActivity() as? BlockchainActivity)?.processDeathOccurredAndThisIsNotLauncherActivity == true) {
            model.disablePermanently()
            lifecycleScope.cancel()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if ((requireActivity() as? BlockchainActivity)?.processDeathOccurredAndThisIsNotLauncherActivity == true) {
            viewLifecycleOwner.lifecycleScope.cancel()
        }
    }

    override fun onResume() {
        super.onResume()
        dispose()
        subscription = model.state.subscribeBy(
            onNext = { render(it) },
            onError = {
                throw it
            },
            onComplete = { Timber.d("***> State on complete!!") }
        )
    }

    override fun onPause() {
        dispose()
        super.onPause()
    }

    protected abstract fun render(newState: S)

    @CallSuper
    protected open fun dispose() {
        subscription?.dispose()
    }
}
