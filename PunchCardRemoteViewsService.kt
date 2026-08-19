package com.example.punchcardwidget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService

class PunchCardRemoteViewsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        return PunchCardViewFactory(applicationContext, appWidgetId)
    }
}

class PunchCardViewFactory(
    private val context: Context,
    private val appWidgetId: Int
) : RemoteViewsService.RemoteViewsFactory {

    private var items: List<PunchCardItem> = emptyList()

    override fun onCreate() {
        reload()
    }

    override fun onDataSetChanged() {
        // chamado depois de notifyAppWidgetViewDataChanged(...)
        reload()
    }

    private fun reload() {
        items = PunchCardStore.load(context, appWidgetId)
        if (items.isEmpty()) {
            items = PunchCardStore.defaultItems()
            PunchCardStore.save(context, appWidgetId, items)
        }
    }

    override fun onDestroy() {}

    override fun getCount(): Int = items.size

    override fun getViewAt(position: Int): RemoteViews {
        val item = items[position]
        val views = RemoteViews(context.packageName, R.layout.punch_card_item)

        val iconName = if (item.checked) item.iconResChecked else item.iconRes
        val iconResId = context.resources.getIdentifier(iconName, "drawable", context.packageName)
        if (iconResId != 0) {
            views.setImageViewResource(R.id.item_icon, iconResId)
        }

        if (!item.label.isNullOrBlank()) {
            views.setTextViewText(R.id.item_label, item.label)
            views.setViewVisibility(R.id.item_label, View.VISIBLE)
        } else {
            views.setViewVisibility(R.id.item_label, View.GONE)
        }

        // fillInIntent: combina com o PendingIntentTemplate definido no Provider
        // para saber QUAL item foi tocado
        val fillInIntent = Intent().apply {
            putExtra(EXTRA_ITEM_ID, item.id)
        }
        views.setOnClickFillInIntent(R.id.item_icon, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = items[position].id.toLong()
    override fun hasStableIds(): Boolean = true
}
