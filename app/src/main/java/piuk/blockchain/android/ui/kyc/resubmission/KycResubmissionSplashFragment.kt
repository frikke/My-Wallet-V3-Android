package piuk.blockchain.android.ui.kyc.resubmission

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.blockchain.analytics.data.logEvent
import com.blockchain.analytics.events.AnalyticsEvents
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.presentation.koin.scopedInject
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.FragmentKycResubmissionSplashBinding
import piuk.blockchain.android.ui.kyc.ParentActivityDelegate
import piuk.blockchain.android.ui.kyc.navhost.KycProgressListener
import piuk.blockchain.android.ui.kyc.navigate
import piuk.blockchain.android.ui.kyc.reentry.KycNavigator
import timber.log.Timber

class KycResubmissionSplashFragment : Fragment() {

    private var _binding: FragmentKycResubmissionSplashBinding? = null
    private val binding: FragmentKycResubmissionSplashBinding
        get() = _binding!!

    private val progressListener: KycProgressListener by ParentActivityDelegate(
        this
    )

    private val kycNavigator: KycNavigator by scopedInject()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentKycResubmissionSplashBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        logEvent(AnalyticsEvents.KycResubmission)

        progressListener.setupHostToolbar(com.blockchain.stringResources.R.string.kyc_resubmission_splash_title)

        binding.buttonKycResubmissionSplashNext.apply {
            text = getString(com.blockchain.stringResources.R.string.common_next)
            buttonState = ButtonState.Enabled
            onClick = {
                buttonState = ButtonState.Disabled

                disposable += kycNavigator.findNextStep()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy(
                        onSuccess = { navigate(it) },
                        onError = { Timber.e(it) }
                    )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private val disposable = CompositeDisposable()

    override fun onPause() {
        disposable.clear()
        super.onPause()
    }
}
