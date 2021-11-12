package piuk.blockchain.android.ui.home.v2

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.airbnb.lottie.LottieAnimationView
import piuk.blockchain.android.R
import piuk.blockchain.android.util.gone

class FragmentTransitions {

    companion object {
        fun showFragment(
            fragmentManager: FragmentManager,
            fragment: Fragment,
            loadingView: LottieAnimationView,
            reloadFragment: Boolean = true
        ) {
            val transaction = fragmentManager.beginTransaction()
            val primaryFragment = fragmentManager.primaryNavigationFragment
            primaryFragment?.let {
                transaction.hide(it)
            }

            val tag = fragment.javaClass.simpleName
            var tempFragment = fragmentManager.findFragmentByTag(tag)

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
    }
}
