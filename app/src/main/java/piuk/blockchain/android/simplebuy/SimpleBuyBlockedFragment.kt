package piuk.blockchain.android.simplebuy

import android.content.res.Resources
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.fragment.app.Fragment
import com.blockchain.nabu.BlockedReason
import com.blockchain.nabu.FeatureAccess
import kotlinx.parcelize.Parcelize
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.SimpleBuyBlockedFragmentBinding

class SimpleBuyBlockedFragment : Fragment() {
    private var _binding: SimpleBuyBlockedFragmentBinding? = null

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
            ok.setOnClickListener {
                activity?.finish()
            }
            title.text = data.title
            description.text = data.description
            notEligibleIcon.setImageResource(data.icon)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val BLOCKED_DATA_KEY = "BLOCKED_DATA_KEY"
        fun newInstance(access: FeatureAccess.Blocked, resources: Resources): SimpleBuyBlockedFragment {
            val data = when (val reason = access.reason) {
                BlockedReason.NotEligible -> BlockedBuyData(
                    title = resources.getString(R.string.sell_is_coming_soon),
                    description = resources.getString(R.string.sell_is_coming_soon_description),
                    icon = R.drawable.ic_trade_not_eligible
                )
                is BlockedReason.TooManyInFlightTransactions -> BlockedBuyData(
                    title = resources.getString(R.string.pending_transaction_limit),
                    description = resources.getString(R.string.pending_buys_description, reason.maxTransactions),
                    icon = R.drawable.ic_trolley_market
                )
            }

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
    @DrawableRes val icon: Int
) : Parcelable
