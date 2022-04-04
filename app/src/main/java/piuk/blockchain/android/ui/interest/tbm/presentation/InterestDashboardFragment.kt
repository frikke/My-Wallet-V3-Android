package piuk.blockchain.android.ui.interest.tbm.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.commonarch.presentation.mvi_v2.MVIFragment
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.commonarch.presentation.mvi_v2.NavigationRouter
import com.blockchain.commonarch.presentation.mvi_v2.bindViewModel
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.divider.HorizontalDivider
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Grey600
import com.blockchain.componentlib.theme.Grey800
import com.blockchain.componentlib.viewextensions.visibleIf
import com.blockchain.nabu.datamanagers.repositories.interest.IneligibilityReason
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.Money
import org.koin.androidx.viewmodel.ext.android.viewModel
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.FragmentInterestDashboardBinding
import piuk.blockchain.android.ui.interest.tbm.domain.model.AssetInterestDetail
import piuk.blockchain.android.ui.interest.tbm.domain.model.AssetInterestInfo
import piuk.blockchain.android.ui.interest.tbm.presentation.adapter.InterestDashboardAdapter

class InterestDashboardFragment : MVIFragment<InterestDashboardViewState>(), NavigationRouter<NavigationEvent> {

    val host: InterestDashboardHost by lazy {
        activity as? InterestDashboardHost
            ?: error("Host fragment is not a InterestDashboardFragment.InterestDashboardHost")
    }

    private lateinit var composeView: ComposeView

    private var _binding: FragmentInterestDashboardBinding? = null
    private val binding: FragmentInterestDashboardBinding
        get() = _binding!!

    private val viewModel: InterestDashboardViewModel by viewModel()

    private val listAdapter: InterestDashboardAdapter by lazy {
        InterestDashboardAdapter(
            verificationClicked = ::startKyc,
            itemClicked = ::interestItemClicked
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInterestDashboardBinding.inflate(inflater, container, false)

        return ComposeView(requireContext()).also { composeView = it }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.interestDashboardList.adapter = listAdapter

        setupViews()
        bindViewModel(viewModel = viewModel, navigator = this, args = ModelConfigArgs.NoArgs)
    }

    private fun setupViews() {
        composeView.apply {
            setContent {
                SheetContent()
            }
        }
    }

    @Composable
    private fun SheetContent() {
        val state = viewModel.viewState.collectAsState()

        LazyColumn {
            items(
                items = state.value.data,
                itemContent = {
                    AssetInterestItem(item = it, isKycGold = state.value.isKycGold)
                }
            )
        }
    }

    @Composable
    private fun AssetInterestItem(item: AssetInterestInfo, isKycGold: Boolean) {
        Column(modifier = Modifier.padding(dimensionResource(R.dimen.standard_margin))) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    modifier = Modifier
                        .size(dimensionResource(R.dimen.standard_margin))
                        .clip(CircleShape),
                    imageResource = ImageResource.Remote(item.getAssetLogo())
                )

                Text(
                    modifier = Modifier.padding(start = dimensionResource(R.dimen.tiny_margin)),
                    style = AppTheme.typography.title3,
                    text = item.getAssetName()
                )
            }

            if (item.assetInterestDetail != null) {
                Spacer(Modifier.size(dimensionResource(R.dimen.very_small_margin)))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        imageResource = ImageResource.Local(id = R.drawable.ic_information)
                    )

                    Text(
                        modifier = Modifier.padding(start = dimensionResource(R.dimen.tiny_margin)),
                        style = AppTheme.typography.paragraph1,
                        color = Grey600,
                        text = buildAnnotatedString {
                            append(stringResource(id = R.string.rewards_dashboard_item_rate_1))

                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                //                            append(item.getInterestRate())
                            }

                            append(stringResource(id = R.string.rewards_dashboard_item_rate_2, item.getAssetName()))
                        },
                    )
                }

                HorizontalDivider(
                    modifier = Modifier
                        .padding(
                            top = dimensionResource(R.dimen.very_small_margin),
                            bottom = dimensionResource(R.dimen.very_small_margin)
                        )
                        .fillMaxWidth()
                )

                Row {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            style = AppTheme.typography.caption1,
                            text = "BTC Balance",
                            color = Grey800
                        )

                        Text(
                            style = AppTheme.typography.paragraph2,
                            text = "12324325245235252 BTC",
                            color = Grey800
                        )
                    }

                    Spacer(Modifier.size(dimensionResource(R.dimen.very_small_margin)))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            style = AppTheme.typography.caption1,
                            text = "Total Rewards Earned",
                            color = Grey800
                        )

                        Text(
                            style = AppTheme.typography.paragraph2,
                            text = "0.00 BTC",
                            color = Grey800
                        )
                    }
                }
            }

            Spacer(Modifier.size(dimensionResource(R.dimen.very_small_margin)))

            PrimaryButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(
                    when {
                        item.hasDetail().not() -> R.string.rewards_dashboard_item_action_earn
                        item.getBalance()!!.isPositive -> R.string.rewards_dashboard_item_action_view
                        else -> R.string.rewards_dashboard_item_action_earn
                    }
                ),
                state = if (item.hasDetail()) ButtonState.Enabled else ButtonState.Disabled,
                onClick = { },
            )

            if (item.hasDetail().not() || item.getInligibilityReason() != IneligibilityReason.NONE) {
                Spacer(Modifier.size(dimensionResource(R.dimen.tiny_margin)))
                AssetInterestItemExplainer(
                    stringResource(
                        when (item.getInligibilityReason()) {
                            IneligibilityReason.REGION -> R.string.rewards_item_issue_region
                            IneligibilityReason.KYC_TIER -> R.string.rewards_item_issue_kyc
                            else -> R.string.rewards_item_issue_other
                        }
                    )
                )
            }
        }
    }

    @Composable
    fun AssetInterestItemExplainer(explanation: String) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                imageResource = ImageResource.Local(id = R.drawable.ic_information)
            )

            Text(
                modifier = Modifier.padding(start = dimensionResource(R.dimen.minuscule_margin)),
                style = AppTheme.typography.paragraph1,
                text = explanation,
                color = Grey800
            )
        }
    }

    override fun onStateUpdated(state: InterestDashboardViewState) {
        with(binding) {
            interestDashboardProgress.visibleIf { state.isLoading }

            interestDashboardList.visibleIf { state.isLoading.not() && state.isError.not() }

            interestError.setDetails(
                title = R.string.rewards_error_title,
                description = R.string.rewards_error_desc,
                contactSupportEnabled = true
            ) {
                //                loadInterestDetails()
            }
            interestError.visibleIf { state.isError }

            println("------ data: ${state.data}")

            listAdapter.items = state.data
            listAdapter.notifyDataSetChanged()
        }
    }

    override fun route(navigationEvent: NavigationEvent) {
    }

    private fun interestItemClicked(cryptoCurrency: AssetInfo, hasBalance: Boolean) {
        println("------ cryptoCurrency: $cryptoCurrency")
        //        compositeDisposable += coincore[cryptoCurrency].accountGroup(AssetFilter.Interest).subscribe {
        //            val interestAccount = it.accounts.first() as CryptoInterestAccount
        //            if (hasBalance) {
        //                host.showInterestSummarySheet(interestAccount)
        //            } else {
        //                startActivity(
        //                    TransactionFlowActivity.newIntent(
        //                        context = requireContext(),
        //                        target = it.accounts.first(),
        //                        action = AssetAction.InterestDeposit
        //                    )
        //                )
        //            }
        //        }
    }

    private fun startKyc() {
        host.startKyc()
    }

    companion object {
        fun newInstance(): InterestDashboardFragment = InterestDashboardFragment()
    }

    @Preview
    @Composable
    private fun PreviewAssetInterestItemError() {
        AssetInterestItem(
            AssetInterestInfo(
                CryptoCurrency.BTC,
                null
            ),
            true
        )
    }

    @Preview
    @Composable
    private fun PreviewAssetInterestItem() {
        AssetInterestItem(
            AssetInterestInfo(
                CryptoCurrency.BTC,
                AssetInterestDetail(
                    totalInterest = Money.fromMajor(CryptoCurrency.BTC, 1.toBigDecimal()),
                    totalBalance = Money.fromMajor(CryptoCurrency.BTC, 123.toBigDecimal()),
                    rate = 12.34,
                    eligible = true,
                    ineligibilityReason = IneligibilityReason.KYC_TIER,
                    totalBalanceFiat = Money.fromMajor(CryptoCurrency.BTC, 3.toBigDecimal())
                )
            ),
            true
        )
    }
}
