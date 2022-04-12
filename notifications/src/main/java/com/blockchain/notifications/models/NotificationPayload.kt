package com.blockchain.notifications.models

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
            else if (payload["type"] != null)
                NotificationType.fromString(payload["type"])
            else null

    val mdid: String?
        get() = data?.mdid

    val deeplinkURL: String?
        get() = data?.deeplinkURL

    init {
        if (map.containsKey("title")) {
            title = map["title"]
        }
        if (map.containsKey("body")) {
            body = map["body"]
        }
        if (map.containsKey("data")) {
            data = NotificationData(map["data"])
        }
    }

    companion object {
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

        init {
            try {
                val jsonObject = JSONObject(data)
                if (jsonObject.has("id")) {
                    mdid = jsonObject.getString("id")
                }
                if (jsonObject.has("type")) {
                    type = NotificationType.fromString(jsonObject.getString("type"))
                }
                if (jsonObject.has("address")) {
                    address = jsonObject.getString("address")
                }
                if (jsonObject.has("url")) {
                    deeplinkURL = jsonObject.getString("url")
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
