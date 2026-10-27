package uk.co.milncj.quick.reminder

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
import androidx.work.Worker
import androidx.work.WorkerParameters
import uk.co.milncj.quick.MainActivity
import uk.co.milncj.quick.R
import uk.co.milncj.quick.data.QuickRepository
import java.util.concurrent.TimeUnit

class ReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        val settings = ReminderPreferences(applicationContext).load()
        if (!settings.enabled) return Result.success()

        val repository = QuickRepository(applicationContext)
        repository.purgeExpiredDone()
        val waiting = repository.waiting()
        if (waiting.isEmpty()) return Result.success()

        if (!canNotify()) return Result.success()
        createChannel()

        val oldCount = if (settings.oldItemDays > 0) {
            val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(settings.oldItemDays.toLong())
            waiting.count { it.createdAt <= cutoff }
        } else 0

        val detail = if (oldCount > 0) {
            "$oldCount ${if (oldCount == 1) "item has" else "items have"} been waiting more than ${settings.oldItemDays} days."
        } else {
            "Tap to review what is waiting."
        }

        val openWaiting = Intent(applicationContext, MainActivity::class.java).apply {
            action = MainActivity.ACTION_OPEN_WAITING
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            4001,
            openWaiting,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_quick_launcher)
            .setContentTitle("Quick — ${waiting.size} waiting")
            .setContentText(detail)
            .setStyle(NotificationCompat.BigTextStyle().bigText(detail))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
        return Result.success()
    }

    private fun canNotify(): Boolean {
        if (!NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()) return false
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Waiting reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "A gentle daily reminder when Quick still has items waiting."
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "quick_waiting_reminders"
        private const val NOTIFICATION_ID = 4001
    }
}
