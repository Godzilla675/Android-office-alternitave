package com.officesuite.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.officesuite.app.MainActivity
import com.officesuite.app.R

/**
 * Widget provider for quick actions widget.
 * Provides quick access to common app actions from the home screen.
 */
class QuickActionsWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Update each widget instance
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // Widget first created
    }

    override fun onDisabled(context: Context) {
        // All widget instances removed
    }

    companion object {
        internal fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_quick_actions)

            // Set up Open File button
            val openIntent = Intent(context, MainActivity::class.java).apply {
                action = ACTION_OPEN_FILE
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openPendingIntent = PendingIntent.getActivity(
                context,
                0,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.button_open, openPendingIntent)

            // Set up Scan button
            val scanIntent = Intent(context, MainActivity::class.java).apply {
                action = ACTION_SCAN
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val scanPendingIntent = PendingIntent.getActivity(
                context,
                1,
                scanIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.button_scan, scanPendingIntent)

            // Set up Convert button
            val convertIntent = Intent(context, MainActivity::class.java).apply {
                action = ACTION_CONVERT
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val convertPendingIntent = PendingIntent.getActivity(
                context,
                2,
                convertIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.button_convert, convertPendingIntent)

            // Set up Create button
            val createIntent = Intent(context, MainActivity::class.java).apply {
                action = ACTION_CREATE_NEW
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val createPendingIntent = PendingIntent.getActivity(
                context,
                3,
                createIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.button_create, createPendingIntent)

            // Update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        const val ACTION_OPEN_FILE = "com.officesuite.app.ACTION_OPEN_FILE"
        const val ACTION_SCAN = "com.officesuite.app.ACTION_SCAN"
        const val ACTION_CONVERT = "com.officesuite.app.ACTION_CONVERT"
        const val ACTION_CREATE_NEW = "com.officesuite.app.ACTION_CREATE_NEW"
    }
}
