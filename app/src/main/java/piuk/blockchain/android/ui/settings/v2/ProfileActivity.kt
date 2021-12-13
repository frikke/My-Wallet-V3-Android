package piuk.blockchain.android.ui.settings.v2

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.blockchain.componentlib.controls.TextInputState
import com.blockchain.koin.scopedInject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ActivityProfileBinding
import piuk.blockchain.android.databinding.ToolbarGeneralBinding
import piuk.blockchain.android.ui.base.mvi.MviActivity
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

    override val toolbarBinding: ToolbarGeneralBinding?
        get() = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setupToolbar()
    }

    override fun render(newState: ProfileState) {
        if (newState.profileViewToLaunch is ProfileViewToLaunch.View) {
            setViewMode()
        } else {
            setupEditMode()
        }

        // TODO ask what to show in case of error
        if (newState.hasFailed) {
            Toast.makeText(this, "There was something wrong", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupToolbar() {
        binding.toolbar.apply {
            title = context.getString(R.string.settings_profile_toolbar)
            onBackButtonClick = { onBackPressed() }
        }
    }

    private fun setupEditMode() {
        enableTextInputs(true)
        with(binding) {
            editProfile.gone()
            splitButtons.visible()
            splitButtons.apply {
                primaryButtonText = getString(R.string.common_cancel)
                onPrimaryButtonClick = {
                    model.process(
                        ProfileIntent.UpdateProfileView(
                            profileViewToLaunch = ProfileViewToLaunch.View
                        )
                    )
                }
                secondaryButtonText = getString(R.string.common_save)
                onSecondaryButtonClick = {
                    model.process(
                        ProfileIntent.UpdateProfile(
                            email = binding.email.toString(),
                            phoneNumber = binding.phone.toString()
                        )
                    )
                }
            }
        }
    }

    private fun setViewMode() {
        enableTextInputs(false)
        with(binding) {
            splitButtons.gone()
            editProfile.apply {
                visible()
                text = getString(R.string.edit)
                onClick = {
                    model.process(
                        ProfileIntent.UpdateProfileView(
                            profileViewToLaunch = ProfileViewToLaunch.Edit
                        )
                    )
                }
            }
        }
    }

    private fun enableTextInputs(isEditable: Boolean) {
        val inputState = if (isEditable) TextInputState.Default() else TextInputState.Disabled()
        with(binding) {
            name.apply {
                labelText = context.getString(R.string.profile_label_name)
                value = "Paco"
                state = inputState
            }
            surname.apply {
                labelText = context.getString(R.string.profile_label_surname)
                value = "Martinez"
                state = inputState
            }
            email.apply {
                labelText = context.getString(R.string.profile_label_email)
                value = "paco@gmail.com"
                state = inputState
            }
            phone.apply {
                labelText = context.getString(R.string.profile_label_mobile)
                value = "0775182932"
                state = inputState
            }
        }
    }

    override fun onDestroy() {
        model.clearDisposables()
        super.onDestroy()
    }

    companion object {
        fun newIntent(context: Context): Intent =
            Intent(context, ProfileActivity::class.java)
    }
}
