package com.blockchain.commonarch.presentation.mvi

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import com.blockchain.commonarch.BuildConfig
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import timber.log.Timber

abstract class MviActivity<M : MviModel<S, I>, I : MviIntent<S>, S : MviState, E : ViewBinding> : BlockchainActivity() {

    protected abstract val model: M

    var subscription: Disposable? = null

    val binding: E by lazy {
        initBinding()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
    }

    @UiThread
    override fun onResume() {
        super.onResume()
        subscription?.dispose()
        subscription = model.state
            .subscribeBy(
                onNext = {
                    render(it)
                },
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
        subscription?.dispose()
        subscription = null
        super.onPause()
    }

    override fun onDestroy() {
        model.destroy()
        super.onDestroy()
    }

    abstract fun initBinding(): E

    protected abstract fun render(newState: S)

    companion object {
        inline fun <reified T : AppCompatActivity> start(ctx: Context) {
            ctx.startActivity(Intent(ctx, T::class.java))
        }
    }
}
