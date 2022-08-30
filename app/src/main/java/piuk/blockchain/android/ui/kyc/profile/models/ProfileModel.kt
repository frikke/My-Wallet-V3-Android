package piuk.blockchain.android.ui.kyc.profile.models

import android.os.Parcelable
import com.blockchain.domain.common.model.CountryIso
import com.blockchain.domain.common.model.StateIso
import kotlinx.parcelize.Parcelize

@Parcelize
data class ProfileModel(
    val firstName: String,
    val lastName: String,
    val countryCode: CountryIso,
    val stateCode: StateIso?,
) : Parcelable
