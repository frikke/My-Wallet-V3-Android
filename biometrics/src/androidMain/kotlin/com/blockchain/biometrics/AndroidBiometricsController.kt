package com.blockchain.biometrics

import android.os.Build
import androidx.annotation.VisibleForTesting
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.ERROR_LOCKOUT
import androidx.biometric.BiometricPrompt.ERROR_LOCKOUT_PERMANENT
import androidx.biometric.BiometricPrompt.ERROR_NEGATIVE_BUTTON
import androidx.biometric.BiometricPrompt.ERROR_USER_CANCELED
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.blockchain.biometrics.BiometricAuthError.BiometricAuthFailed
import com.blockchain.biometrics.BiometricAuthError.BiometricAuthLockout
import com.blockchain.biometrics.BiometricAuthError.BiometricAuthLockoutPermanent
import com.blockchain.biometrics.BiometricAuthError.BiometricAuthOther
import com.blockchain.biometrics.BiometricAuthError.BiometricKeysInvalidated
import com.blockchain.biometrics.BiometricAuthError.BiometricsNoSuitableMethods
import com.blockchain.logging.RemoteLogger
import java.util.concurrent.Executor
import javax.crypto.IllegalBlockSizeException
import timber.log.Timber

interface AndroidBiometricsController<TBiometricData : BiometricData> : BiometricAuth {
    fun authenticate(
        executor: Executor,
        fragment: Fragment,
        type: BiometricsType,
        promptInfo: PromptInfo,
        callback: BiometricsCallback<TBiometricData>
    )

    fun authenticate(
        executor: Executor,
        activity: FragmentActivity,
        type: BiometricsType,
        promptInfo: PromptInfo,
        callback: BiometricsCallback<TBiometricData>
    )
}

data class PromptInfo(
    val title: String,
    val description: String,
    val negativeButtonText: String
) {
    fun asBiometricPromptInfo(): BiometricPrompt.PromptInfo =
        BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setDescription(description)
            .setConfirmationRequired(false)
            .setNegativeButtonText(
                negativeButtonText
            )
            .build()
}

class AndroidBiometricsControllerImpl<TBiometricData : BiometricData>(
    private val biometricData: TBiometricData,
    private val biometricDataFactory: BiometricDataFactory<TBiometricData>,
    private val biometricDataRepository: BiometricDataRepository,
    private val biometricsManager: BiometricManager,
    private val cryptographyManager: CryptographyManager,
    private val remoteLogger: RemoteLogger
) : AndroidBiometricsController<TBiometricData> {

    override val isBiometricAuthEnabled: Boolean
        get() = getStrongAuthMethods() == BiometricManager.BIOMETRIC_SUCCESS

    override val isHardwareDetected: Boolean
        get() = getStrongAuthMethods() != BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE

    override val areBiometricsEnrolled: Boolean
        get() = getStrongAuthMethods() != BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED

    override val isBiometricUnlockEnabled: Boolean
        get() = isBiometricAuthEnabled &&
            biometricDataRepository.isBiometricsEnabled() &&
            getEncodedData().isNotEmpty()

    override fun setBiometricUnlockDisabled() {
        cryptographyManager.clearData(KEY_CIPHER_INITIALIZER)
        biometricDataRepository.clearBiometricEncryptedData()
        biometricDataRepository.setBiometricsEnabled(false)
    }

    private fun getStrongAuthMethods() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            biometricsManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG
            )
        } else {
            @Suppress("DEPRECATION")
            biometricsManager.canAuthenticate()
        }

    override fun authenticate(
        executor: Executor,
        fragment: Fragment,
        type: BiometricsType,
        promptInfo: PromptInfo,
        callback: BiometricsCallback<TBiometricData>
    ) {
        val biometricPrompt = BiometricPrompt(fragment, executor, getAuthenticationCallback(callback, type))

        authenticate(type, biometricPrompt, promptInfo.asBiometricPromptInfo(), callback)
    }

    override fun authenticate(
        executor: Executor,
        activity: FragmentActivity,
        type: BiometricsType,
        promptInfo: PromptInfo,
        callback: BiometricsCallback<TBiometricData>
    ) {
        val biometricPrompt = BiometricPrompt(activity, executor, getAuthenticationCallback(callback, type))
        authenticate(type, biometricPrompt, promptInfo.asBiometricPromptInfo(), callback)
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun getAuthenticationCallback(
        callback: BiometricsCallback<TBiometricData>,
        type: BiometricsType
    ) = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            super.onAuthenticationError(errorCode, errString)
            when (errorCode) {
                ERROR_NEGATIVE_BUTTON,
                ERROR_USER_CANCELED -> callback.onAuthCancelled()
                ERROR_LOCKOUT -> callback.onAuthFailed(BiometricAuthLockout)
                ERROR_LOCKOUT_PERMANENT -> callback.onAuthFailed(BiometricAuthLockoutPermanent)
                else -> callback.onAuthFailed(BiometricAuthOther(errString.toString()))
            }
            Timber.d("Biometric authentication failed: $errorCode - $errString")
        }

        override fun onAuthenticationFailed() {
            super.onAuthenticationFailed()
            callback.onAuthFailed(BiometricAuthFailed)
        }

        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            super.onAuthenticationSucceeded(result)
            if (type == BiometricsType.TYPE_REGISTER) {
                result.cryptoObject?.let { cryptoObject ->
                    try {
                        val encryptedString = processEncryption(cryptoObject, biometricData.asByteArray())
                        storeEncodedData(encryptedString)
                        biometricDataRepository.setBiometricsEnabled(true)
                        callback.onAuthSuccess(biometricData)
                    } catch (e: IllegalBlockSizeException) {
                        callback.onAuthFailed(BiometricKeysInvalidated)
                    } catch (e: Exception) {
                        remoteLogger.logException(e, "Exception when registering biometrics")
                        callback.onAuthFailed(BiometricAuthOther(e.message ?: e.toString()))
                    }
                }
            } else {
                result.cryptoObject?.let { cryptoObject ->
                    try {
                        val data = processDecryption(cryptoObject)
                        callback.onAuthSuccess(biometricDataFactory.fromByteArray(data))
                    } catch (e: IllegalBlockSizeException) {
                        callback.onAuthFailed(BiometricKeysInvalidated)
                    } catch (e: Exception) {
                        remoteLogger.logException(e, "Exception when logging in with biometrics")
                        callback.onAuthFailed(BiometricAuthOther(e.message ?: e.toString()))
                    }
                }
            }
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun authenticate(
        type: BiometricsType,
        biometricPrompt: BiometricPrompt,
        promptInfo: BiometricPrompt.PromptInfo,
        callback: BiometricsCallback<TBiometricData>
    ) {
        if (isBiometricAuthEnabled) {
            when (type) {
                BiometricsType.TYPE_REGISTER -> {
                    handleCipherStates(
                        cryptographyManager.getInitializedCipherForEncryption(KEY_CIPHER_INITIALIZER, true),
                        biometricPrompt,
                        promptInfo,
                        callback
                    )
                }
                BiometricsType.TYPE_LOGIN -> {
                    handleCipherStates(
                        cryptographyManager.getInitializedCipherForDecryption(
                            KEY_CIPHER_INITIALIZER,
                            SEPARATOR,
                            getEncodedData(),
                            true
                        ),
                        biometricPrompt,
                        promptInfo,
                        callback
                    )
                }
            }
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun handleCipherStates(
        state: CipherState,
        biometricPrompt: BiometricPrompt,
        promptInfo: BiometricPrompt.PromptInfo,
        callback: BiometricsCallback<TBiometricData>
    ) =
        when (state) {
            is CipherState.CipherSuccess -> {
                biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(state.cipher))
            }
            is CipherState.CipherInvalidatedError -> {
                setBiometricUnlockDisabled()
                callback.onAuthFailed(BiometricKeysInvalidated)
            }
            is CipherState.CipherNoSuitableBiometrics -> {
                setBiometricUnlockDisabled()
                callback.onAuthFailed(BiometricsNoSuitableMethods)
            }
            is CipherState.CipherOtherError -> {
                callback.onAuthFailed(BiometricAuthOther(state.e.message ?: "Unknown error"))
            }
        }

    /**
     * We must keep the current separator and ordering of data to maintain compatibility with the old fingerprinting library
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun processEncryption(cryptoObject: BiometricPrompt.CryptoObject, unencryptedData: ByteArray): String {
        cryptoObject.cipher?.let {
            return cryptographyManager.encryptAndEncodeData(it, SEPARATOR, unencryptedData)
        } ?: throw IllegalStateException("There is no cipher with which to encrypt")
    }

    /**
     * We must keep the current separator and ordering of data to maintain compatibility with the old fingerprinting library
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun processDecryption(cryptoObject: BiometricPrompt.CryptoObject): ByteArray {
        cryptoObject.cipher?.let { cipher ->
            return cryptographyManager.decodeAndDecryptData(cipher, SEPARATOR, getEncodedData())
        } ?: throw IllegalStateException("There is no cipher with which to decrypt")
    }

    /**
     * Stores encoded and encrypted data to the repository
     */
    fun storeEncodedData(value: String) {
        biometricDataRepository.storeBiometricEncryptedData(value)
    }

    /**
     * Retrieve previously saved encoded & encrypted data from the biometric repository.
     * @return A [String] wrapping the saved String, or empty string if not found
     */
    private fun getEncodedData(): String =
        biometricDataRepository.getBiometricEncryptedData() ?: ""

    companion object {
        private const val SEPARATOR = "-_-"
        private const val KEY_CIPHER_INITIALIZER = "encrypted_pin_code"
    }
}
