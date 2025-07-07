package com.fittrack.ui.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.edit
import androidx.recyclerview.widget.RecyclerView
import com.example.fittrack.databinding.ItemWorkoutOptionBinding
class WorkoutOptionAdapter(
    private val context: Context,  private val options: List<String>,
    private val workoutType: String,private val onWorkoutCompleted: (durationMinutes: Int) -> Unit
// ✅ New callback
) : RecyclerView.Adapter<WorkoutOptionAdapter.OptionViewHolder>() {
    private val sharedPref: SharedPreferences =
        context.getSharedPreferences("WorkoutPrefs", Context.MODE_PRIVATE)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OptionViewHolder {
        val binding = ItemWorkoutOptionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OptionViewHolder(binding)
    }
    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: OptionViewHolder, position: Int) {
        val optionName = options[position]
        val keyBase = "${workoutType}_$optionName"
        val count = sharedPref.getInt("${keyBase}_count", 0)
        val lastDuration = sharedPref.getLong("${keyBase}_lastDuration", 0L)
        val caloriesPerMin = 5
        val lastCalories = (lastDuration / 60000) * caloriesPerMin
        holder.binding.tvOptionItem.text = optionName
        holder.binding.tvWorkoutCount.text =
            "Completed: $count times\nLast: ${lastDuration / 1000} sec | $lastCalories kcal"
        holder.binding.btnStartWorkout.text = if (holder.isWorkoutRunning) {
            "Stop $optionName"
        } else {
            "Start $optionName" }
        holder.binding.btnStartWorkout.setOnClickListener {
            if (!holder.isWorkoutRunning) {
                // Start workout
                holder.startTime = SystemClock.elapsedRealtime()
                holder.isWorkoutRunning = true
                holder.binding.btnStartWorkout.text = "Stop $optionName"
                Toast.makeText(context, "Started $optionName", Toast.LENGTH_SHORT).show()
            } else {
                // Stop workout
                val endTime = SystemClock.elapsedRealtime()
                val duration = endTime - holder.startTime
                val durationMinutes = (duration / 60000).toInt().coerceAtLeast(1) // at least 1 minute
                val newCount = sharedPref.getInt("${keyBase}_count", 0) + 1
                val caloriesBurned = durationMinutes * caloriesPerMin
                // Save to SharedPreferences
                sharedPref.edit {
                    putInt("${keyBase}_count", newCount)
                    putLong("${keyBase}_lastDuration", duration) }
                onWorkoutCompleted(durationMinutes)  // ✅ Trigger callback
                holder.binding.tvWorkoutCount.text =
                    "Completed: $newCount times\nLast: ${duration / 1000} sec | $caloriesBurned kcal"
                holder.isWorkoutRunning = false
                holder.binding.btnStartWorkout.text = "Start $optionName"
                Toast.makeText(context, "Finished $optionName!", Toast.LENGTH_SHORT).show()
            }
        }
    }
    override fun getItemCount(): Int = options.size
    inner class OptionViewHolder(val binding: ItemWorkoutOptionBinding) : RecyclerView.ViewHolder(binding.root) {
        var isWorkoutRunning = false
        var startTime = 0L
    }
}
