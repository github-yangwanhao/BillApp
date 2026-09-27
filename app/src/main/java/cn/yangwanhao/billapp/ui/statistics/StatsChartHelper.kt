package cn.yangwanhao.billapp.ui.statistics

import android.graphics.Color
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import cn.yangwanhao.billapp.dto.TrendPoint

object StatsChartHelper {

    private const val COLOR_INCOME = 0xFF2B8A3E.toInt()
    private const val COLOR_EXPENSE = 0xFFFA5252.toInt()
    private const val COLOR_GRID = 0xFFF1F3F5.toInt()
    private const val COLOR_TEXT = 0xFFADB5BD.toInt()

    /**
     * 渲染折线图
     *
     * @param showBigAmount true=含大额（Y 轴取最大值）；false=不含大额（Y 轴取 95% 分位）
     */
    fun render(
        chart: LineChart,
        points: List<TrendPoint>,
        showIncomeLine: Boolean,
        mode: StatsViewMode,
        showBigAmount: Boolean = false,
        highlightIndex: Int = -1
    ) {
        if (points.isEmpty()) {
            chart.clear()
            chart.invalidate()
            return
        }

        // ============ 构造数据点 ============
        val incomeEntries = ArrayList<Entry>()
        val expenseEntries = ArrayList<Entry>()
        points.forEachIndexed { i, p ->
            if (showIncomeLine) {
                incomeEntries.add(Entry(i.toFloat(), p.income / 100f))
            }
            expenseEntries.add(Entry(i.toFloat(), p.expense / 100f))
        }

        val dataSets = ArrayList<ILineDataSet>()

        if (showIncomeLine) {
            val incomeSet = LineDataSet(incomeEntries, "收入").apply {
                color = COLOR_INCOME
                lineWidth = 2f
                setDrawCircles(true)
                setCircleColor(Color.WHITE)
                circleRadius = 3f
                setCircleHoleColor(COLOR_INCOME)
                setDrawCircleHole(true)
                circleHoleRadius = 1.5f
                setDrawValues(false)
                setMode(LineDataSet.Mode.LINEAR)
                setDrawFilled(true)
                fillColor = COLOR_INCOME
                fillAlpha = 30
            }
            dataSets.add(incomeSet)
        }

        val expenseSet = LineDataSet(expenseEntries, "支出").apply {
            color = COLOR_EXPENSE
            lineWidth = 2f
            setDrawCircles(true)
            setCircleColor(Color.WHITE)
            circleRadius = 2.5f
            setCircleHoleColor(COLOR_EXPENSE)
            setDrawCircleHole(true)
            circleHoleRadius = 1.2f
            setDrawValues(false)
            setMode(LineDataSet.Mode.LINEAR)
            setDrawFilled(true)
            fillColor = COLOR_EXPENSE
            fillAlpha = 25
        }
        dataSets.add(expenseSet)

        chart.data = LineData(dataSets)

        // ============ 计算 Y 轴上限 ============
        val allValues = expenseEntries.map { it.y } +
                (if (showIncomeLine) incomeEntries.map { it.y } else emptyList())

        val yMax = if (allValues.isEmpty()) {
            100f
        } else if (showBigAmount) {
            // 含大额：取最大值
            allValues.max() * 1.2f
        } else {
            // 不含大额：取 95% 分位
            val sorted = allValues.sorted()
            val p95Index = (sorted.size * 0.95).toInt().coerceAtMost(sorted.size - 1)
            sorted[p95Index] * 1.2f
        }

        // ============ X 轴 ============
        chart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            setDrawGridLines(false)
            setDrawAxisLine(false)
            granularity = 1f
            textSize = 10f
            textColor = COLOR_TEXT
            yOffset = 6f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val idx = value.toInt()
                    return if (idx in points.indices) points[idx].label else ""
                }
            }
            setLabelCount(computeLabelCount(points.size, mode), false)
        }

        // ============ Y 轴 ============
        chart.axisLeft.apply {
            setDrawGridLines(true)
            gridColor = COLOR_GRID
            gridLineWidth = 1f
            setDrawAxisLine(false)
            setDrawLabels(true)
            textSize = 10f
            textColor = COLOR_TEXT
            axisMinimum = 0f
            axisMaximum = yMax
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return when {
                        value >= 10000 -> "${(value / 10000).toInt()}万"
                        value >= 1000 -> "${(value / 1000).toInt()}k"
                        else -> value.toInt().toString()
                    }
                }
            }
        }
        chart.axisRight.isEnabled = false

        // ============ 其他配置 ============
        chart.description.isEnabled = false
        chart.legend.isEnabled = false
        chart.setTouchEnabled(true)
        chart.isDragEnabled = true
        chart.setScaleEnabled(false)
        chart.setPinchZoom(false)
        chart.isDoubleTapToZoomEnabled = false
        chart.isHighlightPerTapEnabled = true

        // 🔥 横向可滑动：一次只显示 N 个点，其余通过滑动查看
        // 🔥 横向可滑动：一次只显示 N 个点，其余通过滑动查看
        when (mode) {
            StatsViewMode.MONTH -> {
                chart.setVisibleXRangeMaximum(7f)   // 一次最多显示 7 天
                chart.setVisibleXRangeMinimum(6f)   // 最少 3 天（防止过度放大）
            }
            StatsViewMode.YEAR -> {
                chart.setVisibleXRangeMaximum(7f)
                chart.setVisibleXRangeMinimum(6f)
            }
            StatsViewMode.ALL -> {
                chart.setVisibleXRangeMaximum(7f)
                chart.setVisibleXRangeMinimum(6f)
            }
        }

        // view port offsets（用 dp 换算）
        val density = chart.resources.displayMetrics.density
        val dp = { v: Float -> v * density }
        chart.setViewPortOffsets(dp(36f), dp(16f), dp(20f), dp(42f))

        chart.notifyDataSetChanged()
        chart.invalidate()

        // 高亮指定点
        if (highlightIndex in points.indices) {
            chart.highlightValue(highlightIndex.toFloat(), 0, false)
        }
    }

    /**
     * X 轴标签数（与 setVisibleXRangeMinimum 保持一致）
     */
    private fun computeLabelCount(size: Int, mode: StatsViewMode): Int {
        return when (mode) {
            StatsViewMode.MONTH -> 7
            StatsViewMode.YEAR -> 6
            StatsViewMode.ALL -> 5
        }.coerceAtMost(size)
    }

    /**
     * 计算默认高亮索引
     */
    fun computeDefaultHighlight(points: List<TrendPoint>, mode: StatsViewMode): Int {
        if (points.isEmpty()) return -1

        val now = java.util.Calendar.getInstance()
        val today = now.get(java.util.Calendar.DAY_OF_MONTH)
        val currentMonth = now.get(java.util.Calendar.MONTH) + 1
        val currentYear = now.get(java.util.Calendar.YEAR)

        val targetIndex = when (mode) {
            StatsViewMode.MONTH -> today - 1
            StatsViewMode.YEAR -> currentMonth - 1
            StatsViewMode.ALL -> {
                val idx = points.indexOfFirst { it.label == currentYear.toString() }
                if (idx >= 0) idx else -1
            }
        }

        if (targetIndex in points.indices) return targetIndex

        val lastWithData = points.indexOfLast { it.income > 0 || it.expense > 0 }
        return if (lastWithData >= 0) lastWithData else 0
    }
}