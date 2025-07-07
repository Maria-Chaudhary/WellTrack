package com.fittrack.ui.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.*
import android.util.Log
import android.util.Size
import android.view.*
import android.widget.*
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.work.*
import com.example.fittrack.GoogleFitHelper
import com.example.fittrack.R
import com.example.fittrack.DailyRefreshWorker
import com.google.android.material.progressindicator.CircularProgressIndicator
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

@Suppress("DEPRECATION")
class SummaryFragment : Fragment() {

    private lateinit var fitHelper: GoogleFitHelper
    private lateinit var stepsProgressBar: CircularProgressIndicator
    private lateinit var tvSteps: TextView
    private lateinit var tvCalories: TextView
    private lateinit var tvDistance: TextView
    private lateinit var tvActiveTime: TextView
    private lateinit var tvHeartRate: TextView
    private lateinit var previewView: PreviewView
    private lateinit var btnRefresh: ImageButton
    private lateinit var tvMotivation: TextView

    private lateinit var cameraExecutor: ExecutorService
    private val redIntensityList = mutableListOf<Float>()
    private var stepsGoal = 10000
    private var fingerDetected = false
    private var fingerTimer: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_summary, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        fitHelper = GoogleFitHelper(requireContext())

        stepsProgressBar = view.findViewById(R.id.stepsProgressBar)
        tvSteps = view.findViewById(R.id.tvSteps)
        tvCalories = view.findViewById(R.id.tvCalories)
        tvDistance = view.findViewById(R.id.tvDistance)
        tvActiveTime = view.findViewById(R.id.tvActiveTime)
        tvHeartRate = view.findViewById(R.id.tvHeartRate)
        previewView = view.findViewById(R.id.previewView)
        btnRefresh = view.findViewById(R.id.btnRefresh)
        tvMotivation = view.findViewById(R.id.tvMotivation)
        cameraExecutor = Executors.newSingleThreadExecutor()
        btnRefresh.setOnClickListener {
            resetTrackingData()
        }
        previewView.visibility = View.GONE
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startInvisibleCameraForHeartRate()
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.CAMERA),
                1
            )
        }
        if (fitHelper.hasPermissions()) {
            fetchGoogleFitData()
            startRealTimeFitListeners()
        } else {
            fitHelper.requestPermissions(requireActivity())
        }
        scheduleDailyRefresh()
    }
    private fun startInvisibleCameraForHeartRate() {
        redIntensityList.clear()
        previewView.visibility = View.GONE
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val analyzer = ImageAnalysis.Builder()
                .setTargetResolution(Size(640, 480))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build().also {
                    it.setAnalyzer(cameraExecutor, ::analyzeHeartRate)
                }
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(viewLifecycleOwner, cameraSelector, analyzer)
            } catch (e: Exception) {
                Log.e("HeartRate", "Camera bind failed: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun analyzeHeartRate(imageProxy: ImageProxy) {
        val buffer = imageProxy.planes[0].buffer
        val data = ByteArray(buffer.remaining())
        buffer.get(data)
        imageProxy.close()
        val avgIntensity = data.map { it.toUByte().toInt() }.average().toFloat()
        redIntensityList.add(avgIntensity)
        if (redIntensityList.size > 60) redIntensityList.removeAt(0)

        if (avgIntensity < 100f && !fingerDetected) {
            fingerDetected = true
            showCameraPreviewTemporarily()
        } else if (avgIntensity > 150f && fingerDetected) {
            fingerDetected = false
            hideCameraPreview() }
        if (redIntensityList.size == 60) {
            val mean = redIntensityList.average().toFloat()
            val peaks = redIntensityList.windowed(3).count {
                it[1] > it[0] && it[1] > it[2] && it[1] > mean }
            val bpm = (peaks / 4f) * 60f
            Log.d("HeartRate", "Detected BPM: $bpm")
            updateHeartRateUI(bpm)
            redIntensityList.clear()
        }
    }

    private fun showCameraPreviewTemporarily() {
        previewView.post {
            previewView.visibility = View.VISIBLE

            fingerTimer?.cancel()
            fingerTimer = lifecycleScope.launch {
                delay(5000)
                previewView.visibility = View.GONE
            }
        }
    }

    private fun hideCameraPreview() {
        previewView.post {
            previewView.visibility = View.GONE
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateHeartRateUI(bpm: Float) {
        if (!isAdded || view == null) return
        requireActivity().runOnUiThread {
            tvHeartRate.text = "${bpm.roundToInt()} bpm"
        }
    }

    fun fetchGoogleFitData() {
        fitHelper.readDailySteps {
            updateStepsUI(it)
            updateCaloriesUI(fitHelper.calculateCaloriesFromSteps(it))
        }
        fitHelper.readDistance { updateDistanceUI(it) }
        fitHelper.readActiveMinutes { updateActiveMinutesUI(it) }
    }

    private fun startRealTimeFitListeners() {
        fitHelper.startStepListener {
            updateStepsUI(it)
            updateCaloriesUI(fitHelper.calculateCaloriesFromSteps(it))
        }
        fitHelper.startCalorieListener {
            updateCaloriesUI(it)
        }
    }
    @SuppressLint("SetTextI18n")
    private fun updateStepsUI(steps: Int) {
        if (!isAdded || view == null) return
        requireActivity().runOnUiThread {
            stepsProgressBar.max = stepsGoal  // Set the goal as the progress bar's max
            stepsProgressBar.setProgressCompat(steps.coerceAtMost(stepsGoal), true)

            // Optional: Change color if steps > 0
            val colorRes = if (steps > 0)
                R.color.green_500
            else
                R.color.white

            stepsProgressBar.setIndicatorColor(
                ContextCompat.getColor(requireContext(), colorRes)
            )

            tvSteps.text = "$steps\nsteps" }
    }
    @SuppressLint("SetTextI18n")
    private fun updateCaloriesUI(calories: Float) {
        if (!isAdded || view == null) return
        requireActivity().runOnUiThread {
            tvCalories.text = "${calories.toInt()} kcal"
        }
    }

    private fun updateDistanceUI(distance: Float) {
        if (!isAdded || view == null) return
        requireActivity().runOnUiThread {
            tvDistance.text = String.format(Locale.getDefault(), "%.2f mi", distance / 1609.34f)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateActiveMinutesUI(minutes: Int) {
        if (!isAdded || view == null) return
        requireActivity().runOnUiThread {
            tvActiveTime.text = "$minutes min"
        }
    }

    private fun resetTrackingData() {
        try {
            fitHelper.stopAllListeners()
        } catch (_: Exception) {
        }
        updateStepsUI(0)
        updateCaloriesUI(0f)
        updateDistanceUI(0f)
        updateActiveMinutesUI(0)
        updateHeartRateUI(0f)
        Handler(Looper.getMainLooper()).postDelayed({
            if (fitHelper.hasPermissions()) {
                fetchGoogleFitData()
                startRealTimeFitListeners()
            }
        }, 1000)
    }

    private fun scheduleDailyRefresh() {
        val workRequest = PeriodicWorkRequestBuilder<DailyRefreshWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(1, TimeUnit.HOURS)
            .build()

        WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
            "DailySummaryRefresh",
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        try {
            fitHelper.stopAllListeners()
        } catch (_: Exception) {
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startInvisibleCameraForHeartRate()
        }
    }
}
