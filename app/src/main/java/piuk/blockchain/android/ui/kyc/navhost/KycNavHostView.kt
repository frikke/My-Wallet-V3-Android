package piuk.blockchain.android.ui.kyc.navhost

import androidx.annotation.StringRes
import androidx.navigation.NavDirections
import piuk.blockchain.android.ui.base.View
import piuk.blockchain.android.ui.kyc.navhost.models.KycEntryPoint

interface KycNavHostView : View {

    val entryPoint: KycEntryPoint

    val isCowboysUser: Boolean

    fun displayLoading(loading: Boolean)

    fun showErrorSnackbarAndFinish(@StringRes message: Int)

    fun navigate(directions: NavDirections)

    fun navigateToKycSplash()

    fun navigateToResubmissionSplash()
}
