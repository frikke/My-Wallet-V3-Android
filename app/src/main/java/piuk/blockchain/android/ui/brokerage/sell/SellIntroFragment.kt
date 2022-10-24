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
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.SingleAccountList
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.NavigationRouter
import com.blockchain.commonarch.presentation.mvi_v2.bindViewModel
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.core.sell.domain.SellEligibility
import com.blockchain.core.sell.domain.SellUserEligibility
import com.blockchain.data.DataResource
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.koin.hideDustFeatureFlag
import com.blockchain.koin.payloadScope
import com.blockchain.koin.sellOrder
import com.blockchain.nabu.BlockedReason
import com.blockchain.preferences.LocalSettingsPrefs
import com.blockchain.presentation.koin.scopedInject
import com.blockchain.utils.zipObservables
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Single
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.component.KoinScopeComponent
import org.koin.core.scope.Scope
import piuk.blockchain.android.R
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.databinding.SellIntroFragmentBinding
import piuk.blockchain.android.simplebuy.BuySellViewedEvent
import piuk.blockchain.android.simplebuy.ClientErrorAnalytics
import piuk.blockchain.android.simplebuy.ClientErrorAnalytics.Companion.OOPS_ERROR
import piuk.blockchain.android.support.SupportCentreActivity
import piuk.blockchain.android.ui.base.MVIViewPagerFragment
import piuk.blockchain.android.ui.brokerage.BuySellFragment
import piuk.blockchain.android.ui.customviews.ButtonOptions
import piuk.blockchain.android.ui.customviews.VerifyIdentityNumericBenefitItem
import piuk.blockchain.android.ui.customviews.account.AccountListViewItem
import piuk.blockchain.android.ui.customviews.account.CellDecorator
import piuk.blockchain.android.ui.home.HomeNavigator
import piuk.blockchain.android.ui.transactionflow.analytics.SellAssetScreenViewedEvent
import piuk.blockchain.android.ui.transactionflow.analytics.SellAssetSelectedEvent
import piuk.blockchain.android.ui.transactionflow.flow.TransactionFlowActivity
import piuk.blockchain.android.ui.transfer.AccountsSorting
import piuk.blockchain.android.urllinks.URL_RUSSIA_SANCTIONS_EU5
import piuk.blockchain.android.util.openUrl
import retrofit2.HttpException

class SellIntroFragment : MVIViewPagerFragment<SellViewState>(), NavigationRouter<SellNavigation>, KoinScopeComponent {

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
        viewModel.onIntent(SellIntent.CheckSellEligibility(showLoader = false))
    }

    private var _binding: SellIntroFragmentBinding? = null
    private val binding: SellIntroFragmentBinding
        get() = _binding!!

    private val coincore: Coincore by scopedInject()
    private val analytics: Analytics by inject()
    private val accountsSorting: AccountsSorting by scopedInject(sellOrder)

    private val localSettingsPrefs: LocalSettingsPrefs by inject()
    private val hideDustFlag: FeatureFlag by scopedInject(hideDustFeatureFlag)

    private var supportedSellCryptos: List<AssetInfo> = emptyList()
    private var hasEnteredSearchTerm: Boolean = false

    private val listOfSellAccounts: Single<List<CryptoAccount>>
        get() = hideDustFlag.enabled.flatMap { flagEnabled ->
            if (flagEnabled && localSettingsPrefs.hideSmallBalancesEnabled) {
                coincore.walletsWithActions(
                    actions = setOf(AssetAction.Sell),
                    sorter = accountsSorting.sorter()
                ).flatMap { accountList ->
                    filterAccountsList(accountList)
                }.cache()
            } else {
                coincore.walletsWithActions(
                    actions = setOf(AssetAction.Sell),
                    sorter = accountsSorting.sorter()
                ).map {
                    it.filterIsInstance<CryptoAccount>()
                }.cache()
            }
        }

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
        when (state.data) {
            is DataResource.Data -> {
                hideLoading()

                when (val eligibilityData = state.data.data) {
                    is SellEligibility.Eligible -> {
                        renderEligibleUser(eligibilityData)
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
                        is BlockedReason.InsufficientTier.Unknown -> renderNonKycedUserUi()
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
                hideLoading()

                renderSellError()
                logErrorAnalytics(
                    nabuApiException = (state.data.error as? HttpException)?.let {
                        NabuApiExceptionFactory.fromResponseBody(it)
                    },
                    errorDescription = state.data.error.message,
                    error = if (state.data.error is HttpException) {
                        ClientErrorAnalytics.NABU_ERROR
                    } else ClientErrorAnalytics.UNKNOWN_ERROR,
                    source = if (state.data.error is HttpException) {
                        ClientErrorAnalytics.Companion.Source.NABU
                    } else {
                        ClientErrorAnalytics.Companion.Source.CLIENT
                    },
                    title = OOPS_ERROR,
                    action = ClientErrorAnalytics.ACTION_SELL
                )
            }
            DataResource.Loading -> if (state.showLoader) {
                showLoading()
            }
        }
    }

    private fun filterAccountsList(accountList: SingleAccountList): Single<List<CryptoAccount>> =
        accountList.map { account ->
            account.balanceRx
        }.zipObservables().map {
            accountList.mapIndexedNotNull { index, singleAccount ->
                if (!it[index].totalFiat.isDust()) {
                    singleAccount
                } else {
                    null
                }
            }.filterIsInstance<CryptoAccount>()
        }.firstOrError()

    private fun renderEligibleUser(eligible: SellEligibility.Eligible) {
        supportedSellCryptos = eligible.sellAssets

        with(binding) {
            kycBenefits.gone()
            sellAccountsContainer.visible()
            sellIntroSearch.apply {
                label = getString(R.string.search_coins_hint)
                onValueChange = { searchedText ->
                    this@with.onSearchTermUpdated(searchedText)
                }
            }
            with(accountsList) {
                initialise(
                    filteredAccountList(supportedSellCryptos),
                    status = ::statusDecorator,
                )
                onAccountSelected = { account ->
                    analytics.logEvent(SellAssetSelectedEvent(type = account.label))
                    (account as? CryptoAccount)?.let {
                        startSellFlow(it)
                    }
                }
                onListLoaded = { isEmpty ->
                    hideLoading()

                    if (isEmpty) {
                        if (hasEnteredSearchTerm) {
                            sellSearchEmpty.visible()
                            accountsList.gone()
                        } else {
                            renderSellEmpty()
                        }
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
            sellEmpty.setDetails {
                viewModel.onIntent(SellIntent.CheckSellEligibility(showLoader = true))
            }
            sellEmpty.visible()
        }
    }

    private fun renderSellEmpty() {
        with(binding) {
            sellAccountsContainer.gone()

            sellEmpty.setDetails(
                R.string.sell_intro_empty_title,
                R.string.sell_intro_empty_label,
                ctaText = R.string.buy_now
            ) {
                host.onSellListEmptyCta()
            }
            sellEmpty.visible()
        }
    }

    private fun renderBlockedDueToSanctions(reason: BlockedReason.Sanctions) {
        with(binding) {
            sellAccountsContainer.gone()

            customEmptyState.apply {
                title = R.string.account_restricted
                descriptionText = when (reason) {
                    BlockedReason.Sanctions.RussiaEU5 -> getString(R.string.russia_sanctions_eu5_sheet_subtitle)
                    is BlockedReason.Sanctions.Unknown -> reason.message
                }
                icon = R.drawable.ic_wallet_intro_image
                ctaText = R.string.common_learn_more
                ctaAction = { requireContext().openUrl(URL_RUSSIA_SANCTIONS_EU5) }
                visible()
            }
        }
    }

    private fun renderRejectedKycedUserUi() {
        with(binding) {
            kycBenefits.visible()
            sellAccountsContainer.gone()

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
            kycBenefits.visible()
            sellAccountsContainer.gone()

            kycBenefits.initWithBenefits(
                benefits = listOf(
                    VerifyIdentityNumericBenefitItem(
                        getString(R.string.sell_intro_kyc_title_1),
                        getString(R.string.sell_intro_kyc_subtitle_1)
                    ),
                    VerifyIdentityNumericBenefitItem(
                        getString(R.string.sell_intro_kyc_title_2),
                        getString(R.string.sell_intro_kyc_subtitle_2)
                    ),
                    VerifyIdentityNumericBenefitItem(
                        getString(R.string.sell_intro_kyc_title_3),
                        getString(R.string.sell_intro_kyc_subtitle_3)
                    )
                ),
                title = getString(R.string.sell_crypto),
                description = getString(R.string.sell_crypto_subtitle),
                icon = R.drawable.ic_cart,
                secondaryButton = ButtonOptions(false) {},
                primaryButton = ButtonOptions(true) {
                    (activity as? HomeNavigator)?.launchKyc(CampaignType.SimpleBuy)
                },
                showSheetIndicator = false
            )
        }
    }

    private fun SellIntroFragmentBinding.onSearchTermUpdated(searchTerm: String) {
        with(accountsList) {
            visible()
            loadItems(
                filteredAccountList(
                    supportedSellCryptos = supportedSellCryptos,
                    searchTerm = searchTerm
                ),
                accountsLocksSource = Single.just(emptyList())
            )
        }

        hasEnteredSearchTerm = searchTerm.isNotEmpty()
        sellSearchEmpty.gone()
        sellIntroSearch.visible()
    }

    private fun filteredAccountList(
        supportedSellCryptos: List<AssetInfo>,
        searchTerm: String = ""
    ): Single<List<AccountListViewItem>> =
        listOfSellAccounts
            .doOnSubscribe {
                showLoading()
            }
            .map { list ->
                list.filter { account ->
                    if (searchTerm.isNotEmpty()) {
                        supportedSellCryptos.contains(account.currency) &&
                            (
                                account.currency.name.contains(searchTerm, true) ||
                                    account.currency.networkTicker.contains(searchTerm, true)
                                )
                    } else {
                        true
                    }
                }.map(AccountListViewItem.Companion::create)
            }

    private fun statusDecorator(account: BlockchainAccount): CellDecorator = SellCellDecorator(account)

    private fun startSellFlow(it: CryptoAccount) {
        analytics.logEvent(BuySellViewedEvent(BuySellFragment.BuySellViewType.TYPE_SELL))

        startForResult.launch(
            TransactionFlowActivity.newIntent(
                context = requireActivity(),
                sourceAccount = it,
                action = AssetAction.Sell
            )
        )
    }

    override fun onResumeFragment() {
        viewModel.onIntent(SellIntent.CheckSellEligibility(showLoader = false))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showLoading() {
        (requireActivity() as? BlockchainActivity)?.showLoading()
    }

    private fun hideLoading() {
        (requireActivity() as? BlockchainActivity)?.hideLoading()
    }

    companion object {
        fun newInstance() = SellIntroFragment()
    }

    override fun route(navigationEvent: SellNavigation) {
        // do nothing
    }
}
