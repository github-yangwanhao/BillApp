package cn.yangwanhao.billapp.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import cn.yangwanhao.billapp.MainActivity
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

    private var currentTab = 0  // 0=支出, 1=收入

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

        (activity as? MainActivity)?.homeFragment = this

        setupViewPager()
        setupTabToggle()
        setupObservers()
        setupFab()
        loadStats()
    }

    // ============================================================
    // ViewPager2 设置
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
        // 默认选中支出
        updateTabSelection(0)

        // 点击支出按钮 → 切换到支出
        binding.homeTabExpense.setOnClickListener {
            binding.homeViewPager.currentItem = 0
        }

        // 点击收入按钮 → 切换到收入
        binding.homeTabIncome.setOnClickListener {
            binding.homeViewPager.currentItem = 1
        }
    }

    /**
     * 更新 Tab 样式（完全手动控制）
     * @param position 0=支出, 1=收入
     */
    private fun updateTabSelection(position: Int) {
        val isExpense = position == 0

        // 支出按钮
        binding.homeTabExpense.apply {
            // 设置背景色（直接使用 ColorStateList）
            backgroundTintList = android.content.res.ColorStateList.valueOf(
                if (isExpense) android.graphics.Color.WHITE else android.graphics.Color.TRANSPARENT
            )
            setTextColor(
                if (isExpense) android.graphics.Color.parseColor("#6366F1")
                else android.graphics.Color.parseColor("#868E96")
            )
        }

        // 收入按钮
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
    // 数据观察 & 统计加载
    // ============================================================
    private fun setupObservers() {
        consumeViewModel.adapterItems.observe(viewLifecycleOwner) {
            loadStats()
        }

        incomeViewModel.adapterItems.observe(viewLifecycleOwner) {
            // 收入列表更新时，如果当前是收入 Tab，可刷新统计
        }

        consumeViewModel.loadFirstPage()
        incomeViewModel.loadFirstPage()
    }

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

    // ============================================================
    // FAB 按钮
    // ============================================================
    private fun setupFab() {
        binding.homeFabAdd.setOnClickListener {
            val dialog = AddExpenseDialogFragment()
            dialog.setOnSaveSuccessListener {
                when (binding.homeViewPager.currentItem) {
                    0 -> {
                        consumeViewModel.refresh()
                        loadStats()
                    }
                    1 -> incomeViewModel.refresh()
                }
                Toast.makeText(requireContext(), "账单已更新", Toast.LENGTH_SHORT).show()
            }
            dialog.show(childFragmentManager, "AddExpenseDialog")
        }
    }

    // ============================================================
    // 供外部调用
    // ============================================================
    fun refreshConsume() {
        consumeViewModel.refresh()
        loadStats()
    }

    fun refreshIncome() {
        incomeViewModel.refresh()
    }

    fun loadMoreConsume() {
        consumeViewModel.loadNextPage()
    }

    fun loadMoreIncome() {
        incomeViewModel.loadNextPage()
    }

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
        (activity as? MainActivity)?.homeFragment = null
        _binding = null
    }

    private fun updateFabStyle(position: Int) {
        val fab = binding.homeFabAdd
        if (position == 0) {
            // 支出：红色
            fab.backgroundTintList = android.content.res.ColorStateList.valueOf(
                android.graphics.Color.parseColor("#D32F2F")
            )
            fab.setOnClickListener { showExpenseDialog() }
            binding.homeStatsCard.visibility = View.VISIBLE
            fab.visibility = View.VISIBLE
        } else {
            // 收入：绿色
            fab.backgroundTintList = android.content.res.ColorStateList.valueOf(
                android.graphics.Color.parseColor("#2B8A3E")
            )
            fab.setOnClickListener { showIncomeDialog() }
            binding.homeStatsCard.visibility = View.GONE
            fab.visibility = View.VISIBLE
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
            // 收入统计暂未实现，可留空
        }
        dialog.show(childFragmentManager, "IncomeAddDialog")
    }
}