package id.openwear.diskhealth.provider

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import id.openwear.diskhealth.R
import id.openwear.diskhealth.data.DeviceInfoReader
import id.openwear.diskhealth.ui.Artwork

/**
 * Device dashboard widget, ported from v4.0.1 `DashboardWidgetProvider` +
 * `p057w1.a`.
 *
 * The refresh button broadcasts [ACTION_REFRESH_DASHBOARD] to this provider.
 * The root view consumes [ACTION_DO_NOTHING] so tapping the card does not open
 * the app, matching the original.
 */
class DashboardWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH_DASHBOARD) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, DashboardWidgetProvider::class.java)
            )
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
        val info = DeviceInfoReader.read(context)
        val compact = manager.getAppWidgetOptions(widgetId)
            .getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) < 180
        val views = RemoteViews(
            context.packageName,
            if (compact) R.layout.widget_dashboard_compact else R.layout.widget_dashboard
        )

        views.setTextViewText(R.id.tvDevice, info.model.uppercase())
        val powerProfileBattery =
            DeviceInfoReader.powerProfileDouble(context).toInt().toString()
        views.setTextViewText(
            R.id.tvBatteryCapacity,
            context.getString(R.string.battery_capacity) + " : " + powerProfileBattery + " mAh"
        )
        views.setTextViewText(
            R.id.tvWearLevel,
            id.openwear.diskhealth.data.WearLevelReader.healthText(context)
        )
        if (!compact) {
            views.setTextViewText(R.id.tvCPU, context.getString(R.string.processor) + " : " + info.cpu)
            views.setTextViewText(R.id.tvGPU, context.getString(R.string.graphic) + " : " + info.gpu)
            views.setTextViewText(R.id.tvTotalRam, context.getString(R.string.total_ram) + " : " + info.ram)
            views.setTextViewText(R.id.tvTotalStorage, context.getString(R.string.total_storage) + " : " + info.storage)
            views.setTextViewText(R.id.tvActiveSlot, context.getString(R.string.active_slot) + " : " + info.activeSlot)
            // The v4 widget sources these two values in the opposite order from the
            // main dashboard: battery = PowerProfile, profile = charge_full.
            views.setTextViewText(
                R.id.tvKernelPowerProfile,
                context.getString(R.string.power_profile) + " : " + info.batteryCapacity
            )
        }
        views.setImageViewResource(R.id.deviceimage, Artwork.forModel(info.model))

        val refresh = Intent(context, DashboardWidgetProvider::class.java)
            .setAction(ACTION_REFRESH_DASHBOARD)
        views.setOnClickPendingIntent(
            R.id.btn_refresh,
            PendingIntent.getBroadcast(
                context,
                widgetId,
                refresh,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )

        val noop = Intent(context, DashboardWidgetProvider::class.java)
            .setAction(ACTION_DO_NOTHING)
        views.setOnClickPendingIntent(
            R.id.widget_root,
            PendingIntent.getBroadcast(
                context,
                widgetId + 1000,
                noop,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )

        manager.updateAppWidget(widgetId, views)
    }

    private companion object {
        const val ACTION_REFRESH_DASHBOARD = "id.vern.wearlevelinsight.widgets.ACTION_REFRESH_DASHBOARD"
        const val ACTION_DO_NOTHING = "id.vern.wearlevelinsight.widgets.ACTION_DO_NOTHING"
    }
}
