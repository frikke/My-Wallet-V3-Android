package com.blockchain.commonarch.presentation.mvi

import androidx.annotation.CallSuper
import androidx.viewbinding.ViewBinding
import com.blockchain.commonarch.BuildConfig
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import com.blockchain.commonarch.presentation.base.SlidingModalBottomDialog
import timber.log.Timber

abstract class MviBottomSheet<M : MviModel<S, I>, I : MviIntent<S>, S : MviState, E : ViewBinding> :
    SlidingModalBottomDialog<E>() {

    protected abstract val model: M

    private var subscription: Disposable? = null

    override fun onResume() {
        super.onResume()
        dispose()
        subscription = model.state.subscribeBy(
            onNext = { render(it) },
            onError = {
                if (BuildConfig.DEBUG) {
                    throw it
                }
                Timber.e(it)
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
