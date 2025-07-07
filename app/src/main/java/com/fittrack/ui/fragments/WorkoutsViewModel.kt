package com.fittrack.ui.fragments
import androidx.lifecycle.ViewModel
class WorkoutsViewModel : ViewModel() {
    val strengthList = listOf(
        "Push-ups",
        "Dumbbell Bench Press",
        "Barbell Squats",
        "Deadlifts",
        "Shoulder Press")
    val hiitList = listOf(
        "Jumping Jacks",
        "Burpees",
        "Mountain Climbers",
        "High Knees",
        "Lunges with Jumps")
    val yogaList = listOf(
        "Sun Salutation",
        "Tree Pose",
        "Downward Dog",
        "Warrior Pose",
        "Child's Pose")
    val cardioList = listOf(
        "Treadmill Running",
        "Cycling",
        "Skipping Rope",
        "Rowing Machine",
        "Elliptical Trainer")
}
