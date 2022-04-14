package piuk.blockchain.android.ui.termsconditions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import androidx.fragment.app.Fragment
import com.blockchain.analytics.Analytics
import com.blockchain.componentlib.alert.BlockchainSnackbar
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.core.user.NabuUserDataManager
import com.blockchain.koin.scopedInject
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.FragmentTermsConditionsBinding

class TermsAndConditionsFragment : Fragment() {

    interface Host {
        fun termsAndConditionsAccepted()
    }

    private lateinit var binding: FragmentTermsConditionsBinding

    private val disposables = CompositeDisposable()

    private val userDataManager: NabuUserDataManager by scopedInject()
    private val analytics: Analytics by inject()

    private val url: String by lazy {
        arguments?.getString(ARG_URL).orEmpty()
    }

    private val host: Host
        get() = requireActivity() as Host

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        analytics.logEvent(TermsAndConditionsAnalytics.Viewed)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentTermsConditionsBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.webView.loadUrl(url)
        binding.webView.addJavascriptInterface(
            JavascriptInterfaceHandler(object : JavascriptInterfaceHandler.Listener {
                override fun onTermsAccepted() {
                    termsAndConditionsAccepted()
                }
            }),
            WEB_INTERFACE_NAME
        )
    }

    private fun termsAndConditionsAccepted() {
        disposables += userDataManager.signLatestTermsAndConditions()
            .doOnSubscribe {
                binding.progress.visible()
            }
            .doOnTerminate {
                binding.progress.gone()
            }
            .subscribeBy(
                onComplete = {
                    analytics.logEvent(TermsAndConditionsAnalytics.Accepted)
                    host.termsAndConditionsAccepted()
                },
                onError = {
                    BlockchainSnackbar.make(
                        binding.root,
                        getString(R.string.common_error),
                        type = SnackbarType.Error
                    ).show()
                }
            )
    }

    override fun onDestroyView() {
        disposables.clear()
        super.onDestroyView()
    }

    companion object {
        private const val WEB_INTERFACE_NAME = "BCWebCallbacks"
        private const val ARG_URL = "ARG_URL"

        fun newInstance(url: String): TermsAndConditionsFragment =
            TermsAndConditionsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_URL, url)
                }
            }
    }
}

private class JavascriptInterfaceHandler(
    private val listener: Listener
) {

    interface Listener {
        fun onTermsAccepted()
    }

    @JavascriptInterface
    fun termsAccepted() {
        listener.onTermsAccepted()
    }
}
