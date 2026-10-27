package uk.co.milncj.quick.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import uk.co.milncj.quick.MainActivity
import uk.co.milncj.quick.R
import uk.co.milncj.quick.data.CaptureStatus
import uk.co.milncj.quick.data.QuickDatabase

class QuickWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val waitingCount = runCatching { QuickDatabase(context).list(CaptureStatus.WAITING).size }.getOrDefault(0)
        appWidgetIds.forEach { appWidgetId ->
            val views = RemoteViews(context.packageName, R.layout.widget_quick)
            views.setTextViewText(
                R.id.widget_waiting,
                if (waitingCount == 0) "Waiting" else "$waitingCount Waiting"
            )
            views.setOnClickPendingIntent(
                R.id.widget_capture,
                launchPendingIntent(context, MainActivity.ACTION_OPEN_CAPTURE, 1001)
            )
            views.setOnClickPendingIntent(
                R.id.widget_voice,
                launchPendingIntent(context, MainActivity.ACTION_OPEN_VOICE, 1002)
            )
            views.setOnClickPendingIntent(
                R.id.widget_photo,
                launchPendingIntent(context, MainActivity.ACTION_OPEN_PHOTO, 1003)
            )
            views.setOnClickPendingIntent(
                R.id.widget_waiting,
                launchPendingIntent(context, MainActivity.ACTION_OPEN_WAITING, 1004)
            )
            views.setOnClickPendingIntent(
                R.id.widget_title,
                launchPendingIntent(context, MainActivity.ACTION_OPEN_CAPTURE, 1005)
            )
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    private fun launchPendingIntent(
        context: Context,
        action: String,
        requestCode: Int
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            this.action = action
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, QuickWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isNotEmpty()) QuickWidgetProvider().onUpdate(context, manager, ids)
        }
    }
}
