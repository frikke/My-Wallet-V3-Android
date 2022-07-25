package piuk.blockchain.android.ui.cowboys

import android.content.Context
import android.content.Intent
import android.os.Bundle
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
import com.blockchain.componentlib.navigation.NavigationBar
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.deeplinking.navigation.Destination
import piuk.blockchain.android.R
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.ui.home.MainActivity
import piuk.blockchain.android.ui.kyc.navhost.KycNavHostActivity

class CowboysFlowActivity : BlockchainActivity() {

    private var showInitialFlow by mutableStateOf(true)

    override val alwaysDisableScreenshots: Boolean
        get() = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(
            ComposeView(this).apply {
                setContent {
                    AppTheme {
                        Column {
                            NavigationBar(title = "Cowboys")

                            if (showInitialFlow) {
                                IntroductionInfo(
                                    onPrimaryCtaClick = {
                                        KycNavHostActivity.startForResult(
                                            this@CowboysFlowActivity, CampaignType.SimpleBuy, SDD_REQUEST
                                        )
                                    }
                                )
                            } else {
                                ContinuingInfo(
                                    onPrimaryCtaClick = {
                                        startActivity(
                                            MainActivity.newIntent(
                                                this@CowboysFlowActivity,
                                                Destination.AssetEnterAmountDestination(BTC_TICKER)
                                            )
                                        )
                                        finish()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SDD_REQUEST) {
            showInitialFlow = false
        }
    }

    companion object {
        private const val SDD_REQUEST = 567
        private const val BTC_TICKER = "BTC"
        fun newIntent(context: Context) =
            Intent(context, CowboysFlowActivity::class.java)
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
fun ContinuingInfo(onPrimaryCtaClick: () -> Unit) {
    Column(
        modifier = Modifier.padding(all = dimensionResource(R.dimen.standard_margin))
    ) {
        SimpleText(
            text = "Registration complete! You have entered the raffle", style = ComposeTypographies.Title2,
            color = ComposeColors.Title,
            gravity = ComposeGravities.Centre
        )

        PrimaryButton(text = "Continue", onClick = onPrimaryCtaClick)
    }
}
