package piuk.blockchain.android.ui.brokerage.sell

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import com.blockchain.analytics.Analytics
import com.blockchain.api.NabuApiException
import com.blockchain.api.NabuApiExceptionFactory
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.CryptoAccount
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.NavigationRouter
import com.blockchain.commonarch.presentation.mvi_v2.bindViewModel
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.goneIf
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.componentlib.viewextensions.visibleIf
import com.blockchain.core.sell.domain.SellEligibility
import com.blockchain.core.sell.domain.SellUserEligibility
import com.blockchain.data.DataResource
import com.blockchain.data.doOnData
import com.blockchain.domain.common.model.BuySellViewType
import com.blockchain.koin.payloadScope
import com.blockchain.nabu.BlockedReason
import com.blockchain.presentation.customviews.kyc.KycUpgradeNowSheet
import com.blockchain.presentation.openUrl
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Single
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.component.KoinScopeComponent
import org.koin.core.scope.Scope
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.SellIntroFragmentBinding
import piuk.blockchain.android.simplebuy.BuySellViewedEvent
import piuk.blockchain.android.simplebuy.ClientErrorAnalytics
import piuk.blockchain.android.simplebuy.ClientErrorAnalytics.Companion.OOPS_ERROR
import piuk.blockchain.android.support.SupportCentreActivity
import piuk.blockchain.android.ui.base.MVIViewPagerFragment
import piuk.blockchain.android.ui.customviews.ButtonOptions
import piuk.blockchain.android.ui.customviews.VerifyIdentityNumericBenefitItem
import piuk.blockchain.android.ui.customviews.account.AccountListViewItem
import piuk.blockchain.android.ui.customviews.account.CellDecorator
import piuk.blockchain.android.ui.transactionflow.analytics.SellAssetScreenViewedEvent
import piuk.blockchain.android.ui.transactionflow.analytics.SellAssetSelectedEvent
import piuk.blockchain.android.ui.transactionflow.flow.TransactionFlowActivity
import piuk.blockchain.android.urllinks.URL_RUSSIA_SANCTIONS_EU5
import piuk.blockchain.android.urllinks.URL_RUSSIA_SANCTIONS_EU8
import retrofit2.HttpException

class SellIntroFragment :
    MVIViewPagerFragment<SellViewState>(),
    NavigationRouter<SellNavigation>,
    KoinScopeComponent {

    override val scope: Scope
        get() = payloadScope

    private val viewModel by viewModel<SellViewModel>()

    interface SellIntroHost {
        fun onSellFinished()
        fun onSellInfoClicked()
        fun onSellListEmptyCta()
    }

    private val host: SellIntroHost by lazy {
        parentFragment as? SellIntroHost ?: throw IllegalStateException(
            "Host fragment is not a SellIntroHost"
        )
    }

    private val startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        host.onSellFinished()
    }

    private var _binding: SellIntroFragmentBinding? = null
    private val binding: SellIntroFragmentBinding
        get() = _binding!!

    private val analytics: Analytics by inject()

    private var hasEnteredSearchTerm: Boolean = false
    private var isAccountsFirstLoad: Boolean = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = SellIntroFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        analytics.logEvent(SellAssetScreenViewedEvent)

        with(binding) {
            with(sellSearchEmpty) {
                text = getString(R.string.search_empty)
                gravity = ComposeGravities.Centre
                style = ComposeTypographies.Body1
                textColor = ComposeColors.Dark
            }

            sellEmpty.gone()
            customEmptyState.gone()
        }

        bindViewModel(
            viewModel = viewModel,
            navigator = this,
            args = ModelConfigArgs.NoArgs
        )
    }

    override fun onStateUpdated(state: SellViewState) {
        when (state.sellEligibility) {
            is DataResource.Data -> {

                when (val eligibilityData = state.sellEligibility.data) {
                    is SellEligibility.Eligible -> {
                        renderEligibleUser(eligibilityData.sellAssets)
                    }
                    is SellEligibility.KycBlocked -> {
                        when (eligibilityData.reason) {
                            SellUserEligibility.KycRejectedUser -> renderRejectedKycedUserUi()
                            SellUserEligibility.KycdUser -> {
                                // do nothing - handled by list case
                            }
                            SellUserEligibility.NonKycdUser -> renderNonKycedUserUi()
                        }
                    }
                    is SellEligibility.NotEligible -> when (eligibilityData.reason) {
                        is BlockedReason.InsufficientTier -> renderNonKycedUserUi()
                        is BlockedReason.NotEligible -> renderRejectedKycedUserUi()
                        is BlockedReason.Sanctions -> renderBlockedDueToSanctions(
                            eligibilityData.reason as BlockedReason.Sanctions
                        )
                        else -> {
                            // do nothing - other states are handled by the repository
                        }
                    }
                }
            }
            is DataResource.Error -> {

                renderSellError()
                logErrorAnalytics(
                    nabuApiException = (state.sellEligibility.error as? HttpException)?.let {
                        NabuApiExceptionFactory.fromResponseBody(it)
                    },
                    errorDescription = state.sellEligibility.error.message,
                    error = if (state.sellEligibility.error is HttpException) {
                        ClientErrorAnalytics.NABU_ERROR
                    } else ClientErrorAnalytics.UNKNOWN_ERROR,
                    source = if (state.sellEligibility.error is HttpException) {
                        ClientErrorAnalytics.Companion.Source.NABU
                    } else {
                        ClientErrorAnalytics.Companion.Source.CLIENT
                    },
                    title = OOPS_ERROR,
                    action = ClientErrorAnalytics.ACTION_SELL
                )
            }
            DataResource.Loading -> {}
        }

        state.supportedAccountList.doOnData { supportedAccounts ->
            with(binding.accountsList) {
                if (isAccountsFirstLoad) {
                    initialise(
                        Single.just(supportedAccounts.map { (AccountListViewItem(it)) }),
                        status = ::statusDecorator,
                    )
                    isAccountsFirstLoad = false
                } else {
                    loadItems(
                        Single.just(supportedAccounts.map { (AccountListViewItem(it)) }),
                        accountsLocksSource = Single.just(emptyList())
                    )
                }
            }
        }
    }

    private fun renderEligibleUser(supportedAssets: List<AssetInfo>) {
        with(binding) {
            kycBenefits.gone()
            sellAccountsContainer.visible()
            sellIntroSearch.apply {
                placeholder = getString(R.string.search_coins_hint)
                onValueChange = { searchTerm ->
                    hasEnteredSearchTerm = searchTerm.isNotEmpty()
                    viewModel.onIntent(SellIntent.FilterAccounts(searchTerm))
                }
            }

            with(accountsList) {
                onAccountSelected = { account ->
                    analytics.logEvent(SellAssetSelectedEvent(type = account.label))
                    (account as? CryptoAccount)?.let {
                        startSellFlow(it)
                    }
                }
                onListLoaded = { isEmpty ->

                    sellSearchEmpty.visibleIf { isEmpty && hasEnteredSearchTerm }
                    accountsList.goneIf { isEmpty }

                    if (!hasEnteredSearchTerm) {
                        if (isEmpty)
                            renderSellEmpty()
                        else sellEmpty.gone()
                    }
                }
            }
        }
    }

    private fun logErrorAnalytics(
        title: String,
        error: String,
        source: ClientErrorAnalytics.Companion.Source,
        action: String,
        nabuApiException: NabuApiException? = null,
        errorDescription: String? = null,
    ) {
        analytics.logEvent(
            ClientErrorAnalytics.ClientLogError(
                nabuApiException = nabuApiException,
                error = error,
                source = source,
                title = title,
                action = action,
                errorDescription = errorDescription,
                categories = nabuApiException?.getServerSideErrorInfo()?.categories ?: emptyList()
            )
        )
    }

    private fun renderSellError() {
        with(binding) {
            sellAccountsContainer.gone()
            sellEmpty.setDetails(
                action = {
                    viewModel.onIntent(SellIntent.CheckSellEligibility(showLoader = true))
                },
                onContactSupport = { requireContext().startActivity(SupportCentreActivity.newIntent(requireContext())) }
            )
            sellEmpty.visible()
        }
    }

    private fun renderSellEmpty() {
        with(binding) {
            sellAccountsContainer.gone()

            sellEmpty.setDetails(
                title = R.string.sell_intro_empty_title,
                description = R.string.sell_intro_empty_label,
                ctaText = R.string.buy_now,
                action = { host.onSellListEmptyCta() },
                onContactSupport = { requireContext().startActivity(SupportCentreActivity.newIntent(requireContext())) }
            )
            sellEmpty.visible()
        }
    }

    private fun renderBlockedDueToSanctions(reason: BlockedReason.Sanctions) {
        val action = {
            when (reason) {
                is BlockedReason.Sanctions.RussiaEU5 -> requireContext().openUrl(URL_RUSSIA_SANCTIONS_EU5)
                is BlockedReason.Sanctions.RussiaEU8 -> requireContext().openUrl(URL_RUSSIA_SANCTIONS_EU8)
                is BlockedReason.Sanctions.Unknown -> {}
            }
        }

        with(binding) {
            sellAccountsContainer.gone()

            customEmptyState.apply {
                title = R.string.account_restricted
                descriptionText = reason.message
                icon = R.drawable.ic_wallet_intro_image
                ctaText = R.string.common_learn_more
                ctaAction = action
                visible()
            }
        }
    }

    private fun renderRejectedKycedUserUi() {
        with(binding) {
            kycBenefits.visible()
            sellAccountsContainer.gone()
            kycStepsContainer.gone()

            kycBenefits.initWithBenefits(
                benefits = listOf(
                    VerifyIdentityNumericBenefitItem(
                        getString(R.string.invalid_id),
                        getString(R.string.invalid_id_description)
                    ),
                    VerifyIdentityNumericBenefitItem(
                        getString(R.string.information_missmatch),
                        getString(R.string.information_missmatch_description)
                    ),
                    VerifyIdentityNumericBenefitItem(
                        getString(R.string.blocked_by_local_laws),
                        getString(R.string.sell_intro_kyc_subtitle_3)
                    )
                ),
                title = getString(R.string.unable_to_verify_id),
                description = getString(R.string.unable_to_verify_id_description),
                icon = R.drawable.ic_cart,
                secondaryButton = ButtonOptions(true, getString(R.string.contact_support)) {
                    startActivity(SupportCentreActivity.newIntent(requireContext()))
                },
                primaryButton = ButtonOptions(false) {},
                showSheetIndicator = false,
                footerText = getString(R.string.error_contact_support)
            )
        }
    }

    private fun renderNonKycedUserUi() {
        with(binding) {
            if (childFragmentManager.findFragmentById(R.id.kyc_steps_container) == null) {
                childFragmentManager.beginTransaction()
                    .replace(R.id.kyc_steps_container, KycUpgradeNowSheet.newInstance())
                    .commitAllowingStateLoss()
            }

            kycBenefits.gone()
            kycStepsContainer.visible()
            sellAccountsContainer.gone()
        }
    }

    private fun statusDecorator(account: BlockchainAccount): CellDecorator = SellCellDecorator(account)

    private fun startSellFlow(it: CryptoAccount) {
        analytics.logEvent(BuySellViewedEvent(BuySellViewType.TYPE_SELL))

        startForResult.launch(
            TransactionFlowActivity.newIntent(
                context = requireActivity(),
                sourceAccount = it,
                action = AssetAction.Sell
            )
        )
    }

    override fun onResume() {
        super.onResume()
        viewModel.onIntent(SellIntent.CheckSellEligibility(showLoader = false))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = SellIntroFragment()
    }

    override fun route(navigationEvent: SellNavigation) {
        // do nothing
    }
}
