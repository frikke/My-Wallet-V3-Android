package piuk.blockchain.android.ui.settings.v2.security

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import com.blockchain.biometrics.BiometricAuthError
import com.blockchain.biometrics.BiometricsCallback
import com.blockchain.biometrics.BiometricsType
import com.blockchain.commonarch.presentation.mvi.MviActivity
import com.blockchain.componentlib.databinding.ToolbarGeneralBinding
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.componentlib.viewextensions.visibleIf
import com.blockchain.koin.scopedInject
import piuk.blockchain.android.R
import piuk.blockchain.android.data.biometrics.BiometricPromptUtil
import piuk.blockchain.android.data.biometrics.BiometricsController
import piuk.blockchain.android.data.biometrics.WalletBiometricData
import piuk.blockchain.android.databinding.ActivitySecurityBinding
import piuk.blockchain.android.ui.customviews.toast
import piuk.blockchain.android.ui.settings.SettingsAnalytics

class SecurityActivity : MviActivity<SecurityModel, SecurityIntent, SecurityState, ActivitySecurityBinding>() {

    override val model: SecurityModel by scopedInject()

    override fun initBinding(): ActivitySecurityBinding = ActivitySecurityBinding.inflate(layoutInflater)

    override val alwaysDisableScreenshots: Boolean = true

    override val toolbarBinding: ToolbarGeneralBinding
        get() = binding.toolbar

    private val biometricsController: BiometricsController by scopedInject()

    private val onBiometricsAddedResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        model.process(SecurityIntent.ToggleBiometrics)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        updateToolbar(
            toolbarTitle = getString(R.string.security_toolbar),
            backAction = { onBackPressed() }
        )

        with(binding) {
            securityTwoFa.apply {
                primaryText = getString(R.string.security_two_fa_title)
                onCheckedChange = {
                    model.process(SecurityIntent.ToggleTwoFa)
                }
            }

            securityChangePassword.apply {
                primaryText = getString(R.string.security_password_title)
                secondaryText = getString(R.string.security_password_subtitle)
                onClick = {
                    toast("Coming soon")
                }
            }

            securityChangePin.apply {
                primaryText = getString(R.string.security_pin_title)
                secondaryText = getString(R.string.security_pin_subtitle)
                onClick = {
                    toast("Coming soon")
                }
            }

            securityBackupPhrase.apply {
                primaryText = getString(R.string.security_backup_phrase_title)
                secondaryText = getString(R.string.security_backup_phrase_subtitle)
                onClick = {
                    toast("Coming soon")
                }
            }

            securityBiometrics.apply {
                gone()
                primaryText = getString(R.string.security_biometrics_title)
                onCheckedChange = {
                    model.process(SecurityIntent.ToggleBiometrics)
                }
            }
            biometricsBottomDivider.gone()

            securityScreenshots.apply {
                primaryText = getString(R.string.security_screenshots_title)
                onCheckedChange = {
                    model.process(SecurityIntent.ToggleScreenshots)
                }
            }

            securityTor.apply {
                primaryText = getString(R.string.security_tor_title)
                secondaryText = getString(R.string.security_tor_subtitle)
                onCheckedChange = {
                    model.process(SecurityIntent.ToggleTor)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        model.process(SecurityIntent.LoadInitialInformation)
    }

    override fun render(newState: SecurityState) {
        if (newState.securityViewState != SecurityViewState.None) {
            when (newState.securityViewState) {
                SecurityViewState.ConfirmBiometricsDisabling -> {
                    showDisableBiometricsConfirmationDialog()
                }
                SecurityViewState.ShowEnrollBiometrics -> {
                    showNoBiometricsAddedDialog()
                }
                SecurityViewState.ShowEnableBiometrics -> {
                    showBiometricsConfirmationSheet()
                }
                SecurityViewState.ShowVerifyPhoneNumberRequired -> {
                    // TODO show phone number verification bottom sheet from Profile when that is merged to develop
                }
                SecurityViewState.ShowDisablingOnWebRequired -> {
                    // TODO
                }
                SecurityViewState.None -> {
                    // do nothing
                }
            }
            model.process(SecurityIntent.ResetViewState)
        }

        newState.securityInfo?.let {
            renderSecuritySettings(it)
        }

        if (newState.errorState != SecurityError.NONE) {
            processError(newState.errorState)
        }
    }

    private fun renderSecuritySettings(it: SecurityInfo) {
        with(binding) {
            if (it.isBiometricsVisible) {
                biometricsBottomDivider.visible()
                securityBiometrics.visible()
                securityBiometrics.isChecked = it.isBiometricsEnabled
            } else {
                biometricsBottomDivider.gone()
                securityBiometrics.gone()
            }
            securityTwoFa.isChecked = it.isTwoFaEnabled
            securityScreenshots.isChecked = it.areScreenshotsEnabled
            securityTor.visibleIf { it.isTorFilteringEnabled }
            securityTor.isChecked = it.isTorFilteringEnabled
            torBottomDivider.visibleIf { it.isTorFilteringEnabled }
        }
    }

    private fun processError(errorState: SecurityError) {
        when (errorState) {
            SecurityError.LOAD_INITIAL_INFO_FAIL -> {
                toast("Error loading initial info")
            }
            SecurityError.PIN_MISSING_EXCEPTION -> {
                toast("PIN missing")
            }
            SecurityError.BIOMETRICS_DISABLING_FAIL -> {
                toast("failed disabling biometrics")
            }
            SecurityError.TWO_FA_TOGGLE_FAIL -> {
                toast("failed toggling two fa")
            }
            SecurityError.TOR_FILTER_UPDATE_FAIL -> {
                toast("failed toggling tor")
            }
            SecurityError.SCREENSHOT_UPDATE_FAIL -> {
                toast("failed toggling screenshots")
            }
            SecurityError.NONE -> {
                // do nothing
            }
        }
        model.process(SecurityIntent.ResetErrorState)
    }

    private fun showBiometricsConfirmationSheet() {
        biometricsController.authenticate(
            this, BiometricsType.TYPE_REGISTER,
            object : BiometricsCallback<WalletBiometricData> {
                override fun onAuthSuccess(data: WalletBiometricData) {
                    model.process(SecurityIntent.ToggleBiometrics)
                }

                override fun onAuthFailed(error: BiometricAuthError) {
                    handleAuthFailed(error)
                }

                override fun onAuthCancelled() {
                    model.process(SecurityIntent.DisableBiometrics)
                }
            }
        )
    }

    private fun handleAuthFailed(error: BiometricAuthError) {
        when (error) {
            is BiometricAuthError.BiometricKeysInvalidated -> BiometricPromptUtil.showActionableInvalidatedKeysDialog(
                this,
                positiveActionCallback = {
                    model.process(SecurityIntent.ToggleBiometrics)
                },
                negativeActionCallback = {
                    onBiometricsAddedResult.launch(
                        Intent(
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                                android.provider.Settings.ACTION_SECURITY_SETTINGS
                            } else {
                                android.provider.Settings.ACTION_BIOMETRIC_ENROLL
                            }
                        )
                    )
                }
            )
            is BiometricAuthError.BiometricsNoSuitableMethods -> showNoBiometricsAddedDialog()
            is BiometricAuthError.BiometricAuthLockout -> BiometricPromptUtil.showAuthLockoutDialog(this)
            is BiometricAuthError.BiometricAuthLockoutPermanent -> BiometricPromptUtil.showPermanentAuthLockoutDialog(
                this
            )
            is BiometricAuthError.BiometricAuthOther -> BiometricPromptUtil.showBiometricsGenericError(
                this, error.error
            )
            else -> {
                // do nothing
            }
        }
        model.process(SecurityIntent.DisableBiometrics)
        analytics.logEvent(SettingsAnalytics.BiometricsOptionUpdated(false))
    }

    private fun showDisableBiometricsConfirmationDialog() {
        AlertDialog.Builder(this, R.style.AlertDialogStyle)
            .setTitle(R.string.app_name)
            .setMessage(R.string.biometric_disable_message)
            .setCancelable(true)
            .setPositiveButton(R.string.common_yes) { di, _ ->
                model.process(SecurityIntent.DisableBiometrics)
                di.dismiss()
            }
            .setNegativeButton(android.R.string.cancel) { di, _ ->
                di.dismiss()
            }
            .show()
    }

    private fun showNoBiometricsAddedDialog() {
        AlertDialog.Builder(this, R.style.AlertDialogStyle)
            .setTitle(R.string.app_name)
            .setMessage(R.string.fingerprint_no_fingerprints_added)
            .setCancelable(true)
            .setPositiveButton(R.string.common_yes) { _, _ ->
                onBiometricsAddedResult.launch(
                    Intent(
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                            android.provider.Settings.ACTION_SECURITY_SETTINGS
                        } else {
                            android.provider.Settings.ACTION_BIOMETRIC_ENROLL
                        }
                    )
                )
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    companion object {
        fun newIntent(context: Context) = Intent(context, SecurityActivity::class.java)
    }
}
