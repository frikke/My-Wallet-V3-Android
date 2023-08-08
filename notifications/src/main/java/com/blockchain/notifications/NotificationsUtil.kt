package com.blockchain.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.net.Uri
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.blockchain.analytics.Analytics
import com.blockchain.analytics.NotificationReceived
import com.blockchain.notifications.analytics.NotificationAnalyticsEvents
import timber.log.Timber

class NotificationsUtil(
    private val context: Context,
    private val notificationManager: NotificationManager,
    private val analytics: Analytics
) {

    fun triggerNotification(
        title: String?,
        marquee: String?,
        text: String?,
        @DrawableRes icon: Int = R.drawable.ic_notification,
        pendingIntent: PendingIntent,
        id: Int,
        @StringRes appName: Int,
        @ColorRes colorRes: Int,
        channelId: String? = null,
        source: String
    ) {
        if (title.isNullOrEmpty() || title.isBlank() || text.isNullOrEmpty() || text.isBlank()) {
            Timber.e("Empty Notification: title and/or body were not passed!")
            analytics.logEvent(
                NotificationAnalyticsEvents.MissingNotificationData(
                    source = source,
                    title = title,
                    text = text,
                    marquee = marquee
                )
            )
        } else {
            val builder = NotificationCompat.Builder(
                context,
                channelId ?: NOTIFICATION_CHANNEL_ID
            ).setSmallIcon(icon)
                .setColor(ContextCompat.getColor(context, colorRes))
                .setContentTitle(title)
                .setContentIntent(pendingIntent)
                .setWhen(System.currentTimeMillis())
                .setSound(Uri.parse("android.resource://${context.packageName}/${R.raw.beep}"))
                .setTicker(marquee)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVibrate(longArrayOf(100))
                .setDefaults(Notification.DEFAULT_LIGHTS)
                .setContentText(text)

            // TODO: Maybe pass in specific channel names here, such as "payments" and "contacts"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val notificationChannel = NotificationChannel(
                channelId ?: NOTIFICATION_CHANNEL_ID,
                context.getString(appName),
                importance
            ).apply {
                enableLights(true)
                lightColor = ContextCompat.getColor(context, colorRes)
                enableVibration(true)
                vibrationPattern = longArrayOf(100)
            }
            notificationManager.createNotificationChannel(notificationChannel)

            notificationManager.notify(id, builder.build())
            analytics.logEvent(NotificationReceived)
        }
    }

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "group_01"
        const val ID_BACKGROUND_NOTIFICATION = 1337
        const val ID_FOREGROUND_NOTIFICATION = 1338
        const val ID_BACKGROUND_NOTIFICATION_2FA = 1339
    }
}
