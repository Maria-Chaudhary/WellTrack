package com.fittrack.ui.fragments

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import com.example.fittrack.GoogleFitHelper
import com.example.fittrack.R
import androidx.core.graphics.toColorInt

class GoalsFragment : Fragment() {

    private lateinit var fitHelper: GoogleFitHelper

    private lateinit var progressSteps: ProgressBar
    private lateinit var progressWorkout: ProgressBar
    private lateinit var progressCalories: ProgressBar
    private lateinit var progressSleep: ProgressBar
    private lateinit var stepsGoalSummary: TextView
    private lateinit var workoutGoalSummary: TextView
    private lateinit var caloriesGoalSummary: TextView
    private lateinit var sleepGoalSummary: TextView
    private lateinit var motivationText: TextView
    private val prefsName = "goals_prefs"
    private var stepsGoal = 10000
    private var workoutGoal = 60
    private var caloriesGoal = 1000
    private var sleepGoal = 8  // Sleep goal updated by SleepFragment

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_goal, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        fitHelper = GoogleFitHelper(requireContext())
        progressSteps = view.findViewById(R.id.progressStepsGoal)
        progressWorkout = view.findViewById(R.id.progressWorkoutGoal)
        progressCalories = view.findViewById(R.id.progressCaloriesGoal)
        progressSleep = view.findViewById(R.id.progressSleepGoal)
        stepsGoalSummary = view.findViewById(R.id.stepsGoalSummary)
        workoutGoalSummary = view.findViewById(R.id.workoutGoalSummary)
        caloriesGoalSummary = view.findViewById(R.id.caloriesGoalSummary)
        sleepGoalSummary = view.findViewById(R.id.sleepGoalSummary)
        motivationText = view.findViewById(R.id.goalsMotivationText)
        loadGoals()
        fetchSteps()
        fetchCalories()
        fetchSleep()
        updateWorkoutProgress(getWorkoutMinutesFromPrefs())
        view.findViewById<ImageButton>(R.id.editStepsGoalButton).setOnClickListener {
            showEditDialog("Steps Goal", stepsGoal) { newGoal ->
                stepsGoal = newGoal
                saveGoals()
                fetchSteps()
            }
        }
        view.findViewById<ImageButton>(R.id.editWorkoutGoalButton).setOnClickListener {
            showEditDialog("Workout Goal (min)", workoutGoal) { newGoal ->
                workoutGoal = newGoal
                saveGoals()
                // Inside onViewCreated():
                updateWorkoutProgress(getWorkoutMinutesFromPrefs())
            }
        }

        view.findViewById<ImageButton>(R.id.editCaloriesGoalButton).setOnClickListener {
            showEditDialog("Calories Goal", caloriesGoal) { newGoal ->
                caloriesGoal = newGoal
                saveGoals()
                fetchCalories()
            }
        }

        view.findViewById<ImageButton>(R.id.editSleepGoalButton).setOnClickListener {
            showEditDialog("Sleep Goal (h)", sleepGoal) { newGoal ->
                sleepGoal = newGoal
                saveGoals()
                fetchSleep()
            }
        }
    }
    private fun loadGoals() {
        val prefs = requireActivity().getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        stepsGoal = prefs.getInt("steps_goal", 10000)
        workoutGoal = prefs.getInt("workout_goal", 60)
        caloriesGoal = prefs.getInt("calories_goal", 1000)
        sleepGoal = prefs.getInt("sleep_goal", 8)
    }

    private fun saveGoals() {
        val prefs = requireActivity().getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        prefs.edit {
            putInt("steps_goal", stepsGoal)
            putInt("workout_goal", workoutGoal)
            putInt("calories_goal", caloriesGoal)
            putInt("sleep_goal", sleepGoal)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun fetchSteps() {
        fitHelper.readDailySteps { steps ->
            if (!isAdded || view == null) return@readDailySteps
            requireActivity().runOnUiThread {
                progressSteps.max = stepsGoal
                progressSteps.progress = steps
                stepsGoalSummary.text = "$steps / $stepsGoal"

                val color = if (steps >= stepsGoal) "#00C853" else "#4CAF50"
                progressSteps.progressTintList = android.content.res.ColorStateList.valueOf(color.toColorInt())

                if (steps >= stepsGoal) {
                    showGoalAchievedNotification(getString(R.string.goal_steps_achieved))
                }

                updateMotivation()
            }
        }
    }
    @SuppressLint("SetTextI18n")
    private fun fetchCalories() {
        fitHelper.readDailySteps { steps ->
            if (!isAdded || view == null) return@readDailySteps
            requireActivity().runOnUiThread {
                val calories = fitHelper.calculateCaloriesFromSteps(steps)

                progressCalories.max = caloriesGoal
                progressCalories.progress = calories.toInt()
                caloriesGoalSummary.text = "$calories / $caloriesGoal"

                val color = if (calories >= caloriesGoal) "#00C853" else "#FF9800"
                progressCalories.progressTintList = android.content.res.ColorStateList.valueOf(color.toColorInt())

                if (calories >= caloriesGoal) {
                    showGoalAchievedNotification(getString(R.string.goal_calories_achieved))
                }

                updateMotivation()
            }
        }
    }
    @SuppressLint("SetTextI18n")
    private fun fetchSleep() {
        if (!isAdded || view == null) return
        val prefs = requireActivity().getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val sleepHours = prefs.getFloat("sleep_hours", 0f)
        val sleepInt = sleepHours.toInt()

        progressSleep.max = sleepGoal
        progressSleep.progress = sleepInt
        sleepGoalSummary.text = "$sleepInt / $sleepGoal"
        val color = if (sleepInt >= sleepGoal) "#00C853" else "#9C27B0"
        progressSleep.progressTintList = android.content.res.ColorStateList.valueOf(color.toColorInt())

        if (sleepInt >= sleepGoal) {
            showGoalAchievedNotification(getString(R.string.goal_sleep_achieved))
        }

        updateMotivation()
    }
    private fun showEditDialog(title: String, currentValue: Int, onSave: (Int) -> Unit) {
        val input = EditText(requireContext())
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        input.setText(currentValue.toString())

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.edit_goal_title, title))
            .setView(input)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val newValue = input.text.toString().toIntOrNull() ?: currentValue
                onSave(newValue)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showGoalAchievedNotification(message: String) {
        if (!isAdded || context == null) return
        val notificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val channelId = "goals_channel"

        val channel = android.app.NotificationChannel(channelId, "Goals", android.app.NotificationManager.IMPORTANCE_HIGH)
        notificationManager.createNotificationChannel(channel)

        val notification = androidx.core.app.NotificationCompat.Builder(requireContext(), channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Congratulations 🎉")
            .setContentText(message)
            .setAutoCancel(true)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun updateMotivation() {
        if (!isAdded || view == null) return
        val messages = listOf(
            getString(R.string.motivation_1),
            getString(R.string.motivation_2),
            getString(R.string.motivation_3),
            getString(R.string.motivation_4),
            getString(R.string.motivation_5)
        )
        motivationText.text = messages.random()


    }
  private  fun getWorkoutMinutesFromPrefs(): Int {
        val prefs = requireContext().getSharedPreferences("WorkoutPrefs", Context.MODE_PRIVATE)
        return prefs.getInt("workout_minutes", 0)
    }
    @SuppressLint("SetTextI18n")
    private fun updateWorkoutProgress(workoutMinutes: Int) {
        if (!isAdded || view == null) return
        progressWorkout.max = workoutGoal
        progressWorkout.progress = workoutMinutes
        workoutGoalSummary.text = "$workoutMinutes / $workoutGoal"

        val color = if (workoutMinutes >= workoutGoal) "#00C853" else "#2196F3"
        progressWorkout.progressTintList = android.content.res.ColorStateList.valueOf(color.toColorInt())

        if (workoutMinutes >= workoutGoal) {
            showGoalAchievedNotification(getString(R.string.goal_workout_achieved))
        }

        updateMotivation()
    }


    override fun onResume() {
        super.onResume()
        loadGoals()
        fetchSteps()
        fetchCalories()
        updateWorkoutProgress(getWorkoutMinutesFromPrefs())
        fetchSleep()
    }
}
