package piuk.blockchain.android.ui.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import piuk.blockchain.android.databinding.ActivityPinEntryBinding
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy

class PinEntryActivity : BlockchainActivity() {
    private val binding: ActivityPinEntryBinding by lazy {
        ActivityPinEntryBinding.inflate(layoutInflater)
    }

    private val pinEntryFragment: PinEntryFragment by lazy {
        PinEntryFragment.newInstance(isAfterCreateWallet)
    }

    private val isAfterCreateWallet: Boolean by unsafeLazy {
        intent.getBooleanExtra(EXTRA_IS_AFTER_WALLET_CREATION, false)
    }

    private val originSettings: Boolean by unsafeLazy {
        intent.getBooleanExtra(KEY_ORIGIN_SETTINGS, false)
    }

    override val alwaysDisableScreenshots: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        supportFragmentManager.beginTransaction()
            .add(binding.pinContainer.id, pinEntryFragment)
            .commitAllowingStateLoss()
    }

    override fun onBackPressed() {
        when {
            pinEntryFragment.isValidatingPinForResult -> { finishWithResultCanceled() }
            originSettings -> { super.onBackPressed() }
            pinEntryFragment.allowExit() -> { appUtil.logout() }
        }
    }

    private fun finishWithResultCanceled() {
        val intent = Intent()
        setResult(RESULT_CANCELED, intent)
        finish()
    }

    companion object {

        const val REQUEST_CODE_UPDATE = 188
        private const val EXTRA_IS_AFTER_WALLET_CREATION = "piuk.blockchain.android.EXTRA_IS_AFTER_WALLET_CREATION"
        private const val IS_CHANGING_PIN = "is_changing_pin"
        const val KEY_ORIGIN_SETTINGS = "pin_from_settings"

        fun start(context: Context, originSettings: Boolean = false) {
            val intent = Intent(context, PinEntryActivity::class.java)
            intent.putExtra(KEY_ORIGIN_SETTINGS, originSettings)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }

        fun startAfterWalletCreation(context: Context) {
            val intent = Intent(context, PinEntryActivity::class.java)
            intent.putExtra(EXTRA_IS_AFTER_WALLET_CREATION, true)
            context.startActivity(intent)
        }

        fun startPinChange(context: Context) {
            val intent = Intent(context, PinEntryActivity::class.java)
            intent.putExtra(KEY_ORIGIN_SETTINGS, true)
            context.startActivity(intent)
        }
    }
}
