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
 * Quick Notes Widget for instant note capture
 * Part of Medium Priority Features Phase 2: Advanced Note-Taking
 * 
 * Provides a home screen widget for quick note-taking without opening the full app.
 */
class QuickNotesWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // Widget first instance created
    }

    override fun onDisabled(context: Context) {
        // All widget instances removed
    }

    companion object {
        const val ACTION_QUICK_NOTE = "com.officesuite.app.ACTION_QUICK_NOTE"
        const val ACTION_VOICE_NOTE = "com.officesuite.app.ACTION_VOICE_NOTE"
        const val ACTION_CHECKLIST = "com.officesuite.app.ACTION_CHECKLIST"
        const val ACTION_PHOTO_NOTE = "com.officesuite.app.ACTION_PHOTO_NOTE"

        internal fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_quick_notes)

            // Quick Note button - Opens markdown editor for quick text note
            val quickNoteIntent = Intent(context, MainActivity::class.java).apply {
                action = ACTION_QUICK_NOTE
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val quickNotePendingIntent = PendingIntent.getActivity(
                context,
                10,
                quickNoteIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.button_quick_note, quickNotePendingIntent)

            // Checklist button - Opens new checklist
            val checklistIntent = Intent(context, MainActivity::class.java).apply {
                action = ACTION_CHECKLIST
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val checklistPendingIntent = PendingIntent.getActivity(
                context,
                11,
                checklistIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.button_checklist, checklistPendingIntent)

            // Photo Note button - Opens camera for scan
            val photoNoteIntent = Intent(context, MainActivity::class.java).apply {
                action = ACTION_PHOTO_NOTE
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val photoNotePendingIntent = PendingIntent.getActivity(
                context,
                12,
                photoNoteIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.button_photo_note, photoNotePendingIntent)

            // Update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
