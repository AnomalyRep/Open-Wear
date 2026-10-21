package id.openwear.diskhealth.provider

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import id.openwear.diskhealth.R
import id.openwear.diskhealth.data.WearGroup
import id.openwear.diskhealth.data.WearLevelReader
import kotlinx.coroutines.runBlocking

/**
 * Wear-level home screen widget, ported from v4.0.1
 * `WearlevelWidgetProvider` + `p057w1.b` + `m2.n.c`.
 *
 * A refresh broadcast rescans the eMMC/UFS lifetime files, then rebuilds both
 * wear groups. Tapping the widget triggers the same refresh.
 */
class WearLevelWidget : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_WEAR_REFRESH) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, WearLevelWidget::class.java))
            onUpdate(context, manager, ids)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val appContext = context.applicationContext
        for (id in appWidgetIds) {
            Thread {
                runCatching { updateWidget(appContext, appWidgetManager, id) }
            }.start()
        }
    }

    private fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
        runBlocking { WearLevelReader.scan(context) }
        val stats = WearLevelReader.stats(context)

        val compact = manager.getAppWidgetOptions(widgetId)
            .getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) < 180
        val views = RemoteViews(
            context.packageName,
            if (compact) R.layout.widget_wearlevel_compact else R.layout.widget_wearlevel
        )
        views.setTextViewText(
            R.id.tv_disk_type,
            stats.diskType + " " + context.getString(R.string.wear_level)
        )
        if (compact) {
            val group = if (stats.groupA.visible) stats.groupA else stats.groupB
            bindCompact(views, group)
        } else {
            bindGroup(views, true, stats.groupA)
            bindGroup(views, false, stats.groupB)
        }

        val refresh = Intent(context, WearLevelWidget::class.java).setAction(ACTION_WEAR_REFRESH)
        val pending = PendingIntent.getBroadcast(
            context,
            widgetId,
            refresh,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.cv_wear_level, pending)
        manager.updateAppWidget(widgetId, views)
    }

    private fun bindCompact(views: RemoteViews, group: WearGroup) {
        views.setTextViewText(R.id.tv_wear_level_title_a, group.title.orEmpty())
        bindNode(
            views,
            group.one,
            R.id.fl_1a,
            R.id.tv_hex_1_a,
            R.id.tv_wear_level_percent_1a,
            R.id.wear_level_progress_1a
        )
    }

    private fun bindGroup(views: RemoteViews, isA: Boolean, group: WearGroup) {
        val container = if (isA) R.id.rl_a else R.id.rl_b
        if (!group.visible) {
            views.setViewVisibility(container, android.view.View.GONE)
            return
        }
        views.setViewVisibility(container, android.view.View.VISIBLE)
        views.setTextViewText(
            if (isA) R.id.tv_wear_level_title_a else R.id.tv_wear_level_title_b,
            group.title.orEmpty()
        )
        bindNode(
            views,
            group.one,
            if (isA) R.id.fl_1a else R.id.fl_1b,
            if (isA) R.id.tv_hex_1_a else R.id.tv_hex_1_b,
            if (isA) R.id.tv_wear_level_percent_1a else R.id.tv_wear_level_percent_1b,
            if (isA) R.id.wear_level_progress_1a else R.id.wear_level_progress_1b
        )
        bindNode(
            views,
            group.two,
            if (isA) R.id.fl_2a else R.id.fl_2b,
            if (isA) R.id.tv_hex_2_a else R.id.tv_hex_2_b,
            if (isA) R.id.tv_wear_level_percent_2a else R.id.tv_wear_level_percent_2b,
            if (isA) R.id.wear_level_progress_2a else R.id.wear_level_progress_2b
        )
    }

    private fun bindNode(
        views: RemoteViews,
        node: id.openwear.diskhealth.data.WearNode?,
        frameId: Int,
        hexId: Int,
        percentId: Int,
        progressId: Int
    ) {
        if (node == null || !node.visible) {
            views.setViewVisibility(frameId, android.view.View.GONE)
            return
        }
        views.setViewVisibility(frameId, android.view.View.VISIBLE)
        views.setTextViewText(hexId, node.hex)
        views.setTextViewText(percentId, node.percentText)
        views.setProgressBar(progressId, 100, node.percent, false)
    }

    private companion object {
        const val ACTION_WEAR_REFRESH = "id.vern.wearlevelinsight.widgets.action.WEAR_REFRESH"
    }
}
