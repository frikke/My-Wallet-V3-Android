package piuk.blockchain.android.ui.settings.v2.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.blockchain.koin.scopedInject
import piuk.blockchain.android.databinding.FragmentUpdatePhoneBinding
import piuk.blockchain.android.ui.base.mvi.MviFragment

// TODO AND-5625
class UpdatePhoneFragment : MviFragment<ProfileModel, ProfileIntent, ProfileState, FragmentUpdatePhoneBinding>() {

    override val model: ProfileModel by scopedInject()

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentUpdatePhoneBinding =
        FragmentUpdatePhoneBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun render(newState: ProfileState) {
    }

    // TODO logic to ve moved to new screens, following ticket AND-5625

    //    private fun setupEditMode(
    //        basicProfileInfo: BasicProfileInfo?,
    //        userInfoSettings: WalletSettingsService.UserInfoSettings?
    //    ) {
    //        enableTextInputs(isEditable = true, basicProfileInfo = basicProfileInfo, userInfoSettings = userInfoSettings)
    //        with(binding) {
    //            editProfile.gone()
    //            contactSupport.visible()
    //            splitButtons.visible()
    //            splitButtons.apply {
    //                primaryButtonText = getString(R.string.common_cancel)
    //                onPrimaryButtonClick = {
    //                    model.process(
    //                        ProfileIntent.UpdateProfileView(
    //                            profileViewToLaunch = ProfileViewState.View
    //                        )
    //                    )
    //                }
    //                secondaryButtonText = getString(R.string.common_save)
    //                onSecondaryButtonClick = {
    //                    if (areValidUserInputs(authType = userInfoSettings?.authType ?: Settings.AUTH_TYPE_OFF)
    //                    ) {
    //                        model.process(
    //                            ProfileIntent.SaveProfile(
    //                                WalletSettingsService.UserInfoSettings(
    //                                    email = binding.email.value,
    //                                    mobileWithPrefix = binding.dialCode.value + binding.phone.value
    //                                )
    //                            )
    //                        )
    //                    }
    //                }
    //            }
    //        }
    //    }
    //
    //    private fun setViewMode(
    //        basicProfileInfo: BasicProfileInfo?,
    //        userInfoSettings: WalletSettingsService.UserInfoSettings?
    //    ) {
    //        enableTextInputs(isEditable = false, basicProfileInfo = basicProfileInfo, userInfoSettings = userInfoSettings)
    //        with(binding) {
    //            splitButtons.gone()
    //            contactSupport.gone()
    //            editProfile.apply {
    //                visible()
    //                text = getString(R.string.edit)
    //                onClick = {
    //                    model.process(
    //                        ProfileIntent.UpdateProfileView(
    //                            profileViewToLaunch = ProfileViewState.Edit
    //                        )
    //                    )
    //                }
    //            }
    //        }
    //    }
    //    private fun onVerifyPhoneClicked() {
    //        model.process(ProfileIntent.SaveAndSendSMS(binding.dialCode.value + binding.phone.value))
    //    }
    //
    //    private fun enableTextInputs(
    //        isEditable: Boolean,
    //        basicProfileInfo: BasicProfileInfo?,
    //        userInfoSettings: WalletSettingsService.UserInfoSettings?
    //    ) {
    //        val inputState = if (isEditable) TextInputState.Default() else TextInputState.Disabled()
    //        with(binding) {
    //            if (userTier != Tier.BRONZE) {
    //                name.apply {
    //                    labelText = context.getString(R.string.profile_label_name)
    //                    value = basicProfileInfo?.firstName.orEmpty()
    //                    state = TextInputState.Disabled()
    //                }
    //                surname.apply {
    //                    labelText = context.getString(R.string.profile_label_surname)
    //                    value = basicProfileInfo?.lastName.orEmpty()
    //                    state = TextInputState.Disabled()
    //                }
    //            }
    //            email.apply {
    //                labelText = context.getString(R.string.profile_label_email)
    //                singleLine = true
    //                inputType = KeyboardType.Text
    //                value = userInfoSettings?.email.orEmpty()
    //                state = inputState
    //                onValueChange = { value = it }
    //                trailingIconResource = ImageResource.None
    //            }
    //
    //            phone.apply {
    //                labelText = context.getString(R.string.profile_label_mobile)
    //                value = userInfoSettings?.mobileNoPrefix.orEmpty()
    //                state = inputState
    //                inputType = KeyboardType.Number
    //                singleLine = true
    //                onValueChange = { value = it }
    //                trailingIconResource = ImageResource.None
    //            }
    //
    //            dialCode.apply {
    //                labelText = context.getString(R.string.profile_label_dial_code)
    //                singleLine = true
    //                value = userInfoSettings?.dialCode.orEmpty()
    //                state = TextInputState.Disabled()
    //                trailingIconResource = ImageResource.None
    //            }
    //
    //            val showPhoneWarning = userInfoSettings?.mobileVerified == false && !isEditable
    //            verifyPhoneMsg.visibleIf { showPhoneWarning }
    //            verifyPhoneBtn.apply {
    //                visibleIf { showPhoneWarning }
    //                text = getString(R.string.profile_verify_phone)
    //                onClick = { onVerifyPhoneClicked() }
    //            }
    //
    //            setCountryListener(isEditable)
    //        }
    //    }
    //
    //    private fun setCountryListener(isEditable: Boolean) {
    //        with(binding) {
    //            val picker = CountryPicker.Builder()
    //                .with(this@ProfileActivity)
    //                .listener { country -> setCountryInfo(country.dialCode, country.flag) }
    //                .theme(CountryPicker.THEME_NEW)
    //                .build()
    //
    //            val country = picker.countryFromSIM
    //                ?: picker.getCountryByLocale(Locale.getDefault())
    //                ?: picker.getCountryByISO("US")
    //
    //            setCountryInfo(country.dialCode, country.flag)
    //
    //            if (isEditable) {
    //                openCountryPicker.setOnClickListener {
    //                    picker.showBottomSheet(this@ProfileActivity)
    //                }
    //            } else {
    //                openCountryPicker.setOnClickListener(null)
    //            }
    //        }
    //    }
    //
    //    private fun areValidUserInputs(authType: Int): Boolean {
    //        return showWarningToDisable2fa(authType) && isValidMobile()
    //    }
    //
    //
    //    private fun isValidMobile(): Boolean {
    //        val newPhoneNumber = binding.dialCode.value + binding.phone.value
    //        return if (!formatChecker.isValidMobileNumber(newPhoneNumber)) {
    //            binding.phone.apply {
    //                state = TextInputState.Error(getString(R.string.invalid_mobile))
    //                trailingIconResource = ImageResource.Local(R.drawable.ic_alert, null)
    //            }
    //            false
    //        } else {
    //            binding.phone.apply {
    //                trailingIconResource = ImageResource.None
    //                state = TextInputState.Default()
    //            }
    //            true
    //        }
    //    }
    //
    //    private fun showWarningToDisable2fa(authType: Int): Boolean {
    //        return if (authType != Settings.AUTH_TYPE_OFF) {
    //            binding.phone.apply {
    //                state = TextInputState.Error(getString(R.string.profile_disable_2fa_first))
    //                trailingIconResource = ImageResource.Local(R.drawable.ic_alert, null)
    //            }
    //            false
    //        } else {
    //            binding.phone.apply {
    //                trailingIconResource = ImageResource.None
    //                state = TextInputState.Default()
    //            }
    //            true
    //        }
    //    }
    //
    //    private fun setCountryInfo(dialCode: String, flagResourceId: Int) {
    //        val drawable = ContextCompat.getDrawable(this, flagResourceId)
    //        binding.flag.background = drawable
    //        binding.dialCode.value = dialCode
    //    }
    //    private fun showDialogVerifySms() {
    //        val editText = AppCompatEditText(this@ProfileActivity)
    //        editText.isSingleLine = true
    //
    //        val dialog = AlertDialog.Builder(this@ProfileActivity, R.style.AlertDialogStyle)
    //            .setTitle(R.string.verify_mobile)
    //            .setMessage(R.string.verify_sms_summary)
    //            .setView(ViewUtils.getAlertDialogPaddedView(this@ProfileActivity, editText))
    //            .setCancelable(false)
    //            .setPositiveButton(R.string.verify, null)
    //            .setNegativeButton(android.R.string.cancel, null)
    //            .setNeutralButton(R.string.resend) { _, _ ->
    //                model.process(ProfileIntent.SaveAndSendSMS(binding.dialCode.value + binding.phone.value))
    //            }
    //            .create()
    //
    //        dialog.setOnShowListener {
    //            val positive = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
    //            positive.setOnClickListener {
    //                val code = editText.text.toString()
    //                if (code.isNotEmpty()) {
    //                    model.process(ProfileIntent.VerifyPhoneNumber(code))
    //                    dialog.dismiss()
    //                    ViewUtils.hideKeyboard(this@ProfileActivity)
    //                }
    //            }
    //        }
    //        dialog.show()
    //    }
}
