package com.fittrack.ui.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.os.*
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.fittrack.GoogleFitHelper
import com.example.fittrack.R
import com.google.android.material.progressindicator.CircularProgressIndicator
import java.util.*

class ChallengeFragment : Fragment() {
    private lateinit var googleFitHelper: GoogleFitHelper
    private lateinit var challengeProgress: CircularProgressIndicator
    private lateinit var progressPercentText: TextView
    private lateinit var challengeProgressText: TextView
    private lateinit var challengeTitle: TextView
    private lateinit var challengeDescription: TextView
    private lateinit var congratulationsText: TextView
    private lateinit var nextChallengeText: TextView
    private lateinit var currentChallenge: DailyChallenge
    private val handler = Handler(Looper.getMainLooper())
    data class DailyChallenge(val title: String, val target: Float, val type: String, val description: String)
    private val challenges = listOf(
        DailyChallenge("Burn 500 Calories", 500f, "calories", "Burn 500 calories today by staying active."),
        DailyChallenge("Walk 6000 Steps", 6000f, "steps", "Walk at least 6000 steps today."),
        DailyChallenge("Walk 3 KM", 3000f, "distance", "Walk or run 3 kilometers today."),
        DailyChallenge("Burn 700 Calories", 700f, "calories", "Burn 700 calories with workout or sports."),
        DailyChallenge("Walk 8000 Steps", 8000f, "steps", "Walk at least 8000 steps today."),
        DailyChallenge("Walk 5 KM", 5000f, "distance", "Walk or run 5 kilometers today.")
    )
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_challenge, container, false)
        if (!isAdded) return view
        googleFitHelper = GoogleFitHelper(requireContext())
        challengeProgress = view.findViewById(R.id.challengeProgress)
        progressPercentText = view.findViewById(R.id.progressPercentText)
        challengeProgressText = view.findViewById(R.id.challengeProgressText)
        challengeTitle = view.findViewById(R.id.challengeTitle)
        challengeDescription = view.findViewById(R.id.challengeDescription)
        congratulationsText = view.findViewById(R.id.congratulationsText)
        nextChallengeText = view.findViewById(R.id.nextChallengeText)
        loadTodayChallenge()
        startRealTimeTracking()
        return view
    }

    private fun loadTodayChallenge() {
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val index = dayOfYear % challenges.size
        currentChallenge = challenges[index]

        if (!isAdded || view == null) return
        challengeTitle.text = currentChallenge.title
        challengeDescription.text = currentChallenge.description
    }

    private fun startRealTimeTracking() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (!isAdded || context == null) return
                when (currentChallenge.type) {
                    "steps" -> googleFitHelper.readDailySteps { steps ->
                        if (isAdded && view != null) {
                            updateProgress(steps.toFloat(), currentChallenge.target, "steps")
                        }
                    }
                    "calories" -> googleFitHelper.readCalories { calories ->
                        if (isAdded && view != null) {
                            updateProgress(calories, currentChallenge.target, "kcal")
                        }
                    }
                    "distance" -> googleFitHelper.readDistance { meters ->
                        if (isAdded && view != null) {
                            updateProgress(meters, currentChallenge.target, "m")
                        }
                    }
                }
                handler.postDelayed(this, 10_000)
            }
        }, 0)
    }

    @SuppressLint("SetTextI18n")
    private fun updateProgress(value: Float, target: Float, unit: String) {
        if (!isAdded || view == null) return

        val percent = (value / target * 100).coerceAtMost(100f)
        challengeProgress.setProgressCompat(percent.toInt(), true)
        progressPercentText.text = "${percent.toInt()}%"

        val displayValue = when (unit) {
            "steps" -> "${value.toInt()} / ${target.toInt()} steps"
            "kcal" -> "%.0f / %.0f kcal".format(value, target)
            "m" -> "%.2f / %.2f km".format(value / 1000f, target / 1000f)
            else -> "%.0f / %.0f".format(value, target)
        }
        challengeProgressText.text = displayValue
        if (percent >= 100f && !isAlreadyCompletedToday()) {
            markCompleted() }
        if (isAlreadyCompletedToday()) {
            congratulationsText.visibility = View.VISIBLE
            nextChallengeText.visibility = View.VISIBLE
        } else {
            congratulationsText.visibility = View.GONE
            nextChallengeText.visibility = View.GONE
        } }

    @SuppressLint("UseKtx")
    private fun markCompleted() {
        if (!isAdded || context == null) return
        Toast.makeText(requireContext(), "🎉 Challenge Completed!", Toast.LENGTH_LONG).show()
        val prefs = requireContext().getSharedPreferences("challenge_prefs", Context.MODE_PRIVATE)
        val dayKey = "completed_day_${Calendar.getInstance().get(Calendar.DAY_OF_YEAR)}"
        prefs.edit().putBoolean(dayKey, true).apply()

        if (!isAdded || view == null) return
        congratulationsText.visibility = View.VISIBLE
        nextChallengeText.visibility = View.VISIBLE
    }
    private fun isAlreadyCompletedToday(): Boolean {
        if (!isAdded || context == null) return false
        val prefs = requireContext().getSharedPreferences("challenge_prefs", Context.MODE_PRIVATE)
        val dayKey = "completed_day_${Calendar.getInstance().get(Calendar.DAY_OF_YEAR)}"
        return prefs.getBoolean(dayKey, false)
    }
    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null)
    }
}
