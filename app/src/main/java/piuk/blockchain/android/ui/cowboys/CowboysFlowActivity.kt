package piuk.blockchain.android.ui.cowboys

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.dimensionResource
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.button.SecondaryButton
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.domain.paymentmethods.model.PaymentMethod
import com.blockchain.koin.scopedInject
import com.blockchain.preferences.CowboysPrefs
import info.blockchain.balance.AssetCatalogue
import piuk.blockchain.android.R
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.simplebuy.SimpleBuyActivity
import piuk.blockchain.android.ui.home.MainActivity
import piuk.blockchain.android.ui.kyc.navhost.KycNavHostActivity

class CowboysFlowActivity : BlockchainActivity() {

    private var flowStep by mutableStateOf(FlowStep.Intro)
    private val assetCatalogue: AssetCatalogue by scopedInject()
    private val cowboysPrefs: CowboysPrefs by scopedInject()

    private val simpleBuyActivityResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            flowStep = FlowStep.PostBuy
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

        setContentView(
            ComposeView(this).apply {
                setContent {
                    AppTheme {
                        Column {
                            when (flowStep) {
                                FlowStep.Intro -> {
                                    IntroductionInfo(
                                        onPrimaryCtaClick = {
                                            cowboysPrefs.hasSeenCowboysFlow = true

                                            KycNavHostActivity.startForResult(
                                                this@CowboysFlowActivity, CampaignType.SimpleBuy, SDD_REQUEST
                                            )
                                        }
                                    )
                                }
                                FlowStep.Raffle -> {
                                    RaffleInfo(
                                        onPrimaryCtaClick = {
                                            assetCatalogue.assetInfoFromNetworkTicker(BTC_TICKER)?.let { assetInfo ->
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
                                        },
                                        onSecondaryCtaClick = {
                                            navigateToMainActivity()
                                        }
                                    )
                                }
                                FlowStep.PostBuy -> {
                                    PostBuyInfo(
                                        onPrimaryCtaClick = {
                                            KycNavHostActivity.startForResult(
                                                this@CowboysFlowActivity, CampaignType.SimpleBuy,
                                                GOLD_VERIFICATION_REQUEST
                                            )

                                            finish()
                                        },
                                        onSecondaryCtaClick = {
                                            navigateToMainActivity()
                                        }
                                    )
                                }
                            }
                        }
                    }
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
                flowStep = FlowStep.Raffle
            }
            GOLD_VERIFICATION_REQUEST -> {
                navigateToMainActivity()
            }
        }
    }

    companion object {
        private const val SDD_REQUEST = 567
        private const val GOLD_VERIFICATION_REQUEST = 890
        private const val BTC_TICKER = "BTC"
        private const val FLOW_STEP = "FLOW_STEP"
        fun newIntent(context: Context, flowStep: FlowStep = FlowStep.Intro) =
            Intent(context, CowboysFlowActivity::class.java).apply {
                putExtra(FLOW_STEP, flowStep)
            }
    }
}

@Composable
fun IntroductionInfo(onPrimaryCtaClick: () -> Unit) {
    Column(
        modifier = Modifier.padding(all = dimensionResource(R.dimen.standard_margin))
    ) {
        SimpleText(
            text = "Welcome Cowboys fan to BC!", style = ComposeTypographies.Title2,
            color = ComposeColors.Title,
            gravity = ComposeGravities.Centre
        )

        PrimaryButton(text = "Continue", onClick = onPrimaryCtaClick)
    }
}

@Composable
fun RaffleInfo(onPrimaryCtaClick: () -> Unit, onSecondaryCtaClick: () -> Unit) {
    Column(
        modifier = Modifier.padding(all = dimensionResource(R.dimen.standard_margin))
    ) {
        SimpleText(
            text = "Registration complete! You have entered the raffle", style = ComposeTypographies.Title2,
            color = ComposeColors.Title,
            gravity = ComposeGravities.Centre
        )

        PrimaryButton(text = "Continue", onClick = onPrimaryCtaClick)

        SecondaryButton(text = "Maybe later", onClick = onSecondaryCtaClick)
    }
}

@Composable
fun PostBuyInfo(onPrimaryCtaClick: () -> Unit, onSecondaryCtaClick: () -> Unit) {
    Column(
        modifier = Modifier.padding(all = dimensionResource(R.dimen.standard_margin))
    ) {
        SimpleText(
            text = "Buy Completed! Go Cowboys", style = ComposeTypographies.Title2,
            color = ComposeColors.Title,
            gravity = ComposeGravities.Centre
        )

        PrimaryButton(text = "Verify ID", onClick = onPrimaryCtaClick)

        SecondaryButton(text = "Maybe later", onClick = onSecondaryCtaClick)
    }
}

enum class FlowStep {
    Intro,
    Raffle,
    PostBuy
}
