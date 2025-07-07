package com.fittrack.ui.fragments

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import lecho.lib.hellocharts.model.*
import lecho.lib.hellocharts.view.ColumnChartView
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.graphics.toColorInt
import com.example.fittrack.R

class SleepFragment : Fragment() {

    private lateinit var lastNightSleepText: TextView
    private lateinit var sleepGoalProgressText: TextView
    private lateinit var sleepProgressBar: ProgressBar
    private lateinit var sleepHistoryText: TextView
    private lateinit var sleepChart: ColumnChartView
    private lateinit var motivationText: TextView
    private var sleepGoal = 8
    private val prefsName = "goals_prefs"
    private val historyKey = "sleep_history"
    private val sleepHistory = mutableListOf<Pair<String, Float>>() // date -> hours

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_sleep, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        lastNightSleepText = view.findViewById(R.id.lastNightSleepText)
        sleepGoalProgressText = view.findViewById(R.id.sleepGoalProgressText)
        sleepProgressBar = view.findViewById(R.id.sleepProgressBar)
        sleepHistoryText = view.findViewById(R.id.sleepHistoryText)
        sleepChart = view.findViewById(R.id.sleepChart)
        motivationText = view.findViewById(R.id.sleepMotivationText)
        loadSleepGoal()
        loadHistory()
        fetchSleep()
        view.findViewById<Button>(R.id.addSleepButton).setOnClickListener {
            showAddSleepDialog()
        }
    }

    private fun loadSleepGoal() {
        val prefs = requireActivity().getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        sleepGoal = prefs.getInt("sleep_goal", 8)
    }

    private fun saveSleepGoal(newGoal: Int) {
        val prefs = requireActivity().getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        prefs.edit {
            putInt("sleep_goal", newGoal)
        }
        sleepGoal = newGoal
    }
    private fun saveHistory() {
        val prefs = requireActivity().getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val set = sleepHistory.map { "${it.first}~${it.second}" }.toSet()
        prefs.edit { putStringSet(historyKey, set) }
    }
    private fun loadHistory() {
        val prefs = requireActivity().getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val savedSet = prefs.getStringSet(historyKey, emptySet())
        sleepHistory.clear()
        savedSet?.forEach {
            val parts = it.split("~")
            if (parts.size == 2) {
                val date = parts[0]
                val hours = parts[1].toFloatOrNull() ?: 0f
                sleepHistory.add(date to hours)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun fetchSleep() {
        val prefs = requireActivity().getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val sleepHours = prefs.getFloat("sleep_hours", 0f)

        val sleepInt = sleepHours.toInt()
        val minutes = ((sleepHours - sleepInt) * 60).toInt()
        lastNightSleepText.text = "Last Night: $sleepInt h $minutes m"
        sleepGoalProgressText.text = "Goal: $sleepGoal hours"
        sleepProgressBar.max = sleepGoal
        sleepProgressBar.progress = sleepInt
        val today = SimpleDateFormat("EEE dd", Locale.getDefault()).format(Date())
        if (sleepHistory.isEmpty() || sleepHistory.last().first != today) {
            sleepHistory.add(Pair(today, sleepHours))
            if (sleepHistory.size > 7) sleepHistory.removeAt(0)
            saveHistory()
        }
        updateMotivation()
        updateSleepHistoryUI()
        updateChart()
    }

    private fun showAddSleepDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_sleep, null)
        val inputHours = dialogView.findViewById<EditText>(R.id.inputHours)
        val inputMinutes = dialogView.findViewById<EditText>(R.id.inputMinutes)
        AlertDialog.Builder(requireContext())
            .setTitle("Add Sleep Record")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val hours = inputHours.text.toString().toFloatOrNull() ?: 0f
                val minutes = inputMinutes.text.toString().toFloatOrNull() ?: 0f
                val totalSleep = hours + (minutes / 60f)

                val prefs = requireActivity().getSharedPreferences(prefsName, Context.MODE_PRIVATE)
                prefs.edit { putFloat("sleep_hours", totalSleep) }

                val today = SimpleDateFormat("EEE dd", Locale.getDefault()).format(Date())
                sleepHistory.removeAll { it.first == today }
                sleepHistory.add(Pair(today, totalSleep))
                if (sleepHistory.size > 7) sleepHistory.removeAt(0)
                saveHistory()
                if (totalSleep > sleepGoal) {
                    saveSleepGoal(totalSleep.toInt() + 1)
                }
                fetchSleep()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateSleepHistoryUI() {
        val historyStr = sleepHistory.joinToString("\n") {
            "${it.first}: ${"%.1f".format(it.second)} h"
        }
        sleepHistoryText.text = historyStr.ifEmpty { "- No records yet" }
    }
    private fun updateChart() {
        val columns = mutableListOf<Column>()
        val axisXValues = mutableListOf<AxisValue>()
        for ((index, entry) in sleepHistory.withIndex()) {
            val hours = entry.second
            val dateLabel = entry.first.split(" ")[1] // only day number
            val values = listOf(SubcolumnValue(hours, "#ff00ff".toColorInt()))
            columns.add(Column(values).setHasLabels(true))
            axisXValues.add(AxisValue(index.toFloat()).setLabel(dateLabel))
        }
        val data = ColumnChartData(columns)
        data.axisXBottom = Axis(axisXValues).setName("Day").setHasLines(true)
        data.axisYLeft = Axis().setName("Hours Slept").setHasLines(true)

        sleepChart.columnChartData = data
    }

    private fun updateMotivation() {
        val messages = listOf(
            "Great sleep = great energy! 💪",
            "Consistency is key 💤",
            "You're building a healthy habit 🌙",
            "Your body will thank you 🧘‍♀️",
            "Well done on your sleep progress! ⭐"
        )
        motivationText.text = messages.random()
    }

    override fun onResume() {
        super.onResume()
        loadSleepGoal()
        loadHistory()
        fetchSleep()
    }
}
