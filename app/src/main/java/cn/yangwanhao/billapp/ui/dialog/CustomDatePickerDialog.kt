package cn.yangwanhao.billapp.ui.dialog

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import cn.yangwanhao.billapp.R
import java.util.Calendar

class CustomDatePickerDialog(
    context: Context,
    private val maxDate: Calendar? = null,  // 最大可选日期，null表示不限制
    private val onDateSelected: (year: Int, month: Int, day: Int) -> Unit
) : BottomSheetDialog(context) {

    private val calendar = Calendar.getInstance()
    private val currentYear = calendar.get(Calendar.YEAR)
    private val currentMonth = calendar.get(Calendar.MONTH)
    private val currentDay = calendar.get(Calendar.DAY_OF_MONTH)

    // 当前显示的月份
    private var displayYear = currentYear
    private var displayMonth = currentMonth

    // 选中的日期（默认为今天）
    private var selectedYear = currentYear
    private var selectedMonth = currentMonth
    private var selectedDay = currentDay

    // 用于月份切换动画
    private var isAnimating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_custom_date_picker)

        // 设置宽度为 match_parent
        window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        // 默认展开底部抽屉
        behavior.state = BottomSheetBehavior.STATE_EXPANDED

        initViews()
        renderCalendar()
    }

    private fun initViews() {
        // 上一月
        findViewById<TextView>(R.id.tvPrevMonth)?.setOnClickListener {
            if (isAnimating) return@setOnClickListener
            isAnimating = true
            animateOut {
                displayMonth--
                if (displayMonth < 0) {
                    displayMonth = 11
                    displayYear--
                }
                renderCalendar()
                animateIn { isAnimating = false }
            }
        }

        // 下一月
        findViewById<TextView>(R.id.tvNextMonth)?.setOnClickListener {
            if (isAnimating) return@setOnClickListener

            // 计算下一个月
            var newYear = displayYear
            var newMonth = displayMonth + 1
            if (newMonth > 11) {
                newMonth = 0
                newYear++
            }

            // 检查是否超过最大日期所在月份
            if (maxDate != null) {
                val maxYear = maxDate.get(Calendar.YEAR)
                val maxMonth = maxDate.get(Calendar.MONTH)
                if (newYear > maxYear || (newYear == maxYear && newMonth > maxMonth)) {
                    // 已经到达最大月份，不允许继续
                    Toast.makeText(context, "不能选择未来的月份", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            isAnimating = true
            animateOut {
                displayYear = newYear
                displayMonth = newMonth
                renderCalendar()
                animateIn { isAnimating = false }
            }
        }

        findViewById<TextView>(R.id.btnCancel)?.setOnClickListener {
            dismiss()
        }

        findViewById<TextView>(R.id.btnConfirm)?.setOnClickListener {
            onDateSelected(selectedYear, selectedMonth + 1, selectedDay)
            dismiss()
        }
    }

    /**
     * 向左滑出动画（切换上一月时使用）
     */
    private fun animateOut(onComplete: () -> Unit) {
        val grid = findViewById<GridLayout>(R.id.glDays)
        if (grid == null) {
            onComplete()
            return
        }
        val animator = android.animation.ObjectAnimator.ofFloat(
            grid,
            "translationX",
            0f,
            -grid.width.toFloat()
        ).apply {
            duration = 250
            interpolator = DecelerateInterpolator()
        }
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                onComplete()
            }
        })
        animator.start()
    }

    /**
     * 向右滑入动画（切换下一月时使用）
     * 使用 grid.post 确保视图已测量完成，width 不为 0
     */
    private fun animateIn(onComplete: () -> Unit) {
        val grid = findViewById<GridLayout>(R.id.glDays)
        if (grid == null) {
            onComplete()
            return
        }
        grid.post {
            grid.translationX = grid.width.toFloat()
            val animator = android.animation.ObjectAnimator.ofFloat(
                grid,
                "translationX",
                grid.width.toFloat(),
                0f
            ).apply {
                duration = 250
                interpolator = DecelerateInterpolator()
            }
            animator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onComplete()
                }
            })
            animator.start()
        }
    }

    private fun renderCalendar() {
        // 更新年月标题
        findViewById<TextView>(R.id.tvYearMonth)?.text =
            "${displayYear}年${displayMonth + 1}月"

        val grid = findViewById<GridLayout>(R.id.glDays)
        grid?.removeAllViews()
        grid?.translationX = 0f

        // 获取当月第一天是星期几（1=星期一，7=星期日）
        val firstDayCalendar = Calendar.getInstance().apply {
            set(displayYear, displayMonth, 1)
        }
        val firstDayOfWeek = firstDayCalendar.get(Calendar.DAY_OF_WEEK)
        val firstDayInChina = if (firstDayOfWeek == Calendar.SUNDAY) 7 else firstDayOfWeek - 1

        val daysInMonth = firstDayCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        // 填充空白格（禁用）
        for (i in 1 until firstDayInChina) {
            val emptyView = createDayView("", isToday = false, isSelected = false, isDisabled = true)
            grid?.addView(emptyView)
        }

        // 填充日期
        for (day in 1..daysInMonth) {
            val isToday = displayYear == currentYear &&
                    displayMonth == currentMonth &&
                    day == currentDay

            val isSelected = displayYear == selectedYear &&
                    displayMonth == selectedMonth &&
                    day == selectedDay

            // 判断是否超过最大日期
            val isDisabled = maxDate?.let { max ->
                val cal = Calendar.getInstance().apply {
                    set(displayYear, displayMonth, day)
                }
                cal.after(max)
            } ?: false

            val dayView = createDayView(day.toString(), isToday, isSelected, isDisabled)
            if (!isDisabled) {
                val position = day
                dayView.setOnClickListener {
                    selectedYear = displayYear
                    selectedMonth = displayMonth
                    selectedDay = position
                    renderCalendar()
                }
            }
            grid?.addView(dayView)
        }

        grid?.requestLayout()
    }

    private fun createDayView(
        text: String,
        isToday: Boolean = false,
        isSelected: Boolean = false,
        isDisabled: Boolean = false
    ): TextView {
        val dpToPx = { dp: Float ->
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, context.resources.displayMetrics
            ).toInt()
        }

        return TextView(context).apply {
            this.text = text
            gravity = android.view.Gravity.CENTER
            textSize = 16f
            includeFontPadding = false
            setPadding(dpToPx(4f), dpToPx(8f), dpToPx(4f), dpToPx(8f))

            layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = dpToPx(50f)
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(dpToPx(4f), dpToPx(4f), dpToPx(4f), dpToPx(4f))
            }

            if (isDisabled) {
                background = ContextCompat.getDrawable(context, R.drawable.bg_day_disabled)
                setTextColor(ContextCompat.getColor(context, R.color.day_text_disabled))
                isClickable = false
                isFocusable = false
            } else {
                background = when {
                    isSelected -> ContextCompat.getDrawable(context, R.drawable.bg_range_start)
                    isToday -> ContextCompat.getDrawable(context, R.drawable.bg_day_today)
                    else -> ContextCompat.getDrawable(context, R.drawable.bg_month_normal)
                }
                setTextColor(
                    when {
                        isSelected -> ContextCompat.getColor(context, android.R.color.white)
                        isToday -> ContextCompat.getColor(context, android.R.color.white)
                        else -> ContextCompat.getColor(context, R.color.day_text_normal)
                    }
                )
                isClickable = true
                isFocusable = true
            }

            contentDescription = if (text.isNotEmpty()) {
                "${displayYear}年${displayMonth + 1}月${text.toInt()}日"
            } else {
                null
            }
        }
    }
}