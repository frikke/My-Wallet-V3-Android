package piuk.blockchain.android.ui.educational.walletmodes

import androidx.lifecycle.ViewModel
import com.blockchain.preferences.EducationalScreensPrefs

class EducationalWalletModeViewModel(
    private val educationalScreensPrefs: EducationalScreensPrefs
) : ViewModel() {
    fun markAsSeen() {
        educationalScreensPrefs.hasSeenEducationalWalletMode = true
    }
}
