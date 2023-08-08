package piuk.blockchain.android.ui.linkbank

import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.annotation.StringRes
import com.blockchain.commonarch.presentation.mvi.MviFragment
import com.blockchain.componentlib.alert.BlockchainSnackbar
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.componentlib.button.common.BaseButtonView
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.domain.paymentmethods.model.BankAuthError
import com.blockchain.domain.paymentmethods.model.BankAuthSource
import com.blockchain.domain.paymentmethods.model.BankLinkingProcessState
import com.blockchain.domain.paymentmethods.model.BankPartner
import com.blockchain.domain.paymentmethods.model.BankPaymentApproval
import com.blockchain.domain.paymentmethods.model.LinkBankTransfer
import com.blockchain.domain.paymentmethods.model.LinkedBank
import com.blockchain.domain.paymentmethods.model.PlaidAttributes
import com.blockchain.domain.paymentmethods.model.YapilyAttributes
import com.blockchain.domain.paymentmethods.model.YodleeAttributes
import com.blockchain.extensions.exhaustive
import com.blockchain.presentation.koin.scopedInject
import com.blockchain.utils.unsafeLazy
import info.blockchain.balance.FiatCurrency
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.FragmentLinkABankBinding
import piuk.blockchain.android.support.SupportCentreActivity
import piuk.blockchain.android.urllinks.URL_YODLEE_SUPPORT_LEARN_MORE
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.android.util.loadRemoteErrorAndStatusIcons
import piuk.blockchain.android.util.loadRemoteErrorIcon

class BankAuthFragment : MviFragment<BankAuthModel, BankAuthIntent, BankAuthState, FragmentLinkABankBinding>() {

    override val model: BankAuthModel by scopedInject()

    private val isFromDeepLink: Boolean by lazy {
        arguments?.getBoolean(FROM_DEEP_LINK, false) ?: false
    }

    private val isForApproval: Boolean by lazy {
        arguments?.getBoolean(FOR_APPROVAL, false) ?: false
    }

    private val accountProviderId: String by lazy {
        arguments?.getString(ACCOUNT_PROVIDER_ID).orEmpty()
    }

    private val accountId: String by lazy {
        arguments?.getString(ACCOUNT_ID).orEmpty()
    }

    private val approvalData: BankPaymentApproval? by unsafeLazy {
        arguments?.getSerializable(APPROVAL_DATA) as? BankPaymentApproval
    }

    private val linkingBankId: String by lazy {
        arguments?.getString(LINKING_BANK_ID).orEmpty()
    }

    private val linkingBankToken: String by lazy {
        arguments?.getString(LINKING_BANK_TOKEN).orEmpty()
    }

    private val errorState: BankAuthError? by unsafeLazy {
        arguments?.getSerializable(ERROR_STATE) as? BankAuthError
    }

    private val authSource: BankAuthSource by lazy {
        arguments?.getSerializable(LINK_BANK_SOURCE) as BankAuthSource
    }

    private val linkBankTransfer: LinkBankTransfer? by unsafeLazy {
        arguments?.getSerializable(LINK_BANK_TRANSFER) as? LinkBankTransfer
    }

    private val refreshBankAccountId: String? by lazy {
        arguments?.getString(REFRESH_BANK_ACCOUNT_ID)
    }

    private var hasChosenExternalApp: Boolean = false
    private var hasExternalLinkingLaunched: Boolean = false

    override fun onAttach(context: Context) {
        super.onAttach(context)
        setupBackPress()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity.updateToolbar(
            toolbarTitle = if (isForApproval) {
                getString(com.blockchain.stringResources.R.string.approve_payment)
            } else {
                getString(com.blockchain.stringResources.R.string.link_a_bank)
            }
        )

        if (savedInstanceState == null) {
            when {
                isForApproval -> {
                    approvalData?.let {
                        model.process(
                            BankAuthIntent.UpdateForApproval(
                                authorisationUrl = it.authorisationUrl,
                                callbackPath = it.linkedBank.callbackPath
                            )
                        )
                    }
                }

                isFromDeepLink -> {
                    model.process(BankAuthIntent.GetLinkedBankState(linkingBankId, isFromDeepLink))
                }

                refreshBankAccountId != null -> {
                    model.process(BankAuthIntent.RefreshPlaidAccount(refreshBankAccountId))
                }

                else -> {
                    startBankAuthentication()
                }
            }
        }

        with(binding) {
            mainCta.apply {
                text = getString(com.blockchain.stringResources.R.string.common_retry)
                onClick = {
                    logRetryLaunchAnalytics()
                    mainCta.gone()
                    secondaryCta.gone()
                    startBankAuthentication()
                }
            }

            secondaryCta.apply {
                text = getString(com.blockchain.stringResources.R.string.yodlee_linking_cancel)
            }
        }
    }

    private fun setupBackPress() {
        activity.onBackPressedDispatcher.addCallback(owner = this) {
            navigator().bankAuthCancelled()
        }
    }

    override fun onResume() {
        super.onResume()
        if (hasExternalLinkingLaunched && !hasChosenExternalApp) {
            showAppOpeningRetry()
        }
    }

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentLinkABankBinding =
        FragmentLinkABankBinding.inflate(inflater, container, false)

    override fun render(newState: BankAuthState) {
        when (newState.bankLinkingProcessState) {
            BankLinkingProcessState.CANCELED -> {
                navigator().bankAuthCancelled()
                return
            }

            BankLinkingProcessState.LINKING -> showLinkingInProgress(newState.linkBankTransfer)
            BankLinkingProcessState.ACTIVATING -> showActivationInProgress()
            BankLinkingProcessState.IN_EXTERNAL_FLOW -> {
                newState.linkBankTransfer?.attributes?.let {
                    if (it is YapilyAttributes) {
                        showExternalFlow(it)
                    }
                }
            }

            BankLinkingProcessState.IN_REFRESH_FLOW -> {
                showLoading()
                newState.refreshBankInfo?.let {
                    navigator().launchPlaidLink(
                        PlaidAttributes(
                            linkToken = it.linkToken,
                            linkUrl = it.linkUrl,
                            tokenExpiresAt = it.tokenExpiresAt
                        ),
                        id = it.id
                    )
                }
            }

            BankLinkingProcessState.APPROVAL -> {
                approvalData?.let {
                    showApprovalInProgress(it.linkedBank)
                }
            }

            BankLinkingProcessState.APPROVAL_WAIT -> showBankApproval()
            BankLinkingProcessState.LINKING_SUCCESS -> processLinkingSuccess(newState)
            BankLinkingProcessState.NONE -> {
                // do nothing
            }
        }.exhaustive

        if (!newState.linkBankUrl.isNullOrEmpty()) {
            handleExternalLinking(newState)
        }

        val error = newState.errorState ?: errorState
        error?.let {
            showErrorState(
                errorState = it,
                partner = newState.linkBankTransfer?.partner,
                bankId = newState.linkBankTransfer?.id.orEmpty()
            )
        }
    }

    private fun processLinkingSuccess(state: BankAuthState) {
        if (!isForApproval) {
            state.linkedBank?.let {
                showLinkingSuccess(
                    label = it.accountName,
                    id = it.id,
                    partner = it.partner,
                    currency = it.currency
                )
            }
        }
    }

    private fun startBankAuthentication() {
        if (isForApproval) {
            approvalData?.let {
                model.process(BankAuthIntent.UpdateForApproval(it.authorisationUrl, it.linkedBank.callbackPath))
            }
        } else {
            linkBankTransfer?.let {
                if (it.partner == BankPartner.PLAID) {
                    model.process(
                        BankAuthIntent.LinkPlaidAccount(
                            accountId = accountId,
                            linkBankAccountId = linkingBankId,
                            linkBankToken = linkingBankToken,
                            linkBankTransfer = it,
                            authSource = authSource
                        )
                    )
                } else {
                    model.process(
                        BankAuthIntent.UpdateAccountProvider(
                            accountProviderId,
                            accountId,
                            linkingBankId,
                            it,
                            authSource
                        )
                    )
                }
            }
        }
    }

    private fun showAppOpeningRetry() {
        with(binding) {
            mainCta.visible()
            linkBankProgress.gone()
            linkBankStateIndicator.visible()
            linkBankTitle.text = getString(com.blockchain.stringResources.R.string.yapily_bank_link_choice_error_title)
            linkBankSubtitle.text =
                getString(com.blockchain.stringResources.R.string.yapily_bank_link_choice_error_subtitle)
            linkBankStateIndicator.setImageResource(R.drawable.ic_alert_white_bkgd)
        }
        model.process(BankAuthIntent.ResetBankLinking)
        hasExternalLinkingLaunched = false
    }

    private fun handleExternalLinking(newState: BankAuthState) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(newState.linkBankUrl))

        val receiver = ChooserReceiver()
        receiver.registerListener(object : ChooserCallback {
            override fun onItemChosen() {
                hasChosenExternalApp = true
                if (isForApproval) {
                    model.process(BankAuthIntent.StartBankApproval(newState.callbackPathUrl))
                } else {
                    model.process(BankAuthIntent.StartBankLinking)
                }
            }
        })
        val receiverIntent = Intent(context, receiver.javaClass)
        val pendingIntent =
            PendingIntent.getBroadcast(
                context,
                0,
                receiverIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        try {
            startActivity(
                Intent.createChooser(
                    intent,
                    getString(com.blockchain.stringResources.R.string.yapily_bank_link_app_choice_title),
                    pendingIntent.intentSender
                )
            )
            hasExternalLinkingLaunched = true
            model.process(BankAuthIntent.ClearBankLinkingUrl)
        } catch (e: ActivityNotFoundException) {
            model.process(BankAuthIntent.ErrorIntent())
            BlockchainSnackbar.make(
                binding.root,
                getString(com.blockchain.stringResources.R.string.yapily_bank_link_no_apps),
                type = SnackbarType.Error
            ).show()
        }
    }

    private fun showBankApproval() {
        showLoading()
        setTitleAndSubtitle(
            getString(
                com.blockchain.stringResources.R.string.yapily_linking_in_progress_title,
                approvalData?.linkedBank?.accountName ?: getString(
                    com.blockchain.stringResources.R.string.yapily_linking_default_bank
                )
            ),
            getString(com.blockchain.stringResources.R.string.yapily_approval_in_progress_subtitle)
        )

        binding.mainCta.visible()
        showCancelButton(com.blockchain.stringResources.R.string.cancel_order)
    }

    private fun showActivationInProgress() {
        showLoading()
        binding.linkBankIcon.setImageResource(R.drawable.ic_blockchain_blue_circle)
        setTitleAndSubtitle(
            getString(com.blockchain.stringResources.R.string.yapily_activating_title),
            getString(com.blockchain.stringResources.R.string.yapily_activating_subtitle)
        )
    }

    private fun showApprovalInProgress(linkedBank: LinkedBank) {
        showLoading()
        setTitleAndSubtitle(
            getString(com.blockchain.stringResources.R.string.yapily_approving_title, linkedBank.bankName),
            getString(com.blockchain.stringResources.R.string.yapily_approving_subtitle)
        )
        model.process(BankAuthIntent.ClearApprovalState)
    }

    private fun showExternalFlow(attrs: YapilyAttributes) {
        showLoading()
        showCancelButton(com.blockchain.stringResources.R.string.common_cancel)
        hasChosenExternalApp = false
        setTitleAndSubtitle(
            getString(
                com.blockchain.stringResources.R.string.yapily_linking_in_progress_title,
                attrs.institutionList.find { institution -> institution.id == accountId }?.name
                    ?: getString(com.blockchain.stringResources.R.string.yapily_linking_default_bank)
            ),
            getString(com.blockchain.stringResources.R.string.yapily_linking_in_progress_subtitle)
        )
        model.process(BankAuthIntent.GetLinkedBankState(linkingBankId))
    }

    private fun setTitleAndSubtitle(title: String, subtitle: String) {
        with(binding) {
            linkBankTitle.text = title
            linkBankSubtitle.text = subtitle
        }
    }

    private fun showLoading() {
        with(binding) {
            linkBankProgress.visible()
            linkBankStateIndicator.gone()
        }
    }

    private fun setButtonsForErrors(errorState: BankAuthError, partner: BankPartner?, linkBankTransferId: String) {
        when (errorState) {
            BankAuthError.LinkedBankAlreadyLinked -> {
                showGoBackButton(binding.mainCta) {
                    logRetryAnalytics(errorState, partner)
                    navigateBack(partner, linkBankTransferId)
                }
            }

            BankAuthError.LinkedBankInvalid,
            BankAuthError.LinkedBankAccountUnsupported
            -> {
                showGoBackButton(binding.mainCta) {
                    navigateBack(partner, linkBankTransferId)
                }
            }

            BankAuthError.LinkedBankInfoNotFound,
            BankAuthError.LinkedBankInternalFailure,
            BankAuthError.LinkedBankFailure,
            BankAuthError.LinkedBankFraud,
            BankAuthError.LinkedBankRejected
            -> {
                binding.mainCta.apply {
                    text = getString(com.blockchain.stringResources.R.string.bank_linking_try_again)
                    visible()
                    onClick = {
                        logRetryAnalytics(errorState, partner)
                        retryLinking(partner, linkBankTransferId)
                    }
                }
                showGoBackButton(binding.secondaryCta) {
                    logCancelAnalytics(errorState, partner)
                    navigator().bankAuthCancelled()
                }
            }

            BankAuthError.LinkedBankExpired -> {
                binding.mainCta.apply {
                    text = getString(com.blockchain.stringResources.R.string.bank_linking_try_another_method)
                    visible()
                    onClick = {
                        logRetryAnalytics(errorState, partner)
                        retryLinking(partner, linkBankTransferId)
                    }
                }
                showGoBackButton(binding.secondaryCta) {
                    logCancelAnalytics(errorState, partner)
                    navigator().bankAuthCancelled()
                }
            }

            BankAuthError.LinkedBankNamesMismatched -> {
                binding.mainCta.apply {
                    text = getString(com.blockchain.stringResources.R.string.bank_linking_try_different_account)
                    visible()
                    onClick = {
                        logRetryAnalytics(errorState, partner)
                        retryLinking(partner, linkBankTransferId)
                    }
                }
                showGoBackButton(binding.secondaryCta) {
                    logCancelAnalytics(errorState, partner)
                    navigator().bankAuthCancelled()
                }
            }

            else -> {
                showGoBackButton(binding.mainCta) {
                    navigator().bankAuthCancelled()
                }
            }
        }
    }

    private fun navigateBack(partner: BankPartner?, bankId: String) {
        when (partner) {
            BankPartner.YAPILY ->
                navigator().launchYapilyBankSelection(linkBankTransfer?.attributes as YapilyAttributes)

            BankPartner.YODLEE ->
                navigator().launchYodleeSplash(linkBankTransfer?.attributes as YodleeAttributes, bankId)

            BankPartner.PLAID ->
                navigator().bankAuthCancelled()

            null ->
                navigator().bankAuthCancelled()
        }.exhaustive
    }

    private fun retryLinking(partner: BankPartner?, bankId: String) {
        when (partner) {
            BankPartner.YAPILY ->
                navigator().launchYapilyBankSelection(linkBankTransfer?.attributes as YapilyAttributes)

            BankPartner.YODLEE ->
                navigator().launchYodleeSplash(linkBankTransfer?.attributes as YodleeAttributes, bankId)

            BankPartner.PLAID ->
                navigator().launchPlaidLink(linkBankTransfer?.attributes as PlaidAttributes, bankId)

            null ->
                navigator().bankAuthCancelled()
        }.exhaustive
    }

    private fun showErrorState(errorState: BankAuthError, partner: BankPartner?, bankId: String) {
        setErrorIcons(errorState)
        setButtonsForErrors(errorState, partner, bankId)

        when (errorState) {
            BankAuthError.BankLinkingTimeout -> {
                setTitleAndSubtitle(
                    getString(com.blockchain.stringResources.R.string.bank_linking_timeout_error_title),
                    getString(com.blockchain.stringResources.R.string.bank_linking_timeout_error_subtitle)
                )
            }

            BankAuthError.LinkedBankInvalid,
            BankAuthError.LinkedBankAccountUnsupported
            -> {
                logAnalytics(BankAuthAnalytics.INCORRECT_ACCOUNT, partner)
                when (partner) {
                    BankPartner.YODLEE, BankPartner.PLAID ->
                        setTitleAndSubtitle(
                            getString(com.blockchain.stringResources.R.string.bank_linking_account_ach_error_title),
                            getString(com.blockchain.stringResources.R.string.bank_linking_account_ach_error_subtitle)
                        )

                    BankPartner.YAPILY, null ->
                        setTitleAndSubtitle(
                            getString(com.blockchain.stringResources.R.string.bank_linking_account_ob_error_title),
                            getString(com.blockchain.stringResources.R.string.bank_linking_account_ob_error_subtitle)
                        )
                }.exhaustive
            }

            BankAuthError.LinkedBankInfoNotFound,
            BankAuthError.LinkedBankInternalFailure,
            BankAuthError.LinkedBankFailure,
            BankAuthError.LinkedBankFraud
            -> {
                when (partner) {
                    BankPartner.YODLEE, BankPartner.PLAID ->
                        setTitleAndSubtitle(
                            getString(com.blockchain.stringResources.R.string.bank_linking_failed_ob_title),
                            getString(com.blockchain.stringResources.R.string.bank_linking_failed_subtitle)
                        )

                    BankPartner.YAPILY, null ->
                        setTitleAndSubtitle(
                            getString(com.blockchain.stringResources.R.string.bank_linking_failed_ach_title),
                            getString(com.blockchain.stringResources.R.string.bank_linking_failed_subtitle)
                        )
                }.exhaustive
            }

            BankAuthError.LinkedBankAlreadyLinked -> {
                logAnalytics(BankAuthAnalytics.ALREADY_LINKED, partner)
                setTitleAndSubtitle(
                    getString(com.blockchain.stringResources.R.string.bank_linking_already_linked_error_title),
                    getString(com.blockchain.stringResources.R.string.bank_linking_already_linked_error_subtitle)
                )
            }

            BankAuthError.LinkedBankNamesMismatched -> {
                logAnalytics(BankAuthAnalytics.ACCOUNT_MISMATCH, partner)
                with(binding) {
                    linkBankTitle.text =
                        getString(com.blockchain.stringResources.R.string.bank_linking_mismatched_title)

                    val linksMap = mapOf<String, Uri>(
                        "learn_more" to Uri.parse(URL_YODLEE_SUPPORT_LEARN_MORE)
                    )

                    val text = StringUtils.getStringWithMappedAnnotations(
                        requireContext(),
                        com.blockchain.stringResources.R.string.bank_linking_mismatched_subtitle,
                        linksMap
                    )
                    linkBankSubtitle.text = text
                    linkBankSubtitle.movementMethod = LinkMovementMethod.getInstance()
                }
            }

            BankAuthError.LinkedBankExpired -> {
                when (partner) {
                    BankPartner.YODLEE, BankPartner.PLAID ->
                        setTitleAndSubtitle(
                            getString(com.blockchain.stringResources.R.string.bank_linking_expired_ach_title),
                            getString(com.blockchain.stringResources.R.string.bank_linking_expired_subtitle)
                        )

                    BankPartner.YAPILY, null ->
                        setTitleAndSubtitle(
                            getString(com.blockchain.stringResources.R.string.bank_linking_expired_ob_title),
                            getString(com.blockchain.stringResources.R.string.bank_linking_expired_subtitle)
                        )
                }.exhaustive
            }

            BankAuthError.LinkedBankRejected -> {
                with(binding) {
                    linkBankTitle.text = getString(com.blockchain.stringResources.R.string.bank_linking_rejected_title)

                    val text = StringUtils.getStringWithMappedAnnotations(
                        requireContext(),
                        com.blockchain.stringResources.R.string.bank_linking_rejected_subtitle,
                        emptyMap(),
                        onClick = {
                            requireActivity().startActivity(SupportCentreActivity.newIntent(requireContext()))
                        }
                    )

                    linkBankSubtitle.text = text
                    linkBankSubtitle.movementMethod = LinkMovementMethod.getInstance()
                }
            }

            is BankAuthError.ServerSideDrivenLinkedBankError -> setTitleAndSubtitle(
                errorState.title,
                errorState.message
            )

            else -> {
                logAnalytics(BankAuthAnalytics.GENERIC_ERROR, partner)
                when (partner) {
                    BankPartner.YODLEE, BankPartner.PLAID ->
                        setTitleAndSubtitle(
                            getString(com.blockchain.stringResources.R.string.bank_linking_failed_ach_title),
                            getString(com.blockchain.stringResources.R.string.bank_linking_failed_subtitle)
                        )

                    BankPartner.YAPILY, null ->
                        setTitleAndSubtitle(
                            getString(com.blockchain.stringResources.R.string.bank_linking_failed_ob_title),
                            getString(com.blockchain.stringResources.R.string.bank_linking_failed_subtitle)
                        )
                }.exhaustive
            }
        }
    }

    private fun setErrorIcons(
        state: BankAuthError
    ) {
        with(binding) {
            linkBankProgress.gone()
            if (state is BankAuthError.ServerSideDrivenLinkedBankError) {
                when {
                    // we have been provided both icon and status
                    state.iconUrl.isNotEmpty() && state.statusIconUrl.isNotEmpty() -> {
                        requireContext().loadRemoteErrorAndStatusIcons(
                            state.iconUrl,
                            state.statusIconUrl,
                            onIconLoadSuccess = { drawable ->
                                linkBankIcon.setImageDrawable(drawable)
                            },
                            onIconLoadError = {
                                showDefaultErrorIcons(state)
                            },
                            onStatusIconLoadSuccess = { drawable ->
                                linkBankStateIndicator.apply {
                                    setImageDrawable(drawable)
                                    visible()
                                }
                            },
                            onStatusIconLoadError = {
                                showDefaultErrorIcons(state)
                            }
                        )
                    }
                    // we only have one icon
                    state.iconUrl.isNotEmpty() && state.statusIconUrl.isEmpty() -> {
                        requireContext().loadRemoteErrorIcon(
                            state.iconUrl,
                            onIconLoadSuccess = { drawable ->
                                linkBankIcon.setImageDrawable(drawable)
                            },
                            onIconLoadError = {
                                showDefaultErrorIcons(state)
                            }
                        )
                    }
                    // no icons provided
                    else -> {
                        showDefaultErrorIcons(state)
                    }
                }
            } else {
                showDefaultErrorIcons(state)
            }
        }
    }

    private fun FragmentLinkABankBinding.showDefaultErrorIcons(state: BankAuthError) {
        linkBankIcon.setImageResource(R.drawable.ic_bank_details_big)
        linkBankStateIndicator.apply {
            setImageResource(
                when (state) {
                    BankAuthError.LinkedBankAlreadyLinked -> R.drawable.ic_question_mark_white_bkgd
                    BankAuthError.LinkedBankRejected -> R.drawable.ic_cross_white_bckg
                    else -> R.drawable.ic_alert_white_bkgd
                }
            )
            visible()
        }
    }

    private fun logRetryAnalytics(state: BankAuthError, partner: BankPartner?) =
        logAnalytics(
            when (state) {
                BankAuthError.LinkedBankAlreadyLinked -> BankAuthAnalytics.ALREADY_LINKED_RETRY
                BankAuthError.LinkedBankInfoNotFound -> BankAuthAnalytics.INCORRECT_ACCOUNT_RETRY
                BankAuthError.LinkedBankNamesMismatched -> BankAuthAnalytics.ACCOUNT_MISMATCH_RETRY
                else -> BankAuthAnalytics.GENERIC_ERROR_RETRY
            },
            partner
        )

    private fun logCancelAnalytics(state: BankAuthError, partner: BankPartner?) =
        logAnalytics(
            when (state) {
                BankAuthError.LinkedBankAlreadyLinked -> BankAuthAnalytics.ALREADY_LINKED_CANCEL
                BankAuthError.LinkedBankInfoNotFound -> BankAuthAnalytics.INCORRECT_ACCOUNT_CANCEL
                BankAuthError.LinkedBankNamesMismatched -> BankAuthAnalytics.ACCOUNT_MISMATCH_CANCEL
                else -> BankAuthAnalytics.GENERIC_ERROR_CANCEL
            },
            partner
        )

    private fun logAnalytics(event: BankAuthAnalytics, partner: BankPartner?) {
        partner?.let {
            analytics.logEvent(bankAuthEvent(event, it))
        }
    }

    private fun showLinkingInProgress(linkBank: LinkBankTransfer?) {
        with(binding) {
            linkBankIcon.setImageResource(R.drawable.ic_blockchain_blue_circle)
            linkBankProgress.visible()
            linkBankStateIndicator.gone()
            linkBank?.partner?.let {
                when (it) {
                    BankPartner.YAPILY -> {
                        val attrs = linkBank.attributes as YapilyAttributes
                        setTitleAndSubtitle(
                            getString(
                                com.blockchain.stringResources.R.string.yapily_linking_pending_title,
                                attrs.institutionList.find { institution -> institution.id == accountId }?.name
                                    ?: getString(com.blockchain.stringResources.R.string.yapily_linking_default_bank)
                            ),
                            getString(com.blockchain.stringResources.R.string.yapily_linking_pending_subtitle)
                        )
                    }

                    BankPartner.YODLEE, BankPartner.PLAID -> {
                        setTitleAndSubtitle(
                            getString(com.blockchain.stringResources.R.string.yodlee_linking_title),
                            getString(com.blockchain.stringResources.R.string.yodlee_linking_subtitle)
                        )
                    }
                }.exhaustive
            }
        }
    }

    private fun showCancelButton(@StringRes resId: Int) {
        with(binding) {
            secondaryCta.apply {
                visible()
                text = getString(resId)
                onClick = {
                    model.process(BankAuthIntent.CancelOrder)
                }
                analytics.logEvent(bankAuthEvent(BankAuthAnalytics.PIS_EXTERNAL_FLOW_CANCEL, authSource))
            }
        }
    }

    private fun showGoBackButton(button: BaseButtonView, onClick: () -> Unit) {
        button.apply {
            text = getString(com.blockchain.stringResources.R.string.common_go_back)
            visible()
            this@apply.onClick = onClick
        }
    }

    private fun showLinkingSuccess(label: String, id: String, partner: BankPartner?, currency: FiatCurrency) {
        logAnalytics(BankAuthAnalytics.SUCCESS, partner)

        with(binding) {
            linkBankIcon.setImageResource(R.drawable.ic_bank_details_big)
            linkBankProgress.gone()
            linkBankStateIndicator.apply {
                setImageResource(R.drawable.ic_check_circle)
                visible()
            }
            mainCta.apply {
                text = getString(com.blockchain.stringResources.R.string.common_continue)
                visible()
                onClick = {
                    navigator().bankLinkingFinished(id, currency)
                }
            }
            setTitleAndSubtitle(
                getString(com.blockchain.stringResources.R.string.bank_linking_success_title),
                getString(com.blockchain.stringResources.R.string.bank_linking_success_subtitle, label)
            )
        }
    }

    private fun logRetryLaunchAnalytics() =
        analytics.logEvent(
            bankAuthEvent(
                if (isForApproval) {
                    BankAuthAnalytics.PIS_EXTERNAL_FLOW_RETRY
                } else {
                    BankAuthAnalytics.AIS_EXTERNAL_FLOW_RETRY
                },
                authSource
            )
        )

    private fun navigator(): BankAuthFlowNavigator =
        (activity as? BankAuthFlowNavigator)
            ?: throw IllegalStateException("Parent must implement BankAuthFlowNavigator")

    companion object {
        private const val ACCOUNT_PROVIDER_ID = "ACCOUNT_PROVIDER_ID"
        private const val ACCOUNT_ID = "ACCOUNT_ID"
        private const val LINKING_BANK_ID = "LINKING_BANK_ID"
        private const val LINKING_BANK_TOKEN = "LINKING_BANK_TOKEN"
        private const val LINK_BANK_TRANSFER = "LINK_BANK_TRANSFER"
        private const val LINK_BANK_SOURCE = "LINK_BANK_SOURCE"
        private const val ERROR_STATE = "ERROR_STATE"
        private const val FROM_DEEP_LINK = "FROM_DEEP_LINK"
        private const val FOR_APPROVAL = "FOR_APPROVAL"
        private const val APPROVAL_DATA = "APPROVAL_DATA"
        private const val REFRESH_BANK_ACCOUNT_ID = "REFRESH_BANK_ACCOUNT_ID"

        fun newInstance(
            accountProviderId: String,
            accountId: String,
            linkingBankId: String,
            linkingBankToken: String? = null,
            linkBankTransfer: LinkBankTransfer? = null,
            authSource: BankAuthSource
        ) = BankAuthFragment().apply {
            arguments = Bundle().apply {
                putString(ACCOUNT_PROVIDER_ID, accountProviderId)
                putString(ACCOUNT_ID, accountId)
                putString(LINKING_BANK_ID, linkingBankId)
                putString(LINKING_BANK_TOKEN, linkingBankToken)
                putSerializable(LINK_BANK_TRANSFER, linkBankTransfer)
                putSerializable(LINK_BANK_SOURCE, authSource)
            }
        }

        fun newInstance(
            errorState: BankAuthError,
            authSource: BankAuthSource
        ) = BankAuthFragment().apply {
            arguments = Bundle().apply {
                putSerializable(ERROR_STATE, errorState)
                putSerializable(LINK_BANK_SOURCE, authSource)
            }
        }

        fun newInstance(
            approvalData: BankPaymentApproval,
            authSource: BankAuthSource
        ) = BankAuthFragment().apply {
            arguments = Bundle().apply {
                putSerializable(APPROVAL_DATA, approvalData)
                putBoolean(FOR_APPROVAL, true)
                putSerializable(LINK_BANK_SOURCE, authSource)
            }
        }

        fun newInstance(
            linkingId: String,
            authSource: BankAuthSource,
            fromDeepLink: Boolean = true
        ) = BankAuthFragment().apply {
            arguments = Bundle().apply {
                putBoolean(FROM_DEEP_LINK, fromDeepLink)
                putString(LINKING_BANK_ID, linkingId)
                putSerializable(LINK_BANK_SOURCE, authSource)
            }
        }

        fun newInstance(
            refreshBankAccountId: String?,
            authSource: BankAuthSource
        ) = BankAuthFragment().apply {
            arguments = Bundle().apply {
                putString(REFRESH_BANK_ACCOUNT_ID, refreshBankAccountId)
                putSerializable(LINK_BANK_SOURCE, authSource)
                putBoolean(FROM_DEEP_LINK, false)
            }
        }
    }
}

interface ChooserCallback {
    fun onItemChosen()
}

class ChooserReceiver : BroadcastReceiver() {
    companion object {
        // must be a static variable for inter process communication
        var listener: ChooserCallback? = null
    }

    fun registerListener(callback: ChooserCallback) {
        listener = callback
    }

    override fun onReceive(context: Context, intent: Intent) {
        // we only get this onReceive if an app has been selected, so notify regardless of which app was chosen
        listener?.run {
            onItemChosen()
        }
    }
}
