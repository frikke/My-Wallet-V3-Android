package piuk.blockchain.android.ui.login

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.componentlib.viewextensions.visibleIf
import com.blockchain.koin.customerSupportSheetFeatureFlag
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.remoteconfig.FeatureFlag
import java.util.concurrent.atomic.AtomicBoolean
import org.koin.android.ext.android.inject
import org.koin.java.KoinJavaComponent.get
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.FragmentVerifyDeviceBinding
import piuk.blockchain.android.ui.customersupport.CustomerSupportAnalytics
import piuk.blockchain.android.ui.customersupport.CustomerSupportSheet
import piuk.blockchain.android.ui.customviews.BlockchainSnackbar

class VerifyDeviceFragment : Fragment(), Analytics by get(Analytics::class.java) {

    private var _binding: FragmentVerifyDeviceBinding? = null

    val binding get() = _binding!!

    private val customerSupportSheetFF: FeatureFlag by inject(customerSupportSheetFeatureFlag)

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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentVerifyDeviceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            backButton.setOnClickListener {
                (requireActivity() as LoginIntentCoordinator).process(LoginIntents.RevertToEmailInput)
                requireActivity().supportFragmentManager.popBackStack()
            }
            customerSupport.setOnClickListener {
                logEvent(CustomerSupportAnalytics.CustomerSupportClicked)
                showCustomerSupportSheet()
            }
            customerSupportSheetFF.enabled.onErrorReturn { false }
                .subscribe { enabled -> customerSupport.visibleIf { enabled } }
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
                    (requireActivity() as LoginIntentCoordinator)
                        .process(LoginIntents.SendEmail(sessionId, email, captcha))
                    BlockchainSnackbar.make(
                        root, getString(R.string.verify_device_email_resent),
                        type = SnackbarType.Success
                    ).show()
                } else {
                    BlockchainSnackbar.make(
                        root, getString(R.string.verify_device_resend_blocked),
                        type = SnackbarType.Error
                    ).show()
                }
            }
        }
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

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun showCustomerSupportSheet() {
        (requireActivity() as BlockchainActivity).showBottomSheet(CustomerSupportSheet.newInstance())
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
