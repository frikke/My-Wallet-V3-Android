package piuk.blockchain.android.ui.kyc.address

import com.blockchain.addressverification.ui.AddressVerificationSavingError
import com.blockchain.domain.dataremediation.model.Questionnaire
import piuk.blockchain.android.ui.base.View
import piuk.blockchain.android.ui.kyc.profile.models.ProfileModel

interface KycHomeAddressView : View {

    val profileModel: ProfileModel

    fun showErrorWhileSaving(error: AddressVerificationSavingError)

    fun dismissProgressDialog()

    fun showProgressDialog()

    fun continueToVeriffSplash(countryCode: String)

    fun continueToTier2MoreInfoNeeded(countryCode: String)

    fun continueToQuestionnaire(questionnaire: Questionnaire, countryCode: String)
    fun tier1Complete()
    fun onSddVerified()
}
