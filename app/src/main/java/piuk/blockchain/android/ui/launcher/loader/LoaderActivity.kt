package piuk.blockchain.android.ui.launcher.loader

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatEditText
import com.blockchain.analytics.events.KYCAnalyticsEvents
import com.blockchain.analytics.events.LaunchOrigin
import com.blockchain.commonarch.presentation.mvi.MviActivity
import com.blockchain.componentlib.alert.BlockchainSnackbar
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.componentlib.databinding.ToolbarGeneralBinding
import com.blockchain.componentlib.navigation.ModeBackgroundColor
import com.blockchain.componentlib.navigation.NavigationBarButton
import com.blockchain.componentlib.viewextensions.getAlertDialogPaddedView
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.hideKeyboard
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.componentlib.viewextensions.visibleIf
import com.blockchain.presentation.koin.scopedInject
import io.reactivex.rxjava3.disposables.CompositeDisposable
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ActivityLoaderBinding
import piuk.blockchain.android.ui.home.HomeActivityLauncher
import piuk.blockchain.android.ui.kyc.email.entry.EmailEntryHost
import piuk.blockchain.android.ui.kyc.email.entry.KycEmailVerificationFragment
import piuk.blockchain.android.ui.launcher.LauncherActivity
import piuk.blockchain.android.ui.settings.security.pin.PinActivity
import piuk.blockchain.android.util.AppUtil

class LoaderActivity :
    MviActivity<LoaderModel, LoaderIntents, LoaderState, ActivityLoaderBinding>(),
    EmailEntryHost {

    private val referralCode: String? by lazy {
        intent?.getStringExtra(PinActivity.KEY_REFERRAL_CODE)
    }

    private val compositeDisposable = CompositeDisposable()

    override val model: LoaderModel by scopedInject()

    override val alwaysDisableScreenshots: Boolean = true

    override val statusbarColor = ModeBackgroundColor.Current

    override fun initBinding(): ActivityLoaderBinding = ActivityLoaderBinding.inflate(layoutInflater)

    override val toolbarBinding: ToolbarGeneralBinding
        get() = binding.toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        updateToolbarBackground(modeColor = ModeBackgroundColor.None, mutedBackground = true)

        val extras = intent?.extras
        val isPinValidated = extras?.getBoolean(INTENT_EXTRA_VERIFIED, false) ?: false
        val loginMethod = (extras?.getSerializable(AppUtil.LOGIN_METHOD) as? LoginMethod)
            ?: LoginMethod.UNDEFINED
        model.process(LoaderIntents.CheckIsLoggedIn(isPinValidated, loginMethod, referralCode))
    }

    override fun render(newState: LoaderState) {
        when (val loaderStep = newState.nextLoadingStep) {
            is LoadingStep.Launcher -> startSingleActivity(LauncherActivity::class.java)
            is LoadingStep.RequestPin -> onRequestPin()
            // These below should always come only after a ProgressStep.FINISH has been emitted
            is LoadingStep.EmailVerification -> launchEmailVerification()
            is LoadingStep.Main -> onStartMainActivity(loaderStep.data, loaderStep.shouldLaunchUiTour)
            else -> {
                // do nothing
            }
        }

        updateUi(newState)
    }

    private fun updateUi(newState: LoaderState) {
        when (newState.nextProgressStep) {
            ProgressStep.START -> {
                updateProgressVisibility(true)
            }
            ProgressStep.LOADING_PRICES -> {
                updateProgressVisibility(true)
                updateProgressText(com.blockchain.stringResources.R.string.loading_prices)
            }
            ProgressStep.SYNCING_ACCOUNT -> {
                updateProgressVisibility(true)
                updateProgressText(com.blockchain.stringResources.R.string.syncing_account)
            }
            ProgressStep.DECRYPTING_WALLET -> {
                updateProgressVisibility(true)
                updateProgressText(com.blockchain.stringResources.R.string.decrypting_wallet)
            }
            ProgressStep.FINISH -> {
                updateProgressVisibility(false)
            }
            else -> {}
        }

        if (newState.shouldShowSecondPasswordDialog) {
            showSecondPasswordDialog()
        }

        if (newState.shouldShowMetadataNodeFailure) {
            showMetadataNodeFailure()
        }

        if (newState.toastType != null) {
            BlockchainSnackbar.make(
                binding.root,
                getString(
                    when (newState.toastType) {
                        ToastType.INVALID_PASSWORD -> com.blockchain.stringResources.R.string.invalid_password
                        ToastType.UNEXPECTED_ERROR -> com.blockchain.stringResources.R.string.unexpected_error
                    }
                ),
                type = SnackbarType.Error
            ).show()
        }
    }

    override fun onEmailEntryFragmentUpdated(showSkipButton: Boolean, buttonAction: () -> Unit) =
        updateToolbar(
            toolbarTitle = getString(com.blockchain.stringResources.R.string.security_check),
            menuItems = if (showSkipButton) {
                listOf(
                    NavigationBarButton.TextWithColorInt(
                        getString(com.blockchain.stringResources.R.string.common_skip),
                        com.blockchain.componentlib.R.color.primary,
                        buttonAction
                    )
                )
            } else {
                emptyList()
            }
        )

    override fun onEmailVerified() {
        model.process(LoaderIntents.LaunchDashboard(data = null, shouldLaunchUiTour = true))
    }

    override fun onEmailVerificationSkipped() {
        model.process(LoaderIntents.LaunchDashboard(data = null, shouldLaunchUiTour = true))
        analytics.logEvent(KYCAnalyticsEvents.EmailVeriffSkipped(LaunchOrigin.SIGN_UP))
    }

    override fun onDestroy() {
        compositeDisposable.clear()
        super.onDestroy()
    }

    private fun onRequestPin() {
        startActivity(
            PinActivity.newIntent(
                context = this,
                startForResult = false,
                originScreen = PinActivity.Companion.OriginScreenToPin.LOADER_SCREEN,
                addFlagsToClear = true,
                referralCode = referralCode
            )
        )
    }

    private val homeActivityLauncher: HomeActivityLauncher by inject()

    private fun onStartMainActivity(mainData: String?, shouldLaunchUiTour: Boolean) {
        startActivity(
            homeActivityLauncher.newIntent(
                context = this,
                intentData = mainData,
                shouldBeNewTask = true
            )
        )
        finish()
    }

    private fun launchEmailVerification() {
        binding.progress.gone()
        binding.contentFrame.visible()
        binding.toolbar.navigationToolbar.gone()
        analytics.logEvent(KYCAnalyticsEvents.EmailVeriffRequested(LaunchOrigin.SIGN_UP))
        if (supportFragmentManager.findFragmentById(R.id.content_frame) !is KycEmailVerificationFragment) {
            supportFragmentManager.beginTransaction()
                .replace(
                    R.id.content_frame,
                    KycEmailVerificationFragment.newInstance(canBeSkipped = true),
                    KycEmailVerificationFragment::class.simpleName
                )
                .commitAllowingStateLoss()
        }
    }

    private fun showMetadataNodeFailure() {
        if (!isFinishing) {
            AlertDialog.Builder(this, com.blockchain.componentlib.R.style.AlertDialogStyle)
                .setTitle(com.blockchain.stringResources.R.string.app_name)
                .setMessage(com.blockchain.stringResources.R.string.metadata_load_failure)
                .setPositiveButton(com.blockchain.stringResources.R.string.retry) { _, _ -> onRequestPin() }
                .setNegativeButton(com.blockchain.stringResources.R.string.exit) { _, _ -> finish() }
                .setCancelable(false)
                .setOnDismissListener {
                    model.process(LoaderIntents.HideMetadataNodeFailure)
                }
                .create()
                .show()
        }
    }

    private fun showSecondPasswordDialog() {
        val editText = AppCompatEditText(this)
        editText.setHint(com.blockchain.stringResources.R.string.password)
        editText.inputType =
            InputType.TYPE_CLASS_TEXT or
            InputType.TYPE_TEXT_VARIATION_PASSWORD or
            InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS

        val frameLayout = getAlertDialogPaddedView(editText)

        AlertDialog.Builder(this, com.blockchain.componentlib.R.style.AlertDialogStyle)
            .setTitle(com.blockchain.stringResources.R.string.second_password_dlg_title)
            .setMessage(com.blockchain.stringResources.R.string.eth_second_password_prompt)
            .setView(frameLayout)
            .setCancelable(false)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                hideKeyboard()
                model.process(LoaderIntents.DecryptAndSetupMetadata(editText.text.toString()))
            }
            .setOnDismissListener {
                model.process(LoaderIntents.HideSecondPasswordDialog)
            }
            .create()
            .show()
    }

    private fun updateProgressVisibility(show: Boolean) {
        binding.progress.visibleIf { show }
    }

    private fun updateProgressText(text: Int) {
        binding.progress.text = getString(text)
    }

    private fun startSingleActivity(clazz: Class<*>) {
        val intent = Intent(this, clazz).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
        finish()
    }

    companion object {
        private const val INTENT_EXTRA_VERIFIED = "verified"
    }
}
