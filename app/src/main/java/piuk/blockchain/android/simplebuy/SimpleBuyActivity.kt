package piuk.blockchain.android.simplebuy

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
import androidx.lifecycle.lifecycleScope
import com.blockchain.api.NabuApiException
import com.blockchain.api.ServerErrorAction
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.commonarch.presentation.base.addAnimationTransaction
import com.blockchain.commonarch.presentation.base.trackProgress
import com.blockchain.componentlib.databinding.FragmentActivityBinding
import com.blockchain.componentlib.databinding.ToolbarGeneralBinding
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.hideKeyboard
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.deeplinking.navigation.DeeplinkRedirector
import com.blockchain.domain.dataremediation.DataRemediationService
import com.blockchain.domain.dataremediation.model.QuestionnaireContext
import com.blockchain.extensions.exhaustive
import com.blockchain.koin.payloadScope
import com.blockchain.koin.scopedInject
import com.blockchain.nabu.BlockedReason
import com.blockchain.nabu.FeatureAccess
import com.blockchain.outcome.doOnSuccess
import com.blockchain.payments.googlepay.interceptor.GooglePayResponseInterceptor
import com.blockchain.payments.googlepay.interceptor.OnGooglePayDataReceivedListener
import com.blockchain.preferences.BankLinkingPrefs
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.FiatCurrency
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.simplebuy.ClientErrorAnalytics.Companion.ACTION_BUY
import piuk.blockchain.android.simplebuy.sheets.CurrencySelectionSheet
import piuk.blockchain.android.ui.base.ErrorButtonCopies
import piuk.blockchain.android.ui.base.ErrorDialogData
import piuk.blockchain.android.ui.base.ErrorSlidingBottomDialog
import piuk.blockchain.android.ui.base.mapToErrorCopies
import piuk.blockchain.android.ui.customviews.BlockedDueToSanctionsSheet
import piuk.blockchain.android.ui.dashboard.sheets.KycUpgradeNowSheet
import piuk.blockchain.android.ui.dataremediation.QuestionnaireSheet
import piuk.blockchain.android.ui.home.MainActivity
import piuk.blockchain.android.ui.kyc.navhost.KycNavHostActivity
import piuk.blockchain.android.ui.linkbank.BankAuthDeepLinkState
import piuk.blockchain.android.ui.linkbank.BankAuthFlowState
import piuk.blockchain.android.ui.linkbank.BankAuthRefreshContract
import piuk.blockchain.android.ui.linkbank.BankAuthSource
import piuk.blockchain.android.ui.linkbank.fromPreferencesValue
import piuk.blockchain.android.ui.linkbank.toPreferencesValue
import piuk.blockchain.android.ui.recurringbuy.RecurringBuyFirstTimeBuyerFragment
import piuk.blockchain.android.ui.recurringbuy.RecurringBuySuccessfulFragment
import piuk.blockchain.android.ui.sell.BuySellFragment
import piuk.blockchain.androidcore.utils.extensions.emptySubscribe
import piuk.blockchain.androidcore.utils.helperfunctions.consume
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import timber.log.Timber

class SimpleBuyActivity :
    BlockchainActivity(),
    SimpleBuyNavigator,
    KycUpgradeNowSheet.Host,
    QuestionnaireSheet.Host,
    ErrorSlidingBottomDialog.Host {
    override val alwaysDisableScreenshots: Boolean
        get() = false

    override val enableLogoutTimer: Boolean = false
    private val compositeDisposable = CompositeDisposable()
    private val buyFlowNavigator: BuyFlowNavigator by scopedInject()
    private val bankLinkingPrefs: BankLinkingPrefs by scopedInject()
    private val deeplinkRedirector: DeeplinkRedirector by scopedInject()
    private val assetCatalogue: AssetCatalogue by inject()
    private val googlePayResponseInterceptor: GooglePayResponseInterceptor by inject()
    private val dataRemediationService: DataRemediationService by scopedInject()

    private var primaryErrorCtaAction = {}
    private var secondaryErrorCtaAction = {}
    private var tertiaryErrorCtaAction = {}

    private val startedFromDashboard: Boolean by unsafeLazy {
        intent.getBooleanExtra(STARTED_FROM_NAVIGATION_KEY, false)
    }

    private val startedFromApprovalDeepLink: Boolean by unsafeLazy {
        intent.getBooleanExtra(STARTED_FROM_APPROVAL_KEY, false)
    }

    private val preselectedPaymentMethodId: String? by unsafeLazy {
        intent.getStringExtra(PRESELECTED_PAYMENT_METHOD)
    }

    private val preselectedAmount: String? by unsafeLazy {
        intent.getStringExtra(PRESELECTED_AMOUNT)
    }

    private val startedFromKycResume: Boolean by unsafeLazy {
        intent.getBooleanExtra(STARTED_FROM_KYC_RESUME, false)
    }

    private val launchLinkNewCard: Boolean by unsafeLazy {
        intent.getBooleanExtra(LAUNCH_LINK_CARD, false)
    }

    private val launchSelectNewPaymentMethod: Boolean by unsafeLazy {
        intent.getBooleanExtra(LAUNCH_SELECT_PAYMENT_METHOD, false)
    }

    private val asset: AssetInfo? by unsafeLazy {
        intent.getStringExtra(ASSET_KEY)?.let {
            assetCatalogue.assetInfoFromNetworkTicker(it)
        }
    }

    override val toolbarBinding: ToolbarGeneralBinding
        get() = binding.toolbar

    private val binding: FragmentActivityBinding by lazy {
        FragmentActivityBinding.inflate(layoutInflater)
    }

    private val refreshBankResultLauncher = registerForActivityResult(BankAuthRefreshContract()) { refreshSuccess ->
        if (refreshSuccess) {
            popFragmentsInStackUntilFind(
                fragmentName = SimpleBuyCheckoutFragment::class.simpleName.orEmpty(),
                popInclusive = true
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        updateToolbar(
            toolbarTitle = getString(R.string.common_buy),
            backAction = { super.onBackPressed() }
        )
        if (savedInstanceState == null) {
            if (startedFromApprovalDeepLink) {
                bankLinkingPrefs.getBankLinkingState().fromPreferencesValue()?.let {
                    bankLinkingPrefs.setBankLinkingState(
                        it.copy(bankAuthFlow = BankAuthFlowState.BANK_APPROVAL_COMPLETE).toPreferencesValue()
                    )
                } ?: run {
                    bankLinkingPrefs.setBankLinkingState(
                        BankAuthDeepLinkState(
                            bankAuthFlow = BankAuthFlowState.BANK_APPROVAL_COMPLETE
                        ).toPreferencesValue()
                    )
                }
            }
            analytics.logEvent(BuySellViewedEvent(BuySellFragment.BuySellViewType.TYPE_BUY))
            subscribeForNavigation()
        }
    }

    override fun onSheetClosed(sheet: BottomSheetDialogFragment) {
        super<SimpleBuyNavigator>.onSheetClosed(sheet)
        when (sheet) {
            is KycUpgradeNowSheet,
            is BlockedDueToSanctionsSheet,
            -> exitSimpleBuyFlow()
            is ErrorSlidingBottomDialog -> {
                // do nothing for now
                Timber.e("----- ErrorSlidingBottomDialog sheet closed")
            }
            else -> subscribeForNavigation(true)
        }
    }

    private fun subscribeForNavigation(failOnUnavailableCurrency: Boolean = false) {
        compositeDisposable += buyFlowNavigator.navigateTo(
            startedFromKycResume,
            startedFromDashboard,
            startedFromApprovalDeepLink,
            asset,
            failOnUnavailableCurrency
        ).trackProgress(appUtil.activityIndicator)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy {
                when (it) {
                    is BuyNavigation.CurrencySelection -> launchCurrencySelector(it.currencies, it.selectedCurrency)
                    is BuyNavigation.FlowScreenWithCurrency -> startFlow(it)
                    is BuyNavigation.BlockBuy -> blockBuy(it.reason)
                    BuyNavigation.OrderInProgressScreen -> goToPaymentScreen(false, startedFromApprovalDeepLink)
                    BuyNavigation.CurrencyNotAvailable -> finish()
                }.exhaustive
            }
    }

    private fun launchCurrencySelector(currencies: List<FiatCurrency>, selectedCurrency: FiatCurrency) {
        compositeDisposable.clear()
        showBottomSheet(
            CurrencySelectionSheet.newInstance(
                currencies = currencies,
                selectedCurrency = selectedCurrency,
                currencySelectionType = CurrencySelectionSheet.Companion.CurrencySelectionType.TRADING_CURRENCY
            )
        )
    }

    private fun startFlow(screenWithCurrency: BuyNavigation.FlowScreenWithCurrency) {
        when (screenWithCurrency.flowScreen) {
            FlowScreen.ENTER_AMOUNT -> {
                goToBuyCryptoScreen(
                    addToBackStack = false,
                    preselectedAsset = screenWithCurrency.cryptoCurrency,
                    preselectedPaymentMethodId = preselectedPaymentMethodId,
                    preselectedAmount = preselectedAmount,
                    launchLinkCard = launchLinkNewCard,
                    launchPaymentMethodSelection = launchSelectNewPaymentMethod
                )
                checkQuestionnaire()
            }
            FlowScreen.KYC -> startKyc()
            FlowScreen.KYC_VERIFICATION -> goToKycVerificationScreen(false)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()
        googlePayResponseInterceptor.clear()
        payloadScope.get<CreateBuyOrderUseCase>().stopQuoteFetching(true)
    }

    override fun exitSimpleBuyFlow() {
        if (!startedFromDashboard) {
            startActivity(MainActivity.newIntentAsNewTask(this))
        } else {
            finish()
        }
    }

    override fun popFragmentsInStackUntilFind(fragmentName: String, popInclusive: Boolean) {
        supportFragmentManager.popBackStack(
            fragmentName,
            if (popInclusive) POP_BACK_STACK_INCLUSIVE else 0
        )
    }

    override fun goToBuyCryptoScreen(
        addToBackStack: Boolean,
        preselectedAsset: AssetInfo,
        preselectedPaymentMethodId: String?,
        preselectedAmount: String?,
        launchLinkCard: Boolean,
        launchPaymentMethodSelection: Boolean,
    ) {
        supportFragmentManager.beginTransaction()
            .addAnimationTransaction()
            .replace(
                R.id.content_frame,
                SimpleBuyCryptoFragment.newInstance(
                    asset = preselectedAsset,
                    preselectedMethodId = preselectedPaymentMethodId,
                    preselectedAmount = preselectedAmount,
                    launchLinkCard = launchLinkNewCard,
                    launchPaymentMethodSelection = launchSelectNewPaymentMethod
                ),
                SimpleBuyCryptoFragment::class.simpleName
            )
            .apply {
                if (addToBackStack) {
                    addToBackStack(SimpleBuyCryptoFragment::class.simpleName)
                }
            }
            .commitAllowingStateLoss()
    }

    override fun goToCheckOutScreen(addToBackStack: Boolean) {
        hideKeyboard()
        supportFragmentManager.beginTransaction()
            .addAnimationTransaction()
            .replace(R.id.content_frame, SimpleBuyCheckoutFragment(), SimpleBuyCheckoutFragment::class.simpleName)
            .apply {
                if (addToBackStack) {
                    addToBackStack(SimpleBuyCheckoutFragment::class.simpleName)
                }
            }
            .commitAllowingStateLoss()
    }

    override fun goToPendingOrderScreen() {
        supportFragmentManager.beginTransaction()
            .addAnimationTransaction()
            .replace(
                R.id.content_frame,
                SimpleBuyCheckoutFragment.newInstance(true),
                SimpleBuyCheckoutFragment::class.simpleName
            )
            .commitAllowingStateLoss()
    }

    override fun goToKycVerificationScreen(addToBackStack: Boolean) {
        supportFragmentManager.beginTransaction()
            .addAnimationTransaction()
            .replace(R.id.content_frame, SimpleBuyPendingKycFragment(), SimpleBuyPendingKycFragment::class.simpleName)
            .apply {
                if (addToBackStack) {
                    addToBackStack(SimpleBuyPendingKycFragment::class.simpleName)
                }
            }
            .commitAllowingStateLoss()
    }

    private fun blockBuy(reason: BlockedReason) {
        when (reason) {
            is BlockedReason.InsufficientTier -> showBottomSheet(KycUpgradeNowSheet.newInstance())
            is BlockedReason.Sanctions -> showBottomSheet(BlockedDueToSanctionsSheet.newInstance(reason))
            BlockedReason.NotEligible,
            is BlockedReason.TooManyInFlightTransactions -> {
                supportFragmentManager.beginTransaction()
                    .addAnimationTransaction()
                    .replace(
                        R.id.content_frame,
                        SimpleBuyBlockedFragment.newInstance(FeatureAccess.Blocked(reason), resources),
                        SimpleBuyBlockedFragment::class.simpleName
                    )
                    .commitAllowingStateLoss()
            }
        }
    }

    override fun startKyc() {
        KycNavHostActivity.startForResult(this, CampaignType.SimpleBuy, KYC_STARTED)
    }

    override fun pop() = onBackPressed()

    override fun hasMoreThanOneFragmentInTheStack(): Boolean =
        supportFragmentManager.backStackEntryCount > 1

    override fun goToPaymentScreen(addToBackStack: Boolean, isPaymentAuthorised: Boolean) {
        supportFragmentManager.beginTransaction()
            .addAnimationTransaction()
            .replace(
                R.id.content_frame, SimpleBuyPaymentFragment.newInstance(isPaymentAuthorised),
                SimpleBuyPaymentFragment::class.simpleName
            )
            .apply {
                if (addToBackStack) {
                    addToBackStack(SimpleBuyPaymentFragment::class.simpleName)
                }
            }
            .commitAllowingStateLoss()
    }

    override fun onSupportNavigateUp(): Boolean = consume { onBackPressed() }

    override fun showLoading() = binding.progress.visible()

    override fun hideLoading() = binding.progress.gone()

    override fun goToSetupFirstRecurringBuy(addToBackStack: Boolean) {
        supportFragmentManager.beginTransaction()
            .addAnimationTransaction()
            .replace(R.id.content_frame, RecurringBuyFirstTimeBuyerFragment())
            .apply {
                if (addToBackStack) {
                    addToBackStack(SimpleBuyPaymentFragment::class.simpleName)
                }
            }
            .commitAllowingStateLoss()
    }

    override fun goToFirstRecurringBuyCreated(addToBackStack: Boolean) {
        supportFragmentManager.beginTransaction()
            .addAnimationTransaction()
            .replace(R.id.content_frame, RecurringBuySuccessfulFragment())
            .apply {
                if (addToBackStack) {
                    addToBackStack(SimpleBuyPaymentFragment::class.simpleName)
                }
            }
            .commitAllowingStateLoss()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        supportFragmentManager.fragments.forEach {
            (it as? OnGooglePayDataReceivedListener)?.let { listener ->
                googlePayResponseInterceptor.setPaymentDataReceivedListener(listener)
            }
        }
        googlePayResponseInterceptor.interceptActivityResult(requestCode, resultCode, data)
    }

    override fun startKycClicked() {
        startKyc()
    }

    override fun showErrorInBottomSheet(
        title: String,
        description: String,
        serverErrorHandling: List<ServerErrorAction>,
        error: String,
        errorDescription: String?,
        nabuApiException: NabuApiException?,
    ) {
        serverErrorHandling.assignErrorActions()

        showBottomSheet(
            ErrorSlidingBottomDialog.newInstance(
                ErrorDialogData(
                    title = title,
                    description = description,
                    errorButtonCopies = if (serverErrorHandling.isEmpty()) {
                        ErrorButtonCopies(
                            primaryButtonText = getString(R.string.common_ok)
                        )
                    } else {
                        serverErrorHandling.mapToErrorCopies()
                    },
                    error = error,
                    nabuApiException = nabuApiException,
                    errorDescription = description,
                    action = ACTION_BUY,
                    analyticsCategories = nabuApiException?.getServerSideErrorInfo()?.categories ?: emptyList()
                )
            )
        )
    }

    override fun showBankRefreshError(accountId: String) {
        primaryErrorCtaAction = {
            if (accountId.isNotEmpty()) {
                refreshBankResultLauncher.launch(Pair(accountId, BankAuthSource.SIMPLE_BUY))
            }
        }

        showBottomSheet(
            ErrorSlidingBottomDialog.newInstance(
                ErrorDialogData(
                    title = getString(R.string.common_oops_bank),
                    description = getString(R.string.trading_deposit_description_requires_update),
                    errorButtonCopies = ErrorButtonCopies(
                        primaryButtonText = getString(R.string.trading_deposit_relink_bank_account),
                        secondaryButtonText = getString(R.string.common_ok),
                    ),
                    action = ACTION_BUY,
                    analyticsCategories = emptyList()
                )
            )
        )
    }

    private fun checkQuestionnaire() {
        lifecycleScope.launchWhenCreated {
            dataRemediationService.getQuestionnaire(QuestionnaireContext.TRADING)
                .doOnSuccess { questionnaire ->
                    if (questionnaire != null) {
                        showBottomSheet(QuestionnaireSheet.newInstance(questionnaire, true))
                    }
                }
        }
    }

    private fun List<ServerErrorAction>.assignErrorActions() =
        mapIndexed { index, info ->
            when (index) {
                0 -> primaryErrorCtaAction = {
                    if (info.deeplinkPath.isNotEmpty()) {
                        redirectToDeeplinkProcessor(info.deeplinkPath)
                    }
                }
                1 -> secondaryErrorCtaAction = {
                    if (info.deeplinkPath.isNotEmpty()) {
                        redirectToDeeplinkProcessor(info.deeplinkPath)
                    }
                }
                2 -> tertiaryErrorCtaAction = {
                    if (info.deeplinkPath.isNotEmpty()) {
                        redirectToDeeplinkProcessor(info.deeplinkPath)
                    }
                }
                else -> {
                    // do nothing, only support 3 error types
                }
            }
        }

    private fun redirectToDeeplinkProcessor(link: String) {
        compositeDisposable += deeplinkRedirector.processDeeplinkURL(
            link.appendTickerToDeeplink()
        ).emptySubscribe()
    }

    private fun String.appendTickerToDeeplink(): Uri =
        Uri.parse("$this?code=${asset?.networkTicker}")

    override fun onErrorPrimaryCta() {
        primaryErrorCtaAction()
    }

    override fun onErrorSecondaryCta() {
        secondaryErrorCtaAction()
    }

    override fun onErrorTertiaryCta() {
        tertiaryErrorCtaAction()
    }

    override fun onSheetClosed() {
        // do nothing
    }

    override fun goToBlockedBuyScreen() {
        blockBuy(BlockedReason.NotEligible)
    }

    override fun questionnaireSubmittedSuccessfully() {
        // no op
    }

    override fun questionnaireSkipped() {
        // no op
    }

    companion object {
        const val KYC_STARTED = 6788
        const val RESULT_KYC_SIMPLE_BUY_COMPLETE = 7854

        private const val STARTED_FROM_NAVIGATION_KEY = "started_from_navigation_key"
        private const val STARTED_FROM_APPROVAL_KEY = "STARTED_FROM_APPROVAL_KEY"
        private const val ASSET_KEY = "crypto_currency_key"
        private const val PRESELECTED_PAYMENT_METHOD = "preselected_payment_method_key"
        private const val PRESELECTED_AMOUNT = "preselected_amount_key"
        private const val STARTED_FROM_KYC_RESUME = "started_from_kyc_resume_key"
        private const val LAUNCH_LINK_CARD = "launch_link_card"
        private const val LAUNCH_SELECT_PAYMENT_METHOD = "launch_select_new_method"

        fun newIntent(
            context: Context,
            asset: AssetInfo? = null,
            launchFromNavigationBar: Boolean = false,
            launchKycResume: Boolean = false,
            preselectedPaymentMethodId: String? = null,
            preselectedAmount: String? = null,
            launchFromApprovalDeepLink: Boolean = false,
            launchLinkCard: Boolean = false,
            launchNewPaymentMethodSelection: Boolean = false,
        ) = Intent(context, SimpleBuyActivity::class.java).apply {
            putExtra(STARTED_FROM_NAVIGATION_KEY, launchFromNavigationBar)
            putExtra(ASSET_KEY, asset?.networkTicker)
            putExtra(STARTED_FROM_KYC_RESUME, launchKycResume)
            putExtra(PRESELECTED_PAYMENT_METHOD, preselectedPaymentMethodId)
            putExtra(PRESELECTED_AMOUNT, preselectedAmount)
            putExtra(STARTED_FROM_APPROVAL_KEY, launchFromApprovalDeepLink)
            putExtra(LAUNCH_LINK_CARD, launchLinkCard)
            putExtra(LAUNCH_SELECT_PAYMENT_METHOD, launchNewPaymentMethodSelection)
        }
    }
}
