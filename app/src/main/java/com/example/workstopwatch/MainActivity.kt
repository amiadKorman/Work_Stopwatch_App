package com.example.workstopwatch

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.Editable
import android.text.TextWatcher
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    // timers views
    private lateinit var tvCurrentLapTime: TextView
    private lateinit var tvLastLapTime: TextView
    private lateinit var tvElapsedTime: TextView

    // buttons
    private lateinit var btnStart: Button
    private lateinit var btnPause: Button
    private lateinit var btnReset: Button
    private lateinit var btnLap: Button

    // lap views
    private lateinit var tvLapCounter: TextView
    private lateinit var tvAverageLap: TextView
    private lateinit var etRemainTime: EditText
    private lateinit var tvFastestLap: TextView

    // workers view
    private lateinit var etBox: EditText
    private lateinit var etUnit: EditText
    private lateinit var tvOverallUnits: TextView
    private lateinit var etWorkers: EditText
    private lateinit var tvCurrentMoney: TextView
    private lateinit var tvEstimatedHourMoney: TextView
    private lateinit var tvEstimatedDayMoney: TextView

    // handler
    private lateinit var handler: Handler

    // vibrator
    private lateinit var vibrator: Vibrator

    // variables
    private var isRunning = false
    private var startTime: Long = 0
    private var startLapTime: Long = 0
    private var elapsedTime: Long = 0
    private var elapsedLapTime: Long = 0
    private var lastLapTime: Long = 0
    private var fastestLapTime: Long = Long.MAX_VALUE
    private var lapCount: Int = 0
    private var totalLapTime: Long = 0
    private var box: Int = 0
    private var unit: Double = 0.0
    private var overallUnits: Int = 0
    private var workers: Int = 0
    private var remainingTime: Long = 0
    private var overallTime: Long = 0
    private var minutesInput: Long? = 0
    private var averageLapTime: Long = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val currentLanguage = Locale.getDefault().language
        if (currentLanguage == "he" || currentLanguage == "iw") {
            window.decorView.rootView.layoutDirection = View.LAYOUT_DIRECTION_RTL
        } else {
            window.decorView.rootView.layoutDirection = View.LAYOUT_DIRECTION_LTR
        }

        // timers views
        tvCurrentLapTime = findViewById(R.id.tvCurrentLapTime)
        tvLastLapTime = findViewById(R.id.tvLastLapTime)
        tvElapsedTime = findViewById(R.id.tvElapsedTime)
        // buttons
        btnStart = findViewById(R.id.btnStart)
        btnPause = findViewById(R.id.btnPause)
        btnReset = findViewById(R.id.btnReset)
        btnLap = findViewById(R.id.btnLap)
        // lap views
        tvLapCounter = findViewById(R.id.tvLapCounter)
        tvAverageLap = findViewById(R.id.tvAverageLap)
        etRemainTime = findViewById(R.id.etRemainTime)
        tvFastestLap = findViewById(R.id.tvFastestLap)
        // workers view
        etBox = findViewById(R.id.etBox)
        etUnit = findViewById(R.id.etUnit)
        tvOverallUnits = findViewById(R.id.tvOverallUnits)
        etWorkers = findViewById(R.id.etWorkers)
        tvCurrentMoney = findViewById(R.id.tvCurrentMoney)
        tvEstimatedHourMoney = findViewById(R.id.tvEstimatedHourMoney)
        tvEstimatedDayMoney = findViewById(R.id.tvEstimatedDayMoney)
        // handler
        handler = Handler(Looper.getMainLooper())
        // initialize vibrator
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        // buttons visibility
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // No action needed
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Check if both EditText fields are filled
                val isInput1Filled = etRemainTime.text.isNotBlank()
                val isInput2Filled = etWorkers.text.isNotBlank()
                val isInput3Filled = etBox.text.isNotBlank()
                val isInput4Filled = etUnit.text.isNotBlank()

                // Enable or disable the button based on both inputs
                if (isInput1Filled && isInput2Filled && isInput3Filled && isInput4Filled) {
                    btnStart.isEnabled = true
                    btnStart.setBackgroundColor(getColor(R.color.green))
                } else {
                    btnStart.isEnabled = false
                }
            }

            override fun afterTextChanged(s: Editable?) {
                // No action needed
            }
        }

        etRemainTime.addTextChangedListener(textWatcher)
        etWorkers.addTextChangedListener(textWatcher)
        etBox.addTextChangedListener(textWatcher)
        etUnit.addTextChangedListener(textWatcher)

        etWorkers.setText(getString(R.string.default_unit_ils))
        etRemainTime.setText(getString(R.string.default_box_units))
    }

    fun onStartClicked(view: View) {
        if (!isRunning) {
            startTime = System.currentTimeMillis()
            startLapTime = startTime
            lastLapTime = 0
            // Start the timers
            handler.postDelayed(updateTimer, 0)
            handler.postDelayed(updateCurrentLapTimer, 0)
            isRunning = true
            btnStart.visibility = View.GONE
            btnReset.visibility = View.GONE
            btnLap.visibility = View.VISIBLE
            btnPause.visibility = View.VISIBLE
            etWorkers.isEnabled = false
            etRemainTime.isEnabled = false
            etBox.isEnabled = false
            etUnit.isEnabled = false

            if (btnStart.text == getString(R.string.start)) {
                unit = etUnit.text.toString().toDoubleOrNull() ?: 0.0
                box = etBox.text.toString().toIntOrNull() ?: 0
                workers = etWorkers.text.toString().toIntOrNull() ?: 0
                minutesInput = etRemainTime.text.toString().toLongOrNull()
                overallTime = minutesInput?.times(60_000) ?: 0
                remainingTime = minutesInput?.times(60_000) ?: 0
            }

            // Trigger vibration when the button is clicked
            vibratePhone()
        }
    }

    fun onPauseClicked(view: View) {
        if (isRunning) {
            // Stop the timers
            handler.removeCallbacks(updateTimer)
            handler.removeCallbacks(updateCurrentLapTimer)

            // Update the elapsed time
            val pauseTime = System.currentTimeMillis()
            elapsedTime += pauseTime - startTime
            elapsedLapTime += pauseTime - startLapTime

            isRunning = false
            btnStart.visibility = View.VISIBLE
            btnPause.visibility = View.GONE
            btnLap.visibility = View.GONE
            btnReset.visibility = View.VISIBLE
            btnStart.text = getString(R.string.resume)

            // Trigger vibration when the button is clicked
            vibratePhone()
        }
    }

    fun onResetClicked(view: View) {
        // Stop the timers
        handler.removeCallbacks(updateTimer)
        handler.removeCallbacks(updateCurrentLapTimer)

        // Reset fields
        isRunning = false
        elapsedTime = 0
        elapsedLapTime = 0
        lastLapTime = 0
        fastestLapTime = Long.MAX_VALUE
        lapCount = 0
        totalLapTime = 0

        // Clear input fields
        etRemainTime.text.clear()
        etWorkers.text.clear()

        etBox.setText(getString(R.string.default_box_units))
        etUnit.setText(getString(R.string.default_unit_ils))

        etWorkers.setText(getString(R.string.default_unit_ils))
        etRemainTime.setText(getString(R.string.default_box_units))

        // Reset visibility and text of buttons
        btnStart.visibility = View.VISIBLE
        btnPause.visibility = View.GONE
        btnLap.visibility = View.GONE
        btnReset.visibility = View.GONE
        btnStart.text = getString(R.string.start)

        // Enable input fields
        etWorkers.isEnabled = true
        etRemainTime.isEnabled = true
        etBox.isEnabled = true
        etUnit.isEnabled = true

        // Reset button color
        btnStart.background.clearColorFilter()

        // Reset UI views
        tvCurrentLapTime.text = getString(R.string.millis_timer)
        tvLastLapTime.text = getString(R.string.empty_timer)
        tvElapsedTime.text = getString(R.string.elapsed_timer)
        tvLapCounter.text = getString(R.string.empty_value)
        tvAverageLap.text = getString(R.string.empty_timer)
        tvFastestLap.text = getString(R.string.empty_timer)
        tvOverallUnits.text = getString(R.string.empty_value)
        tvCurrentMoney.text = getString(R.string.empty_value)
        tvEstimatedHourMoney.text = getString(R.string.empty_value)
        tvEstimatedDayMoney.text = getString(R.string.empty_value)

        // Trigger vibration when the button is clicked
        vibratePhone()
    }

    fun onLapClicked(view: View) {
        if (isRunning) {
            startLapTime = System.currentTimeMillis()

            val relativeLapTime = startLapTime - startTime - lastLapTime + elapsedLapTime
            lastLapTime = startLapTime - startTime

            updateLastLap(relativeLapTime)
            updateLapCounter()
            updateAverageLap(relativeLapTime)
            updateFastestLap(relativeLapTime)
            updateOverallUnitsAmount()
            updateCurrentMoney()
            updateEstimatedMoney()

            elapsedLapTime = 0

            // Trigger vibration when the button is clicked
            vibratePhone()
        }
    }

    private fun updateLastLap(lapTime: Long) {
        val lapTimeString = formatTime(lapTime)
        tvLastLapTime.text = lapTimeString
    }

    private fun updateLapCounter() {
        lapCount++
        tvLapCounter.text = "$lapCount"
    }

    private fun updateAverageLap(lapTime: Long) {21
        totalLapTime += lapTime
        averageLapTime = totalLapTime / lapCount
        tvAverageLap.text = formatTime(averageLapTime)
    }

    private fun updateFastestLap(lapTime: Long) {
        if (lapTime < fastestLapTime) {
            fastestLapTime = lapTime
            tvFastestLap.text = formatTime(fastestLapTime)
        }
    }

    private fun updateOverallUnitsAmount() {
        overallUnits = lapCount * box
        tvOverallUnits.text = "$overallUnits"
    }

    private fun updateCurrentMoney() {
        if (lapCount == 0 || box == 0 || unit == 0.0) {
            tvCurrentMoney.text = getString(R.string.empty_value)
            return
        }
        val currentMoney = lapCount * box * unit / workers
        val decimalFormat = DecimalFormat("#.###")
        decimalFormat.decimalFormatSymbols = DecimalFormatSymbols.getInstance(Locale.getDefault())
        decimalFormat.isDecimalSeparatorAlwaysShown = false
        tvCurrentMoney.text = decimalFormat.format(currentMoney)
    }

    private fun updateEstimatedMoney() {
        if (averageLapTime == 0L || workers == 0 || box == 0 || unit == 0.0) {
            tvEstimatedHourMoney.text = getString(R.string.empty_value)
            tvEstimatedDayMoney.text = getString(R.string.empty_value)
            return
        }
        val estimatedHourLaps = 3600000 / averageLapTime.toDouble()
        val estimatedDayLaps = overallTime.toDouble() / averageLapTime.toDouble()

        val estimatedHourMoney = estimatedHourLaps * box.toDouble() * unit / workers.toDouble()
        val estimatedDayMoney = estimatedDayLaps * box.toDouble() * unit / workers.toDouble()

        tvEstimatedHourMoney.text = String.format("%.3f", estimatedHourMoney)
        tvEstimatedDayMoney.text = String.format("%.3f", estimatedDayMoney)
    }

    private val updateTimer = object : Runnable {
        override fun run() {
            val currentTime = System.currentTimeMillis()
            val updatedElapsedTime = elapsedTime + currentTime - startTime
            tvElapsedTime.text = formatTime(updatedElapsedTime, false)

            val elapsedMinutes = TimeUnit.MILLISECONDS.toMinutes(updatedElapsedTime)
            remainingTime = minutesInput?.minus(elapsedMinutes)?.times(60_000) ?: 0

            if (remainingTime < 0) {
                // Timer reached 0, stop the timer
                handler.removeCallbacks(this)
            } else {
                // Update the remaining time
                val remainingTimeString = formatTime(remainingTime, false)
                etRemainTime.setText(remainingTimeString)
                //handler.postDelayed(this, 0)
            }

            handler.postDelayed(this, 0)
        }
    }

    private val updateCurrentLapTimer = object : Runnable {
        override fun run() {
            val currentTime = System.currentTimeMillis()
            val currentLapTime = elapsedLapTime + currentTime - startLapTime
            tvCurrentLapTime.text = formatTime(currentLapTime)

            handler.postDelayed(this, 0)
        }
    }

    private fun formatTime(timeInMillis: Long, includeMilliseconds: Boolean = true): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeInMillis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(timeInMillis) % 60

        return if (includeMilliseconds) {
            val milliseconds = (timeInMillis % 1000) / 10
            String.format("%02d:%02d.%02d", minutes, seconds, milliseconds)
        } else {
            val hours = TimeUnit.MILLISECONDS.toHours(timeInMillis)
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("isRunning", isRunning)
        outState.putLong("startTime", startTime)
        outState.putLong("startLapTime", startLapTime)
        outState.putLong("elapsedTime", elapsedTime)
        outState.putLong("elapsedLapTime", elapsedLapTime)
        outState.putLong("lastLapTime", lastLapTime)
        outState.putLong("fastestLapTime", fastestLapTime)
        outState.putInt("lapCount", lapCount)
        outState.putLong("totalLapTime", totalLapTime)
        outState.putInt("box", box)
        outState.putDouble("unit", unit)
        outState.putInt("overallUnits", overallUnits)
        outState.putInt("workers", workers)
        outState.putLong("remainingTime", remainingTime)
        outState.putLong("overallTime", overallTime)
        outState.putLong("minutesInput", minutesInput ?: 0)
        outState.putLong("averageLapTime", averageLapTime)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        isRunning = savedInstanceState.getBoolean("isRunning")
        startTime = savedInstanceState.getLong("startTime")
        startLapTime = savedInstanceState.getLong("startLapTime")
        elapsedTime = savedInstanceState.getLong("elapsedTime")
        elapsedLapTime = savedInstanceState.getLong("elapsedLapTime")
        lastLapTime = savedInstanceState.getLong("lastLapTime")
        fastestLapTime = savedInstanceState.getLong("fastestLapTime")
        lapCount = savedInstanceState.getInt("lapCount")
        totalLapTime = savedInstanceState.getLong("totalLapTime")
        box = savedInstanceState.getInt("box")
        unit = savedInstanceState.getDouble("unit")
        overallUnits = savedInstanceState.getInt("overallUnits")
        workers = savedInstanceState.getInt("workers")
        remainingTime = savedInstanceState.getLong("remainingTime")
        overallTime = savedInstanceState.getLong("overallTime")
        minutesInput = savedInstanceState.getLong("minutesInput")
        averageLapTime = savedInstanceState.getLong("averageLapTime")

        if (isRunning) {
            handler.postDelayed(updateTimer, 0)
            handler.postDelayed(updateCurrentLapTimer, 0)
            btnStart.visibility = View.GONE
            btnReset.visibility = View.GONE
            btnLap.visibility = View.VISIBLE
            btnPause.visibility = View.VISIBLE
        } else {
            btnStart.text = getString(R.string.resume)
            btnStart.visibility = View.VISIBLE
            btnLap.visibility = View.GONE
            btnReset.visibility = View.VISIBLE
            etWorkers.isEnabled = false
            etRemainTime.isEnabled = false
            etBox.isEnabled = false
            etUnit.isEnabled = false
        }

        // Update the UI based on the restored state
        tvCurrentLapTime.text = formatTime(elapsedLapTime)
        tvLastLapTime.text = formatTime(lastLapTime)
        tvElapsedTime.text = formatTime(elapsedTime, false)
        tvLapCounter.text = lapCount.toString()
        tvAverageLap.text = formatTime(averageLapTime)
        etRemainTime.setText(formatTime(remainingTime, false))
        tvFastestLap.text = formatTime(fastestLapTime)
        tvOverallUnits.text = overallUnits.toString()
        etWorkers.setText(workers.toString())
        val decimalFormat = DecimalFormat("#.###")
        decimalFormat.decimalFormatSymbols = DecimalFormatSymbols.getInstance(Locale.getDefault())
        decimalFormat.isDecimalSeparatorAlwaysShown = false
        tvCurrentMoney.text = String.format(decimalFormat.format(lapCount * box * unit / workers))
        tvEstimatedHourMoney.text = String.format(
            "%.3f",
            3600000 / averageLapTime.toDouble() * box.toDouble() * unit / workers.toDouble()
        )
        tvEstimatedDayMoney.text = String.format(
            "%.3f",
            overallTime.toDouble() / averageLapTime.toDouble() * box.toDouble() * unit / workers.toDouble()
        )
    }

    // Function to trigger vibration
    private fun vibratePhone() {
        vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
    }

}
