package piuk.blockchain.android.ui.settings.v2.profile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.blockchain.api.services.WalletSettingsService
import com.blockchain.componentlib.controls.TextInputState
import com.blockchain.componentlib.tag.TagType
import com.blockchain.componentlib.tag.TagViewState
import com.blockchain.koin.scopedInject
import com.blockchain.nabu.BasicProfileInfo
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ActivityProfileBinding
import piuk.blockchain.android.databinding.ToolbarGeneralBinding
import piuk.blockchain.android.ui.base.mvi.MviActivity
import piuk.blockchain.android.ui.settings.v2.RedesignSettingsPhase2Activity.Companion.BASIC_INFO
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.visible

class ProfileActivity :
    MviActivity<ProfileModel,
        ProfileIntent,
        ProfileState,
        ActivityProfileBinding>() {

    override val model: ProfileModel by scopedInject()

    override fun initBinding(): ActivityProfileBinding =
        ActivityProfileBinding.inflate(layoutInflater)

    override val alwaysDisableScreenshots: Boolean = true

    override val toolbarBinding: ToolbarGeneralBinding
        get() = binding.toolbar

    private val basicProfileInfo by lazy {
        intent.getSerializableExtra(BASIC_INFO) as BasicProfileInfo
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setupToolbar()
    }

    override fun onResume() {
        super.onResume()
        model.process(ProfileIntent.LoadProfile)
    }

    override fun render(newState: ProfileState) {
        if (newState.profileViewState is ProfileViewState.View) {
            setViewMode(basicProfileInfo, newState.userInfoSettings)
        } else {
            setupEditMode(basicProfileInfo, newState.userInfoSettings)
        }

        // TODO ask what to show in case of error
        if (newState.savingHasFailed) {
            Toast.makeText(this, "Something went wrong saving user information", Toast.LENGTH_SHORT).show()
        } else {
            updateTags(
                emailVerified = newState.userInfoSettings?.emailVerified ?: false,
                mobileVerified = newState.userInfoSettings?.mobileVerified ?: false
            )
        }
        if (newState.loadingHasFailed) {
            Toast.makeText(this, "Something went wrong loading user information", Toast.LENGTH_SHORT).show()
        } else {
            updateTags(
                emailVerified = newState.userInfoSettings?.emailVerified ?: false,
                mobileVerified = newState.userInfoSettings?.mobileVerified ?: false
            )
        }
    }

    private fun setupToolbar() {
        updateToolbar(
            toolbarTitle = getString(R.string.profile_toolbar),
            backAction = { onBackPressed() }
        )
    }

    private fun setupEditMode(
        basicProfileInfo: BasicProfileInfo?,
        userInfoSettings: WalletSettingsService.UserInfoSettings?
    ) {
        enableTextInputs(isEditable = true, basicProfileInfo = basicProfileInfo, userInfoSettings = userInfoSettings)
        with(binding) {
            editProfile.gone()
            splitButtons.visible()
            splitButtons.apply {
                primaryButtonText = getString(R.string.common_cancel)
                onPrimaryButtonClick = {
                    model.process(
                        ProfileIntent.UpdateProfileView(
                            profileViewToLaunch = ProfileViewState.View
                        )
                    )
                }
                secondaryButtonText = getString(R.string.common_save)
                onSecondaryButtonClick = {
                    model.process(
                        ProfileIntent.UpdateProfile(
                            WalletSettingsService.UserInfoSettings(
                                email = binding.email.toString(),
                                emailVerified = userInfoSettings?.email == binding.email.toString(),
                                mobile = binding.phone.toString(),
                                mobileVerified = userInfoSettings?.mobile == binding.phone.toString()
                            )
                        )
                    )
                }
            }
        }
    }

    private fun updateTags(emailVerified: Boolean, mobileVerified: Boolean) {
        val typeEmailTag = if (emailVerified) TagType.Success else TagType.Warning
        val textEmailTag = if (emailVerified) getString(R.string.verified) else getString(R.string.unverified)

        val typeMobileTag = if (mobileVerified) TagType.Success else TagType.Warning
        val textMobileTag = if (mobileVerified) getString(R.string.verified) else getString(R.string.unverified)

        with(binding) {
            tagEmail.tags = listOf(
                TagViewState(
                    value = textEmailTag,
                    type = typeEmailTag
                )
            )
            tagMobile.tags = listOf(
                TagViewState(
                    value = textMobileTag,
                    type = typeMobileTag
                )
            )
        }
    }

    private fun setViewMode(
        basicProfileInfo: BasicProfileInfo?,
        userInfoSettings: WalletSettingsService.UserInfoSettings?
    ) {
        enableTextInputs(isEditable = false, basicProfileInfo = basicProfileInfo, userInfoSettings = userInfoSettings)
        with(binding) {
            splitButtons.gone()
            editProfile.apply {
                visible()
                text = getString(R.string.edit)
                onClick = {
                    model.process(
                        ProfileIntent.UpdateProfileView(
                            profileViewToLaunch = ProfileViewState.Edit
                        )
                    )
                }
            }
        }
    }

    private fun enableTextInputs(
        isEditable: Boolean,
        basicProfileInfo: BasicProfileInfo?,
        userInfoSettings: WalletSettingsService.UserInfoSettings?
    ) {
        val inputState = if (isEditable) TextInputState.Default() else TextInputState.Disabled()
        with(binding) {
            if (basicProfileInfo != null) {
                name.apply {
                    labelText = context.getString(R.string.profile_label_name)
                    value = basicProfileInfo.firstName
                    state = TextInputState.Disabled()
                }
                surname.apply {
                    labelText = context.getString(R.string.profile_label_surname)
                    value = basicProfileInfo.lastName
                    state = TextInputState.Disabled()
                }
            }
            if (userInfoSettings != null) {
                email.apply {
                    labelText = context.getString(R.string.profile_label_email)
                    value = userInfoSettings.email.orEmpty()
                    state = inputState
                }

                phone.apply {
                    labelText = context.getString(R.string.profile_label_mobile)
                    value = userInfoSettings.mobile.orEmpty()
                    state = inputState
                }
            }
        }
    }

    companion object {
        fun newIntent(context: Context, basicProfileInfo: BasicProfileInfo?) =
            Intent(context, ProfileActivity::class.java).apply {
                putExtra(BASIC_INFO, basicProfileInfo)
            }
    }
}
