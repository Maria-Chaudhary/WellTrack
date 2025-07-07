@file:Suppress("DEPRECATION")

package com.fittrack.ui.fragments

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.fittrack.LoginActivity
import com.example.fittrack.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.material.snackbar.Snackbar
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.request.DataDeleteRequest
import com.example.fittrack.GoogleFitHelper
import java.util.concurrent.TimeUnit
import androidx.core.content.edit
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.fitness.Fitness
import com.google.firebase.auth.FirebaseAuth
import androidx.appcompat.widget.SwitchCompat

class SettingsFragment : Fragment() {

    private lateinit var switchReminder: SwitchCompat
    private lateinit var tvVersion: TextView
    private lateinit var sharedPrefs: SharedPreferences

    private fun deleteGoogleFitData(context: Context) {
        val account = GoogleSignIn.getAccountForExtension(context, GoogleFitHelper.fitnessOptions)

        val startTime = 1L
        val endTime = System.currentTimeMillis()

        val request = DataDeleteRequest.Builder()
            .setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS)
            .addDataType(DataType.TYPE_STEP_COUNT_DELTA)
            .addDataType(DataType.TYPE_CALORIES_EXPENDED)
            .addDataType(DataType.TYPE_DISTANCE_DELTA)
            .addDataType(DataType.TYPE_MOVE_MINUTES)
            .addDataType(DataType.TYPE_HEART_RATE_BPM)
            .addDataType(DataType.TYPE_SLEEP_SEGMENT)
            .addDataType(DataType.TYPE_ACTIVITY_SEGMENT)
            .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA)
            .addDataType(DataType.AGGREGATE_CALORIES_EXPENDED)
            .addDataType(DataType.AGGREGATE_DISTANCE_DELTA)
            .addDataType(DataType.AGGREGATE_HEART_RATE_SUMMARY)
            .addDataType(DataType.AGGREGATE_ACTIVITY_SUMMARY)
            .build()

        context.getSharedPreferences("goals_prefs", Context.MODE_PRIVATE).edit { clear() }

        Fitness.getHistoryClient(context, account)
            .deleteData(request)
            .addOnSuccessListener {
                Log.d("Reset", "✅ All Google Fit data deleted successfully")
            }
            .addOnFailureListener {
                Log.e("Reset", "❌ Failed to delete Google Fit data", it)
            }
    }

    @SuppressLint("UseKtx", "SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        sharedPrefs = requireContext().getSharedPreferences("goals_prefs", 0)

        switchReminder = view.findViewById(R.id.switchReminder)
        tvVersion = view.findViewById(R.id.tvVersion)

        switchReminder.isChecked = sharedPrefs.getBoolean("reminder_enabled", false)

        switchReminder.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("reminder_enabled", isChecked).apply()

            if (isChecked) {
                scheduleReminder(requireContext())
            } else {
                cancelReminder(requireContext())
            }

            Snackbar.make(view, if (isChecked) "Reminders Enabled" else "Reminders Disabled", Snackbar.LENGTH_SHORT).show()
        }

        view.findViewById<Button>(R.id.btnChangeGoal).setOnClickListener {
            Snackbar.make(view, "Goal Change feature coming soon", Snackbar.LENGTH_SHORT).show()
        }

        view.findViewById<Button>(R.id.btnResetAll).setOnClickListener {
            sharedPrefs.edit { clear() }
            deleteGoogleFitData(requireContext())
            Snackbar.make(view, "All data reset successfully", Snackbar.LENGTH_SHORT).show()
        }

        view.findViewById<Button>(R.id.btnLogout).setOnClickListener {
            sharedPrefs.edit { clear() }

            FirebaseAuth.getInstance().signOut()
            GoogleSignIn.getClient(requireContext(), GoogleSignInOptions.DEFAULT_SIGN_IN).signOut()

            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
        }

        view.findViewById<Button>(R.id.btnRateApp).setOnClickListener {
            try {
                val uri = Uri.parse("market://details?id=" + requireContext().packageName)
                startActivity(Intent(Intent.ACTION_VIEW, uri))
            } catch (e: Exception) {
                val uri = Uri.parse("https://play.google.com/store/apps/details?id=" + requireContext().packageName)
                startActivity(Intent(Intent.ACTION_VIEW, uri))
            }
        }

        view.findViewById<Button>(R.id.btnPrivacy).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://yourprivacyurl.com")))
        }

        view.findViewById<Button>(R.id.btnFeedback).setOnClickListener {
            val email = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, arrayOf("support@fittrack.com"))
                putExtra(Intent.EXTRA_SUBJECT, "Feedback on FitTrack App")
            }
            startActivity(email)
        }

        view.findViewById<Button>(R.id.btnAppInfo).setOnClickListener {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:" + requireContext().packageName)
            })
        }

        tvVersion.text = "Version 1.0.78"

        return view
    }
}

fun scheduleReminder(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, ReminderReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val triggerTime = System.currentTimeMillis() + 60 * 60 * 1000 // 1 hour later

    alarmManager.setRepeating(
        AlarmManager.RTC_WAKEUP,
        triggerTime,
        AlarmManager.INTERVAL_HOUR,
        pendingIntent
    )
}

fun cancelReminder(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, ReminderReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    alarmManager.cancel(pendingIntent)
}
