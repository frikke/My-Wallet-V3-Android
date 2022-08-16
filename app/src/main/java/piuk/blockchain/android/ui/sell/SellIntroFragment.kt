package piuk.blockchain.android.ui.sell

import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
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
import com.blockchain.commonarch.presentation.base.trackProgress
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.core.kyc.domain.KycService
import com.blockchain.core.kyc.domain.model.KycTier
import com.blockchain.koin.scopedInject
import com.blockchain.nabu.BlockedReason
import com.blockchain.nabu.Feature
import com.blockchain.nabu.FeatureAccess
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.SimpleBuyEligibilityProvider
import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.asAssetInfoOrThrow
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.kotlin.zipWith
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.rx3.rxSingle
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.databinding.SellIntroFragmentBinding
import piuk.blockchain.android.simplebuy.BuySellViewedEvent
import piuk.blockchain.android.simplebuy.ClientErrorAnalytics
import piuk.blockchain.android.simplebuy.ClientErrorAnalytics.Companion.OOPS_ERROR
import piuk.blockchain.android.support.SupportCentreActivity
import piuk.blockchain.android.ui.base.ViewPagerFragment
import piuk.blockchain.android.ui.customviews.ButtonOptions
import piuk.blockchain.android.ui.customviews.IntroHeaderView
import piuk.blockchain.android.ui.customviews.VerifyIdentityNumericBenefitItem
import piuk.blockchain.android.ui.customviews.account.AccountListViewItem
import piuk.blockchain.android.ui.customviews.account.CellDecorator
import piuk.blockchain.android.ui.home.HomeNavigator
import piuk.blockchain.android.ui.transactionflow.flow.TransactionFlowActivity
import piuk.blockchain.android.ui.transfer.AccountsSorting
import piuk.blockchain.android.urllinks.URL_RUSSIA_SANCTIONS_EU5
import piuk.blockchain.android.util.openUrl
import retrofit2.HttpException

class SellIntroFragment : ViewPagerFragment() {
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
        checkEligibilityAndLoadSellDetails(showLoader = false)
    }

    private var _binding: SellIntroFragmentBinding? = null
    private val binding: SellIntroFragmentBinding
        get() = _binding!!

    private val kycService: KycService by scopedInject()
    private val coincore: Coincore by scopedInject()
    private val custodialWalletManager: CustodialWalletManager by scopedInject()
    private val eligibilityProvider: SimpleBuyEligibilityProvider by scopedInject()
    private val currencyPrefs: CurrencyPrefs by inject()
    private val analytics: Analytics by inject()
    private val accountsSorting: AccountsSorting by scopedInject()
    private val userIdentity: UserIdentity by scopedInject()
    private val compositeDisposable = CompositeDisposable()

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
        checkEligibilityAndLoadSellDetails()
    }

    private fun checkEligibilityAndLoadSellDetails(showLoader: Boolean = true) {
        compositeDisposable +=
            userIdentity.userAccessForFeature(Feature.Sell)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe {
                    binding.sellEmpty.gone()
                    binding.customEmptyState.gone()
                }
                .subscribeBy(
                    onSuccess = { eligibility ->
                        when (val reason = (eligibility as? FeatureAccess.Blocked)?.reason) {
                            is BlockedReason.InsufficientTier -> renderNonKycedUserUi()
                            BlockedReason.NotEligible -> renderRejectedKycedUserUi()
                            is BlockedReason.Sanctions -> renderBlockedDueToSanctions(reason)
                            is BlockedReason.TooManyInFlightTransactions,
                            null,
                            -> loadSellDetails(showLoader)
                        }
                    },
                    onError = {
                        renderSellError()
                        logErrorAnalytics(
                            nabuApiException = if (it is HttpException) {
                                NabuApiExceptionFactory.fromResponseBody(it)
                            } else null,
                            error = OOPS_ERROR,
                            source = if (it is HttpException) {
                                ClientErrorAnalytics.Companion.Source.NABU
                            } else {
                                ClientErrorAnalytics.Companion.Source.CLIENT
                            },
                            title = OOPS_ERROR,
                            action = ClientErrorAnalytics.ACTION_SELL
                        )
                    }
                )
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

    private fun loadSellDetails(showLoader: Boolean) {
        binding.accountsList.activityIndicator = if (showLoader) activityIndicator else null

        compositeDisposable += kycService.getTiersLegacy()
            .zipWith(eligibilityProvider.isEligibleForSimpleBuy(forceRefresh = true))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .trackProgress(binding.accountsList.activityIndicator)
            .subscribeBy(onSuccess = { (kyc, eligible) ->
                when {
                    kyc.isApprovedFor(KycTier.GOLD) && eligible -> {
                        renderKycedUserUi()
                    }
                    kyc.isRejectedFor(KycTier.GOLD) -> {
                        renderRejectedKycedUserUi()
                    }
                    kyc.isApprovedFor(KycTier.GOLD) && !eligible -> {
                        renderRejectedKycedUserUi()
                    }
                    else -> {
                        renderNonKycedUserUi()
                    }
                }
            }, onError = {
                renderSellError()
                logErrorAnalytics(
                    nabuApiException = (it as? HttpException)?.let {
                        NabuApiExceptionFactory.fromResponseBody(it)
                    },
                    errorDescription = it.message,
                    error = if (it is HttpException) {
                        ClientErrorAnalytics.NABU_ERROR
                    } else ClientErrorAnalytics.UNKNOWN_ERROR,
                    source = if (it is HttpException) {
                        ClientErrorAnalytics.Companion.Source.NABU
                    } else {
                        ClientErrorAnalytics.Companion.Source.CLIENT
                    },
                    title = OOPS_ERROR,
                    action = ClientErrorAnalytics.ACTION_SELL
                )
            })
    }

    private fun renderSellError() {
        with(binding) {
            accountsList.gone()
            sellEmpty.setDetails {
                checkEligibilityAndLoadSellDetails()
            }
            sellEmpty.visible()
        }
    }

    private fun renderSellEmpty() {
        with(binding) {
            accountsList.gone()
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
            accountsList.gone()
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
            accountsList.gone()

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
            accountsList.gone()

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

    private fun renderKycedUserUi() {
        with(binding) {
            kycBenefits.gone()
            accountsList.visible()

            compositeDisposable += supportedCryptoCurrencies()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .trackProgress(binding.accountsList.activityIndicator)
                .subscribeBy(onSuccess = { supportedCryptos ->
                    val introHeaderView = IntroHeaderView(requireContext())
                    introHeaderView.setDetails(
                        icon = R.drawable.ic_sell_minus,
                        label = R.string.select_wallet_to_sell,
                        title = R.string.sell_for_cash
                    )

                    accountsList.initialise(
                        coincore.walletsWithActions(
                            actions = setOf(AssetAction.Sell),
                            sorter = accountsSorting.sorter()
                        ).map {
                            it.filterIsInstance<CryptoAccount>().filter { account ->
                                supportedCryptos.contains(account.currency)
                            }.map(AccountListViewItem.Companion::create)
                        },
                        status = ::statusDecorator,
                        introView = introHeaderView
                    )

                    renderSellInfo()

                    accountsList.onAccountSelected = { account ->
                        (account as? CryptoAccount)?.let {
                            startSellFlow(it)
                        }
                    }

                    accountsList.onListLoaded = {
                        if (it) renderSellEmpty()
                    }
                }, onError = {
                    renderSellError()
                    logErrorAnalytics(
                        nabuApiException = (it as? HttpException)?.let {
                            NabuApiExceptionFactory.fromResponseBody(it)
                        },
                        errorDescription = it.message,
                        error = if (it is HttpException) {
                            ClientErrorAnalytics.NABU_ERROR
                        } else ClientErrorAnalytics.UNKNOWN_ERROR,
                        source = if (it is HttpException) {
                            ClientErrorAnalytics.Companion.Source.NABU
                        } else {
                            ClientErrorAnalytics.Companion.Source.CLIENT
                        },
                        title = OOPS_ERROR,
                        action = ClientErrorAnalytics.ACTION_SELL
                    )
                })
        }
    }

    private fun renderSellInfo() {
        val sellInfoIntro = getString(R.string.sell_info_blurb_1)
        val sellInfoBold = getString(R.string.sell_info_blurb_2)
        val sellInfoEnd = getString(R.string.sell_info_blurb_3)

        val sb = SpannableStringBuilder()
            .append(sellInfoIntro)
            .append(sellInfoBold)
            .append(sellInfoEnd)
        sb.setSpan(
            StyleSpan(Typeface.BOLD), sellInfoIntro.length, sellInfoIntro.length + sellInfoBold.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
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

    private fun supportedCryptoCurrencies(): Single<List<AssetInfo>> {
        val availableFiats =
            rxSingle { custodialWalletManager.getSupportedFundsFiats(currencyPrefs.selectedFiatCurrency).first() }
        return Single.zip(
            custodialWalletManager.getSupportedBuySellCryptoCurrencies(), availableFiats
        ) { supportedPairs, fiats ->
            supportedPairs
                .filter { fiats.contains(it.destination) }
                .map { it.source.asAssetInfoOrThrow() }
        }
    }

    override fun onResumeFragment() {
        checkEligibilityAndLoadSellDetails(false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        compositeDisposable.clear()
        _binding = null
    }

    companion object {
        private const val TX_FLOW_REQUEST = 123

        fun newInstance() = SellIntroFragment()
    }
}
