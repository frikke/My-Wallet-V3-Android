package piuk.blockchain.android.ui.maintenance.presentation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.blockchain.commonarch.presentation.mvi_v2.ViewState

data class AppMaintenanceViewState(
    @DrawableRes val image: Int,
    @StringRes val title: Int,
    @StringRes val description: Int,
    @StringRes val button1Text: Int?,
    @StringRes val button2Text: Int?
) : ViewState