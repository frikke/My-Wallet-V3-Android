package piuk.blockchain.android.cards.cvv

import com.blockchain.commonarch.presentation.mvi_v2.Intent

sealed class SecurityCodeIntent : Intent<SecurityCodeModelState> {
    data class CvvInputChanged(val cvvValue: String) : SecurityCodeIntent()
    object NextClicked : SecurityCodeIntent()
}
