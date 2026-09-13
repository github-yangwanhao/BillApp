package cn.yangwanhao.billapp.ui.home

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.yangwanhao.billapp.R
import cn.yangwanhao.billapp.common.DateUtil
import cn.yangwanhao.billapp.databinding.FragmentIncomeListBinding
import cn.yangwanhao.billapp.ui.adapter.IncomeListAdapter
import kotlinx.coroutines.launch

class IncomeListFragment : Fragment() {

    private var _binding: FragmentIncomeListBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: IncomeListAdapter

    companion object {
        fun newInstance(): IncomeListFragment {
            return IncomeListFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIncomeListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val layoutManager = LinearLayoutManager(context)
        binding.rvIncomeList.layoutManager = layoutManager

        adapter = IncomeListAdapter(
            onItemClick = { item -> showIncomeDetail(item) },
            onItemLongClick = { item -> showDeleteConfirm(item) }
        )
        binding.rvIncomeList.adapter = adapter

        // 监听数据变化
        (parentFragment as? HomeFragment)?.let { homeFragment ->
            homeFragment.incomeViewModel.adapterItems.observe(viewLifecycleOwner) { items ->
                val safeItems = items ?: emptyList()
                adapter.submitList(safeItems)
            }

            // 监听统计信息
            homeFragment.incomeViewModel.monthlyTotal.observe(viewLifecycleOwner) { total ->
                updateStats()
            }
            homeFragment.incomeViewModel.monthlyCount.observe(viewLifecycleOwner) { count ->
                updateStats()
            }
            homeFragment.incomeViewModel.crossMonthCount.observe(viewLifecycleOwner) { count ->
                updateStats()
            }
        }

        // 滚动加载更多
        // 滚动加载更多
        binding.rvIncomeList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0) {
                    val totalItemCount = layoutManager.itemCount
                    val lastVisibleItem = layoutManager.findLastVisibleItemPosition()
                    if (lastVisibleItem >= totalItemCount - 3) {
                        (parentFragment as? HomeFragment)?.loadMoreIncome()
                    }
                }
            }
        })
    }

    /**
     * 更新统计卡片
     */
    @SuppressLint("SetTextI18n")
    private fun updateStats() {
        val homeFragment = parentFragment as? HomeFragment ?: return
        val total = homeFragment.incomeViewModel.monthlyTotal.value ?: 0
        val count = homeFragment.incomeViewModel.monthlyCount.value ?: 0
        val crossCount = homeFragment.incomeViewModel.crossMonthCount.value ?: 0

        val yuan = total / 100.0
        binding.tvStatsAmount.text = "¥${String.format("%.2f", yuan)}"
        binding.tvStatsBadge.text = "+¥${String.format("%.0f", yuan)}"

        val subText = if (crossCount > 0) {
            "共 $count 笔 · 含 $crossCount 笔跨月归属"
        } else {
            "共 $count 笔"
        }
        binding.tvStatsSub.text = subText
    }

    // ============================================================
    //  点击查看详情
    // ============================================================
    @SuppressLint("DefaultLocale")
    private fun showIncomeDetail(item: IncomeListAdapter.IncomeListItem.IncomeItem) {
        val dateStr = DateUtil.dateIntToDisplay(item.postDate)
        val belongMonthStr = DateUtil.monthIntToDisplay(item.billMonth)
        val amountYuan = item.amount / 100.0
        val amountStr = String.format("%.2f", amountYuan)
        val remark = item.remark.ifEmpty { "无" }

        val message = """
            入账日期：$dateStr
            所属月份：$belongMonthStr
            分类：${item.categoryName}
            金额：+¥$amountStr
            备注：$remark
        """.trimIndent()

        AlertDialog.Builder(requireContext())
            .setTitle("收入详情")
            .setMessage(message)
            .setPositiveButton("确定", null)
            .show()
    }

    // ============================================================
    //  长按删除
    // ============================================================
    @SuppressLint("DefaultLocale")
    private fun showDeleteConfirm(item: IncomeListAdapter.IncomeListItem.IncomeItem) {
        val dateStr = DateUtil.dateIntToDisplay(item.postDate)
        val amountYuan = item.amount / 100.0
        val amountStr = String.format("%.2f", amountYuan)
        val remark = item.remark.ifEmpty { "无" }

        val message = """
            入账日期：$dateStr
            分类：${item.categoryName}
            金额：+¥$amountStr
            备注：$remark
            
            确定要删除这笔收入吗？
        """.trimIndent()

        AlertDialog.Builder(requireContext())
            .setTitle("⚠️ 删除确认")
            .setMessage(message)
            .setPositiveButton("删除") { _, _ ->
                (parentFragment as? HomeFragment)?.deleteIncomeBill(item.id)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}