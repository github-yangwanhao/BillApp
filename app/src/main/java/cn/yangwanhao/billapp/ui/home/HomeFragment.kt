package cn.yangwanhao.billapp.ui.home

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import cn.yangwanhao.billapp.databinding.FragmentHomeBinding
import cn.yangwanhao.billapp.ui.home.add.AddExpenseDialogFragment
import cn.yangwanhao.billapp.ui.home.add.IncomeAddDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    val consumeViewModel: ConsumeBillViewModel by viewModels()
    val incomeViewModel: IncomeBillViewModel by viewModels()

    private var currentTab = 0

    private val currentMonth: Int
        get() {
            val calendar = Calendar.getInstance()
            return calendar.get(Calendar.YEAR) * 100 + (calendar.get(Calendar.MONTH) + 1)
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewPager()
        setupTabToggle()
        setupObservers()
        updateFabStyle(0)   // 默认显示支出卡片
        loadStats()          // 加载支出统计
    }

    // ============================================================
    // ViewPager2
    // ============================================================
    private fun setupViewPager() {
        val adapter = HomeViewPagerAdapter(this)
        binding.homeViewPager.adapter = adapter

        binding.homeViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                currentTab = position
                updateTabSelection(position)
                updateFabStyle(position)
            }
        })
    }

    private fun setupTabToggle() {
        updateTabSelection(0)
        binding.homeTabExpense.setOnClickListener {
            binding.homeViewPager.currentItem = 0
        }
        binding.homeTabIncome.setOnClickListener {
            binding.homeViewPager.currentItem = 1
        }
    }

    private fun updateTabSelection(position: Int) {
        val isExpense = position == 0

        binding.homeTabExpense.apply {
            backgroundTintList = android.content.res.ColorStateList.valueOf(
                if (isExpense) android.graphics.Color.WHITE else android.graphics.Color.TRANSPARENT
            )
            setTextColor(
                if (isExpense) android.graphics.Color.parseColor("#6366F1")
                else android.graphics.Color.parseColor("#868E96")
            )
        }
        binding.homeTabIncome.apply {
            backgroundTintList = android.content.res.ColorStateList.valueOf(
                if (!isExpense) android.graphics.Color.WHITE else android.graphics.Color.TRANSPARENT
            )
            setTextColor(
                if (!isExpense) android.graphics.Color.parseColor("#6366F1")
                else android.graphics.Color.parseColor("#868E96")
            )
        }
    }

    // ============================================================
    // 数据观察
    // ============================================================
    private fun setupObservers() {
        // 支出列表变化时刷新支出统计
        consumeViewModel.adapterItems.observe(viewLifecycleOwner) {
            loadStats()
        }

        // 收入统计 LiveData（由 HomeFragment 统一更新顶部卡片）
        incomeViewModel.monthlyTotal.observe(viewLifecycleOwner) { updateIncomeStatsUI() }
        incomeViewModel.monthlyCount.observe(viewLifecycleOwner) { updateIncomeStatsUI() }
        incomeViewModel.crossMonthCount.observe(viewLifecycleOwner) { updateIncomeStatsUI() }

        // 收入列表刷新后触发一次统计刷新
        incomeViewModel.adapterItems.observe(viewLifecycleOwner) {
            incomeViewModel.loadMonthStats(currentMonth)
        }

        consumeViewModel.loadFirstPage()
        incomeViewModel.loadFirstPage()
    }

    // ============================================================
    // 统计 UI 更新
    // ============================================================
    private fun loadStats() {
        lifecycleScope.launch {
            try {
                val summary = withContext(Dispatchers.IO) {
                    consumeViewModel.consumeBillRepository.getMonthSummary(currentMonth)
                }
                withContext(Dispatchers.Main) {
                    val yuan = summary.totalAmount / 100.0
                    binding.homeStatsAmount.text = "¥${String.format("%.2f", yuan)}"
                    binding.homeStatsBadge.text = "共 ${summary.count} 笔"
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateIncomeStatsUI() {
        val total = incomeViewModel.monthlyTotal.value ?: 0
        val count = incomeViewModel.monthlyCount.value ?: 0

        val yuan = total / 100.0
        binding.homeIncomeStatsAmount.text = "¥${String.format("%.2f", yuan)}"
        binding.homeIncomeStatsBadge.text = "共 $count 笔"
    }

    // ============================================================
    // FAB 与卡片切换
    // ============================================================
    private fun updateFabStyle(position: Int) {
        val fab = binding.homeFabAdd
        if (position == 0) {
            // 支出
            fab.backgroundTintList = android.content.res.ColorStateList.valueOf(
                android.graphics.Color.parseColor("#FF922B")
            )
            fab.setOnClickListener { showExpenseDialog() }
            binding.homeStatsCard.visibility = View.VISIBLE
            binding.homeStatsCardIncome.visibility = View.GONE
        } else {
            // 收入
            fab.backgroundTintList = android.content.res.ColorStateList.valueOf(
                android.graphics.Color.parseColor("#2B8A3E")
            )
            fab.setOnClickListener { showIncomeDialog() }
            binding.homeStatsCard.visibility = View.GONE
            binding.homeStatsCardIncome.visibility = View.VISIBLE
            // 切到收入时刷新收入统计
            incomeViewModel.loadMonthStats(currentMonth)
        }
    }

    private fun showExpenseDialog() {
        val dialog = AddExpenseDialogFragment()
        dialog.setOnSaveSuccessListener {
            consumeViewModel.refresh()
            loadStats()
        }
        dialog.show(childFragmentManager, "AddExpenseDialog")
    }

    private fun showIncomeDialog() {
        val dialog = IncomeAddDialogFragment()
        dialog.setOnSaveSuccessListener {
            incomeViewModel.refresh()
            incomeViewModel.loadMonthStats(currentMonth)
        }
        dialog.show(childFragmentManager, "IncomeAddDialog")
    }

    // ============================================================
    // 供子 Fragment 调用
    // ============================================================
    fun refreshConsume() {
        consumeViewModel.refresh()
        loadStats()
    }

    fun refreshIncome() {
        incomeViewModel.refresh()
    }

    fun loadMoreConsume() = consumeViewModel.loadNextPage()

    fun loadMoreIncome() = incomeViewModel.loadNextPage()

    fun deleteConsumeBill(billId: Long) {
        consumeViewModel.deleteBill(billId)
        loadStats()
    }

    fun deleteIncomeBill(billId: Long) {
        incomeViewModel.deleteBill(billId)
    }

    fun setCurrentTab(position: Int) {
        binding.homeViewPager.currentItem = position
        updateTabSelection(position)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}