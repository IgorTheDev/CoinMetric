package com.coinmetric.ui

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class LimitNotificationHelper(private val context: Context) {
    fun notifyLimitExceeded(categoryName: String, spent: Double, limit: Double) {
        showNotification(
            id = ("exceeded_$categoryName").hashCode(),
            title = "Превышен лимит категории",
            text = "$categoryName: ${"%.2f".format(spent)} ₽ из ${"%.2f".format(limit)} ₽",
            priority = NotificationCompat.PRIORITY_HIGH,
        )
    }

    fun notifyLimitAlmostReached(categoryName: String, spent: Double, limit: Double) {
        showNotification(
            id = ("almost_$categoryName").hashCode(),
            title = "Лимит почти исчерпан",
            text = "$categoryName: ${"%.2f".format(spent)} ₽ из ${"%.2f".format(limit)} ₽",
            priority = NotificationCompat.PRIORITY_DEFAULT,
        )
    }

    private fun showNotification(id: Int, title: String, text: String, priority: Int) {
        ensureChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(priority)
            .build()

        NotificationManagerCompat.from(context).notify(id, notification)
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Лимиты бюджета",
                NotificationManager.IMPORTANCE_HIGH,
            )
            channel.description = "Уведомления о приближении и превышении лимитов категорий"
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "coinmetric_limit_alerts"
    }
}
