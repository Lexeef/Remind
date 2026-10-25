package com.example.napominalka

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(ctx: Context, intent: Intent) {
        val id = intent.getLongExtra("id", 0L)
        val text = intent.getStringExtra("text") ?: "Напоминание"

        val nm = ctx.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(CHANNEL, "Напоминания", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Уведомления о напоминаниях"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 400, 200, 400)
        }
        nm.createNotificationChannel(channel)

        val open = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val n = Notification.Builder(ctx, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Напоминалка")
            .setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setCategory(Notification.CATEGORY_REMINDER)
            .build()

        nm.notify(id.toInt(), n)
        ReminderStore.markDone(ctx, id)
    }

    companion object {
        const val CHANNEL = "reminders"
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            ReminderScheduler.rescheduleAll(ctx)
        }
    }
}
