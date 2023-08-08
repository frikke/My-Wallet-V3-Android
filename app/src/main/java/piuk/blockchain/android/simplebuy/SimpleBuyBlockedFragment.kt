package piuk.blockchain.android.simplebuy

import android.content.res.Resources
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.fragment.app.Fragment
import com.blockchain.analytics.Analytics
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.extensions.exhaustive
import com.blockchain.nabu.BlockedReason
import com.blockchain.nabu.FeatureAccess
import kotlinx.parcelize.Parcelize
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.SimpleBuyBlockedFragmentBinding
import piuk.blockchain.android.simplebuy.ClientErrorAnalytics.Companion.INELIGIBLE
import piuk.blockchain.android.simplebuy.ClientErrorAnalytics.Companion.PENDING_ORDERS_LIMIT_REACHED

class SimpleBuyBlockedFragment : Fragment() {
    private var _binding: SimpleBuyBlockedFragmentBinding? = null

    private val analytics: Analytics by inject()

    private val binding: SimpleBuyBlockedFragmentBinding
        get() = _binding!!

    private val data: BlockedBuyData by lazy {
        (arguments?.getParcelable(BLOCKED_DATA_KEY) as? BlockedBuyData) ?: throw IllegalStateException("Missing data")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = SimpleBuyBlockedFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            ok.apply {
                text = getString(com.blockchain.stringResources.R.string.common_ok)
                onClick = { activity?.finish() }
            }
            title.text = data.title
            description.text = data.description
            notEligibleIcon.setImageResource(data.icon)
        }

        (activity as BlockchainActivity).updateToolbar(
            toolbarTitle = getString(com.blockchain.stringResources.R.string.empty),
            menuItems = emptyList(),
            backAction = { activity?.finish() }
        )

        logErrorAnalytics(data.title, data.error, data.description)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun logErrorAnalytics(title: String, error: String, description: String) {
        analytics.logEvent(
            ClientErrorAnalytics.ClientLogError(
                nabuApiException = null,
                errorDescription = description,
                error = error,
                source = ClientErrorAnalytics.Companion.Source.CLIENT,
                title = title,
                action = ClientErrorAnalytics.ACTION_BUY,
                categories = emptyList()
            )
        )
    }

    companion object {
        private const val BLOCKED_DATA_KEY = "BLOCKED_DATA_KEY"

        fun newInstance(
            access: FeatureAccess.Blocked,
            resources: Resources
        ): SimpleBuyBlockedFragment {
            val data = when (val reason = access.reason) {
                is BlockedReason.NotEligible -> {
                    BlockedBuyData(
                        title = resources.getString(com.blockchain.stringResources.R.string.sell_is_coming_soon),
                        description = resources.getString(
                            com.blockchain.stringResources.R.string.sell_is_coming_soon_description
                        ),
                        icon = R.drawable.ic_trade_not_eligible,
                        error = INELIGIBLE
                    )
                }

                is BlockedReason.TooManyInFlightTransactions -> {
                    BlockedBuyData(
                        title = resources.getString(com.blockchain.stringResources.R.string.pending_transaction_limit),
                        description = resources.getString(
                            com.blockchain.stringResources.R.string.pending_buys_description,
                            reason.maxTransactions
                        ),
                        icon = R.drawable.ic_trolley_market,
                        error = PENDING_ORDERS_LIMIT_REACHED
                    )
                }

                is BlockedReason.ShouldAcknowledgeStakingWithdrawal,
                is BlockedReason.ShouldAcknowledgeActiveRewardsWithdrawalWarning,
                is BlockedReason.InsufficientTier,
                is BlockedReason.Sanctions -> throw IllegalStateException("Not used in Buy")
            }.exhaustive

            return SimpleBuyBlockedFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(BLOCKED_DATA_KEY, data)
                }
            }
        }
    }
}

@Parcelize
private data class BlockedBuyData(
    val title: String,
    val description: String,
    @DrawableRes val icon: Int,
    val error: String
) : Parcelable
