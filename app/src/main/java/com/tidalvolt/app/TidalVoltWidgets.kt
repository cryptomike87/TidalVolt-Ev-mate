package com.tidalvolt.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

open class TidalVoltWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        TidalVoltWidgetUpdater.update(context)
    }
}

class BatteryWidgetProvider : TidalVoltWidgetProvider()
class ChargingWidgetProvider : TidalVoltWidgetProvider()
class VehicleOverviewWidgetProvider : TidalVoltWidgetProvider()

object TidalVoltWidgetUpdater {
    fun update(context: Context) {
        val prefs = context.getSharedPreferences("tidalvolt_widget_data", Context.MODE_PRIVATE)
        val soc = prefs.getString("soc", null)?.let { "$it%" } ?: "—"
        val range = prefs.getString("range", null)?.let { "$it mi" } ?: "Range unavailable"
        val speed = prefs.getString("speed", null)?.let { "$it mph" } ?: "— mph"
        val charging = when (prefs.getString("charging", null)) {
            "true" -> "Charging"
            "false" -> "Not charging"
            else -> "Charging status unavailable"
        }
        val power = prefs.getString("power", null)?.let { "$it kW" } ?: "— kW"
        val eta = prefs.getString("eta", null)?.let { "$it min to full" } ?: "ETA unavailable"
        val updated = prefs.getString("updated", null) ?: "Waiting for vehicle data"
        val manager = AppWidgetManager.getInstance(context)

        updateType(context, manager, BatteryWidgetProvider::class.java, R.layout.widget_battery) { views ->
            views.setTextViewText(R.id.widget_battery_soc, soc)
            views.setTextViewText(R.id.widget_battery_range, range)
            views.setTextViewText(R.id.widget_battery_updated, updated)
        }
        updateType(context, manager, ChargingWidgetProvider::class.java, R.layout.widget_charging) { views ->
            views.setTextViewText(R.id.widget_charging_state, charging)
            views.setTextViewText(R.id.widget_charging_power, "Power: $power")
            views.setTextViewText(R.id.widget_charging_eta, eta)
            views.setTextViewText(R.id.widget_charging_soc, "Battery: $soc")
        }
        updateType(context, manager, VehicleOverviewWidgetProvider::class.java, R.layout.widget_vehicle_overview) { views ->
            views.setTextViewText(R.id.widget_overview_soc, soc)
            views.setTextViewText(R.id.widget_overview_range, range)
            views.setTextViewText(R.id.widget_overview_speed, speed)
            views.setTextViewText(R.id.widget_overview_state, charging)
            views.setTextViewText(R.id.widget_overview_updated, updated)
        }
    }

    private fun updateType(
        context: Context,
        manager: AppWidgetManager,
        provider: Class<out AppWidgetProvider>,
        layout: Int,
        bind: (RemoteViews) -> Unit
    ) {
        val ids = manager.getAppWidgetIds(ComponentName(context, provider))
        if (ids.isEmpty()) return
        val views = RemoteViews(context.packageName, layout)
        bind(views)
        val openApp = PendingIntent.getActivity(
            context, provider.name.hashCode(),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, openApp)
        manager.updateAppWidget(ids, views)
    }
}
