package com.blockchain.core.settings

import com.blockchain.api.services.WalletSettingsService
import com.blockchain.core.settings.datastore.SettingsStore
import com.blockchain.core.utils.schedulers.applySchedulers
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.store.firstOutcome
import com.blockchain.utils.rxSingleOutcome
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.FiatCurrency
import info.blockchain.wallet.api.data.Settings
import info.blockchain.wallet.settings.SettingsManager
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.rx3.asCoroutineDispatcher
import kotlinx.coroutines.rx3.await

class SettingsDataManager(
    private val settingsService: SettingsService,
    private val settingsStore: SettingsStore,
    private val currencyPrefs: CurrencyPrefs,
    private val walletSettingsService: WalletSettingsService,
    private val assetCatalogue: AssetCatalogue
) {
    /**
     * Grabs the latest user [Settings] object from memory, or makes a web request if not
     * available.
     *
     * @return An [Observable] object wrapping a [Settings] object
     */
    fun getSettings(): Observable<Settings> =
        rxSingleOutcome(Schedulers.io().asCoroutineDispatcher()) {
            settingsStore.stream(FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale)).firstOutcome()
        }.toObservable()

    fun clearSettingsCache() = settingsStore.markAsStale()

    /**
     * Updates the settings object by syncing it with the server. Must be called to set up the
     * [SettingsManager] class before a fetch is called.
     *
     * @param guid The user's GUID
     * @param sharedKey The shared key
     * @return An [Observable] object wrapping a [Settings] object
     */
    fun initSettings(guid: String, sharedKey: String): Observable<Settings> {
        settingsService.initSettings(guid, sharedKey)
        return fetchSettings().applySchedulers()
    }

    /**
     * Fetches the latest user [Settings] object from the server
     *
     * @return An [Single] object wrapping a [Settings] object
     */
    fun fetchSettings(): Observable<Settings> =
        rxSingleOutcome(Schedulers.io().asCoroutineDispatcher()) {
            settingsStore.stream(FreshnessStrategy.Fresh).firstOutcome()
        }.toObservable()
            .applySchedulers()

    /**
     * Update the user's email and fetches an updated [Settings] object.
     *
     * !!! Do not call this directly, it is best to use [EmailSyncUpdater] as it handles syncing of changes with Nabu.
     *
     * @param email The email to be stored
     * @return An [Observable] object wrapping a [Settings] object
     */
    fun updateEmail(email: String): Observable<Settings> =
        settingsService.updateEmail(email)
            .flatMap { fetchSettings() }
            .applySchedulers()

    fun resendVerificationEmail(email: String): Observable<Settings> =
        settingsService.resendVerificationEmail(email)
            .flatMap { fetchSettings() }
            .applySchedulers()

    /**
     * Update the user's phone number and fetches an updated [Settings] object.
     *
     * @param sms The phone number to be stored
     * @return An [Observable] object wrapping a [Settings] object
     */
    // TODO DELETE WHEN REMOVING REDESIGN FF (and the rest of the methods linked)
    fun updateSms(sms: String): Observable<Settings> =
        settingsService.updateSms(sms)
            .flatMap { fetchSettings() }
            .applySchedulers()

    /**
     * Update the user's phone number and fetches an updated [Settings] object.
     *
     * @param sms The phone number to be stored
     * @return An [Observable] object wrapping a [Settings] object
     */
    fun updateSms(sms: String, forceJson: Boolean): Single<Settings> =
        settingsService.updateSms(sms, forceJson)
            .flatMap { it.handleError() }
            .flatMap { fetchSettings().firstOrError() }
            .applySchedulers()

    /**
     * Verify the user's phone number with a verification code and fetches an updated [Settings] object.
     *
     * @param code The verification code
     * @return An [Observable] object wrapping a [Settings] object
     */
    fun verifySms(code: String): Observable<Settings> =
        settingsService.verifySms(code)
            .flatMap { fetchSettings() }
            .applySchedulers()

    /**
     * Update the user's Tor blocking preference and fetches an updated [Settings] object.
     *
     * @param blocked The user's preference for blocking Tor requests
     * @return An [Observable] object wrapping a [Settings] object
     */
    fun updateTor(blocked: Boolean): Observable<Settings> =
        settingsService.updateTor(blocked)
            .flatMap { fetchSettings() }
            .applySchedulers()

    /**
     * Update the user's two factor status
     *
     * @param authType The auth type being used for 2FA
     * @return An [Observable] object wrapping a [Settings] object
     * @see SettingsManager for notification types
     */
    fun updateTwoFactor(authType: Int): Observable<Settings> =
        settingsService.updateTwoFactor(authType)
            .flatMap { fetchSettings() }
            .applySchedulers()

    /**
     * Update the user's notification preferences and fetches an updated [Settings] object.
     *
     * @param notificationType The type of notification to enable
     * @param notifications An ArrayList of the currently enabled notifications
     * @return An [Observable] object wrapping a [Settings] object
     * @see SettingsManager for notification types
     */
    fun enableNotification(notificationType: Int, notifications: List<Int>): Observable<Settings> {
        return if (notifications.isEmpty() || notifications.contains(SettingsManager.NOTIFICATION_TYPE_NONE)) {
            // No notification type registered, enable
            settingsService.enableNotifications(true)
                .flatMap { updateNotifications(notificationType) }
                .applySchedulers()
        } else if (notifications.size == 1 &&
            (
                notifications.contains(SettingsManager.NOTIFICATION_TYPE_EMAIL) &&
                    notificationType == SettingsManager.NOTIFICATION_TYPE_SMS ||
                    notifications.contains(SettingsManager.NOTIFICATION_TYPE_SMS) &&
                    notificationType == SettingsManager.NOTIFICATION_TYPE_EMAIL
                )
        ) {
            // Contains another type already, send "All"
            settingsService.enableNotifications(true)
                .flatMap { updateNotifications(SettingsManager.NOTIFICATION_TYPE_ALL) }
                .applySchedulers()
        } else {
            settingsService.enableNotifications(true)
                .flatMap { updateNotifications(notificationType) }
                .applySchedulers()
        }
    }

    /**
     * Update the user's notification preferences and fetches an updated [Settings] object.
     *
     * @param notificationType The type of notification to disable
     * @param notifications An ArrayList of the currently enabled notifications
     * @return An [Observable] object wrapping a [Settings] object
     * @see SettingsManager for notification types
     */
    fun disableNotification(notificationType: Int, notifications: List<Int>): Observable<Settings> {
        return if (notifications.isEmpty() || notifications.contains(SettingsManager.NOTIFICATION_TYPE_NONE)) {
            // No notifications anyway, return Settings
            fetchSettings()
                .applySchedulers()
        } else if (notifications.contains(SettingsManager.NOTIFICATION_TYPE_ALL) ||
            notifications.contains(SettingsManager.NOTIFICATION_TYPE_EMAIL) &&
            notifications.contains(SettingsManager.NOTIFICATION_TYPE_SMS)
        ) {
            // All types enabled, disable passed type and enable other
            updateNotifications(
                if (notificationType == SettingsManager.NOTIFICATION_TYPE_EMAIL) {
                    SettingsManager.NOTIFICATION_TYPE_SMS
                } else {
                    SettingsManager.NOTIFICATION_TYPE_EMAIL
                }
            ).applySchedulers()
        } else if (notifications.size == 1) {
            if (notifications[0] == notificationType) {
                // Remove all
                settingsService.enableNotifications(false)
                    .flatMap { updateNotifications(SettingsManager.NOTIFICATION_TYPE_NONE) }
                    .applySchedulers()
            } else {
                // Notification type not present, no need to remove it
                fetchSettings()
                    .applySchedulers()
            }
        } else {
            // This should never be reached
            fetchSettings()
                .applySchedulers()
        }
    }

    /**
     * Updates a passed notification type and then fetches the current settings object.
     *
     * @param notificationType The notification type you wish to enable/disable
     * @return An [Observable] wrapping the Settings object
     * @see SettingsManager for notification types
     */
    private fun updateNotifications(notificationType: Int): Observable<Settings> =
        settingsService.updateNotifications(notificationType)
            .flatMap { fetchSettings() }
            .applySchedulers()

    /**
     * Update the user's fiat unit preference and fetches an updated [Settings] object.
     *
     * @param fiatUnit The user's preference for fiat unit
     * @return An [Observable] object wrapping a [Settings] object
     */
    fun updateFiatUnit(fiatUnit: FiatCurrency): Observable<Settings> =
        settingsService.updateFiatUnit(fiatUnit.networkTicker)
            .flatMap { fetchSettings() }
            .doOnNext {
                currencyPrefs.selectedFiatCurrency = fiatUnit
            }
            .applySchedulers()

    fun setDefaultUserFiat(): Single<String> {
        val userFiat = FiatCurrency.locale().takeIf { assetCatalogue.fiatFromNetworkTicker(it.networkTicker) != null }
            ?: FiatCurrency.Dollars
        return settingsService.updateFiatUnit(userFiat.networkTicker)
            .flatMap { fetchSettings() }
            .singleOrError()
            .map { it.currency }
            .doFinally { currencyPrefs.selectedFiatCurrency = userFiat }
    }

    fun triggerEmailAlertLegacy(guid: String, sharedKey: String) =
        walletSettingsService.triggerAlert(
            guid = guid,
            sharedKey = sharedKey
        )

    suspend fun triggerEmailAlert(guid: String, sharedKey: String) =
        walletSettingsService.triggerAlert(
            guid = guid,
            sharedKey = sharedKey
        ).await()

    fun fetchWalletSettings(guid: String, sharedKey: String) =
        walletSettingsService.fetchWalletSettings(
            guid = guid,
            sharedKey = sharedKey
        )

    fun triggerOnChainTransaction(
        guid: String,
        sharedKey: String,
        currency: String,
        amount: String
    ): Completable {
        return walletSettingsService.triggerOnChainTransaction(
            guid = guid,
            sharedKey = sharedKey,
            currency = currency,
            amount = amount
        )
    }
}
