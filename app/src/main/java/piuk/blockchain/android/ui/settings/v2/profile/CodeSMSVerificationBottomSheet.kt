package piuk.blockchain.android.ui.settings.v2.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.blockchain.commonarch.presentation.base.HostedBottomSheet
import com.blockchain.commonarch.presentation.mvi.MviBottomSheet
import com.blockchain.koin.scopedInject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.BottomSheetCodeSmsVerificationBinding
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.ui.customviews.toast

class CodeSMSVerificationBottomSheet :
    MviBottomSheet<SMSVerificationModel, SMSVerificationIntent,
        SMSVerificationState, BottomSheetCodeSmsVerificationBinding>() {

    interface Host : HostedBottomSheet.Host {
        fun onReloadProfile()
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.FloatingBottomSheet)
    }

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): BottomSheetCodeSmsVerificationBinding =
        BottomSheetCodeSmsVerificationBinding.inflate(inflater, container, false)

    override fun initControls(binding: BottomSheetCodeSmsVerificationBinding) {
        setupUI()
    }

    override fun render(newState: SMSVerificationState) {
        when (newState.error) {
            VerificationError.VerifyPhoneError -> toast(
                getString(R.string.profile_verification_code_error), ToastCustom.TYPE_ERROR
            )
            VerificationError.ResendSmsError -> toast(
                getString(R.string.profile_resend_sms_error), ToastCustom.TYPE_ERROR
            )
        }
        if (newState.isCodeSmsSent == true) {
            toast(getString(R.string.code_verification_resent_sms), ToastCustom.TYPE_OK)
            model.process(SMSVerificationIntent.ResetCodeSentVerification)
        }
        if (newState.isPhoneVerified == true) {
            host.onReloadProfile()
            dismiss()
        }
    }

    private fun setupUI() {
        with(binding) {
            codeSms.apply {
                singleLine = true
                labelText = getString(R.string.code_verification_enter)
                onValueChange = { value = it }
                placeholderText = context.getString(R.string.code_verification_placeholder)
            }
            resendSms.apply {
                text = getString(R.string.code_verification_re_send_text)
                onClick = { resendCodeSMS() }
            }
            verifyCode.apply {
                text = getString(R.string.code_verification_verify_code)
                onClick = { verifyCodeSMS() }
            }
        }
    }

    private fun resendCodeSMS() {
        model.process(SMSVerificationIntent.ResendCodeSMS(phoneNumber))
    }

    private fun verifyCodeSMS() {
        model.process(SMSVerificationIntent.VerifyPhoneNumber(binding.codeSms.value))
    }

    companion object {
        private const val PHONE_NUMBER = "phone_number"

        fun newInstance(phoneNumber: String) =
            CodeSMSVerificationBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(PHONE_NUMBER, phoneNumber)
                }
            }
    }
}
