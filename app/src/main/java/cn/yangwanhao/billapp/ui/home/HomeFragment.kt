package cn.yangwanhao.billapp.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.viewpager2.widget.ViewPager2
import cn.yangwanhao.billapp.databinding.FragmentHomeBinding
import cn.yangwanhao.billapp.ui.add.AddExpenseDialogFragment
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

        // 1. 初始化 ViewPager2 和 TabLayout
        val adapter = ViewPagerAdapter(this)
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = if (position == 0) "支出" else "收入"
        }.attach()

        // 🔥 监听 ViewPager2 页面切换，切换时刷新数据
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                when (position) {
                    0 -> {
                        // 切换到支出 Tab → 刷新消费数据
                        consumeViewModel.refresh()
                    }
                    1 -> {
                        // 切换到收入 Tab → 刷新收入数据
                        incomeViewModel.refresh()
                    }
                }
            }
        })

        // 2. 首次加载数据
        consumeViewModel.loadFirstPage()
        incomeViewModel.loadFirstPage()

        // 3. FAB 点击事件
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}