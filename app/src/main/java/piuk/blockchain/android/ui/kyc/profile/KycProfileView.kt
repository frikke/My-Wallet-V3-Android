package piuk.blockchain.android.ui.kyc.profile

import java.util.Calendar
import piuk.blockchain.android.ui.base.View
import piuk.blockchain.android.ui.kyc.address.models.OldProfileModel
import piuk.blockchain.android.ui.kyc.profile.models.ProfileModel

interface KycProfileView : View {

    val firstName: String

    val lastName: String

    val countryCode: String

    val stateCode: String?

    val stateName: String?

    var dateOfBirth: Calendar?

    fun setButtonEnabled(enabled: Boolean)

    fun navigateToAddressVerification(profileModel: ProfileModel)

    fun navigateToOldAddressVerification(oldProfileModel: OldProfileModel)

    fun showErrorSnackbar(message: String)

    fun dismissProgressDialog()

    fun showProgressDialog()

    fun restoreUiState(
        firstName: String,
        lastName: String,
        displayDob: String,
        dobCalendar: Calendar
    )
}
