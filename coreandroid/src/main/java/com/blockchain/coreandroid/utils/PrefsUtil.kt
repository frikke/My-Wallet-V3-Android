package com.blockchain.coreandroid.utils

import android.annotation.SuppressLint
import android.app.backup.BackupManager
import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.annotation.VisibleForTesting
import com.blockchain.core.chains.ethereum.EthDataManager.Companion.ETH_CHAIN_ID
import com.blockchain.core.utils.AESUtilWrapper
import com.blockchain.core.utils.DeviceIdGeneratorService
import com.blockchain.core.utils.EncryptedPrefs
import com.blockchain.core.utils.UUIDGenerator
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.preferences.AppInfoPrefs
import com.blockchain.preferences.AppMaintenancePrefs
import com.blockchain.preferences.AppRatingPrefs
import com.blockchain.preferences.AuthPrefs
import com.blockchain.preferences.Authorization
import com.blockchain.preferences.BankLinkingPrefs
import com.blockchain.preferences.BrowserIdentity
import com.blockchain.preferences.BrowserIdentityMapping
import com.blockchain.preferences.CountryPrefs
import com.blockchain.preferences.CowboysPrefs
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.DashboardPrefs
import com.blockchain.preferences.DexPrefs
import com.blockchain.preferences.ExchangeCampaignPrefs
import com.blockchain.preferences.IterableAnnouncementsPrefs
import com.blockchain.preferences.LocalSettingsPrefs
import com.blockchain.preferences.MaskedValuePrefs
import com.blockchain.preferences.NftAnnouncementPrefs
import com.blockchain.preferences.NotificationPrefs
import com.blockchain.preferences.OnboardingPrefs
import com.blockchain.preferences.PricesPrefs
import com.blockchain.preferences.RecurringBuyPrefs
import com.blockchain.preferences.ReferralPrefs
import com.blockchain.preferences.RemoteConfigPrefs
import com.blockchain.preferences.SecureChannelPrefs
import com.blockchain.preferences.SecurityPrefs
import com.blockchain.preferences.SessionPrefs
import com.blockchain.preferences.SimpleBuyPrefs
import com.blockchain.preferences.SmallBalancesPrefs
import com.blockchain.preferences.SuperAppMvpPrefs
import com.blockchain.preferences.TransactionPrefs
import com.blockchain.preferences.WalletModePrefs
import com.blockchain.preferences.WalletStatusPrefs
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.FiatCurrency
import info.blockchain.wallet.crypto.AESUtil
import java.util.concurrent.TimeUnit
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.Sha256Hash
import org.spongycastle.util.encoders.Hex

class PrefsUtil(
    private val ctx: Context,
    private val store: SharedPreferences,
    private val backupStore: SharedPreferences,
    private val idGenerator: DeviceIdGeneratorService,
    private val uuidGenerator: UUIDGenerator,
    private val assetCatalogue: AssetCatalogue,
    private val environmentConfig: EnvironmentConfig
) : SessionPrefs,
    CurrencyPrefs,
    NotificationPrefs,
    DashboardPrefs,
    SecurityPrefs,
    SecureChannelPrefs,
    WalletModePrefs,
    PricesPrefs,
    SmallBalancesPrefs,
    DexPrefs,
    SimpleBuyPrefs,
    RecurringBuyPrefs,
    WalletStatusPrefs,
    TransactionPrefs,
    EncryptedPrefs,
    AuthPrefs,
    BankLinkingPrefs,
    AppInfoPrefs,
    RemoteConfigPrefs,
    OnboardingPrefs,
    AppMaintenancePrefs,
    AppRatingPrefs,
    NftAnnouncementPrefs,
    ReferralPrefs,
    LocalSettingsPrefs,
    SuperAppMvpPrefs,
    CowboysPrefs,
    CountryPrefs,
    ExchangeCampaignPrefs,
    IterableAnnouncementsPrefs,
    MaskedValuePrefs {

    private var isUnderAutomationTesting = false // Don't persist!

    override val isUnderTest: Boolean
        get() = isUnderAutomationTesting

    override fun setIsUnderTest() {
        isUnderAutomationTesting = true
    }

    override val deviceId: String
        get() {
            return if (qaRandomiseDeviceId) {
                uuidGenerator.generateUUID()
            } else {
                var deviceId = getValue(KEY_PRE_IDV_DEVICE_ID, "")
                if (deviceId.isEmpty()) {
                    deviceId = idGenerator.generateId()
                    setValue(KEY_PRE_IDV_DEVICE_ID, deviceId)
                }
                deviceId
            }
        }

    override var pinId: String
        get() = getValue(KEY_PIN_IDENTIFIER) ?: backupStore.getString(KEY_PIN_IDENTIFIER, null) ?: ""
        @SuppressLint("ApplySharedPref")
        set(value) {
            setValue(KEY_PIN_IDENTIFIER, value)
            backupStore.edit().putString(KEY_PIN_IDENTIFIER, value).commit()
            BackupManager.dataChanged(ctx.packageName)
        }

    override var devicePreIDVCheckFailed: Boolean
        get() = getValue(KEY_PRE_IDV_FAILED, false)
        set(value) = setValue(KEY_PRE_IDV_FAILED, value)

    override var showTradingAccountsOnPkwMode: Boolean
        get() = getValue(KEY_SHOW_TRADING_ON_PKW_MODE, false)
        set(value) {
            setValue(KEY_SHOW_TRADING_ON_PKW_MODE, value)
        }

    override var showPkwAccountsOnTradingMode: Boolean
        get() = getValue(KEY_SHOW_PKW_ON_TRADING_MODE, true)
        set(value) {
            setValue(KEY_SHOW_PKW_ON_TRADING_MODE, value)
        }

    override var isOnboardingComplete: Boolean
        get() = getValue(KEY_ONBOARDING_COMPLETE, false)
        set(completed) = setValue(KEY_ONBOARDING_COMPLETE, completed)

    override var isCustodialIntroSeen: Boolean
        get() = getValue(KEY_CUSTODIAL_INTRO_SEEN, false)
        set(seen) = setValue(KEY_CUSTODIAL_INTRO_SEEN, seen)

    override var isPrivateKeyIntroSeen: Boolean
        get() = getValue(KEY_PRIVATE_KEY_INTRO_SEEN, false)
        set(seen) = setValue(KEY_PRIVATE_KEY_INTRO_SEEN, seen)

    override var isRewardsIntroSeen: Boolean
        get() = getValue(KEY_REWARDS_INTRO_SEEN, false)
        set(seen) = setValue(KEY_REWARDS_INTRO_SEEN, seen)

    override var isStakingIntroSeen: Boolean
        get() = getValue(KEY_STAKING_INTRO_SEEN, false)
        set(seen) = setValue(KEY_STAKING_INTRO_SEEN, seen)

    override var isActiveRewardsIntroSeen: Boolean
        get() = getValue(KEY_ACTIVE_REWARDS_INTRO_SEEN, false)
        set(seen) = setValue(KEY_ACTIVE_REWARDS_INTRO_SEEN, seen)

    override var remainingSendsWithoutBackup: Int
        get() = getValue(KEY_REMAINING_SENDS_WITHOUT_BACKUP, MAX_ALLOWED_SENDS)
        set(remaining) = setValue(KEY_REMAINING_SENDS_WITHOUT_BACKUP, remaining)

    override val areScreenshotsEnabled: Boolean
        get() = getValue(KEY_SCREENSHOTS_ENABLED, false)

    override var trustScreenOverlay: Boolean
        get() = getValue(KEY_OVERLAY_TRUSTED, environmentConfig.isRunningInDebugMode())
        set(v) = setValue(KEY_OVERLAY_TRUSTED, v)

    // SecurityPrefs
    override var disableRootedWarning: Boolean
        get() = getValue(KEY_ROOT_WARNING_DISABLED, false)
        set(v) = setValue(KEY_ROOT_WARNING_DISABLED, v)

    override fun setScreenshotsEnabled(enable: Boolean) =
        setValue(KEY_SCREENSHOTS_ENABLED, enable)

    // From CurrencyPrefs
    override var selectedFiatCurrency: FiatCurrency
        get() = assetCatalogue.fiatFromNetworkTicker(getValue(KEY_SELECTED_FIAT, ""))
            ?: FiatCurrency.locale().takeIf { assetCatalogue.fiatFromNetworkTicker(it.networkTicker) != null }
            ?: FiatCurrency.Dollars
        set(fiat) {
            setValue(KEY_SELECTED_FIAT, fiat.networkTicker)
        }

    override val noCurrencySet: Boolean
        get() = getValue(KEY_SELECTED_FIAT, "").isEmpty()

    override fun simpleBuyState(): String? {
        return getValue(KEY_SIMPLE_BUY_STATE, "").takeIf { it != "" }
    }

    override fun cardState(): String? {
        return getValue(KEY_CARD_STATE, "").takeIf { it != "" }
    }

    override fun updateCardState(cardState: String) {
        setValue(KEY_CARD_STATE, cardState)
    }

    override fun clearCardState() {
        removeValue(KEY_CARD_STATE)
    }

    override fun updateSupportedCards(cardTypes: String) {
        setValue(KEY_SUPPORTED_CARDS_STATE, cardTypes)
    }

    override fun getSupportedCardTypes(): String? =
        getValue(KEY_SUPPORTED_CARDS_STATE, "").takeIf { it != "" }

    override fun getLastAmount(pair: String): String = getValue(KEY_SIMPLE_BUY_AMOUNT + pair, "")

    override fun setLastAmount(pair: String, amount: String) = setValue(KEY_SIMPLE_BUY_AMOUNT + pair, amount)

    override fun getLastPaymentMethodId(): String = getValue(KEY_SIMPLE_BUY_PAYMENT_ID, "")

    override fun setLastPaymentMethodId(paymentMethodId: String) = setValue(KEY_SIMPLE_BUY_PAYMENT_ID, paymentMethodId)

    override fun updateSimpleBuyState(simpleBuyState: String) = setValue(KEY_SIMPLE_BUY_STATE, simpleBuyState)

    override fun getBankLinkingState(): String = getValue(KEY_BANK_LINKING, "")

    override fun setBankLinkingState(state: String) = setValue(KEY_BANK_LINKING, state)

    override fun getDynamicOneTimeTokenUrl(): String = getValue(KEY_ONE_TIME_TOKEN_PATH, "")

    override val isRemoteConfigStale: Boolean
        get() = getValue(CONFIG_STALE, false)

    override fun updateRemoteConfigStaleStatus(isStale: Boolean) {
        setValue(CONFIG_STALE, isStale)
    }

    override val brokerageErrorsCode: String
        get() = getValue(BROKERAGE_ERROR_CODE, "")

    override fun updateBrokerageErrorCode(code: String) = setValue(BROKERAGE_ERROR_CODE, code)

    override val brokerageErrorsEnabled: Boolean
        get() = getValue(BROKERAGE_ERRORS_ENABLED, false)

    override fun updateBrokerageErrorStatus(enabled: Boolean) = setValue(BROKERAGE_ERRORS_ENABLED, enabled)

    override var installationVersionName: String
        get() = getValue(APP_INSTALLATION_VERSION_NAME, AppInfoPrefs.DEFAULT_APP_VERSION_NAME)
        set(value) {
            setValue(APP_INSTALLATION_VERSION_NAME, value)
        }

    override var currentStoredVersionCode: Int
        get() = getValue(APP_CURRENT_VERSION_CODE, AppInfoPrefs.DEFAULT_APP_VERSION_CODE)
        set(value) {
            setValue(APP_CURRENT_VERSION_CODE, value)
        }

    override fun setDynamicOneTimeTokenUrl(path: String) {
        val previousValue = getDynamicOneTimeTokenUrl()
        if (path.isNotEmpty() && previousValue != path) {
            setValue(KEY_ONE_TIME_TOKEN_PATH, path)
        }
    }

    override fun clearBuyState() = removeValue(KEY_SIMPLE_BUY_STATE)

    override var isFirstTimeBuyer: Boolean
        get() = getValue(KEY_FIRST_TIME_BUYER, true)
        set(value) {
            setValue(KEY_FIRST_TIME_BUYER, value)
        }

    override var hasSeenRecurringBuyOptions: Boolean
        get() = getValue(KEY_HAS_SEEN_RB_OPTIONS, false)
        set(value) {
            setValue(KEY_HAS_SEEN_RB_OPTIONS, value)
        }

    override var tradingCurrency: FiatCurrency?
        get() = assetCatalogue.fromNetworkTicker(getValue(KEY_SIMPLE_BUY_CURRENCY, "")) as? FiatCurrency
        set(value) {
            if (value != null) {
                setValue(KEY_SIMPLE_BUY_CURRENCY, value.networkTicker)
            } else {
                removeValue(KEY_SIMPLE_BUY_CURRENCY)
            }
        }

    override var hasCompletedAtLeastOneBuy: Boolean
        get() = getValue(KEY_HAS_COMPLETED_AT_LEAST_ONE_BUY, false)
        set(value) {
            setValue(KEY_HAS_COMPLETED_AT_LEAST_ONE_BUY, value)
        }

    override var buysCompletedCount: Int
        get() = getValue(KEY_BUYS_COMPLETED_COUNT, if (hasCompletedAtLeastOneBuy) 1 else 0)
        set(value) {
            setValue(KEY_BUYS_COMPLETED_COUNT, value)
        }

    // Wallet Status
    override var lastBackupTime: Long
        get() = getValue(BACKUP_DATE_KEY, 0L)
        set(v) = setValue(BACKUP_DATE_KEY, v)

    override val isWalletBackedUp: Boolean
        get() = lastBackupTime != 0L

    override var isWalletBackUpSkipped: Boolean
        get() = getValue(IS_WALLET_BACKUP_SKIPPED, false)
        set(value) = setValue(IS_WALLET_BACKUP_SKIPPED, value)

    override var hasSeenDefiOnboarding: Boolean
        get() = getValue(HAS_SEEN_DEFI_ONBOARDING, false)
        set(value) = setValue(HAS_SEEN_DEFI_ONBOARDING, value)

    override var hasSeenEarnProductIntro: Boolean
        get() = getValue(HAS_SEEN_EARN_PRODUCT_ONBOARDING, false)
        set(value) = setValue(HAS_SEEN_EARN_PRODUCT_ONBOARDING, value)

    override val isWalletFunded: Boolean
        get() = getValue(WALLET_FUNDED_KEY, false)

    override fun setWalletFunded() = setValue(WALLET_FUNDED_KEY, true)

    override val hasMadeBitPayTransaction: Boolean
        get() = getValue(BITPAY_TRANSACTION_SUCCEEDED, false)

    override fun setBitPaySuccess() = setValue(BITPAY_TRANSACTION_SUCCEEDED, true)

    override fun setFeeTypeForAsset(asset: AssetInfo, type: Int) =
        setValue(NETWORK_FEE_PRIORITY_KEY + asset.networkTicker, type)

    override fun getFeeTypeForAsset(asset: AssetInfo): Int =
        getValue(NETWORK_FEE_PRIORITY_KEY + asset.networkTicker, -1)

    override val hasSeenSwapPromo: Boolean
        get() = getValue(SWAP_KYC_PROMO, false)

    override fun setSeenSwapPromo() = setValue(SWAP_KYC_PROMO, true)

    override var isNewlyCreated: Boolean
        get() = getValue(KEY_NEWLY_CREATED_WALLET, false)
        set(newlyCreated) = setValue(KEY_NEWLY_CREATED_WALLET, newlyCreated)

    override var isRestored: Boolean
        get() = getValue(KEY_RESTORED_WALLET, false)
        set(isRestored) = setValue(KEY_RESTORED_WALLET, isRestored)

    override var isAppUnlocked: Boolean
        get() = getValue(KEY_LOGGED_IN, false)
        set(loggedIn) = setValue(KEY_LOGGED_IN, loggedIn)

    override val resendSmsRetries: Int
        get() = getValue(TWO_FA_SMS_RETRIES, WalletStatusPrefs.MAX_ALLOWED_RETRIES)

    override fun setResendSmsRetries(retries: Int) {
        setValue(TWO_FA_SMS_RETRIES, retries)
    }

    override var email: String
        get() = getValue(KEY_EMAIL, "")
        set(value) = setValue(KEY_EMAIL, value)

    override var countrySelectedOnSignUp: String
        get() = getValue(COUNTRY_SIGN_UP, "")
        set(value) = setValue(COUNTRY_SIGN_UP, value)

    override var stateSelectedOnSignUp: String
        get() = getValue(STATE_SIGNED_UP, "")
        set(value) = setValue(STATE_SIGNED_UP, value)

    override fun clearGeolocationPreferences() {
        removeValue(COUNTRY_SIGN_UP)
        removeValue(STATE_SIGNED_UP)
    }

    // Notification prefs
    override var arePushNotificationsEnabled: Boolean
        get() = getValue(KEY_PUSH_NOTIFICATION_ENABLED, true)
        set(v) = setValue(KEY_PUSH_NOTIFICATION_ENABLED, v)

    override var firebaseToken: String
        get() = getValue(KEY_FIREBASE_TOKEN, "")
        set(v) = setValue(KEY_FIREBASE_TOKEN, v)

    @SuppressLint("ApplySharedPref")
    override fun backupCurrentPrefs(encryptionKey: String) {
        backupStore.edit()
            .clear()
            .putString(KEY_PIN_IDENTIFIER, getValue(KEY_PIN_IDENTIFIER, ""))
            .putString(KEY_ENCRYPTED_PASSWORD, getValue(KEY_ENCRYPTED_PASSWORD, ""))
            .putString(
                KEY_ENCRYPTED_GUID,
                AESUtilWrapper.encrypt(
                    getValue(KEY_WALLET_GUID, ""),
                    encryptionKey,
                    AESUtil.PIN_PBKDF2_ITERATIONS_GUID
                )
            )
            .putString(
                KEY_ENCRYPTED_SHARED_KEY,
                AESUtilWrapper.encrypt(
                    getValue(KEY_SHARED_KEY, ""),
                    encryptionKey,
                    AESUtil.PIN_PBKDF2_ITERATIONS_SHAREDKEY
                )
            )
            .commit()

        BackupManager.dataChanged(ctx.packageName)
    }

    override fun restoreFromBackup(decryptionKey: String) {
        // Pull in the values from the backup, we don't have local state
        setValue(
            KEY_PIN_IDENTIFIER,
            backupStore.getString(KEY_PIN_IDENTIFIER, "") ?: ""
        )
        setValue(
            KEY_ENCRYPTED_PASSWORD,
            backupStore.getString(KEY_ENCRYPTED_PASSWORD, "") ?: ""
        )
        setValue(
            KEY_WALLET_GUID,
            AESUtilWrapper.decrypt(
                backupStore.getString(KEY_ENCRYPTED_GUID, "").orEmpty(),
                decryptionKey,
                AESUtil.PIN_PBKDF2_ITERATIONS_GUID
            )
        )
        setValue(
            KEY_SHARED_KEY,
            AESUtilWrapper.decrypt(
                backupStore.getString(KEY_ENCRYPTED_SHARED_KEY, "").orEmpty(),
                decryptionKey,
                AESUtil.PIN_PBKDF2_ITERATIONS_SHAREDKEY
            )
        )
    }

    override var backupEnabled: Boolean
        get() = getValue(KEY_CLOUD_BACKUP_ENABLED, true)
        set(value) {
            setValue(KEY_CLOUD_BACKUP_ENABLED, value)
            if (!value) {
                clearBackup()
            }
        }

    override fun hasBackup(): Boolean =
        backupEnabled && backupStore.getString(KEY_ENCRYPTED_GUID, "").isNullOrEmpty().not()

    @SuppressLint("ApplySharedPref")
    override fun clearBackup() {
        // We need to set all the backed values here and not just clear(), since that deletes the
        // prefs files, so there is nothing to back up, so the next restore will return the wallet
        // we just logged out of.
        backupStore.edit()
            .putString(KEY_PIN_IDENTIFIER, "")
            .putString(KEY_ENCRYPTED_PASSWORD, "")
            .putString(KEY_ENCRYPTED_GUID, "")
            .putString(KEY_ENCRYPTED_SHARED_KEY, "")
            .commit()

        BackupManager.dataChanged(ctx.packageName)
    }

    override var encodedPin: String
        get() = decodeFromBase64ToString(getValue(KEY_ENCRYPTED_PIN_CODE, ""))
        set(value) = setValue(KEY_ENCRYPTED_PIN_CODE, encodeToBase64(value))

    override var biometricsEnabled: Boolean
        get() = getValue(KEY_FINGERPRINT_ENABLED, false)
        set(value) = setValue(KEY_FINGERPRINT_ENABLED, value)

    override var sharedKey: String
        get() = getValue(KEY_SHARED_KEY, "")
        set(value) = setValue(KEY_SHARED_KEY, value)

    override var walletGuid: String
        get() = getValue(KEY_WALLET_GUID, "")
        set(value) = setValue(KEY_WALLET_GUID, value)

    override var encryptedPassword: String
        get() = getValue(KEY_ENCRYPTED_PASSWORD, "")
        set(value) = setValue(KEY_ENCRYPTED_PASSWORD, value)

    override var pinFails: Int
        get() = getValue(KEY_PIN_FAILS, 0)
        set(value) = setValue(KEY_PIN_FAILS, value)

    override fun clearEncodedPin() {
        removeValue(KEY_ENCRYPTED_PIN_CODE)
    }

    override var sessionId: String
        get() = getValue(SESSION_ID, "")
        set(value) = setValue(SESSION_ID, value)

    override var emailVerified: Boolean
        get() = getValue(KEY_EMAIL_VERIFIED, false)
        set(value) = setValue(KEY_EMAIL_VERIFIED, value)

    // Onboarding Prefs
    override var isLandingCtaDismissed: Boolean
        get() = getValue(KEY_IS_LANDING_CTA_DISMISSED, false)
        set(value) = setValue(KEY_IS_LANDING_CTA_DISMISSED, value)

    override var isSendNetworkWarningDismissed: Boolean
        get() = getValue(KEY_IS_SEND_NETWORK_WARNING_DISMISSED, false)
        set(value) = setValue(KEY_IS_SEND_NETWORK_WARNING_DISMISSED, value)

    override fun clearSessionId() = removeValue(SESSION_ID)

    override fun removePinID() {
        removeValue(KEY_PIN_FAILS)
    }

    private fun encodeToBase64(data: String) =
        Base64.encodeToString(data.toByteArray(charset("UTF-8")), Base64.DEFAULT)

    private fun decodeFromBase64ToString(data: String): String =
        String(Base64.decode(data.toByteArray(charset("UTF-8")), Base64.DEFAULT))

    // Raw accessors
    private fun getValue(name: String): String? =
        store.getString(name, null)

    private fun getValue(name: String, defaultValue: String): String =
        store.getString(name, defaultValue).orEmpty()

    private fun getValue(name: String, defaultValue: Int): Int =
        store.getInt(name, defaultValue)

    private fun getValue(name: String, defaultValue: Long): Long =
        try {
            store.getLong(name, defaultValue)
        } catch (e: Exception) {
            store.getInt(name, defaultValue.toInt()).toLong()
        }

    private fun getValue(name: String, defaultValue: Boolean): Boolean =
        store.getBoolean(name, defaultValue)

    private fun setValue(name: String, value: String) {
        store.edit().putString(name, value).apply()
    }

    private fun setValue(name: String, value: Int) {
        store.edit().putInt(name, if (value < 0) 0 else value).apply()
    }

    private fun setValue(name: String, value: Long) {
        store.edit().putLong(name, if (value < 0L) 0L else value).apply()
    }

    private fun setValue(name: String, value: Boolean) {
        store.edit().putBoolean(name, value).apply()
    }

    private fun has(name: String): Boolean = store.contains(name)

    private fun removeValue(name: String) {
        store.edit().remove(name).apply()
    }

    override fun clear() {
        val versionCode = store.getInt(APP_CURRENT_VERSION_CODE, AppInfoPrefs.DEFAULT_APP_VERSION_CODE)
        val installedVersion = store.getString(APP_INSTALLATION_VERSION_NAME, AppInfoPrefs.DEFAULT_APP_VERSION_NAME)
            ?: AppInfoPrefs.DEFAULT_APP_VERSION_NAME
        val firebaseToken = store.getString(KEY_FIREBASE_TOKEN, "").orEmpty()
        val isLandingCtaDismissed = store.getBoolean(KEY_IS_LANDING_CTA_DISMISSED, false)

        store.edit().clear().apply()

        setValue(APP_CURRENT_VERSION_CODE, versionCode)
        setValue(APP_INSTALLATION_VERSION_NAME, installedVersion)
        setValue(KEY_FIREBASE_TOKEN, firebaseToken)
        setValue(KEY_IS_LANDING_CTA_DISMISSED, isLandingCtaDismissed)

        clearBackup()
    }

    // Secure Channel Prefs
    override val deviceKey: String
        get() = if (has(KEY_SECURE_CHANNEL_IDENTITY_KEY)) {
            getValue(KEY_SECURE_CHANNEL_IDENTITY_KEY, "")
        } else {
            val key = ECKey()
            setValue(KEY_SECURE_CHANNEL_IDENTITY_KEY, key.privateKeyAsHex)
            getValue(KEY_SECURE_CHANNEL_IDENTITY_KEY, "")
        }

    private fun getBrowserIdentityMapping() =
        Json.decodeFromString<BrowserIdentityMapping>(
            getValue(KEY_SECURE_CHANNEL_BROWSER_MAPPINGS, """{ "mapping": {} }""")
        )

    private fun setBrowserIdentityMapping(browserIdentity: BrowserIdentityMapping) =
        setValue(KEY_SECURE_CHANNEL_BROWSER_MAPPINGS, Json.encodeToString(browserIdentity))

    override fun getBrowserIdentity(pubkeyHash: String): BrowserIdentity? {
        val browserIdentityMapping = getBrowserIdentityMapping()
        val mapping = browserIdentityMapping.mapping
        return mapping.get(pubkeyHash)
    }

    override fun addBrowserIdentity(browserIdentity: BrowserIdentity) {
        getBrowserIdentityMapping().mapping
            .also { it.put(browserIdentity.pubKeyHash(), browserIdentity) }
            .also { setBrowserIdentityMapping(BrowserIdentityMapping(it)) }
    }

    override fun removeBrowserIdentity(pubkeyHash: String) {
        getBrowserIdentityMapping().mapping
            .also { it.remove(pubkeyHash) }
            .also { setBrowserIdentityMapping(BrowserIdentityMapping(it)) }
    }

    override fun updateBrowserIdentityUsedTimestamp(pubkeyHash: String) {
        getBrowserIdentityMapping().mapping
            .also { it.get(pubkeyHash)?.lastUsed = System.currentTimeMillis() }
            .also { setBrowserIdentityMapping(BrowserIdentityMapping(it)) }
    }

    override fun addBrowserIdentityAuthorization(pubkeyHash: String, authorization: Authorization) {
        getBrowserIdentityMapping().mapping
            .also { it.get(pubkeyHash)?.authorized?.add(authorization) }
            .also { setBrowserIdentityMapping(BrowserIdentityMapping(it)) }
    }

    override fun pruneBrowserIdentities() {
        getBrowserIdentityMapping().mapping
            .filterNot {
                it.value.lastUsed == 0L &&
                    (System.currentTimeMillis() - it.value.scanned) > TimeUnit.MINUTES.toMillis(2)
            }
            .toMutableMap()
            .also { setBrowserIdentityMapping(BrowserIdentityMapping(it)) }
    }

    // app maintenance
    override var isAppMaintenanceRemoteConfigIgnored: Boolean
        get() = getValue(APP_MAINTENANCE_IGNORE_REMOTE_CONFIG, false)
        set(value) = setValue(APP_MAINTENANCE_IGNORE_REMOTE_CONFIG, value)

    override var isAppMaintenanceDebugOverrideEnabled: Boolean
        get() = getValue(APP_MAINTENANCE_DEBUG_OVERRIDE, false)
        set(value) = setValue(APP_MAINTENANCE_DEBUG_OVERRIDE, value)

    override var appMaintenanceDebugJson: String
        get() = getValue(APP_MAINTENANCE_DEBUG_JSON, "")
        set(value) = setValue(APP_MAINTENANCE_DEBUG_JSON, value)

    // app rating
    override var completed: Boolean
        get() = getValue(APP_RATING_COMPLETED, false)
        set(value) = setValue(APP_RATING_COMPLETED, value)

    override var promptDateMillis: Long
        get() = getValue(APP_RATING_PROMPT_DATE, 0L)
        set(value) = setValue(APP_RATING_PROMPT_DATE, value)

    override fun resetAppRatingData() {
        completed = false
        promptDateMillis = 0L
    }

    override var isNftAnnouncementDismissed: Boolean
        get() = getValue(NFT_ANNOUNCEMENT_DISMISSED, false)
        set(value) = setValue(NFT_ANNOUNCEMENT_DISMISSED, value)

    override var isJoinNftWaitlistSuccessful: Boolean
        get() = getValue(NFT_ANNOUNCEMENT_JOIN_WAITLIST, false)
        set(value) = setValue(NFT_ANNOUNCEMENT_JOIN_WAITLIST, value)

    override var hasReferralIconBeenClicked: Boolean
        get() = getValue(REFERRAL_ICON_CLICKED, false)
        set(value) = setValue(REFERRAL_ICON_CLICKED, value)

    override var referralSuccessTitle: String
        get() = getValue(REFERRAL_SUCCESS_TITLE, "")
        set(value) = setValue(REFERRAL_SUCCESS_TITLE, value)

    override var referralSuccessBody: String
        get() = getValue(REFERRAL_SUCCESS_BODY, "")
        set(value) = setValue(REFERRAL_SUCCESS_BODY, value)

    override var isChartVibrationEnabled: Boolean
        get() = getValue(CHART_VIBRATION_ENABLED, true)
        set(value) = setValue(CHART_VIBRATION_ENABLED, value)

    override var hideSmallBalancesEnabled: Boolean
        get() = getValue(SMALL_BALANCES, true)
        set(value) = setValue(SMALL_BALANCES, value)

    // Session prefs
    override var qaRandomiseDeviceId: Boolean
        get() = getValue(KEY_IS_DEVICE_ID_RANDOMISED, false)
        set(value) = setValue(KEY_IS_DEVICE_ID_RANDOMISED, value)

    override fun recordDismissal(key: String, time: Long) =
        setValue(key, time)

    override fun deleteDismissalRecord(key: String) =
        removeValue(key)

    override fun getDismissalEntry(key: String): Long =
        getValue(key, 0L)

    override fun getLegacyDismissalEntry(key: String): Boolean =
        getValue(key, false)

    /**
     * Clears everything but the GUID for logging back in and the deviceId - for pre-IDV checking
     */
    override fun unPairWallet() {
        val guid = getValue(KEY_WALLET_GUID, "")
        val deviceId = getValue(KEY_PRE_IDV_DEVICE_ID, "")

        clear()

        setValue(KEY_LOGGED_IN, true)
        setValue(KEY_WALLET_GUID, guid)
        setValue(KEY_PRE_IDV_DEVICE_ID, deviceId)
    }

    override var metadataUri: String
        get() = getValue(KEY_METADATA_URI, "")
        set(value) = setValue(KEY_METADATA_URI, value)

    override var keySchemeUrl: String
        get() = getValue(KEY_SCHEME_URL, "")
        set(value) = setValue(KEY_SCHEME_URL, value)

    override var analyticsReportedNabuUser: String
        get() = getValue(ANALYTICS_REPORTED_NABU_USER_KEY, "")
        set(value) = setValue(ANALYTICS_REPORTED_NABU_USER_KEY, value)

    override var analyticsReportedWalletKey: String
        get() = getValue(ANALYTICS_REPORTED_WALLET_KEY, "")
        set(value) = setValue(ANALYTICS_REPORTED_WALLET_KEY, value)

    override var deeplinkUri: String
        get() = getValue(KEY_DEEP_LINK_URI, "")
        set(value) = setValue(KEY_DEEP_LINK_URI, value)

    override fun clearDeeplinkUri() = removeValue(KEY_DEEP_LINK_URI)

    override var hasSeenEducationalWalletMode: Boolean
        get() = getValue(HAS_SEEN_EDUCATIONAL_WALLET_MODE, false)
        set(value) = setValue(HAS_SEEN_EDUCATIONAL_WALLET_MODE, value)

    override var shouldHighlightModeSwitch: Boolean
        get() = getValue(SHOULD_HIGHLIGHT_MODE_SWITCH, true)
        set(value) = setValue(SHOULD_HIGHLIGHT_MODE_SWITCH, value)

    override var hasSeenCowboysFlow: Boolean
        get() = getValue(HAS_SEEN_COWBOYS_FLOW, false)
        set(value) = setValue(HAS_SEEN_COWBOYS_FLOW, value)

    override var hasCowboysReferralBeenDismissed: Boolean
        get() = getValue(COWBOYS_REFERRAL_CARD_DISMISSED, false)
        set(value) = setValue(COWBOYS_REFERRAL_CARD_DISMISSED, value)

    override var latestPricesMode: String?
        get() = getValue(PRICES_FILTER_MODE, "").takeIf { it.isNotEmpty() }
        set(value) {
            require(value != null)
            setValue(PRICES_FILTER_MODE, value)
        }

    override var dismissCount: Int
        get() = getValue(CAMPAIGN_DISMISS_COUNT, ExchangeCampaignPrefs.DEFAULT_DISMISS_COUNT)
        set(value) {
            setValue(CAMPAIGN_DISMISS_COUNT, value)
        }

    override var actionTaken: Boolean
        get() = getValue(CAMPAIGN_ACTION_TAKEN, ExchangeCampaignPrefs.DEFAULT_ACTION_TAKEN)
        set(value) {
            setValue(CAMPAIGN_ACTION_TAKEN, value)
        }

    // iterable announcements
    private fun parseAnnouncements(value: String): MutableList<String> = value.split(",").toMutableList()
    private fun formatAnnouncements(list: List<String>): String = list.joinToString(separator = ",")

    override fun markAsSeen(id: String) {
        getValue(ITERABLE_SEEN_ANNOUNCEMENTS, "")
            .run { parseAnnouncements(this) }
            .apply { add(id) }
            .also { setValue(ITERABLE_SEEN_ANNOUNCEMENTS, formatAnnouncements(it)) }
    }

    override fun markAsDeleted(id: String) {
        getValue(ITERABLE_DELETED_ANNOUNCEMENTS, "")
            .run { parseAnnouncements(this) }
            .apply { add(id) }
            .also { setValue(ITERABLE_DELETED_ANNOUNCEMENTS, formatAnnouncements(it)) }
    }

    override fun seenAnnouncements(): List<String> {
        return getValue(ITERABLE_SEEN_ANNOUNCEMENTS, "")
            .run { parseAnnouncements(this) }
    }

    override fun deletedAnnouncements(): List<String> {
        return getValue(ITERABLE_DELETED_ANNOUNCEMENTS, "")
            .run { parseAnnouncements(this) }
    }

    override fun updateSeenAnnouncements(ids: List<String>) {
        formatAnnouncements(ids)
            .also { setValue(ITERABLE_SEEN_ANNOUNCEMENTS, it) }
    }

    override fun syncDeletedAnnouncements(allAnnouncements: List<String>) {
        val local = deletedAnnouncements().toMutableList()
        local.removeIf { !allAnnouncements.contains(it) }
    }

    override var shouldMaskValues: Boolean
        get() = getValue(MASK_VALUES, false)
        set(value) = setValue(MASK_VALUES, value)

    companion object {
        const val KEY_PRE_IDV_FAILED = "pre_idv_check_failed"

        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val KEY_PIN_IDENTIFIER = "pin_kookup_key" // Historical misspelling. DO NOT FIX.

        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val KEY_SELECTED_FIAT = "ccurrency" // Historical misspelling, don't update

        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val KEY_PRE_IDV_DEVICE_ID = "pre_idv_device_id"

        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val KEY_LOGGED_IN = "logged_in"

        private const val KEY_SIMPLE_BUY_AMOUNT = "key_simple_buy_amount_"
        private const val KEY_SIMPLE_BUY_PAYMENT_ID = "key_simple_buy_payment_id"
        private const val KEY_SIMPLE_BUY_STATE = "key_simple_buy_state_2"
        private const val KEY_CARD_STATE = "key_card_state"
        private const val KEY_FIRST_TIME_BUYER = "key_first_time_buyer"
        private const val KEY_HAS_SEEN_RB_OPTIONS = "key_has_seen_rb_options"
        private const val KEY_SIMPLE_BUY_CURRENCY = "key_trading_urrency_currency"
        private const val KEY_HAS_COMPLETED_AT_LEAST_ONE_BUY = "has_completed_at_least_one_buy"
        private const val KEY_BUYS_COMPLETED_COUNT = "KEY_BUYS_COMPLETED_COUNT"

        private const val KEY_SUPPORTED_CARDS_STATE = "key_supported_cards"
        private const val KEY_BANK_LINKING = "KEY_BANK_LINKING"
        private const val KEY_ONE_TIME_TOKEN_PATH = "KEY_ONE_TIME_TOKEN_PATH"

        private const val KEY_CUSTODIAL_INTRO_SEEN = "key_custodial_balance_intro_seen"
        private const val KEY_PRIVATE_KEY_INTRO_SEEN = "key_private_key_intro_seen"
        private const val KEY_REWARDS_INTRO_SEEN = "key_rewards_intro_seen"
        private const val KEY_STAKING_INTRO_SEEN = "key_staking_intro_seen"
        private const val KEY_ACTIVE_REWARDS_INTRO_SEEN = "key_active_rewards_intro_seen"
        private const val KEY_REMAINING_SENDS_WITHOUT_BACKUP = "key_remaining_sends_without_backup"
        private const val MAX_ALLOWED_SENDS = 5
        private const val KEY_TAPPED_FAB = "key_tapped_fab"
        private const val KEY_SCREENSHOTS_ENABLED = "screenshots_enabled"
        private const val KEY_ONBOARDING_COMPLETE = "KEY_ONBOARDING_COMPLETE"

        private const val BACKUP_DATE_KEY = "BACKUP_DATE_KEY"
        private const val IS_WALLET_BACKUP_SKIPPED = "IS_WALLET_BACKUP_SKIPPED"
        private const val HAS_SEEN_DEFI_ONBOARDING = "HAS_SEEN_DEFI_ONBOARDING"
        private const val HAS_SEEN_EARN_PRODUCT_ONBOARDING = "HAS_SEEN_EARN_PRODUCT_ONBOARDING"
        private const val WALLET_FUNDED_KEY = "WALLET_FUNDED_KEY"
        private const val BITPAY_TRANSACTION_SUCCEEDED = "BITPAY_TRANSACTION_SUCCEEDED"
        private const val NETWORK_FEE_PRIORITY_KEY = "fee_type_key_"
        private const val SWAP_KYC_PROMO = "SWAP_KYC_PROMO"
        private const val KEY_NEWLY_CREATED_WALLET = "newly_created_wallet"
        private const val KEY_RESTORED_WALLET = "restored_wallet"
        private const val KEY_SHOW_TRADING_ON_PKW_MODE = "SHOW_TRADING_ON_PKW_MODE"
        private const val KEY_SHOW_PKW_ON_TRADING_MODE = "KEY_SHOW_PKW_ON_TRADING_MODE"

        private const val TWO_FA_SMS_RETRIES = "TWO_FA_SMS_RETRIES"
        private const val KEY_EMAIL = "KEY_EMAIL"
        private const val COUNTRY_SIGN_UP = "COUNTRY_SIGN_UP"
        private const val STATE_SIGNED_UP = "STATE_SIGNED_UP"
        private const val APP_CURRENT_VERSION_CODE = "APP_CURRENT_VERSION_CODE"
        private const val APP_INSTALLATION_VERSION_NAME = "APP_INSTALLATION_VERSION_NAME"

        // For QA:
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val KEY_IS_DEVICE_ID_RANDOMISED = "random_device_id"

        private const val KEY_FIREBASE_TOKEN = "firebase_token"
        private const val KEY_PUSH_NOTIFICATION_ENABLED = "push_notification_enabled"

        // Cloud backup keys
        private const val KEY_ENCRYPTED_GUID = "encrypted_guid"
        private const val KEY_ENCRYPTED_SHARED_KEY = "encrypted_shared_key"
        private const val KEY_CLOUD_BACKUP_ENABLED = "backup_enabled"
        private const val KEY_SECURE_CHANNEL_IDENTITY_KEY = "secure_channel_identity"
        private const val KEY_SECURE_CHANNEL_BROWSER_MAPPINGS = "secure_channel_browsers"

        // Onboarding
        private const val KEY_IS_LANDING_CTA_DISMISSED = "KEY_IS_LANDING_PAGE_DISMISSED"
        private const val KEY_IS_SEND_NETWORK_WARNING_DISMISSED =
            "KEY_IS_SEND_NETWORK_WARNING_DISMISSED"

        // Auth prefs
        // NOTE: for historical purposes, should be used as the cryptography cipher key
        private const val KEY_ENCRYPTED_PIN_CODE = "encrypted_pin_code"
        private const val KEY_FINGERPRINT_ENABLED = "fingerprint_enabled"

        private const val KEY_WALLET_GUID = "guid"
        private const val KEY_SHARED_KEY = "sharedKey"
        private const val KEY_ENCRYPTED_PASSWORD = "encrypted_password"
        private const val KEY_PIN_FAILS = "pin_fails"
        const val SESSION_ID = "session_id"

        // remote config prefs
        private const val CONFIG_STALE = "CONFIG_STALE"
        private const val BROKERAGE_ERROR_CODE = "BROKERAGE_ERROR_CODE"
        private const val BROKERAGE_ERRORS_ENABLED = "BROKERAGE_ERRORS_ENABLED"

        private const val KEY_DASHBOARD_ORDER = "dashboard_asset_order"

        // App Maintenance
        private const val APP_MAINTENANCE_IGNORE_REMOTE_CONFIG = "APP_MAINTENANCE_IGNORE_REMOTE_CONFIG"
        private const val APP_MAINTENANCE_DEBUG_OVERRIDE = "APP_MAINTENANCE_DEBUG_OVERRIDE"
        private const val APP_MAINTENANCE_DEBUG_JSON = "APP_MAINTENANCE_DEBUG_JSON"

        // App Rating
        private const val APP_RATING_COMPLETED = "APP_RATING_COMPLETED"
        private const val APP_RATING_PROMPT_DATE = "APP_RATING_PROMPT_DATE"

        // Nft Announcement
        private const val NFT_ANNOUNCEMENT_DISMISSED = "NFT_ANNOUNCEMENT_DISMISSED"
        private const val NFT_ANNOUNCEMENT_JOIN_WAITLIST = "NFT_ANNOUNCEMENT_JOIN_WAITLIST"

        // Referral
        private const val REFERRAL_ICON_CLICKED = "REFERRAL_ICON_CLICKED"
        private const val REFERRAL_SUCCESS_TITLE = "REFERRAL_SUCCESS_TITLE"
        private const val REFERRAL_SUCCESS_BODY = "REFERRAL_SUCCESS_BODY"

        // Local Settings
        private const val CHART_VIBRATION_ENABLED = "CHART_VIBRATION_ENABLED"
        private const val SMALL_BALANCES = "SMALL_BALANCES"

        // Session
        private const val KEY_EMAIL_VERIFIED = "code_verified"
        private const val KEY_SCHEME_URL = "scheme_url"
        private const val KEY_METADATA_URI = "metadata_uri"
        private const val ANALYTICS_REPORTED_NABU_USER_KEY = "analytics_reported_nabu_user_key"
        private const val ANALYTICS_REPORTED_WALLET_KEY = "analytics_reported_wallet_key"
        private const val KEY_DEEP_LINK_URI = "deeplink_uri"

        // Security
        private const val KEY_OVERLAY_TRUSTED = "overlay_trusted"
        private const val KEY_ROOT_WARNING_DISABLED = "disable_root_warning"

        // Educational Screens
        private const val HAS_SEEN_EDUCATIONAL_WALLET_MODE = "has_seen_educational_wallet_mode"
        private const val SHOULD_HIGHLIGHT_MODE_SWITCH = "should_hightlight_modes_switch"

        // Cowboys promo
        private const val HAS_SEEN_COWBOYS_FLOW = "has_seen_cowboys_flow"
        private const val COWBOYS_REFERRAL_CARD_DISMISSED = "referral_card_dismissed"

        // multiapp assets

        // Exchange Campaign
        private const val CAMPAIGN_DISMISS_COUNT = "campaign_show_count"
        private const val CAMPAIGN_ACTION_TAKEN = "campaign_action_taken"

        private const val PRICES_FILTER_MODE = "prices_filter_mode"

        private const val WALLET_MODE_LEGACY_KEY = "WALLET_MODE"
        private const val WALLET_MODE_KEY = "WALLET_MODE_UPDATED_KEY"
        private const val USER_DEFAULTED_TO_PKW = "USER_DEFAULTED_TO_PKW"
        private const val USER_COUNTRY = "USER_COUNTRY"
        private const val SHOULD_SHOW_SMALL_BALANCES = "should_show_small_balances"
        private const val DEX_INTRO_SHOWN = "dex_intro_shown"
        private const val DEX_LAST_SELECTED_SLIPPAGE_INDEX = "LAST_SELECTED_SLIPPAGE_INDEX"
        private const val DEX_LAST_SELECTED_DESTINATION_TICKER = "DEX_LAST_SELECTED_DESTINATION_TICKER"
        private const val DEX_SELECTED_CHAIN_ID = "DEX_SELECTED_CHAIN_ID"
        private const val ALLOWANCE_APPROVED_BUT_PENDING_TOKENS = "DEX_ALLOWANCE_APPROVED_BUT_PENDING_TOKENS"

        // iterable announcements
        private const val ITERABLE_SEEN_ANNOUNCEMENTS = "ITERABLE_SEEN_ANNOUNCEMENTS"
        private const val ITERABLE_DELETED_ANNOUNCEMENTS = "ITERABLE_DELETED_ANNOUNCEMENTS"

        // masked values
        private const val MASK_VALUES = "MASK_VALUES"
    }

    override val legacyWalletMode: String
        get() = getValue(WALLET_MODE_LEGACY_KEY, "")

    override var currentWalletMode: String
        get() = getValue(WALLET_MODE_KEY, "")
        set(value) {
            setValue(WALLET_MODE_KEY, value)
        }

    override var userDefaultedToPKW: Boolean
        get() = getValue(USER_DEFAULTED_TO_PKW, false)
        set(value) {
            setValue(USER_DEFAULTED_TO_PKW, value)
        }
    override var showSmallBalances: Boolean
        get() = getValue(SHOULD_SHOW_SMALL_BALANCES, true)
        set(value) {
            setValue(SHOULD_SHOW_SMALL_BALANCES, value)
        }

    override val dexIntroShown: Boolean
        get() = getValue(DEX_INTRO_SHOWN, false)

    override fun markDexIntroAsSeen() {
        setValue(DEX_INTRO_SHOWN, true)
    }

    override var selectedSlippageIndex: Int
        get() =
            getValue(DEX_LAST_SELECTED_SLIPPAGE_INDEX, -1)
        set(value) {
            setValue(DEX_LAST_SELECTED_SLIPPAGE_INDEX, value)
        }

    override var selectedDestinationCurrencyTicker: String
        get() =
            getValue(DEX_LAST_SELECTED_DESTINATION_TICKER, "")
        set(value) {
            setValue(DEX_LAST_SELECTED_DESTINATION_TICKER, value)
        }
    override var country: String
        get() =
            getValue(USER_COUNTRY, "")
        set(value) {
            setValue(USER_COUNTRY, value)
        }
    override var selectedChainId: Int
        get() = getValue(DEX_SELECTED_CHAIN_ID, ETH_CHAIN_ID)
        set(value) {
            setValue(DEX_SELECTED_CHAIN_ID, value)
        }

    override var allowanceApprovedButPendingTokens: Set<String>
        get() = getValue(ALLOWANCE_APPROVED_BUT_PENDING_TOKENS, "").split(",").toSet()
        set(value) {
            setValue(ALLOWANCE_APPROVED_BUT_PENDING_TOKENS, value.joinToString(separator = ","))
        }
}

fun BrowserIdentity.pubKeyHash() = Sha256Hash.of(Hex.decode(this.pubkey)).toString()
