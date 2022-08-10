package piuk.blockchain.android.ui.settings.v2.security

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import com.blockchain.biometrics.BiometricAuthError
import com.blockchain.biometrics.BiometricsCallback
import com.blockchain.biometrics.BiometricsType
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.commonarch.presentation.base.updateToolbar
import com.blockchain.commonarch.presentation.mvi.MviFragment
import com.blockchain.componentlib.alert.BlockchainSnackbar
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.componentlib.tag.TagType
import com.blockchain.componentlib.tag.TagViewState
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.componentlib.viewextensions.visibleIf
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.koin.backupPhraseFeatureFlag
import com.blockchain.koin.scopedInject
import com.blockchain.presentation.backup.BackupPhraseActivity
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.data.biometrics.BiometricPromptUtil
import piuk.blockchain.android.data.biometrics.BiometricsController
import piuk.blockchain.android.data.biometrics.WalletBiometricData
import piuk.blockchain.android.databinding.FragmentSecurityBinding
import piuk.blockchain.android.ui.BottomSheetInformation
import piuk.blockchain.android.ui.backup.BackupWalletActivity
import piuk.blockchain.android.ui.settings.SettingsAnalytics
import piuk.blockchain.android.ui.settings.v2.SettingsNavigator
import piuk.blockchain.android.ui.settings.v2.SettingsScreen
import piuk.blockchain.android.ui.settings.v2.sheets.BackupPhraseInfoSheet
import piuk.blockchain.android.ui.settings.v2.sheets.BiometricsInfoSheet
import piuk.blockchain.android.ui.settings.v2.sheets.TwoFactorInfoSheet
import piuk.blockchain.android.ui.settings.v2.sheets.sms.SMSPhoneVerificationBottomSheet
import piuk.blockchain.android.urllinks.WEB_WALLET_LOGIN_URI

class SecurityFragment :
    MviFragment<SecurityModel, SecurityIntent, SecurityState, FragmentSecurityBinding>(),
    SMSPhoneVerificationBottomSheet.Host,
    TwoFactorInfoSheet.Host,
    BiometricsInfoSheet.Host,
    BackupPhraseInfoSheet.Host,
    BottomSheetInformation.Host,
    SettingsScreen {

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentSecurityBinding =
        FragmentSecurityBinding.inflate(inflater, container, false)

    override fun navigator(): SettingsNavigator =
        (activity as? SettingsNavigator) ?: throw IllegalStateException(
            "Parent must implement SettingsNavigator"
        )

    override val model: SecurityModel by scopedInject()

    private val biometricsController: BiometricsController by scopedInject()

    private val onBiometricsAddedResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        model.process(SecurityIntent.ToggleBiometrics)
    }

    private val backupFeatureFlag: FeatureFlag by inject(backupPhraseFeatureFlag)

    private val onBackupResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        model.process(SecurityIntent.LoadInitialInformation)
    }

    private val onWebWalletOpenResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        model.process(SecurityIntent.LoadInitialInformation)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUi()
    }

    private fun initUi() {
        updateToolbar(
            toolbarTitle = getString(R.string.security_toolbar),
            menuItems = emptyList()
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
                    model.process(SecurityIntent.CheckCanChangePassword)
                }
            }

            securityChangePin.apply {
                primaryText = getString(R.string.security_pin_title)
                secondaryText = getString(R.string.security_pin_subtitle)
                onClick = {
                    navigator().goToPinChange()
                }
            }

            securityBackupPhrase.apply {
                primaryText = getString(R.string.security_backup_phrase_title)
                secondaryText = getString(R.string.security_backup_phrase_subtitle)
                onClick = {
                    startBackupPhraseFlow()
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

            securityCloudBackup.apply {
                primaryText = getString(R.string.enable_cloud_backup)
                secondaryText = getString(R.string.enable_cloud_backup_summary)
                onCheckedChange = {
                    model.process(SecurityIntent.ToggleCloudBackup)
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
            renderViewState(newState.securityViewState)
        }

        newState.securityInfo?.let {
            renderSecuritySettings(it)
        }

        if (newState.errorState != SecurityError.NONE) {
            processError(newState.errorState)
        }
    }

    private fun renderViewState(viewState: SecurityViewState) {
        when (viewState) {
            SecurityViewState.ConfirmBiometricsDisabling -> {
                showDisableBiometricsConfirmationSheet()
            }
            SecurityViewState.ShowEnrollBiometrics -> {
                showNoBiometricsAddedSheet()
            }
            SecurityViewState.ShowEnableBiometrics -> {
                showBiometricsConfirmationSheet()
            }
            SecurityViewState.ShowEnterPhoneNumberRequired -> {
                showPhoneNumberRequired()
            }
            is SecurityViewState.ShowVerifyPhoneNumberRequired -> {
                showBottomSheet(SMSPhoneVerificationBottomSheet.newInstance(viewState.phoneNumber))
            }
            SecurityViewState.ShowDisablingOnWebRequired -> {
                showBottomSheet(
                    TwoFactorInfoSheet.newInstance(TwoFactorInfoSheet.Companion.TwoFaSheetMode.DISABLE_ON_WEB)
                )
            }
            SecurityViewState.ShowConfirmTwoFaEnabling -> {
                showBottomSheet(TwoFactorInfoSheet.newInstance(TwoFactorInfoSheet.Companion.TwoFaSheetMode.ENABLE))
            }
            SecurityViewState.LaunchPasswordChange -> {
                navigator().goToPasswordChange()
            }
            SecurityViewState.ShowMustBackWalletUp -> {
                showBottomSheet(BackupPhraseInfoSheet.newInstance())
            }
            SecurityViewState.None -> {
                // do nothing
            }
        }
        model.process(SecurityIntent.ResetViewState)
    }

    private fun renderSecuritySettings(securityInfo: SecurityInfo) {
        with(binding) {
            if (securityInfo.isBiometricsVisible) {
                biometricsBottomDivider.visible()
                securityBiometrics.visible()
                securityBiometrics.isChecked = securityInfo.isBiometricsEnabled
            } else {
                biometricsBottomDivider.gone()
                securityBiometrics.gone()
            }
            securityBackupPhrase.apply {
                tags = if (securityInfo.isWalletBackedUp) {
                    listOf(TagViewState(getString(R.string.security_backup_phrase_pill_backed_up), TagType.Success()))
                } else {
                    listOf(TagViewState(getString(R.string.security_backup_phrase_pill_not_backed_up), TagType.Error()))
                }
            }
            securityTwoFa.isChecked = securityInfo.isTwoFaEnabled
            securityScreenshots.isChecked = securityInfo.areScreenshotsEnabled
            securityTor.visibleIf { securityInfo.isTorFilteringEnabled }
            securityTor.isChecked = securityInfo.isTorFilteringEnabled
            torBottomDivider.visibleIf { securityInfo.isTorFilteringEnabled }
            securityCloudBackup.isChecked = securityInfo.isCloudBackupEnabled
        }
    }

    private fun processError(errorState: SecurityError) {
        when (errorState) {
            SecurityError.LOAD_INITIAL_INFO_FAIL -> {
                showErrorSnackbar(R.string.security_error_initial_info_load)
                (requireActivity() as BlockchainActivity).onBackPressed()
            }
            SecurityError.PIN_MISSING_EXCEPTION -> {
                showErrorSnackbar(R.string.security_error_pin_missing)
                (requireActivity() as BlockchainActivity).onBackPressed()
            }
            SecurityError.BIOMETRICS_DISABLING_FAIL -> {
                showErrorSnackbar(R.string.security_error_biometrics_disable)
            }
            SecurityError.TWO_FA_TOGGLE_FAIL -> {
                showErrorSnackbar(R.string.security_error_two_fa)
            }
            SecurityError.TOR_FILTER_UPDATE_FAIL -> {
                showErrorSnackbar(R.string.security_error_two_fa)
            }
            SecurityError.SCREENSHOT_UPDATE_FAIL -> {
                showErrorSnackbar(R.string.security_error_screenshots)
            }
            SecurityError.NONE -> {
                // do nothing
            }
        }
        model.process(SecurityIntent.ResetErrorState)
    }

    private fun showErrorSnackbar(@StringRes message: Int) {
        BlockchainSnackbar.make(
            binding.root,
            getString(message),
            type = SnackbarType.Error
        ).show()
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
                requireContext(),
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
            is BiometricAuthError.BiometricsNoSuitableMethods -> showNoBiometricsAddedSheet()
            is BiometricAuthError.BiometricAuthLockout -> BiometricPromptUtil.showAuthLockoutDialog(requireContext())
            is BiometricAuthError.BiometricAuthLockoutPermanent ->
                BiometricPromptUtil.showPermanentAuthLockoutDialog(requireContext())
            is BiometricAuthError.BiometricAuthOther ->
                BiometricPromptUtil.showBiometricsGenericError(requireContext(), error.error)
            else -> {
                // do nothing
            }
        }
        model.process(SecurityIntent.DisableBiometrics)
        analytics.logEvent(SettingsAnalytics.BiometricsOptionUpdated(false))
    }

    private fun showDisableBiometricsConfirmationSheet() {
        showBottomSheet(
            BiometricsInfoSheet.newInstance(BiometricsInfoSheet.Companion.BiometricSheetMode.DISABLE_CONFIRMATION)
        )
    }

    private fun showNoBiometricsAddedSheet() {
        showBottomSheet(
            BiometricsInfoSheet.newInstance(BiometricsInfoSheet.Companion.BiometricSheetMode.NO_BIOMETRICS_ADDED)
        )
    }

    private fun showPhoneNumberRequired() {
        showBottomSheet(
            BottomSheetInformation.newInstance(
                title = getString(R.string.security_missing_phone_number),
                description = getString(R.string.security_missing_phone_number_description),
                primaryCtaText = getString(R.string.security_missing_phone_number_cta),
            )
        )
    }

    override fun onPhoneNumberVerified() {
        model.process(SecurityIntent.ToggleTwoFa)
    }

    override fun onEnableSMSTwoFa() {
        model.process(SecurityIntent.EnableTwoFa)
    }

    override fun onActionOnWebTwoFa() {
        onWebWalletOpenResult.launch(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.$WEB_WALLET_LOGIN_URI")))
    }

    override fun onPositiveActionClicked(sheetMode: BiometricsInfoSheet.Companion.BiometricSheetMode) {
        when (sheetMode) {
            BiometricsInfoSheet.Companion.BiometricSheetMode.DISABLE_CONFIRMATION -> {
                model.process(SecurityIntent.DisableBiometrics)
            }
            BiometricsInfoSheet.Companion.BiometricSheetMode.NO_BIOMETRICS_ADDED -> {
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
        }
    }

    override fun onBackupNow() {
        startBackupPhraseFlow()
    }

    private fun startBackupPhraseFlow() {
        backupFeatureFlag.enabled.subscribe { isEnabled ->
            if (isEnabled) {
                launchPhraseRecovery()
            } else {
                onBackupResult.launch(BackupWalletActivity.newIntent(requireContext()))
            }
        }
    }

    private fun launchPhraseRecovery() {
        startActivity(BackupPhraseActivity.newIntent(context = activity))
    }

    override fun onSheetClosed() {
        // do nothing
    }

    companion object {
        fun newInstance() = SecurityFragment()
    }

    override fun primaryButtonClicked() {
        navigator().goToProfile()
    }

    override fun secondButtonClicked() {
        // do nothing
    }
}
