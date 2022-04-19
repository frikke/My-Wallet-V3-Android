package piuk.blockchain.android.ui.base

import android.os.Bundle
import androidx.annotation.CallSuper
import com.blockchain.commonarch.presentation.base.BlockchainActivity

abstract class MvpActivity<V : MvpView, P : MvpPresenter<V>> : BlockchainActivity() {

    protected abstract val presenter: P
    protected abstract val view: V

    final override val alwaysDisableScreenshots
        get() = presenter.alwaysDisableScreenshots

    final override val enableLogoutTimer
        get() = presenter.enableLogoutTimer

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        presenter.onCreateView()
    }

    @CallSuper
    override fun onResume() {
        super.onResume()
        presenter.attachView(view)
    }

    @CallSuper
    override fun onPause() {
        presenter.detachView(view)
        super.onPause()
    }
}
