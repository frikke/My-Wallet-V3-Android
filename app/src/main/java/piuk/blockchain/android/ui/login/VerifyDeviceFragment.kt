package piuk.blockchain.android.ui.login

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import com.blockchain.koin.scopedInject
import java.util.concurrent.atomic.AtomicBoolean
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.FragmentVerifyDeviceBinding
import piuk.blockchain.android.ui.base.mvi.MviFragment
import piuk.blockchain.android.ui.customviews.ToastCustom

class VerifyDeviceFragment : MviFragment<LoginModel, LoginIntents, LoginState, FragmentVerifyDeviceBinding>() {

    override val model: LoginModel by scopedInject()

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentVerifyDeviceBinding =
        FragmentVerifyDeviceBinding.inflate(inflater, container, false)

    private val isTimerRunning = AtomicBoolean(false)
    private val timer = object : CountDownTimer(RESEND_TIMEOUT, TIMER_STEP) {
        override fun onTick(millisUntilFinished: Long) {
            isTimerRunning.set(true)
        }

        override fun onFinish() {
            isTimerRunning.set(false)
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                binding.resendEmailButton.isActivated = true
            }
        }
    }

    private val email: String by lazy {
        arguments?.getString(EMAIL) ?: throw IllegalArgumentException("No email specified")
    }

    private val sessionId: String by lazy {
        arguments?.getString(SESSION_ID) ?: throw IllegalArgumentException("No session id specified")
    }

    private val captcha: String by lazy {
        arguments?.getString(CAPTCHA) ?: throw IllegalArgumentException("No captcha specified")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            backButton.setOnClickListener {
                model.process(LoginIntents.RevertToEmailInput)
            }
            verifyDeviceDescription.text = getString(R.string.verify_device_desc)
            openEmailButton.setOnClickListener {
                Intent(Intent.ACTION_MAIN).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addCategory(Intent.CATEGORY_APP_EMAIL)
                    startActivity(Intent.createChooser(this, getString(R.string.security_centre_email_check)))
                }
            }
            resendEmailButton.setOnClickListener {
                if (!isTimerRunning.get()) {
                    timer.start()
                    model.process(LoginIntents.SendEmail(sessionId, email, captcha))
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
        // do nothing
    }

    override fun onPause() {
        super.onPause()
        if (isTimerRunning.get()) {
            timer.cancel()
        }
    }

    override fun onResume() {
        super.onResume()
        // check if the timer was running before onPause was called, if so, re-init
        if (isTimerRunning.get()) {
            timer.start()
        }
    }

    companion object {
        private const val RESEND_TIMEOUT = 30000L
        private const val TIMER_STEP = 1000L
        private const val SESSION_ID = "SESSION_ID"
        private const val EMAIL = "EMAIL"
        private const val CAPTCHA = "CAPTCHA"

        fun newInstance(sessionId: String, email: String, captcha: String): Fragment =
            VerifyDeviceFragment().apply {
                arguments = Bundle().apply {
                    putString(SESSION_ID, sessionId)
                    putString(EMAIL, email)
                    putString(CAPTCHA, captcha)
                }
            }
    }
}
