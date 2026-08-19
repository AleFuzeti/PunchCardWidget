package com.example.punchcardwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

const val ACTION_TOGGLE = "com.example.punchcardwidget.ACTION_TOGGLE"
const val EXTRA_ITEM_ID = "extra_item_id"

class PunchCardWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == ACTION_TOGGLE) {
            val appWidgetId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
            val itemId = intent.getIntExtra(EXTRA_ITEM_ID, -1)
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID && itemId != -1) {
                PunchCardStore.toggle(context, appWidgetId, itemId)

                val appWidgetManager = AppWidgetManager.getInstance(context)
                // avisa o RemoteViewsFactory que os dados mudaram, força reler
                appWidgetManager.notifyAppWidgetViewDataChanged(
                    appWidgetId, R.id.punch_card_grid
                )
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        appWidgetIds.forEach { PunchCardStore.clear(context, it) }
    }

    companion object {
        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.punch_card_widget)

            // intent que aponta pro RemoteViewsService que fornece os itens do GridView
            val serviceIntent = Intent(context, PunchCardRemoteViewsService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = android.net.Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.punch_card_grid, serviceIntent)
            views.setEmptyView(R.id.punch_card_grid, R.id.empty_view)

            // template de clique: cada item do grid preenche o appWidgetId/itemId
            // via fillInIntent (ver PunchCardRemoteViewsService)
            val toggleIntent = Intent(context, PunchCardWidgetProvider::class.java).apply {
                action = ACTION_TOGGLE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = android.net.Uri.parse("punchcard://widget/$appWidgetId")
            }
            val togglePendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId,
                toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            views.setPendingIntentTemplate(R.id.punch_card_grid, togglePendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
