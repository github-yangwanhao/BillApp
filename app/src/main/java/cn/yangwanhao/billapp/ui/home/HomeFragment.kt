package cn.yangwanhao.billapp.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.viewpager2.widget.ViewPager2
import cn.yangwanhao.billapp.MainActivity
import cn.yangwanhao.billapp.databinding.FragmentHomeBinding
import cn.yangwanhao.billapp.ui.home.add.AddExpenseDialogFragment
import com.google.android.material.tabs.TabLayoutMediator

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    val consumeViewModel: ConsumeBillViewModel by viewModels()
    val incomeViewModel: IncomeBillViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 注册到 Activity
        (activity as? MainActivity)?.homeFragment = this

        // 🔥 ViewPager2 适配器：3 个页面（首页/统计/我的）
        val adapter = HomeViewPagerAdapter(this)
        binding.viewPager.adapter = adapter

        // 🔥 TabLayout 只用于支出/收入 Tab（如果只需两个 Tab，保留）
        // 如果底部导航已经有三 Tab，这里可能不需要 TabLayout，或者只显示支出/收入
        // 如果 TabLayout 不需要，可以隐藏
        // 这里根据你的实际需求决定
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "支出"
                1 -> "收入"
                else -> ""  // 统计和我的页面不显示 Tab
            }
        }.attach()
        // 只在第 0、1 页显示 TabLayout，第 2 页隐藏
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                binding.tabLayout.visibility = if (position <= 1) View.VISIBLE else View.GONE
            }
        })

        // 加载数据（仅在首页时加载）
        consumeViewModel.loadFirstPage()
        incomeViewModel.loadFirstPage()

        // FAB 点击事件（仅在首页时显示）
        binding.fabAddBill.setOnClickListener {
            val dialog = AddExpenseDialogFragment()
            dialog.setOnSaveSuccessListener {
                when (binding.tabLayout.selectedTabPosition) {
                    0 -> consumeViewModel.refresh()
                    1 -> incomeViewModel.refresh()
                }
                Toast.makeText(requireContext(), "账单已更新", Toast.LENGTH_SHORT).show()
            }
            dialog.show(childFragmentManager, "AddExpenseDialog")
        }

        // 切换页面时控制 FAB 显隐
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                // 第 0 页（首页）显示 FAB，其他隐藏
                binding.fabAddBill.visibility = if (position == 0) View.VISIBLE else View.GONE
            }
        })
    }

    // ============================================================
    //  供子 Fragment 调用 - 刷新列表
    // ============================================================
    fun refreshConsume() {
        consumeViewModel.refresh()
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
    }

    fun deleteIncomeBill(billId: Long) {
        incomeViewModel.deleteBill(billId)
    }

    /**
     * 供 Activity 调用，切换 ViewPager2 的 Tab
     */
    fun setCurrentTab(position: Int) {
        binding.viewPager.currentItem = position
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // 取消注册
        (activity as? MainActivity)?.homeFragment = null
        _binding = null
    }
}