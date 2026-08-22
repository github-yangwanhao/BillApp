package cn.yangwanhao.billapp.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.ImageButton
import android.widget.TextView
import cn.yangwanhao.billapp.R
import cn.yangwanhao.billapp.common.DateUtil
import cn.yangwanhao.billapp.ui.adapter.BillListAdapter
import cn.yangwanhao.billapp.ui.adapter.DictSpinnerAdapter
import cn.yangwanhao.billapp.ui.viewmodel.HomeViewModel
import android.widget.Spinner

class HomeFragment : Fragment() {

    private lateinit var viewModel: HomeViewModel
    private lateinit var adapter: BillListAdapter

    // 视图引用
    private lateinit var tvMonth: TextView
    private lateinit var btnPrevMonth: ImageButton
    private lateinit var btnNextMonth: ImageButton
    private lateinit var tvMonthTotal: TextView
    private lateinit var rvBills: RecyclerView
    private lateinit var btnAddConsume: View
    private lateinit var btnAddIncome: View

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 获取 ViewModel（与 Activity 共享同一个实例）
        viewModel = ViewModelProvider(requireActivity())[HomeViewModel::class.java]

        // 绑定视图
        tvMonth = view.findViewById(R.id.tv_month)
        btnPrevMonth = view.findViewById(R.id.btn_prev_month)
        btnNextMonth = view.findViewById(R.id.btn_next_month)
        tvMonthTotal = view.findViewById(R.id.tv_month_total)
        rvBills = view.findViewById(R.id.rv_bills)
        btnAddConsume = view.findViewById(R.id.btn_add_consume)
        btnAddIncome = view.findViewById(R.id.btn_add_income)

        // 初始化 RecyclerView
        adapter = BillListAdapter()
        rvBills.layoutManager = LinearLayoutManager(requireContext())
        rvBills.adapter = adapter

        // 月份切换按钮
        btnPrevMonth.setOnClickListener { viewModel.previousMonth() }
        btnNextMonth.setOnClickListener { viewModel.nextMonth() }

        // 新增按钮
        btnAddConsume.setOnClickListener { showAddBillDialog(false) }
        btnAddIncome.setOnClickListener { showAddBillDialog(true) }

        // 观察数据变化
        observeData()
    }

    /** 观察 ViewModel 中的数据变化，自动刷新 UI */
    private fun observeData() {
        // 月份变化
        viewModel.currentMonth.observe(viewLifecycleOwner) { month ->
            tvMonth.text = DateUtil.monthIntToDisplay(month)
        }

        // 月度支出合计变化
        viewModel.monthTotal.observe(viewLifecycleOwner) { total ->
            val yuan = total / 100.0
            tvMonthTotal.text = "¥${String.format("%.2f", yuan)}"
        }

        // 账单列表变化
        viewModel.billItems.observe(viewLifecycleOwner) { items ->
            adapter.submitList(items)
        }

        // Toast 提示
        viewModel.toastMessage.observe(viewLifecycleOwner) { msg ->
            if (!msg.isNullOrEmpty()) {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                viewModel.onToastConsumed()
            }
        }
    }

    /** 弹出新增账单对话框 */
    private fun showAddBillDialog(isIncome: Boolean) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_bill, null)

        val spinnerCategory: Spinner = dialogView.findViewById(R.id.spinner_category)
        val spinnerChannel: Spinner = dialogView.findViewById(R.id.spinner_channel)
        val etAmount: EditText = dialogView.findViewById(R.id.et_amount)
        val etRemark: EditText = dialogView.findViewById(R.id.et_remark)

        // 根据类型显示/隐藏支付渠道选择器
        if (isIncome) {
            spinnerChannel.visibility = View.GONE
            dialogView.findViewById<TextView>(R.id.tv_channel_label).visibility = View.GONE
        }

        // 设置分类下拉
        val categoryAdapter = DictSpinnerAdapter(requireContext())
        spinnerCategory.adapter = categoryAdapter

        // 设置支付渠道下拉
        val channelAdapter = DictSpinnerAdapter(requireContext())
        spinnerChannel.adapter = channelAdapter

        // 加载下拉数据
        if (isIncome) {
            viewModel.incomeCategories.observe(viewLifecycleOwner) { items ->
                categoryAdapter.setData(items)
            }
        } else {
            viewModel.consumeCategories.observe(viewLifecycleOwner) { items ->
                categoryAdapter.setData(items)
            }
            viewModel.payChannels.observe(viewLifecycleOwner) { items ->
                channelAdapter.setData(items)
            }
        }

        val title = if (isIncome) "新增收入" else "新增支出"

        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setView(dialogView)
            .setPositiveButton("保存") { _, _ ->
                // 获取金额
                val amountStr = etAmount.text.toString().trim()
                if (amountStr.isEmpty()) {
                    Toast.makeText(requireContext(), "请输入金额", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // 将元转为分（四舍五入）
                val amountYuan = amountStr.toDoubleOrNull()
                if (amountYuan == null || amountYuan <= 0) {
                    Toast.makeText(requireContext(), "请输入有效金额", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val amountFen = (amountYuan * 100).toInt()

                // 获取选中的分类ID
                val categoryItem = categoryAdapter.getItemAt(spinnerCategory.selectedItemPosition)

                // 获取备注
                val remark = etRemark.text.toString().trim()

                if (isIncome) {
                    viewModel.addIncomeBill(
                        amount = amountFen,
                        categoryId = categoryItem.id.toInt(),
                        remark = remark
                    )
                } else {
                    // 获取选中的支付渠道ID
                    val channelItem = channelAdapter.getItemAt(spinnerChannel.selectedItemPosition)
                    viewModel.addConsumeBill(
                        amount = amountFen,
                        categoryId = categoryItem.id.toInt(),
                        payChannelId = channelItem.id.toInt(),
                        remark = remark
                    )
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
}