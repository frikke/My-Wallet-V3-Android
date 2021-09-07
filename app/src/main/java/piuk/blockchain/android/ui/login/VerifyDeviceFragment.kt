package piuk.blockchain.android.ui.login

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.blockchain.koin.scopedInject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.FragmentVerifyDeviceBinding
import piuk.blockchain.android.ui.base.mvi.MviFragment
import piuk.blockchain.android.ui.customviews.ToastCustom
import java.util.concurrent.atomic.AtomicBoolean

class VerifyDeviceFragment : MviFragment<LoginModel, LoginIntents, LoginState, FragmentVerifyDeviceBinding>() {

    override val model: LoginModel by scopedInject()

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentVerifyDeviceBinding =
        FragmentVerifyDeviceBinding.inflate(inflater, container, false)

    private lateinit var state: LoginState

    private val isTimerRunning = AtomicBoolean(false)
    private val timer = object : CountDownTimer(RESEND_TIMEOUT, TIMER_STEP) {
        override fun onTick(millisUntilFinished: Long) {
            isTimerRunning.set(true)
        }

        override fun onFinish() {
            isTimerRunning.set(false)
            binding.resendEmailButton.isActivated = true
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            backButton.setOnClickListener {
                parentFragmentManager.popBackStack()
            }
            verifyDeviceDescription.text = getString(R.string.verify_device_desc)
            openEmailButton.setOnClickListener {
                Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_APP_EMAIL)
                    startActivity(Intent.createChooser(this, getString(R.string.security_centre_email_check)))
                }
            }
            resendEmailButton.setOnClickListener {
                if (!isTimerRunning.get()) {
                    timer.start()
                    model.process(LoginIntents.SendEmail(state.sessionId, state.email, state.captcha))
                    ToastCustom.makeText(
                        requireContext(), getString(R.string.verify_device_email_resent), Toast.LENGTH_SHORT,
                        ToastCustom.TYPE_OK
                    )
                } else {
                    ToastCustom.makeText(
                        requireContext(), getString(R.string.verify_device_resend_blocked), Toast.LENGTH_LONG,
                        ToastCustom.TYPE_ERROR
                    )
                }
            }
        }
    }

    override fun render(newState: LoginState) {
        state = newState
    }

    companion object {
        private const val RESEND_TIMEOUT = 30000L
        private const val TIMER_STEP = 1000L
    }
}