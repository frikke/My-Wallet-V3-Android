package com.blockchain.preferences

interface NotificationPrefs {
    suspend fun arePushNotificationsEnabled(): Boolean
    suspend fun setPushNotificationsEnabled(pushNotificationsEnabled: Boolean)

    suspend fun getFirebaseToken(): String
    suspend fun setFirebaseToken(firebaseToken: String)
}
