package piuk.blockchain.android.ui.blockchaincard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.blockchain.blockchaincard.domain.models.BlockchainCard
import com.blockchain.blockchaincard.domain.models.BlockchainCardAddress
import com.blockchain.blockchaincard.domain.models.BlockchainCardProduct
import com.blockchain.blockchaincard.ui.BlockchainCardHostFragment
import com.blockchain.blockchaincard.ui.composables.BlockchainCardNavHost
import com.blockchain.blockchaincard.viewmodel.BlockchainCardArgs
import com.blockchain.blockchaincard.viewmodel.BlockchainCardIntent
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.NullCryptoAccount
import com.blockchain.commonarch.presentation.base.updateToolbar
import info.blockchain.balance.AssetInfo
import piuk.blockchain.android.R
import piuk.blockchain.android.simplebuy.SimpleBuyActivity
import piuk.blockchain.android.ui.transactionflow.flow.TransactionFlowActivity

class BlockchainCardFragment : BlockchainCardHostFragment() {

    override fun onResume() {
        super.onResume()
        updateToolbar(
            toolbarTitle = getString(R.string.blockchain_card),
            menuItems = emptyList()
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            val viewModel =
                if (modelArgs is BlockchainCardArgs.CardArgs) manageCardViewModel
                else orderCardViewModel

            setContent {
                viewModel.viewCreated(modelArgs)
                BlockchainCardNavHost(viewModel = viewModel, modelArgs = modelArgs)
            }
        }
    }

    companion object {
        fun newInstance(blockchainCard: BlockchainCard): BlockchainCardHostFragment =
            (BlockchainCardFragment() as BlockchainCardHostFragment).newInstance(blockchainCard)
        fun newInstance(blockchainCardProduct: BlockchainCardProduct): BlockchainCardHostFragment =
            (BlockchainCardFragment() as BlockchainCardHostFragment).newInstance(blockchainCardProduct)
    }

    override fun newInstance(blockchainCard: BlockchainCard) =
        BlockchainCardFragment().apply {
            arguments = Bundle().apply {
                putParcelable(BLOCKCHAIN_CARD, blockchainCard)
            }
        }

    override fun newInstance(blockchainCardProduct: BlockchainCardProduct) =
        BlockchainCardFragment().apply {
            arguments = Bundle().apply {
                putParcelable(BLOCKCHAIN_PRODUCT, blockchainCardProduct)
            }
        }

    override fun startBuy(asset: AssetInfo) =
        startActivity(
            activity?.let {
                SimpleBuyActivity.newIntent(
                    context = it,
                    asset = asset,
                    launchFromNavigationBar = true
                )
            }
        )

    override fun startDeposit(account: FiatAccount) {
        startActivity(
            TransactionFlowActivity.newIntent(
                context = requireActivity(),
                sourceAccount = NullCryptoAccount(),
                target = account,
                action = AssetAction.FiatDeposit
            )
        )
    }

    override fun startKycAddressVerification(address: BlockchainCardAddress) {
        val fragment = BlockchainCardKycAddressVerificationFragment.newInstance(address)
        requireActivity().supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
            .replace(((view as ViewGroup).parent as View).id, fragment, fragment::class.simpleName)
            .addToBackStack(fragment::class.simpleName)
            .commitAllowingStateLoss()
    }

    override fun updateKycAddress(address: BlockchainCardAddress) {
        if (modelArgs is BlockchainCardArgs.CardArgs) {
            manageCardViewModel.onIntent(BlockchainCardIntent.UpdateBillingAddress(address))
        } else {
            orderCardViewModel.onIntent(BlockchainCardIntent.UpdateBillingAddress(address))
        }
    }
}
