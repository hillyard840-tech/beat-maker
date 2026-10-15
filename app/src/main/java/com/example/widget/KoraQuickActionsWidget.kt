package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R

class KoraQuickActionsWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.kora_widget_layout)

        // PendingIntent helper
        fun createActionPendingIntent(action: String, requestCode: Int): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(MainActivity.EXTRA_ACTION, action)
            }
            return PendingIntent.getActivity(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        // Attach PendingIntents to widget buttons
        views.setOnClickPendingIntent(R.id.btn_widget_voice, createActionPendingIntent("voice_mode", 101))
        views.setOnClickPendingIntent(R.id.btn_widget_camera, createActionPendingIntent("camera_diagnostic", 102))
        views.setOnClickPendingIntent(R.id.btn_widget_research, createActionPendingIntent("deep_research", 103))
        views.setOnClickPendingIntent(R.id.btn_widget_new_chat, createActionPendingIntent("new_chat", 104))

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
