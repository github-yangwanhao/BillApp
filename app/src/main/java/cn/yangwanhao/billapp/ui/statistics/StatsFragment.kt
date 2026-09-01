package cn.yangwanhao.billapp.ui.statistics

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import cn.yangwanhao.billapp.R

class StatsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_stats, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 隐藏所有原有的统计控件（直接用占位文字替代）
        // 找到现有控件并隐藏
        val tvStatsMonth: TextView? = view.findViewById(R.id.tv_stats_month)
        val tvStatsIncome: TextView? = view.findViewById(R.id.tv_stats_income)
        val tvStatsExpense: TextView? = view.findViewById(R.id.tv_stats_expense)
        val tvStatsBalance: TextView? = view.findViewById(R.id.tv_stats_balance)
        val btnStatsPrev: View? = view.findViewById(R.id.btn_stats_prev)
        val btnStatsNext: View? = view.findViewById(R.id.btn_stats_next)

        // 隐藏原有内容
        tvStatsMonth?.visibility = View.GONE
        tvStatsIncome?.visibility = View.GONE
        tvStatsExpense?.visibility = View.GONE
        tvStatsBalance?.visibility = View.GONE
        btnStatsPrev?.visibility = View.GONE
        btnStatsNext?.visibility = View.GONE

        // 使用 tv_stats_income 作为占位文字显示（它是 TextView，居中显示合适）
        tvStatsIncome?.apply {
            visibility = View.VISIBLE
            text = "📊\n统计功能开发中\n敬请期待"
            textSize = 18f
            gravity = Gravity.CENTER
        }
    }
}