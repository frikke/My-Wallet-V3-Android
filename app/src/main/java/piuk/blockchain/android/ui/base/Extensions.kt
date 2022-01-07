package piuk.blockchain.android.ui.base

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.airbnb.lottie.LottieAnimationView
import com.blockchain.componentlib.navigation.NavigationBarButton
import piuk.blockchain.android.R
import piuk.blockchain.android.util.gone

fun Fragment.loadToolbar(
    titleToolbar: String = "",
    menuItems: List<NavigationBarButton>? = null,
    backAction: (() -> Unit)? = null
) {
    (activity as? BlockchainActivity)?.loadToolbar(titleToolbar, menuItems, backAction)
}

fun Fragment.updateTitleToolbar(titleToolbar: String = "") {
    (activity as? BlockchainActivity)?.updateTitleToolbar(titleToolbar = titleToolbar)
}

fun Fragment.updateBackButton(backAction: () -> Unit) {
    (activity as? BlockchainActivity)?.updateBackButton(backAction)
}

fun Fragment.updateMenuItems(menuItems: List<NavigationBarButton>) {
    (activity as? BlockchainActivity)?.updateMenuItems(menuItems)
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
    reloadFragment: Boolean = false
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
