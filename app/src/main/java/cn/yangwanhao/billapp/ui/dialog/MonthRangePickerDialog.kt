package cn.yangwanhao.billapp.ui.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.yangwanhao.billapp.R
import cn.yangwanhao.billapp.ui.adapter.YearAdapter
import java.util.Calendar

class MonthRangePickerDialog(context: Context) : Dialog(context) {

    private lateinit var rvYears: RecyclerView
    private lateinit var tvRange: TextView
    private lateinit var btnCancel: Button
    private lateinit var btnConfirm: Button

    // 当前选中状态（完整年月）
    private var startYear: Int? = null
    private var startMonth: Int? = null
    private var endYear: Int? = null
    private var endMonth: Int? = null

    private lateinit var yearAdapter: YearAdapter
    private var onConfirmListener: ((Int, Int, Int, Int) -> Unit)? = null

    private val yearRange: IntRange by lazy {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        (currentYear - 5)..(currentYear + 5)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_month_range_picker)

        initViews()
        setupRecyclerView()
        setupListeners()

        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val currentPos = yearRange.indexOf(currentYear)
        if (currentPos >= 0) {
            rvYears.scrollToPosition(currentPos)
        }

        updateRangeText()
    }

    private fun initViews() {
        rvYears = findViewById(R.id.rvYears)
        tvRange = findViewById(R.id.tvSelectedRange)
        btnCancel = findViewById(R.id.btnCancel)
        btnConfirm = findViewById(R.id.btnConfirm)

        // 🔥 默认选中当前月作为起始月份
        val currentCalendar = Calendar.getInstance()
        startYear = currentCalendar.get(Calendar.YEAR)
        startMonth = currentCalendar.get(Calendar.MONTH) + 1

        setupRecyclerView()
        setupListeners()

        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val currentPos = yearRange.indexOf(currentYear)
        if (currentPos >= 0) {
            rvYears.scrollToPosition(currentPos)
        }

        updateRangeText()
    }

    private fun setupRecyclerView() {
        yearAdapter = YearAdapter(
            yearRange = yearRange,
            startYear = startYear,
            startMonth = startMonth,
            endYear = endYear,
            endMonth = endMonth,
            onMonthSelected = { year, month ->
                handleMonthClick(year, month)
            }
        )
        rvYears.layoutManager = LinearLayoutManager(context)
        rvYears.adapter = yearAdapter
    }

    /**
     * 核心点击逻辑：
     * - 第一次点击 → 设为起始
     * - 第二次点击，若晚于起始 → 设为结束
     * - 第二次点击，若早于起始 → 交换（起始变为新的，结束清空）
     * - 第三次点击（已有完整范围）→ 重置，当前点击作为第一次
     */
    private fun handleMonthClick(year: Int, month: Int) {
        when {
            // 没有起始 → 设为起始
            startYear == null || startMonth == null -> {
                startYear = year
                startMonth = month
                endYear = null
                endMonth = null
            }
            // 已有起始，没有结束 → 处理第二次点击
            endYear == null || endMonth == null -> {
                // 比较新点击是否早于起始
                val newDate = year * 12 + (month - 1)
                val startDate = startYear!! * 12 + (startMonth!! - 1)

                if (newDate < startDate) {
                    // 新点击早于起始 → 交换：新点击变为起始，清空结束
                    startYear = year
                    startMonth = month
                    endYear = null
                    endMonth = null
                } else {
                    // 新点击晚于起始 → 设为结束
                    endYear = year
                    endMonth = month
                }
            }
            // 已有完整范围 → 重置，当前点击作为第一次
            else -> {
                startYear = year
                startMonth = month
                endYear = null
                endMonth = null
            }
        }

        // 更新视图
        yearAdapter.updateSelection(startYear, startMonth, endYear, endMonth)
        updateRangeText()

        // 如果起始年份变化，滚动到起始年份
        if (startYear != null) {
            val position = yearRange.indexOf(startYear!!)
            if (position >= 0) {
                rvYears.smoothScrollToPosition(position)
            }
        }
    }

    private fun updateRangeText() {
        val text = when {
            startYear != null && startMonth != null && endYear != null && endMonth != null -> {
                // 计算总期数
                val totalMonths = (endYear!! - startYear!!) * 12 + (endMonth!! - startMonth!!) + 1
                "${startYear}年${startMonth}月  ~  ${endYear}年${endMonth}月 (共${totalMonths}期)"
            }
            startYear != null && startMonth != null ->
                "起始：${startYear}年${startMonth}月，请选择结束月份"
            else ->
                "请点击月份选择起始月和结束月"
        }
        tvRange.text = text
    }

    private fun setupListeners() {
        btnCancel.setOnClickListener { dismiss() }

        btnConfirm.setOnClickListener {
            if (startYear != null && startMonth != null && endYear != null && endMonth != null) {
                onConfirmListener?.invoke(
                    startYear!!,
                    startMonth!!,
                    endYear!!,
                    endMonth!!
                )
                dismiss()
            } else {
                Toast.makeText(context, "请选择完整的月份范围", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun setOnConfirmListener(listener: (Int, Int, Int, Int) -> Unit) {
        this.onConfirmListener = listener
    }
}