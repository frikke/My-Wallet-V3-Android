package piuk.blockchain.android.ui.base

import android.os.Bundle
import androidx.annotation.CallSuper
import com.blockchain.notifications.analytics.ProviderSpecificAnalytics
import org.koin.android.ext.android.inject

@Deprecated("Use the kotlin-friendly MvpActivity, MvpPresenter, MvpView instead")
abstract class BaseMvpActivity<VIEW : View, PRESENTER : BasePresenter<VIEW>> : BlockchainActivity() {

    override val alwaysDisableScreenshots: Boolean
        get() = false

    protected var presenter: PRESENTER? = null
        private set

    private val specificAnalytics: ProviderSpecificAnalytics by inject()

    @CallSuper override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        presenter = createPresenter()
        presenter?.initView(getView())
        logScreenView()
    }

    /**
     * Allows us to disable logging of screen viewing on unimportant pages.
     */
    protected open fun logScreenView() {
        specificAnalytics.logContentView(javaClass.simpleName)
    }

    @CallSuper override fun onPause() {
        super.onPause()
        presenter?.onViewPaused()
    }

    @CallSuper override fun onDestroy() {
        super.onDestroy()
        presenter?.onViewDestroyed()
        presenter = null
    }

    protected fun onViewReady() {
        presenter?.onViewReady()
    }

    protected abstract fun createPresenter(): PRESENTER
    protected abstract fun getView(): VIEW
}
