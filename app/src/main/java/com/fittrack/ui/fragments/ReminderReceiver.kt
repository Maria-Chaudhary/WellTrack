package com.fittrack.ui.fragments

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.fittrack.MainActivity
import com.example.fittrack.R
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.content.edit

class ReminderReceiver : BroadcastReceiver() {
    private val channelId = "activity_reminder_channel"
    private val notificationId = 1001
    @SuppressLint("ObsoleteSdkInt")
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("reminder_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("reminder_enabled", true)) return  // ✅ Check if reminders are enabled
        // Get message from intent if available (Test Reminder)
        val message = intent.getStringExtra("reminder_message")
            ?: buildReminderMessageFromPrefs(prefs)
        // Update last reminder time + progress
        val remindersSentToday = prefs.getInt("reminders_sent_today", 0) + 1
        prefs.edit {
            putInt("reminders_sent_today", remindersSentToday)
                .putString("last_reminder_time", getCurrentTimeString())  }
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Create channel if needed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Activity Reminder", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)   }
        // Intent to open app when clicked
        val activityIntent = Intent(context, MainActivity::class.java)
        val contentIntent = PendingIntent.getActivity(context, 0, activityIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        // Build notification
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("FitTrack Reminder")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        notificationManager.notify(notificationId, notification)    // Show notification
    }
    private fun buildReminderMessageFromPrefs(prefs: android.content.SharedPreferences): String {
        val list = mutableListOf<String>()
        if (prefs.getBoolean("reminder_water", false)) list.add("💧 Water")
        if (prefs.getBoolean("reminder_stretch", false)) list.add("🧘‍♀️ Stretch")
        if (prefs.getBoolean("reminder_eye", false)) list.add("👀 Eye Break")
        if (prefs.getBoolean("reminder_posture", false)) list.add("🪑 Posture")
        if (prefs.getBoolean("reminder_breathing", false)) list.add("🫁 Breathing")
        if (prefs.getBoolean("reminder_walk", false)) list.add("🚶‍♀️ Walk")
        if (prefs.getBoolean("reminder_hydration", false)) list.add("💦 Hydration")
        return if (list.isEmpty()) {
            "Time to Move!"
        } else {
            "Time to Move!\nAlso: ${list.joinToString(", ")}" } }
    private fun getCurrentTimeString(): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return sdf.format(Date())
    }
}
