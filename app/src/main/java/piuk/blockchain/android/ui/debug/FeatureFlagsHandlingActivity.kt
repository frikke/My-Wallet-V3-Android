package piuk.blockchain.android.ui.debug

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.componentlib.alert.BlockchainSnackbar
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.componentlib.demo.ComponentLibDemoActivity
import com.blockchain.componentlib.viewextensions.getTextString
import com.blockchain.componentlib.viewextensions.visibleIf
import com.blockchain.core.access.PinRepository
import com.blockchain.core.kyc.data.datasources.KycTiersStore
import com.blockchain.logging.RemoteLogger
import com.blockchain.nabu.api.getuser.data.GetUserStore
import com.blockchain.preferences.AppMaintenancePrefs
import com.blockchain.preferences.AppRatingPrefs
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.HandholdPrefs
import com.blockchain.preferences.NotificationPrefs
import com.blockchain.preferences.RemoteConfigPrefs
import com.blockchain.preferences.RuntimePermissionsPrefs
import com.blockchain.preferences.SessionPrefs
import com.blockchain.preferences.SimpleBuyPrefs
import com.blockchain.presentation.koin.scopedInject
import com.blockchain.storedatasource.StoreWiper
import com.google.android.material.snackbar.Snackbar
import io.reactivex.rxjava3.disposables.CompositeDisposable
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.koin.android.ext.android.inject
import piuk.blockchain.android.databinding.ActivityLocalFeatureFlagsBinding
import piuk.blockchain.android.ui.referral.presentation.ReferralInviteNowSheet
import piuk.blockchain.android.util.AppUtil

// todo (gabor): revert this back to AppCompatActivity once trigger mechanism in place
class FeatureFlagsHandlingActivity : BlockchainActivity() {

    private lateinit var binding: ActivityLocalFeatureFlagsBinding
    private val featureFlagHandler: FeatureFlagHandler by inject()
    private val compositeDisposable = CompositeDisposable()
    private val notificationPrefs: NotificationPrefs by inject()
    private val sessionPrefs: SessionPrefs by inject()
    private val appUtils: AppUtil by inject()
    private val loginState: PinRepository by inject()
    private val remoteLogger: RemoteLogger by inject()
    private val simpleBuyPrefs: SimpleBuyPrefs by inject()
    private val currencyPrefs: CurrencyPrefs by inject()
    private val handholdPrefs: HandholdPrefs by inject()
    private val appMaintenancePrefs: AppMaintenancePrefs by inject()
    private val appRatingPrefs: AppRatingPrefs by inject()
    private val remoteConfigPrefs: RemoteConfigPrefs by inject()
    private val getUserStore: GetUserStore by scopedInject()
    private val kycTiersStore: KycTiersStore by scopedInject()
    private val storeWiper: StoreWiper by scopedInject()
    private val runtimePermissionsPrefs: RuntimePermissionsPrefs by inject()

    private val featuresAdapter: FeatureFlagAdapter = FeatureFlagAdapter()

    override val alwaysDisableScreenshots = false

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
        }.sortedByDescending { it.name }

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

            resetRuntimePermissionCooldown.setOnClickListener { resetRuntimePermissionCooldown() }
            btnResetUserCache.setOnClickListener { onResetUserCache() }
            btnResetStoreCaches.setOnClickListener { onResetStoreCaches() }
            btnShowReferralSheet.setOnClickListener { showInviteNow() }
            resetAppRating.setOnClickListener { resetAppRating() }
            btnRndDeviceId.setOnClickListener { onRndDeviceId() }
            btnResetWallet.setOnClickListener { onResetWallet() }
            btnResetPrefs.setOnClickListener { onResetPrefs() }
            btnComponentLib.setOnClickListener { onComponentLib() }
            deviceCurrency.text = "Select a new currency. Current one is ${currencyPrefs.selectedFiatCurrency}"
            firebaseToken.text = notificationPrefs.firebaseToken

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
                remoteConfigPrefs.updateBrokerageErrorCode(brokerageErrorInput.text?.toString()?.trim().orEmpty())
                BlockchainSnackbar.make(this@with.root, "Updated error message", type = SnackbarType.Success).show()
            }

            brokerageErrorSwitch.isChecked = remoteConfigPrefs.brokerageErrorsEnabled

            // handhold
            handholdOverride.setOnCheckedChangeListener { buttonView, isChecked ->
                handholdPrefs.overrideHandholdVerification = isChecked
            }
            handholdOverride.isChecked = handholdPrefs.overrideHandholdVerification

            handholdEmail.setOnCheckedChangeListener { buttonView, isChecked ->
                handholdPrefs.debugHandholdEmailVerified = isChecked
            }
            handholdEmail.isChecked = handholdPrefs.debugHandholdEmailVerified

            handholdKyc.setOnCheckedChangeListener { buttonView, isChecked ->
                handholdPrefs.debugHandholdKycVerified = isChecked
            }
            handholdKyc.isChecked = handholdPrefs.debugHandholdKycVerified

            handholdBuy.setOnCheckedChangeListener { buttonView, isChecked ->
                handholdPrefs.debugHandholdBuyVerified = isChecked
            }
            handholdBuy.isChecked = handholdPrefs.debugHandholdBuyVerified

            // app maintenance
            ignoreAppMaintenanceRcSwitch.setOnCheckedChangeListener { _, isChecked ->
                appMaintenancePrefs.isAppMaintenanceRemoteConfigIgnored = isChecked
            }
            ignoreAppMaintenanceRcSwitch.isChecked = appMaintenancePrefs.isAppMaintenanceRemoteConfigIgnored

            appMaintenanceSwitch.setOnCheckedChangeListener { _, isChecked ->
                appMaintenanceJson.visibleIf { isChecked }
                btnSaveAppMaintenanceJson.visibleIf { isChecked }

                if (isChecked.not()) {
                    appMaintenancePrefs.isAppMaintenanceDebugOverrideEnabled = false
                }
            }
            appMaintenanceSwitch.isChecked = appMaintenancePrefs.isAppMaintenanceDebugOverrideEnabled

            appMaintenanceJson.setText(appMaintenancePrefs.appMaintenanceDebugJson)

            btnSaveAppMaintenanceJson.setOnClickListener {
                appMaintenanceJson.getTextString().let { json ->
                    try {
                        Json.parseToJsonElement(json)
                        appMaintenancePrefs.isAppMaintenanceDebugOverrideEnabled = true
                        appMaintenancePrefs.appMaintenanceDebugJson = json
                        BlockchainSnackbar.make(this@with.root, "Json saved", type = SnackbarType.Success).show()
                    } catch (e: SerializationException) {
                        BlockchainSnackbar.make(this@with.root, "Malformed Json!", type = SnackbarType.Error).show()
                    }
                }
            }
        }
    }

    private fun resetRuntimePermissionCooldown() {
        runtimePermissionsPrefs.notificationLastRequestMillis = 0
    }

    private fun showSnackbar(text: String) {
        BlockchainSnackbar.make(
            binding.root,
            text,
            duration = Snackbar.LENGTH_SHORT
        ).show()
    }

    private fun onResetUserCache() {
        getUserStore.markAsStale()
        kycTiersStore.invalidate()
    }

    private fun onResetStoreCaches() {
        lifecycleScope.launch {
            storeWiper.wipe()
        }
    }

    private fun showInviteNow() {
        showBottomSheet(
            ReferralInviteNowSheet()
        )
    }

    private fun clearSimpleBuyState() {
        simpleBuyPrefs.clearBuyState()
        showSnackbar("Local SB State cleared")
    }

    private fun resetAppRating() {
        appRatingPrefs.resetAppRatingData()
    }

    private fun onRndDeviceId() {
        sessionPrefs.qaRandomiseDeviceId = true
        showSnackbar("Device ID randomisation enabled")
    }

    private fun onResetWallet() {
        appUtils.clearCredentialsAndRestart()
        showSnackbar("Wallet reset")
    }

    private fun onResetPrefs() {
        sessionPrefs.clear()
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
