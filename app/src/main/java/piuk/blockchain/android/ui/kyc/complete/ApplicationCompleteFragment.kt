package piuk.blockchain.android.ui.kyc.complete

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.blockchain.analytics.Analytics
import com.blockchain.analytics.events.KYCAnalyticsEvents
import com.blockchain.core.kyc.domain.KycService
import com.blockchain.core.kyc.domain.model.KycTier
import com.blockchain.presentation.koin.scopedInject
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.kotlin.zipWith
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.databinding.FragmentKycCompleteBinding
import piuk.blockchain.android.simplebuy.SimpleBuyActivity
import piuk.blockchain.android.ui.home.MainActivity
import piuk.blockchain.android.ui.kyc.ParentActivityDelegate
import piuk.blockchain.android.ui.kyc.navhost.KycNavHostActivity
import piuk.blockchain.android.ui.kyc.navhost.KycProgressListener
import piuk.blockchain.android.ui.kyc.navigate
import piuk.blockchain.android.util.throttledClicks
import timber.log.Timber

class ApplicationCompleteFragment : Fragment() {

    private var _binding: FragmentKycCompleteBinding? = null
    private val binding: FragmentKycCompleteBinding
        get() = _binding!!

    private val progressListener: KycProgressListener by ParentActivityDelegate(this)

    private val compositeDisposable = CompositeDisposable()
    private val analytics: Analytics by inject()
    private val kycService: KycService by scopedInject()

    override fun onAttach(context: Context) {
        super.onAttach(context)

        requireActivity().onBackPressedDispatcher.addCallback(owner = this) {
            requireActivity().setResult(AppCompatActivity.RESULT_OK)
            requireActivity().finish()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentKycCompleteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        progressListener.setupHostToolbar(R.string.kyc_complete_title)
        progressListener.hideBackButton()
    }

    override fun onResume() {
        super.onResume()

        compositeDisposable +=
            binding.buttonDone
                .throttledClicks().zipWith(
                    if (progressListener.campaignType == CampaignType.Swap ||
                        progressListener.campaignType == CampaignType.None
                    ) {
                        kycService.getTiersLegacy().toObservable()
                            .map { it.isApprovedFor(KycTier.SILVER) || it.isApprovedFor(KycTier.GOLD) }
                            .onErrorReturn { false }
                    } else {
                        Observable.just(false)
                    }
                ).subscribeBy(
                    onNext = { (_, _) ->
                        when (progressListener.campaignType) {
                            CampaignType.Swap -> {
                                launchSwap()
                            }
                            CampaignType.SimpleBuy -> {
                                activity?.setResult(SimpleBuyActivity.RESULT_KYC_SIMPLE_BUY_COMPLETE)
                                activity?.finish()
                            }
                            CampaignType.None,
                            CampaignType.Interest -> {
                                activity?.setResult(KycNavHostActivity.RESULT_KYC_FOR_TIER_COMPLETE)
                                activity?.finish()
                            }
                            else -> navigate(ApplicationCompleteFragmentDirections.actionTier2Complete())
                        }
                        analytics.logEvent(KYCAnalyticsEvents.VeriffInfoSubmitted)
                    },
                    onError = { Timber.e(it) }
                )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun launchSwap() {
        startActivity(
            MainActivity.newIntent(
                requireContext(),
                shouldShowSwap = true,
                shouldBeNewTask = true
            )
        )
        activity?.finish()
    }

    override fun onPause() {
        super.onPause()
        compositeDisposable.clear()
    }
}
