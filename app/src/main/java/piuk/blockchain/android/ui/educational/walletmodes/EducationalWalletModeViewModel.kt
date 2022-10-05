package piuk.blockchain.android.ui.educational.walletmodes

import androidx.lifecycle.ViewModel
import com.blockchain.preferences.SuperAppMvpPrefs

class EducationalWalletModeViewModel(
    private val educationalScreensPrefs: SuperAppMvpPrefs
) : ViewModel() {
    fun markAsSeen() {
        educationalScreensPrefs.hasSeenEducationalWalletMode = true
    }
}
