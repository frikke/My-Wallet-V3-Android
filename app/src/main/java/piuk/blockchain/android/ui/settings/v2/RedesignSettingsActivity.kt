package piuk.blockchain.android.ui.settings.v2

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.blockchain.componentlib.navigation.NavigationBarButton
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ActivitySettingsBinding
import piuk.blockchain.android.ui.base.BlockchainActivity

class RedesignSettingsActivity : BlockchainActivity() {

    private val binding: ActivitySettingsBinding by lazy {
        ActivitySettingsBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setupToolbar()
    }

    override val alwaysDisableScreenshots: Boolean = true

    private fun setupToolbar() {
        binding.toolbar.apply {
            title = getString(R.string.toolbar_settings)
            onBackButtonClick = { super.onBackPressed() }
            navigationBarButtons = listOf(
                NavigationBarButton.Icon(R.drawable.ic_qr_scan) {
                    Toast.makeText(context, "clicked", Toast.LENGTH_SHORT).show()
                },
            )
        }
    }

    companion object {
        fun newIntent(context: Context): Intent =
            Intent(context, RedesignSettingsActivity::class.java)
    }
}
