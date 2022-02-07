package piuk.blockchain.android.ui.base

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.componentlib.navigation.NavigationBarButton
import piuk.blockchain.android.R

fun Fragment.updateToolbar(
    titleToolbar: String = "",
    menuItems: List<NavigationBarButton>? = null,
    backAction: (() -> Unit)? = null
) {
    (activity as? BlockchainActivity)?.updateToolbar(titleToolbar, menuItems, backAction)
}

fun Fragment.updateTitleToolbar(titleToolbar: String = "") {
    (activity as? BlockchainActivity)?.updateToolbarTitle(title = titleToolbar)
}

fun Fragment.updateToolbarBackAction(backAction: () -> Unit) {
    (activity as? BlockchainActivity)?.updateToolbarBackAction(backAction)
}

fun Fragment.updateToolbarMenuItems(menuItems: List<NavigationBarButton>) {
    (activity as? BlockchainActivity)?.updateToolbarMenuItems(menuItems)
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
    transaction.setPrimaryNavigationFragment(tempFragment)
    transaction.commitNowAllowingStateLoss()
}
