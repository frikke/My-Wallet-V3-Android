package piuk.blockchain.android.cards.cvv

import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.button.ButtonState
import piuk.blockchain.android.R

data class SecurityCodeViewState(
    val cardDetailsLoading: Boolean,
    val nextButtonState: ButtonState,
    val cvv: String,
    val cvvLength: Int = 3,
    val cardName: String?,
    val lastCardDigits: String?,
    val cardIcon: ImageResource = ImageResource.Local(id = R.drawable.ic_card_icon, contentDescription = null),
    val error: UpdateSecurityCodeError? = null
) : ViewState
