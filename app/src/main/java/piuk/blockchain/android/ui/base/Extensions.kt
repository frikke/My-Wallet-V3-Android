package piuk.blockchain.android.ui.base

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import piuk.blockchain.android.R

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
