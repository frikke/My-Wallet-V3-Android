package piuk.blockchain.android.ui.createwallet

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.blockchain.analytics.events.AnalyticsEvents
import com.blockchain.commonarch.presentation.base.HostedBottomSheet
import com.blockchain.commonarch.presentation.mvi_v2.MVIActivity
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.NavigationRouter
import com.blockchain.commonarch.presentation.mvi_v2.bindViewModel
import com.blockchain.componentlib.alert.BlockchainSnackbar
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.componentlib.databinding.ToolbarGeneralBinding
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.hideKeyboard
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.koin.payloadScope
import com.blockchain.koin.referralsFeatureFlag
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.android.scope.AndroidScopeComponent
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.scope.Scope
import piuk.blockchain.android.R
import piuk.blockchain.android.cards.CountryPickerItem
import piuk.blockchain.android.cards.PickerItem
import piuk.blockchain.android.cards.PickerItemListener
import piuk.blockchain.android.cards.SearchPickerItemBottomSheet
import piuk.blockchain.android.cards.StatePickerItem
import piuk.blockchain.android.databinding.ActivityCreateWalletBinding
import piuk.blockchain.android.ui.customviews.CircularProgressDrawable
import piuk.blockchain.android.ui.settings.v2.security.pin.PinActivity
import piuk.blockchain.android.urllinks.URL_BACKUP_INFO
import piuk.blockchain.android.urllinks.URL_PRIVACY_POLICY
import piuk.blockchain.android.urllinks.URL_TOS_POLICY
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidcore.utils.helperfunctions.consume

class CreateWalletActivity :
    MVIActivity<CreateWalletViewState>(),
    NavigationRouter<CreateWalletNavigation>,
    PickerItemListener,
    HostedBottomSheet.Host,
    AndroidScopeComponent {

    override val scope: Scope = payloadScope
    override val alwaysDisableScreenshots: Boolean = true

    private val binding: ActivityCreateWalletBinding by lazy {
        ActivityCreateWalletBinding.inflate(layoutInflater)
    }

    override val toolbarBinding: ToolbarGeneralBinding?
        get() = binding.toolbar

    private val viewModel: CreateWalletViewModel by viewModel()

    private val referralFF: FeatureFlag by inject(referralsFeatureFlag)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        bindViewModel(viewModel, this, ModelConfigArgs.NoArgs)

        updateToolbar(
            toolbarTitle = getString(R.string.new_account_title_1),
            backAction = { onBackPressedDispatcher.onBackPressed() }
        )

        with(binding) {
            emailAddress.setOnClickListener { analytics.logEventOnce(AnalyticsEvents.WalletSignupClickEmail) }
            emailAddress.doAfterTextChanged {
                viewModel.onIntent(CreateWalletIntent.EmailInputChanged(it?.toString().orEmpty()))
            }
            walletPass.doAfterTextChanged {
                entropyContainerNew.visible()
                analytics.logEventOnce(AnalyticsEvents.WalletSignupClickPasswordFirst)
                binding.entropyContainerNew.updatePassword(it.toString())
                viewModel.onIntent(CreateWalletIntent.PasswordInputChanged(it?.toString().orEmpty()))
            }
            walletPassConfirm.doAfterTextChanged {
                analytics.logEventOnce(AnalyticsEvents.WalletSignupClickPasswordSecond)
                viewModel.onIntent(CreateWalletIntent.PasswordConfirmationInputChanged(it?.toString().orEmpty()))
            }
            walletTermsCheckbox.setOnCheckedChangeListener { _, isChecked ->
                viewModel.onIntent(CreateWalletIntent.TermsOfServiceStateChanged(isChecked))
            }

            lifecycleScope.launch {
                if (!referralFF.coEnabled()) return@launch
                referralLayout.visible()
                referralCode.doAfterTextChanged {
                    viewModel.onIntent(CreateWalletIntent.ReferralInputChanged(it?.toString().orEmpty()))
                }
            }

            commandNext.setOnClickListener {
                viewModel.onIntent(CreateWalletIntent.NextClicked)
            }

            updatePasswordDisclaimer()

            walletPassConfirm.setOnEditorActionListener { _, i, _ ->
                consume {
                    if (i != EditorInfo.IME_ACTION_NEXT) return@consume
                    hideKeyboard()
                    val countryInputState = viewState?.countryInputState
                    if (countryInputState is CountryInputState.Loaded) {
                        openCountryPicker(countryInputState)
                    }
                }
            }
        }
    }

    override fun onItemPicked(item: PickerItem) {
        when (item) {
            is CountryPickerItem -> {
                viewModel.onIntent(CreateWalletIntent.CountryInputChanged(item.code))
                WalletCreationAnalytics.CountrySelectedOnSignUp(item.code)
            }
            is StatePickerItem -> {
                viewModel.onIntent(CreateWalletIntent.StateInputChanged(item.code))
                WalletCreationAnalytics.StateSelectedOnSignUp(item.code)
            }
        }
        hideKeyboard()
    }

    private var viewState: CreateWalletViewState? = null
    override fun onStateUpdated(state: CreateWalletViewState) = with(binding) {
        viewState = state
        if (emailAddress.text.toString() != state.emailInput) emailAddress.setText(state.emailInput)
        if (walletPass.text.toString() != state.passwordInput) walletPass.setText(state.passwordInput)
        if (walletPassConfirm.text.toString() != state.passwordConfirmationInput)
            walletPassConfirm.setText(state.passwordConfirmationInput)

        when (state.countryInputState) {
            CountryInputState.Loading -> {
                country.setOnClickListener(null)
                country.text = null
                selectCountry.endIconDrawable = CircularProgressDrawable(this@CreateWalletActivity)
            }
            is CountryInputState.Loaded -> {
                country.setOnClickListener {
                    openCountryPicker(state.countryInputState)
                }
                country.setText(state.countryInputState.selected?.name)
                selectCountry.endIconDrawable =
                    ContextCompat.getDrawable(this@CreateWalletActivity, R.drawable.ic_arrow_down)
            }
        }

        when (state.stateInputState) {
            StateInputState.Hidden -> selectState.gone()
            StateInputState.Loading -> {
                selectState.visible()
                binding.state.text = null
                selectState.endIconDrawable = CircularProgressDrawable(this@CreateWalletActivity)
            }
            is StateInputState.Loaded -> {
                selectState.visible()
                selectState.endIconDrawable =
                    ContextCompat.getDrawable(this@CreateWalletActivity, R.drawable.ic_arrow_down)
                binding.state.setOnClickListener {
                    showBottomSheet(
                        SearchPickerItemBottomSheet.newInstance(
                            state.stateInputState.states.map { state ->
                                StatePickerItem(state.stateCode, state.name)
                            }
                        )
                    )
                }
                binding.state.setText(state.stateInputState.selected?.name)
            }
        }

        if (walletTermsCheckbox.isChecked != state.areTermsOfServiceChecked) {
            walletTermsCheckbox.isChecked = state.areTermsOfServiceChecked
        }

        if (referralCode.text.toString() != state.referralCodeInput) {
            referralCode.setText(state.referralCodeInput)
        }
        referralLayout.error = if (state.isInvalidReferralErrorShowing) {
            getString(R.string.new_account_referral_code_invalid)
        } else {
            null
        }

        if (state.isCreateWalletLoading) {
            showProgressDialog(R.string.creating_wallet)
        } else {
            dismissProgressDialog()
        }
        commandNext.isEnabled = state.isNextEnabled

        if (state.error != null) {
            when (state.error) {
                CreateWalletError.InvalidEmail,
                CreateWalletError.InvalidPasswordTooLong,
                CreateWalletError.InvalidPasswordTooShort,
                CreateWalletError.PasswordsMismatch,
                is CreateWalletError.Unknown,
                CreateWalletError.WalletCreationFailed -> {
                    BlockchainSnackbar.make(
                        binding.root,
                        state.error.errorMessage(),
                        type = SnackbarType.Error
                    ).show()
                }
                CreateWalletError.InvalidPasswordTooWeak -> {
                    AlertDialog.Builder(this@CreateWalletActivity, R.style.AlertDialogStyle)
                        .setTitle(R.string.app_name)
                        .setMessage(state.error.errorMessage())
                        .setPositiveButton(R.string.common_retry) { _, _ ->
                            binding.walletPass.requestFocus()
                        }.show()
                }
            }
            viewModel.onIntent(CreateWalletIntent.ErrorHandled)
        }
    }

    override fun route(navigationEvent: CreateWalletNavigation) {
        when (navigationEvent) {
            is CreateWalletNavigation.PinEntry -> {
                hideKeyboard()
                startActivity(
                    PinActivity.newIntent(
                        context = this,
                        startForResult = false,
                        originScreen = PinActivity.Companion.OriginScreenToPin.CREATE_WALLET,
                        addFlagsToClear = true,
                        referralCode = navigationEvent.referralCode
                    )
                )
            }
        }
    }

    private fun openCountryPicker(countryInputState: CountryInputState.Loaded) {
        showBottomSheet(
            SearchPickerItemBottomSheet.newInstance(
                countryInputState.countries.map { country ->
                    CountryPickerItem(country.countryCode)
                }
            )
        )
    }

    private fun updatePasswordDisclaimer() {
        val linksMap = mapOf<String, Uri>(
            "backup" to Uri.parse(URL_BACKUP_INFO),
            "terms" to Uri.parse(URL_TOS_POLICY),
            "privacy" to Uri.parse(URL_PRIVACY_POLICY)
        )

        val disclaimerText = StringUtils.getStringWithMappedAnnotations(
            this,
            R.string.password_disclaimer_1,
            linksMap
        )

        binding.walletTermsBlurb.apply {
            text = disclaimerText
            movementMethod = LinkMovementMethod.getInstance()
        }
    }

    override fun onSheetClosed() {
        // no op
        hideKeyboard()
    }

    private fun CreateWalletError.errorMessage(): String = when (this) {
        CreateWalletError.InvalidEmail -> getString(R.string.invalid_email)
        CreateWalletError.InvalidPasswordTooLong -> getString(R.string.invalid_password)
        CreateWalletError.InvalidPasswordTooShort -> getString(R.string.invalid_password_too_short)
        CreateWalletError.InvalidPasswordTooWeak -> getString(R.string.weak_password)
        CreateWalletError.PasswordsMismatch -> getString(R.string.password_mismatch_error)
        CreateWalletError.WalletCreationFailed -> getString(R.string.hd_error)
        is CreateWalletError.Unknown -> this.message ?: getString(R.string.something_went_wrong_try_again)
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, CreateWalletActivity::class.java))
        }
    }
}
