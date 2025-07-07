package com.fittrack.ui.fragments

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import com.example.fittrack.R
import java.text.SimpleDateFormat
import java.util.*

class ReminderFragment : Fragment() {
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var reminderSwitch: Switch
    private lateinit var intervalInput: EditText
    private lateinit var nextReminderText: TextView
    private lateinit var lastReminderText: TextView
    private lateinit var testReminderButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var progressText: TextView

    // Checkboxes
    private lateinit var waterCheckbox: CheckBox
    private lateinit var stretchCheckbox: CheckBox
    private lateinit var eyeBreakCheckbox: CheckBox
    private lateinit var postureCheckbox: CheckBox
    private lateinit var breathingCheckbox: CheckBox
    private lateinit var walkCheckbox: CheckBox
    private lateinit var hydrationCheckbox: CheckBox

    private val prefsName = "reminder_prefs"
    private val maxRemindersPerDay = 5

    private var remindersSentToday = 0

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_reminder, container, false)

        // Find views
        reminderSwitch = view.findViewById(R.id.reminderSwitch)
        intervalInput = view.findViewById(R.id.intervalInput)
        nextReminderText = view.findViewById(R.id.nextReminderText)
        lastReminderText = view.findViewById(R.id.lastReminderText)
        testReminderButton = view.findViewById(R.id.testReminderButton)
        progressBar = view.findViewById(R.id.progressBar)
        progressText = view.findViewById(R.id.progressText)
        // Checkboxes
        waterCheckbox = view.findViewById(R.id.waterReminderCheckbox)
        stretchCheckbox = view.findViewById(R.id.stretchReminderCheckbox)
        eyeBreakCheckbox = view.findViewById(R.id.eyeBreakReminderCheckbox)
        postureCheckbox = view.findViewById(R.id.postureReminderCheckbox)
        breathingCheckbox = view.findViewById(R.id.breathingReminderCheckbox)
        walkCheckbox = view.findViewById(R.id.walkReminderCheckbox)
        hydrationCheckbox = view.findViewById(R.id.hydrationReminderCheckbox)
        // Load saved state
        val prefs = requireActivity().getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("reminder_enabled", false)
        val interval = prefs.getInt("reminder_interval", 60)
        remindersSentToday = prefs.getInt("reminders_sent_today", 0)

        reminderSwitch.isChecked = isEnabled
        intervalInput.setText(interval.toString())
        loadCheckboxStates()
        updateProgress()
        // Set initial UI
        if (isEnabled) {
            val nextReminderInMin = getNextReminderInMinutes()
            nextReminderText.text = "Next reminder in: $nextReminderInMin min"
        } else {
            nextReminderText.text = "Next reminder in: --"
        }
        val lastTime = prefs.getString("last_reminder_time", "--")
        lastReminderText.text = "Last reminder sent: $lastTime"

        // Switch toggle
        reminderSwitch.setOnCheckedChangeListener { _, isChecked ->
            val inputMinutes = intervalInput.text.toString().toIntOrNull() ?: 60
            prefs.edit {
                putBoolean("reminder_enabled", isChecked)
                putInt("reminder_interval", inputMinutes)
            }
            if (isChecked) {
                scheduleReminder(inputMinutes)
                Toast.makeText(requireContext(), "Reminder scheduled every $inputMinutes min", Toast.LENGTH_SHORT).show()
                val nextReminderInMin = getNextReminderInMinutes()
                nextReminderText.text = "Next reminder in: $nextReminderInMin min"
            } else {
                cancelReminder()
                Toast.makeText(requireContext(), "Reminder cancelled", Toast.LENGTH_SHORT).show()
                nextReminderText.text = "Next reminder in: --"
            }
        }
        // Test button
        testReminderButton.setOnClickListener {
            sendTestReminder()
        }
        // Save checkboxes when clicked
        val saveCheckboxState: (CheckBox, String) -> Unit = { checkbox, key ->
            checkbox.setOnCheckedChangeListener { _, isChecked ->
                prefs.edit {
                    putBoolean(key, isChecked)
                }
            }
        }
        saveCheckboxState(waterCheckbox, "reminder_water")
        saveCheckboxState(stretchCheckbox, "reminder_stretch")
        saveCheckboxState(eyeBreakCheckbox, "reminder_eye")
        saveCheckboxState(postureCheckbox, "reminder_posture")
        saveCheckboxState(breathingCheckbox, "reminder_breathing")
        saveCheckboxState(walkCheckbox, "reminder_walk")
        saveCheckboxState(hydrationCheckbox, "reminder_hydration")
        return view
    }

    @SuppressLint("ScheduleExactAlarm")
    private fun scheduleReminder(intervalMinutes: Int) {
        val alarmManager = requireActivity().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(requireContext(), ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(requireContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val intervalMillis = intervalMinutes * 60 * 1000L
        val triggerAtMillis = System.currentTimeMillis() + intervalMillis

        // Disable at night (after 9 PM)
        val hourNow = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (hourNow >= 21) {
            Toast.makeText(requireContext(), "Reminder disabled at night (after 9 PM)", Toast.LENGTH_SHORT).show()
            return
        }
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )
    }

    private fun cancelReminder() {
        val alarmManager = requireActivity().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(requireContext(), ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(requireContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        alarmManager.cancel(pendingIntent)
    }

    private fun getNextReminderInMinutes(): Int {
        val prefs = requireActivity().getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val interval = prefs.getInt("reminder_interval", 60)
        return interval
    }
    @SuppressLint("SetTextI18n")
    private fun sendTestReminder() {
        // Update UI and progress
        remindersSentToday++
        if (remindersSentToday > maxRemindersPerDay) {
            remindersSentToday = maxRemindersPerDay
        }
        val prefs = requireActivity().getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        prefs.edit {
            putInt("reminders_sent_today", remindersSentToday)
            putString("last_reminder_time", getCurrentTimeString())
        }
        updateProgress()
        lastReminderText.text = "Last reminder sent: ${getCurrentTimeString()}"
        nextReminderText.text = "Next reminder in: ${getNextReminderInMinutes()} min"
        // Build dynamic message
        val message = buildReminderMessage()
        // Send broadcast to ReminderReceiver — with message
        val intent = Intent(requireContext(), ReminderReceiver::class.java)
        intent.putExtra("reminder_message", message)

        requireContext().sendBroadcast(intent)
    }

    private fun buildReminderMessage(): String {
        val list = mutableListOf<String>()
        if (waterCheckbox.isChecked) list.add("💧 Water")
        if (stretchCheckbox.isChecked) list.add("🧘‍♀️ Stretch")
        if (eyeBreakCheckbox.isChecked) list.add("👀 Eye Break")
        if (postureCheckbox.isChecked) list.add("🪑 Posture")
        if (breathingCheckbox.isChecked) list.add("🫁 Breathing")
        if (walkCheckbox.isChecked) list.add("🚶‍♀️ Walk")
        if (hydrationCheckbox.isChecked) list.add("💦 Hydration")

        return if (list.isEmpty()) {
            "Time to Move!"
        } else {
            "Time to Move!\nAlso: ${list.joinToString(", ")}"
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateProgress() {
        progressBar.progress = remindersSentToday
        progressText.text = "Reminders today: $remindersSentToday/$maxRemindersPerDay"
    }

    private fun getCurrentTimeString(): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun loadCheckboxStates() {
        val prefs = requireActivity().getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        waterCheckbox.isChecked = prefs.getBoolean("reminder_water", false)
        stretchCheckbox.isChecked = prefs.getBoolean("reminder_stretch", false)
        eyeBreakCheckbox.isChecked = prefs.getBoolean("reminder_eye", false)
        postureCheckbox.isChecked = prefs.getBoolean("reminder_posture", false)
        breathingCheckbox.isChecked = prefs.getBoolean("reminder_breathing", false)
        walkCheckbox.isChecked = prefs.getBoolean("reminder_walk", false)
        hydrationCheckbox.isChecked = prefs.getBoolean("reminder_hydration", false)
    }
}
