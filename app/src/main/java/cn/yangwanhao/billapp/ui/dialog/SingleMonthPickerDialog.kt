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

class SingleMonthPickerDialog(
    context: Context,
    private val maxYearMonth: Int? = null  // 新增：最大可选年月
) : Dialog(context) {

    private lateinit var rvYears: RecyclerView
    private lateinit var tvSelected: TextView
    private lateinit var btnCancel: Button
    private lateinit var btnConfirm: Button

    private var selectedYear: Int? = null
    private var selectedMonth: Int? = null

    private lateinit var yearAdapter: YearAdapter
    private var onConfirmListener: ((year: Int, month: Int) -> Unit)? = null

    private val yearRange: IntRange by lazy {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        // 从5年前到当前年，不包含未来年份
        (currentYear - 1)..currentYear
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_month_range_picker)

        rvYears = findViewById(R.id.rvYears)
        tvSelected = findViewById(R.id.tvSelectedRange)
        btnCancel = findViewById(R.id.btnCancel)
        btnConfirm = findViewById(R.id.btnConfirm)

        // 默认选中当前月份
        val calendar = Calendar.getInstance()
        selectedYear = calendar.get(Calendar.YEAR)
        selectedMonth = calendar.get(Calendar.MONTH) + 1

        setupRecyclerView()
        setupListeners()
        updateSelectedText()

        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val pos = yearRange.indexOf(currentYear)
        if (pos >= 0) {
            rvYears.scrollToPosition(pos)
        }
    }

    private fun setupRecyclerView() {
        yearAdapter = YearAdapter(
            yearRange = yearRange,
            startYear = selectedYear,
            startMonth = selectedMonth,
            endYear = null,
            endMonth = null,
            onMonthSelected = { year, month ->
                handleMonthClick(year, month)
            },
            maxYearMonth = maxYearMonth  // 传入限制
        )
        rvYears.layoutManager = LinearLayoutManager(context)
        rvYears.adapter = yearAdapter
    }

    private fun handleMonthClick(year: Int, month: Int) {
        // 检查是否超过最大年月
        val currentYearMonth = year * 100 + month
        if (maxYearMonth != null && currentYearMonth > maxYearMonth) {
            Toast.makeText(context, "不能选择未来的月份", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedYear == year && selectedMonth == month) {
            selectedYear = null
            selectedMonth = null
        } else {
            selectedYear = year
            selectedMonth = month
        }
        yearAdapter.updateSelection(selectedYear, selectedMonth, null, null)
        updateSelectedText()
    }

    private fun updateSelectedText() {
        val text = if (selectedYear != null && selectedMonth != null) {
            "${selectedYear}年${selectedMonth}月"
        } else {
            "请选择月份"
        }
        tvSelected.text = text
    }

    private fun setupListeners() {
        btnCancel.setOnClickListener { dismiss() }

        btnConfirm.setOnClickListener {
            if (selectedYear != null && selectedMonth != null) {
                onConfirmListener?.invoke(selectedYear!!, selectedMonth!!)
                dismiss()
            } else {
                Toast.makeText(context, "请选择一个月份", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun setOnConfirmListener(listener: (Int, Int) -> Unit) {
        this.onConfirmListener = listener
    }
}