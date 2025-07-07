package com.fittrack.ui.fragments

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fittrack.databinding.DialogHeightWeightBinding
import com.example.fittrack.databinding.FragmentWorkoutsBinding
import androidx.core.content.edit

class WorkoutsFragment : Fragment() {
    private var _binding: FragmentWorkoutsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: WorkoutsViewModel by viewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWorkoutsBinding.inflate(inflater, container, false)
        return binding.root
    }
    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvWorkoutsTitle.text = "💪 My Workouts"
        setupRecycler(binding.recyclerStrength, addIcons(viewModel.strengthList, "🏋️"), "strength")
        setupRecycler(binding.recyclerHIIT, addIcons(viewModel.hiitList, "⚡"), "hiit")
        setupRecycler(binding.recyclerYoga, addIcons(viewModel.yogaList, "🧘"), "yoga")
        setupRecycler(binding.recyclerCardio, addIcons(viewModel.cardioList, "🏃"), "cardio")
        binding.arrowStrength.setOnClickListener { toggleRecycler(binding.recyclerStrength) }
        binding.arrowHIIT.setOnClickListener { toggleRecycler(binding.recyclerHIIT) }
        binding.arrowYoga.setOnClickListener { toggleRecycler(binding.recyclerYoga) }
        binding.arrowCardio.setOnClickListener { toggleRecycler(binding.recyclerCardio) }

        binding.btnResetCounts.setOnClickListener {
            resetAllCounts()
        }
        binding.btnSuggestWorkout.setOnClickListener {
            showBMIInputDialog()
        }
    }

    private fun setupRecycler(
        recycler: androidx.recyclerview.widget.RecyclerView,
        options: List<String>,
        type: String
    ) {
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = WorkoutOptionAdapter(requireContext(), options, type) { workoutName ->
            // 🔥 Workout clicked, increment total minutes
            incrementWorkoutMinutes(10) // assuming 10 min per workout
            Toast.makeText(requireContext(), "$workoutName logged +10 mins", Toast.LENGTH_SHORT).show()
        }
    }
    private fun toggleRecycler(recycler: androidx.recyclerview.widget.RecyclerView) {
        recycler.visibility = if (recycler.isVisible) View.GONE else View.VISIBLE
    }
    @SuppressLint("NotifyDataSetChanged")
    private fun resetAllCounts() {
        val sharedPref = requireContext().getSharedPreferences("WorkoutPrefs", Context.MODE_PRIVATE)
        sharedPref.edit {
            clear()
            putInt("workout_minutes", 0) // Reset workout minutes too
        }

        binding.recyclerStrength.adapter?.notifyDataSetChanged()
        binding.recyclerHIIT.adapter?.notifyDataSetChanged()
        binding.recyclerYoga.adapter?.notifyDataSetChanged()
        binding.recyclerCardio.adapter?.notifyDataSetChanged()
    }

    private fun addIcons(list: List<String>, emoji: String): List<String> {
        return list.map { "$emoji $it" }
    }

    @SuppressLint("SetTextI18n")
    private fun showBMIInputDialog() {
        val dialogBinding = DialogHeightWeightBinding.inflate(layoutInflater)
        AlertDialog.Builder(requireContext())
            .setTitle("Enter Height and Weight")
            .setView(dialogBinding.root)
            .setPositiveButton("Suggest") { _, _ ->
                val heightStr = dialogBinding.inputHeight.text.toString()
                val weightStr = dialogBinding.inputWeight.text.toString()
                val heightCm = heightStr.toFloatOrNull()
                val weightKg = weightStr.toFloatOrNull()
                if (heightCm == null || heightCm <= 30f) {
                    binding.tvWorkoutSuggestions.text = ""
                    Toast.makeText(requireContext(), "Please enter a valid height in cm (>30).", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (weightKg == null || weightKg <= 10f) {
                    binding.tvWorkoutSuggestions.text = ""
                    Toast.makeText(requireContext(), "Please enter a valid weight in kg (>10).", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val heightM = heightCm / 100f
                val bmi = weightKg / (heightM * heightM)
                val suggestion = getWorkoutSuggestionByBMI(bmi)

                binding.tvWorkoutSuggestions.text =
                    "BMI: %.1f\nSuggested Workouts:\n$suggestion".format(bmi)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    private fun getWorkoutSuggestionByBMI(bmi: Float): String {
        return when {
            bmi < 18.5 -> "Yoga: 🧘 Tree Pose, Sun Salutation\nStrength: 🏋️ Light Dumbbell Press"
            bmi in 18.5..24.9 -> "Cardio: 🏃 Running, Cycling\nHIIT: ⚡ Burpees, High Knees"
            bmi in 25.0..29.9 -> "Cardio: 🏃 Treadmill, Jump Rope\nYoga: 🧘 Downward Dog"
            else -> "Low-Impact Cardio: 🏃 Elliptical, Walking\nYoga: 🧘 Child's Pose, Warrior"  }
    }
    // ✅ Add this helper to update workout minutes
    private fun incrementWorkoutMinutes(minutes: Int) {
        val prefs = requireContext().getSharedPreferences("WorkoutPrefs", Context.MODE_PRIVATE)
        val current = prefs.getInt("workout_minutes", 0)
        prefs.edit { putInt("workout_minutes", current + minutes) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
