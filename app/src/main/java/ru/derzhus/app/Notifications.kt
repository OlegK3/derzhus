package ru.derzhus.app

import android.Manifest
import android.annotation.SuppressLint
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
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.Calendar
import java.util.concurrent.TimeUnit

object Notifier {
    private const val CHANNEL = "daily"

    fun ensureChannel(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Ежедневная поддержка", NotificationManager.IMPORTANCE_DEFAULT)
        )
    }

    @SuppressLint("MissingPermission")
    fun post(ctx: Context, id: Int, title: String, text: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val pi = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(ctx).notify(id, n)
    }
}

object Scheduler {
    /** Раз в сутки в выбранный час (по местному времени); выключается в настройках. */
    fun apply(ctx: Context, s: Settings) {
        val wm = WorkManager.getInstance(ctx)
        if (!s.remindersOn) {
            wm.cancelUniqueWork("morning")
            return
        }
        val now = System.currentTimeMillis()
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, s.reminderHour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now) add(Calendar.DAY_OF_MONTH, 1)
        }
        val req = PeriodicWorkRequestBuilder<MorningWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(next.timeInMillis - now, TimeUnit.MILLISECONDS)
            .build()
        wm.enqueueUniquePeriodicWork("morning", ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE, req)
    }
}

class MorningWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val now = System.currentTimeMillis()
        Notifier.ensureChannel(ctx)

        Store.load(ctx).forEachIndexed { index, h ->
            val d = Calc.fullDays(h, now)
            val title = if (d == 0L) "${h.title}: первый день" else "${h.title}: ${Calc.daysText(d)}"
            // Награда, которую получили именно сегодня
            val fresh = Game.dayAwards.firstOrNull { it.threshold == d && d > 0 }
            val praise = Praise.daily(h, now)
            val text = if (fresh != null) "Новая награда: ${fresh.title}. $praise" else praise
            Notifier.post(ctx, index + 1, title, text)
        }

        DerzhusWidget().updateAll(ctx)
        return Result.success()
    }
}
