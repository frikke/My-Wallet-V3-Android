package piuk.blockchain.android.ui.settings.profile.phone

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.text.input.KeyboardType
import com.blockchain.commonarch.presentation.base.updateTitleToolbar
import com.blockchain.commonarch.presentation.mvi.MviFragment
import com.blockchain.componentlib.alert.BlockchainSnackbar
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.controls.TextInputState
import com.blockchain.componentlib.viewextensions.visibleIf
import com.blockchain.presentation.koin.scopedInject
import com.mukesh.countrypicker.CountryPicker
import info.blockchain.wallet.api.data.Settings
import java.util.Locale
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.FragmentUpdatePhoneBinding
import piuk.blockchain.android.ui.settings.sheets.sms.SMSPhoneVerificationBottomSheet
import piuk.blockchain.android.util.FormatChecker

class UpdatePhoneFragment :
    MviFragment<PhoneModel, PhoneIntent, PhoneState, FragmentUpdatePhoneBinding>(),
    SMSPhoneVerificationBottomSheet.Host {

    private val formatChecker: FormatChecker by inject()

    override val model: PhoneModel by scopedInject()

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentUpdatePhoneBinding =
        FragmentUpdatePhoneBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        this.updateTitleToolbar(getString(com.blockchain.stringResources.R.string.profile_toolbar_mobile))
        binding.updatePhone.buttonState = ButtonState.Disabled
    }

    override fun onResume() {
        super.onResume()
        model.process(PhoneIntent.LoadProfile)
    }

    override fun render(newState: PhoneState) {
        if (!newState.isLoading) {
            newState.userInfoSettings?.let {
                init(it.mobileVerified, it.mobileNoPrefix, it.authType, it.mobileWithPrefix.orEmpty(), it.smsDialCode)
            }
        }

        if (newState.error != PhoneError.None) handleErrors(newState.error)

        if (newState.codeSent) {
            showDialogVerifySms(newState.userInfoSettings?.mobileWithPrefix.orEmpty())
            model.process(PhoneIntent.ResetCodeSentVerification)
        }
    }

    private fun init(
        mobileVerified: Boolean,
        mobileNoPrefix: String,
        authType: Int,
        mobileWithPrefix: String,
        smsDialCode: String
    ) {
        updateUI(
            mobileVerified = mobileVerified,
            mobileNoPrefix = mobileNoPrefix,
            authType = authType,
            mobileWithPrefix = mobileWithPrefix
        )
        if (binding.dialCodeValue.text.toString().isEmpty()) {
            setCountryListener(smsDialCode, mobileWithPrefix)
        }
    }

    private fun handleErrors(error: PhoneError) {
        BlockchainSnackbar.make(
            view = binding.root,
            message = getString(
                when (error) {
                    PhoneError.PhoneNumberNotValidError ->
                        com.blockchain.stringResources.R.string.profile_update_error_phone_invalid

                    PhoneError.ResendSmsError -> com.blockchain.stringResources.R.string.profile_update_error_resend_sms
                    else -> com.blockchain.stringResources.R.string.profile_update_error_phone
                }
            ),
            type = SnackbarType.Error
        ).show()

        model.process(PhoneIntent.ClearErrors)
    }

    private fun changeStateCta(newPhone: String, currentPhone: String) {
        val stateButton = if (newPhone == currentPhone) ButtonState.Disabled else ButtonState.Enabled
        val stateVerifyButton = if (newPhone == currentPhone) ButtonState.Enabled else ButtonState.Disabled
        binding.updatePhone.buttonState = stateButton
        binding.verifyPhoneBtn.buttonState = stateVerifyButton
    }

    private fun updateUI(mobileVerified: Boolean, mobileNoPrefix: String, authType: Int, mobileWithPrefix: String) {
        with(binding) {
            phone.apply {
                labelText = context.getString(com.blockchain.stringResources.R.string.profile_label_mobile)
                value = mobileNoPrefix
                inputType = KeyboardType.Number
                singleLine = true
                value = mobileNoPrefix
                onValueChange = {
                    changeStateCta(
                        binding.dialCodeValue.text.toString() + it,
                        mobileWithPrefix.filterNot { it.isWhitespace() }
                    )
                    value = it
                }
                trailingIconResource = ImageResource.None
            }

            changeStateCta(
                binding.dialCodeValue.text.toString() + binding.phone.value,
                mobileWithPrefix.filterNot { it.isWhitespace() }
            )

            verifyPhoneBtn.visibleIf { !mobileVerified }
            verifyPhoneBtn.apply {
                text = getString(com.blockchain.stringResources.R.string.profile_verify_phone)
                onClick = {
                    if (isValidMobileNumber(authType)) onVerifyPhoneClicked()
                }
            }

            updatePhone.apply {
                text = getString(com.blockchain.stringResources.R.string.profile_update)
                onClick = {
                    if (isValidMobileNumber(authType)) onUpdatePhoneClicked()
                }
            }
        }
    }

    private fun onVerifyPhoneClicked() {
        model.process(PhoneIntent.ResendCodeSMS)
    }

    private fun onUpdatePhoneClicked() {
        model.process(PhoneIntent.SavePhoneNumber(binding.dialCodeValue.text.toString() + binding.phone.value))
    }

    private fun isValidMobileNumber(authType: Int): Boolean = showWarningToDisable2fa(authType) && isValidMobile()

    private fun isValidMobile(): Boolean {
        val newPhoneNumber = binding.dialCodeValue.text.toString() + binding.phone.value
        return if (!formatChecker.isValidMobileNumber(newPhoneNumber)) {
            binding.phone.apply {
                state = TextInputState.Error(getString(com.blockchain.stringResources.R.string.invalid_mobile))
                trailingIconResource = ImageResource.Local(com.blockchain.componentlib.R.drawable.ic_alert, null)
            }
            false
        } else {
            binding.phone.apply {
                trailingIconResource = ImageResource.None
                state = TextInputState.Default()
            }
            true
        }
    }

    private fun showWarningToDisable2fa(authType: Int): Boolean {
        return if (authType != Settings.AUTH_TYPE_OFF) {
            binding.phone.apply {
                state =
                    TextInputState.Error(getString(com.blockchain.stringResources.R.string.profile_disable_2fa_first))
                trailingIconResource = ImageResource.Local(com.blockchain.componentlib.R.drawable.ic_alert, null)
            }
            false
        } else {
            binding.phone.apply {
                trailingIconResource = ImageResource.None
                state = TextInputState.Default()
            }
            true
        }
    }

    private fun setCountryListener(countryCode: String, mobileWithPrefix: String) {
        with(binding) {
            val picker = CountryPicker.Builder()
                .with(requireContext())
                .listener { country -> setCountryInfo(country.dialCode, mobileWithPrefix) }
                .theme(CountryPicker.THEME_NEW)
                .build()

            if (countryCode.isEmpty()) {
                val country = picker.countryFromSIM
                    ?: picker.getCountryByLocale(Locale.getDefault())
                    ?: picker.getCountryByISO("US")
                setCountryInfo(country.dialCode, mobileWithPrefix)
            } else {
                setCountryInfo(countryCode, mobileWithPrefix)
            }
            dialCode.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) picker.showBottomSheet(activity)
                true
            }
        }
    }

    private fun setCountryInfo(dialCode: String, mobileWithPrefix: String) {
        binding.dialCodeValue.text = dialCode
        changeStateCta(
            binding.dialCodeValue.text.filterNot { it.isWhitespace() }.toString() + binding.phone.value,
            mobileWithPrefix.filterNot { it.isWhitespace() }
        )
    }

    private fun showDialogVerifySms(mobileWithPrefix: String) {
        SMSPhoneVerificationBottomSheet.newInstance(mobileWithPrefix)
            .show(childFragmentManager, BOTTOM_SHEET)
    }

    companion object {
        fun newInstance() = UpdatePhoneFragment()
    }

    override fun onPhoneNumberVerified() {
        model.process(PhoneIntent.LoadProfile)
    }

    override fun onSheetClosed() {
        // Do nothing
    }
}
