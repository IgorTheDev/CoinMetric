package com.coinmetric.ui

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.coinmetric.MainActivity

class SyncNotificationHelper(private val context: Context) {
    fun notifySyncSuccess(message: String = "Синхронизация с облаком завершена успешно") {
        showNotification(
            id = "sync_success".hashCode(),
            title = "Синхронизация данных",
            text = message,
            priority = NotificationCompat.PRIORITY_LOW,
        )
    }

    fun notifySyncError(message: String = "Ошибка синхронизации данных") {
        showNotification(
            id = "sync_error".hashCode(),
            title = "Ошибка синхронизации",
            text = message,
            priority = NotificationCompat.PRIORITY_HIGH,
        )
    }

    private fun showNotification(id: Int, title: String, text: String, priority: Int) {
        ensureChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val openAnalyticsIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_START_ROUTE, MainActivity.ROUTE_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            id,
            openAnalyticsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(priority)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(id, notification)
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Синхронизация данных",
                NotificationManager.IMPORTANCE_DEFAULT,
            )
            channel.description = "Уведомления о синхронизации данных с облаком"
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "coinmetric_sync_channel"
    }
}