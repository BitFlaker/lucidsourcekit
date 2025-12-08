package com.bitflaker.lucidsourcekit.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Paint.Cap
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.SweepGradient
import android.os.Handler
import android.os.Vibrator
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.bitflaker.lucidsourcekit.R
import com.bitflaker.lucidsourcekit.main.binauralbeats.presets.Brainwaves.Companion.getRemStageAfterAndDuration
import com.bitflaker.lucidsourcekit.main.binauralbeats.presets.Brainwaves.Companion.remAfterMinutes
import com.bitflaker.lucidsourcekit.utils.attrColor
import com.bitflaker.lucidsourcekit.utils.degToRad
import com.bitflaker.lucidsourcekit.utils.dpToPx
import com.bitflaker.lucidsourcekit.utils.getDecimals
import com.bitflaker.lucidsourcekit.utils.getDefaultVibrator
import com.bitflaker.lucidsourcekit.utils.radToDeg
import com.bitflaker.lucidsourcekit.utils.resolveDrawableBitmap
import com.bitflaker.lucidsourcekit.utils.spToPx
import com.bitflaker.lucidsourcekit.utils.vibrateFor
import java.util.Calendar
import java.util.Locale
import java.util.Timer
import java.util.TimerTask
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

class SleepClock : View {
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context) : super(context)

    private var innerRadiusOffset = 0f
    private val timeSetterButtonRadius = 18f.dpToPx
    private val digitToButtonSpace = 5.dpToPx
    private val dataLinePaint = Paint()
    private val dataLinePaintStroker = Paint()
    private val dataLinePaintREM = Paint()
    private val dataLinePaintFocus = Paint()
    private val dataLinePaintButton = Paint()
    private val dataHandPaintMinute = Paint()
    private val dataHandPaintHour = Paint()
    private val dataHandPaintSeconds = Paint()
    private val dataDigitPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val dataHourPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val viewBounds = RectF()
    private var currentTime: Calendar? = null
    private val clockTimer = Timer()
    private val textBounds = Rect()
    private var angleBedtime = 0.0f
    private var angleAlarm = (0.5 * Math.PI).toFloat()
    private var xBedtime = 0f
    private var yBedtime = 0f
    private var xAlarm = 0f
    private var yAlarm = 0f
    private var currentButton = ButtonType.NONE
    private var gradientShaderREM: SweepGradient? = null
    private var gradientShader: SweepGradient? = null
    private var matrixSweepRotationShader: Matrix? = null
    private val colorSurfaceContainerHigh = context.attrColor(R.attr.colorSurfaceContainerHigh)
    private val colorSurfaceContainer = context.attrColor(R.attr.colorSurfaceContainer)
    private val colorTertiary = context.attrColor(R.attr.colorTertiary)
    private val colorActiveMarker = context.attrColor(R.attr.secondaryTextColor)
    private val colorInactiveMarker = context.attrColor(R.attr.colorSurfaceContainer)
    private val colorPrimary = context.attrColor(R.attr.colorPrimary)
    private val colorAlarmInREM = context.attrColor(R.attr.colorTertiaryContainer)
    private val colorAlarmNotInREM = context.attrColor(R.attr.colorPrimaryContainer)
    private val colorOnAlarmNotInREM = context.attrColor(R.attr.colorOnPrimaryContainer)
    private val colorOnAlarmInREM = context.attrColor(R.attr.colorOnSecondaryContainer)
    private val colorsREMRotator = intArrayOf(colorSurfaceContainerHigh, colorTertiary, colorTertiary, colorSurfaceContainerHigh)
    private val colorsRotator = intArrayOf(colorSurfaceContainerHigh, colorPrimary, colorPrimary, colorSurfaceContainerHigh)
    private val vib: Vibrator = context.getDefaultVibrator()
    private var lastStateWasInREM = true
    private var hoursToSleep = 0
    private var minutesToSleep = 0
    private val minutesToFallAsleep = 10
    private val angleOffset = (360 / 12.0) * Math.PI / 180.0
    private val startAngle = (-3 * angleOffset).toFloat()
    var minutesToBedTime: Int = 0
        private set
    var hoursToBedTime: Int = 0
        private set
    var minutesToAlarm: Int = 0
        private set
    var hoursToAlarm: Int = 0
        private set
    var onBedtimeChangedListener: ((Int, Int) -> Unit)? = null
    var onAlarmTimeChangedListener: ((Int, Int) -> Unit)? = null
    var onFirstDrawFinishedListener: (() -> Unit)? = null
    private var drewAtLeastOnce = false
    private var alarmMarkersInactive: Array<Long>? = null
    private var alarmMarkersActive: Array<Long>? = null
    private var clockType: ClockType = ClockType.DEFAULT

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        setClockType(ClockType.DEFAULT)

        dataLinePaint.color = colorSurfaceContainer
        dataLinePaint.isAntiAlias = true
        dataLinePaint.style = Paint.Style.STROKE
        dataLinePaint.strokeWidth = 2f.dpToPx

        dataLinePaintStroker.color = colorSurfaceContainer
        dataLinePaintStroker.isAntiAlias = true
        dataLinePaintStroker.style = Paint.Style.STROKE
        dataLinePaintStroker.strokeWidth = 2f.dpToPx

        dataLinePaintFocus.color = colorPrimary
        dataLinePaintFocus.isAntiAlias = true
        dataLinePaintFocus.style = Paint.Style.STROKE
        dataLinePaintFocus.strokeWidth = 2f.dpToPx

        dataLinePaintButton.isAntiAlias = true
        dataLinePaintButton.style = Paint.Style.FILL

        dataHandPaintMinute.color = context.attrColor(R.attr.primaryTextColor)
        dataHandPaintMinute.isAntiAlias = true
        dataHandPaintMinute.strokeWidth = 3f.dpToPx
        dataHandPaintMinute.strokeCap = Cap.ROUND

        dataHandPaintHour.color = context.attrColor(R.attr.primaryTextColor)
        dataHandPaintHour.isAntiAlias = true
        dataHandPaintHour.strokeWidth = 2f.dpToPx
        dataHandPaintHour.strokeCap = Cap.ROUND

        dataHandPaintSeconds.color = colorTertiary
        dataHandPaintSeconds.isAntiAlias = true
        dataHandPaintSeconds.strokeWidth = 1f.dpToPx

        dataLinePaintREM.color = colorTertiary
        dataLinePaintREM.isAntiAlias = true
        dataLinePaintREM.style = Paint.Style.STROKE
        dataLinePaintREM.strokeCap = Cap.ROUND
        dataLinePaintREM.strokeWidth = 4f.dpToPx

        dataDigitPaint.color = context.attrColor(R.attr.primaryTextColor)
        dataDigitPaint.textSize = 12f.spToPx
        dataDigitPaint.textAlign = Paint.Align.CENTER
        dataDigitPaint.isAntiAlias = true

        dataHourPaint.color = context.attrColor(R.attr.primaryTextColor)
        dataHourPaint.textSize = 24f.spToPx
        dataHourPaint.textAlign = Paint.Align.CENTER
        dataHourPaint.isAntiAlias = true

        this.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> view.parent.requestDisallowInterceptTouchEvent(true)
                MotionEvent.ACTION_UP -> view.parent.requestDisallowInterceptTouchEvent(false)
            }

            view.onTouchEvent(event)
            true
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Set all sides to be at least as large as the
        // largest side as the clock view area has to be a square
        val largestSide = if (width > height) width else height
        setMinimumWidth(largestSide)
        setMinimumHeight(largestSide)

        // Set default clock variables
        innerRadiusOffset = 20f
        val radius = width * 0.5f
        var currentAngleOffset = angleOffset.toFloat()
        var offset = dataLinePaint.strokeWidth
        var innerRadius = radius - innerRadiusOffset.dpToPx
        var clockFaceCount = 13

        // Update default variables to match time selector values if set
        if (clockType == ClockType.TIME_SELECTORS) {
            innerRadiusOffset = 25f
            currentAngleOffset = (angleOffset / 2.0f).toFloat()
            offset += timeSetterButtonRadius
            innerRadius -= timeSetterButtonRadius + digitToButtonSpace
            clockFaceCount = 25


            // TODO: Move shader initializations to a more reasonable place instead of here,
            //  possibly even somewhere where the width and height changes could be reacted to
            if (gradientShaderREM == null) {
                updateGradientShaders()
            }
        }

        // Update the view bounds
        viewBounds.set(offset, offset, width - offset, height - offset)

        // Draw the base clock outline
        canvas.drawOval(viewBounds, dataLinePaint)

        // Draw clock face labels
        var totalAngle = startAngle + currentAngleOffset
        for (i in 1..<clockFaceCount) {
            val currentNumber = i.toString()
            dataDigitPaint.getTextBounds(currentNumber, 0, currentNumber.length, textBounds)
            val xPos = innerRadius * cos(totalAngle) + radius
            val yPos = innerRadius * sin(totalAngle) + radius - textBounds.exactCenterY()
            canvas.drawText(currentNumber, xPos, yPos, dataDigitPaint)
            totalAngle += currentAngleOffset
        }

        // Draw the clock arms (currentTime is only set when `clockType` is `DEFAULT`)
        currentTime?.let { time ->
            val radiusCenter = 5f.dpToPx
            val hourArmLength = radius - innerRadiusOffset.dpToPx - 25.dpToPx
            val minuteArmLength = radius - innerRadiusOffset.dpToPx - 9.dpToPx
            val secondsArmLength = radius - innerRadiusOffset.dpToPx - 5.dpToPx

            val hour = time.get(Calendar.HOUR)
            val minute = time.get(Calendar.MINUTE)
            val second = time.get(Calendar.SECOND)

            val angleHour = startAngle + currentAngleOffset * (hour + (minute / 60.0))
            val angleMinute = startAngle + currentAngleOffset * (minute / 5.0)
            val angleSeconds = startAngle + currentAngleOffset * (second / 5.0)

            // Draw hours arm
            canvas.drawLine(
                radius,
                radius,
                radius + hourArmLength * cos(angleHour).toFloat(),
                radius + hourArmLength * sin(angleHour).toFloat(),
                dataHandPaintHour
            )

            // Draw minutes arm
            canvas.drawLine(
                radius,
                radius,
                radius + minuteArmLength * cos(angleMinute).toFloat(),
                radius + minuteArmLength * sin(angleMinute).toFloat(),
                dataHandPaintHour
            )

            // Draw seconds arm
            canvas.drawLine(
                radius,
                radius,
                radius + secondsArmLength * cos(angleSeconds).toFloat(),
                radius + secondsArmLength * sin(angleSeconds).toFloat(),
                dataHandPaintSeconds
            )

            // Draw center circle
            canvas.drawCircle(radius, radius, radiusCenter, dataHandPaintHour)
        }

        // Draw markers highlighting alarm timestamps on the clock
        drawMarkers(canvas, offset, alarmMarkersInactive, false)
        drawMarkers(canvas, offset, alarmMarkersActive, true)

        // Draw everything special for the `TIME_SELECTORS` clock type
        if (clockType == ClockType.TIME_SELECTORS) {
            // Draw the duration text between bedtime and alarm time
            val sleepDuration = String.format(Locale.ENGLISH, "%02dh %02dm", hoursToSleep, minutesToSleep)
            dataHourPaint.getTextBounds(sleepDuration, 0, sleepDuration.length, textBounds)
            canvas.drawText(sleepDuration, radius, radius - textBounds.exactCenterY(), dataHourPaint)

            // Get coordinates for buttons
            val innerRadius = radius - timeSetterButtonRadius
            xBedtime = radius + innerRadius * cos(startAngle + angleBedtime)
            yBedtime = radius + innerRadius * sin(startAngle + angleBedtime)
            xAlarm = radius + innerRadius * cos(startAngle + angleAlarm)
            yAlarm = radius + innerRadius * sin(startAngle + angleAlarm)

            // Draw REM markers and check if the alarm is within a REM stage
            var isAlarmWithinREM = false
            for (i in remAfterMinutes.indices) {
                val pair = getRemStageAfterAndDuration(i) ?: continue
                val hoursSinceBedtime = pair.first / 60f
                val remDuration = pair.second / 60f

                // Get angle position for approximate middle of REM stage and full duration sweep angle
                val angleCenterREM = startAngle + angleBedtime + hoursSinceBedtime * currentAngleOffset
                val sweepDurationREM = remDuration * currentAngleOffset

                // Ensure alarm angle is larger than or equal to bedtime to be able to compare more easily
                val calcAngleAlarm = if (angleAlarm < angleBedtime) angleAlarm + 2 * Math.PI.toFloat() else angleAlarm
                val alarmIsAfterREMStart = angleCenterREM - (sweepDurationREM / 2.0f) <= startAngle + calcAngleAlarm
                val alarmIsBeforeREMEnd = angleCenterREM + (sweepDurationREM / 2.0f) >= startAngle + calcAngleAlarm

                // Update flag if the alarm is in any REM stage
                isAlarmWithinREM = isAlarmWithinREM || (alarmIsAfterREMStart && alarmIsBeforeREMEnd)

                val radSweepDurationREM = sweepDurationREM.radToDeg
                val separatorSpaceREM = 2.0f

                // Draw track behind REM marker
                canvas.drawArc(
                    viewBounds,
                    angleCenterREM.radToDeg - (radSweepDurationREM / 2.0f) - separatorSpaceREM,
                    radSweepDurationREM + 2 * separatorSpaceREM,
                    false,
                    dataLinePaintStroker
                )

                // Draw REM marker
                canvas.drawArc(
                    viewBounds,
                    angleCenterREM.radToDeg - (radSweepDurationREM / 2.0f),
                    radSweepDurationREM,
                    false,
                    dataLinePaintREM
                )
            }

            // Shortly vibrate when entering a REM period with the alarm slider
            // TODO: Figure out why I was using an `AsyncTask` here before
            if (isAlarmWithinREM && !lastStateWasInREM) {
                vib.vibrateFor(100)
            }
            lastStateWasInREM = isAlarmWithinREM

            // Draw bedtime grip button
            dataLinePaintButton.setColor(colorAlarmNotInREM)
            canvas.drawCircle(xBedtime, yBedtime, timeSetterButtonRadius, dataLinePaintButton)
            val icon = context.resolveDrawableBitmap(R.drawable.ic_baseline_airline_seat_individual_suite_24, colorOnAlarmNotInREM, 16.dpToPx)!!
            canvas.drawBitmap(icon, xBedtime - icon.width / 2.0f, yBedtime - icon.height / 2.0f, dataLinePaint)

            // Draw alarm time grip button
            val colorBackground = if (isAlarmWithinREM) colorAlarmInREM else colorAlarmNotInREM
            val colorForeground = if (isAlarmWithinREM) colorOnAlarmInREM else colorOnAlarmNotInREM
            dataLinePaintButton.setColor(colorBackground)
            canvas.drawCircle(xAlarm, yAlarm, timeSetterButtonRadius, dataLinePaintButton)
            val alarm = context.resolveDrawableBitmap(R.drawable.ic_baseline_access_alarm_24, colorForeground, 16.dpToPx)!!
            canvas.drawBitmap(alarm, xAlarm - alarm.width / 2.0f, yAlarm - alarm.height / 2.0f, dataLinePaint)
        }

        // Raise first draw event after the first draw with an event listener finished
        if (!drewAtLeastOnce && onFirstDrawFinishedListener != null) {
            drewAtLeastOnce = true
            onFirstDrawFinishedListener?.invoke()
        }
    }

    private fun updateGradientShaders() {
        val angleBetween = angleAlarm - angleBedtime
        val sweepAngleBetweenRad = if (angleBedtime < angleAlarm) angleBetween else 2 * Math.PI.toFloat() + angleBetween
        val sweepEndPercentage = sweepAngleBetweenRad / (2 * Math.PI.toFloat())
        updateTimeValues()

        // Initialize `matrixSweepRotationShader` or update it if bedtime has changed
        // due to bedtime being the current button
        if (matrixSweepRotationShader == null || currentButton == ButtonType.BEDTIME) {
            val rotation = (angleBedtime - 90f.degToRad).radToDeg.toInt()
            matrixSweepRotationShader = (matrixSweepRotationShader ?: Matrix()).apply {
                setRotate(0f)
                postRotate(rotation.toFloat(), width / 2.0f, height / 2.0f)
            }
        }

        // TODO: Instead of creating a new object every call for both gradient shaders,
        //  reuse the old object and transform the matrix to use the new positions.
        //  This is especially requiredn due to the frequent calls from `rotateButtons(...)`

        // Initialize shader for REM stage
        gradientShaderREM = SweepGradient(
            width / 2.0f,
            height / 2.0f,
            colorsREMRotator,
            floatArrayOf(0.0f, 0.0f, sweepEndPercentage, sweepEndPercentage)
        ).apply {
            setLocalMatrix(matrixSweepRotationShader)
        }

        // Initialize shader for the lines in between REM stage markers
        gradientShader = SweepGradient(
            width / 2.0f,
            height / 2.0f,
            colorsRotator,
            floatArrayOf(0.0f, 0.0f, sweepEndPercentage, sweepEndPercentage)
        ).apply {
            setLocalMatrix(matrixSweepRotationShader)
        }

        // Apply the initialized shaders
        dataLinePaintREM.setShader(gradientShaderREM)
        dataLinePaint.setShader(gradientShader)
    }

    private fun drawMarkers(canvas: Canvas, offset: Float, markers: Array<Long>?, isActive: Boolean) {
        if (markers == null || clockType != ClockType.DEFAULT) return

        // Save initial color and stroke cap to restore afterwards
        val initialColor = dataLinePaint.color
        val initialCap = dataLinePaint.strokeCap

        // Set color and stroke cap for markers
        dataLinePaint.color = if (isActive) colorActiveMarker else colorInactiveMarker
        dataLinePaint.strokeCap = Cap.ROUND

        // Draw markers
        val radius = width * 0.5f
        val markerLength = 6f.dpToPx
        for (marker in markers) {
            val offsetRadius = radius - offset

            // Get angle and cos and sin from angle
            val angle = startAngle + (marker / 3_600_000f) * angleOffset
            val angleCos = cos(angle).toFloat()
            val angleSin = sin(angle).toFloat()

            // Calculate start and end positions for markers
            val startX = radius + angleCos * (offsetRadius - markerLength)
            val startY = radius + angleSin * (offsetRadius - markerLength)
            val stopX = radius + angleCos * offsetRadius
            val stopY = radius + angleSin * offsetRadius

            // Draw the final marker line
            canvas.drawLine(startX, startY, stopX, stopY, dataLinePaint)
        }

        // Restore initial color and stroke cap
        dataLinePaint.color = initialColor
        dataLinePaint.strokeCap = initialCap
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> currentButton = getButtonAtPosition(event.x, event.y)
            MotionEvent.ACTION_UP -> currentButton = ButtonType.NONE
        }
        setButtonPosition(event, width / 2f)
        return true
    }

    private fun setButtonPosition(event: MotionEvent, radius: Float) {
        if (currentButton == ButtonType.NONE) return

        // Get the distance of the event to the center of the clock
        val centerDistanceX = event.x - radius
        val centerDistanceY = event.y - radius
        val centerDistance = sqrt(centerDistanceX.pow(2f) + centerDistanceY.pow(2f))

        // Get the angle to the event position
        var angle = Math.PI.toFloat() - acos(centerDistanceY / centerDistance)

        // Adjust the angle in case of a negative X coordinate
        if (event.x < radius) {
            angle = 2 * Math.PI.toFloat() - angle
        }

        // Rotate the current button the calculated angle
        setCurrentButtonAngle(angle)
    }

    private fun setCurrentButtonAngle(angle: Float) {
        // Update the bedtime or alarm time button angles depending on what button is currently active
        if (currentButton == ButtonType.BEDTIME) {
            angleBedtime = angle
        }
        else if (currentButton == ButtonType.ALARM) {
            angleAlarm = angle
        }

        // Update the gradient shaders and redraw with the updated shaders
        updateGradientShaders()
        invalidate()
    }

    private fun updateTimeValues() {
        // Get the amount of hours and minutes until bedtime
        val hoursUntilBedtime: Float = (24 * angleBedtime) / (2 * Math.PI.toFloat())
        hoursToBedTime = hoursUntilBedtime.toInt()
        minutesToBedTime = (hoursUntilBedtime.getDecimals() * 60).roundToInt()

        // Prevent rounding issues causing e.g.: 07:60 and instead correcting it to 08:00 as well as
        // correcting a time of 24:00 to 00:00
        if (minutesToBedTime == 60) {
            minutesToBedTime = 0
            hoursToBedTime++
            if (hoursToBedTime == 24) {
                hoursToBedTime = 0
            }
        }

        // Get the amount of hours and minutes until alarm time
        val hoursUntilAlarm: Float = (24 * angleAlarm) / (2 * Math.PI.toFloat())
        hoursToAlarm = hoursUntilAlarm.toInt()
        minutesToAlarm = (hoursUntilAlarm.getDecimals() * 60).roundToInt()

        // Prevent rounding issues causing e.g.: 07:60 and instead correcting it to 08:00 as well as
        // correcting a time of 24:00 to 00:00
        if (minutesToAlarm == 60) {
            minutesToAlarm = 0
            hoursToAlarm++
            if (hoursToAlarm == 24) {
                hoursToAlarm = 0
            }
        }

        // Get time to events in minutes
        val minutesUntilBedtime = hoursToBedTime * 60 + minutesToBedTime
        val minutesUntilAlarm = hoursToAlarm * 60 + minutesToAlarm

        // Get total minutes between bedtime and alarm
        var totalMinutes = (minutesUntilAlarm - minutesUntilBedtime)
        if (minutesUntilBedtime > minutesUntilAlarm) {
            totalMinutes = minutesUntilAlarm + (24 * 60 - minutesUntilBedtime)
        }

        // Account for time required to fall asleep and prevent negative numbers
        totalMinutes = max(0, totalMinutes - minutesToFallAsleep)

        // Get the amount of hours and minutes of the full sleep duration
        val hoursOfSleep: Float = totalMinutes / 60.0f
        hoursToSleep = hoursOfSleep.toInt()
        minutesToSleep = (hoursOfSleep.getDecimals() * 60).roundToInt()

        // Notify listeners about changes in bedtime and alarm time
        onBedtimeChangedListener?.invoke(hoursToBedTime, minutesToBedTime)
        onAlarmTimeChangedListener?.invoke(hoursToAlarm, minutesToAlarm)
    }

    private fun getButtonAtPosition(x: Float, y: Float): ButtonType {
        val buttonRadiusSq = timeSetterButtonRadius.pow(2f)

        // Get the squared distances in x and y between the alarm button and the provided coordinates
        val distanceSqAlarmX = (x - xAlarm).pow(2f)
        val distanceSqAlarmY = (y - yAlarm).pow(2f)

        // Get the squared distances in x and y between the bedtime button and the provided coordinates
        val distanceSqBedtimeX = (x - xBedtime).pow(2f)
        val distanceSqBedtimeY = (y - yBedtime).pow(2f)

        // Check if the given coordinates are within the range of the button and return the button
        // type which it is inside of. In case it is within both buttons, it returns the alarm button
        if (distanceSqAlarmX + distanceSqAlarmY <= buttonRadiusSq) {
            return ButtonType.ALARM
        } else if (distanceSqBedtimeX + distanceSqBedtimeY <= buttonRadiusSq) {
            return ButtonType.BEDTIME
        }
        return ButtonType.NONE
    }

    /**
     * Starts the cycle of updating the clock time every second. This
     * will cause the clock to display its clock arms. This only works
     * when the `clockType` is `ClockType.DEFAULT`
     */
    private fun startAnalogClock() {
        if (clockType != ClockType.DEFAULT) return

        // Set the current time and immediately invalidate to start drawing clock arms
        currentTime = Calendar.getInstance()
        invalidate()

        // Get a calendar which is 1 second in the future
        val cal = Calendar.getInstance()
        cal[Calendar.SECOND] = cal[Calendar.SECOND] + 1
        cal[Calendar.MILLISECOND] = 0

        // Schedule a timer to run every second at about the next full second
        val nextFullSecondIn = cal.timeInMillis - Calendar.getInstance().timeInMillis
        clockTimer.schedule(object : TimerTask() {
            override fun run() {
                currentTime = Calendar.getInstance()
                Handler(context.mainLooper).post {
                    invalidate()
                }
            }
        }, nextFullSecondIn, 1000)
    }

    /**
     * Stops the cycle of updating the clock time every second. This
     * will cause the clock to hide its clock arms. This only works
     * when the `clockType` is not `ClockType.DEFAULT`
     */
    private fun stopAnalogClock() {
        if (clockType == ClockType.DEFAULT) return

        // Cancel the redraw timer and reset currentTime to prevent drawing clock arms
        clockTimer.cancel()
        currentTime = null

        invalidate()
    }

    /**
     * Set the bedtime value of the clock to the provided time (24 hour clock).
     * It will only have an effect if the `clockType` is `ClockType.TIME_SELECTORS`
     */
    fun setBedTime(hour: Int, minute: Int) {
        setTime(ButtonType.BEDTIME, hour, minute)
    }

    /**
     * Set the alarm time value of the clock to the provided time (24 hour clock).
     * It will only have an effect if the `clockType` is `ClockType.TIME_SELECTORS`
     */
    fun setAlarmTime(hour: Int, minute: Int) {
        setTime(ButtonType.ALARM, hour, minute)
    }

    private fun setTime(type: ButtonType, hour: Int, minute: Int) {
        val totalHours = hour + (minute / 60f)
        val anglePerHour = (2 * Math.PI.toFloat()) / 24f
        val angle = totalHours * anglePerHour

        // Manually update the position of the alarm button
        currentButton = type
        setCurrentButtonAngle(angle)
        currentButton = ButtonType.NONE
    }

    /**
     * Allows you to specify if the clock is supposed to be any of the following types:
     * * `DEFAULT`
     *   A default realtime analog clock with clock arms updating every second
     * * `TIME_SELECTORS`
     *   A special clock used to configure bedtime and alarm time. This clock
     *   will also show the possible sleep duration. It will not update every second
     *   and no clock arms are displayed. The bedtime and alarm time can be adjusted
     *   using the sliders of this view or using the available methods
     */
    fun setClockType(type: ClockType) {
        clockType = type
        when (type) {
            ClockType.DEFAULT -> startAnalogClock()
            ClockType.TIME_SELECTORS -> stopAnalogClock()
        }
    }

    /**
     * Sets the timestamps where to show a small line. The line is supposed to indicate
     * an active alarm is set on the provided timestamp. The timestamp is the time in
     * milliseconds between midnight and the time of the alarm
     */
    fun setActiveAlarmMarkers(markers: List<Long>?) {
        alarmMarkersActive = markers?.toTypedArray<Long>()
        invalidate()
    }

    /**
     * Sets the timestamps where to show a small line. The line is supposed to indicate
     * an inactive alarm is set on the provided timestamp. The timestamp is the time in
     * milliseconds between midnight and the time of the alarm
     */
    fun setInactiveAlarmMarkers(markers: List<Long>?) {
        alarmMarkersInactive = markers?.toTypedArray<Long>()
        invalidate()
    }

    enum class ClockType {
        DEFAULT,
        TIME_SELECTORS
    }

    enum class ButtonType {
        NONE,
        BEDTIME,
        ALARM
    }
}