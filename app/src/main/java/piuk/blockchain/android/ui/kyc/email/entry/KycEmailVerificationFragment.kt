package piuk.blockchain.android.ui.kyc.email.entry

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import com.blockchain.chrome.composable.ChromeSingleScreen
import com.blockchain.commonarch.presentation.base.SlidingModalBottomDialog
import com.blockchain.componentlib.navigation.ModeBackgroundColor
import com.blockchain.koin.payloadScope
import com.blockchain.kyc.email.EmailVerification
import com.blockchain.walletmode.WalletMode
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import org.koin.android.scope.AndroidScopeComponent
import org.koin.core.scope.Scope
import piuk.blockchain.android.EmailVerificationArgs
import piuk.blockchain.android.ui.kyc.ParentActivityDelegate

class KycEmailVerificationFragment :
    Fragment(),
    SlidingModalBottomDialog.Host,
    AndroidScopeComponent {

    override val scope: Scope = payloadScope

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
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {

                val legacyToolbar = arguments?.getBoolean("legacyToolbar") == true

                val systemUiController = rememberSystemUiController()
                systemUiController.setStatusBarColor(Color.Transparent)

                if (legacyToolbar) {
                    Screen()
                } else {
                    ChromeSingleScreen(backgroundColor = ModeBackgroundColor.Override(WalletMode.CUSTODIAL)) {
                        Screen()
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        emailEntryHost.onEmailEntryFragmentUpdated(showSkipButton = !emailMustBeValidated) {
            emailEntryHost.onEmailVerificationSkipped()
            emailEntryHost.onEmailEntryFragmentUpdated(showSkipButton = false)
        }
    }

    override fun onSheetClosed() {
    }

    companion object {
        private const val CAN_SKIP = "CAN_SKIP"

        fun newInstance(canBeSkipped: Boolean): KycEmailVerificationFragment =
            KycEmailVerificationFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(CAN_SKIP, canBeSkipped)
                }
            }
    }

    @Composable
    fun Screen() {
        EmailVerification(
            verificationRequired = emailMustBeValidated,
            legacyToolbar = arguments?.getBoolean("legacyToolbar") == true,
            closeOnClick = { emailEntryHost.onEmailVerificationSkipped() },
            nextOnClick = {
                emailEntryHost.onEmailEntryFragmentUpdated(showSkipButton = false)
                emailEntryHost.onEmailVerified()
            }
        )
    }
}

interface EmailEntryHost {
    fun onEmailEntryFragmentUpdated(showSkipButton: Boolean, buttonAction: () -> Unit = {})
    fun onEmailVerified()
    fun onEmailVerificationSkipped()
}
