package piuk.blockchain.android.rating.presentaion

import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import kotlinx.parcelize.Parcelize

@Parcelize
enum class AppRatingTriggerSource(val value: String) : ModelConfigArgs.ParcelableArgs {
    DASHBOARD("DASHBOARD"),
    BUY("BUY"),
    SETTINGS("SETTINGS");

    companion object {
        const val ARGS_KEY = "AppRatingArgs"
    }
}
