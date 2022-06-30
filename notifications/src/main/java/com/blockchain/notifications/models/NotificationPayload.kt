package com.blockchain.notifications.models

import com.blockchain.notifications.models.NotificationDataConstants.DATA
import com.blockchain.notifications.models.NotificationDataConstants.DATA_ADDRESS
import com.blockchain.notifications.models.NotificationDataConstants.DATA_ID
import com.blockchain.notifications.models.NotificationDataConstants.DATA_REFERRAL_SUCCESS_BODY
import com.blockchain.notifications.models.NotificationDataConstants.DATA_REFERRAL_SUCCESS_TITLE
import com.blockchain.notifications.models.NotificationDataConstants.DATA_TYPE
import com.blockchain.notifications.models.NotificationDataConstants.DATA_URL
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber

class NotificationPayload(map: Map<String, String?>) {

    var title: String? = null

    var body: String? = null

    private var data: NotificationData? = null

    val payload: Map<String, String?> = map

    val address: String?
        get() = data?.address

    val type: NotificationType?
        get() =
            if (data != null)
                data!!.type
            else if (payload[TYPE] != null)
                NotificationType.fromString(payload[TYPE])
            else null

    val mdid: String?
        get() = data?.mdid

    val deeplinkURL: String?
        get() = data?.deeplinkURL

    val referralSuccessTitle: String?
        get() = data?.referralSuccessTitle

    val referralSuccessBody: String?
        get() = data?.referralSuccessBody

    init {
        if (map.containsKey(KEY_TITLE)) {
            title = map[KEY_TITLE]
        }
        if (map.containsKey(KEY_BODY)) {
            body = map[KEY_BODY]
        }
        if (map.containsKey(DATA)) {
            data = NotificationData(map[DATA])
        }
    }

    companion object {
        private const val KEY_TITLE = "title"
        private const val KEY_BODY = "body"
        private const val TYPE = "type"

        const val PUB_KEY_HASH = "fcm_data_pubkeyhash"
        const val DATA_MESSAGE = "fcm_data_message"
        const val ORIGIN_IP = "origin_ip"
        const val ORIGIN_COUNTRY = "origin_country"
        const val ORIGIN_BROWSER = "origin_browser"
    }

    private class NotificationData constructor(data: String?) {
        var mdid: String? = null
        var type: NotificationType? = null
        var address: String? = null
        var deeplinkURL: String? = null
        var referralSuccessTitle: String? = null
        var referralSuccessBody: String? = null

        init {
            try {
                val jsonObject = JSONObject(data)
                if (jsonObject.has(DATA_ID)) {
                    mdid = jsonObject.getString(DATA_ID)
                }
                if (jsonObject.has(DATA_TYPE)) {
                    type = NotificationType.fromString(jsonObject.getString(DATA_TYPE))
                }
                if (jsonObject.has(DATA_ADDRESS)) {
                    address = jsonObject.getString(DATA_ADDRESS)
                }
                if (jsonObject.has(DATA_URL)) {
                    deeplinkURL = jsonObject.getString(DATA_URL)
                }
                if (jsonObject.has(DATA_REFERRAL_SUCCESS_TITLE)) {
                    referralSuccessTitle = jsonObject.getString(DATA_REFERRAL_SUCCESS_TITLE)
                }
                if (jsonObject.has(DATA_REFERRAL_SUCCESS_BODY)) {
                    referralSuccessBody = jsonObject.getString(DATA_REFERRAL_SUCCESS_BODY)
                }
            } catch (e: JSONException) {
                Timber.e(e)
            }
        }
    }

    enum class NotificationType(val type: String) {
        PAYMENT("payment"),
        SECURE_CHANNEL_MESSAGE("secure_channel");

        companion object {
            fun fromString(string: String?): NotificationType? {
                if (string != null) {
                    for (notificationType in values()) {
                        if (notificationType.type.equals(string, ignoreCase = true)) {
                            return notificationType
                        }
                    }
                }
                return null
            }
        }
    }
}
