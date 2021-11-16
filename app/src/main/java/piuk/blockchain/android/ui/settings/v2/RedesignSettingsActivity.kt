package piuk.blockchain.android.ui.settings.v2

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.widget.Toolbar
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
        val toolbar = findViewById<Toolbar>(R.id.toolbar_general)
        setupToolbar(toolbar, R.string.action_settings)
        toolbar.setNavigationOnClickListener { onBackPressed() }
    }

    companion object {
        fun newInstance(context: Context): Intent =
            Intent(context, RedesignSettingsActivity::class.java)
    }
}
