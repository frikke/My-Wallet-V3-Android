package piuk.blockchain.android.ui.linkbank

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContract
import com.blockchain.banking.BankPaymentApproval
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.commonarch.presentation.base.SlidingModalBottomDialog
import com.blockchain.commonarch.presentation.mvi_v2.NavigationRouter
import com.blockchain.componentlib.databinding.FragmentActivityBinding
import com.blockchain.componentlib.databinding.ToolbarGeneralBinding
import com.blockchain.domain.paymentmethods.model.BankPartner
import com.blockchain.domain.paymentmethods.model.LinkBankTransfer
import com.blockchain.domain.paymentmethods.model.PlaidAttributes
import com.blockchain.domain.paymentmethods.model.YapilyAttributes
import com.blockchain.domain.paymentmethods.model.YapilyInstitution
import com.blockchain.domain.paymentmethods.model.YodleeAttributes
import com.blockchain.extensions.exhaustive
import com.blockchain.preferences.BankLinkingPrefs
import com.blockchain.presentation.koin.scopedInject
import com.blockchain.utils.consume
import com.plaid.link.OpenPlaidLink
import com.plaid.link.linkTokenConfiguration
import com.plaid.link.result.LinkExit
import com.plaid.link.result.LinkSuccess
import info.blockchain.balance.FiatCurrency
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.fraud.domain.service.FraudFlow
import piuk.blockchain.android.fraud.domain.service.FraudService
import piuk.blockchain.android.ui.dashboard.sheets.WireTransferAccountDetailsBottomSheet
import piuk.blockchain.android.ui.linkbank.BankAuthActivity.Companion.REFRESH_BANK_ACCOUNT_ID
import piuk.blockchain.android.ui.linkbank.BankAuthActivity.Companion.newBankRefreshInstance
import piuk.blockchain.android.ui.linkbank.presentation.openbanking.permission.OpenBankingPermissionFragment
import piuk.blockchain.android.ui.linkbank.presentation.openbanking.permission.OpenBankingPermissionNavEvent
import piuk.blockchain.android.ui.linkbank.yapily.YapilyBankSelectionFragment
import piuk.blockchain.android.ui.linkbank.yodlee.YodleeSplashFragment
import piuk.blockchain.android.ui.linkbank.yodlee.YodleeWebViewFragment

class BankAuthActivity :
    BlockchainActivity(),
    BankAuthFlowNavigator,
    SlidingModalBottomDialog.Host,
    NavigationRouter<OpenBankingPermissionNavEvent> {

    private val linkBankTransfer: LinkBankTransfer
        get() = intent.getSerializableExtra(LINK_BANK_TRANSFER_KEY) as LinkBankTransfer

    private val approvalDetails: BankPaymentApproval?
        get() = intent.getSerializableExtra(LINK_BANK_APPROVAL) as? BankPaymentApproval

    private val authSource: BankAuthSource
        get() = intent.getSerializableExtra(LINK_BANK_SOURCE) as BankAuthSource

    private val linkingId: String
        get() = intent.getStringExtra(LINK_BANK_ID) ?: ""

    private val isFromDeepLink: Boolean
        get() = intent.getBooleanExtra(LAUNCHED_FROM_DEEP_LINK, false)

    private val refreshBankAccountId: String?
        get() = intent.getStringExtra(REFRESH_BANK_ACCOUNT_ID)

    private val plaidResultLauncher = registerForActivityResult(OpenPlaidLink()) {
        when (it) {
            is LinkSuccess -> {
                refreshBankAccountId?.let { accountId ->
                    checkBankLinkingState(accountId)
                } ?: launchPlaidLinking(linkBankTransfer.id, it)
            }
            is LinkExit -> {
                onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    private val bankLinkingPrefs: BankLinkingPrefs by scopedInject()
    private val fraudService: FraudService by inject()

    private val binding: FragmentActivityBinding by lazy {
        FragmentActivityBinding.inflate(layoutInflater)
    }

    override val toolbarBinding: ToolbarGeneralBinding
        get() = binding.toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setupBackPress()

        var title = ""
        if (savedInstanceState == null) {
            when {
                isFromDeepLink -> {
                    title = getString(R.string.link_a_bank)
                    checkBankLinkingState(linkingId)
                }
                approvalDetails != null -> {

                    approvalDetails?.let {
                        title = getString(R.string.approve_payment)

                        yapilyApprovalAccepted(it)
                    } ?: launchBankLinkingWithError(BankAuthError.GenericError)
                }
                refreshBankAccountId != null -> {
                    title = getString(R.string.link_a_bank)
                    launchPlaidRefresh()
                }
                else -> {
                    title = getString(R.string.link_a_bank)
                    checkPartnerAndLaunchFlow(linkBankTransfer)
                }
            }
        }
        updateToolbar(
            toolbarTitle = title,
            backAction = { onSupportNavigateUp() }
        )
    }

    private fun checkBankLinkingState(linkingId: String) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.content_frame, BankAuthFragment.newInstance(linkingId, authSource))
            .commitAllowingStateLoss()
    }

    private fun checkPartnerAndLaunchFlow(linkBankTransfer: LinkBankTransfer) {
        when (linkBankTransfer.partner) {
            BankPartner.YODLEE -> {
                val attributes = linkBankTransfer.attributes as YodleeAttributes
                launchYodleeSplash(attributes, linkBankTransfer.id)
            }
            BankPartner.YAPILY -> {
                launchYapilyBankSelection(linkBankTransfer.attributes as YapilyAttributes)
            }
            BankPartner.PLAID -> {
                launchPlaidLink(
                    linkBankTransfer.attributes as PlaidAttributes,
                    linkBankTransfer.id
                )
            }
        }
    }

    override val alwaysDisableScreenshots: Boolean
        get() = false

    override fun launchYapilyBankSelection(attributes: YapilyAttributes) {
        fraudService.startFlow(FraudFlow.OB_LINK)
        supportFragmentManager.beginTransaction()
            .replace(R.id.content_frame, YapilyBankSelectionFragment.newInstance(attributes, authSource))
            .commitAllowingStateLoss()
    }

    override fun showTransferDetails() {
        showBottomSheet(WireTransferAccountDetailsBottomSheet.newInstance())
    }

    override fun yapilyInstitutionSelected(institution: YapilyInstitution, entity: String) {
        supportFragmentManager.beginTransaction()
            .replace(
                R.id.content_frame,

                OpenBankingPermissionFragment.newInstance(
                    institution = institution,
                    entity = entity,
                    authSource = authSource
                )

            )
            .addToBackStack(null)
            .commitAllowingStateLoss()
    }

    override fun launchPlaidLink(attributes: PlaidAttributes, id: String) {
        fraudService.startFlow(FraudFlow.ACH_LINK)
        plaidResultLauncher.launch(
            linkTokenConfiguration {
                token = attributes.linkToken
            }
        )
    }

    private fun launchPlaidLinking(id: String, linkSuccess: LinkSuccess) {
        supportFragmentManager.beginTransaction()
            .replace(
                R.id.content_frame,
                BankAuthFragment.newInstance(
                    accountProviderId = "",
                    accountId = id,
                    linkingBankId = linkSuccess.metadata.accounts.first().id,
                    linkingBankToken = linkSuccess.publicToken,
                    linkBankTransfer = linkBankTransfer,
                    authSource = authSource
                )
            )
            .addToBackStack(BankAuthFragment::class.simpleName)
            .commitAllowingStateLoss()
    }

    private fun launchPlaidRefresh() {
        supportFragmentManager.beginTransaction()
            .replace(
                R.id.content_frame,
                BankAuthFragment.newInstance(
                    refreshBankAccountId = refreshBankAccountId,
                    authSource = authSource
                )
            )
            .addToBackStack(BankAuthFragment::class.simpleName)
            .commitAllowingStateLoss()
    }

    override fun yapilyAgreementAccepted(institution: YapilyInstitution) {
        launchBankLinking(
            accountProviderId = "",
            accountId = institution.id,
            bankId = linkBankTransfer.id
        )
    }

    override fun yapilyApprovalAccepted(approvalDetails: BankPaymentApproval) {
        supportFragmentManager.beginTransaction()
            .replace(
                R.id.content_frame,
                BankAuthFragment.newInstance(approvalDetails, authSource)
            )
            .commitAllowingStateLoss()
    }

    override fun yapilyAgreementCancelled(isFromApproval: Boolean) =
        if (isFromApproval) {
            resetLocalState()
        } else {
            supportFragmentManager.popBackStack()
        }

    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(owner = this, enabled = approvalDetails != null) {
            resetLocalState()
        }
    }

    private fun resetLocalState() {
        bankLinkingPrefs.setBankLinkingState(BankAuthDeepLinkState().toPreferencesValue())
        setResult(Activity.RESULT_CANCELED)
        finish()
    }

    override fun route(navigationEvent: OpenBankingPermissionNavEvent) {
        when (navigationEvent) {
            is OpenBankingPermissionNavEvent.AgreementAccepted -> {
                launchBankLinking(
                    accountProviderId = "",
                    accountId = navigationEvent.institution.id,
                    bankId = linkBankTransfer.id
                )
            }

            OpenBankingPermissionNavEvent.AgreementDenied -> {
                supportFragmentManager.popBackStack()
            }
        }.exhaustive
    }

    override fun launchYodleeSplash(attributes: YodleeAttributes, bankId: String) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.content_frame, YodleeSplashFragment.newInstance(attributes, bankId))
            .commitAllowingStateLoss()
    }

    override fun launchYodleeWebview(attributes: YodleeAttributes, bankId: String) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.content_frame, YodleeWebViewFragment.newInstance(attributes, bankId))
            .addToBackStack(YodleeWebViewFragment::class.simpleName)
            .commitAllowingStateLoss()
    }

    override fun launchBankLinking(accountProviderId: String, accountId: String, bankId: String) {
        supportFragmentManager.beginTransaction()
            .replace(
                R.id.content_frame,
                BankAuthFragment.newInstance(
                    accountProviderId = accountProviderId,
                    accountId = accountId,
                    linkingBankId = bankId,
                    linkBankTransfer = linkBankTransfer,
                    authSource = authSource
                )
            )
            .addToBackStack(BankAuthFragment::class.simpleName)
            .commitAllowingStateLoss()
    }

    private fun launchBankLinkingWithError(errorState: BankAuthError) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.content_frame, BankAuthFragment.newInstance(errorState, authSource))
            .addToBackStack(BankAuthFragment::class.simpleName)
            .commitAllowingStateLoss()
    }

    override fun retry() {
        when {
            isFromDeepLink -> checkBankLinkingState(linkingId)
            approvalDetails != null -> approvalDetails?.let {
                yapilyApprovalAccepted(it)
            }
            else -> onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun bankLinkingFinished(bankId: String, currency: FiatCurrency) {
        val data = Intent()
        data.putExtra(LINKED_BANK_ID_KEY, bankId)
        data.putExtra(LINKED_BANK_CURRENCY, currency)
        refreshBankAccountId?.let { data.putExtra(REFRESH_BANK_ACCOUNT_ID, it) }
        setResult(RESULT_OK, data)
        finish()
    }

    override fun bankAuthCancelled() {
        resetLocalState()
    }

    override fun onSupportNavigateUp(): Boolean = consume {
        onBackPressedDispatcher.onBackPressed()
    }

    override fun onSheetClosed() {
        // do nothing
    }

    override fun onDestroy() {
        fraudService.endFlows(FraudFlow.ACH_LINK, FraudFlow.OB_LINK)
        super.onDestroy()
    }

    companion object {
        private const val LINK_BANK_TRANSFER_KEY = "LINK_BANK_TRANSFER_KEY"
        private const val LINK_BANK_ID = "LINK_BANK_TRANSFER_KEY"
        private const val LINK_BANK_SOURCE = "LINK_BANK_SOURCE"
        private const val LINK_BANK_APPROVAL = "LINK_BANK_APPROVAL"
        private const val LAUNCHED_FROM_DEEP_LINK = "LAUNCHED_FROM_DEEP_LINK"
        const val LINK_BANK_REQUEST_CODE = 999
        const val LINKED_BANK_ID_KEY = "LINKED_BANK_ID"
        const val LINKED_BANK_CURRENCY = "LINKED_BANK_CURRENCY"
        const val REFRESH_BANK_ACCOUNT_ID = "REFRESH_BANK_ACCOUNT_ID"

        fun newInstance(
            linkBankTransfer: LinkBankTransfer,
            authSource: BankAuthSource,
            context: Context,
        ): Intent {
            val intent = Intent(context, BankAuthActivity::class.java)
            intent.putExtra(LINK_BANK_TRANSFER_KEY, linkBankTransfer)
            intent.putExtra(LINK_BANK_SOURCE, authSource)
            return intent
        }

        fun newInstance(linkingId: String, authSource: BankAuthSource, context: Context): Intent {
            val intent = Intent(context, BankAuthActivity::class.java)
            intent.putExtra(LINK_BANK_ID, linkingId)
            intent.putExtra(LAUNCHED_FROM_DEEP_LINK, true)
            intent.putExtra(LINK_BANK_SOURCE, authSource)
            return intent
        }

        fun newBankRefreshInstance(
            accountId: String,
            authSource: BankAuthSource,
            context: Context,
        ): Intent {
            val intent = Intent(context, BankAuthActivity::class.java)
            intent.putExtra(REFRESH_BANK_ACCOUNT_ID, accountId)
            intent.putExtra(LAUNCHED_FROM_DEEP_LINK, false)
            intent.putExtra(LINK_BANK_SOURCE, authSource)
            return intent
        }

        fun newInstance(approvalData: BankPaymentApproval, authSource: BankAuthSource, context: Context): Intent {
            val intent = Intent(context, BankAuthActivity::class.java)
            intent.putExtra(LINK_BANK_APPROVAL, approvalData)
            intent.putExtra(LINK_BANK_SOURCE, authSource)
            return intent
        }
    }
}

class BankAuthRefreshContract : ActivityResultContract<Pair<String, BankAuthSource>, Boolean>() {

    override fun createIntent(context: Context, input: Pair<String, BankAuthSource>): Intent =
        newBankRefreshInstance(input.first, input.second, context)

    override fun parseResult(resultCode: Int, intent: Intent?): Boolean =
        intent?.getStringExtra(REFRESH_BANK_ACCOUNT_ID) != null
}
