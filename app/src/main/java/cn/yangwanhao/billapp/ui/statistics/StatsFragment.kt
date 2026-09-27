package cn.yangwanhao.billapp.ui.statistics

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import cn.yangwanhao.billapp.R
import cn.yangwanhao.billapp.databinding.FragmentStatsBinding
import cn.yangwanhao.billapp.dto.CategorySum
import cn.yangwanhao.billapp.dto.ChannelSum
import cn.yangwanhao.billapp.dto.TrendPoint
import cn.yangwanhao.billapp.ui.adapter.CategoryRankAdapter
import cn.yangwanhao.billapp.ui.adapter.ChannelRankAdapter
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener

class StatsFragment : Fragment() {

    private var _binding: FragmentStatsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StatsViewModel by viewModels()

    private lateinit var categoryAdapter: CategoryRankAdapter
    private lateinit var channelAdapter: ChannelRankAdapter

    // 🔥 含大额开关状态（Fragment 生命周期内记忆）
    private var showBigAmount = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        setupListeners()
        setupObservers()

        viewModel.refresh()
    }

    // ============================================================
    // RecyclerView 初始化
    // ============================================================
    private fun setupRecyclerViews() {
        categoryAdapter = CategoryRankAdapter(isIncome = false) { item ->
            onCategoryClicked(item)
        }
        binding.rvCategoryRank.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCategoryRank.adapter = categoryAdapter

        channelAdapter = ChannelRankAdapter { item ->
            onChannelClicked(item)
        }
        binding.rvChannelRank.layoutManager = LinearLayoutManager(requireContext())
        binding.rvChannelRank.adapter = channelAdapter
    }

    // ============================================================
    // 监听器
    // ============================================================
    private fun setupListeners() {
        // 时间切换
        binding.btnPrev.setOnClickListener { viewModel.prevPeriod() }
        binding.btnNext.setOnClickListener { viewModel.nextPeriod() }

        // 顶层 Tab
        binding.tabMonth.setOnClickListener { viewModel.setViewMode(StatsViewMode.MONTH) }
        binding.tabYear.setOnClickListener { viewModel.setViewMode(StatsViewMode.YEAR) }
        binding.tabAll.setOnClickListener { viewModel.setViewMode(StatsViewMode.ALL) }

        // 分类 Tab
        binding.tabCatExpense.setOnClickListener {
            viewModel.setCategoryTab(StatsCategoryTab.EXPENSE)
        }
        binding.tabCatIncome.setOnClickListener {
            viewModel.setCategoryTab(StatsCategoryTab.INCOME)
        }

        // 🔥 含大额开关
        binding.tvBigAmountToggle.setOnClickListener {
            showBigAmount = !showBigAmount
            updateToggleUI()
            viewModel.uiState.value?.let { renderChart(it) }
        }
        updateToggleUI()

        // 🔥 下拉刷新
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refresh()
        }
    }

    private fun updateToggleUI() {
        if (showBigAmount) {
            binding.tvBigAmountToggle.background =
                ContextCompat.getDrawable(requireContext(), R.drawable.bg_toggle_on)
            binding.tvBigAmountToggle.setTextColor(0xFFFFFFFF.toInt())
        } else {
            binding.tvBigAmountToggle.background =
                ContextCompat.getDrawable(requireContext(), R.drawable.bg_toggle_off)
            binding.tvBigAmountToggle.setTextColor(0xFF868E96.toInt())
        }
    }

    // ============================================================
    // 数据观察
    // ============================================================
    private fun setupObservers() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            renderTopTabs(state.viewMode)
            renderCatTabs(state.categoryTab)
            renderPeriod(state)
            renderEmptyState(state)          // 🔥 新增
            renderSummary(state)
            renderChart(state)
            renderCategoryRank(state)
            renderChannelRank(state)
            // 🔥 关闭下拉刷新状态
            binding.swipeRefresh.isRefreshing = false
        }
    }

    // ============================================================
    // 渲染：顶部 Tab
    // ============================================================
    private fun renderTopTabs(mode: StatsViewMode) {
        val tabs = listOf(binding.tabMonth, binding.tabYear, binding.tabAll)
        tabs.forEach {
            it.background = null
            it.setTextColor(ContextCompat.getColor(requireContext(), R.color.pub_nav_icon_inactive))
        }
        val selected = when (mode) {
            StatsViewMode.MONTH -> binding.tabMonth
            StatsViewMode.YEAR -> binding.tabYear
            StatsViewMode.ALL -> binding.tabAll
        }
        selected.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_tab_selected)
        selected.setTextColor(ContextCompat.getColor(requireContext(), R.color.pub_primary))
    }

    // ============================================================
    // 渲染：分类 Tab（含支付方式卡片显隐）
    // ============================================================
    private fun renderCatTabs(tab: StatsCategoryTab) {
        val expenseSelected = tab == StatsCategoryTab.EXPENSE
        listOf(binding.tabCatExpense, binding.tabCatIncome).forEach {
            it.background = null
            it.setTextColor(ContextCompat.getColor(requireContext(), R.color.pub_nav_icon_inactive))
        }
        if (expenseSelected) {
            binding.tabCatExpense.background =
                ContextCompat.getDrawable(requireContext(), R.drawable.bg_tab_selected)
            binding.tabCatExpense.setTextColor(ContextCompat.getColor(requireContext(), R.color.pub_primary))
        } else {
            binding.tabCatIncome.background =
                ContextCompat.getDrawable(requireContext(), R.drawable.bg_tab_selected)
            binding.tabCatIncome.setTextColor(ContextCompat.getColor(requireContext(), R.color.income_green))
        }

        // 🔥 支付方式卡片：仅支出时显示
        binding.channelRankCard.visibility = if (expenseSelected) View.VISIBLE else View.GONE
    }

    // ============================================================
    // 渲染：时间切换器
    // ============================================================
    private fun renderPeriod(state: StatsUiState) {
        binding.tvPeriod.text = state.periodText
        binding.btnPrev.isEnabled = state.canPrev
        binding.btnNext.isEnabled = state.canNext
        binding.btnPrev.alpha = if (state.canPrev) 1f else 0.35f
        binding.btnNext.alpha = if (state.canNext) 1f else 0.35f
    }

    // ============================================================
    // 渲染：收支卡片 + 结余
    // ============================================================
    @SuppressLint("SetTextI18n", "DefaultLocale")
    private fun renderSummary(state: StatsUiState) {
        val s = state.summary

        // 收入
        val incomeYuan = s.incomeTotal / 100.0
        binding.tvIncomeAmount.text = "¥${String.format("%,.2f", incomeYuan)}"
        binding.tvIncomeCount.text = "共 ${s.incomeCount} 笔"

        // 支出
        val expenseYuan = s.expenseTotal / 100.0
        binding.tvExpenseAmount.text = "¥${String.format("%,.2f", expenseYuan)}"
        binding.tvExpenseCount.text = "共 ${s.expenseCount} 笔"

        // 结余
        val bal = s.balance
        val balYuan = Math.abs(bal) / 100.0
        binding.tvBalance.text = (if (bal >= 0) "¥" else "-¥") + String.format("%,.2f", balYuan)
        binding.tvBalance.setTextColor(
            if (bal >= 0) ContextCompat.getColor(requireContext(), R.color.income_green)
            else ContextCompat.getColor(requireContext(), R.color.home_amount_negative)
        )

        // 百分比徽章
        val pct = s.balancePercent
        if (pct == null) {
            binding.tvBalanceBadge.text = "—"
            binding.tvBalanceBadge.background =
                ContextCompat.getDrawable(requireContext(), R.drawable.bg_badge_positive)
            binding.tvBalanceBadge.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.pub_nav_icon_inactive)
            )
        } else {
            binding.tvBalanceBadge.text = (if (pct >= 0) "+" else "") + "$pct%"
            val positive = pct >= 0
            binding.tvBalanceBadge.background = ContextCompat.getDrawable(
                requireContext(),
                if (positive) R.drawable.bg_badge_positive else R.drawable.bg_badge_negative
            )
            binding.tvBalanceBadge.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (positive) R.color.income_green else R.color.home_amount_negative
                )
            )
        }
    }

    // ============================================================
    // 渲染：折线图
    // ============================================================
    private fun renderChart(state: StatsUiState) {
        val mode = state.viewMode
        val points = state.trendPoints

        // 标题
        binding.tvChartTitle.text = when (mode) {
            StatsViewMode.MONTH -> "每日趋势"
            StatsViewMode.YEAR -> "每月趋势"
            StatsViewMode.ALL -> "每年趋势"
        }

        // 图例 / 提示显隐
        val showIncomeLine = mode != StatsViewMode.MONTH
        binding.chartLegend.visibility = if (showIncomeLine) View.VISIBLE else View.GONE
        binding.tvChartNote.visibility = if (showIncomeLine) View.GONE else View.VISIBLE

        if (points.isEmpty()) {
            binding.lineChart.clear()
            binding.lineChart.invalidate()
            binding.tvInfoDate.text = "—"
            binding.tvInfoIncome.visibility = View.GONE
            binding.tvInfoExpense.visibility = View.GONE
            return
        }

        // 默认高亮索引
        val defaultIdx = StatsChartHelper.computeDefaultHighlight(points, mode)

        // 渲染图表
        StatsChartHelper.render(
            chart = binding.lineChart,
            points = points,
            showIncomeLine = showIncomeLine,
            mode = mode,
            showBigAmount = showBigAmount,
            highlightIndex = defaultIdx
        )

        // 滚到默认位置
        if (defaultIdx in points.indices) {
            binding.lineChart.moveViewToX(defaultIdx.toFloat())
        }

        // 首次显示信息栏
        renderChartInfo(points, defaultIdx)

        // 点击监听
        binding.lineChart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry?, h: Highlight?) {
                val idx = h?.x?.toInt() ?: return
                if (idx in points.indices) {
                    renderChartInfo(points, idx)
                }
            }

            override fun onNothingSelected() { }
        })
    }

    @SuppressLint("DefaultLocale")
    private fun renderChartInfo(points: List<TrendPoint>, idx: Int) {
        if (idx !in points.indices) return
        val p = points[idx]
        binding.tvInfoDate.text = p.label

        if (p.income > 0) {
            binding.tvInfoIncome.visibility = View.VISIBLE
            binding.tvInfoIncome.text = "● 收入 ¥${String.format("%,.2f", p.income / 100.0)}"
        } else {
            binding.tvInfoIncome.visibility = View.GONE
        }

        if (p.expense > 0) {
            binding.tvInfoExpense.visibility = View.VISIBLE
            binding.tvInfoExpense.text = "● 支出 ¥${String.format("%,.2f", p.expense / 100.0)}"
        } else {
            binding.tvInfoExpense.visibility = View.GONE
        }

        if (p.income == 0 && p.expense == 0) {
            binding.tvInfoDate.text = "${p.label}：无收支"
            binding.tvInfoIncome.visibility = View.GONE
            binding.tvInfoExpense.visibility = View.GONE
        }
    }

    // ============================================================
    // 渲染：分类排行
    // ============================================================
    private fun renderCategoryRank(state: StatsUiState) {
        val isIncome = state.categoryTab == StatsCategoryTab.INCOME

        // 重建 adapter（isIncome 是构造参数）
        categoryAdapter = CategoryRankAdapter(isIncome = isIncome) { item ->
            onCategoryClicked(item)
        }
        binding.rvCategoryRank.adapter = categoryAdapter
        categoryAdapter.submitList(state.categoryRank)

        binding.tvCategorySubtitle.text =
            if (isIncome) "按收入金额" else "按支出金额"
    }

    // ============================================================
    // 渲染：支付方式排行
    // ============================================================
    private fun renderChannelRank(state: StatsUiState) {
        channelAdapter.submitList(state.channelRank)
    }

    // ============================================================
    // 点击回调
    // ============================================================
    private fun onCategoryClicked(item: CategorySum) {
        val state = viewModel.uiState.value ?: return
        val (start, end) = viewModel.getCurrentRange()
        val periodText = viewModel.getCurrentPeriodText()

        val sheet = CategoryDrillSheetFragment.newInstance(
            categoryId = item.categoryId,
            categoryName = item.categoryName,
            isIncome = state.categoryTab == StatsCategoryTab.INCOME,
            periodText = periodText,
            start = start,
            end = end
        )
        sheet.show(childFragmentManager, "CategoryDrill")
    }

    private fun onChannelClicked(item: ChannelSum) {
        Toast.makeText(
            requireContext(),
            "点击了支付方式：${item.channelName}",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * 空数据状态：整个时间区间没有任何账单
     */
    private fun renderEmptyState(state: StatsUiState) {
        val s = state.summary
        val isEmpty = s.incomeCount == 0 && s.expenseCount == 0

        binding.emptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.contentContainer.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }
}