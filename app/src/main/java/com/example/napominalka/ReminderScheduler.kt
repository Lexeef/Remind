package com.example.napominalka

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object ReminderScheduler {

    fun schedule(ctx: Context, r: Reminder) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        val pi = pending(ctx, r)
        val exactAllowed = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
        if (exactAllowed) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, r.time, pi)
        } else {
            // без разрешения на точные будильники — система может сдвинуть на несколько минут
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, r.time, pi)
        }
    }

    fun cancel(ctx: Context, r: Reminder) {
        ctx.getSystemService(AlarmManager::class.java).cancel(pending(ctx, r))
    }

    fun rescheduleAll(ctx: Context) {
        val now = System.currentTimeMillis()
        ReminderStore.load(ctx)
            .filter { !it.done && it.time > now }
            .forEach { schedule(ctx, it) }
    }

    private fun pending(ctx: Context, r: Reminder): PendingIntent {
        val i = Intent(ctx, ReminderReceiver::class.java)
            .putExtra("id", r.id)
            .putExtra("text", r.text)
        return PendingIntent.getBroadcast(
            ctx, r.id.toInt(), i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
