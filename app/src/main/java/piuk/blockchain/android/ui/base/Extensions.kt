package piuk.blockchain.android.ui.base

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.airbnb.lottie.LottieAnimationView
import piuk.blockchain.android.R
import piuk.blockchain.android.util.gone

fun FragmentActivity.setupToolbar(resource: Int, homeAsUpEnabled: Boolean = true) {
    (this as? BlockchainActivity)?.let {
        it.setupToolbar(
            it.supportActionBar ?: return,
            resource
        )
        it.supportActionBar?.setDisplayHomeAsUpEnabled(homeAsUpEnabled)
    }
}

fun FragmentActivity.setupToolbar(resource: String, homeAsUpEnabled: Boolean = true) {
    (this as? BlockchainActivity)?.let {
        it.setupToolbar(
            it.supportActionBar ?: return,
            resource
        )
        it.supportActionBar?.setDisplayHomeAsUpEnabled(homeAsUpEnabled)
    }
}

fun FragmentTransaction.addAnimationTransaction(): FragmentTransaction =
    this.setCustomAnimations(
        R.anim.fragment_slide_left_enter,
        R.anim.fragment_slide_left_exit,
        R.anim.fragment_slide_right_enter,
        R.anim.fragment_slide_right_exit
    )

fun FragmentManager.showFragment(
    fragment: Fragment,
    loadingView: LottieAnimationView,
    reloadFragment: Boolean = true
) {
    val transaction = this.beginTransaction()
    val primaryFragment = this.primaryNavigationFragment
    primaryFragment?.let {
        transaction.hide(it)
    }

    val tag = fragment.javaClass.simpleName
    var tempFragment = this.findFragmentByTag(tag)

    if (reloadFragment && tempFragment != null) {
        transaction.remove(tempFragment)
        tempFragment = null
    }

    if (tempFragment == null) {
        tempFragment = fragment
        transaction.add(R.id.content_frame, tempFragment, tag)
    } else {
        transaction.show(tempFragment)
    }
    hideLoading(loadingView)
    transaction.setPrimaryNavigationFragment(tempFragment)
    transaction.commitNowAllowingStateLoss()
}

private fun hideLoading(loadingView: LottieAnimationView) {
    loadingView.gone()
    loadingView.pauseAnimation()
}
