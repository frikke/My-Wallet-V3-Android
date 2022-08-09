package piuk.blockchain.android.ui.cowboys

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.MinimalButton
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.deeplinking.processor.DeeplinkProcessorV2.Companion.BUY_URL
import com.blockchain.deeplinking.processor.DeeplinkProcessorV2.Companion.KYC_URL
import com.blockchain.domain.common.model.ServerErrorAction
import com.blockchain.domain.paymentmethods.model.PaymentMethod
import com.blockchain.koin.scopedInject
import com.blockchain.nabu.Tier
import info.blockchain.balance.AssetCatalogue
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import piuk.blockchain.android.R
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.simplebuy.SimpleBuyActivity
import piuk.blockchain.android.ui.home.MainActivity
import piuk.blockchain.android.ui.kyc.navhost.KycNavHostActivity
import piuk.blockchain.android.ui.kyc.navhost.KycNavHostActivity.Companion.RESULT_KYC_FOR_SDD_COMPLETE
import timber.log.Timber

class CowboysFlowActivity : BlockchainActivity() {

    private var flowStep = FlowStep.Welcome
    private var interstitialData by mutableStateOf<CowboysInterstitialInfo?>(null)
    private val assetCatalogue: AssetCatalogue by scopedInject()
    private val cowboysDataProvider: CowboysDataProvider by scopedInject()
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

        setContentView(
            ComposeView(this).apply {
                setContent {
                    AppTheme {
                        interstitialData?.let { data ->
                            CowboysInterstitial(
                                info = data,
                                onPrimaryCtaClick = { getPrimaryCtaAction(data.actions[0].deeplinkPath) },
                                onSecondaryCtaClick = { navigateToMainActivity() }
                            )
                        }
                    }
                }
            }
        )
    }

    private fun getPrimaryCtaAction(deeplinkPath: String) {
        with(deeplinkPath) {
            when {
                contains(KYC_URL) -> {
                    when (this.split("=")[1].toInt()) {
                        Tier.SILVER.ordinal -> {
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
                    Timber.e("!!! Cowboys - Unknown link $this")
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
        }.observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = {
                    interstitialData = it
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
fun CowboysInterstitial(
    info: CowboysInterstitialInfo,
    onPrimaryCtaClick: () -> Unit,
    onSecondaryCtaClick: () -> Unit
) {
    ConstraintLayout {
        val (
            primaryButton, secondaryButton, icon, backgroundImage,
            divider, foregroundImage, title, subtitle
        ) = createRefs()

        // TODO(dserrano): Change this to AsyncImage when ready
        Image(
            modifier = Modifier.constrainAs(backgroundImage) {
                top.linkTo(parent.top)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            },
            imageResource = ImageResource.Local(R.drawable.ic_temp_cowboys_background),
            contentScale = ContentScale.FillWidth
        )

        Image(
            modifier = Modifier.constrainAs(foregroundImage) {
                top.linkTo(parent.top, margin = 48.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            },
            imageResource = ImageResource.Local(R.drawable.ic_temp_cowboys_header),
            contentScale = ContentScale.Fit
        )

        Divider(
            modifier = Modifier
                .width(24.dp)
                .constrainAs(divider) {
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    top.linkTo(foregroundImage.bottom, margin = 16.dp)
                }
        )

        // icon
        //        Image(
        //            modifier = Modifier.constrainAs(icon) {
        //                top.linkTo(divider.bottom, margin = 16.dp)
        //                start.linkTo(parent.start)
        //                end.linkTo(parent.end)
        //                bottom.linkTo(backgroundImage.bottom)
        //            },
        //            imageResource = ImageResource.Local(R.drawable.ic_temp_cowboys_icon)
        //        )

        SimpleText(
            modifier = Modifier
                .padding(start = 24.dp, end = 24.dp)
                .constrainAs(title) {
                    top.linkTo(backgroundImage.bottom, margin = 16.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                },
            text = info.title,
            style = ComposeTypographies.Title1,
            color = ComposeColors.Title,
            gravity = ComposeGravities.Centre
        )

        SimpleText(
            modifier = Modifier
                .padding(start = 24.dp, end = 24.dp)
                .constrainAs(subtitle) {
                    top.linkTo(title.bottom, margin = 16.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    // bottom.linkTo(primaryButton.top)
                },
            text = info.message,
            style = ComposeTypographies.Body1,
            color = ComposeColors.Body,
            gravity = ComposeGravities.Centre
        )

        if (info.actions.isNotEmpty()) {
            PrimaryButton(
                modifier = Modifier
                    .constrainAs(primaryButton) {
                        if (info.actions.size == 2) {
                            bottom.linkTo(secondaryButton.top, margin = 16.dp)
                        } else {
                            bottom.linkTo(parent.bottom, margin = 24.dp)
                        }

                        start.linkTo(parent.start, margin = 24.dp)
                        end.linkTo(parent.end, margin = 24.dp)
                        width = Dimension.fillToConstraints
                    },
                text = info.actions[0].title,
                onClick = onPrimaryCtaClick
            )

            if (info.actions.size == 2) {
                MinimalButton(
                    modifier = Modifier
                        .constrainAs(secondaryButton) {
                            bottom.linkTo(parent.bottom, margin = 24.dp)
                            start.linkTo(parent.start, margin = 24.dp)
                            end.linkTo(parent.end, margin = 24.dp)
                            width = Dimension.fillToConstraints
                        },
                    text = info.actions[1].title,
                    onClick = onSecondaryCtaClick
                )
            }
        }
    }
}

@Preview
@Composable
fun CowboysInterstitial() {
    CowboysInterstitial(
        info = CowboysInterstitialInfo(
            "Welcome cowboys",
            "some longer text here to see how it looks",
            "", "", "",
            listOf(
                ServerErrorAction(
                    "primary cta", ""
                ),
                ServerErrorAction(
                    "secondary cta", ""
                )
            )
        ),
        onPrimaryCtaClick = { }, onSecondaryCtaClick = {}
    )
}
