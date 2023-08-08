package piuk.blockchain.android.ui.linkbank.yodlee

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.net.http.SslError
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.SslErrorHandler
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import com.blockchain.analytics.Analytics
import com.blockchain.commonarch.presentation.base.updateTitleToolbar
import com.blockchain.componentlib.alert.BlockchainSnackbar
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.componentlib.viewextensions.visibleIf
import com.blockchain.domain.paymentmethods.model.YodleeAttributes
import com.blockchain.logging.RemoteLogger
import java.net.URLEncoder
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.json.JSONException
import org.json.JSONObject
import org.koin.android.ext.android.inject
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.FragmentYodleeWebviewBinding
import piuk.blockchain.android.simplebuy.SimpleBuyAnalytics
import piuk.blockchain.android.simplebuy.yodlee.FastLinkMessage
import piuk.blockchain.android.simplebuy.yodlee.MessageData
import piuk.blockchain.android.simplebuy.yodlee.SiteData
import piuk.blockchain.android.ui.linkbank.BankAuthFlowNavigator
import timber.log.Timber

class YodleeWebViewFragment :
    Fragment(),
    FastLinkInterfaceHandler.FastLinkListener,
    YodleeWebClient.YodleeWebClientInterface {

    private var _binding: FragmentYodleeWebviewBinding? = null
    private val binding: FragmentYodleeWebviewBinding
        get() = _binding!!

    private val json: Json by inject()
    private val analytics: Analytics by inject()
    private val remoteLogger: RemoteLogger by inject()
    private var isViewLoaded: Boolean = false

    private val attributes: YodleeAttributes by lazy {
        arguments?.getSerializable(ATTRIBUTES) as YodleeAttributes
    }

    private val linkingBankId: String by lazy {
        arguments?.getString(LINKING_BANK_ID) ?: ""
    }

    private val accessTokenKey = "accessToken"
    private val bearerParam: String by lazy { "Bearer ${attributes.token}" }
    private val extraParamsKey = "extraParams"
    private val extraParamConfigName: String
        get() = "configName=${attributes.configName}&intentUrl=${BuildConfig.BROKERAGE_SUCCESS}"
    private val extraParamEncoding = "UTF-8"

    private val yodleeQuery: String by lazy {
        Uri.Builder()
            .appendQueryParameter(accessTokenKey, bearerParam)
            .appendQueryParameter(extraParamsKey, URLEncoder.encode(extraParamConfigName, extraParamEncoding))
            .build().query ?: ""
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentYodleeWebviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateTitleToolbar(getString(com.blockchain.stringResources.R.string.link_a_bank))
        setupWebView()

        binding.yodleeRetry.apply {
            text = getString(com.blockchain.stringResources.R.string.common_retry)
            onClick = {
                loadYodlee()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val settings: WebSettings = binding.yodleeWebview.settings
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            setSupportMultipleWindows(true)
            loadsImagesAutomatically = true
            javaScriptCanOpenWindowsAutomatically = true
            cacheMode = WebSettings.LOAD_NO_CACHE
        }

        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
        with(binding.yodleeWebview) {
            webViewClient = YodleeWebClient(this@YodleeWebViewFragment)
            addJavascriptInterface(
                FastLinkInterfaceHandler(listener = this@YodleeWebViewFragment, json = json),
                "YWebViewHandler"
            )
        }
    }

    override fun onResume() {
        super.onResume()
        // this is here to prevent password autofill from triggering a reload,
        // as onResume gets called after clicking the floating widget
        if (!isViewLoaded) {
            loadYodlee()
        }
    }

    private fun loadYodlee() {
        requireActivity().runOnUiThread {
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                updateViewsVisibility(true)
                with(binding) {
                    yodleeWebview.clearCache(true)
                    yodleeStatusLabel.text = getString(com.blockchain.stringResources.R.string.yodlee_connection_title)
                    yodleeSubtitle.text = getString(com.blockchain.stringResources.R.string.yodlee_connection_subtitle)
                    yodleeWebview.gone()
                    yodleeRetry.gone()
                    yodleeWebview.postUrl(attributes.fastlinkUrl, yodleeQuery.toByteArray())
                }
            }
        }
        isViewLoaded = true
    }

    override fun flowSuccess(providerAccountId: String, accountId: String) {
        analytics.logEvent(SimpleBuyAnalytics.ACH_SUCCESS)
        requireActivity().runOnUiThread {
            navigator().launchBankLinking(
                accountProviderId = providerAccountId,
                accountId = accountId,
                bankId = linkingBankId
            )
        }
    }

    override fun flowError(error: FastLinkInterfaceHandler.FastLinkFlowError, reason: String?) {
        requireActivity().runOnUiThread {
            showError(getString(com.blockchain.stringResources.R.string.yodlee_parsing_error), reason)
        }
    }

    private fun showError(errorText: String, reason: String?) {
        with(binding) {
            yodleeWebview.gone()
            yodleeIcon.gone()
            yodleeProgress.gone()
            yodleeStatusLabel.text = errorText
            yodleeStatusLabel.visible()

            yodleeRetry.visible()
            reason?.let {
                yodleeSubtitle.visible()
                yodleeSubtitle.text = it
            } ?: kotlin.run {
                yodleeSubtitle.gone()
            }
        }
    }

    override fun openExternalUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        requireContext().startActivity(intent)
    }

    override fun pageFinishedLoading() {
        try {
            binding.yodleeWebview.visible()
            updateViewsVisibility(false)
        } catch (e: NullPointerException) {
            remoteLogger.logException(e, "Underlying binding is null")
            BlockchainSnackbar.make(
                binding.root,
                getString(com.blockchain.stringResources.R.string.common_error),
                type = SnackbarType.Error
            ).show()
            navigator().bankAuthCancelled()
        }
    }

    private fun updateViewsVisibility(visible: Boolean) {
        with(binding) {
            yodleeProgress.visibleIf { visible }
            yodleeStatusLabel.visibleIf { visible }
            yodleeSubtitle.visibleIf { visible }
            yodleeIcon.visibleIf { visible }
        }
    }

    private fun navigator(): BankAuthFlowNavigator =
        (activity as? BankAuthFlowNavigator)
            ?: throw IllegalStateException("Parent must implement BankAuthFlowNavigator")

    companion object {
        private const val ATTRIBUTES: String = "ATTRIBUTES"
        private const val LINKING_BANK_ID: String = "LINKING_BANK_ID"

        fun newInstance(
            attributes: YodleeAttributes,
            bankId: String
        ): YodleeWebViewFragment = YodleeWebViewFragment().apply {
            arguments = Bundle().apply {
                putSerializable(ATTRIBUTES, attributes)
                putString(LINKING_BANK_ID, bankId)
            }
        }
    }
}

class YodleeWebClient(private val listener: YodleeWebClientInterface) : WebViewClient() {
    interface YodleeWebClientInterface {
        fun pageFinishedLoading()
    }

    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
        if (BuildConfig.DEBUG) {
            Timber.e("Yodlee SSL error: $error")
            handler?.proceed()
        }
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        listener.pageFinishedLoading()
    }

    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
        view.loadUrl(url)
        return true
    }
}

class FastLinkInterfaceHandler(
    private val listener: FastLinkListener,
    private val json: Json
) {

    interface FastLinkListener {
        fun flowSuccess(providerAccountId: String, accountId: String)
        fun flowError(error: FastLinkFlowError, reason: String? = null)
        fun openExternalUrl(url: String)
    }

    enum class FastLinkFlowError {
        JSON_PARSING,
        FLOW_QUIT_BY_USER,
        OTHER
    }

    @JavascriptInterface
    fun postMessage(data: String?) {
        if (data == null) return
        if (!data.isValidJSONObject()) {
            return
        }
        val message: FastLinkMessage = json.decodeFromString(data)
        val messageType = message.type ?: return
        val messageData = message.data ?: return

        if (messageType.equals(POST_MESSAGE, true) && messageData.action != null) {
            handlePostMessage(messageData)
        } else if (messageType.equals(OPEN_EXTERNAL_URL, true)) {
            messageData.externalUrl?.let {
                listener.openExternalUrl(it)
            }
        }
    }

    private fun handlePostMessage(data: MessageData) {
        data.action?.let {
            if (it.equals(EXIT_ACTION, true)) {
                handleExitAction(data)
            }
        }
    }

    private fun handleExitAction(data: MessageData) {
        if (data.sites?.isNotEmpty() == true && data.sites[0].status.equals(FLOW_SUCCESS, true)) {
            handleSitesSuccess(data.sites[0])
        } else if (data.status != null) {
            handleMessageStatus(data.status, data.reason)
        } else {
            listener.flowError(FastLinkFlowError.OTHER)
        }
    }

    private fun handleSitesSuccess(siteData: SiteData) {
        val accountId = siteData.accountId ?: kotlin.run {
            listener.flowError(FastLinkFlowError.OTHER)
            return
        }
        val providerAccountId: String = siteData.providerAccountId ?: kotlin.run {
            listener.flowError(FastLinkFlowError.OTHER)
            return
        }
        listener.flowSuccess(providerAccountId = providerAccountId, accountId = accountId)
    }

    private fun handleMessageStatus(status: String, reason: String?) {
        if (
            status.equals(FLOW_ABANDONED, true) ||
            status.equals(USER_CLOSE_ACTION, true)
        ) {
            listener.flowError(FastLinkFlowError.FLOW_QUIT_BY_USER, reason)
        } else
            listener.flowError(FastLinkFlowError.OTHER)
    }

    private fun String.isValidJSONObject(): Boolean {
        try {
            JSONObject(this)
        } catch (ex: JSONException) {
            return false
        }
        return true
    }

    companion object {
        // Message types
        private const val POST_MESSAGE = "POST_MESSAGE"
        private const val OPEN_EXTERNAL_URL = "OPEN_EXTERNAL_URL"

        // Handled actions
        private const val EXIT_ACTION = "exit"

        // Data statuses
        private const val FLOW_SUCCESS = "SUCCESS"
        private const val FLOW_ABANDONED = "ACTION_ABANDONED"
        private const val USER_CLOSE_ACTION = "USER_CLOSE_ACTION"
    }
}
