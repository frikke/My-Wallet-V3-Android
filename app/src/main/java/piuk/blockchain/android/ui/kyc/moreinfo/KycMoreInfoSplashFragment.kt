package piuk.blockchain.android.ui.kyc.moreinfo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.blockchain.analytics.data.logEvent
import com.blockchain.analytics.events.AnalyticsEvents
import com.blockchain.componentlib.button.common.BaseButtonView
import com.blockchain.componentlib.viewextensions.inflate
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.kyc.ParentActivityDelegate
import piuk.blockchain.android.ui.kyc.navhost.KycProgressListener
import piuk.blockchain.android.ui.kyc.navigate

class KycMoreInfoSplashFragment : Fragment() {

    private val progressListener: KycProgressListener by ParentActivityDelegate(
        this
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = container?.inflate(R.layout.fragment_kyc_more_info_splash)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        logEvent(AnalyticsEvents.KycMoreInfo)

        requireView().findViewById<BaseButtonView>(R.id.button_kyc_more_info_splash_next).apply {
            text = getString(com.blockchain.stringResources.R.string.common_next)
            onClick = {
                navigate(
                    KycMoreInfoSplashFragmentDirections.actionKycMoreInfoSplashFragmentToMobileVerification(
                        KycMoreInfoSplashFragmentArgs.fromBundle(arguments ?: Bundle()).countryCode
                    )
                )
            }
        }
        progressListener.setupHostToolbar(com.blockchain.stringResources.R.string.kyc_more_info_splash_title)
    }
}
