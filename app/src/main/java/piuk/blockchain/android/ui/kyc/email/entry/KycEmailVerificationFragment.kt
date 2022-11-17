package piuk.blockchain.android.ui.kyc.email.entry

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.lifecycleScope
import com.blockchain.commonarch.presentation.base.SlidingModalBottomDialog
import com.blockchain.commonarch.presentation.mvi_v2.MVIFragment
import com.blockchain.commonarch.presentation.mvi_v2.NavigationRouter
import com.blockchain.commonarch.presentation.mvi_v2.bindViewModel
import com.blockchain.koin.payloadScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import org.koin.android.scope.AndroidScopeComponent
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.scope.Scope
import piuk.blockchain.android.EmailVerificationArgs
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.kyc.ParentActivityDelegate

class KycEmailVerificationFragment :
    MVIFragment<EmailVerificationViewState>(),
    NavigationRouter<Navigation>,
    SlidingModalBottomDialog.Host,
    ResendOrChangeEmailBottomSheet.ResendOrChangeEmailHost,
    EditEmailAddressBottomSheet.Host,
    AndroidScopeComponent {

    override var scope: Scope? = payloadScope

    private val model: EmailVerificationModel by viewModel()

    private val emailEntryHost: EmailEntryHost by ParentActivityDelegate(this)

    private val emailMustBeValidated by lazy {
        when {
            arguments?.containsKey("mustBeValidated") == true ->
                EmailVerificationArgs.fromBundle(arguments ?: Bundle()).mustBeValidated
            arguments?.containsKey(CAN_SKIP) == true -> !requireArguments().getBoolean(CAN_SKIP)
            else -> false
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setContent {
            EmailVerificationScreen(
                viewState = model.viewState,
                onIntent = model::onIntent,
                openInbox = { openInbox() },
                openResendOrChangeSheet = {
                    model.onIntent(EmailVerificationIntent.StopPollingForVerification)
                    ResendOrChangeEmailBottomSheet().show(childFragmentManager, BOTTOM_SHEET)
                },
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViewModel(model, this, Args(emailMustBeValidated))

        emailEntryHost.onEmailEntryFragmentUpdated(showSkipButton = !emailMustBeValidated) {
            emailEntryHost.onEmailVerificationSkipped()
            emailEntryHost.onEmailEntryFragmentUpdated(showSkipButton = false)
        }
    }

    private var exitJob: Job? = null
    override fun onStateUpdated(state: EmailVerificationViewState) {
        if (state.isVerified) {
            emailEntryHost.onEmailEntryFragmentUpdated(showSkipButton = false)

            if (exitJob == null || exitJob?.isCompleted == true) {
                exitJob = lifecycleScope.launchWhenResumed {
                    delay(1000)
                    emailEntryHost.onEmailVerified()
                }
            }
        }
    }

    override fun route(navigationEvent: Navigation) {
        when (navigationEvent) {
            is Navigation.EditEmailSheet -> {
                EditEmailAddressBottomSheet.newInstance(navigationEvent.currentEmail)
                    .show(childFragmentManager, BOTTOM_SHEET)
            }
        }
    }

    private fun openInbox() {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_APP_EMAIL)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(Intent.createChooser(intent, getString(R.string.security_centre_email_check)))
    }

    override fun resendEmail() {
        model.onIntent(EmailVerificationIntent.ResendEmailClicked)
    }

    override fun editEmail() {
        model.onIntent(EmailVerificationIntent.EditEmailClicked)
    }

    override fun updateEmail(email: String) {
        model.onIntent(EmailVerificationIntent.OnEmailChanged(email))
    }

    override fun onSheetClosed() {
        model.onIntent(EmailVerificationIntent.StartPollingForVerification)
    }

    companion object {
        const val BOTTOM_SHEET = "BOTTOM_SHEET"
        private const val CAN_SKIP = "CAN_SKIP"

        fun newInstance(canBeSkipped: Boolean): KycEmailVerificationFragment =
            KycEmailVerificationFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(CAN_SKIP, canBeSkipped)
                }
            }
    }
}

interface EmailEntryHost {
    fun onEmailEntryFragmentUpdated(showSkipButton: Boolean, buttonAction: () -> Unit = {})
    fun onEmailVerified()
    fun onEmailVerificationSkipped()
}
