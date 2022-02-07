package piuk.blockchain.android.ui.home

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.RadioButton
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.componentlib.databinding.ToolbarGeneralBinding
import com.blockchain.nabu.BasicProfileInfo
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ActivityZendeskSubjectBinding
import zendesk.chat.Chat
import zendesk.chat.ChatConfiguration
import zendesk.chat.ChatEngine
import zendesk.chat.ChatProvidersConfiguration
import zendesk.chat.PreChatFormFieldStatus
import zendesk.chat.VisitorInfo
import zendesk.messaging.MessagingActivity

class ZendeskSubjectActivity : BlockchainActivity() {

    private val binding: ActivityZendeskSubjectBinding by lazy {
        ActivityZendeskSubjectBinding.inflate(layoutInflater)
    }

    private val userInformation by lazy {
        intent.getSerializableExtra(USER_INFO) as BasicProfileInfo
    }
    override val alwaysDisableScreenshots: Boolean
        get() = false

    override val toolbarBinding: ToolbarGeneralBinding
        get() = binding.toolbar

    private val subject: String by lazy {
        intent.getStringExtra(SUBJECT).orEmpty()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        updateToolbar(
            toolbarTitle = getString(R.string.contact_support),
            backAction = { onBackPressed() }
        )
        Chat.INSTANCE.init(applicationContext, BuildConfig.ZENDESK_API_KEY)

        setChatVisitorInfo()

        if (subject.isEmpty()) {
            with(binding) {
                zendeskOptions.setOnCheckedChangeListener { _, _ ->
                    zendeskContinue.isEnabled = true
                }

                zendeskContinue.setOnClickListener {
                    val checkedButton = findViewById<RadioButton>(zendeskOptions.checkedRadioButtonId)
                    setupChat(checkedButton.text.toString())
                }
            }
        } else {
            setupChat(subject)
        }
    }

    private fun setupChat(note: String) {
        Chat.INSTANCE.providers()?.profileProvider()?.apply {
            setVisitorNote(note)
            appendVisitorNote(note)
            addVisitorTags(listOf(note), null)
        }

        startChat()
        finish()
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
            .withName(userInformation.firstName)
            .withEmail(userInformation.email)
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

    companion object {
        private const val USER_INFO = "USER_INFO"
        private const val SUBJECT = "SUBJECT"
        private const val ZENDESK_CHANNEL = "wallet_sb_department"

        fun newInstance(context: Context, userInfo: BasicProfileInfo, subject: String = ""): Intent =
            Intent(context, ZendeskSubjectActivity::class.java).apply {
                putExtra(USER_INFO, userInfo)
                putExtra(SUBJECT, subject)
            }
    }
}
