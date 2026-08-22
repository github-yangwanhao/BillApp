package cn.yangwanhao.billapp.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import cn.yangwanhao.billapp.R
import cn.yangwanhao.billapp.common.DateUtil
import cn.yangwanhao.billapp.ui.viewmodel.HomeViewModel

class StatsFragment : Fragment() {

    private lateinit var viewModel: HomeViewModel

    private lateinit var tvStatsMonth: TextView
    private lateinit var btnStatsPrev: ImageButton
    private lateinit var btnStatsNext: ImageButton
    private lateinit var tvStatsIncome: TextView
    private lateinit var tvStatsExpense: TextView
    private lateinit var tvStatsBalance: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_stats, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 与 Activity 共享同一个 HomeViewModel
        viewModel = ViewModelProvider(requireActivity())[HomeViewModel::class.java]

        // 绑定视图
        tvStatsMonth = view.findViewById(R.id.tv_stats_month)
        btnStatsPrev = view.findViewById(R.id.btn_stats_prev)
        btnStatsNext = view.findViewById(R.id.btn_stats_next)
        tvStatsIncome = view.findViewById(R.id.tv_stats_income)
        tvStatsExpense = view.findViewById(R.id.tv_stats_expense)
        tvStatsBalance = view.findViewById(R.id.tv_stats_balance)

        // 月份切换
        btnStatsPrev.setOnClickListener { viewModel.previousMonth() }
        btnStatsNext.setOnClickListener { viewModel.nextMonth() }

        // 观察月份变化
        viewModel.currentMonth.observe(viewLifecycleOwner) { month ->
            tvStatsMonth.text = DateUtil.monthIntToDisplay(month)
        }

        // 观察支出合计变化
        viewModel.monthTotal.observe(viewLifecycleOwner) { expense ->
            val expenseYuan = expense / 100.0
            tvStatsExpense.text = "¥${String.format("%.2f", expenseYuan)}"
            // 结余需要等收入数据也加载完才能计算，这里先显示支出
        }
    }
}