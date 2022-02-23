package piuk.blockchain.android.ui.settings.v2.profile.email

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.compose.ui.text.input.KeyboardType
import com.blockchain.commonarch.presentation.base.FlowFragment
import com.blockchain.commonarch.presentation.base.updateTitleToolbar
import com.blockchain.commonarch.presentation.mvi.MviFragment
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.controls.TextInputState
import com.blockchain.componentlib.viewextensions.visibleIf
import com.blockchain.koin.scopedInject
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.FragmentUpdateEmailBinding
import piuk.blockchain.android.ui.customviews.BlockchainSnackbar
import piuk.blockchain.android.util.FormatChecker

class UpdateEmailFragment :
    MviFragment<EmailModel, EmailIntent, EmailState, FragmentUpdateEmailBinding>(),
    FlowFragment {

    private val formatChecker: FormatChecker by inject()

    override val model: EmailModel by scopedInject()

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentUpdateEmailBinding =
        FragmentUpdateEmailBinding.inflate(inflater, container, false)

    override fun onBackPressed(): Boolean = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        this.updateTitleToolbar(getString(R.string.profile_toolbar_email))
        binding.updateEmail.buttonState = ButtonState.Disabled
    }

    override fun onResume() {
        super.onResume()
        model.process(EmailIntent.LoadProfile)
    }

    override fun render(newState: EmailState) {
        if (!newState.isLoading) {
            newState.userInfoSettings?.let { updateUI(it.emailVerified, it.email.orEmpty()) }
        }

        if (newState.error == EmailError.SaveEmailError) {
            BlockchainSnackbar.make(
                binding.root, getString(R.string.profile_update_error_email), type = SnackbarType.Error
            ).show()
            model.process(EmailIntent.ClearErrors)
        }

        if (newState.error == EmailError.ResendEmailError) {
            BlockchainSnackbar.make(
                binding.root, getString(R.string.profile_update_error_resend_email), type = SnackbarType.Error
            ).show()
            model.process(EmailIntent.ClearErrors)
        }

        if (newState.emailSent) {
            showDialogEmailVerification()
            model.process(EmailIntent.ResetEmailSentVerification)
        }
    }

    private fun onVerifyEmailClicked() {
        model.process(EmailIntent.ResendEmail)
    }

    private fun isValidEmailAddress(): Boolean {
        val newEmailAddress = binding.email.value
        return if (!formatChecker.isValidEmailAddress(newEmailAddress)) {
            binding.email.apply {
                state = TextInputState.Error(getString(R.string.invalid_email))
                trailingIconResource = ImageResource.Local(R.drawable.ic_alert, null)
            }
            false
        } else {
            binding.email.apply {
                trailingIconResource = ImageResource.None
                state = TextInputState.Default()
            }
            true
        }
    }

    private fun updateUI(isEmailVerified: Boolean, emailValue: String) {
        with(binding) {
            email.apply {
                labelText = context.getString(R.string.profile_label_email)
                singleLine = true
                inputType = KeyboardType.Text
                value = emailValue
                state = TextInputState.Default()
                onValueChange = {
                    changeStateCta(it, emailValue)
                    value = it
                }
                trailingIconResource = ImageResource.None
            }

            changeStateCta(binding.email.value, emailValue)
            verifyEmailBtn.visibleIf { !isEmailVerified }
            verifyEmailBtn.apply {
                text = getString(R.string.profile_verify_email)
                onClick = { onVerifyEmailClicked() }
            }

            updateEmail.apply {
                text = getString(R.string.profile_update)
                onClick = {
                    if (isValidEmailAddress()) {
                        model.process(EmailIntent.SaveEmail(binding.email.value))
                    }
                }
            }
        }
    }

    private fun changeStateCta(newEmail: String, currentEmail: String) {
        val stateButton = if (newEmail == currentEmail) ButtonState.Disabled else ButtonState.Enabled
        val stateVerifyButton = if (newEmail == currentEmail) ButtonState.Enabled else ButtonState.Disabled
        binding.updateEmail.buttonState = stateButton
        binding.verifyEmailBtn.buttonState = stateVerifyButton
    }

    private fun checkEmail() {
        Intent(Intent.ACTION_MAIN).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addCategory(Intent.CATEGORY_APP_EMAIL)
            startActivity(Intent.createChooser(this, getString(R.string.security_centre_email_check)))
        }
    }

    override fun onStop() {
        super.onStop()
        model.process(EmailIntent.InvalidateCache)
    }

    private fun showDialogEmailVerification() {
        AlertDialog.Builder(requireContext(), R.style.AlertDialogStyle)
            .setTitle(R.string.verify)
            .setMessage(R.string.verify_email_notice)
            .setCancelable(true)
            .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.dismiss() }
            .setPositiveButton(android.R.string.ok) { _, _ -> checkEmail() }
            .show()
    }

    companion object {
        fun newInstance() = UpdateEmailFragment()
    }
}
