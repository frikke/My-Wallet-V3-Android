package piuk.blockchain.android.simplebuy

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
import androidx.lifecycle.lifecycleScope
import com.blockchain.api.NabuApiException
import com.blockchain.coincore.AssetAction
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.commonarch.presentation.base.addTransactionAnimation
import com.blockchain.commonarch.presentation.base.trackProgress
import com.blockchain.componentlib.databinding.FragmentActivityBinding
import com.blockchain.componentlib.databinding.ToolbarGeneralBinding
import com.blockchain.componentlib.utils.checkValidUrlAndOpen
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.hideKeyboard
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.core.recurringbuy.domain.model.RecurringBuyFrequency
import com.blockchain.deeplinking.navigation.DeeplinkRedirector
import com.blockchain.deeplinking.processor.DeepLinkResult
import com.blockchain.deeplinking.processor.DeeplinkProcessorV2.Companion.ASSET_URL
import com.blockchain.deeplinking.processor.DeeplinkProcessorV2.Companion.PARAMETER_CODE
import com.blockchain.deeplinking.processor.DeeplinkProcessorV2.Companion.PARAMETER_RECURRING_BUY_ID
import com.blockchain.domain.common.model.BuySellViewType
import com.blockchain.domain.common.model.ServerErrorAction
import com.blockchain.domain.common.model.ServerSideUxErrorInfo
import com.blockchain.domain.dataremediation.DataRemediationService
import com.blockchain.domain.dataremediation.model.QuestionnaireContext
import com.blockchain.domain.fiatcurrencies.FiatCurrenciesService
import com.blockchain.domain.paymentmethods.model.BankAuthDeepLinkState
import com.blockchain.domain.paymentmethods.model.BankAuthFlowState
import com.blockchain.domain.paymentmethods.model.BankAuthSource
import com.blockchain.domain.paymentmethods.model.fromPreferencesValue
import com.blockchain.domain.paymentmethods.model.toPreferencesValue
import com.blockchain.extensions.exhaustive
import com.blockchain.fiatActions.QuestionnaireSheetHost
import com.blockchain.home.presentation.navigation.AssetActionsNavigation
import com.blockchain.koin.payloadScope
import com.blockchain.koin.payloadScopeOrNull
import com.blockchain.nabu.BlockedReason
import com.blockchain.nabu.FeatureAccess
import com.blockchain.outcome.doOnSuccess
import com.blockchain.payments.googlepay.interceptor.GooglePayResponseInterceptor
import com.blockchain.payments.googlepay.interceptor.OnGooglePayDataReceivedListener
import com.blockchain.preferences.BankLinkingPrefs
import com.blockchain.presentation.customviews.kyc.KycUpgradeNowSheet
import com.blockchain.presentation.koin.scopedInject
import com.blockchain.transactions.upsell.buy.UpsellBuyBottomSheet
import com.blockchain.utils.consume
import com.blockchain.utils.unsafeLazy
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.FiatCurrency
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import org.koin.android.ext.android.inject
import org.koin.core.parameter.parametersOf
import piuk.blockchain.android.R
import piuk.blockchain.android.fraud.domain.service.FraudFlow
import piuk.blockchain.android.fraud.domain.service.FraudService
import piuk.blockchain.android.simplebuy.ClientErrorAnalytics.Companion.ACTION_BUY
import piuk.blockchain.android.simplebuy.ClientErrorAnalytics.Companion.SETTLEMENT_REFRESH_REQUIRED
import piuk.blockchain.android.simplebuy.sheets.CurrencySelectionSheet
import piuk.blockchain.android.ui.base.ErrorButtonCopies
import piuk.blockchain.android.ui.base.ErrorDialogData
import piuk.blockchain.android.ui.base.ErrorSlidingBottomDialog
import piuk.blockchain.android.ui.base.mapToErrorCopies
import piuk.blockchain.android.ui.customviews.BlockedDueToSanctionsSheet
import piuk.blockchain.android.ui.dataremediation.QuestionnaireSheet
import piuk.blockchain.android.ui.home.HomeActivityLauncher
import piuk.blockchain.android.ui.kyc.navhost.KycNavHostActivity
import piuk.blockchain.android.ui.kyc.navhost.models.KycEntryPoint
import piuk.blockchain.android.ui.linkbank.BankAuthRefreshContract

class SimpleBuyActivity :
    BlockchainActivity(),
    SimpleBuyNavigator,
    KycUpgradeNowSheet.Host,
    QuestionnaireSheetHost,
    RecurringBuyCreatedBottomSheet.Host,
    ErrorSlidingBottomDialog.Host,
    CurrencySelectionSheet.Host,
    UpsellBuyBottomSheet.Host {
    override val alwaysDisableScreenshots: Boolean
        get() = false

    private var isUpdatingCurrency = false

    override val enableLogoutTimer: Boolean = false
    private val compositeDisposable = CompositeDisposable()
    private val buyFlowNavigator: BuyFlowNavigator by scopedInject()
    private val bankLinkingPrefs: BankLinkingPrefs by scopedInject()
    private val deeplinkRedirector: DeeplinkRedirector by scopedInject()
    private val assetCatalogue: AssetCatalogue by inject()
    private val googlePayResponseInterceptor: GooglePayResponseInterceptor by inject()
    private val dataRemediationService: DataRemediationService by scopedInject()
    private val fiatCurrenciesService: FiatCurrenciesService by scopedInject()
    private val fraudService: FraudService by inject()

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

    private val preselectedFiatTicker: String? by unsafeLazy {
        intent.getStringExtra(PRESELECTED_FIAT_TICKER)
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

    private val assetActionsNavigation: AssetActionsNavigation = payloadScope.get {
        parametersOf(
            this
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        updateToolbar(
            toolbarTitle = getString(com.blockchain.stringResources.R.string.common_buy),
            backAction = { onBackPressedDispatcher.onBackPressed() }
        )
        analytics.logEvent(BuyAssetScreenViewedEvent)
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
            analytics.logEvent(BuySellViewedEvent(BuySellViewType.TYPE_BUY))
            subscribeForNavigation()
        }
    }

    override fun onSheetClosed(sheet: BottomSheetDialogFragment) {
        super<SimpleBuyNavigator>.onSheetClosed(sheet)
        when (sheet) {
            is KycUpgradeNowSheet,
            is BlockedDueToSanctionsSheet -> exitSimpleBuyFlow()
            is CurrencySelectionSheet -> {
                // We're using this var because onSheetClosed will always be called, and in case the user
                // has picked a new currency, we want to wait till it's changed in the BE, so if the user
                // changed the currency we don't want to do anything here.
                if (!isUpdatingCurrency) subscribeForNavigation(true)
            }
            is ErrorSlidingBottomDialog -> {
                // do nothing
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
                    BuyNavigation.OrderInProgressScreen -> goToPaymentScreen(
                        addToBackStack = false,
                        isPaymentAuthorised = startedFromApprovalDeepLink
                    )
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
                currencySelectionType = CurrencySelectionSheet.CurrencySelectionType.TRADING_CURRENCY
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
                    preselectedFiatTicker = preselectedFiatTicker,
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
        fraudService.endFlows(
            FraudFlow.ACH_DEPOSIT,
            FraudFlow.OB_DEPOSIT,
            FraudFlow.CARD_DEPOSIT,
            FraudFlow.MOBILE_WALLET_DEPOSIT
        )
        compositeDisposable.clear()
        googlePayResponseInterceptor.clear()
        payloadScopeOrNull?.get<CreateBuyOrderUseCase>()?.stopQuoteFetching(true)
    }
    private val homeActivityLauncher: HomeActivityLauncher by inject()

    override fun exitSimpleBuyFlow() {
        setResult(RESULT_OK)

        if (!startedFromDashboard) {
            startActivity(homeActivityLauncher.newIntentAsNewTask(this))
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
        preselectedFiatTicker: String?
    ) {
        analytics.logEvent(BuyAssetSelectedEvent(type = preselectedAsset.networkTicker))
        supportFragmentManager.beginTransaction()
            .addTransactionAnimation()
            .replace(
                R.id.content_frame,
                SimpleBuyCryptoFragment.newInstance(
                    asset = preselectedAsset,
                    preselectedMethodId = preselectedPaymentMethodId,
                    preselectedAmount = preselectedAmount,
                    preselectedFiatTicker = preselectedFiatTicker,
                    launchLinkCard = launchLinkNewCard,
                    launchPaymentMethodSelection = launchSelectNewPaymentMethod,
                    fromRecurringBuy = intent.getBooleanExtra(ARG_FROM_RECURRING_BUY, false)
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
            .addTransactionAnimation()
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
            .addTransactionAnimation()
            .replace(
                R.id.content_frame,
                SimpleBuyCheckoutFragment.newInstance(true),
                SimpleBuyCheckoutFragment::class.simpleName
            )
            .commitAllowingStateLoss()
    }

    override fun goToKycVerificationScreen(addToBackStack: Boolean) {
        supportFragmentManager.beginTransaction()
            .addTransactionAnimation()
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
            is BlockedReason.NotEligible,
            is BlockedReason.TooManyInFlightTransactions -> {
                supportFragmentManager.beginTransaction()
                    .addTransactionAnimation()
                    .replace(
                        R.id.content_frame,
                        SimpleBuyBlockedFragment.newInstance(FeatureAccess.Blocked(reason), resources),
                        SimpleBuyBlockedFragment::class.simpleName
                    )
                    .commitAllowingStateLoss()
            }
            is BlockedReason.ShouldAcknowledgeStakingWithdrawal -> {
                // do nothing
            }
            is BlockedReason.ShouldAcknowledgeActiveRewardsWithdrawalWarning -> {
                // do nothing
            }
        }
    }

    override fun startKyc() {
        KycNavHostActivity.startForResult(this, KycEntryPoint.Buy, KYC_STARTED)
    }

    override fun pop() = onBackPressedDispatcher.onBackPressed()

    override fun hasMoreThanOneFragmentInTheStack(): Boolean =
        supportFragmentManager.backStackEntryCount > 1

    override fun goToPaymentScreen(
        addToBackStack: Boolean,
        isPaymentAuthorised: Boolean,
        showRecurringBuySuggestion: Boolean,
        recurringBuyFrequencyRemote: RecurringBuyFrequency?
    ) {
        supportFragmentManager.beginTransaction()
            .addTransactionAnimation()
            .replace(
                R.id.content_frame,
                SimpleBuyPaymentFragment.newInstance(
                    isFromDeepLink = isPaymentAuthorised,
                    showRecurringBuySuggestion = showRecurringBuySuggestion,
                    recurringBuyFrequency = recurringBuyFrequencyRemote
                ),
                SimpleBuyPaymentFragment::class.simpleName
            )
            .apply {
                if (addToBackStack) {
                    addToBackStack(SimpleBuyPaymentFragment::class.simpleName)
                }
            }
            .commitAllowingStateLoss()
    }

    override fun onSupportNavigateUp(): Boolean = consume { onBackPressedDispatcher.onBackPressed() }

    override fun showLoading() = binding.progress.visible()

    override fun hideLoading() = binding.progress.gone()

    override fun onCurrencyChanged(
        currency: FiatCurrency,
        selectionType: CurrencySelectionSheet.CurrencySelectionType
    ) {
        when (selectionType) {
            CurrencySelectionSheet.CurrencySelectionType.DISPLAY_CURRENCY ->
                throw UnsupportedOperationException()
            CurrencySelectionSheet.CurrencySelectionType.TRADING_CURRENCY -> {
                isUpdatingCurrency = true
                lifecycleScope.launchWhenCreated {
                    fiatCurrenciesService.setSelectedTradingCurrency(currency)
                    subscribeForNavigation(true)
                }
            }
        }
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
        error: String,
        errorDescription: String?,
        nabuApiException: NabuApiException?,
        serverSideUxErrorInfo: ServerSideUxErrorInfo?,
        closeFlowOnDeeplinkFallback: Boolean
    ) {
        serverSideUxErrorInfo?.actions?.assignErrorActions(closeFlowOnDeeplinkFallback)

        showBottomSheet(
            ErrorSlidingBottomDialog.newInstance(
                ErrorDialogData(
                    title = title,
                    description = description,
                    errorButtonCopies = if (serverSideUxErrorInfo?.actions?.isEmpty() == true) {
                        ErrorButtonCopies(
                            primaryButtonText = getString(com.blockchain.stringResources.R.string.common_ok)
                        )
                    } else {
                        serverSideUxErrorInfo?.actions?.mapToErrorCopies()
                    },
                    error = error,
                    nabuApiException = nabuApiException,
                    errorDescription = description,
                    action = ACTION_BUY,
                    analyticsCategories = serverSideUxErrorInfo?.categories
                        ?: nabuApiException?.getServerSideErrorInfo()?.categories.orEmpty(),
                    iconUrl = serverSideUxErrorInfo?.iconUrl ?: nabuApiException?.getServerSideErrorInfo()?.iconUrl,
                    statusIconUrl = serverSideUxErrorInfo?.statusUrl
                        ?: nabuApiException?.getServerSideErrorInfo()?.statusUrl,
                    errorId = serverSideUxErrorInfo?.id ?: nabuApiException?.getServerSideErrorInfo()?.id
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
                    title = getString(com.blockchain.stringResources.R.string.common_oops_bank),
                    description = getString(
                        com.blockchain.stringResources.R.string.trading_deposit_description_requires_update
                    ),
                    errorButtonCopies = ErrorButtonCopies(
                        primaryButtonText = getString(
                            com.blockchain.stringResources.R.string.trading_deposit_relink_bank_account
                        ),
                        secondaryButtonText = getString(com.blockchain.stringResources.R.string.common_ok)
                    ),
                    error = SETTLEMENT_REFRESH_REQUIRED,
                    action = ACTION_BUY,
                    analyticsCategories = emptyList()
                )
            )
        )
    }

    private fun checkQuestionnaire() {
        lifecycleScope.launchWhenResumed {
            dataRemediationService.getQuestionnaire(QuestionnaireContext.TRADING)
                .doOnSuccess { questionnaire ->
                    if (questionnaire != null) {
                        showBottomSheet(QuestionnaireSheet.newInstance(questionnaire, true))
                    }
                }
        }
    }

    private fun List<ServerErrorAction>.assignErrorActions(closeFlowOnDeeplinkFallback: Boolean) =
        mapIndexed { index, info ->
            when (index) {
                0 -> primaryErrorCtaAction = {
                    if (info.deeplinkPath.isNotEmpty()) {
                        redirectToDeeplinkProcessor(info.deeplinkPath)
                    } else if (closeFlowOnDeeplinkFallback) {
                        finish()
                    }
                }
                1 -> secondaryErrorCtaAction = {
                    if (info.deeplinkPath.isNotEmpty()) {
                        redirectToDeeplinkProcessor(info.deeplinkPath)
                    } else if (closeFlowOnDeeplinkFallback) {
                        finish()
                    }
                }
                2 -> tertiaryErrorCtaAction = {
                    if (info.deeplinkPath.isNotEmpty()) {
                        redirectToDeeplinkProcessor(info.deeplinkPath)
                    } else if (closeFlowOnDeeplinkFallback) {
                        finish()
                    }
                }
                else -> {
                    // do nothing, only support 3 error types
                }
            }
        }

    override fun onErrorPrimaryCta() {
        primaryErrorCtaAction()
    }

    override fun onErrorSecondaryCta() {
        secondaryErrorCtaAction()
    }

    override fun onErrorTertiaryCta() {
        tertiaryErrorCtaAction()
    }

    private fun processDeeplink(deepLink: Uri) {
        compositeDisposable += deeplinkRedirector.processDeeplinkURL(deepLink).subscribeBy(
            onSuccess = {
                if (it is DeepLinkResult.DeepLinkResultUnknownLink) {
                    it.uri?.let { uri ->
                        this@SimpleBuyActivity.checkValidUrlAndOpen(uri)
                    }
                }
            }
        )
    }

    private fun redirectToDeeplinkProcessor(link: String) {
        processDeeplink(Uri.parse(link.appendTickerToDeeplink()))
    }

    override fun viewRecurringBuy(recurringBuyId: String) {
        processDeeplink(
            Uri.parse(ASSET_URL.appendTickerToDeeplink().appendRecurringBuyId(recurringBuyId))
        )
    }

    private fun String.appendTickerToDeeplink() = "$this?$PARAMETER_CODE=${asset?.networkTicker}"

    private fun String.appendRecurringBuyId(recurringBuyId: String) =
        "$this&$PARAMETER_RECURRING_BUY_ID=$recurringBuyId"

    override fun skip() = finish()

    override fun onSheetClosed() {
        // do nothing
    }

    override fun goToBlockedBuyScreen() {
        blockBuy(BlockedReason.NotEligible(null))
    }

    override fun questionnaireSubmittedSuccessfully() {
        // no op
    }

    override fun questionnaireSkipped() {
        // no op
    }

    override fun launchUpSellBottomSheet(assetBoughtTicker: String) {
        showBottomSheet(
            UpsellBuyBottomSheet.newInstance(
                assetTransactedTicker = assetBoughtTicker,
                title = getString(com.blockchain.stringResources.R.string.asset_upsell_title),
                description = getString(com.blockchain.stringResources.R.string.asset_upsell_subtitle)
            )
        )
    }

    override fun launchBuyForAsset(networkTicker: String) {
        assetCatalogue.assetInfoFromNetworkTicker(networkTicker)?.let { asset ->
            assetActionsNavigation.buyCrypto(asset)
            finish()
        }
    }

    override fun launchBuy() {
        assetActionsNavigation.navigate(AssetAction.Buy)
        finish()
    }

    override fun onCloseUpsellAnotherAsset() {
        exitSimpleBuyFlow()
    }

    companion object {
        const val KYC_STARTED = 6788
        const val RESULT_KYC_SIMPLE_BUY_COMPLETE = 7854

        private const val STARTED_FROM_NAVIGATION_KEY = "started_from_navigation_key"
        private const val STARTED_FROM_APPROVAL_KEY = "STARTED_FROM_APPROVAL_KEY"
        private const val ASSET_KEY = "crypto_currency_key"
        private const val PRESELECTED_PAYMENT_METHOD = "preselected_payment_method_key"
        private const val PRESELECTED_AMOUNT = "preselected_amount_key"
        private const val PRESELECTED_FIAT_TICKER = "preselected_fiat_key"
        private const val STARTED_FROM_KYC_RESUME = "started_from_kyc_resume_key"
        private const val LAUNCH_LINK_CARD = "launch_link_card"
        private const val LAUNCH_SELECT_PAYMENT_METHOD = "launch_select_new_method"
        private const val ARG_FROM_RECURRING_BUY = "ARG_FROM_RECURRING_BUY"

        fun newIntent(
            context: Context,
            asset: AssetInfo? = null,
            launchFromNavigationBar: Boolean = false,
            launchKycResume: Boolean = false,
            preselectedPaymentMethodId: String? = null,
            preselectedAmount: String? = null,
            preselectedFiatTicker: String? = null,
            launchFromApprovalDeepLink: Boolean = false,
            launchLinkCard: Boolean = false,
            launchNewPaymentMethodSelection: Boolean = false,
            fromRecurringBuy: Boolean = false
        ) = Intent(context, SimpleBuyActivity::class.java).apply {
            putExtra(STARTED_FROM_NAVIGATION_KEY, launchFromNavigationBar)
            putExtra(ASSET_KEY, asset?.networkTicker)
            putExtra(STARTED_FROM_KYC_RESUME, launchKycResume)
            putExtra(PRESELECTED_PAYMENT_METHOD, preselectedPaymentMethodId)
            putExtra(PRESELECTED_AMOUNT, preselectedAmount)
            putExtra(PRESELECTED_FIAT_TICKER, preselectedFiatTicker)
            putExtra(STARTED_FROM_APPROVAL_KEY, launchFromApprovalDeepLink)
            putExtra(LAUNCH_LINK_CARD, launchLinkCard)
            putExtra(LAUNCH_SELECT_PAYMENT_METHOD, launchNewPaymentMethodSelection)
            putExtra(ARG_FROM_RECURRING_BUY, fromRecurringBuy)
        }
    }
}
