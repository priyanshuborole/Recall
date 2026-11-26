package com.zerotwodev.recall

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.zerotwodev.recall.data.reminderMinuteOrNull
import com.zerotwodev.recall.domain.NotificationPermission
import com.zerotwodev.recall.domain.ReminderScheduler
import kotlinx.coroutines.CompletableDeferred
import java.util.Calendar

private const val CHANNEL_ID = "nightly_question"
private const val NOTIFICATION_ID = 1
private const val ACTION_REMIND = "com.zerotwodev.recall.action.REMIND"
private const val ACTION_NOT_TONIGHT = "com.zerotwodev.recall.action.NOT_TONIGHT"
const val REMINDER_QUESTION = "What’s one thing from today worth remembering?"

/** Uses an inexact alarm, so no exact-alarm permission is needed. It may arrive a few minutes late. */
internal class AndroidReminderScheduler(private val context: Context) : ReminderScheduler {
    override fun schedule(minuteOfDay: Int) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTrigger(minuteOfDay), alarmIntent(context))
    }

    override fun cancel() {
        context.getSystemService(AlarmManager::class.java).cancel(alarmIntent(context))
    }
}

/** The next time [minuteOfDay] comes round in the phone's time zone, at least a minute from now. */
internal fun nextTrigger(minuteOfDay: Int, now: Calendar = Calendar.getInstance()): Long {
    val next = (now.clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, minuteOfDay / 60)
        set(Calendar.MINUTE, minuteOfDay % 60)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    if (next.timeInMillis <= now.timeInMillis + 60_000) next.add(Calendar.DAY_OF_YEAR, 1)
    return next.timeInMillis
}

private fun alarmIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
    context,
    0,
    Intent(context, ReminderReceiver::class.java).setAction(ACTION_REMIND),
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
)

/** Shows the question, then books tomorrow's. Also restores the alarm after a restart or clock change. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val minute = readSettings(context).reminderMinuteOrNull()
        when (intent.action) {
            ACTION_NOT_TONIGHT -> {
                context.getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
                return
            }
            ACTION_REMIND -> if (minute != null) showQuestion(context)
        }
        val scheduler = AndroidReminderScheduler(context)
        if (minute == null) scheduler.cancel() else scheduler.schedule(minute)
    }

    private fun showQuestion(context: Context) {
        if (!AndroidNotificationPermission(context).isGranted()) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Nightly question", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "One gentle question at the time you chose"
            }
        )
        val open = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val openApp = PendingIntent.getActivity(context, 1, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notTonight = PendingIntent.getBroadcast(
            context,
            2,
            Intent(context, ReminderReceiver::class.java).setAction(ACTION_NOT_TONIGHT),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val icon = context.resources.getIdentifier("ic_notification", "drawable", context.packageName)
            .takeIf { it != 0 } ?: context.applicationInfo.icon
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(icon)
            .setContentTitle(REMINDER_QUESTION)
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .addAction(Notification.Action.Builder(null, "Hold to speak", openApp).build())
            .addAction(Notification.Action.Builder(null, "Not tonight", notTonight).build())
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }
}

/** The activity connects its permission launcher here. Before Android 13 no prompt is needed. */
class AndroidNotificationPermission(private val context: Context) : NotificationPermission {
    var launcher: (() -> Unit)? = null
    private var pending: CompletableDeferred<Boolean>? = null

    override fun isGranted(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    override suspend fun request(): Boolean {
        if (isGranted()) return true
        val launch = launcher ?: return false
        val result = CompletableDeferred<Boolean>()
        pending = result
        launch()
        return result.await()
    }

    fun onResult(granted: Boolean) {
        pending?.complete(granted)
        pending = null
    }
}
