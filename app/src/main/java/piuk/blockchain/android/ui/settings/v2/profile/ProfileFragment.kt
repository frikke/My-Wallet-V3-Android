package piuk.blockchain.android.ui.settings.v2.profile

import android.net.Uri
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.blockchain.api.services.WalletSettingsService
import com.blockchain.componentlib.image.ImageResource
import com.blockchain.componentlib.tag.TagType
import com.blockchain.componentlib.tag.TagViewState
import com.blockchain.koin.scopedInject
import com.blockchain.nabu.BasicProfileInfo
import com.blockchain.nabu.Tier
import com.blockchain.notifications.analytics.AnalyticsEvents
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.FragmentProfileBinding
import piuk.blockchain.android.ui.base.mvi.MviFragment
import piuk.blockchain.android.ui.home.ZendeskSubjectActivity
import piuk.blockchain.android.ui.settings.v2.RedesignSettingsPhase2Activity
import piuk.blockchain.android.urllinks.PRIVATE_KEY_EXPLANATION
import piuk.blockchain.android.urllinks.URL_BLOCKCHAIN_SUPPORT_PORTAL
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.android.util.calloutToExternalSupportLinkDlg
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.visible
import piuk.blockchain.android.util.visibleIf

class ProfileFragment : MviFragment<ProfileModel, ProfileIntent, ProfileState, FragmentProfileBinding>() {

    override val model: ProfileModel by scopedInject()

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentProfileBinding =
        FragmentProfileBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTierInfo()
        setContactSupport()
    }

    private val basicProfileInfo by lazy {
        arguments?.getSerializable(RedesignSettingsPhase2Activity.BASIC_INFO) as BasicProfileInfo
    }

    private val userTier by lazy {
        arguments?.getSerializable(RedesignSettingsPhase2Activity.USER_TIER) as Tier
    }

    override fun onResume() {
        super.onResume()
        model.process(ProfileIntent.LoadProfile)
    }

    private fun showLoading() {
        binding.progress.visible()
    }

    private fun hideLoading() {
        binding.progress.gone()
    }

    override fun render(newState: ProfileState) {
        if (newState.isLoading) {
            showLoading()
        } else {
            setProfileRowsInfo(basicProfileInfo, newState.userInfoSettings)
            hideLoading()
        }
    }

    private fun setupTierInfo() {
        with(binding) {
            if (userTier == Tier.BRONZE) {
                userInitials.apply {
                    background = ContextCompat.getDrawable(
                        context,
                        R.drawable.bkgd_profile_circle_empty
                    )
                }
            } else {
                userInitials.apply {
                    background = ContextCompat.getDrawable(
                        context,
                        R.drawable.bkgd_profile_circle
                    )
                    text = getString(
                        R.string.settings_initials,
                        basicProfileInfo.firstName.first().uppercase(),
                        basicProfileInfo.lastName.first().uppercase()
                    )
                }
            }
        }
    }

    private fun setProfileRowsInfo(
        basicProfileInfo: BasicProfileInfo?,
        userInfoSettings: WalletSettingsService.UserInfoSettings?
    ) {
        with(binding) {
            if (userTier != Tier.BRONZE) {
                nameRow.apply {
                    visible()
                    primaryText = getString(R.string.profile_label_name)
                    secondaryText = basicProfileInfo?.firstName.orEmpty() + " " + basicProfileInfo?.lastName.orEmpty()
                    endImageResource = ImageResource.None
                }
            }

            div2.visibleIf { userTier != Tier.BRONZE }
            contactSupport.visibleIf { userTier != Tier.BRONZE }

            emailRow.apply {
                primaryText = getString(R.string.profile_label_email)
                secondaryText = basicProfileInfo?.email.orEmpty()
                onClick = { Toast.makeText(context, "open email updater", Toast.LENGTH_LONG).show() }
            }

            mobileRow.apply {
                primaryText = getString(R.string.profile_label_mobile)
                secondaryText = if (userInfoSettings?.mobileWithPrefix.isNullOrEmpty()) {
                    getString(R.string.profile_mobile_empty)
                } else {
                    userInfoSettings?.mobileWithPrefix
                }
                onClick = { Toast.makeText(context, "open phone updater", Toast.LENGTH_LONG).show() }
            }

            updateTags(
                emailVerified = userInfoSettings?.emailVerified ?: false,
                mobileVerified = userInfoSettings?.mobileVerified ?: false
            )
        }
    }

    private fun updateTags(emailVerified: Boolean, mobileVerified: Boolean) {
        val typeEmailTag = if (emailVerified) TagType.Success() else TagType.Warning()
        val textEmailTag = if (emailVerified) getString(R.string.verified) else getString(R.string.not_verified)

        val typeMobileTag = if (mobileVerified) TagType.Success() else TagType.Warning()
        val textMobileTag = if (mobileVerified) getString(R.string.verified) else getString(R.string.not_verified)

        with(binding) {
            emailRow.tags = listOf(
                TagViewState(
                    value = textEmailTag,
                    type = typeEmailTag
                )
            )
            mobileRow.tags = listOf(
                TagViewState(
                    value = textMobileTag,
                    type = typeMobileTag
                )
            )
        }
    }

    private fun setContactSupport() {
        val map = mapOf("contact_support" to Uri.parse(PRIVATE_KEY_EXPLANATION))
        val contactSupportText = StringUtils.getStringWithMappedAnnotations(
            requireContext(),
            R.string.profile_label_support,
            map
        ) { onSupportClicked() }
        binding.contactSupport.apply {
            text = contactSupportText
            movementMethod = LinkMovementMethod.getInstance()
        }
    }

    private fun onSupportClicked() {
        if (userTier == Tier.GOLD) {
            analytics.logEvent(AnalyticsEvents.Support)
            startActivity(
                ZendeskSubjectActivity.newInstance(
                    context = requireContext(),
                    userInfo = basicProfileInfo,
                    subject = CHANGE_NAME_SUPPORT
                )
            )
        } else {
            calloutToExternalSupportLinkDlg(requireContext(), URL_BLOCKCHAIN_SUPPORT_PORTAL)
        }
    }
    // TODO logic to ve moved to new screens, following tickets AND-5624 and AND-5625

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
    //
    //
    //    private fun onVerifyEmailClicked() {
    //        model.process(ProfileIntent.SaveAndSendEmail(binding.email.value))
    //    }
    //
    //    private fun checkEmail() {
    //        Intent(Intent.ACTION_MAIN).apply {
    //            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    //            addCategory(Intent.CATEGORY_APP_EMAIL)
    //            startActivity(Intent.createChooser(this, getString(R.string.security_centre_email_check)))
    //        }
    //    }
    //
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
    //            val showEmailWarning = userInfoSettings?.emailVerified == false && !isEditable
    //            verifyEmailMsg.visibleIf { showEmailWarning }
    //            verifyEmailBtn.apply {
    //                visibleIf { showEmailWarning }
    //                text = getString(R.string.profile_verify_email)
    //                onClick = { onVerifyEmailClicked() }
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
    //        return showWarningToDisable2fa(authType) && isValidEmailAddress() && isValidMobile()
    //    }
    //
    //    private fun isValidEmailAddress(): Boolean {
    //        val newEmailAddress = binding.email.value
    //        return if (!formatChecker.isValidEmailAddress(newEmailAddress)) {
    //            binding.email.apply {
    //                state = TextInputState.Error(getString(R.string.invalid_email))
    //                trailingIconResource = ImageResource.Local(R.drawable.ic_alert, null)
    //            }
    //            false
    //        } else {
    //            binding.email.apply {
    //                trailingIconResource = ImageResource.None
    //                state = TextInputState.Default()
    //            }
    //            true
    //        }
    //    }
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
    //
    //    private fun showDialogEmailVerification() {
    //        AlertDialog.Builder(this, R.style.AlertDialogStyle)
    //            .setTitle(R.string.verify)
    //            .setMessage(R.string.verify_email_notice)
    //            .setCancelable(true)
    //            .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.dismiss() }
    //            .setPositiveButton(android.R.string.ok) { _, _ -> checkEmail() }
    //            .show()
    //    }
    //
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

    companion object {
        private const val CHANGE_NAME_SUPPORT = "Update name and surname"

        fun newInstance(basicProfileInfo: BasicProfileInfo, tier: Tier) =
            ProfileFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(RedesignSettingsPhase2Activity.BASIC_INFO, basicProfileInfo)
                    putSerializable(RedesignSettingsPhase2Activity.USER_TIER, tier)
                }
            }
    }
}
