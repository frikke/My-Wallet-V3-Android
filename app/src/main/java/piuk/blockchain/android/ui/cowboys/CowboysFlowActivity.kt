package piuk.blockchain.android.ui.cowboys

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ChainStyle
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.fragment.app.FragmentManager
import coil.annotation.ExperimentalCoilApi
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.basic.MarkdownText
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.media.AsyncMediaItem
import com.blockchain.componentlib.navigation.NavigationBar
import com.blockchain.componentlib.navigation.NavigationBarButton
import com.blockchain.componentlib.system.EmbeddedFragment
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Blue600
import com.blockchain.core.kyc.domain.model.KycTier
import com.blockchain.deeplinking.processor.DeeplinkProcessorV2.Companion.BUY_URL
import com.blockchain.deeplinking.processor.DeeplinkProcessorV2.Companion.KYC_URL
import com.blockchain.domain.common.model.PromotionStyleInfo
import com.blockchain.domain.common.model.ServerErrorAction
import com.blockchain.domain.paymentmethods.model.PaymentMethod
import com.blockchain.koin.scopedInject
import info.blockchain.balance.AssetCatalogue
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import piuk.blockchain.android.R
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.simplebuy.SimpleBuyActivity
import piuk.blockchain.android.ui.home.MainActivity
import piuk.blockchain.android.ui.kyc.email.entry.EmailEntryHost
import piuk.blockchain.android.ui.kyc.email.entry.KycEmailEntryFragment
import piuk.blockchain.android.ui.kyc.navhost.KycNavHostActivity
import piuk.blockchain.android.ui.kyc.navhost.KycNavHostActivity.Companion.RESULT_KYC_FOR_SDD_COMPLETE
import timber.log.Timber

class CowboysFlowActivity : BlockchainActivity(), EmailEntryHost {

    private var flowStep by mutableStateOf(FlowStep.Welcome)
    private var interstitialData by mutableStateOf<PromotionStyleInfo?>(null)
    private var shouldShowEmailSkipButton by mutableStateOf(false)
    private var emailSkipAction: () -> Unit = {}
    private val assetCatalogue: AssetCatalogue by scopedInject()
    private val cowboysDataProvider: CowboysPromoDataProvider by scopedInject()
    private val compositeDisposable = CompositeDisposable()

    private val simpleBuyActivityResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            flowStep = FlowStep.Verify
            loadDataForStep(flowStep)
        } else if (it.resultCode == RESULT_CANCELED) {
            navigateToMainActivity()
        }
    }

    private val startingFlowStep by lazy {
        intent.getSerializableExtra(FLOW_STEP) as FlowStep
    }

    override val alwaysDisableScreenshots: Boolean
        get() = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        flowStep = startingFlowStep
        loadDataForStep(flowStep)
        logViewAnalyticsForStep(flowStep)

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    logCloseEventAnalyticsForStep()
                    navigateToMainActivity()
                }
            }
        )

        setContentView(
            ComposeView(this).apply {
                setContent {
                    AppTheme {
                        if (flowStep == FlowStep.EmailVerification) {
                            EmailKycHost(
                                shouldShowEmailSkipButton = shouldShowEmailSkipButton,
                                emailSkipAction = emailSkipAction,
                                fragmentManager = supportFragmentManager
                            )
                        } else {
                            interstitialData?.let { data ->
                                CowboysInterstitial(
                                    info = data,
                                    onPrimaryCtaClick = {
                                        logPrimaryCtaAnalyticsForStep()
                                        getPrimaryCtaAction(data.actions[0].deeplinkPath)
                                    },
                                    onCloseClicked = {
                                        logCloseEventAnalyticsForStep()
                                        navigateToMainActivity()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        )
    }

    override fun onEmailEntryFragmentUpdated(shouldShowButton: Boolean, buttonAction: () -> Unit) {
        emailSkipAction = buttonAction
        shouldShowEmailSkipButton = shouldShowButton
    }

    override fun onEmailVerified() {
        flowStep = FlowStep.Welcome
        loadDataForStep(flowStep)
    }

    override fun onEmailVerificationSkipped() {
        finish()
    }

    private fun getPrimaryCtaAction(deeplinkPath: String) {
        with(deeplinkPath) {
            when {
                contains(KYC_URL) -> {
                    when (this.split("=")[1].toInt()) {
                        KycTier.SILVER.ordinal -> {
                            launchKycForResult(SDD_REQUEST, CampaignType.SimpleBuy)
                        }
                        else -> {
                            launchKycForResult(GOLD_VERIFICATION_REQUEST)
                        }
                    }
                }
                contains(BUY_URL) -> {
                    assetCatalogue.assetInfoFromNetworkTicker(
                        BTC_TICKER
                    )?.let { assetInfo ->
                        simpleBuyActivityResult.launch(
                            SimpleBuyActivity.newIntent(
                                this@CowboysFlowActivity,
                                assetInfo,
                                // this flag needs to be true so the activity closes
                                // itself without starting the dashboard
                                launchFromNavigationBar = true,
                                preselectedPaymentMethodId =
                                PaymentMethod.UNDEFINED_CARD_PAYMENT_ID
                            )
                        )
                    }
                }
                else -> {
                    Timber.e("Cowboys action url - Unknown link: $this")
                }
            }
        }
    }

    private fun launchKycForResult(requestCode: Int, campaignType: CampaignType = CampaignType.None) {
        KycNavHostActivity.startForResult(
            activity = this@CowboysFlowActivity,
            campaignType = campaignType,
            requestCode = requestCode
        )
    }

    private fun loadDataForStep(flowStep: FlowStep) {
        compositeDisposable += when (flowStep) {
            FlowStep.Welcome -> cowboysDataProvider.getWelcomeInterstitial()
                .cache()
            FlowStep.Raffle -> cowboysDataProvider.getRaffleInterstitial()
                .cache()
            FlowStep.Verify -> cowboysDataProvider.getIdentityInterstitial()
                .cache()
            FlowStep.EmailVerification -> Single.error(StepHasNoDataException())
        }.observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = {
                    interstitialData = it
                },
                onError = {
                    if (it !is StepHasNoDataException) {
                        Timber.e("Cowboys data acquisition error: ${it.message}")
                        finish()
                    }
                }
            )
    }

    private fun navigateToMainActivity() {
        startActivity(
            MainActivity.newIntent(
                this@CowboysFlowActivity,
                null,
                shouldLaunchUiTour = false,
                shouldBeNewTask = true
            )
        )

        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            SDD_REQUEST -> {
                if (resultCode == RESULT_KYC_FOR_SDD_COMPLETE) {
                    flowStep = FlowStep.Raffle
                    loadDataForStep(flowStep)
                } else {
                    navigateToMainActivity()
                }
            }
            GOLD_VERIFICATION_REQUEST -> {
                navigateToMainActivity()
            }
        }
    }

    override fun onDestroy() {
        compositeDisposable.clear()
        super.onDestroy()
    }

    private fun logCloseEventAnalyticsForStep() =
        when (flowStep) {
            FlowStep.Welcome -> analytics.logEvent(CowboysAnalytics.WelcomeInterstitialClosed)
            FlowStep.Raffle -> analytics.logEvent(CowboysAnalytics.RaffleInterstitialClosed)
            FlowStep.Verify -> analytics.logEvent(CowboysAnalytics.VerifyIdInterstitialClosed)
            FlowStep.EmailVerification -> {
                // no-op
            }
        }

    private fun logPrimaryCtaAnalyticsForStep() =
        when (flowStep) {
            FlowStep.Welcome -> analytics.logEvent(CowboysAnalytics.WelcomeInterstitialContinueClicked)
            FlowStep.Raffle -> analytics.logEvent(CowboysAnalytics.RaffleInterstitialBuyClicked)
            FlowStep.Verify -> analytics.logEvent(CowboysAnalytics.VerifyIdInterstitialCtaClicked)
            FlowStep.EmailVerification -> {
                // no-op
            }
        }

    private fun logViewAnalyticsForStep(flowStep: FlowStep) =
        when (flowStep) {
            FlowStep.Welcome -> analytics.logEvent(CowboysAnalytics.WelcomeInterstitialViewed)
            FlowStep.Raffle -> analytics.logEvent(CowboysAnalytics.RaffleInterstitialViewed)
            FlowStep.Verify -> analytics.logEvent(CowboysAnalytics.VerifyIdInterstitialViewed)
            FlowStep.EmailVerification -> {
                // no-op
            }
        }

    companion object {
        private const val SDD_REQUEST = 567
        private const val GOLD_VERIFICATION_REQUEST = 890
        private const val BTC_TICKER = "BTC"
        private const val FLOW_STEP = "FLOW_STEP"
        fun newIntent(context: Context, flowStep: FlowStep = FlowStep.Welcome) =
            Intent(context, CowboysFlowActivity::class.java).apply {
                putExtra(FLOW_STEP, flowStep)
            }
    }
}

@Composable
fun EmailKycHost(
    shouldShowEmailSkipButton: Boolean,
    emailSkipAction: () -> Unit,
    fragmentManager: FragmentManager
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        val emailKycFragment = remember { KycEmailEntryFragment.newInstance(true) }

        NavigationBar(
            title = stringResource(R.string.security_check),
            endNavigationBarButtons = if (shouldShowEmailSkipButton) {
                listOf(
                    NavigationBarButton.Text(
                        color = Blue600,
                        text = stringResource(R.string.common_skip),
                        onTextClick = emailSkipAction
                    )
                )
            } else {
                emptyList()
            }
        )

        EmbeddedFragment(
            fragment = emailKycFragment,
            fragmentManager = fragmentManager,
            tag = KycEmailEntryFragment.javaClass.simpleName,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@OptIn(ExperimentalCoilApi::class)
@Composable
fun CowboysInterstitial(
    info: PromotionStyleInfo,
    onPrimaryCtaClick: () -> Unit,
    onCloseClicked: () -> Unit
) {

    ConstraintLayout {
        val (primaryButton, closeButton, icon, backgroundImage, foregroundImage, title, subtitle) = createRefs()

        AsyncMediaItem(
            modifier = Modifier
                .constrainAs(backgroundImage) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    bottom.linkTo(parent.bottom)
                    width = Dimension.fillToConstraints
                },
            url = info.backgroundUrl,
            contentScale = ContentScale.FillWidth,
            contentDescription = "cowboys background"
        )

        if (info.headerUrl.isNotEmpty()) {
            AsyncMediaItem(
                modifier = Modifier
                    .wrapContentHeight()
                    .padding(top = dimensionResource(R.dimen.large_spacing))
                    .constrainAs(foregroundImage) {
                        top.linkTo(parent.top)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        bottom.linkTo(
                            if (info.iconUrl.isNotEmpty()) {
                                icon.top
                            } else {
                                title.top
                            }
                        )
                        width = Dimension.fillToConstraints
                    },
                url = info.headerUrl,
                contentDescription = "cowboys header"
            )
        }

        if (info.iconUrl.isNotEmpty()) {
            AsyncMediaItem(
                modifier = Modifier
                    .requiredSizeIn(
                        minWidth = dimensionResource(id = R.dimen.epic_spacing),
                        minHeight = dimensionResource(id = R.dimen.epic_spacing),
                        maxWidth = 100.dp,
                        maxHeight = 100.dp
                    )
                    .constrainAs(icon) {
                        top.linkTo(parent.top)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        bottom.linkTo(title.top)
                    },
                url = info.iconUrl,
                contentDescription = "cowboys icon"
            )
        }

        MarkdownText(
            modifier = Modifier
                .padding(
                    start = dimensionResource(id = R.dimen.standard_spacing),
                    end = dimensionResource(id = R.dimen.standard_spacing),
                    top = dimensionResource(id = R.dimen.very_small_spacing)
                )
                .constrainAs(title) {
                    top.linkTo(
                        if (info.iconUrl.isNotEmpty()) {
                            icon.bottom
                        } else {
                            foregroundImage.bottom
                        }
                    )
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    bottom.linkTo(subtitle.top, margin = 8.dp)
                },
            markdownText = info.title,
            style = ComposeTypographies.Title1,
            gravity = ComposeGravities.Centre,
            color = ComposeColors.Light
        )

        MarkdownText(
            modifier = Modifier
                .padding(
                    start = dimensionResource(id = R.dimen.standard_spacing),
                    end = dimensionResource(id = R.dimen.standard_spacing),
                    top = dimensionResource(id = R.dimen.tiny_spacing)
                )
                .constrainAs(subtitle) {
                    top.linkTo(title.bottom, margin = 16.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    bottom.linkTo(primaryButton.top, margin = 16.dp)
                },
            markdownText = info.message,
            style = ComposeTypographies.Body1,
            gravity = ComposeGravities.Centre,
            color = ComposeColors.Light
        )

        createVerticalChain(
            foregroundImage, icon, title, subtitle,
            chainStyle = ChainStyle.Packed(0f)
        )

        if (info.actions.isNotEmpty()) {
            PrimaryButton(
                modifier = Modifier
                    .constrainAs(primaryButton) {
                        bottom.linkTo(parent.bottom, margin = 24.dp)
                        start.linkTo(parent.start, margin = 24.dp)
                        end.linkTo(parent.end, margin = 24.dp)
                        width = Dimension.fillToConstraints
                    },
                text = info.actions[0].title,
                onClick = onPrimaryCtaClick
            )
        }

        Image(
            imageResource = ImageResource.Local(R.drawable.ic_close_circle, null),
            modifier = Modifier
                .padding(all = dimensionResource(id = R.dimen.standard_spacing))
                .clickable(true, onClick = onCloseClicked)
                .constrainAs(closeButton) {
                    top.linkTo(parent.top)
                    end.linkTo(parent.end)
                }
        )
    }
}

@Preview
@Composable
fun CowboysInterstitial() {
    AppTheme {
        AppSurface {
            CowboysInterstitial(
                info = PromotionStyleInfo(
                    title = "Welcome cowboys",
                    message = "some longer text here to see how it looks",
                    iconUrl = "https://firebasestorage.googleapis.com/v0/b/fir-staging-92d79.appspot.com/o/" +
                        "announcement.png?alt=media&token=7fa38942-64ed-49ea-ab42-21e5e3ed9afd",
                    headerUrl = "https://firebasestorage.googleapis.com/v0/b/fir-staging-92d79.appspot.com/o/" +
                        "icon-cowboys-circle.svg?alt=media&token=c526e63a-de56-4668-85eb-ecc402c35feb",
                    backgroundUrl = "",
                    foregroundColorScheme = emptyList(),
                    actions = listOf(
                        ServerErrorAction(
                            "primary cta", ""
                        ),
                        ServerErrorAction(
                            "secondary cta", ""
                        )
                    )
                ),
                onPrimaryCtaClick = { }, onCloseClicked = {}
            )
        }
    }
}

private class StepHasNoDataException : Throwable()
