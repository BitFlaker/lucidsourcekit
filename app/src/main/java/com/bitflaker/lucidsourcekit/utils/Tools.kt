package com.bitflaker.lucidsourcekit.utils

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Resources
import android.content.res.Resources.Theme
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.VectorDrawable
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import androidx.annotation.ColorInt
import com.bitflaker.lucidsourcekit.R
import com.bitflaker.lucidsourcekit.datastore.DataStoreKeys
import com.bitflaker.lucidsourcekit.datastore.getSetting
import com.bitflaker.lucidsourcekit.database.MainDatabase
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.DreamClarity
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.DreamMood
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.SleepQuality
import com.bitflaker.lucidsourcekit.database.goals.entities.Goal
import com.bitflaker.lucidsourcekit.main.goals.RandomGoalPicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.system.exitProcess
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import androidx.core.graphics.drawable.toDrawable

object Tools {
    private const val NOTIFICATION_ID_START = 500000
    private val notificationIdMap = hashMapOf(
            Pair("DJR", NOTIFICATION_ID_START + 11),
            Pair("RCR", NOTIFICATION_ID_START + 12),
            Pair("DGR", NOTIFICATION_ID_START + 13),
            Pair("CR", NOTIFICATION_ID_START + 14)
    )
    private val iconsDreamMood = hashMapOf(
        Pair("TRB", R.drawable.ic_baseline_sentiment_very_dissatisfied_24),
        Pair("POR", R.drawable.ic_baseline_sentiment_dissatisfied_24),
        Pair("OKY", R.drawable.ic_baseline_sentiment_neutral_24),
        Pair("GRT", R.drawable.ic_baseline_sentiment_satisfied_24),
        Pair("OSD", R.drawable.ic_baseline_sentiment_very_satisfied_24)
    )
    private var iconsDreamClarity = hashMapOf(
        Pair("VCL", R.drawable.ic_baseline_brightness_4_24),
        Pair("CLD", R.drawable.ic_baseline_brightness_5_24),
        Pair("CLR", R.drawable.ic_baseline_brightness_6_24),
        Pair("CCL", R.drawable.ic_baseline_brightness_7_24)
    )
    private var iconsSleepQuality = hashMapOf(
        Pair("TRB", R.drawable.ic_baseline_star_border_24),
        Pair("POR", R.drawable.ic_baseline_star_half_24),
        Pair("GRT", R.drawable.ic_baseline_star_24),
        Pair("OSD", R.drawable.ic_baseline_stars_24)
    )

    @JvmStatic
    @Deprecated("Use Context.attrColor(Int) instead")
    fun getAttrColor(colorAttr: Int, theme: Theme): Int {
        return getAttrValue(colorAttr, theme).data
    }

    @Deprecated("Use Context.typedValue(Int) instead")
    private fun getAttrValue(attr: Int, theme: Theme): TypedValue {
        val typedValue = TypedValue()
        theme.resolveAttribute(attr, typedValue, true)
        return typedValue
    }

    @JvmStatic
    @Deprecated("Use kotlin version of .dpToPx instead")
    fun dpToPx(context: Context, dp: Double): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }

    @JvmStatic
    @Deprecated("Use kotlin version of .spToPx instead")
    fun spToPx(context: Context, sp: Float): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, context.resources.displayMetrics).toInt()
    }

    @JvmStatic
    @Deprecated("Use kotlin version of .pxToDp instead")
    fun pxToDp(context: Context, px: Double): Int {
        return (px / context.resources.displayMetrics.density).toInt()
    }

    fun showPlaceholderDialog(context: Context) {
        MaterialAlertDialogBuilder(context, R.style.Theme_LucidSourceKit_ThemedDialog)
            .setTitle("Placeholder")
            .setMessage("This action currently is just a placeholder. There will be some functionality behind here at a later point in time")
            .setPositiveButton(context.resources.getString(R.string.ok), null)
            .show()
    }
//
//    fun getRelativeLayoutParamsTopStatusbar(context: Context): RelativeLayout.LayoutParams {
//        val lParams = RelativeLayout.LayoutParams(
//            ViewGroup.LayoutParams.MATCH_PARENT,
//            ViewGroup.LayoutParams.WRAP_CONTENT
//        )
//        lParams.setMargins(
//            dpToPx(context, 15.0),
//            getStatusBarHeight(context),
//            dpToPx(context, 10.0),
//            0
//        )
//        return lParams
//    }
//
//    fun getConstraintLayoutParamsTopStatusbar(
//        layoutParams: ViewGroup.LayoutParams?,
//        context: Context
//    ): ConstraintLayout.LayoutParams {
//        val params = layoutParams as ConstraintLayout.LayoutParams
//        params.setMargins(dpToPx(context, 15.0), getStatusBarHeight(context), 0, 0)
//        return params
//    }
//
//    fun addRelativeLayoutParamsTopStatusbarSpacing(
//        context: Context,
//        lParams: RelativeLayout.LayoutParams
//    ): RelativeLayout.LayoutParams {
//        lParams.topMargin = lParams.topMargin + getStatusBarHeight(context)
//        return lParams
//    }
//
//    fun getStatusBarHeight(context: Context): Int {
//        // TODO: maybe find a better way of getting this data
//        var result = 0
//        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
//        if (resourceId > 0) {
//            result = context.resources.getDimensionPixelSize(resourceId)
//        }
//        return result
//    }

    @ColorInt
    fun getColorAtGradientPosition(x: Float, minX: Float, maxX: Float, @ColorInt vararg colors: Int): Int {
        if (colors.isEmpty()) return -1
        if (colors.size == 1) return colors[0]

        val step = (maxX - minX) / (colors.size - 1)
        for (i in 1..<colors.size) {
            if (x <= minX + step * i) {
                return getColorAtGradientPosition(
                    x,
                    minX + step * (i - 1),
                    minX + step * i,
                    false,
                    colors[i - 1],
                    colors[i]
                )
            }
        }
        return -1
    }

    @ColorInt
    fun getColorAtGradientPosition(x: Float, minX: Float, maxX: Float, reduceAccuracy: Boolean, @ColorInt fromColor: Int, @ColorInt toColor: Int): Int {
        var pos = (x - minX) / (maxX - minX)
        if (reduceAccuracy) {
            pos = (pos * 10).roundToLong() / 10.0f
            pos = (pos * 2).roundToLong() / 2.0f
        }
        val invPos = 1 - pos

        val fromAlpha = Color.alpha(fromColor)
        val fromRed = Color.red(fromColor)
        val fromGreen = Color.green(fromColor)
        val fromBlue = Color.blue(fromColor)

        val toAlpha = Color.alpha(toColor)
        val toRed = Color.red(toColor)
        val toGreen = Color.green(toColor)
        val toBlue = Color.blue(toColor)

        val resAlpha = (fromAlpha * invPos + toAlpha * pos).roundToInt()
        val resRed = (fromRed * invPos + toRed * pos).roundToInt()
        val resGreen = (fromGreen * invPos + toGreen * pos).roundToInt()
        val resBlue = (fromBlue * invPos + toBlue * pos).roundToInt()

        return Color.argb(resAlpha, resRed, resGreen, resBlue)
    }

    fun getTimeSpanFrom(toDaysInPast: Int, getTimeSpanUntilNow: Boolean): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, -toDaysInPast)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis
        if (getTimeSpanUntilNow) {
            calendar.add(Calendar.DAY_OF_MONTH, toDaysInPast)
        }
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endTime = calendar.timeInMillis
        return Pair(startTime, endTime)
    }

    suspend fun getNewShuffleGoals(context: Context): List<Goal> {
        val weightCommon = context.getSetting(DataStoreKeys.GOAL_DIFFICULTY_VALUE_COMMON)
        val weightUncommon = context.getSetting(DataStoreKeys.GOAL_DIFFICULTY_VALUE_UNCOMMON)
        val weightRare = context.getSetting(DataStoreKeys.GOAL_DIFFICULTY_VALUE_RARE)
        val goalCount = context.getSetting(DataStoreKeys.GOAL_DIFFICULTY_COUNT)
        val weights = floatArrayOf(weightCommon, weightUncommon, weightRare)

        val goals = MainDatabase.getInstance(context).goalDao.getAllMaybe()
        if (goals.isNullOrEmpty()) {
            return ArrayList()
        }

        val randomGoalPicker = RandomGoalPicker()
        for (goal in goals) {
            val weight = calculateGoalWeight(goal, weights)
            if (weight > 0.0f) {
                randomGoalPicker.add(weight, goal)
            }
        }

        val shuffledGoals: MutableList<Goal> = ArrayList()
        repeat(goalCount) {
            val goal = randomGoalPicker.randomGoal
            if (goal != null) shuffledGoals.add(goal)
        }

        return shuffledGoals
    }

    private fun calculateGoalWeight(goal: Goal, weights: FloatArray): Float {
        val occurrenceLevel = goal.difficulty.toInt()
        val higherPercentage = goal.difficulty - occurrenceLevel
        val lowerPercentage = 1 - higherPercentage

        if (occurrenceLevel < weights.size) {
            val lowerWeight = weights[occurrenceLevel - 1]
            val higherWeight = weights[occurrenceLevel]
            return lowerPercentage * lowerWeight + higherPercentage * higherWeight
        }
        return weights[weights.size - 1]
    }

    fun getIconsDreamMood(context: Context): Array<Drawable> {
        return DreamMood.defaultData
            .filter { it != DreamMood.DEFAULT }
            .sortedBy { it.value }
            .mapNotNull { iconsDreamMood.getOrDefault(it.moodId, null) }
            .mapNotNull { context.resolveDrawable(it) }
            .toTypedArray()
    }

    fun getIconsDreamClarity(context: Context): Array<Drawable> {
        return DreamClarity.defaultData
            .filter { it != DreamClarity.DEFAULT }
            .sortedBy { it.value }
            .mapNotNull { iconsDreamClarity.getOrDefault(it.clarityId, null) }
            .mapNotNull { context.resolveDrawable(it) }
            .toTypedArray()
    }

    fun getIconsSleepQuality(context: Context): Array<Drawable> {
        return SleepQuality.defaultData
            .filter { it != SleepQuality.DEFAULT }
            .sortedBy { it.value }
            .mapNotNull { iconsSleepQuality.getOrDefault(it.qualityId, null) }
            .mapNotNull { context.resolveDrawable(it) }
            .toTypedArray()
    }

    fun resolveIconDreamMood(context: Context, dreamMoodId: String): Drawable? {
        return context.resolveDrawable(iconsDreamMood.getOrDefault(dreamMoodId, 0))
    }

    fun resolveIconDreamClarity(context: Context, dreamClarityId: String): Drawable? {
        return context.resolveDrawable(iconsDreamClarity.getOrDefault(dreamClarityId, 0))
    }

    fun resolveIconSleepQuality(context: Context, sleepQualityId: String): Drawable? {
        return context.resolveDrawable(iconsSleepQuality.getOrDefault(sleepQualityId, 0))
    }

    fun launchResourceMonitor() = Thread {
        val runtime = Runtime.getRuntime()
        while (true) {
            val usedMemInMB = (runtime.totalMemory() - runtime.freeMemory()) / 1048576L
            val maxHeapSizeInMB = runtime.maxMemory() / 1048576L
            val availHeapSizeInMB = maxHeapSizeInMB - usedMemInMB
            Log.d("ResourceUsage", String.format(Locale.ENGLISH, "Resource-Usage: [%d] [%d] [%d]%n", usedMemInMB, maxHeapSizeInMB, availHeapSizeInMB))
            Thread.sleep(1500)
        }
    }.start()

    @JvmStatic
    fun drawableToBitmap(drawable: Drawable, size: Int): Bitmap {
        if (drawable is BitmapDrawable) return drawable.bitmap
        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(canvas)
        return bitmap
    }

    @JvmStatic
    fun drawableToBitmap(drawable: Drawable, tint: Int, size: Int): Bitmap {
        if (drawable is BitmapDrawable) return drawable.bitmap
        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, size, size)
        drawable.setTint(tint)
        drawable.draw(canvas)
        return bitmap
    }

    @JvmStatic
    @ColorInt
    fun manipulateAlpha(@ColorInt color: Int, factor: Float): Int {
        val alpha = (Color.alpha(color) * factor).roundToInt()
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)
        return Color.argb(alpha, red, green, blue)
    }

    fun getTimeOfDayMillis(calendar: Calendar): Long {
        var timeOfDayMillis = (calendar[Calendar.HOUR_OF_DAY] * 60 * 60 * 1000).toLong()
        timeOfDayMillis += (calendar[Calendar.MINUTE] * 60 * 1000).toLong()
        timeOfDayMillis += (calendar[Calendar.SECOND] * 1000).toLong()
        timeOfDayMillis += calendar[Calendar.MILLISECOND].toLong()
        return timeOfDayMillis
    }

    fun getTimeOfDayMillis(timeInMillis: Long): Long {
        val cal = Calendar.getInstance()
        cal.setTimeInMillis(timeInMillis)
        return getTimeOfDayMillis(cal)
    }

    fun getDateInMillis(timeInMillis: Long): Long {
        return timeInMillis - getTimeOfDayMillis(timeInMillis)
    }

    fun getTimeFromCurrentMidnight(timeInMillis: Long): Long {
        val cal = Calendar.getInstance()
        cal.setTimeInMillis(getMidnightTime() + timeInMillis)
        return cal.timeInMillis
    }

    fun getMidnightTime(timestamp: Long): Long {
        val cal = Calendar.getInstance()
        cal.setTimeInMillis(timestamp)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    @JvmStatic
    fun getMidnightTime(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun getUniqueNotificationId(notificationCategoryId: String): Int {
        return notificationIdMap[notificationCategoryId] ?: -1
    }

    fun resizeDrawable(resources: Resources, drawable: Drawable, width: Int, height: Int): Drawable {
        val bitmap: Bitmap
        if (drawable.javaClass == VectorDrawable::class.java) {
            bitmap = createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
        } else {
            bitmap = (drawable as BitmapDrawable).bitmap
        }
        return bitmap.scale(width, height, false).toDrawable(resources)
    }

    fun isTimeInPast(timestampDayEnd: Long): Boolean {
        return timestampDayEnd <= Calendar.getInstance().timeInMillis
    }

    fun animateBackgroundTint(view: View, @ColorInt colorFrom: Int, @ColorInt colorTo: Int, duration: Int) {
        ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo).apply {
            setDuration(duration.toLong())
            addUpdateListener { animator ->
                view.setBackgroundTintList(ColorStateList.valueOf(animator.animatedValue as Int))
            }
        }.start()
    }

    fun animateImageTint(view: ImageView, @ColorInt colorFrom: Int, @ColorInt colorTo: Int, duration: Int) {
        ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo).apply {
            setDuration(duration.toLong())
            addUpdateListener { animator ->
                view.setImageTintList(ColorStateList.valueOf(animator.animatedValue as Int))
            }
        }.start()
    }

    fun cloneDrawable(drawable: Drawable): Drawable = drawable.constantState!!.newDrawable().apply {
        bounds = drawable.copyBounds()
    }

    @JvmStatic
    fun calendarFromMillis(milliseconds: Long): Calendar = Calendar.getInstance().apply {
        timeInMillis = milliseconds
    }

    fun restartApp(context: Context) {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            context.startActivity(intent)
        }
        exitProcess(0)
    }

    fun getTimeSpanStringZeroed(milliseconds: Long): String {
        val timeSpan = getTimeSpanString(milliseconds)
        if (timeSpan.isEmpty()) {
            return "00m"
        }
        return timeSpan
    }

    fun getTimeSpanString(milliseconds: Long): String {
        val days = TimeUnit.MILLISECONDS.toDays(milliseconds)
        val hours = TimeUnit.MILLISECONDS.toHours(milliseconds) - TimeUnit.DAYS.toHours(days)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds) - TimeUnit.DAYS.toMinutes(days) - TimeUnit.HOURS.toMinutes(hours)

        val parts = mutableListOf<String>()

        if (days > 0) parts.add("%02dd".format(Locale.getDefault(), days))
        if (hours > 0 || parts.isNotEmpty()) parts.add("%02dh".format(Locale.getDefault(), hours))
        if (minutes > 0 || parts.isNotEmpty()) parts.add("%02dm".format(Locale.getDefault(), minutes))

        return parts.joinToString(" ")
    }

    fun runOnUiThread(context: Context, runnable: Runnable?) {
        (context as Activity).runOnUiThread(runnable)
    }
}
