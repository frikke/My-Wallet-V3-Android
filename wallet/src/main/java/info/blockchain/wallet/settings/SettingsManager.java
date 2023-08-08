package info.blockchain.wallet.settings;

import info.blockchain.wallet.api.WalletApi;
import info.blockchain.wallet.api.data.Settings;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import okhttp3.ResponseBody;
import retrofit2.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.abi.datatypes.Bool;

public class SettingsManager {

    private static final Logger log = LoggerFactory.getLogger(SettingsManager.class);

    //API methods
    public static final String METHOD_GET_INFO = "get-info";
    public static final String METHOD_VERIFY_EMAIL = "verify-email";
    public static final String METHOD_RESEND_VERIFY_EMAIL = "resend-verify-email";
    public static final String METHOD_VERIFY_SMS = "verify-sms";
    public static final String METHOD_UPDATE_NOTIFICATION_TYPE = "update-notifications-type";
    public static final String METHOD_UPDATE_NOTIFICATION_ON = "update-notifications-on";
    public static final String METHOD_UPDATE_SMS = "update-sms";
    public static final String METHOD_UPDATE_EMAIL = "update-email";
    public static final String METHOD_UPDATE_BTC_CURRENCY = "update-btc-currency";
    public static final String METHOD_UPDATE_CURRENCY = "update-currency";
    public static final String METHOD_UPDATE_PASSWORD_HINT_1 = "update-password-hint1";
    public static final String METHOD_UPDATE_AUTH_TYPE = "update-auth-type";
    public static final String METHOD_UPDATE_BLOCK_TOR_IPS = "update-block-tor-ips";
    public static final String METHOD_UPDATE_LAST_TX_TIME = "update-last-tx-time";

    //Unused API methods
    /*
    public static final String METHOD_TOGGLE_SAVE_2FA = "update-never-save-auth-type";
    public static final String METHOD_TOGGLE_SAVE_2FA = "update-never-save-auth-type";
    public static final String METHOD_UPDATE_PASSWORD_HINT_2 = "update-password-hint2";
    public static final String METHOD_UPDATE_IP_LOCK = "update-ip-lock";
    public static final String METHOD_UPDATE_IP_LOCK_ON = "update-ip-lock-on";
    public static final String METHOD_UPDATE_LANGUAGE = "update-language";
    public static final String METHOD_UPDATE_LOGGING_LEVEL = "update-logging-level";
    */

    // Notification Settings
    public static final int NOTIFICATION_ON = 2;
    public static final int NOTIFICATION_OFF = 0;

    public static final int NOTIFICATION_TYPE_NONE = 0;
    public static final int NOTIFICATION_TYPE_EMAIL = 1;
    public static final int NOTIFICATION_TYPE_SMS = 32;
    public static final int NOTIFICATION_TYPE_ALL = 33;

    // Auth Settings
    public static final int AUTH_TYPE_OFF = 0;
    public static final int AUTH_TYPE_YUBI_KEY = 1;
    public static final int AUTH_TYPE_EMAIL = 2;
    public static final int AUTH_TYPE_GOOGLE_AUTHENTICATOR = 4;
    public static final int AUTH_TYPE_SMS = 5;

    private String guid;
    private String sharedKey;

    private final WalletApi walletApi;

    public SettingsManager(WalletApi walletApi) {
        this.walletApi = walletApi;
    }

    public void initSettings(String guid, String sharedKey) {
        this.guid      = guid;
        this.sharedKey = sharedKey;
    }

    public Observable<Settings> getInfo() {
        log.info("Fetching settings details");
        return walletApi.fetchSettings(METHOD_GET_INFO, guid, sharedKey);
    }

    public Observable<ResponseBody> updateSetting(String method, String payload) {
        log.info("Update settings");
        return walletApi.updateSettings(method, guid, sharedKey, payload, null);
    }

    public Single<Response<ResponseBody>> updateSetting(String method, String payload, Boolean forceJson) {
        log.info("Update settings");
        return walletApi.updateSettings(method, guid, sharedKey, payload, null, forceJson);
    }

    public Observable<ResponseBody> updateSetting(String method, String payload, String context) {
        log.info("Update settings");
        return walletApi.updateSettings(method, guid, sharedKey, payload, context);
    }

    public Observable<ResponseBody> updateSetting(String method, int payload) {
        log.info("Update settings");
        return walletApi.updateSettings(method, guid, sharedKey, payload + "", null);
    }
}