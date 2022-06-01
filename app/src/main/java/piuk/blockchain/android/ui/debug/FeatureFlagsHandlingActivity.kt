package piuk.blockchain.android.ui.debug

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockchain.componentlib.alert.BlockchainSnackbar
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.componentlib.demo.ComponentLibDemoActivity
import com.blockchain.componentlib.viewextensions.visibleIf
import com.blockchain.koin.scopedInject
import com.blockchain.logging.RemoteLogger
import com.blockchain.preferences.AppRatingPrefs
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.RemoteConfigPrefs
import com.blockchain.preferences.SimpleBuyPrefs
import com.google.android.material.snackbar.Snackbar
import info.blockchain.balance.FiatCurrency
import io.reactivex.rxjava3.disposables.CompositeDisposable
import org.koin.android.ext.android.inject
import piuk.blockchain.android.databinding.ActivityLocalFeatureFlagsBinding
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementList
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder
import piuk.blockchain.android.util.AppUtil
import piuk.blockchain.androidcore.data.access.PinRepository
import piuk.blockchain.androidcore.utils.PersistentPrefs

class FeatureFlagsHandlingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLocalFeatureFlagsBinding
    private val featureFlagHandler: FeatureFlagHandler by inject()
    private val compositeDisposable = CompositeDisposable()
    private val prefs: PersistentPrefs by inject()
    private val appUtils: AppUtil by inject()
    private val loginState: PinRepository by inject()
    private val remoteLogger: RemoteLogger by inject()
    private val simpleBuyPrefs: SimpleBuyPrefs by inject()
    private val currencyPrefs: CurrencyPrefs by inject()
    private val appRatingPrefs: AppRatingPrefs by inject()
    private val remoteConfigPrefs: RemoteConfigPrefs by inject()

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
            resetAppRating.setOnClickListener { resetAppRating() }
            btnRndDeviceId.setOnClickListener { onRndDeviceId() }
            btnResetWallet.setOnClickListener { onResetWallet() }
            btnResetAnnounce.setOnClickListener { onResetAnnounce() }
            btnResetPrefs.setOnClickListener { onResetPrefs() }
            clearSimpleBuyState.setOnClickListener { clearSimpleBuyState() }
            btnComponentLib.setOnClickListener { onComponentLib() }
            deviceCurrency.text = "Select a new currency. Current one is ${currencyPrefs.selectedFiatCurrency}"
            firebaseToken.text = prefs.firebaseToken

            radioEur.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    currencyPrefs.selectedFiatCurrency = FiatCurrency.fromCurrencyCode("EUR")
                    showSnackbar("Currency changed to EUR")
                }
            }

            radioUsd.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    currencyPrefs.selectedFiatCurrency = FiatCurrency.fromCurrencyCode("USD")
                    showSnackbar("Currency changed to USD")
                }
            }

            radioGbp.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    currencyPrefs.selectedFiatCurrency = FiatCurrency.fromCurrencyCode("GBP")
                    showSnackbar("Currency changed to GBP")
                }
            }

            brokerageErrorSwitch.setOnCheckedChangeListener { _, isChecked ->
                brokerageErrorInput.visibleIf { isChecked }
                brokerageErrorCta.visibleIf { isChecked }
                brokerageLink.visibleIf { isChecked }
                brokerageErrorInput.setText(remoteConfigPrefs.brokerageErrorsCode, TextView.BufferType.EDITABLE)

                remoteConfigPrefs.updateBrokerageErrorStatus(isChecked)
                if (!isChecked) {
                    remoteConfigPrefs.updateBrokerageErrorCode("")
                    BlockchainSnackbar.make(this@with.root, "Error message reset", type = SnackbarType.Success).show()
                }
            }

            brokerageErrorCta.setOnClickListener {
                remoteConfigPrefs.updateBrokerageErrorCode(brokerageErrorInput.text?.toString().orEmpty())
                BlockchainSnackbar.make(this@with.root, "Updated error message", type = SnackbarType.Success).show()
            }

            brokerageErrorSwitch.isChecked = remoteConfigPrefs.brokerageErrorsEnabled
        }
    }

    private fun showSnackbar(text: String) {
        BlockchainSnackbar.make(
            binding.root,
            text,
            duration = Snackbar.LENGTH_SHORT,
        ).show()
    }

    private fun clearSimpleBuyState() {
        simpleBuyPrefs.clearBuyState()
        showSnackbar("Local SB State cleared")
    }

    private fun resetAppRating() {
        appRatingPrefs.resetAppRatingData()
    }

    private fun onRndDeviceId() {
        prefs.qaRandomiseDeviceId = true
        showSnackbar("Device ID randomisation enabled")
    }

    private fun onResetWallet() {
        appUtils.clearCredentialsAndRestart()
        showSnackbar("Wallet reset")
    }

    private fun onResetAnnounce() {
        val announcementList: AnnouncementList by scopedInject()
        val dismissRecorder: DismissRecorder by scopedInject()

        dismissRecorder.undismissAll(announcementList)

        showSnackbar("Announcement reset")
    }

    private fun onResetPrefs() {
        prefs.clear()

        remoteLogger.logEvent("debug clear prefs. Pin reset")
        loginState.clearPin()

        showSnackbar("Prefs Reset")
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
