package piuk.blockchain.android.ui.settings.v2.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.ui.text.input.KeyboardType
import androidx.fragment.app.DialogFragment
import com.blockchain.commonarch.presentation.mvi.MviBottomSheet
import org.koin.core.scope.Scope
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.BottomSheetCodeSmsVerificationBinding
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.ui.customviews.toast

class CodeSMSVerificationBottomSheet :
    MviBottomSheet<ProfileModel, ProfileIntent, ProfileState, BottomSheetCodeSmsVerificationBinding>() {

    private val scope: Scope by lazy {
        (requireActivity() as ProfileActivity).scope
    }

    override val model: ProfileModel
        get() = scope.get()

    override fun initControls(binding: BottomSheetCodeSmsVerificationBinding) {
        setupUI()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.FloatingBottomSheet)
    }

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): BottomSheetCodeSmsVerificationBinding =
        BottomSheetCodeSmsVerificationBinding.inflate(inflater, container, false)

    override fun render(newState: ProfileState) {
        when (newState.error) {
            ProfileError.VerifyPhoneError -> toast(
                getString(R.string.profile_verification_code_error), ToastCustom.TYPE_ERROR
            )
            ProfileError.ResendSmsError -> toast(getString(R.string.profile_resend_sms_error), ToastCustom.TYPE_ERROR)
            else -> {}
        }
    }

    private fun setupUI() {
        with(binding) {
            codeSms.apply {
                singleLine = true
                labelText = getString(R.string.profile_enter_code)
                inputType = KeyboardType.Number
                onValueChange = { value = it }
            }
            resendSms.apply {
                text = getString(R.string.profile_re_send_text)
                onClick = { resendCodeSMS() }
            }
            verifyCode.apply {
                text = getString(R.string.profile_verify_code)
                onClick = { verifyCodeSMS() }
            }
        }
    }

    private fun resendCodeSMS() {
        model.process(ProfileIntent.ResendCodeSMS)
    }

    private fun verifyCodeSMS() {
        model.process(ProfileIntent.VerifyPhoneNumber(binding.codeSms.value))
    }

    companion object {
        fun newInstance(): CodeSMSVerificationBottomSheet = CodeSMSVerificationBottomSheet()
    }
}
