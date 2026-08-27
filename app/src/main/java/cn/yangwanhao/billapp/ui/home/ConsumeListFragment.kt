package cn.yangwanhao.billapp.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.yangwanhao.billapp.common.DateUtil
import cn.yangwanhao.billapp.databinding.FragmentBillListBinding
import cn.yangwanhao.billapp.ui.adapter.BillListAdapter

class ConsumeListFragment : Fragment() {

    private var _binding: FragmentBillListBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: BillListAdapter

    companion object {
        fun newInstance(): ConsumeListFragment {
            return ConsumeListFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBillListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val layoutManager = LinearLayoutManager(context)
        binding.recyclerView.layoutManager = layoutManager

        // 🔥 创建 Adapter
        adapter = BillListAdapter(
            onItemClick = { bill -> showBillDetail(bill) },
            onItemLongClick = { bill -> showDeleteConfirm(bill) }
        )
        binding.recyclerView.adapter = adapter

        // 🔥 初始显示空状态（避免数据加载前空白）
        showEmptyState(true)

        // 🔥 监听数据变化
        (parentFragment as? HomeFragment)?.let { homeFragment ->
            homeFragment.consumeViewModel.adapterItems.observe(viewLifecycleOwner) { items ->
                // 安全处理：如果 items 为 null，当作空列表
                val safeItems = items ?: emptyList()

                // 🔥 打印日志，方便调试
                android.util.Log.d("ConsumeList", "收到数据，条数: ${safeItems.size}")

                adapter.submitList(safeItems)
                binding.swipeRefreshLayout.isRefreshing = false

                // 🔥 控制空状态显示
                showEmptyState(safeItems.isEmpty())
            }

            homeFragment.consumeViewModel.hasMore.observe(viewLifecycleOwner) { hasMore ->
                // 可选：显示/隐藏底部加载提示
            }
        }

        // 🔥 下拉刷新
        binding.swipeRefreshLayout.setOnRefreshListener {
            (parentFragment as? HomeFragment)?.refreshConsume()
        }

        // 🔥 滚动加载更多
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0) {
                    val totalItemCount = layoutManager.itemCount
                    val lastVisibleItem = layoutManager.findLastVisibleItemPosition()
                    if (lastVisibleItem >= totalItemCount - 3) {
                        (parentFragment as? HomeFragment)?.loadMoreConsume()
                    }
                }
            }
        })
    }

    // ============================================================
    //  空状态控制
    // ============================================================
    private fun showEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.emptyView.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
            android.util.Log.d("ConsumeList", "显示空状态")
        } else {
            binding.emptyView.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
            android.util.Log.d("ConsumeList", "隐藏空状态")
        }
    }

    // ============================================================
    //  点击查看详情
    // ============================================================
    private fun showBillDetail(bill: BillListAdapter.BillItem) {
        val dateStr = DateUtil.dateIntToDisplay(bill.payDate)
        val amountYuan = bill.amount / 100.0
        val amountStr = String.format("%.2f", amountYuan)
        val sign = if (bill.isIncome) "+" else "-"
        val channel = if (bill.isIncome) "—" else bill.channelName
        val remark = bill.remark.ifEmpty { "无" }

        val message = """
            日期：$dateStr
            分类：${bill.categoryName}
            渠道：$channel
            金额：$sign¥$amountStr
            备注：$remark
        """.trimIndent()

        AlertDialog.Builder(requireContext())
            .setTitle("账单详情")
            .setMessage(message)
            .setPositiveButton("确定", null)
            .show()
    }

    // ============================================================
    //  长按删除
    // ============================================================
    private fun showDeleteConfirm(bill: BillListAdapter.BillItem) {
        // 构建详细信息
        val dateStr = DateUtil.dateIntToDisplay(bill.payDate)
        val amountYuan = bill.amount / 100.0
        val amountStr = String.format("%.2f", amountYuan)
        val sign = if (bill.isIncome) "+" else "-"
        val remark = bill.remark.ifEmpty { "无" }

        val message = """
        日期：$dateStr
        分类：${bill.categoryName}
        渠道：${bill.channelName}
        金额：$sign¥$amountStr
        备注：$remark
        
        确定要删除这笔账单吗？""".trimIndent()

        AlertDialog.Builder(requireContext())
            .setTitle("⚠️ 删除确认")
            .setMessage(message)
            .setPositiveButton("删除") { _, _ ->
                (parentFragment as? HomeFragment)?.deleteConsumeBill(bill.id)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}