package com.tidalvolt.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

/** Local Android event alerts. No messaging account or external service required. */
object VehicleNotificationEvents {
    private const val CHANNEL_ID = "vehicle_events"
    private const val PREFS = "tidalvolt_notification_state"

    fun onTelemetry(context: Context, data: EvTelemetry) {
        val app = context.applicationContext
        val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val settings = app.getSharedPreferences("tidalvolt_notifications", Context.MODE_PRIVATE)
        ensureChannel(app)

        val previousCharging = prefs.getBoolean("previous_charging", false)
        val currentCharging = data.isCharging
        if (currentCharging != null && currentCharging != previousCharging) {
            if (currentCharging && settings.getBoolean("charging_start", true)) {
                post(app, 1001, "Charging started", "Your BYD Yuan Pro has started charging.")
            } else if (!currentCharging && previousCharging && settings.getBoolean("charging_complete", true)) {
                post(app, 1002, "Charging stopped", "Your BYD Yuan Pro is no longer charging.")
            }
            prefs.edit().putBoolean("previous_charging", currentCharging).apply()
        }

        val previousSoc = prefs.getFloat("previous_soc", -1f).toDouble()
        val currentSoc = data.soc
        val threshold = settings.getString("threshold_percent", "20")?.toDoubleOrNull() ?: 20.0
        if (currentSoc != null && settings.getBoolean("battery_threshold", true) &&
            previousSoc > threshold && currentSoc <= threshold) {
            post(app, 1003, "Battery is low", "Battery has reached " + currentSoc.toInt() +
                "% (alert set to " + threshold.toInt() + "%).")
        }
        if (currentSoc != null) prefs.edit().putFloat("previous_soc", currentSoc.toFloat()).apply()

        // Trip completion heuristic: moving, then stopped for at least three minutes.
        val speed = data.speedMph
        val now = data.receivedAtMillis
        if (speed != null && speed > 3.0) {
            prefs.edit().putLong("last_moving_at", now).putBoolean("trip_stop_alerted", false).apply()
        } else if (speed != null && speed < 1.0 && settings.getBoolean("trip_complete", true)) {
            val lastMoving = prefs.getLong("last_moving_at", 0L)
            val alreadyAlerted = prefs.getBoolean("trip_stop_alerted", false)
            if (lastMoving > 0L && now - lastMoving >= 180_000L && !alreadyAlerted) {
                post(app, 1004, "Trip completed", "Your vehicle has been stopped for at least three minutes.")
                prefs.edit().putBoolean("trip_stop_alerted", true).remove("last_moving_at").apply()
            }
        }
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                manager.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, "Vehicle events", NotificationManager.IMPORTANCE_DEFAULT)
                )
            }
        }
    }

    private fun post(context: Context, id: Int, title: String, message: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(context)
        }
        val notification = builder.setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title).setContentText(message)
            .setStyle(Notification.BigTextStyle().bigText(message))
            .setAutoCancel(true).build()
        try { manager.notify(id, notification) } catch (_: SecurityException) { }
    }
}
