package piuk.blockchain.android.ui.settings.security.password

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import com.blockchain.commonarch.presentation.base.updateToolbar
import com.blockchain.commonarch.presentation.mvi.MviFragment
import com.blockchain.componentlib.alert.BlockchainSnackbar
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.componentlib.viewextensions.visibleIf
import com.blockchain.presentation.koin.scopedInject
import com.blockchain.stringResources.R
import piuk.blockchain.android.databinding.FragmentPasswordUpdateBinding

class PasswordChangeFragment :
    MviFragment<PasswordChangeModel, PasswordChangeIntent, PasswordChangeState, FragmentPasswordUpdateBinding>() {
    override val model: PasswordChangeModel by scopedInject()

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentPasswordUpdateBinding =
        FragmentPasswordUpdateBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateToolbar(
            toolbarTitle = getString(R.string.change_password_title),
            menuItems = emptyList()
        )

        with(binding) {
            passwordBlurb.apply {
                text = getString(R.string.password_change_blurb)
                style = ComposeTypographies.Paragraph1
                textColor = ComposeColors.Body
            }
            passwordCta.apply {
                buttonState = ButtonState.Disabled
                text = getString(R.string.password_change_cta)
                onClick = {
                    model.process(
                        PasswordChangeIntent.UpdatePassword(
                            currentPassword = passwordCurrentInput.value,
                            newPassword = passwordNewInput.value,
                            newPasswordConfirmation = passwordConfirmInput.value
                        )
                    )
                }
            }

            passwordCurrentInput.apply {
                labelText = getString(R.string.password_current_hint)
            }

            passwordNewInput.apply {
                labelText = getString(R.string.password_new_hint)

                onValueChange = { text ->
                    passwordStrength.visibleIf { text.isNotEmpty() }
                    val strength = passwordStrength.updatePassword(text)
                    passwordCta.buttonState =
                        if (strength >= 51 && passwordCurrentInput.value.isNotEmpty() &&
                            passwordConfirmInput.value.isNotEmpty()
                        ) {
                            ButtonState.Enabled
                        } else {
                            ButtonState.Disabled
                        }
                }
            }

            passwordConfirmInput.apply {
                labelText = getString(R.string.password_confirm_hint)

                onValueChange = { text ->
                    passwordCta.buttonState =
                        if (passwordCurrentInput.value.isNotEmpty() &&
                            passwordNewInput.value.isNotEmpty()
                        ) {
                            ButtonState.Enabled
                        } else {
                            ButtonState.Disabled
                        }
                }
            }
        }
    }

    override fun render(newState: PasswordChangeState) {
        if (newState.passwordViewState != PasswordViewState.None) {
            when (newState.passwordViewState) {
                PasswordViewState.CheckingPasswords -> {
                    binding.passwordProgress.visible()
                }

                PasswordViewState.PasswordUpdated -> {
                    showSnackBar(
                        R.string.change_password_success,
                        type = SnackbarType.Success
                    )
                    activity.onBackPressedDispatcher.onBackPressed()
                    with(binding) {
                        passwordCurrentInput.value = ""
                        passwordNewInput.value = ""
                        passwordConfirmInput.value = ""
                        passwordProgress.gone()
                    }
                }

                PasswordViewState.None -> {
                    // do nothing
                }
            }
            model.process(PasswordChangeIntent.ResetViewState)
        }

        if (newState.errorState != PasswordChangeError.NONE) {
            binding.passwordProgress.gone()
            processError(newState.errorState)
        }
    }

    private fun processError(errorState: PasswordChangeError) {
        when (errorState) {
            PasswordChangeError.USING_SAME_PASSWORDS -> {
                showSnackBar(
                    R.string.change_password_error_old_new_match,
                    type = SnackbarType.Error
                )
            }

            PasswordChangeError.CURRENT_PASSWORD_WRONG -> {
                showSnackBar(
                    R.string.change_password_error_old_incorrect,
                    type = SnackbarType.Error
                )
            }

            PasswordChangeError.NEW_PASSWORDS_DONT_MATCH -> {
                showSnackBar(
                    R.string.change_password_error_new_dont_match,
                    type = SnackbarType.Error
                )
            }

            PasswordChangeError.NEW_PASSWORD_INVALID_LENGTH -> {
                showSnackBar(
                    R.string.change_password_error_new_length,
                    type = SnackbarType.Error
                )
            }

            PasswordChangeError.NEW_PASSWORD_TOO_WEAK -> {
                showSnackBar(
                    R.string.change_password_error_new_too_weak,
                    type = SnackbarType.Error
                )
            }

            PasswordChangeError.UNKNOWN_ERROR -> {
                showSnackBar(
                    R.string.change_password_error_general,
                    type = SnackbarType.Error
                )
            }

            PasswordChangeError.NONE -> {
                // do nothing
            }
        }

        model.process(PasswordChangeIntent.ResetErrorState)
    }

    private fun showSnackBar(@StringRes stringId: Int, type: SnackbarType) {
        BlockchainSnackbar.make(binding.root, getString(stringId), type = type).show()
    }

    companion object {
        fun newInstance(): PasswordChangeFragment = PasswordChangeFragment()
    }
}
