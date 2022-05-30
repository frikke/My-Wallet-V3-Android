package piuk.blockchain.android.support

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import com.blockchain.commonarch.presentation.mvi.MviActivity
import com.blockchain.componentlib.alert.BlockchainSnackbar
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.componentlib.databinding.ToolbarGeneralBinding
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.koin.scopedInject
import com.blockchain.nabu.BasicProfileInfo
import io.intercom.android.sdk.Intercom
import io.intercom.android.sdk.UserAttributes
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ActivitySupportCentreBinding
import piuk.blockchain.android.ui.BottomSheetInformation
import zendesk.chat.Chat
import zendesk.chat.ChatConfiguration
import zendesk.chat.ChatEngine
import zendesk.chat.ChatProvidersConfiguration
import zendesk.chat.PreChatFormFieldStatus
import zendesk.chat.VisitorInfo
import zendesk.messaging.MessagingActivity

class SupportCentreActivity :
    MviActivity<SupportModel, SupportIntent, SupportState, ActivitySupportCentreBinding>(),
    SupportCentreTopicSheet.Host,
    BottomSheetInformation.Host {

    override val model: SupportModel by scopedInject()

    private var userInfo: BasicProfileInfo? = null
    private var isIntercomEnabled = false

    override val alwaysDisableScreenshots: Boolean
        get() = false

    override val toolbarBinding: ToolbarGeneralBinding
        get() = binding.toolbar

    private val subject: String by lazy {
        intent.getStringExtra(SUBJECT).orEmpty()
    }

    override fun initBinding(): ActivitySupportCentreBinding =
        ActivitySupportCentreBinding.inflate(layoutInflater)

    override fun render(newState: SupportState) {
        if (newState.viewState != SupportViewState.None) {
            when (val state = newState.viewState) {
                SupportViewState.Loading -> {
                    binding.progress.visible()
                }
                is SupportViewState.ShowInfo -> {
                    userInfo = state.userInfo.basicInfo
                    with(binding) {
                        if (state.userInfo.isUserGold) {
                            supportCentreWebview.loadUrl(URL_BLOCKCHAIN_SUPPORT_PORTAL)
                            openChatCta.visible()

                            if (state.userInfo.isIntercomEnabled) {
                                isIntercomEnabled = true
                                val userAttributes = UserAttributes.Builder()
                                    .withName(state.userInfo.basicInfo.firstName)
                                    .withEmail(state.userInfo.basicInfo.email)
                                    .build()
                                Intercom.client().updateUser(userAttributes)
                                // start intercom right away but leave the old functionality behind it
                                Intercom.client().displayMessenger()
                            } else {
                                setChatVisitorInfo()
                            }
                        } else {
                            supportCentreWebview.loadUrl(URL_CONTACT_SUPPORT)
                        }
                    }
                }
                SupportViewState.None -> {
                    // do nothing
                }
                is SupportViewState.TopicSelected -> {
                    if (newState.supportError == SupportError.ErrorStartingChat &&
                        newState.crashErrorCount >= MAX_SETUP_RETRIES
                    ) {
                        showBottomSheet(
                            BottomSheetInformation.newInstance(
                                title = getString(R.string.customer_support_error_title),
                                description = getString(R.string.customer_support_error_description),
                                ctaPrimaryText = getString(R.string.customer_support_error_cta),
                                ctaSecondaryText = getString(R.string.common_close)
                            )
                        )
                    } else {
                        setupChat(state.topic)
                    }
                }
            }

            model.process(SupportIntent.ResetViewState)
        }

        if (newState.supportError != SupportError.None) {
            when (newState.supportError) {
                SupportError.ErrorLoadingProfileInfo -> {
                    with(binding) {
                        supportCentreWebview.loadUrl(URL_CONTACT_SUPPORT)
                        progress.gone()
                        BlockchainSnackbar.make(
                            root, getString(R.string.settings_contact_support_error), type = SnackbarType.Error
                        ).show()
                    }
                }
                SupportError.None -> {
                    // do nothing
                }
                SupportError.ErrorStartingChat -> {
                    // do nothing, this is handled above
                }
            }

            model.process(SupportIntent.ResetErrorState)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        updateToolbar(
            toolbarTitle = getString(R.string.contact_support),
            backAction = { onBackPressed() }
        )

        Chat.INSTANCE.init(applicationContext, BuildConfig.ZENDESK_API_KEY)

        binding.supportCentreWebview.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            setSupportMultipleWindows(true)
            loadsImagesAutomatically = true
            javaScriptCanOpenWindowsAutomatically = true
            cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
        }

        binding.supportCentreWebview.apply {
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    binding.progress.gone()
                }
            }
        }
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)

        model.process(SupportIntent.LoadUserInfo)

        binding.openChatCta.apply {
            text = getString(R.string.contact_support)
            onClick = {
                if (subject.isEmpty()) {
                    showBottomSheet(SupportCentreTopicSheet.newInstance())
                } else {
                    setupChat(subject)
                }
            }
        }
    }

    override fun onTopicSelected(topic: String) {
        model.process(SupportIntent.OnTopicSelected(topic))
    }

    private fun setupChat(note: String) {
        if (isIntercomEnabled) {
            Intercom.client().displayMessenger()
        } else {
            try {
                Chat.INSTANCE.providers()?.profileProvider()?.apply {
                    setVisitorNote(note)
                    appendVisitorNote(note)
                    addVisitorTags(listOf(note), null)
                }

                startChat()
            } catch (t: Throwable) {
                model.process(SupportIntent.UpdateStartingChatError)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun startChat() {
        MessagingActivity.builder()
            .withMultilineResponseOptionsEnabled(true)
            .withEngines(ChatEngine.engine())
            .withBotAvatarDrawable(R.drawable.ic_framed_app_icon)
            .withBotLabelString(getString(R.string.zendesk_bot_name))
            .withToolbarTitle(getString(R.string.zendesk_window_title))
            .show(this, getChatConfiguration())
    }

    private fun setChatVisitorInfo() {
        val visitorInfo = VisitorInfo.builder()
            .withName(userInfo?.firstName ?: "Anonymous")
            .withEmail(userInfo?.email ?: "Unknown email")
            .build()

        val chatProvidersConfiguration = ChatProvidersConfiguration.builder()
            .withVisitorInfo(visitorInfo)
            .withDepartment(ZENDESK_CHANNEL)
            .build()

        Chat.INSTANCE.chatProvidersConfiguration = chatProvidersConfiguration
    }

    private fun getChatConfiguration() = ChatConfiguration.builder()
        .withAgentAvailabilityEnabled(true)
        .withPreChatFormEnabled(true)
        .withDepartmentFieldStatus(PreChatFormFieldStatus.HIDDEN)
        .withPhoneFieldStatus(PreChatFormFieldStatus.HIDDEN)
        .withNameFieldStatus(PreChatFormFieldStatus.HIDDEN)
        .withEmailFieldStatus(PreChatFormFieldStatus.HIDDEN)
        .withOfflineFormEnabled(true)
        .build()

    override fun primaryButtonClicked() {
        binding.supportCentreWebview.loadUrl(URL_CONTACT_SUPPORT)
    }

    override fun secondButtonClicked() {
        // do nothing
    }

    override fun onSheetClosed() {
        // do nothing
    }

    companion object {
        private const val SUBJECT = "SUBJECT"
        private const val ZENDESK_CHANNEL = "wallet_sb_department"
        private const val URL_BLOCKCHAIN_SUPPORT_PORTAL = "https://support.blockchain.com/"
        private const val URL_CONTACT_SUPPORT = "https://support.blockchain.com/hc/requests/new"
        private const val MAX_SETUP_RETRIES = 3

        fun newIntent(context: Context, subject: String = ""): Intent =
            Intent(context, SupportCentreActivity::class.java).apply {
                putExtra(SUBJECT, subject)
            }
    }
}
