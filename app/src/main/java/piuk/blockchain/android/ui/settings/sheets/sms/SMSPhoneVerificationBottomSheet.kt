package piuk.blockchain.android.ui.settings.sheets.sms

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.StringRes
import com.blockchain.commonarch.presentation.base.HostedBottomSheet
import com.blockchain.commonarch.presentation.mvi.MviBottomSheet
import com.blockchain.componentlib.alert.BlockchainSnackbar
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.koin.scopedInject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.BottomSheetCodeSmsVerificationBinding

class SMSPhoneVerificationBottomSheet :
    MviBottomSheet<SMSVerificationModel, SMSVerificationIntent,
        SMSVerificationState, BottomSheetCodeSmsVerificationBinding>() {

    interface Host : HostedBottomSheet.Host {
        fun onPhoneNumberVerified()
    }

    override val model: SMSVerificationModel by scopedInject()

    override val host: Host by lazy {
        super.host as? Host ?: throw IllegalStateException(
            "Host fragment is not a CodeSMSVerificationBottomSheet.Host"
        )
    }

    private val phoneNumber: String by lazy {
        arguments?.getString(PHONE_NUMBER).orEmpty()
    }

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): BottomSheetCodeSmsVerificationBinding =
        BottomSheetCodeSmsVerificationBinding.inflate(inflater, container, false)

    override fun initControls(binding: BottomSheetCodeSmsVerificationBinding) {
        setupUI()
    }

    override fun render(newState: SMSVerificationState) {
        if (newState.error != null) {
            when (newState.error) {
                VerificationError.VerifyPhoneError -> showSnackbar(
                    SnackbarType.Error, R.string.profile_verification_code_error
                )
                VerificationError.ResendSmsError ->
                    showSnackbar(SnackbarType.Error, R.string.profile_resend_sms_error)
            }
            model.process(SMSVerificationIntent.ClearErrors)
        }

        if (newState.isCodeSmsSent) {
            showSnackbar(SnackbarType.Success, R.string.code_verification_resent_sms)
            model.process(SMSVerificationIntent.ResetCodeSentVerification)
        }
        if (newState.isPhoneVerified) {
            showSnackbar(SnackbarType.Success, R.string.sms_verified)
            host.onPhoneNumberVerified()
            dismiss()
        }
    }

    private fun showSnackbar(type: SnackbarType, @StringRes message: Int) {
        BlockchainSnackbar.make(
            dialog?.window?.decorView ?: binding.root,
            getString(message),
            type = type
        ).show()
    }

    private fun setupUI() {
        with(binding) {
            smsCode.apply {
                singleLine = true
                labelText = getString(R.string.code_verification_enter)
                onValueChange = { value = it }
                placeholderText = context.getString(R.string.code_verification_placeholder)
            }
            resendSms.apply {
                text = getString(R.string.code_verification_re_send_text)
                onClick = { resendSMS() }
            }
            verifyCode.apply {
                text = getString(R.string.code_verification_verify_code)
                onClick = { verifyCode() }
            }
            sheetHeader.apply {
                title = getString(R.string.code_verification_title)
                onClosePress = {
                    this@SMSPhoneVerificationBottomSheet.dismiss()
                }
            }
        }
    }

    private fun resendSMS() {
        model.process(SMSVerificationIntent.ResendSMS(phoneNumber))
    }

    private fun verifyCode() {
        model.process(SMSVerificationIntent.VerifySMSCode(binding.smsCode.value))
    }

    companion object {
        private const val PHONE_NUMBER = "phone_number"

        fun newInstance(phoneNumber: String) =
            SMSPhoneVerificationBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(PHONE_NUMBER, phoneNumber)
                }
            }
    }
}
