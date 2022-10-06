package piuk.blockchain.android.ui.blockchaincard

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.blockchain.blockchaincard.domain.models.BlockchainCard
import com.blockchain.blockchaincard.domain.models.BlockchainCardAddress
import com.blockchain.blockchaincard.domain.models.BlockchainCardGoogleWalletPushTokenizeData
import com.blockchain.blockchaincard.domain.models.BlockchainCardProduct
import com.blockchain.blockchaincard.googlewallet.manager.GoogleWalletManager
import com.blockchain.blockchaincard.ui.BlockchainCardHostActivity
import com.blockchain.blockchaincard.ui.BlockchainCardHostFragment
import com.blockchain.blockchaincard.viewmodel.BlockchainCardArgs
import com.blockchain.blockchaincard.viewmodel.BlockchainCardIntent
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.NullCryptoAccount
import com.blockchain.commonarch.presentation.base.addAnimationTransaction
import com.blockchain.componentlib.databinding.ToolbarGeneralBinding
import com.blockchain.koin.scopedInject
import info.blockchain.balance.AssetInfo
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ActivityBlockchainCardBinding
import piuk.blockchain.android.simplebuy.SimpleBuyActivity
import piuk.blockchain.android.ui.transactionflow.flow.TransactionFlowActivity
import timber.log.Timber

const val GOOGLE_PAY_TOKENIZE_REQUEST_CODE = 101

class BlockchainCardActivity : BlockchainCardHostActivity() {

    override val alwaysDisableScreenshots: Boolean // TODO (labreu): disable screenshots if card number is visible
        get() = false

    override val toolbarBinding: ToolbarGeneralBinding
        get() = binding.toolbar

    private val binding: ActivityBlockchainCardBinding by lazy {
        ActivityBlockchainCardBinding.inflate(layoutInflater)
    }

    private val googleWalletManager: GoogleWalletManager by scopedInject()

    private val blockchainCardFragment by lazy {
        (intent?.getParcelableExtra(BlockchainCardHostFragment.BLOCKCHAIN_CARD) as? BlockchainCard)?.let { card ->
            BlockchainCardFragment.newInstance(card)
        } ?: (
            intent?.getParcelableExtra(
                BlockchainCardHostFragment.BLOCKCHAIN_PRODUCT
            ) as? BlockchainCardProduct
            )?.let { product ->
            BlockchainCardFragment.newInstance(product)
        } ?: throw IllegalStateException("Missing card or product data")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        updateToolbar(
            toolbarTitle = getString(R.string.blockchain_card),
            menuItems = emptyList(),
            backAction = { onBackPressedDispatcher.onBackPressed() }
        )

        supportFragmentManager.beginTransaction()
            .addAnimationTransaction()
            .replace(binding.blockchainCardContentFrame.id, blockchainCardFragment)
            .commitNowAllowingStateLoss()
    }

    companion object {
        fun newIntent(context: Context, blockchainCard: BlockchainCard): Intent =
            (BlockchainCardActivity() as BlockchainCardHostActivity).newIntent(context, blockchainCard)
        fun newIntent(context: Context, blockchainCardProduct: BlockchainCardProduct): Intent =
            (BlockchainCardActivity() as BlockchainCardHostActivity).newIntent(context, blockchainCardProduct)
    }

    override fun newIntent(context: Context, blockchainCard: BlockchainCard) =
        Intent(context, BlockchainCardActivity::class.java).apply {
            putExtra(BLOCKCHAIN_CARD, blockchainCard)
        }

    override fun newIntent(context: Context, blockchainCardProduct: BlockchainCardProduct) =
        Intent(context, BlockchainCardActivity::class.java).apply {
            putExtra(BLOCKCHAIN_PRODUCT, blockchainCardProduct)
        }

    override fun startBuy(asset: AssetInfo) =
        startActivity(
            SimpleBuyActivity.newIntent(
                context = this,
                asset = asset,
                launchFromNavigationBar = true
            )
        )

    override fun startDeposit(account: FiatAccount) {
        startActivity(
            TransactionFlowActivity.newIntent(
                context = this,
                sourceAccount = NullCryptoAccount(),
                target = account,
                action = AssetAction.FiatDeposit
            )
        )
    }

    override fun startKycAddressVerification(address: BlockchainCardAddress) {
        val fragment = BlockchainCardKycAddressVerificationFragment.newInstance(address)
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
            .replace(R.id.blockchain_card_content_frame, fragment, fragment::class.simpleName)
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

    override fun startAddCardToGoogleWallet(pushTokenizeData: BlockchainCardGoogleWalletPushTokenizeData) {
        googleWalletManager.pushTokenizeRequest(this, pushTokenizeData, GOOGLE_PAY_TOKENIZE_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GOOGLE_PAY_TOKENIZE_REQUEST_CODE) {
            when (resultCode) {
                RESULT_OK -> {
                    Timber.d("Google Wallet: Add Card Success!")
                    manageCardViewModel.onIntent(BlockchainCardIntent.GoogleWalletAddCardSuccess)
                }
                RESULT_CANCELED -> {
                    Timber.e("Google Wallet: Add Card Failed!")
                    manageCardViewModel.onIntent(BlockchainCardIntent.GoogleWalletAddCardFailed)
                }
            }
        }
    }
}
