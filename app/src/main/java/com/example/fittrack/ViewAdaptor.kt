package com.example.fittrack

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.fittrack.ui.fragments.SummaryFragment
import com.fittrack.ui.fragments.GoalsFragment
import com.fittrack.ui.fragments.WorkoutsFragment
import com.fittrack.ui.fragments.ReminderFragment
import com.fittrack.ui.fragments.ChallengeFragment
import com.fittrack.ui.fragments.SleepFragment
import com.fittrack.ui.fragments.SettingsFragment


class ViewPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 7
    val summaryFragment = SummaryFragment()
    private val goalsFragment = GoalsFragment()
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> summaryFragment
            1 -> goalsFragment
            2 -> SleepFragment()
            3 -> WorkoutsFragment()
            4 -> ChallengeFragment()
            5 -> ReminderFragment()
            6 -> SettingsFragment()
            else -> SummaryFragment()
        }
    }
}

