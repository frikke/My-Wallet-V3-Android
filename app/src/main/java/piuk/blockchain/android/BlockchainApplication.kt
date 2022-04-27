package piuk.blockchain.android

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.RemoteException
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ProcessLifecycleOwner
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.blockchain.analytics.Analytics
import com.blockchain.analytics.events.AppLaunchEvent
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.koin.KoinStarter
import com.blockchain.lifecycle.LifecycleInterestedComponent
import com.blockchain.logging.RemoteLogger
import com.blockchain.preferences.AppInfoPrefs
import com.blockchain.preferences.AppInfoPrefs.Companion.DEFAULT_APP_VERSION_CODE
import com.facebook.stetho.Stetho
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.security.ProviderInstaller
import com.google.android.play.core.missingsplits.MissingSplitsManagerFactory
import io.intercom.android.sdk.Intercom
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.exceptions.CompositeException
import io.reactivex.rxjava3.exceptions.OnErrorNotImplementedException
import io.reactivex.rxjava3.exceptions.UndeliverableException
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.plugins.RxJavaPlugins
import io.reactivex.rxjava3.schedulers.Schedulers
import java.lang.RuntimeException
import org.koin.android.ext.android.inject
import piuk.blockchain.android.data.coinswebsocket.service.CoinsWebSocketService
import piuk.blockchain.android.data.connectivity.ConnectivityManager
import piuk.blockchain.android.identity.SiftDigitalTrust
import piuk.blockchain.android.ui.home.models.MetadataEvent
import piuk.blockchain.android.ui.ssl.SSLVerifyActivity
import piuk.blockchain.android.util.AppAnalytics
import piuk.blockchain.android.util.AppUtil
import piuk.blockchain.android.util.CurrentContextAccess
import piuk.blockchain.android.util.lifecycle.AppLifecycleListener
import piuk.blockchain.androidcore.data.connectivity.ConnectionEvent
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.data.rxjava.SSLPinningObservable
import timber.log.Timber

open class BlockchainApplication : Application() {

    private val environmentSettings: EnvironmentConfig by inject()
    private val lifeCycleInterestedComponent: LifecycleInterestedComponent by inject()
    private val appInfoPrefs: AppInfoPrefs by inject()
    private val rxBus: RxBus by inject()
    private val sslPinningObservable: SSLPinningObservable by inject()
    private val currentContextAccess: CurrentContextAccess by inject()
    private val appUtils: AppUtil by inject()
    private val analytics: Analytics by inject()
    private val remoteLogger: RemoteLogger by inject()
    private val coinsWebSocketService: CoinsWebSocketService by inject()
    private val trust: SiftDigitalTrust by inject()

    private val lifecycleListener: AppLifecycleListener by lazy {
        AppLifecycleListener(lifeCycleInterestedComponent, remoteLogger)
    }

    override fun onCreate() {

        if (MissingSplitsManagerFactory.create(this).disableAppIfMissingRequiredSplits()) {
            // Skip rest of the initialization to prevent the app from crashing.
            return
        }

        super.onCreate()

        // TODO disable dark mode for now, re-enable once we're further into the redesign
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        // Init Timber
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Build the DI graphs:
        KoinStarter.start(this)
        initRemoteLogger()
        initLifecycleListener()

        if (environmentSettings.isCompanyInternalBuild() || environmentSettings.isRunningInDebugMode()) {
            Intercom.initialize(this, BuildConfig.INTERCOM_API_KEY, BuildConfig.INTERCOM_APP_ID)
        }

        if (environmentSettings.isRunningInDebugMode()) {
            Stetho.initializeWithDefaults(this)
        }

        UncaughtExceptionHandler.install(appUtils)

        RxJavaPlugins.setErrorHandler { _throwable ->
            val exception = when {
                (_throwable is CompositeException) -> _throwable.exceptions[0]
                (_throwable is OnErrorNotImplementedException) -> _throwable.cause
                (_throwable is UndeliverableException) -> _throwable.cause
                else -> _throwable
            }
            if (exception is RuntimeException) {
                throw exception
            }
            Timber.tag(RX_ERROR_TAG).e(exception)
        }

        ConnectivityManager.getInstance().registerNetworkListener(this, rxBus)

        checkSecurityProviderAndPatchIfNeeded()

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)

        registerActivityLifecycleCallbacks(activityCallback)

        // Report Google Play Services availability
        analytics.logEvent(AppLaunchEvent(isGooglePlayServicesAvailable(this)))
        // Register the notification channel if necessary
        initNotifications()

        sslPinningObservable
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onNext = ::onConnectionEvent)

        initRxBus()

        AppVersioningChecks(
            context = this,
            appInfoPrefs = appInfoPrefs,
            onAppInstalled = { code, name, installReferrer, installTimestampSeconds, adId ->
                onAppInstalled(
                    code, name, installReferrer, installTimestampSeconds, adId
                )
            },
            onAppAppUpdated = { appUpdated -> onAppUpdated(appUpdated) }
        ).checkForPotentialNewInstallOrUpdate()
    }

    private fun onAppUpdated(updateInfo: AppUpdateInfo) {
        analytics.logEvent(
            AppAnalytics.AppUpdated(
                previousVersionCode = updateInfo.previousVersionCode,
                installedVersion = updateInfo.installedVersionName,
                currentVersionCode = updateInfo.versionCode,
                currentVersionName = updateInfo.versionName
            )
        )
    }

    private fun onAppInstalled(
        versionCode: Int,
        versionName: String,
        installReferrer: String,
        installTimestampSeconds: Long,
        adId: String
    ) {
        analytics.logEvent(
            AppAnalytics.AppInstalled(
                versionCode = versionCode,
                versionName = versionName,
                installReferrer = installReferrer,
                installBeginSeconds = installTimestampSeconds,
                adId = adId
            )
        )
    }

    @SuppressLint("CheckResult")
    private fun initRxBus() {
        rxBus.register(MetadataEvent::class.java)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onNext = ::onBusMetadataEvent)
    }

    private fun onConnectionEvent(event: ConnectionEvent) {
        SSLVerifyActivity.start(applicationContext, event)
    }

    private fun onBusMetadataEvent(event: MetadataEvent) {
        startCoinsWebService()
    }

    private fun startCoinsWebService() {
        coinsWebSocketService.start()
    }

    private fun initLifecycleListener() {
        ProcessLifecycleOwner.get().lifecycle
            .addObserver(lifecycleListener)
    }

    private fun initNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            // Create the NotificationChannel
            val channel2FA = NotificationChannel(
                "notifications_2fa",
                getString(R.string.notification_2fa_summary),
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = getString(R.string.notification_2fa_description) }

            // We create two channels, since the user may want to opt out of
            // payments notifications in the settings, and we don't need the
            // high importance flag on those.
            val channelPayments = NotificationChannel(
                "notifications_payments",
                getString(R.string.notification_payments_summary),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = getString(R.string.notification_payments_description) }
            // TODO do we want some custom vibration pattern?
            notificationManager.createNotificationChannel(channel2FA)
            notificationManager.createNotificationChannel(channelPayments)
        }
    }

    private fun initRemoteLogger() {
        remoteLogger.init(this)
        remoteLogger.userLanguageLocale(resources.configuration.locale.language)
    }

    /**
     * This patches a device's Security Provider asynchronously to help defend against various
     * vulnerabilities. This provider is normally updated in Google Play Services anyway, but this
     * will catch any immediate issues that haven't been fixed in a slow rollout.
     *
     * In the future, we may want to show some kind of warning to users or even stop the app, but
     * this will harm users with versions of Android without GMS approval.
     *
     * @see [Updating
     * Your Security Provider](https://developer.android.com/training/articles/security-gms-provider.html)
     */
    protected open fun checkSecurityProviderAndPatchIfNeeded() {
        ProviderInstaller.installIfNeededAsync(
            this,
            object : ProviderInstaller.ProviderInstallListener {
                override fun onProviderInstalled() {
                    Timber.i("Security Provider installed")
                }

                override fun onProviderInstallFailed(errorCode: Int, intent: Intent?) {
                    if (GoogleApiAvailability.getInstance().isUserResolvableError(errorCode)) {
                        showError(errorCode)
                    } else {
                        // Google Play services is not available.
                        onProviderInstallerNotAvailable()
                    }
                }
            }
        )
    }

    /**
     * Show a dialog prompting the user to install/update/enable Google Play services.
     *
     * @param errorCode Recoverable error code
     */
    internal fun showError(errorCode: Int) {
        // TODO: 05/08/2016 Decide if we should alert users here or not
        Timber.e(
            "Security Provider install failed with recoverable error: %s",
            GoogleApiAvailability.getInstance().getErrorString(errorCode)
        )
    }

    /**
     * This is reached if the provider cannot be updated for some reason. App should consider all
     * HTTP communication to be vulnerable, and take appropriate action.
     */
    internal fun onProviderInstallerNotAvailable() {
        // TODO: 05/08/2016 Decide if we should take action here or not
        // Timber.wtf("Security Provider Installer not available")
    }

    /**
     * Returns true if Google Play Services are found and ready to use.
     *
     * @param context The current Application Context
     */
    private fun isGooglePlayServicesAvailable(context: Context): Boolean {
        val availability = GoogleApiAvailability.getInstance()
        return availability.isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
    }

    private val activityCallback = object : ActivityLifecycleCallbacks {

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            trust.onActivityCreate(activity)
            trackPotentialDeeplink(activity)
        }

        override fun onActivityResumed(activity: Activity) {
            currentContextAccess.contextOpen(activity)
            trust.onActivityResume(activity)
        }

        override fun onActivityPaused(activity: Activity) {
            currentContextAccess.contextClose(activity)
            trust.onActivityPause()
        }

        override fun onActivityStarted(activity: Activity) {}

        override fun onActivityStopped(activity: Activity) {
            trust.onActivityClose()
        }

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

        override fun onActivityDestroyed(activity: Activity) {}
    }

    private fun trackPotentialDeeplink(activity: Activity) {
        activity.intent?.data?.let { analytics.logEvent(AppAnalytics.AppDeepLinked) }
    }

    companion object {
        private const val RX_ERROR_TAG = "RxJava Error"
    }
}

private class AppVersioningChecks(
    private val context: Context,
    private val appInfoPrefs: AppInfoPrefs,
    private val onAppInstalled: (
        versionCode: Int,
        versionName: String,
        installReferrer: String,
        installTimestampSeconds: Long,
        adId: String
    ) -> Unit,
    private val onAppAppUpdated: (appUpdated: AppUpdateInfo) -> Unit
) {

    fun checkForPotentialNewInstallOrUpdate() {
        val installedVersion =
            appInfoPrefs.installationVersionName.takeIf { it != AppInfoPrefs.DEFAULT_APP_VERSION_NAME }
        installedVersion?.let {
            checkForPotentialUpdate(it)
        } ?: kotlin.run {
            checkForInstalledVersion()
        }
    }

    private fun checkForInstalledVersion() {
        getAdvertisingId(context)
            .subscribeBy { adId ->
                val referrerClient = InstallReferrerClient.newBuilder(context).build()
                referrerClient.startConnection(object : InstallReferrerStateListener {
                    override fun onInstallReferrerSetupFinished(responseCode: Int) {
                        when (responseCode) {
                            InstallReferrerClient.InstallReferrerResponse.OK -> {
                                try {
                                    referrerClient.installReferrer?.installVersion?.let {
                                        appInfoPrefs.installationVersionName = it
                                        val runningVersionIsTheInstalled = it == BuildConfig.VERSION_NAME
                                        if (runningVersionIsTheInstalled) {
                                            onAppInstalled(
                                                BuildConfig.VERSION_CODE,
                                                BuildConfig.VERSION_NAME,
                                                referrerClient.installReferrer.installReferrer,
                                                referrerClient.installReferrer.installBeginTimestampSeconds,
                                                adId
                                            )
                                        } else {
                                            checkForPotentialUpdate(it)
                                        }
                                    }
                                } catch (e: RemoteException) {
                                    Timber.e(e)
                                } finally {
                                    referrerClient.endConnection()
                                }
                            }
                            InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED -> {
                                referrerClient.endConnection()
                            }
                            InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE -> {
                                referrerClient.endConnection()
                            }
                        }
                    }

                    override fun onInstallReferrerServiceDisconnected() {}
                })
            }
    }

    private fun getAdvertisingId(context: Context): Single<String> {
        return Single.fromCallable {
            AdvertisingIdClient.getAdvertisingIdInfo(context).id.orEmpty()
        }.onErrorReturn {
            ""
        }.subscribeOn(Schedulers.io())
    }

    private fun checkForPotentialUpdate(installedVersion: String) {

        val runningVersionName = BuildConfig.VERSION_NAME
        val runningVersionCode = BuildConfig.VERSION_CODE
        val versionCodeUpdated = runningVersionCode != appInfoPrefs.currentStoredVersionCode

        val appHasJustBeenUpdated = runningVersionName != installedVersion && versionCodeUpdated

        if (appHasJustBeenUpdated) {
            onAppAppUpdated(
                AppUpdateInfo(
                    versionCode = runningVersionCode,
                    versionName = runningVersionName,
                    installedVersionName = installedVersion,
                    previousVersionCode = appInfoPrefs.currentStoredVersionCode.takeIf {
                        it != DEFAULT_APP_VERSION_CODE
                    }
                )
            )
        }
        if (versionCodeUpdated) {
            appInfoPrefs.currentStoredVersionCode = runningVersionCode
        }
    }
}

data class AppUpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val previousVersionCode: Int?,
    val installedVersionName: String
)
