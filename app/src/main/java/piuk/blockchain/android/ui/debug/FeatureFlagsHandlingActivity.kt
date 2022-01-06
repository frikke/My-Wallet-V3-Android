package piuk.blockchain.android.ui.debug

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockchain.componentlib.demo.ComponentLibDemoActivity
import com.blockchain.featureflags.InternalFeatureFlagApi
import com.blockchain.koin.scopedInject
import com.blockchain.logging.CrashLogger
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.SimpleBuyPrefs
import info.blockchain.balance.FiatCurrency
import io.reactivex.rxjava3.disposables.CompositeDisposable
import org.koin.android.ext.android.inject
import piuk.blockchain.android.databinding.ActivityLocalFeatureFlagsBinding
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementList
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder
import piuk.blockchain.android.util.AppUtil
import piuk.blockchain.androidcore.data.access.PinRepository
import piuk.blockchain.androidcore.utils.PersistentPrefs

class FeatureFlagsHandlingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLocalFeatureFlagsBinding
    private val internalFlags: InternalFeatureFlagApi by inject()
    private val featureFlagHandler: FeatureFlagHandler by inject()
    private val compositeDisposable = CompositeDisposable()
    private val prefs: PersistentPrefs by inject()
    private val appUtil: AppUtil by inject()
    private val loginState: PinRepository by inject()
    private val crashLogger: CrashLogger by inject()
    private val simpleBuyPrefs: SimpleBuyPrefs by inject()
    private val currencyPrefs: CurrencyPrefs by inject()

    private val featuresAdapter: FeatureFlagAdapter = FeatureFlagAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLocalFeatureFlagsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        featuresAdapter.items = featureFlagHandler.getAllFeatureFlags().entries.map { (featureFlag, status) ->
            FeatureFlagItem(
                name = featureFlag.readableName,
                featureFlagState = status,
                onStatusChanged = { featureStatus ->
                    featureFlagHandler.setFeatureFlagState(featureFlag, featureStatus)
                }
            )
        }

        with(binding) {
            featureFlagList.apply {
                layoutManager = LinearLayoutManager(
                    this@FeatureFlagsHandlingActivity,
                    LinearLayoutManager.VERTICAL,
                    false
                )
                adapter = featuresAdapter
            }
            val parent = nestedParent
            internalFlags.getAll().entries.forEachIndexed { index, flag ->
                val switch = SwitchCompat(this@FeatureFlagsHandlingActivity)
                switch.text = flag.key.readableName
                switch.isChecked = flag.value
                switch.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        internalFlags.enable(flag.key)
                    } else {
                        internalFlags.disable(flag.key)
                    }
                }
                // adding index specifically so flags show before all other items
                parent.addView(switch, index + 1)
            }
            btnRndDeviceId.setOnClickListener { onRndDeviceId() }
            btnResetWallet.setOnClickListener { onResetWallet() }
            btnResetAnnounce.setOnClickListener { onResetAnnounce() }
            btnResetPrefs.setOnClickListener { onResetPrefs() }
            clearSimpleBuyState.setOnClickListener { clearSimpleBuyState() }
            btnStoreLinkId.setOnClickListener { prefs.pitToWalletLinkId = "11111111-2222-3333-4444-55556666677" }
            btnComponentLib.setOnClickListener { onComponentLib() }
            deviceCurrency.text = "Select a new currency. Current one is ${currencyPrefs.selectedFiatCurrency}"
            firebaseToken.text = prefs.firebaseToken

            radioEur.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    currencyPrefs.selectedFiatCurrency = FiatCurrency.fromCurrencyCode("EUR")
                    showToast("Currency changed to EUR")
                }
            }

            radioUsd.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    currencyPrefs.selectedFiatCurrency = FiatCurrency.fromCurrencyCode("USD")
                    showToast("Currency changed to USD")
                }
            }

            radioGbp.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    currencyPrefs.selectedFiatCurrency = FiatCurrency.fromCurrencyCode("GBP")
                    showToast("Currency changed to GBP")
                }
            }
        }
    }

    private fun showToast(text: String) {
        ToastCustom.makeText(this, text, Toast.LENGTH_SHORT, ToastCustom.TYPE_GENERAL)
    }

    private fun clearSimpleBuyState() {
        simpleBuyPrefs.clearBuyState()
        showToast("Local SB State cleared")
    }

    private fun onRndDeviceId() {
        prefs.qaRandomiseDeviceId = true
        showToast("Device ID randomisation enabled")
    }

    private fun onResetWallet() {
        appUtil.clearCredentialsAndRestart()
        showToast("Wallet reset")
    }

    private fun onResetAnnounce() {
        val announcementList: AnnouncementList by scopedInject()
        val dismissRecorder: DismissRecorder by scopedInject()

        dismissRecorder.undismissAll(announcementList)

        showToast("Announcement reset")
    }

    private fun onResetPrefs() {
        prefs.clear()

        crashLogger.logEvent("debug clear prefs. Pin reset")
        loginState.clearPin()

        showToast("Prefs Reset")
    }

    private fun onComponentLib() {
        startActivity(Intent(this, ComponentLibDemoActivity::class.java))
    }

    override fun onPause() {
        compositeDisposable.clear()
        super.onPause()
    }

    companion object {
        fun newIntent(context: Context) = Intent(context, FeatureFlagsHandlingActivity::class.java)
    }
}
