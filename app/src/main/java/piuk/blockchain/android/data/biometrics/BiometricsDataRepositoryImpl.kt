package piuk.blockchain.android.data.biometrics

import com.blockchain.biometrics.BiometricDataRepository
import com.blockchain.preferences.AuthPrefs

class BiometricsDataRepositoryImpl(
    val authPrefs: AuthPrefs
) : BiometricDataRepository {
    override fun isBiometricsEnabled(): Boolean =
        authPrefs.biometricsEnabled

    override fun setBiometricsEnabled(enabled: Boolean) {
        authPrefs.biometricsEnabled = enabled
    }

    override fun storeBiometricEncryptedData(value: String) {
        authPrefs.encodedPin = value
    }

    override fun getBiometricEncryptedData(): String? =
        authPrefs.encodedPin

    override fun clearBiometricEncryptedData() {
        authPrefs.clearEncodedPin()
    }
}
