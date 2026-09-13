package cn.yangwanhao.billapp.ui.home

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.yangwanhao.billapp.common.DateUtil
import cn.yangwanhao.billapp.databinding.FragmentIncomeListBinding
import cn.yangwanhao.billapp.ui.adapter.IncomeListAdapter

class IncomeListFragment : Fragment() {

    private var _binding: FragmentIncomeListBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: IncomeListAdapter

    companion object {
        fun newInstance(): IncomeListFragment = IncomeListFragment()
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

        // 初始显示空状态，避免数据加载前闪白
        showEmptyState(true)

        // 🔥 下拉刷新
        binding.swipeRefreshLayoutIncome.setOnRefreshListener {
            (parentFragment as? HomeFragment)?.refreshIncome()
        }

        // 监听列表数据（统计由 HomeFragment 统一处理）
        (parentFragment as? HomeFragment)?.let { homeFragment ->
            homeFragment.incomeViewModel.adapterItems.observe(viewLifecycleOwner) { items ->
                val safeItems = items ?: emptyList()
                adapter.submitList(safeItems)
                showEmptyState(safeItems.isEmpty())
                // 🔥 关闭下拉刷新状态
                binding.swipeRefreshLayoutIncome.isRefreshing = false
            }
        }

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

    private fun showEmptyState(isEmpty: Boolean) {
        binding.emptyView.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.rvIncomeList.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    // ============================================================
    //  点击查看详情
    // ============================================================
    @SuppressLint("DefaultLocale")
    private fun showIncomeDetail(item: IncomeListAdapter.IncomeListItem.IncomeItem) {
        val dateStr = DateUtil.dateIntToDisplay(item.postDate)
        val belongMonthStr = DateUtil.monthIntToDisplay(item.billMonth)
        val amountStr = String.format("%.2f", item.amount / 100.0)
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
        val amountStr = String.format("%.2f", item.amount / 100.0)
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