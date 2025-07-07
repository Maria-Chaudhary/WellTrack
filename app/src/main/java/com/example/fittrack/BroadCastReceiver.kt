package com.example.fittrack


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BroadCastReceiver(private val onRefresh: () -> Unit) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == ACTION_REFRESH_GOALS) {
            onRefresh()
        }
    }

    companion object {
        const val ACTION_REFRESH_GOALS = "com.fittrack.ACTION_REFRESH_GOALS"
    }
}

