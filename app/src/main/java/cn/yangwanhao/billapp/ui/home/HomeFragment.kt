package cn.yangwanhao.billapp.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import cn.yangwanhao.billapp.databinding.FragmentHomeBinding
import com.google.android.material.tabs.TabLayoutMediator

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val consumeViewModel: ConsumeBillViewModel by viewModels()
    private val incomeViewModel: IncomeBillViewModel by viewModels()

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

        // 2. 观察分页数据，提交给 Adapter
        consumeViewModel.bills.observe(viewLifecycleOwner) { list ->
            adapter.consumeAdapter.submitList(list)
        }
        incomeViewModel.bills.observe(viewLifecycleOwner) { list ->
            adapter.incomeAdapter.submitList(list)
        }

        // 3. 首次进入页面，加载第一页数据
        consumeViewModel.loadFirstPage()
        incomeViewModel.loadFirstPage()

        // 4. FAB 点击事件
        binding.fabAddBill.setOnClickListener {
            val currentTab = binding.tabLayout.selectedTabPosition
            val dialog = AddBillDialogFragment.newInstance(currentTab) {
                // 保存成功后回调：重新从第一页加载
                if (currentTab == 0) consumeViewModel.loadFirstPage()
                else incomeViewModel.loadFirstPage()
            }
            dialog.show(childFragmentManager, "AddBillDialog")
        }
    }

    // ===== 供子 Fragment 调用，触发加载下一页 =====

    fun loadMoreConsume() {
        consumeViewModel.loadNextPage()
    }

    fun loadMoreIncome() {
        incomeViewModel.loadNextPage()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}