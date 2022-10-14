package piuk.blockchain.android.ui.kyc.mobile.entry

import androidx.annotation.StringRes
import com.blockchain.core.settings.PhoneNumber
import io.reactivex.rxjava3.core.Observable
import piuk.blockchain.android.ui.base.View
import piuk.blockchain.android.ui.kyc.mobile.entry.models.PhoneDisplayModel

interface KycMobileEntryView : View {

    val uiStateObservable: Observable<Pair<PhoneNumber, Unit>>

    fun preFillPhoneNumber(phoneNumber: String)

    fun showErrorSnackbar(@StringRes message: Int)

    fun dismissProgressDialog()

    fun showProgressDialog()

    fun continueSignUp(displayModel: PhoneDisplayModel)
}
