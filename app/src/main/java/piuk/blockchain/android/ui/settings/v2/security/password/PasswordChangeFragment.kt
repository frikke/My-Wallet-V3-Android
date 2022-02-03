package piuk.blockchain.android.ui.settings.v2.security.password

import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.blockchain.commonarch.presentation.mvi.MviFragment
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.componentlib.viewextensions.visibleIf
import com.blockchain.koin.scopedInject
import com.google.android.material.snackbar.Snackbar
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.FragmentPasswordUpdateBinding
import piuk.blockchain.android.ui.base.updateTitleToolbar
import piuk.blockchain.android.util.AfterTextChangedWatcher

class PasswordChangeFragment :
    MviFragment<PasswordChangeModel, PasswordChangeIntent, PasswordChangeState, FragmentPasswordUpdateBinding>() {
    override val model: PasswordChangeModel by scopedInject()

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentPasswordUpdateBinding =
        FragmentPasswordUpdateBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        this.updateTitleToolbar(getString(R.string.change_password_title))

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
                            currentPassword = fieldPassword.text.toString(),
                            newPassword = fieldNewPassword.text.toString(),
                            newPasswordConfirmation = fieldConfirmPassword.text.toString()
                        )
                    )
                }
            }

            fieldNewPassword.addTextChangedListener(object : AfterTextChangedWatcher() {
                override fun afterTextChanged(editable: Editable) {
                    fieldNewPassword.postDelayed({
                        if (!requireActivity().isFinishing) {
                            passwordStrength.visibleIf { editable.isNotEmpty() }
                            val strength = passwordStrength.updatePassword(editable.toString())
                            passwordCta.buttonState =
                                if (strength >= 51 && fieldPassword.text?.isNotEmpty() == true &&
                                    fieldConfirmPassword.text?.isNotEmpty() == true
                                ) {
                                    ButtonState.Enabled
                                } else {
                                    ButtonState.Disabled
                                }
                        }
                    }, 200)
                }
            })

            fieldConfirmPassword.addTextChangedListener(object : AfterTextChangedWatcher() {
                override fun afterTextChanged(editable: Editable) {
                    fieldNewPassword.postDelayed({
                        if (!requireActivity().isFinishing) {
                            passwordCta.buttonState =
                                if (fieldPassword.text?.isNotEmpty() == true &&
                                    fieldNewPassword.text?.isNotEmpty() == true
                                ) {
                                    ButtonState.Enabled
                                } else {
                                    ButtonState.Disabled
                                }
                        }
                    }, 200)
                }
            })
        }
    }

    override fun render(newState: PasswordChangeState) {
        if (newState.passwordViewState != PasswordViewState.None) {
            when (newState.passwordViewState) {
                PasswordViewState.CheckingPasswords -> {
                    binding.passwordProgress.visible()
                }
                PasswordViewState.PasswordUpdated -> {
                    Snackbar.make(binding.root, R.string.change_password_success, Snackbar.LENGTH_LONG).show()
                    with(binding) {
                        fieldPassword.setText("")
                        fieldNewPassword.setText("")
                        fieldConfirmPassword.setText("")
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
                Snackbar.make(binding.root, R.string.change_password_error_old_new_match, Snackbar.LENGTH_LONG).show()
            }
            PasswordChangeError.CURRENT_PASSWORD_WRONG -> {
                Snackbar.make(binding.root, R.string.change_password_error_old_incorrect, Snackbar.LENGTH_LONG).show()
            }
            PasswordChangeError.NEW_PASSWORDS_DONT_MATCH -> {
                Snackbar.make(binding.root, R.string.change_password_error_new_dont_match, Snackbar.LENGTH_LONG).show()
            }
            PasswordChangeError.NEW_PASSWORD_INVALID_LENGTH -> {
                Snackbar.make(binding.root, R.string.change_password_error_new_length, Snackbar.LENGTH_LONG).show()
            }
            PasswordChangeError.NEW_PASSWORD_TOO_WEAK -> {
                Snackbar.make(binding.root, R.string.change_password_error_new_too_weak, Snackbar.LENGTH_LONG).show()
            }
            PasswordChangeError.UNKNOWN_ERROR -> {
                Snackbar.make(binding.root, R.string.change_password_error_general, Snackbar.LENGTH_LONG).show()
            }
            PasswordChangeError.NONE -> {
                // do nothing
            }
        }

        model.process(PasswordChangeIntent.ResetErrorState)
    }

    companion object {
        fun newInstance(): PasswordChangeFragment = PasswordChangeFragment()
    }
}
