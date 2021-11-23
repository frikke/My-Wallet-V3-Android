package piuk.blockchain.android.ui.base

import androidx.fragment.app.Fragment
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import org.koin.android.ext.android.inject
import piuk.blockchain.android.ui.home.v2.RedesignMainActivity
import piuk.blockchain.android.util.ActivityIndicator
import piuk.blockchain.android.util.AppUtil

open class ViewPagerFragment : Fragment() {

    protected var activityIndicator: ActivityIndicator = ActivityIndicator()

    private val appUtil: AppUtil by inject()
    private val disposable = CompositeDisposable()
    private var isFirstLoad = true

    override fun onResume() {
        super.onResume()

        val blockchainActivity = activity as? BlockchainActivity

        isFirstLoad = false
        if (!isFirstLoad) onResumeFragment()

        if (blockchainActivity == null) return
        disposable += activityIndicator.loading
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy {
                if (it == true) {
                    blockchainActivity.showLoading()
                } else {
                    blockchainActivity.hideLoading()
                }
            }
    }

    override fun onPause() {
        super.onPause()
        ((activity as? BlockchainActivity) ?: (activity as? RedesignMainActivity))?.hideLoading()
        disposable.clear()
    }

    open fun onResumeFragment() {}
}
