@file:Suppress("DEPRECATION")
package com.example.fittrack
import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.*
import com.google.android.gms.fitness.request.OnDataPointListener
import com.google.android.gms.fitness.request.SensorRequest
import java.util.concurrent.TimeUnit
class GoogleFitHelper(private val context: Context) {


    companion object {
        const val GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 1001
        val fitnessOptions: FitnessOptions = FitnessOptions.builder()
            .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.TYPE_STEP_COUNT_CUMULATIVE, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.TYPE_CALORIES_EXPENDED, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.TYPE_DISTANCE_DELTA, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.TYPE_HEART_RATE_BPM, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.TYPE_MOVE_MINUTES, FitnessOptions.ACCESS_READ)
            .build()
    }
    private fun getGoogleAccount(): GoogleSignInAccount {
        return GoogleSignIn.getAccountForExtension(context, fitnessOptions)
    }
    fun hasPermissions(): Boolean {
        return GoogleSignIn.hasPermissions(getGoogleAccount(), fitnessOptions)
    }
    fun requestPermissions(activity: Activity) {
        GoogleSignIn.requestPermissions(
            activity,
            GOOGLE_FIT_PERMISSIONS_REQUEST_CODE,
            getGoogleAccount(),
            fitnessOptions
        )
    }
    fun readDailySteps(callback: (Int) -> Unit) {
        Fitness.getHistoryClient(context, getGoogleAccount())
            .readDailyTotal(DataType.TYPE_STEP_COUNT_DELTA)
            .addOnSuccessListener { total ->
                val steps = total.dataPoints.firstOrNull()
                    ?.getValue(Field.FIELD_STEPS)?.asInt() ?: 0
                callback(steps)
            }
            .addOnFailureListener {
                Log.e("GoogleFit", "Error reading steps", it)
                callback(0)
            }
    }
    fun calculateCaloriesFromSteps(steps: Int): Float {
        return steps * 0.04f
    }
    fun readCalories(callback: (Float) -> Unit) {
        Fitness.getHistoryClient(context, getGoogleAccount())
            .readDailyTotal(DataType.TYPE_CALORIES_EXPENDED)
            .addOnSuccessListener { total ->
                val calories = total.dataPoints.firstOrNull()
                    ?.getValue(Field.FIELD_CALORIES)?.asFloat() ?: 0f
                callback(calories)
            }
            .addOnFailureListener {
                Log.e("GoogleFit", "Error reading calories", it)
                callback(0f)
            }
    }
    fun readDistance(callback: (Float) -> Unit) {
        Fitness.getHistoryClient(context, getGoogleAccount())
            .readDailyTotal(DataType.TYPE_DISTANCE_DELTA)
            .addOnSuccessListener { total ->
                val meters = total.dataPoints.firstOrNull()
                    ?.getValue(Field.FIELD_DISTANCE)?.asFloat() ?: 0f
                callback(meters)
            }
            .addOnFailureListener {
                Log.e("GoogleFit", "Error reading distance", it)
                callback(0f)
            }
    }
    fun readActiveMinutes(callback: (Int) -> Unit) {
        Fitness.getHistoryClient(context, getGoogleAccount())
            .readDailyTotal(DataType.TYPE_MOVE_MINUTES)
            .addOnSuccessListener { total ->
                val minutes = total.dataPoints.firstOrNull()
                    ?.getValue(Field.FIELD_DURATION)?.asInt()
                    ?: total.dataPoints.firstOrNull()?.getValue(Field.FIELD_INTENSITY)?.asInt()
                    ?: 0
                callback(minutes)
            }
            .addOnFailureListener {
                Log.e("GoogleFit", "Error reading active minutes", it)
                callback(0)
            }
    }

    // ===== Real-Time Listeners =====
    private var stepListener: OnDataPointListener? = null
    private var calorieListener: OnDataPointListener? = null

    fun startStepListener(onStepChanged: (Int) -> Unit) {
        stopStepListener()
        stepListener = OnDataPointListener { dp ->
            dp.dataType.fields.forEach {
                val steps = dp.getValue(it).asInt()
                onStepChanged(steps)
            }
        }
        val request = SensorRequest.Builder()
            .setDataType(DataType.TYPE_STEP_COUNT_DELTA)
            .setSamplingRate(5, TimeUnit.SECONDS)
            .build()

        Fitness.getSensorsClient(context, getGoogleAccount())
            .add(request, stepListener!!)
            .addOnFailureListener {
                Log.e("GoogleFit", "Step listener error", it)
            }
    }
    fun startCalorieListener(onCaloriesChanged: (Float) -> Unit) {
        stopCalorieListener()
        calorieListener = OnDataPointListener { dp ->
            dp.dataType.fields.forEach {
                val cal = dp.getValue(it).asFloat()
                onCaloriesChanged(cal)
            }
        }
        val request = SensorRequest.Builder()
            .setDataType(DataType.TYPE_CALORIES_EXPENDED)
            .setSamplingRate(10, TimeUnit.SECONDS)
            .build()

        Fitness.getSensorsClient(context, getGoogleAccount())
            .add(request, calorieListener!!)
            .addOnFailureListener {
                Log.e("GoogleFit", "Calorie listener error", it)
            }
    }

    fun stopAllListeners() {
        stopStepListener()
        stopCalorieListener()
    }

    private fun stopStepListener() {
        stepListener?.let {
            Fitness.getSensorsClient(context, getGoogleAccount()).remove(it)
        }
        stepListener = null
    }

    private fun stopCalorieListener() {
        calorieListener?.let {
            Fitness.getSensorsClient(context, getGoogleAccount()).remove(it)
        }
        calorieListener = null
    }

}
